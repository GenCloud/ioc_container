package org.di.listeners.impl;

import org.di.listeners.Event;

/**
 * @author GenCloud
 * @date 05.09.2018
 */
public class OnContextDestroyEvent extends Event {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public OnContextDestroyEvent(Object source) {
        super(source);
    }
}
