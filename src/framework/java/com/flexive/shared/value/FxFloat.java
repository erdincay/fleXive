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

import com.flexive.shared.FxContext;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import org.apache.commons.lang.math.NumberUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * A multilingual Float, internally represented as Float
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxFloat extends FxValue<Float, FxFloat> implements Serializable {
    private static final long serialVersionUID = -5037251646368566073L;

    public final static Float EMPTY = 0.0f;

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxFloat(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxFloat(boolean multiLanguage, long defaultLanguage, Map<Long, Float> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxFloat(long defaultLanguage, Map<Long, Float> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxFloat(boolean multiLanguage, Map<Long, Float> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxFloat(Map<Long, Float> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxFloat(Map<Long, Float[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxFloat(boolean multiLanguage, long defaultLanguage, Float value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxFloat(long defaultLanguage, Float value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxFloat(boolean multiLanguage, Float value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxFloat(Float value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxFloat(FxValue<Float, FxFloat> clone) {
        super(clone);
    }


    /**
     * Evaluates the given string value to an object of type T.
     *
     * @param value string value to be evaluated
     * @return the value interpreted as T
     */
    @Override
    public Float fromString(String value) {
        return FxFormatUtils.toFloat(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPortableStringValue(Float value) {
        return FxValueRendererFactory.getPortableNumberFormatInstance().format(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float fromPortableString(String value) {
        try {
            return FxValueRendererFactory.getPortableNumberFormatInstance().parse(value).floatValue();
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
    public FxFloat copy() {
        return new FxFloat(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Float> getValueClass() {
        return Float.class;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public Float getEmptyValue() {
        return EMPTY;
    }

    @Override
    public int compareTo(FxValue o) {
        if (!(o instanceof FxFloat) || isEmpty() || o.isEmpty()) {
            return super.compareTo(o);
        }
        return NumberUtils.compare(getBestTranslation(), ((FxFloat) o).getBestTranslation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getStringValue(Float value) {
        return value == null ? "" : FxValueRendererFactory.getDefaultFormatter(getClass()).format(this, value, FxContext.getUserTicket().getLanguage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isImmutableValueType() {
        return true;
    }
}
