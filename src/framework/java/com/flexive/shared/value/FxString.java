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

import com.flexive.shared.FxLanguage;

import java.io.Serializable;
import java.util.Map;

/**
 * A multilingual String
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxString extends FxValue<String, FxString> implements Serializable {

    private static final long serialVersionUID = -1441854978719391696L;
    public final static String EMPTY = "";

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxString(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    public FxString(boolean multiLanguage, Map<Long, String> translations) {
        super(multiLanguage, translations);
    }

    public FxString(boolean multiLanguage, long defaultLanguage, Map<Long, String> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    public FxString(boolean multiLanguage, long defaultLanguage, String value) {
        super(multiLanguage, defaultLanguage, value);
    }

    public FxString(boolean multiLanguage, String value) {
        super(multiLanguage, value);
    }

    public FxString(Map<Long, String[]> translations, int pos) {
        super(translations, pos);
    }

    public FxString(FxString clone) {
        super(clone);
    }

    public FxString(Map<Long, String> translations) {
        super(translations);
    }

    public FxString(long defaultLanguage, Map<Long, String> translations) {
        super((defaultLanguage != FxLanguage.SYSTEM_ID), defaultLanguage, translations);
    }

    public FxString(long defaultLanguage, String value) {
        super(defaultLanguage, value);
    }

    public FxString(String value) {
        super(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxString copy() {
        return new FxString(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String fromString(String value) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isImmutableValueType() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<String> getValueClass() {
        return String.class;
	}
}
