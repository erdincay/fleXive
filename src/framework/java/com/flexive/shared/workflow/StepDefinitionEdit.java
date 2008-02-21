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
package com.flexive.shared.workflow;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * Editable step definition.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StepDefinitionEdit extends StepDefinition implements Serializable {
    private static final long serialVersionUID = 8136768479690229989L;

    /**
     * Copy constructor taking a step definition object
     *
     * @param stepDefinition the source step definition
     */
    public StepDefinitionEdit(StepDefinition stepDefinition) {
        super(stepDefinition.getId(), stepDefinition.getLabel().copy(), stepDefinition.getName(),
                stepDefinition.getUniqueTargetId());
    }

    /**
     * Public default constructor.
     */
    public StepDefinitionEdit() {
    }

    /**
     * Set the step definition name
     *
     * @param name the step definition name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Set the step definition id
     *
     * @param id the step definition id
     */
    public void setId(long id) {
        this.id = id;
    }


    /**
     * Set the step definition name
     *
     * @param name the step definition name
     */
    public void setLabel(FxString name) {
        this.label = name;
    }


    /**
     * Set the step definition's unique target step definition
     *
     * @param uniqueTarget the step definition's unique target step definition
     */
    public void setUniqueTargetId(long uniqueTarget) {
        if (uniqueTarget != -1 && uniqueTarget == this.id) {
            throw new FxInvalidParameterException("UNIQUETARGET", "ex.stepdefinition.uniqueTarget.circular.self",
                    this.label + " (Id: " + this.id + ")").asRuntimeException();
        }
        this.uniqueTargetId = uniqueTarget;
    }
}
