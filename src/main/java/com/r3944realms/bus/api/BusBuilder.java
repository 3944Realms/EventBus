package com.r3944realms.bus.api;

import com.r3944realms.bus.BusBuilderImpl;

/**
 * 这是总线构造类 返回一个实现
 */
public interface BusBuilder {
    static BusBuilder builder() {
        return new BusBuilderImpl();
    }
    BusBuilder setExceptionHandler(IEventExceptionHandler handler);
    BusBuilder startShutdown();
    BusBuilder checkTypesOnDispatch();
    BusBuilder classChecker(IEventCLassChecker checker);

    /**
     * 允许调用 {@link IEventBus post(EventPriority, Event)}
     */
    BusBuilder allowPerPhasePost();
    IEventBus build();

}
