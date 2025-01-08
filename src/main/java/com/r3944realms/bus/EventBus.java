package com.r3944realms.bus;

import com.r3944realms.bus.api.*;
import net.jodah.typetools.TypeResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.r3944realms.bus.LogMakers.EVENTBUS;

public class EventBus implements IEventBus, IEventExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean checkTypesOnDispatchProperty = Boolean.parseBoolean(System.getProperty("eventbus.checkTypesOnDispatch", "false"));

    private ConcurrentHashMap<Object, List<EventListener>> listeners = new ConcurrentHashMap<>();
    private final LockHelper<Class<?>, ListenerList> listenerLists = LockHelper.withIdentityHashMap();
    private final IEventExceptionHandler exceptionHandler;
    private volatile boolean shutdown;

    private final IEventCLassChecker classChecker;
    private final boolean checkTypesOnDispatch;
    private final boolean allowPerPhasePost;

    private EventBus() {
        this(new BusBuilderImpl());
    }

    private EventBus(
            final IEventExceptionHandler handler,
            boolean startShutdown, IEventCLassChecker classChecker,
            boolean checkTypesOnDispatch,
            boolean allowPerPhasePost
    ) {
        exceptionHandler = handler == null ? this : handler;
        this.shutdown = startShutdown;
        this.classChecker = classChecker;
        this.checkTypesOnDispatch = checkTypesOnDispatch || checkTypesOnDispatchProperty;
        this.allowPerPhasePost = allowPerPhasePost;
    }

    public EventBus(final BusBuilderImpl busBuilder) {
        this(
                busBuilder.exceptionHandler,
                busBuilder.startShutdown,
                busBuilder.classChecker,
                busBuilder.checkTypesOnDispatch,
                busBuilder.allowPerPhasePost);
    }

    @Override
    public void register(Object target) {
        if (listeners.containsKey(target))
            return;

        boolean isStatic = target.getClass() == Class.class;
        Class<?> clazz = isStatic ? (Class<?>) target : target.getClass();
        checkSupertypes(clazz, clazz);

        int foundMethods = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class))
                continue;
            if (Modifier.isStatic(method.getModifiers()) == isStatic)
                registerListener(target, method, method);
            else {
                if (isStatic)
                    throw new IllegalArgumentException("""
                            预期的带@SubscribeEvent方法%s应该是静态的
                            因为register()带有一个类类型的调用。
                            要么让方法静态，要么调用一个带%s实例的register()方法
                            """.formatted(method, clazz));
                else
                    throw new IllegalArgumentException("""
                            
                            """);
            }
            ++foundMethods;
        }
    }

    private static void checkSupertypes(Class<?> registerType, Class<?> type) {
        if (type == null || type == Object.class)
            return;
        if (type != registerType) {
            for (var method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubscribeEvent.class))
                    throw new IllegalArgumentException("""
                            正在尝试注册%s类型的监听器对象，
                            但是，它的父类%s有一个带有@SubscribeEvent注解的方法：%s。
                            这是不允许的！只有监听器对象可以具有@SubscribeEvent方法。
                            """.formatted(registerType, type, method));
            }
        }
        checkSupertypes(registerType, type.getSuperclass());
        Stream.of(type.getInterfaces())
                .forEach(i -> checkSupertypes(registerType, i));
    }

    @Override
    public <T extends Event> void addListener(final Consumer<T> consumer) {
        addListener(EventPriority.NORMAL, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final Consumer<T> consumer) {
        addListener(priority, false, consumer);
    }

    @Override
    public <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> consumer) {
        addListener(EventPriority.NORMAL, receiveCanceled, consumer);
    }

    @Override
    public <T extends Event> void addListener(Class<T> eventType, Consumer<T> consumer) {
        addListener(EventPriority.NORMAL, false, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer) {
        addListener(priority, false, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer) {
         addListener(EventPriority.NORMAL, receiveCanceled, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCanceled, final Consumer<T> consumer) {
        addListener(priority, passNotGenericFilter(receiveCanceled), consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCanceled, final Class<T> eventType, final Consumer<T> consumer) {
        addListener(priority, passNotGenericFilter(receiveCanceled), eventType, consumer);
    }

    @SuppressWarnings("unchecked")
    private void registerListener(final Object target, final Method method, final Method real) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1)
            throw new IllegalArgumentException("""
                    方法 %s 带有@SubscribeEvent注解,
                    其需要 %d 个参数，
                    但是事件处理器方法必须要求为单一参数。
                    """.formatted(method, parameterTypes.length));

        Class<?> eventType = parameterTypes[0];

        if(!Event.class.isAssignableFrom(eventType))
            throw new IllegalArgumentException("""
                    方法 %s 虽然带有@SubscribeEvent注解，
                    但是其唯一参数类型 %s 并不是事件类的子类
                    """.formatted(method, eventType));
        try {
            classChecker.check((Class<? extends Event>) eventType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("""
                    方法 %s 虽然带有@SubscribeEvent注解，
                    但是其参数在 %s 总线上无效
                    """.formatted(method, eventType), e);
        }
        register(eventType, target, real);
    }



    private void register(Class<?> eventType, Object target, Method method) {
        SubscribeEventListener listener = new SubscribeEventListener(target, method);
        addToListeners(target, eventType, listener, listener.getPriority());
    }

    private void addToListeners(final Object target, final Class<?> eventType, final EventListener listener, final EventPriority priority) {
        if (Modifier.isAbstract(eventType.getModifiers()))
            throw new IllegalArgumentException("""
                    不能为抽象的%s注册监听器，请为其非抽象子类注册监听器。
                    """.formatted(eventType));
        getListenList(eventType).register(priority, listener);
        List<EventListener> others = listeners.computeIfAbsent(target, k -> Collections.synchronizedList(new ArrayList<>()));
        others.add(listener);
    }

    private ListenerList getListenList(Class<?> eventType) {
        ListenerList list = listenerLists.get(eventType);
        if (list != null)
            return list;

        if (Modifier.isAbstract(eventType.getSuperclass().getModifiers())) {
            validateAbstractChain(eventType.getSuperclass());

            return listenerLists.computeIfAbsent(eventType, e -> new ListenerList(e, allowPerPhasePost));
        } else
            return listenerLists.computeIfAbsent(eventType, e -> new ListenerList(e, getListenList(e.getSuperclass()), allowPerPhasePost));
    }

    private static void validateAbstractChain(Class<?> eventType) {
        while (eventType != Event.class) {
            if(!Modifier.isAbstract(eventType.getSuperclass().getModifiers())) {
                throw new IllegalArgumentException("""
                        抽象事件 %s 有一个非抽象的父类 %s。
                        该父类必须为抽象类。
                        """.formatted(eventType, eventType.getSuperclass()));
            }

            eventType = eventType.getSuperclass();
        }
    }
    @SuppressWarnings("unchecked")
    private <T extends Event> Class<T> getEventClass(Consumer<T> consumer) {
        Class<T> eventClass = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
        if((Class<?>)eventClass == TypeResolver.Unknown.class) {
            LOGGER.error(EVENTBUS, "不能够为\"{}\"解析处理器", consumer.toString());
            throw new IllegalStateException("无法解析无返回事件类型：" + consumer);
        }
        return eventClass;
    }

    private <T extends Event> void addListener(final EventPriority priority, @Nullable Predicate<? super T> filter, final Consumer<T> consumer) {
        Class<T> eventClass = getEventClass(consumer);
        if (Objects.equals(eventClass, Event.class)) {
            LOGGER.warn(EVENTBUS, "尝试添加一个具有通用计算类型的Lambda监听器。" +
                    "你能确保他会按你的预期工作吗? 注意：Lambda表达式所具有的通用计算类型" +
                    "将会在运行时擦除以及无法被解析。");
        }
        addListener(priority, filter, eventClass, consumer);
    }

    private <T extends Event> Predicate<T> passNotGenericFilter(boolean receiveCanceled) {
        //该转型是安全的，因为当事件是不可取消时过滤器会被移除
        return receiveCanceled ? null : e -> !((ICancellableEvent)e).isCanceled();
    }

    private <T extends Event> void addListener(final EventPriority priority, @Nullable Predicate<? super T> filter, final Class<T> eventClass, final Consumer<T> consumer) {
        try {
            classChecker.check(eventClass);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "该事件" + eventClass + "的监听器带有一个无效于当前事件总系的参数"
            );
        }
        @SuppressWarnings("unchecked") EventListener listener = filter == null ?
                new ConsumerEventHandler((Consumer<Event>) consumer) :
                new ConsumerEventHandler.WithPredicate((Consumer<Event>) consumer, (Predicate<Event>) filter);
        addToListeners(consumer, eventClass, listener, priority);
    }
    private void doPostChecks(Event event) {
        if (checkTypesOnDispatch) {
            try {
                classChecker.check(event.getClass());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "不能发布事件的类型"+event.getClass().getSimpleName()+"在该总线上", e
                );
            }
        }
    }

    @Override
    public void unregister(Object object) {
        List<EventListener> list = listeners.remove(object);
        if(list == null)
            return;
        for (ListenerList listenerList : listenerLists.getReadMap().values()) {
            for (EventListener listener : list) {
                listenerList.unregister(listener);
            }
        }
    }

    @Override
    public <T extends Event> T post(T event) {
        if(shutdown) {
            return event;
        }
        doPostChecks(event);
        return post(event, getListenList(event.getClass()).getListeners());
    }

    @Override
    public <T extends Event> T post(EventPriority phase, T event) {
        if (!allowPerPhasePost) {
            throw new IllegalStateException("该总线不允许呼叫指定阶段的发布");
        }
        if(shutdown) {
            return event;
        }
        doPostChecks(event);
        return post(event, getListenList(event.getClass()).getPrePhaseListeners(phase));
    }

    private <T extends Event> T post(T event, EventListener[] listeners) {
        int index = 0;
        try {
            for (;index < listeners.length; index++) {
                listeners[index].invoke(event);
            }
        } catch (Throwable throwable) {
            exceptionHandler.handleException(this, event, listeners, index, throwable);
            throw throwable;
        }
        return event;
    }

    @Override
    public void handleException(IEventBus bus, Event event, EventListener[] listeners, int index, Throwable throwable) {
        LOGGER.error(EVENTBUS, () -> new EventBusErrorMessage(index, listeners, throwable));
    }

    @Override
    public void start() {
        this.shutdown = false;
    }
}
