package com.r3944realms.bus.api;

/**
 * 事件监听器通过保证实现这个类实现
 */
public abstract sealed class EventListener
    permits ConsumerEventHandler, GeneratedEventListener, SubscribeEventListener{
    public abstract void invoke(Event event);
}
