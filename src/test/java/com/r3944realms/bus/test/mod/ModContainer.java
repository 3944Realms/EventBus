package com.r3944realms.bus.test.mod;

import com.r3944realms.bus.api.Event;
import com.r3944realms.bus.api.EventPriority;
import com.r3944realms.bus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import static com.r3944realms.bus.test.mod.Logging.LOADING;

public abstract class ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();

    protected final String modId;

    public ModContainer(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    @Nullable
    public abstract IEventBus getEventBus();

    public final <T extends Event> void acceptEvent(T event) {
        IEventBus eventBus = getEventBus();
        if (eventBus == null)
            return;
        try {
            LOGGER.trace("Firing event for modid {} : {}", this.getModId() , event);
            eventBus.post(event);
            LOGGER.trace("Fired event for modid {} : {}", this.getModId() , event);
        } catch (Throwable t) {
            LOGGER.error(LOADING, "Caught excepting during event {} disptch for modid {}", event, this.getModId(),t);
            throw new RuntimeException(t);
        }
    }
    public final <T extends Event> void acceptEvent(EventPriority phase, T event) {
        IEventBus eventBus = getEventBus();
        if (eventBus == null)
            return;
        try {
            LOGGER.trace("Firing event for modid {} : {}", this.getModId() , event);
            eventBus.post(phase, event);
            LOGGER.trace("Fired event for modid {} : {}", this.getModId() , event);
        } catch (Throwable t) {
            LOGGER.error(LOADING, "Caught excepting during event {} disptch for modid {}", event, this.getModId(),t);
            throw new RuntimeException(t);
        }
    }
}
