package com.r3944realms.bus.api;

@FunctionalInterface
public interface IEventCLassChecker {
    /**
     * 仅在事件不能被总线接收时抛出异常 {@link IllegalArgumentException}.
     * @throws IllegalArgumentException 事件类无效
     */
    void check(Class<? extends Event> eventClass) throws IllegalArgumentException;
}
