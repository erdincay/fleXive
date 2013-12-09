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

import java.io.Serializable;
import java.util.Map;

/**
 * A multilingual Binary
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxBinary extends FxValue<BinaryDescriptor, FxBinary> implements Serializable {
    private static final long serialVersionUID = -4166874760670402680L;

    public final static BinaryDescriptor EMPTY = new BinaryDescriptor();

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxBinary(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxBinary(boolean multiLanguage, long defaultLanguage, Map<Long, BinaryDescriptor> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxBinary(long defaultLanguage, Map<Long, BinaryDescriptor> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxBinary(boolean multiLanguage, Map<Long, BinaryDescriptor> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxBinary(Map<Long, BinaryDescriptor> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxBinary(Map<Long, BinaryDescriptor[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxBinary(boolean multiLanguage, long defaultLanguage, BinaryDescriptor value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxBinary(long defaultLanguage, BinaryDescriptor value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxBinary(boolean multiLanguage, BinaryDescriptor value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxBinary(BinaryDescriptor value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxBinary(FxValue<BinaryDescriptor, FxBinary> clone) {
        super(clone);
    }


    /**
     * Evaluates the given string value to an object of type T.
     *
     * @param value string value to be evaluated
     * @return the value interpreted as T
     */
    @Override
    public BinaryDescriptor fromString(String value) {
        return new BinaryDescriptor(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue(BinaryDescriptor value) {
        return value.getHandle();
    }

    /**
     * Creates a copy of the given object (useful if the actual type is unknown).
     *
     * @return a copy of the given object (useful if the actual type is unknown).
     */
    @Override
    public FxBinary copy() {
        return new FxBinary(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isImmutableValueType() {
        // for practical purposes the binary descriptor is not mutable, since it is not manipulated
        // but only replaced as a whole when then value changes.
        return true;
    }

    /**
     * Replace all occurances of handle with the replacement BinaryDescriptor
     *
     * @param handle      the handle to replace
     * @param replacement the replacement
     */
    public void _replaceHandle(String handle, BinaryDescriptor replacement) {
        if (this.singleValue != null && this.singleValue.getHandle() != null &&
                this.singleValue.getHandle().equals(handle))
            this.singleValue = replacement;
        if (this.translations == null || this.translations.size() == 0)
            return;
        for (long bd_key : translations.keySet()) {
            if (translations.get(bd_key).getHandle() != null && translations.get(bd_key).getHandle().equals(handle))
                translations.put(bd_key, replacement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<BinaryDescriptor> getValueClass() {
        return BinaryDescriptor.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryDescriptor getEmptyValue() {
        return new BinaryDescriptor();
    }
}
