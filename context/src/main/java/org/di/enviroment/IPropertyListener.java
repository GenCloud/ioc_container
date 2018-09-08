package org.di.enviroment;

/**
 * Properties event listener. Target class should implement this interface to be available to listen nProperty events.
 * In other cases events will not be called.
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public interface IPropertyListener {
    /**
     * When parsing of configuration file starts.
     *
     * @param path Path to configuration file.
     */
    void onStart(String path);

    /**
     * When some property is missing.
     *
     * @param name Missed property name.
     */
    void onPropertyMiss(String name);

    /**
     * When done parsing configuration file.
     *
     * @param path File path.
     */
    void onDone(String path);

    /**
     * When property value casting is invalid.
     *
     * @param name  Property name.
     * @param value Casted value.
     */
    void onInvalidPropertyCast(String name, String value);
}
