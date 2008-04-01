package com.flexive.shared.configuration;

import java.util.Map;
import java.util.HashMap;

/**
 * Provides a type-safe collection of parameters and their values.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class ParameterMap {
    private Map<Parameter, Object> values;

    public <T> void put(Parameter<T> parameter, T value) {
        if (values == null) {
            values = new HashMap<Parameter, Object>();
        }
        values.put(parameter, value);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T get(Parameter<T> parameter) {
        if (values == null) {
            return null;
        }
        return (T) values.get(parameter);
    }
}
