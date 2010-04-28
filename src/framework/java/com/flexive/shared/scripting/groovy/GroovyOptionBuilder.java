/** *************************************************************
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
 ************************************************************** */
package com.flexive.shared.scripting.groovy;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.structure.FxStructureOption;
import groovy.util.BuilderSupport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The GroovyOptionBuilder for FxStructureOptions provides a quick way to generate a List of FxStructureOptions<br />
 * The Builder always returns a List&lt;FxStructureOption&gt;<br /><br />
 * General usage:<br /><br />
 * A list w/ one option:<br />
 * <pre>
 * new GroovyOptionBuilder().[OPTNAME](value: STRING/BOOLEAN, overridable:BOOLEAN, isInherited:BOOLEAN)<br />
 * </pre>
 * A list w/ several options:<br />
 * <pre>
 * new GroovyOptionBuilder().[OPTNAME](value: STRING/BOOLEAN, overridable:BOOLEAN, isInherited:BOOLEAN) {<br />
 *      [OPTNAME]([attributes]
 *      [OPTNAME]([attributes]
 *      ...
 * }
 * </pre><br /><br />
 * The attributes &quot;overridable&quot; and &quot;isInherited&quot; will default to &quot;true&quot; if not called explicitly
 * <br /><br />
 * Example:<br />
 * <pre>
 * new GroovyOptionBuilder().option1(value:"FOO") {
 *      option2(value: false)
 *      option3(value: "BAR", overridable: false, isInherited:true)
 *      option4(value: true, overridable: true, isInherited:true)
 * }
 * </pre>
 *
 * @author Christopher Blasnik (cblasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 *
 * @since 3.1
 */
public class GroovyOptionBuilder extends BuilderSupport implements Serializable {
    private static final long serialVersionUID = 812333306936605973L;

    private List<FxStructureOption> optionList;

    public GroovyOptionBuilder() {
        optionList = new ArrayList<FxStructureOption>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setParent(Object o, Object o1) {
        // unused
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object o) {
        return optionList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object o, Object o1) {
        return optionList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object o, Map map) {
        boolean overridable, isInherited;
        Object value = getMapValue(map, "value", true, "");
        if(value instanceof Boolean) {
            value = (Boolean)value ? FxStructureOption.VALUE_TRUE : FxStructureOption.VALUE_FALSE;
        }

        if (o instanceof FxStructureOption) {
            FxStructureOption inOption = (FxStructureOption) o;
            overridable = (Boolean) getMapValue(map, "overridable", false, inOption.isOverridable());
            isInherited = (Boolean) getMapValue(map, "isInherited", false, inOption.getIsInherited());
            optionList.add(new FxStructureOption(inOption.getKey(), overridable, true, isInherited, (String)value));
        } else if (o instanceof String) {
            overridable = (Boolean) getMapValue(map, "overridable", false, true);
            isInherited = (Boolean) getMapValue(map, "isInherited", false, true);
            optionList.add(new FxStructureOption(o.toString().toUpperCase(), overridable, true, isInherited, (String)value));
        }
        return optionList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object o, Map map, Object o1) {
        return optionList;
    }

    /**
     * Retrieve a (required) value from the given map or return a defaultValue
     *
     * @param map the input Map
     * @param key the key whose value we want to retrieve
     * @param required flag to true of the given key is required in the map
     * @param defaultValue the default value to return (if !required and not found in map)
     * @return the map value for the given key as an Object
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Object getMapValue(Map map, String key, boolean required, Object defaultValue) {
        if (!map.containsKey(key)) {
            if (!required)
                return defaultValue;
            throw new FxInvalidParameterException(key, "ex.scripting.optBuilder.required.val.missing", key).asRuntimeException();

        }
        return map.get(key);
    }
}
