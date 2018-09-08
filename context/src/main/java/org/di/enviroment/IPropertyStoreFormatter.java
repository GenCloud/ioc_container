package org.di.enviroment;

import java.io.IOException;

/**
 * Interface for store configuration files in different formats.
 * <p>
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public interface IPropertyStoreFormatter {
    /**
     * Adds property entry.
     *
     * @param key   Entry key.
     * @param value Entry value.
     */
    void addPair(String key, String value);

    /**
     * Generates configuration file text based on type of this store formatter.
     *
     * @return Generated configuration file.
     * @throws IOException Used to re-throw unusual exceptions during format.
     */
    String generate() throws IOException;
}
