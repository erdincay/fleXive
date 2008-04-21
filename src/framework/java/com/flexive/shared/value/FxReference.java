/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

import com.flexive.shared.content.FxPK;

import java.io.Serializable;
import java.util.Map;

/**
 * A multilingual content reference, internally represented as FxPK
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see FxPK
 */
public class FxReference extends FxValue<ReferencedContent, FxReference> implements Serializable {

    private static final long serialVersionUID = 6559690796573041346L;
    public final static ReferencedContent EMPTY = new ReferencedContent(new FxPK());

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxReference(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxReference(boolean multiLanguage, long defaultLanguage, Map<Long, ReferencedContent> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxReference(long defaultLanguage, Map<Long, ReferencedContent> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxReference(boolean multiLanguage, Map<Long, ReferencedContent> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxReference(Map<Long, ReferencedContent> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxReference(Map<Long, ReferencedContent[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxReference(boolean multiLanguage, long defaultLanguage, ReferencedContent value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxReference(long defaultLanguage, ReferencedContent value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxReference(boolean multiLanguage, ReferencedContent value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxReference(ReferencedContent value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxReference(FxValue<ReferencedContent, FxReference> clone) {
        super(clone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ReferencedContent> getValueClass() {
        return ReferencedContent.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferencedContent fromString(String value) {
        return ReferencedContent.fromString(value);
    }

    /** {@inheritDoc} */
    @Override
    public String getStringValue(ReferencedContent value) {
        return value.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxReference copy() {
        return new FxReference(this);
    }

    @Override
    public int compareTo(FxValue o) {
        if (isEmpty() || o.isEmpty() || !(o instanceof FxReference)) {
            return super.compareTo(o);
        }
        return getBestTranslation().compareTo(((FxReference) o).getBestTranslation());
    }
}
