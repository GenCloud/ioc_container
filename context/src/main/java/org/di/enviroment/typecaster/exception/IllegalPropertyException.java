package org.di.enviroment.typecaster.exception;

import java.io.IOException;

/**
 * Illegal type casting appeared when type caster cannot identify field type.
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class IllegalPropertyException extends IOException {
    public IllegalPropertyException() {
        super();
    }

    public IllegalPropertyException(String message) {
        super(message);
    }

    public IllegalPropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalPropertyException(Throwable cause) {
        super(cause);
    }
}
