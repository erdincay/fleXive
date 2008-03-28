package com.flexive.shared.search;

import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.FxDataType;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

/**
 * FxSQL functions available for date/datetime queries and selects.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public enum DateFunction implements FxSQLFunction {
    YEAR("YEAR"), MONTH("MONTH"), DAY("DAY"), HOUR("HOUR"), MINUTE("MINUTE"), SECOND("SECOND");

    private final String sqlName;

    DateFunction(String sqlName) {
        this.sqlName = sqlName;
    }

    public String getSqlName() {
        return sqlName;
    }

    public FxDataType getOverrideDataType() {
        return FxDataType.Number;   // all currently supported date functions return integers
    }


    /**
     * Return the date function for the given FxSQL name (e.g. "year").
     *
     * @param sqlName   the FxSQL function name
     * @return  the date function
     * @throws com.flexive.shared.exceptions.FxRuntimeException if the given SQL name is not a date function
     */
    public static DateFunction getBySqlName(String sqlName) {
        for (DateFunction function: DateFunction.values()) {
            if (function.getSqlName().equalsIgnoreCase(sqlName)) {
                return function;
            }
        }
        throw new FxNotFoundException("ex.sqlSearch.function.date", sqlName,
                StringUtils.join(FxSQLFunctions.getSqlNames(Arrays.asList(DateFunction.values())), ", ")).asRuntimeException();
    }
}
