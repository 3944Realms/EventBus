package com.r3944realms.bus;

import com.r3944realms.bus.api.EventListener;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class EventBusErrorMessage implements Message, StringBuilderFormattable {
    private final int index;
    private final EventListener[] listeners;
    private final Throwable throwable;

    public EventBusErrorMessage(final int index, final EventListener[] listeners, final Throwable throwable) {
        this.index = index;
        this.listeners = listeners;
        this.throwable = throwable;
    }

    @Override
    public String getFormattedMessage() {
        return "";
    }

    @Override
    public String getFormat() {
        return "";
    }

    @Override
    public Object[] getParameters() {
        return new Object[0];
    }

    @Override
    public Throwable getThrowable() {
        //Copy from NeoForgeBus:
        //      Cannot return the throwable here - it causes weird classloading issues inside log4j
        return null;
    }

    @Override
    public void formatTo(StringBuilder buffer) {
        buffer.
                append("Exception caught during firing event" ).append(throwable.getMessage()).append("\n").
                append("\tIndex:").append(index).append("\n").
                append("\tListeners:");
        for (int i = 0; i < listeners.length; i++) {
            buffer.append("\t\t").append(i).append(": ").append(listeners[i]).append('\n');
        }
        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        buffer.append(sw);
    }
}
