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
package com.flexive.shared.exceptions;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Localized Exception message handling
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxExceptionMessage implements Serializable {
    private static final long serialVersionUID = -545689173017222846L;
    private static transient Log LOG = LogFactory.getLog(FxExceptionMessage.class);

    /**
     * Default language of a resource if no _locale file is present
     */
    private static final String EXCEPTION_BUNDLE = "FxExceptionMessages";
    private String key;
    private Object[] values;

    /**
     * Ctor
     *
     * @param key    resource key
     * @param values optional values for placeholders in key ({x})
     */
    public FxExceptionMessage(String key, Object... values) {
        this.key = key;
        this.values = values != null ? FxArrayUtils.clone(values) : new Object[0];
    }

    /**
     * Getter for the key
     *
     * @return key resource key
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the localized message for a given language code
     *
     * @param localeId locale id of the desired output
     * @return localized message
     */
    public String getLocalizedMessage(long localeId) {
        FxLanguage locale;
        try {
            locale = EJBLookup.getLanguageEngine().load(localeId);
        } catch (FxApplicationException e) {
            String msg = "[Invalid locale id (" + localeId + ") requested for " + key + "!]";
            LOG.warn(msg);
            return msg;
        }
        return getLocalizedMessage(locale.getId(), locale.getIso2digit());
    }

    /**
     * Get the localized message for given ISO code
     *
     * @param localeIso requested ISO code for desired output
     * @return localized message
     */
    public String getLocalizedMessage(String localeIso) {
        FxLanguage locale;
        try {
            locale = EJBLookup.getLanguageEngine().load(localeIso);
        } catch (FxApplicationException e) {
            String msg = "[Invalid locale code (" + localeIso + ") requested for " + key + "!]";
            LOG.warn(msg);
            return msg;
        }
        return getLocalizedMessage(locale.getId(), locale.getIso2digit());
    }

    /**
     * Get the localized message for a given language
     *
     * @param locale locale of the desired output
     * @return localized message
     */
    public String getLocalizedMessage(FxLanguage locale) {
        return getLocalizedMessage(locale.getId(), locale.getIso2digit());
    }

    /**
     * Get the localized message for a given language code and ISO
     *
     * @param localeId  id of the requested locale
     * @param localeIso ISO code of the requested locale
     * @return localized message
     */
    public String getLocalizedMessage(long localeId, String localeIso) {
        return FxSharedUtils.getLocalizedMessage(EXCEPTION_BUNDLE, localeId, localeIso, key, values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FxExceptionMessage))
            return false;
        FxExceptionMessage o = (FxExceptionMessage) obj;
        return o.getKey().equals(this.getKey()) && ArrayUtils.isEquals(o.values, this.values);
    }

    @Override
    public int hashCode() {
        int result;
        result = key.hashCode();
        result = 31 * result + (values != null ? Arrays.hashCode(values) : 0);
        return result;
    }
}

