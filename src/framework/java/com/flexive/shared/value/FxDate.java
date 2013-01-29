/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.value.renderer.FxValueRendererFactory;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * A multilingual Date, internally represented as java.util.Date; EMPTY is a Date with a timestamp of <code>0</code> (usually 01/01/1970)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDate extends FxValue<Date, FxDate> implements Serializable {
    private static final long serialVersionUID = 6502991989171392125L;

    public final static Date EMPTY = new Date(0);

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxDate(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDate(boolean multiLanguage, long defaultLanguage, Map<Long, Date> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDate(long defaultLanguage, Map<Long, Date> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxDate(boolean multiLanguage, Map<Long, Date> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxDate(Map<Long, Date> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxDate(Map<Long, Date[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDate(boolean multiLanguage, long defaultLanguage, Date value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDate(long defaultLanguage, Date value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxDate(boolean multiLanguage, Date value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxDate(Date value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxDate(FxValue<Date, FxDate> clone) {
        super(clone);
    }


    /**
     * Evaluates the given string value to an object of type T.
     *
     * @param value string value to be evaluated
     * @return the value interpreted as T
     */
    @Override
    public Date fromString(String value) {
        return FxFormatUtils.toDate(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue(Date value) {
        return FxValueRendererFactory.getDateFormat().format(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPortableStringValue(Date value) {
        return FxFormatUtils.getUniversalDateTimeFormat().format(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date fromPortableString(String value) {
        try {
            return FxFormatUtils.getUniversalDateTimeFormat().parse(value);
        } catch (ParseException e) {
            throw new FxConversionException(e, "ex.conversion.value.error", FxFloat.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public FxDate copy() {
        return new FxDate(this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Date copyValue(Date value) {
        return new Date(value.getTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Date> getValueClass() {
        return Date.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getEmptyValue() {
        return (Date) EMPTY.clone();
    }

}
