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
import com.flexive.shared.FxLanguage;

import java.io.Serializable;
import java.util.Map;

/**
 * A multilingual Boolean, internally represented as Boolean; EMPTY is <code>false</code>
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxBoolean extends FxValue<Boolean, FxBoolean> implements Serializable {
    private static final long serialVersionUID = -1223682858131434249L;

    public final static Boolean EMPTY = Boolean.FALSE;

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     */
    public FxBoolean(long defaultLanguage) {
        super(defaultLanguage, defaultLanguage > FxLanguage.SYSTEM_ID);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxBoolean(boolean multiLanguage, long defaultLanguage, Map<Long, Boolean> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxBoolean(long defaultLanguage, Map<Long, Boolean> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxBoolean(boolean multiLanguage, Map<Long, Boolean> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxBoolean(Map<Long, Boolean> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxBoolean(Map<Long, Boolean[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxBoolean(boolean multiLanguage, long defaultLanguage, Boolean value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxBoolean(long defaultLanguage, Boolean value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxBoolean(boolean multiLanguage, Boolean value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxBoolean(Boolean value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxBoolean(FxValue<Boolean, FxBoolean> clone) {
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
    public Boolean fromString(String value) {
        return FxFormatUtils.toBoolean(value);
    }


    /** {@inheritDoc} */
    @Override
    public String getStringValue(Boolean value) {
        return value.toString();
    }

    /**
     * Creates a copy of the given object (useful if the actual type is unknown).
     *
     * @return a copy of the given object (useful if the actual type is unknown).
     */
    @Override
    public FxBoolean copy() {
        return new FxBoolean(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Boolean> getValueClass() {
        return Boolean.class;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getEmptyValue() {
        return EMPTY;
    }

}
