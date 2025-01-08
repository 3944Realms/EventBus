package com.r3944realms.bus.api;

public interface ICancellableEvent {
    default void setCanceled(boolean canceled) {
        ((Event)this).isCancelled = canceled;
    }
    default boolean isCanceled() {
        return ((Event)this).isCancelled;
    }
}
