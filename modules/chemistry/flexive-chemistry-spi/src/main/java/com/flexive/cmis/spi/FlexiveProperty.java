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
package com.flexive.cmis.spi;

import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.value.FxValue;
import org.apache.chemistry.Property;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * An editable property instance of a flexive FxContent instance.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveProperty implements Property {
    private final List<FxValue> values;
    private final FlexivePropertyDefinition definition;

    /**
     * Create a new property. The values will be taken from the given content instance.
     *
     * @param content       the content instance
     * @param definition    the property definition (wrapping the property assignment)
     * @param xpath         the property XPath
     */
/*
    public FlexiveProperty(FxContent content, FlexivePropertyDefinition definition, String xpath) {
        this.definition = definition;
        if (definition.getAssignment().getMultiplicity().getMax() > 1) {
            this.values = content.getValues(xpath);
        } else {
            values = Lists.newArrayList();
            final FxValue value = content.getValue(xpath);
            if (value != null) {
                values.add(value);
            }
        }
        if (this.values.isEmpty()) {
            values.add(definition.getAssignment().getEmptyValue());
        }
    }
*/

    /**
     * Create a new property.
     *
     * @param values            the property values
     * @param baseAssignment    the base assignment ID
     */
    public FlexiveProperty(List<FxValue> values, FxPropertyAssignment baseAssignment) {
        this.values = newArrayList(values);
        if (this.values.isEmpty()) {
            values.add(baseAssignment.getEmptyValue());
        }
        this.definition = new FlexivePropertyDefinition(baseAssignment);
    }

    public FlexivePropertyDefinition getDefinition() {
        return definition;
    }

    public Serializable getValue() {
        if (definition.isMultiValued()) {
            // return correctly typed array, not collection (requirement by Chemistry impl)
            final Object[] result = (Object[]) Array.newInstance(
                    values.get(0).getBestTranslation().getClass(),
                    values.size()
            );
            int index = 0;
            for (FxValue value : values) {
                result[index++] = SPIUtils.convertValue(value, definition);
            }
            return result;
        } else if (values.isEmpty()) {
            return null;
        } else {
            return SPIUtils.convertValue(values.get(0), definition);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void setValue(Serializable value) {
        if (definition.isMultiValued()) {
            throw new UnsupportedOperationException("Setting multiple values is not supported yet");
        }
        this.values.get(0).setValue(value);
    }
}
