package com.r3944realms.bus;

import com.r3944realms.bus.api.*;

import java.lang.reflect.Method;

import static org.objectweb.asm.Type.getMethodDescriptor;

/**
 * 包装生成自{@link SubscribeEvent}注解方法的事件处理器
 */
public final class SubscribeEventListener extends EventListener implements IWrapperListener {
    private final EventListener handler;
    private final SubscribeEvent subInfo;
    private String readable;
    public SubscribeEventListener(Object target, Method method) {
        handler = EventListenerFactory.create(method, target);

        subInfo = method.getAnnotation(SubscribeEvent.class);
        readable = "@SubscribeEvent: " + target + " " + method.getName() + getMethodDescriptor(method);
    }

    @Override
    public void invoke(Event event) {
        if (handler != null)
            //因为如果这个事件是不可取消的，那么检查将会被移除，所以这个转型是安全的
            if(!((ICancellableEvent)event).isCanceled())
                handler.invoke(event);
    }

    @Override
    public EventListener getWithoutCheck() {
        return handler;
    }
    public EventPriority getPriority() {
        return subInfo.priority();
    }

    @Override
    public String toString() {
        return readable;
    }
}
