/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
import java.util.Map;

/**
 * A multilingual LargeNumber, internally represented as Long
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxLargeNumber extends FxValue<Long, FxLargeNumber> implements Serializable {

    private static final long serialVersionUID = 3429868264535549630L;
    public final static Long EMPTY = 0L;

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxLargeNumber(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxLargeNumber(boolean multiLanguage, long defaultLanguage, Map<Long, Long> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxLargeNumber(long defaultLanguage, Map<Long, Long> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxLargeNumber(boolean multiLanguage, Map<Long, Long> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxLargeNumber(Map<Long, Long> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxLargeNumber(Map<Long, Long[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxLargeNumber(boolean multiLanguage, long defaultLanguage, Long value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxLargeNumber(long defaultLanguage, Long value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxLargeNumber(boolean multiLanguage, Long value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxLargeNumber(Long value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxLargeNumber(FxValue<Long, FxLargeNumber> clone) {
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
    public Long fromString(String value) {
        return FxFormatUtils.toLong(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPortableStringValue(Long value) {
        return FxValueRendererFactory.getPortableNumberFormatInstance().format(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long fromPortableString(String value) {
        try {
            return FxValueRendererFactory.getPortableNumberFormatInstance().parse(value).longValue();
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.value.error", FxFloat.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * Creates a copy of the given object (useful if the actual type is unknown).
     *
     * @return a copy of the given object (useful if the actual type is unknown).
     */
    @Override
    public FxLargeNumber copy() {
        return new FxLargeNumber(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Long> getValueClass() {
        return Long.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getEmptyValue() {
        return EMPTY;
    }


    @Override
    public int compareTo(FxValue o) {
        if (!(o instanceof FxLargeNumber) || isEmpty() || o.isEmpty()) {
            return super.compareTo(o);
        }
        final Long value = getBestTranslation();
        final Long otherValue = ((FxLargeNumber) o).getBestTranslation();
        return value > otherValue ? 1 : value < otherValue ? -1 : 0;
    }

}
