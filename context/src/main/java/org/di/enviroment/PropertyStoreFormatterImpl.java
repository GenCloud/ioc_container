package org.di.enviroment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formatter base with using LinkedHashMap to store property pairs.
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public abstract class PropertyStoreFormatterImpl implements IPropertyStoreFormatter {
    protected Map<String, String> pairs = new LinkedHashMap<>();

    @Override
    public void addPair(String key, String value) {
        if (!pairs.containsKey(key)) {
            pairs.put(key, value);
        }
    }
}
