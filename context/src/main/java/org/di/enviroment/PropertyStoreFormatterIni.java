package org.di.enviroment;

import java.util.Map;

/**
 * Store generator for ini-files format.
 * <p>
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class PropertyStoreFormatterIni extends PropertyStoreFormatterImpl {
    @Override
    public String generate() {
        String lineSeparator = System.getProperty("line.separator");
        boolean isFirstField = true;
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> pair : pairs.entrySet()) {
            if (isFirstField)
                isFirstField = false;
            else
                builder.append(lineSeparator).append(lineSeparator);

            builder.append(pair.getKey()).append(" = ").append(pair.getValue());
        }

        return builder.toString();
    }
}
