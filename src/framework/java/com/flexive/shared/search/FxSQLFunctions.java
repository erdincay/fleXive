package com.flexive.shared.search;

import com.flexive.shared.exceptions.FxNotFoundException;

import java.util.*;

import org.apache.commons.lang.StringUtils;

/**
 * Container for all supported FxSQL functions.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public final class FxSQLFunctions {
    private static final Map<String, FxSQLFunction> FUNCTIONS = new HashMap<String, FxSQLFunction>();
    static {
        for (FxSQLFunction[] values: Arrays.asList(new FxSQLFunction[][] {
                DateFunction.values()
        })) {
            for (FxSQLFunction function: values) {
                if (FUNCTIONS.put(function.getSqlName().toLowerCase(), function) != null) {
                    throw new IllegalStateException("Duplicate FxSQL function mapping for '" + function.getSqlName() + "'");
                }
            }
        }
    }

    /**
     * Prevent instantiation.
     */
    private FxSQLFunctions() {
    }

    /**
     * Returns the {@link FxSQLFunction} object mapped to the given function name. If name does not
     * represent a known FxSQL function, a FxRuntimeException is thrown.
     *
     * @param name      the function name to be  looked up
     * @return          the {@link FxSQLFunction} object
     */
    public static FxSQLFunction forName(String name) {
        return asFunctions(Arrays.asList(name)).get(0);
    }

    /**
     * Converts the given function names to {@link FxSQLFunction} objects. If an entry does not
     * represent a known FxSQL function, a FxRuntimeException is thrown.
     *
     * @param values    the function names to be converted
     * @return          the {@link FxSQLFunction} objects
     */
    public static List<FxSQLFunction> asFunctions(Collection<String> values) {
        final List<FxSQLFunction> result = new ArrayList<FxSQLFunction>(values.size());
        for (String value: values) {
            if (!FUNCTIONS.containsKey(value.toLowerCase())) {
                throw new FxNotFoundException("ex.sqlSearch.function.notFound", value,
                        StringUtils.join(getSqlNames(FUNCTIONS.values()), ", ")).asRuntimeException();
            }
            result.add(FUNCTIONS.get(value.toLowerCase()));
        }
        return result;
    }

    /**
     * Return the FxSQL function names for the given list of {@link com.flexive.shared.search.FxSQLFunction}
     * objects, i.e. project the result of {@link FxSQLFunction#getSqlName()}.
     *
     * @param functions the FxSQL function object
     * @return  the FxSQL function names as used in FxSQL
     */
    public static List<String> getSqlNames(Collection<? extends FxSQLFunction> functions) {
        final List<String> result = new ArrayList<String>(functions.size());
        for (FxSQLFunction function : functions) {
            result.add(function.getSqlName());
        }
        return result;
    }
}
