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
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * FxValue implementation for FxSelectList items with many selectable items
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSelectMany extends FxValue<SelectMany, FxSelectMany> implements Serializable {

    private static final long serialVersionUID = -5974340863508061455L;
    private FxSelectList list = null;

    /**
     * Protected Ctor used for "re-constructing" from XML
     */
    protected FxSelectMany() {
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxSelectMany(boolean multiLanguage, long defaultLanguage, Map<Long, SelectMany> translations) {
        super(multiLanguage, defaultLanguage, translations);
        checkForEmptyTranslations(translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxSelectMany(long defaultLanguage, Map<Long, SelectMany> translations) {
        super(defaultLanguage, translations);
        checkForEmptyTranslations(translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxSelectMany(boolean multiLanguage, Map<Long, SelectMany> translations) {
        super(multiLanguage, translations);
        checkForEmptyTranslations(translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxSelectMany(Map<Long, SelectMany> translations) {
        super(translations);
        checkForEmptyTranslations(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxSelectMany(Map<Long, SelectMany[]> translations, int pos) {
        super(translations, pos);
        FxSharedUtils.checkParameterEmpty(translations, "translations");
        for (SelectMany[] translation: translations.values()) {
            FxSharedUtils.checkParameterEmpty(translation[pos], "translation");
        }
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxSelectMany(boolean multiLanguage, long defaultLanguage, SelectMany value) {
        super(multiLanguage, defaultLanguage, value);
        FxSharedUtils.checkParameterEmpty(value, "value");
        this.list = value.getSelectList();
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxSelectMany(long defaultLanguage, SelectMany value) {
        super(defaultLanguage, value);
        FxSharedUtils.checkParameterEmpty(value, "value");
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxSelectMany(boolean multiLanguage, SelectMany value) {
        super(multiLanguage, value);
        FxSharedUtils.checkParameterEmpty(value, "value");
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxSelectMany(SelectMany value) {
        super(value);
        FxSharedUtils.checkParameterEmpty(value, "value");
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxSelectMany(FxValue<SelectMany, FxSelectMany> clone) {
        super(clone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<SelectMany> getValueClass() {
        return SelectMany.class;
    }

    /** {@inheritDoc} */
    @Override
    public String getStringValue(SelectMany value) {
        StringBuilder out = new StringBuilder();
        for (FxSelectListItem item: value.getSelected()) {
            out.append(out.length() > 0 ? "," : "").append(item.getId());
        }
        return out.toString();
    }

    /**
     * Evaluates the given string value to an object of type SelectMany.
     *
     * @param value comma seperated list of selected entries
     * @return the value interpreted as SelectMany
     */
    @Override
    public SelectMany fromString(String value) {
        if (StringUtils.isEmpty(value))
            throw new FxInvalidParameterException("value", "ex.content.value.invalid.list").asRuntimeException();
        List<FxSelectListItem> items = new ArrayList<FxSelectListItem>(10);
        Scanner sc = new Scanner(value).useDelimiter(",");
        while (sc.hasNextLong()) {
            items.add(CacheAdmin.getEnvironment().getSelectListItem(sc.nextLong()));
        }
        if (items.size() == 0)
            throw new FxInvalidParameterException("value", "ex.content.value.invalid.list").asRuntimeException();
        SelectMany sel = new SelectMany(items.get(0).getList());
        for (FxSelectListItem item : items)
            sel.selectItem(item);
        return sel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxSelectMany copy() {
        return new FxSelectMany(this);
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
     * Get the SelectList for this SelectMany
     *
     * @return SelectList
     */
    public FxSelectList getSelectList() {
        if (list == null) {
            if (this.isMultiLanguage()) {
                list = this.getBestTranslation().getSelectList();
            } else
                list = this.singleValue.getSelectList();
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isAcceptsEmptyDefaultTranslations() {
        return false;
    }

    private void checkForEmptyTranslations(Map<Long, SelectMany> translations) {
        FxSharedUtils.checkParameterEmpty(translations, "translations");
        for (SelectMany translation: translations.values()) {
            FxSharedUtils.checkParameterEmpty(translation, "translation");
        }
    }


}

