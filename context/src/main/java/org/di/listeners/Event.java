package org.di.listeners;

import java.util.EventObject;

/**
 * Wrapper class for all application events.
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public abstract class Event extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public Event(Object source) {
        super(source);
    }
}
