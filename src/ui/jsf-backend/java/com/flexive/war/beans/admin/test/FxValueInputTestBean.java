/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.beans.admin.test;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultRow;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.*;
import com.flexive.shared.value.mapper.FxPkSelectOneInputMapper;
import com.flexive.shared.value.mapper.InputMapper;
import com.flexive.shared.value.mapper.NumberQueryInputMapper;
import com.flexive.shared.value.mapper.SelectOneInputMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.flexive.shared.FxLanguage.ENGLISH;
import static com.flexive.shared.FxLanguage.GERMAN;

/**
 * Miscellanous test methods for the fxValueInput test page (/adm/test/fxvalueinput.xhtml).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxValueInputTestBean implements Serializable {
    private static final long serialVersionUID = -4832646489836422606L;

    /**
     * ui:repeat update workaround
     */
    public static class ValueHolder {
        private FxValue value;

        public ValueHolder(FxValue value) {
            this.value = value;
        }

        public FxValue getValue() {
            return value;
        }

        public void setValue(FxValue value) {
            this.value = value;
        }
    }

    private FxValue stringValue = new FxString(false, "");
    private FxString userStringValue = new FxString(false, "");
    private FxReference singleMappedValue = new FxReference(false, new ReferencedContent(-1)).setXPath("searchtest/referenceSearchProp").setEmpty();
    private FxReference singleMappedValue2 = new FxReference(false, new ReferencedContent(-1)).setXPath("searchtest/referenceSearchProp").setEmpty();
    private FxReference multiMappedValue = new FxReference(true, new ReferencedContent(-1)).setXPath("searchtest/referenceSearchProp").setEmpty();
    private FxReference multiMappedValue2 = new FxReference(true, new ReferencedContent(-1)).setXPath("searchtest/referenceSearchProp").setEmpty();
    private FxLargeNumber singleSelectValue = new FxLargeNumber(false, -1L).setEmpty();
    private FxLargeNumber multiSelectValue = new FxLargeNumber(true, -1L).setEmpty();
    private FxString valueNoPrepend = new FxString(false, "");
    private List<ValueHolder> basicValues;
    private List<FxValue> basicValues2;
    private InputMapper referenceInputMapper;

    public void submit() {
        // TODO: evaluate input values
    }

    public List<ValueHolder> getBasicValues() {
        if (basicValues == null) {
            final List<FxValue> values = createFxValues();
            basicValues = new ArrayList<ValueHolder>(values.size());
            for (FxValue value : values) {
                basicValues.add(new ValueHolder(value));
            }
        }
        return basicValues;
    }

    public List<FxValue> getBasicValues2() {
        if (basicValues2 == null) {
            basicValues2 = createFxValues();
        }
        return basicValues2;
    }

    @SuppressWarnings({"RedundantArrayCreation"})
    private List<FxValue> createFxValues() {
        final FxSelectList countriesList = CacheAdmin.getEnvironment().getSelectList(FxSelectList.COUNTRIES);
        final List<FxSelectListItem> countries = countriesList.getItems();
        final DateRange range = new DateRange(new Date(), new Date());
        final DateRange range2 = new DateRange(new Date(0), new Date(1000000));
        return Arrays.asList(new FxValue[]{
                new FxString(false, "string singlelanguage"),
                new FxString(ENGLISH, "english").setTranslation(GERMAN, "deutsch"),
                new FxBoolean(false, false),
                new FxBoolean(ENGLISH, true).setTranslation(GERMAN, false),
                new FxNumber(false, 21),
                new FxNumber(ENGLISH, 1).setTranslation(GERMAN, 2),
                new FxLargeNumber(false, 22L),
                new FxLargeNumber(ENGLISH, 3L).setTranslation(GERMAN, 4L),
                new FxFloat(false, 3.14f),
                new FxFloat(ENGLISH, 3.14f).setTranslation(GERMAN, 3.15f),
                new FxDouble(false, Math.PI),
                new FxDouble(ENGLISH, Math.PI).setTranslation(GERMAN, Math.PI - 0.1),
                new FxDate(false, new Date()),
                new FxDate(ENGLISH, new Date()).setTranslation(GERMAN, new Date(new Date().getTime() - 24*3600*1000)),
                new FxDateTime(false, new Date()),
                new FxDateTime(ENGLISH, new Date()).setTranslation(GERMAN, new Date(new Date().getTime() - 24*3600*1000)),
                new FxDateRange(false, range.copy()),
                new FxDateRange(ENGLISH, range.copy()).setTranslation(GERMAN, range2.copy()),
                new FxDateTimeRange(false, range.copy()),
                new FxDateTimeRange(ENGLISH, range.copy()).setTranslation(GERMAN, range2.copy()),
                new FxHTML(false, "HTML value"),
                new FxHTML(ENGLISH, "english HTML").setTranslation(GERMAN, "deutsches HTML"),
                new FxSelectOne(false, countries.get(0)),
                new FxSelectOne(ENGLISH, countries.get(1)).setTranslation(GERMAN, countries.get(2)),
                new FxSelectMany(false, new SelectMany(countriesList)),
                new FxSelectMany(ENGLISH, new SelectMany(countriesList)).setTranslation(GERMAN, new SelectMany(countriesList)),
        });
    }

    public FxValue getStringValue() {
        return stringValue;
    }

    public void setStringValue(FxValue stringValue) {
        this.stringValue = stringValue;
    }

    public FxString getUserStringValue() {
        return userStringValue;
    }

    public void setUserStringValue(FxString userStringValue) {
        this.userStringValue = userStringValue;
    }

    public FxReference getSingleMappedValue() {
        return singleMappedValue;
    }

    public void setSingleMappedValue(FxReference singleMappedValue) {
        this.singleMappedValue = singleMappedValue;
    }

    public FxReference getMultiMappedValue() {
        return multiMappedValue;
    }

    public void setMultiMappedValue(FxReference multiMappedValue) {
        this.multiMappedValue = multiMappedValue;
    }

    public InputMapper getReferenceInputMapper() throws FxApplicationException {
        if (referenceInputMapper == null) {
            final FxResultSet result = new SqlQueryBuilder().select("@pk", "caption").maxRows(30).getResult();
            final FxSelectList list = new FxSelectList("");
            for (FxResultRow row : result.getResultRows()) {
                new FxSelectListItem(row.getPk(1).getId(), row.getString(2), list, -1, (FxString) row.getFxValue(2));
            }
            referenceInputMapper = new FxPkSelectOneInputMapper(list);
        }
        return referenceInputMapper;
    }

    public InputMapper getNumberQueryInputMapper() {
        return new NumberQueryInputMapper.ReferenceQueryInputMapper(
                CacheAdmin.getEnvironment().getProperty("referenceSearchProp")
        );
    }

    public FxReference getSingleMappedValue2() {
        return singleMappedValue2;
    }

    public void setSingleMappedValue2(FxReference singleMappedValue2) {
        this.singleMappedValue2 = singleMappedValue2;
    }

    public FxReference getMultiMappedValue2() {
        return multiMappedValue2;
    }

    public void setMultiMappedValue2(FxReference multiMappedValue2) {
        this.multiMappedValue2 = multiMappedValue2;
    }

    public FxLargeNumber getSingleSelectValue() {
        return singleSelectValue;
    }

    public void setSingleSelectValue(FxLargeNumber singleSelectValue) {
        this.singleSelectValue = singleSelectValue;
    }

    public FxLargeNumber getMultiSelectValue() {
        return multiSelectValue;
    }

    public void setMultiSelectValue(FxLargeNumber multiSelectValue) {
        this.multiSelectValue = multiSelectValue;
    }

    public InputMapper getSelectOneInputMapper() {
        return new SelectOneInputMapper(CacheAdmin.getEnvironment().getSelectList(FxSelectList.COUNTRIES));
    }

    public FxString getValueNoPrepend() {
        return valueNoPrepend;
    }

    public void setValueNoPrepend(FxString valueNoPrepend) {
        this.valueNoPrepend = valueNoPrepend;
    }
}
