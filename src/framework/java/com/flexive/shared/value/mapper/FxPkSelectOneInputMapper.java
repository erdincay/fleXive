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
package com.flexive.shared.value.mapper;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.ValueComparator;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.FxReference;
import com.flexive.shared.value.FxSelectOne;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.ReferencedContent;

import java.util.List;

/**
 * Input mapper for mapping a FxReference property to a select list. The select
 * list item IDs must match the FxPK id values of the select list.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxPkSelectOneInputMapper extends InputMapper<FxReference, FxSelectOne> {
    private static final long serialVersionUID = -8142350562154899086L;
    
    private final FxSelectList selectList;

    /**
     * Create a new select-one input mapper. The input mapper will select a select item
     * of the given list based on the PK value of the value passed
     * to {@link #doEncode(com.flexive.shared.value.FxReference FxReference)} and return
     * the appropriate {@link FxSelectOne} object. If no valid value is selected,
     * the first select list value will be used.
     *
     * @param selectList    the select list. If null or empty, a RuntimeException will be trhown.
     */
    public FxPkSelectOneInputMapper(FxSelectList selectList) {
        if (selectList == null) {
            throw new FxInvalidParameterException("SELECTLIST", "ex.content.value.mapper.select.empty").asRuntimeException();
        } else if (selectList.getItems().isEmpty()) {
            // we need an "empty" element
            new FxSelectListItem(-1, "", selectList, -1, new FxString(""));
        }
        this.selectList = selectList;
    }

    /** {@inheritDoc} */
    @Override
    protected FxSelectOne doEncode(FxReference value) {
        final FxSelectOne select;
        if (value.isMultiLanguage()) {
            select = new FxSelectOne(
                    value.getDefaultLanguage(),
                    createSelectItem(value, value.getDefaultLanguage())
            );
            for (long languageId: value.getTranslatedLanguages()) {
                select.setTranslation(languageId, createSelectItem(value, languageId));
            }
        } else {
            select = new FxSelectOne(false, createSelectItem(value, -1));
        }
        return applySettings(select, value);
    }

    private FxSelectListItem createSelectItem(FxReference value, long languageId) {
        final ReferencedContent translation = languageId != -1
                ? value.getTranslation(languageId) : value.getDefaultTranslation();
        if (translation == null) {
            return defaultSelectItem();
        } else {
            return selectList.containsItem(translation.getId())
                    ? selectList.getItem(translation.getId()) : defaultSelectItem();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected FxReference doDecode(FxSelectOne value) {
        final FxReference reference;
        if (value.isMultiLanguage()) {
            reference = new FxReference(value.getDefaultLanguage(),
                    new ReferencedContent(value.getDefaultTranslation().getId()));
            for (long languageId: value.getTranslatedLanguages()) {
                reference.setTranslation(languageId, new ReferencedContent(value.getTranslation(languageId).getId()));
            }
        } else {
            reference = new FxReference(false, new ReferencedContent(value.getDefaultTranslation().getId()));
        }
        return applySettings(reference, value);
    }

    /** {@inheritDoc} */
    @Override
    public List<? extends ValueComparator> getAvailableValueComparators() {
        return PropertyValueComparator.getAvailable(FxDataType.SelectOne);
    }

    private FxSelectListItem defaultSelectItem() {
        return selectList.getItems().isEmpty()
                    ? new FxSelectListItem(-1, "", selectList, -1, new FxString(""))
                : selectList.getItems().get(0);
    }


}
