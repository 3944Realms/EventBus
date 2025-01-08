package com.r3944realms.bus.api;

import java.util.function.Consumer;

/**
 * 事件总系API
 * 注册以及发布事件
 */
public interface IEventBus {
    /**
     * 注册一个实例对象或实例类, 已及为所有带 {@link SubscribeEvent} 注解的方法添加监听器。<br/>
     * 以这里为基础。<br/>
     *<br/>
     * 根据传输的参数类型，来执行不同的侦听器创建行为。<br/>
     *<br/>
     * <dl>
     *     <dt>对象实例</dt>
     *     <dd>扫描带有{@link SubscribeEvent}注解的<em>非静态</em>方法<br/>并为遍历每一个这样的方法其添加监听器。<br/>
     *     </dd>
     *     <dt>类实例</dt>
     *     <dd>扫描带有{@link SubscribeEvent}注解的<em>静态</em>方法<br/>并为遍历的每一个这样的方法添加监听器<br/>
     *     </dd>
     * </dl>
     *
     * @param target  用于扫描和创建事件监听器的{@link Class 类} 实例 或任意一个对象。、
     */
    void register(Object target);

    /**
     * 添加一个{@link EventPriority#NORMAL 普通优先级}且不可取消事件的无返回的监听器。
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(Consumer<T> consumer);

    /**
     * 添加一个不可取消事件的无返回监视器。当其它方法中的一个无法确定要订阅的具体{@link Event 事件}子类时使用这个方法。
     * @param eventType 所订阅具体的{@link Event 事件}子类
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(Class<T> eventType, Consumer<T> consumer);

    /**
     * 添加一个明确{@link EventPriority 优先级}且不可取消事件的无返回的监听器。
     * @param priority 此监听器的{@link EventPriority 优先级}
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer);

    /**
     * 添加一个明确{@link EventPriority 优先级}且不可取消事件的无返回监视器。当其它方法中的一个无法确定要订阅的具体{@link Event 事件}子类时使用这个方法。
     * @param priority 此监听器的{@link EventPriority 优先级}
     * @param eventType 所订阅具体的{@link Event 事件}子类
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer);

    /**
     * 添加一个明确{@link EventPriority 优先级}且可能可取消事件的无返回监视器。
     * @param priority 此监听器的{@link EventPriority 优先级}
     * @param receiveCanceled 此监视器是否应接收已{@link ICancellableEvent}取消的事件
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> consumer);

    /**
     * 添加一个明确{@link EventPriority 优先级}且可能可取消事件的无返回监视。当其它方法中的一个无法确定要订阅的具体{@link Event 事件}子类时使用这个方法。
     * @param priority 此监听器的{@link EventPriority 优先级}
     * @param receiveCanceled 此监视器是否应接收已{@link ICancellableEvent}取消的事件
     * @param eventType 所订阅具体的{@link Event 事件}子类
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer);

    /**
     * 添加一个可能可取消事件的无返回监视器。
     * @param receiveCanceled 此监视器是否应接收已{@link ICancellableEvent}取消的事件
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> consumer);

    /**
     * 添加一个可能可取消事件的无返回监视。
     * <p>
     * 当其它方法中的一个无法确定要订阅的具体{@link Event 事件}子类时使用这个方法。
     * @param receiveCanceled 此监视器是否应接收已{@link ICancellableEvent}取消的事件
     * @param eventType 所订阅具体的{@link Event 事件}子类
     * @param consumer 当接收到一个匹配的事件将回调调用
     */
    <T extends Event> void addListener(boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer);

    /**
     * 从事件总线里取消注册提供的监听器。从事件里移除所有的监听器。
     * <p>
     * <br/> <b>注意：如果无返回值函数需要注销，则可以将其存储在变量中。</b>
     * @param target 要取消注册的对象，{@link Class 类}，{@link Consumer 无返回函数}
     */
    void unregister(Object target);

    /**
     * 将事件提分发给合适的监听器。
     * <p>
     * 如果此总线尚未启动，则事件将返回而不被调度。
     * @param event 将分配给监听器的事件
     * @return 传入的事件
     */
    <T extends Event> T post(T event);

    /**
     * 将事件分发给合适的注册有指定的{@link EventPriority 优先级}的监听器。
     * <p>
     * 如果此总线尚未启动，则事件将返回而不被调度。
     * <p>
     * 通过此方法逐阶段手动发布事件的性能不如通过{@link #post(Event)}调用分派到所有阶段。当不需要按阶段调度时，更倾向于后者。
     * @param event 将分配给监听器的事件
     * @return 传入的事件
     * @throws IllegalStateException 如果目前总线不允许提前阶段发布。
     * @see BusBuilder#allowPerPhasePost()
     */
    <T extends Event> T post(EventPriority priority, T event);
    void start();
}
