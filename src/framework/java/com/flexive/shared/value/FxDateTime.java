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
package com.flexive.shared.value;

import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.FxFormatUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * A multilingual DateTime, internally represented as java.util.Date; EMPTY is a Date with a timestamp of <code>0</code> (usually 01/01/1970)
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDateTime extends FxValue<Date, FxDateTime> implements Serializable {
    private static final long serialVersionUID = -6368191431091556952L;
    public final static Date EMPTY = new Date(0);

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxDateTime(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDateTime(boolean multiLanguage, long defaultLanguage, Map<Long, Date> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDateTime(long defaultLanguage, Map<Long, Date> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxDateTime(boolean multiLanguage, Map<Long, Date> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxDateTime(Map<Long, Date> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxDateTime(Map<Long, Date[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDateTime(boolean multiLanguage, long defaultLanguage, Date value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDateTime(long defaultLanguage, Date value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxDateTime(boolean multiLanguage, Date value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxDateTime(Date value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxDateTime(FxValue<Date, FxDateTime> clone) {
        super(clone);
    }

    /** {@inheritDoc} */
    @Override
    public Class<Date> getValueClass() {
        return Date.class;
    }

    /** {@inheritDoc} */
    @Override
    public Date fromString(String value) {
        return FxFormatUtils.toDateTime(value);
    }

    /** {@inheritDoc} */
    @Override
    public FxDateTime copy() {
        return new FxDateTime(this);
    }

    /** {@inheritDoc} */
    @Override
    public String getStringValue(Date value) {
        //TODO: use a better date parser
        return new SimpleDateFormat(FxFormatUtils.UNIVERSAL_TIMEFORMAT).format(value);
//        return DateFormat.getDateInstance().format(value);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isImmutableValueType() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getSqlValue() {
        if (isEmpty()) {
            throw new FxInvalidStateException("ex.content.value.sql.empty").asRuntimeException();
        }
        return "'" + FxFormatUtils.getDateTimeFormat().format(getDefaultTranslation()) + "'";
    }
}
