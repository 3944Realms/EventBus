package com.r3944realms.bus;

import com.r3944realms.bus.api.BusBuilder;
import com.r3944realms.bus.api.IEventBus;
import com.r3944realms.bus.api.IEventCLassChecker;
import com.r3944realms.bus.api.IEventExceptionHandler;

public final class BusBuilderImpl implements BusBuilder {
    IEventExceptionHandler exceptionHandler;
    boolean startShutdown = false;
    boolean checkTypesOnDispatch = false;
    IEventCLassChecker classChecker;
    boolean allowPerPhasePost = false;

    public BusBuilder setExceptionHandler(IEventExceptionHandler handler) {
        this.exceptionHandler = handler;
        return this;
    }

    @Override
    public BusBuilder startShutdown() {
        this.startShutdown = true;
        return this;
    }

    @Override
    public BusBuilder checkTypesOnDispatch() {
        this.checkTypesOnDispatch = true;
        return this;
    }

    @Override
    public BusBuilder classChecker(IEventCLassChecker checker) {
        this.classChecker = checker;
        return this;
    }

    @Override
    public BusBuilder allowPerPhasePost() {
        this.allowPerPhasePost = true;
        return this;
    }

    @Override
    public IEventBus build() {
        return new EventBus(this);
    }
}
