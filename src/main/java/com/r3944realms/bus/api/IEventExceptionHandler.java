package com.r3944realms.bus.api;

public interface IEventExceptionHandler {
    /**
     * 当{@link EventListener}在事件总线上为指定事件抛出异常时将触发。
     *  <br/>
     * 此方法返回后，原始{@link Throwable}向上传播
     * @param bus 所触发事件所在的总线
     * @param event 触发的事件
     * @param listeners (有序)所有监听该事件的监听器数组
     * @param index 目前触发该事件的监听器索引
     * @param throwable 将要抛出的异常
     */
    void handleException(IEventBus bus, Event event, EventListener[] listeners, int index, Throwable throwable);
}
