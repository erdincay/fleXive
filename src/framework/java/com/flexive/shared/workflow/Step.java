/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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

import java.io.Serializable;

/**
 * StepEngine data beans.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Step implements Serializable {
    private static final long serialVersionUID = -4546587502140978353L;

    protected long id = -1;
    protected long workflowId = -1;
    protected long stepDefinitionId = -1;
    protected long aclId = -1;


    /**
     * Creates a new workflow step.
     * 
     * @param id        the step ID
     * @param stepDefinitionId  the step definition ID
     * @param workflowId    the workflow ID
     * @param aclId     the ACL ID for the step
     */
    public Step(long id, long stepDefinitionId, long workflowId, long aclId) {
        this.id = id;
        this.workflowId = workflowId;
        this.stepDefinitionId = stepDefinitionId;
        this.aclId = aclId;
    }

    /**
     * Creates a new workflow step that is not assigned to an existing workflow.
     *
     * @param id        the step ID
     * @param stepDefinitionId  the step definition ID
     * @param aclId     the ACL ID for the step
     */
    public Step(long id, long stepDefinitionId, long aclId) {
        this.id = id;
        this.stepDefinitionId = stepDefinitionId;
        this.aclId = aclId;
    }

    /**
     * Creates a new workflow step.
     * 
     * @param id        the step ID
     * @param step		the step instance to be used for the other fields
     */
    public Step(long id, Step step) {
        this.id = id;
        this.workflowId = step.getWorkflowId();
        this.stepDefinitionId = step.getStepDefinitionId();
        this.aclId = step.getAclId();
    }

    /**
     * Copy constructor
     * @param step  source step
     */
    public Step(Step step) {
        this(step.getId(), step.getStepDefinitionId(), step.getWorkflowId(), step.getAclId());
    }

    /**
     * Protected default constructor.
     */
    protected Step() {
    }


    /**
     * Returns this step as an editable instance.
     *
     * @return  this step as an editable instance.
     */
    public StepEdit asEditable() {
        return new StepEdit(this);
    }

    /**
     * Compares two workflow steps by their internal ID.
     * @param o the step this object should be compared to
     * @return true if the steps are equal
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Step && ((Step) o).getId() == id;
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (int) id;
    }

    /**
     * Returns the unqiue stepDefinition id this step belongs to.
     *
     * @return the unqiue stepDefinition id this step belongs to.
     */
    public long getStepDefinitionId() {
        return stepDefinitionId;
    }


    /**
     * Returns the unique id of the step.
     *
     * @return the unique id of the step
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the of the workfow the step belongs to.
     *
     * @return the id of the workfow the step belongs to
     */
    public long getWorkflowId() {
        return workflowId;
    }


    /**
     * Returns the id of the ACL assigned to the step.
     *
     * @return the id of the ACL assigned to the step.
     */
    public long getAclId() {
        return aclId;
    }

    /**
     * Returns true id the step is the LIVE step.
     *
     * @return true if the step is the LIVE step.
     */
    public boolean isLiveStep() {
        return (stepDefinitionId==StepDefinition.LIVE_STEP_ID);
    }

    /**
     * Returns true id the step is the EDIT step.
     *
     * @return true if the step is the EDIT step.
     */
    public boolean isEditStep() {
        return (stepDefinitionId==StepDefinition.EDIT_STEP_ID);
    }

    /** {@inheritDoc} */
	@Override
	public String toString() {
		return "Step[id=" + id + "]";
	}


}
