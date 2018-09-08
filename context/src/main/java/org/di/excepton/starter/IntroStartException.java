package org.di.excepton.starter;

import org.di.excepton.IntroException;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
public class IntroStartException extends IntroException {
    public IntroStartException() {
        super();
    }

    public IntroStartException(String message) {
        super(message);
    }

    protected IntroStartException(String message, Throwable cause) {
        super(message, cause);
    }

    protected IntroStartException(Throwable cause) {
        super(cause);
    }
}
