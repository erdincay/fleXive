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
package com.flexive.shared.workflow;

import com.flexive.shared.security.ACLCategory;

import java.io.Serializable;

/**
 * Editable workflow step.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StepEdit extends Step implements Serializable {
    private static final long serialVersionUID = 7604922500076818L;

    /**
     * Constructor from a Step
     *
     * @param step the source step
     */
    public StepEdit(Step step) {
        super(step);
    }

    /**
     * Default constructor
     */
    public StepEdit() {
    }

    /**
     * Sets the ACL ID to be used for this step.
     *
     * @param aclId the ACL ID to be used for this step.
     */
    public void setAclId(long aclId) {
        this.aclId = aclId;
    }

    /**
     * Sets the ID for this step.
     *
     * @param id the ID for this step.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Sets the step definition ID to be used for this step.
     *
     * @param stepDefinitionId the step definition ID to be used for this step.
     */
    public void setStepDefinitionId(long stepDefinitionId) {
        this.stepDefinitionId = stepDefinitionId;
    }

    /**
     * Sets the workflow ID for this step.
     *
     * @param workflowId the workflow ID for this step.
     */
    public void setWorkflowId(long workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * Ctor for a new step based on a step definition using the default ACL
     *
     * @param stepDefinitionId step definition to use
     */
    public StepEdit(long stepDefinitionId) {
        super(-1, stepDefinitionId, -1, ACLCategory.WORKFLOW.getDefaultId());
    }

    /**
     * Convenience method to create new step based on a step definition using the default ACL
     *
     * @param stepDefinitionId step definition to use
     * @return StepEdit
     */
    public static StepEdit createNew(long stepDefinitionId) {
        return new StepEdit(stepDefinitionId);
    }
}
