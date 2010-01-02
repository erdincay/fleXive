/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.search.query.ValueComparator;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.value.FxValue;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * InputMapper introduces another level of abstraction for the FxValue input
 * components.
 * <p>When rendering the input component, {@link InputMapper#encode(FxValue)} is
 * called and may wrap the given value in another object. For example, an ordinal
 * {@link com.flexive.shared.value.FxLargeNumber FxLargeNumber} like the ACL ID may be mapped
 * to a {@link com.flexive.shared.value.FxSelectOne FxSelectOne} value for input rendering.
 * </p>
 * <p>
 * When decoding the value submitted from the input form, {@link #decode(com.flexive.shared.value.FxValue)}
 * is called with an instance of the mapped type. In the example given above,
 * the decode method would store the element ID of the
 * {@link com.flexive.shared.value.FxSelectOne FxSelectOne} value in a
 * {@link com.flexive.shared.value.FxLargeNumber FxLargeNumber} instance.
 * </p>
 * <p>
 * Use {@link #getInstance(FxProperty)} to retrieve a new InputMapper for the given
 * structure property, which for example allows to select ACLs with a select list
 * instead of a numeric input field.
 * </p>
 * <p>
 * You may also implement your own input mapper and supply it to the FxValueInput component
 * or attach it to a search query node.
 * </p>
 *
 * @param <BaseType> the source value to be mapped (must extend FxValue).
 * @param <MappedType> the target type of the mapping (must extend FxValue).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class InputMapper<BaseType extends FxValue, MappedType extends FxValue> implements Serializable {
    private String autocompleteHandler;

    /**
     * Map the given value to the destination FxValue type. The resulting object will be
     * used for rendering the input element for the given value.
     *
     * @param value the value to be mapped
     * @return  the mapped type
     */
    public final MappedType encode(BaseType value) {
        final MappedType encodedValue = doEncode(value);
        encodedValue.setXPath(value.getXPath());
        return encodedValue;
    }

    /**
     * Decode the mapped type. Called after the mapped type has been updated in
     * the user form.
     *
     * @param value the mapped value type, possibly modified by the user
     * @return  the corresponding base type
     */
    public final BaseType decode(MappedType value) {
        final BaseType decodedValue = doDecode(value);
        decodedValue.setXPath(value.getXPath());
        return decodedValue;
    }

    /**
     * Map the given value to the destination FxValue type. The resulting object will be
     * used for rendering the input element for the given value.
     *
     * @param value the value to be mapped
     * @return  the mapped type
     */
    protected abstract MappedType doEncode(BaseType value);

    /**
     * Decode the mapped type. Called after the mapped type has been updated in
     * the user form.
     *
     * @param value the mapped value type, possibly modified by the user
     * @return  the corresponding base type
     */
    protected abstract BaseType doDecode(MappedType value);

    /**
     * Return an (optional) autocomplete handler for the input component, for example:<br/>
     * <code>new flexive.yui.AutoCompleteHandler()</code>. 
     *
     * @return  an (optional) autocomplete handler for the input component.
     */
    public String getAutocompleteHandler() {
        return autocompleteHandler;
    }

    /**
     * Returns all available value comparators available in search queries. If the returned
     * list is empty, it is ignored.
     *
     * @return  all available value comparators available in search queries
     */
    public List<? extends ValueComparator> getAvailableValueComparators() {
        return new ArrayList<ValueComparator>(0);
    }

    /**
     * Return a new input mapper instance for the given structure property. For example,
     * this allows to use a select list to choose an ACL, instead of a plain numeric input field.
     *
     * @param property  the property for which values will be mapped
     * @return  an input mapper
     */
    public static InputMapper getInstance(FxProperty property) {
        final String name = property.getName();
        final InputMapper mapper;
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        if ("ACL".equals(name)) {
            mapper = new SelectOneInputMapper(FxSelectList.createList("ACL", environment.getACLs()));
        } else if ("TYPEDEF".equals(name)) {
            mapper = new SelectOneInputMapper(FxSelectList.createList("TYPEDEF", environment.getTypes(true, true, true, false)));
        } else if ("MANDATOR".equals(name)) {
            mapper = new SelectOneInputMapper(FxSelectList.createListWithName("MANDATOR", environment.getMandators(true, false)));
        } else if ("STEP".equals(name)) {
            mapper = new SelectOneInputMapper(FxSelectList.createList("STEP", environment.getStepDefinitions()));
        } else if ("CREATED_BY".equals(name) || "MODIFIED_BY".equals(name)) {
            mapper = new NumberQueryInputMapper.AccountQueryInputMapper();
        } else if (FxDataType.Reference.equals(property.getDataType())) {
            mapper = new NumberQueryInputMapper.ReferenceQueryInputMapper(property);
        } else {
            mapper = IdentityInputMapper.getInstance();
        }
        return mapper;
    }


    /**
     * Build an autocomplete handler for the given JSON/RPC query call.
     *
     * @param jsonRpcQuery  the query method, for example:<br/>
     * <code>AutoCompleteProvider.userQuery</code>
     * @param args additional parameters for the javascript function. The first parameter is always
     * the query string.
     */
    protected final void buildAutocompleteHandler(String jsonRpcQuery, String... args) {
        this.autocompleteHandler = "new flexive.yui.AutoCompleteHandler(function(query) {"
                + "return eval(\"(\" + flexive.util.getJsonRpc()." + jsonRpcQuery + "(query"
                + (args.length > 0 ? ',' + StringUtils.join(args, ',') : "") + ") + \")\");"
                + "})";
    }

    /**
     * Applies general settings from the old value to the new one.
     *
     * @param newValue  the value constructed from oldValue
     * @param oldValue  the original value
     * @param <T>       the value type
     * @return          newValue
     * @since           3.1
     */
    protected <T extends FxValue> T applySettings(T newValue, FxValue oldValue) {
        if (oldValue.isReadOnly()) {
            newValue.setReadOnly();
        }
        return newValue;
    }
}
