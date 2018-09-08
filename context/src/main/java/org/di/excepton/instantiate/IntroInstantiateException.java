package org.di.excepton.instantiate;

import org.di.excepton.IntroException;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
public class IntroInstantiateException extends IntroException {
    public IntroInstantiateException() {
        super();
    }

    public IntroInstantiateException(String message) {
        super(message);
    }

    public IntroInstantiateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntroInstantiateException(Throwable cause) {
        super(cause);
    }

    public <T> IntroInstantiateException(Class<T> clazz, String message) {
        super("Failed to instantiate [" + clazz.getName() + "]: " + message);
    }
}
