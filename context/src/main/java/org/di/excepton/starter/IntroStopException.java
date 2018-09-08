package org.di.excepton.starter;

import org.di.excepton.IntroException;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
public class IntroStopException extends IntroException {
    public IntroStopException() {
        super();
    }

    public IntroStopException(String message) {
        super(message);
    }

    protected IntroStopException(String message, Throwable cause) {
        super(message, cause);
    }

    protected IntroStopException(Throwable cause) {
        super(cause);
    }
}
