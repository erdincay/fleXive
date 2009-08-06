/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
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
package com.flexive.shared.cmis.search;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A boxed column value in a CMIS result set. You can access the boxed value with
 * {@link #getValue()}, the {@link #equals(Object)} and {@link #toString()} functions will be
 * forwarded to the boxed instance (unless it is null).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public abstract class CmisResultValue<T> implements Serializable {
    private final T value;

    static CmisResultValue createResultValue(Object value) {
        if (value == null) {
            return new NullResult();
        } else if (value instanceof FxValue) {
            return new FxValueResult((FxValue) value);
        } else if (value instanceof String) {
            return new StringResult((String) value);
        } else if (value instanceof List) {
            return createListValue(value);
        } else {
            return new PrimitiveResult(value);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static CmisResultValue createListValue(Object value) {
        final List list = (List) value;
        return list.isEmpty() ? new NullResult() : new MultivaluedResult(list);
    }

    protected CmisResultValue(T value) {
        this.value = value;
    }

    public boolean isAggregate() {
        return false;
    }

    public T getValue() {
        return value;
    }

    public List<T> getValues() {
        return Arrays.asList(value);
    }

    /**
     * Return "true" if this value is considered to be empty because the database did not return a valiue,
     * e.g. a null value.
     * @return  "true" if this value is considered to be empty
     */
    public boolean isEmpty() {
        return value != null;
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "";
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(Object obj) {
        return (value == null && obj == null) ||
                (value != null && (value.equals(obj)
                        || (obj instanceof CmisResultValue && value.equals(((CmisResultValue) obj).getValue()))));
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    /**
     * Return the value as a {@link Byte}. If {@link #isEmpty()} is true, a
     * {@link com.flexive.shared.exceptions.FxRuntimeException} is thrown.
     *
     * @return  the value as a {@link Byte}.
     */
    public byte getByte() {
        return getNumberValue().byteValue();
    }

    /**
     * Return the value as a {@link Integer}. If {@link #isEmpty()} is true, a
     * {@link com.flexive.shared.exceptions.FxRuntimeException} is thrown.
     *
     * @return  the value as a {@link Integer}.
     */
    public int getInt() {
        return getNumberValue().intValue();
    }

    /**
     * Return the value as a {@link Long}. If {@link #isEmpty()} is true, a
     * {@link com.flexive.shared.exceptions.FxRuntimeException} is thrown.
     *
     * @return  the value as a {@link Long}.
     */
    public long getLong() {
        return getNumberValue().longValue();
    }

    /**
     * Return the value as a {@link Float}. If {@link #isEmpty()} is true, a
     * {@link com.flexive.shared.exceptions.FxRuntimeException} is thrown.
     *
     * @return  the value as a {@link Float}.
     */
    public float getFloat() {
        return getNumberValue().floatValue();
    }

    /**
     * Return the value as a {@link Double}. If {@link #isEmpty()} is true, a
     * {@link com.flexive.shared.exceptions.FxRuntimeException} is thrown.
     *
     * @return  the value as a {@link Double}.
     */
    public double getDouble() {
        return getNumberValue().doubleValue();
    }

    /**
     * Return the value as a {@link String}. If the boxed value is not a {@link String}, the
     * result of the {@link #toString()} method is returned.
     *
     * @return  the value as a {@link String}.
     */
    public String getString() {
        return toString();
    }

    /**
     * Return the {@link BinaryDescriptor} of a binary column.
     *
     * @return  the {@link BinaryDescriptor} of a binary column.
     */
    public BinaryDescriptor getBinary() {
        return (value instanceof FxValue)
                ? ((FxBinary) value).getBestTranslation()
                : (BinaryDescriptor) value;
    }

    /**
     * Return the value as a FxValue. Note that CMIS queries return primitive values by default, in this
     * case this method will throw a ClassCastException.
     *
     * @return the value as a FxValue
     */
    public FxValue getFxValue() {
        return (FxValue) value;
    }

    /**
     * Helper method for the getXXX methods that returns the boxed value as a {@link Number}, or throws
     * a runtime exception if the boxed value is not a Number.
     *
     * @return  the boxed value as a number
     */
    protected Number getNumberValue() {
        if (value instanceof Number) {
            return (Number) value;
        } else {
            throw new FxInvalidParameterException("value", "ex.cmis.search.resultset.column.value.number", value)
                    .asRuntimeException();
        }
    }

    private static class NullResult extends CmisResultValue<Object> {
        private static final long serialVersionUID = -8644499861775780439L;

        private NullResult() {
            super(null);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NullResult;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class FxValueResult extends CmisResultValue<FxValue> {
        private static final long serialVersionUID = 2931932900356665485L;

        private FxValueResult(FxValue value) {
            super(value);
        }

        @Override
        protected Number getNumberValue() {
            if (getValue().getBestTranslation() instanceof Number) {
                return (Number) getValue().getBestTranslation();
            } else {
                return super.getNumberValue();
            }
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
        @Override
        public boolean equals(Object obj) {
            return (obj == null || obj instanceof FxValue)
                    // match full FxValue object
                    ? super.equals(obj)
                    // unbox FxValue, since argument is no FxValue. Since our result values have only
                    // one language set, this does not change the meaning of equals, but saves the calling
                    // from creating FxValue objects
                    : obj.equals(getValue().getBestTranslation());
        }

        @Override
        public int hashCode() {
            return super.hashCode();    // will be forwarded to FxValue instance
        }

        @Override
        public boolean isEmpty() {
            return getValue().isEmpty();
        }
    }

    private static class StringResult extends CmisResultValue<String> {
        private static final long serialVersionUID = -7269388311553435220L;

        private StringResult(String value) {
            super(value);
        }

        @Override
        public boolean isEmpty() {
            return StringUtils.isEmpty(getValue());
        }
    }

    private static class PrimitiveResult extends CmisResultValue<Object> {
        private static final long serialVersionUID = 9022502355885954860L;

        private PrimitiveResult(Object value) {
            super(value);
        }
    }

    private static class MultivaluedResult<T> extends CmisResultValue<T> {
        private static final long serialVersionUID = 354333942633247097L;
        private final List<T> values;

        private MultivaluedResult(List<T> list) {
            super(list != null && !list.isEmpty() ? list.get(0) : null);
            this.values = Collections.unmodifiableList(list);
        }

        @Override
        public boolean isAggregate() {
            return true;
        }

        @Override
        public List<T> getValues() {
            return values;
        }

        @Override
        public boolean isEmpty() {
            return values == null || values.isEmpty();
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }
}
