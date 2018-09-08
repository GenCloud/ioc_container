package org.di.excepton;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
public abstract class IntroException extends Exception {
    public IntroException() {
        super();
    }

    protected IntroException(String message) {
        super(message);
    }

    protected IntroException(String message, Throwable cause) {
        super(message, cause);
    }

    protected IntroException(Throwable cause) {
        super(cause);
    }
}
