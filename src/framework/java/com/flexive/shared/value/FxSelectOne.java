/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;

import java.io.Serializable;
import java.util.Map;

/**
 * FxValue implementation for FxSelectList items with one selectable item
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSelectOne extends FxValue<FxSelectListItem, FxSelectOne> implements Serializable {

    private static final long serialVersionUID = 5125426376824746602L;

    final static FxSelectListItem EMPTY = FxSelectListItem.EMPTY;
    private FxSelectList list = null;

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxSelectOne(boolean multiLanguage, long defaultLanguage, Map<Long, FxSelectListItem> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxSelectOne(long defaultLanguage, Map<Long, FxSelectListItem> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxSelectOne(boolean multiLanguage, Map<Long, FxSelectListItem> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxSelectOne(Map<Long, FxSelectListItem> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxSelectOne(Map<Long, FxSelectListItem[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxSelectOne(boolean multiLanguage, long defaultLanguage, FxSelectListItem value) {
        super(multiLanguage, defaultLanguage, value);
        list = value.getList();
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxSelectOne(long defaultLanguage, FxSelectListItem value) {
        super(defaultLanguage, value);
        list = value.getList();
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxSelectOne(boolean multiLanguage, FxSelectListItem value) {
        super(multiLanguage, value);
        list = value.getList();
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxSelectOne(FxSelectListItem value) {
        super(value);
        list = value.getList();
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxSelectOne(FxValue<FxSelectListItem, FxSelectOne> clone) {
        super(clone);
        list = ((FxSelectOne) clone).getSelectList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<FxSelectListItem> getValueClass() {
        return FxSelectListItem.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxSelectListItem fromString(String value) {
        list = null;
        return CacheAdmin.getEnvironment().getSelectListItem(Long.parseLong(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxSelectOne copy() {
        return new FxSelectOne(this);
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
     * Get the SelectList for this SelectOne
     *
     * @return SelectList
     */
    public FxSelectList getSelectList() {
        if (list == null) {
            if (this.isMultiLanguage()) {
                list = this.getBestTranslation().getList();
            } else
                list = this.singleValue.getList();
        }
        return list;
    }
}
