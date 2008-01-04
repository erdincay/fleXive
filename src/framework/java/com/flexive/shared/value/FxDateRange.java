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

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxInvalidStateException;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.text.SimpleDateFormat;

/**
 * A multilingual Date range, internally represented as java.util.Date; EMPTY is a Date with a timestamp of <code>0</code> (usually 01/01/1970)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDateRange extends FxValue<DateRange, FxDateRange> implements Serializable {
    private static final long serialVersionUID = 8158482067603102306L;

    public final static DateRange EMPTY = new DateRange(new Date(0), new Date(0));


    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDateRange(boolean multiLanguage, long defaultLanguage, Map<Long, DateRange> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDateRange(long defaultLanguage, Map<Long, DateRange> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxDateRange(boolean multiLanguage, Map<Long, DateRange> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxDateRange(Map<Long, DateRange> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxDateRange(Map<Long, DateRange[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDateRange(boolean multiLanguage, long defaultLanguage, DateRange value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDateRange(long defaultLanguage, DateRange value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxDateRange(boolean multiLanguage, DateRange value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxDateRange(DateRange value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxDateRange(FxValue<DateRange, FxDateRange> clone) {
        super(clone);
    }

    /**
     * Return true if T is immutable (e.g. java.lang.String). This prevents cloning
     * of the translations in copy constructors.
     *
     * @return true if T is immutable (e.g. java.lang.String)
     */
    @Override
    public boolean isImmutableValueType() {
        return true;
    }


    /**
     * Evaluates the given string value to an object of type T.
     *
     * @param value string value to be evaluated
     * @return the value interpreted as T
     */
    @Override
    public DateRange fromString(String value) {
        return new DateRange(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue(DateRange value) {
        return value.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSqlValue() {
        throw new FxInvalidStateException("ex.content.value.sql.notSupported", getClass().getSimpleName()).asRuntimeException();
    }

    /**
     * Creates a copy of the given object (useful if the actual type is unknown).
     *
     * @return a copy of the given object (useful if the actual type is unknown).
     */
    @Override
    public FxDateRange copy() {
        return new FxDateRange(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<DateRange> getValueClass() {
        return DateRange.class;
	}
}
