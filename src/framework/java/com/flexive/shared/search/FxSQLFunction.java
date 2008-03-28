package com.flexive.shared.search;

import com.flexive.shared.structure.FxDataType;

/**
 * A SQL function, e.g. YEAR(prop).
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public interface FxSQLFunction {
    /**
     * Return the FxSQL name of the function, e.g. "year".
     *
     * @return  the FxSQL name of the function, e.g. "year".
     */
    String getSqlName();

    /**
     * Specify the datatype this function returns. If null, the original datatype
     * of the expression should be used.
     *
     * @return  the datatype this function returns
     */
    FxDataType getOverrideDataType();
}
