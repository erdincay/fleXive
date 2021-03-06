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
package com.flexive.shared.value.mapper;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.ValueComparator;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.FxLargeNumber;
import com.flexive.shared.value.FxSelectOne;
import com.flexive.shared.value.FxString;

import java.util.List;

/**
 * Maps the given ordinal input type to a select list. The select list must contain at least one element.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SelectOneInputMapper extends InputMapper<FxLargeNumber, FxSelectOne> {
    private static final long serialVersionUID = 5769658905887047422L;

    private final FxSelectList selectList;

    /**
     * Create a new select-one input mapper. The input mapper will select a select item
     * of the given list based on the ordinal value of the value passed
     * to {@link #doEncode(com.flexive.shared.value.FxLargeNumber FxLargeNumber)} and return
     * the appropriate {@link com.flexive.shared.value.FxSelectOne} object. If no valid value is selected,
     * the first select list value will be used.
     * 
     * @param selectList    the select list. If null or empty, a RuntimeException will be trhown.
     */
    public SelectOneInputMapper(FxSelectList selectList) {
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
    protected FxSelectOne doEncode(FxLargeNumber value) {
        if (value.isMultiLanguage()) {
            final FxSelectOne encoded = new FxSelectOne(value.getDefaultLanguage(), getSelectItem(value, value.getDefaultLanguage()));
            for (long languageId : value.getTranslatedLanguages()) {
                encoded.setTranslation(languageId, getSelectItem(value, languageId));
            }
            return applySettings(encoded, value);
        } else {
            return applySettings(new FxSelectOne(false, getSelectItem(value, -1)), value);
        }
    }

    private FxSelectListItem getSelectItem(FxLargeNumber value, long languageId) {
        try {
            return selectList.getItem(languageId != -1 ? value.getTranslation(languageId) : value.getDefaultTranslation());
        } catch (FxRuntimeException e) {
            return (selectList.isSortEntries() ? selectList.getItemsSortedByLabel() : selectList.getItems()).get(0);    // select first item as default
        }
    }

    /** {@inheritDoc} */
    @Override
    protected FxLargeNumber doDecode(FxSelectOne value) {
        if (value.isMultiLanguage()) {
            final FxLargeNumber number = new FxLargeNumber(value.getDefaultLanguage(), value.getDefaultTranslation().getId());
            for (long languageId : value.getTranslatedLanguages()) {
                number.setTranslation(languageId, value.getTranslation(languageId).getId());
            }
            return applySettings(number, value);
        } else {
            return applySettings(
                    new FxLargeNumber(value.isMultiLanguage(), value.getDefaultTranslation().getId()),
                    value
            );
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<? extends ValueComparator> getAvailableValueComparators() {
        return PropertyValueComparator.getAvailable(FxDataType.SelectOne);
    }
}
