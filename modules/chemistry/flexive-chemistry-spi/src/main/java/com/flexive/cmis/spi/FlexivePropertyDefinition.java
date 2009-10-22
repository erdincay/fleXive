/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
import com.flexive.shared.structure.FxStructureOption;
import com.flexive.shared.XPathElement;
import org.apache.chemistry.Choice;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.PropertyType;
import org.apache.chemistry.Updatability;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexivePropertyDefinition implements PropertyDefinition {
    private final FxPropertyAssignment assignment;
    private final String name;
    private final PropertyType overrideType;

    public FlexivePropertyDefinition(FxPropertyAssignment assignment) {
        this.assignment = assignment;
        this.name = XPathElement.stripType(assignment.getXPath()).substring(1).toLowerCase();
        this.overrideType = null;
    }

    public FlexivePropertyDefinition(FxPropertyAssignment assignment, String overrideName, PropertyType overrideType) {
        this.assignment = assignment;
        this.name = overrideName;
        this.overrideType = overrideType;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return getName();
    }

    public String getDisplayName() {
        return assignment.getDisplayName();
    }

    public String getDescription() {
        return "";
    }

    public boolean isInherited() {
        return assignment.isDerivedAssignment();
    }

    public PropertyType getType() {
        if (overrideType != null) {
            return overrideType;
        }
        return SPIUtils.mapPropertyType(assignment.getProperty());
    }

    public boolean isMultiValued() {
        return assignment.getMultiplicity().getMax() > 1;
    }

    public List<Choice> getChoices() {
        throw new UnsupportedOperationException();
    }

    public boolean isOpenChoice() {
        throw new UnsupportedOperationException();
    }

    public boolean isRequired() {
        return assignment.getMultiplicity().getMin() > 0;
    }

    public Serializable getDefaultValue() {
        return assignment.getDefaultValue() != null
                ? assignment.getDefaultValue().getBestTranslation().toString()
                : null;
    }

    public Updatability getUpdatability() {
        return Updatability.READ_WRITE; // TODO: check user privileges
    }

    public boolean isQueryable() {
        return assignment.getOption(FxStructureOption.OPTION_SEARCHABLE).isValueTrue();
    }

    public boolean isOrderable() {
        return true;
    }

    public int getPrecision() {
        switch (assignment.getProperty().getDataType()) {
            case Float:
            case Number:
                return 32;
            case Double:
            case LargeNumber:
                return 64;
            default:
                throw new IllegalArgumentException("Datatype is not a number: " + assignment.getProperty().getDataType());
        }
    }

    public Integer getMinValue() {
        return assignment.getMultiplicity().getMin();
    }

    public Integer getMaxValue() {
        return assignment.getMultiplicity().getMax();
    }

    public int getMaxLength() {
        return assignment.getMaxLength();
    }

    public URI getSchemaURI() {
        return null;
    }

    public String getEncoding() {
        return "UTF-8";
    }

    public boolean validates(Serializable value) {
        return assignment.isValid(value);    // TODO
    }

    public String validationError(Serializable value) {
        return validates(value) ? null : "assignment constraint violated";      // TODO
    }

    public String getLocalName() {
        return assignment.getXPath();
    }

    public URI getLocalNamespace() {
        return null;
    }

    public String getQueryName() {
        return assignment.getAlias();
    }

    public FxPropertyAssignment getAssignment() {
        return assignment;
    }
}
