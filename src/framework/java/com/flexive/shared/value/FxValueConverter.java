/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.shared.value;

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxConversionException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Convert Strings to the data types used in FxValue
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxValueConverter {

    /**
     * Convert a String to Boolean
     *
     * @param value value to convert
     * @return Boolean
     */
    public static Boolean toBoolean(String value) {
        try {
            value = FxFormatUtils.unquote(value);
            return Boolean.parseBoolean(value) || "1".equals(value);
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.error", FxBoolean.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }


    /**
     * Convert a String to Integer
     *
     * @param value value to convert
     * @return Integer
     */
    public static Integer toInteger(String value) {
        try {
            return Integer.parseInt(FxFormatUtils.unquote(value));
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.error", FxNumber.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * Convert a String to Long
     *
     * @param value value to convert
     * @return Long
     */
    public static Long toLong(String value) {
        try {
            return Long.parseLong(FxFormatUtils.unquote(value));
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.error", FxLargeNumber.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * Convert a String to Double
     *
     * @param value value to convert
     * @return Double
     */
    public static Double toDouble(String value) {
        try {
            return Double.parseDouble(FxFormatUtils.unquote(value));
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.error", FxDouble.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * Convert a String to Float
     *
     * @param value value to convert
     * @return Float
     */
    public static Float toFloat(String value) {
        try {
            return Float.parseFloat(FxFormatUtils.unquote(value));
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.error", FxFloat.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * Convert a String to Date
     *
     * @param value value to convert
     * @return Date
     */
    public static Date toDate(String value) {
        try {
            //TODO: use a better date parser
            return FxFormatUtils.getDateFormat().parse(FxFormatUtils.unquote(value));
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.error", FxDate.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * Convert a String to DateTime
     *
     * @param value value to convert
     * @return Date
     */
    public static Date toDateTime(String value) {
        try {
            //TODO: use a better date parser
            return FxFormatUtils.getDateTimeFormat().parse(FxFormatUtils.unquote(value));
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.error", FxDate.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }
}
