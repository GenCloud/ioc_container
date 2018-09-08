package org.di.listeners;

import java.util.EventListener;

/**
 * Interface to be implemented by application event listeners.
 *
 * @author GenCloud
 * @date 04.09.2018
 */
@FunctionalInterface
public interface Listener<E extends Event> extends EventListener {
    void onEvent(E event);
}
