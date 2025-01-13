package com.r3944realms.bus.test.mod;

import com.r3944realms.bus.EventBusErrorMessage;
import com.r3944realms.bus.api.BusBuilder;
import com.r3944realms.bus.api.Event;
import com.r3944realms.bus.api.EventListener;
import com.r3944realms.bus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;

public class TestModContainer extends ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker LOADING = MarkerManager.getMarker("LOADING");
    private final IEventBus eventBus;

    public TestModContainer(String modId) {
        super(modId);
        this.eventBus = BusBuilder.builder()
                .setExceptionHandler(this::onEventFailed)
                .startShutdown()
                .allowPerPhasePost()
                .build();
    }

    @Override
    public @Nullable IEventBus getEventBus() {
        return eventBus;
    }

    private void onEventFailed(IEventBus eventBus, Event event, EventListener[] iEventListeners, int i, Throwable throwable) {
        LOGGER.error(new EventBusErrorMessage(i, iEventListeners, throwable));
    }
}
