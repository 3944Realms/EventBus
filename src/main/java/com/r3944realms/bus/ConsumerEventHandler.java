package com.r3944realms.bus;

import com.r3944realms.bus.api.Event;
import com.r3944realms.bus.api.EventListener;

import java.util.function.Consumer;
import java.util.function.Predicate;

public sealed class ConsumerEventHandler extends EventListener {
    protected final Consumer<Event> consumer;

    public ConsumerEventHandler(Consumer<Event> consumer) {
        this.consumer = consumer;
    }

    @Override
    public String toString() {
        return consumer.toString();
    }

    @Override
    public void invoke(Event event) {
        consumer.accept(event);
    }

    public static final class WithPredicate extends ConsumerEventHandler implements IWrapperListener {
        private final Predicate<Event> predicate;
        private final EventListener withoutCheck;
        public WithPredicate(Consumer<Event> consumer, Predicate<Event> predicate) {
            super(consumer);
            this.predicate = predicate;
            this.withoutCheck = new ConsumerEventHandler(consumer);
        }

        @Override
        public void invoke(Event event) {
            if (predicate.test(event))
                consumer.accept(event);

        }

        @Override
        public EventListener getWithoutCheck() {
            return withoutCheck;
        }
    }
}
