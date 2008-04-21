/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.shared.workflow;

import com.flexive.shared.AbstractSelectableObjectWithLabel;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * Definition of a workflow step.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StepDefinition extends AbstractSelectableObjectWithLabel implements Serializable {
    private static final long serialVersionUID = 7468501311030816400L;

    /**
     * The id of the LIVE step.
     */
    public static final long LIVE_STEP_ID = 1;

    /**
     * The id of the EDIT step.
     */
    public static final long EDIT_STEP_ID = 2;

    protected FxString label = null;
    protected String name = null;
    protected long uniqueTargetId = -1;
    protected long id = -1;

    /**
     * StepDefinition Constructor.
     *
     * @param label          the unique label of the step
     * @param name           the name of the step
     * @param id             the unique id of the step
     * @param uniqueTargetId the unique target id (-1 if it there is no unique target)
     */
    public StepDefinition(long id, FxString label, String name, long uniqueTargetId) {
        this.label = label;
        this.name = name;
        this.id = id;
        if (uniqueTargetId >= 0 && id == uniqueTargetId) {
            throw new FxInvalidParameterException("UNIQUETARGET", "ex.stepdefinition.uniqueTarget.circular.self",
                    this.label + " (Id: " + this.id + ")").asRuntimeException();
        }
        this.uniqueTargetId = (uniqueTargetId < 0) ? -1 : uniqueTargetId;
    }

    /**
     * StepDefinition Constructor.
     *
     * @param label          the label of the step
     * @param name           the name of the step
     * @param uniqueTargetId the unique target id (-1 if it there is no unique target)
     */
    public StepDefinition(FxString label, String name, long uniqueTargetId) {
        this(-1, label, name, uniqueTargetId);
    }

    /**
     * Returns an editable step definition object.
     *
     * @return this step definition as an editable object.
     */
    public StepDefinitionEdit asEditable() {
        return new StepDefinitionEdit(this);
    }


    /**
     * Default constructor.
     */
    protected StepDefinition() {
    }


    /**
     * Returns the label of the step definition.
     *
     * @return the label of the step definition.
     */
    public FxString getLabel() {
        return this.label;
    }

    /**
     * Returns the name of the step definition.
     *
     * @return the name of the step definition.
     */
    public String getName() {
        return this.name;
    }


    /**
     * Returns the id of the step definition
     *
     * @return the id of the step definition
     */
    public long getId() {
        return this.id;
    }

    /**
     * Returns true if only one version of a entry may be in a step using this step definition.
     *
     * @return true if only one version of a entry may be in a step using this step definition.
     */
    public boolean isUnique() {
        return (this.uniqueTargetId != -1);
    }

    /**
     * Returns the stepDefinition used as target by the unique flag.
     *
     * @return the stepDefinition used as target by the unique flag.
     */
    public long getUniqueTargetId() {
        return this.uniqueTargetId;
    }

    /**
     * Returns true if the step definition is needed by the system and can not be deleted.
     *
     * @return true if the step definition is needed by the system and can not be deleted.
     */
    public boolean isSystemStepDefinition() {
        return (id == LIVE_STEP_ID || id == EDIT_STEP_ID);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "StepDefinition[id=" + id + "]";
    }


}
