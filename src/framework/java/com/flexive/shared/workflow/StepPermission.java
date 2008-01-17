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

import java.io.Serializable;

/**
 * Class storing a step and its permissions.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StepPermission implements Serializable {
    private static final long serialVersionUID = 6829315756578605674L;

    protected long step = -1;
    protected boolean mayRead = false;
    protected boolean mayEdit = false;
    protected boolean mayRelate = false;
    protected boolean mayDelete = false;
    protected boolean mayExport = false;
    protected boolean mayCreate = false;
    protected long stepDefId = -1;
    protected long workflow = -1;

    /**
     * Constructor.
     *
     * @param stepId     the step id
     * @param stepDefId  the step definition ID
     * @param workflowId the workflow id
     * @param read       the read permission
     * @param edit       the edit permission
     * @param relate     the relate permission
     * @param delete     the delete permission
     * @param export     the export permission
     * @param create     the create permission
     */
    public StepPermission(long stepId, long stepDefId, long workflowId, boolean read, boolean edit, boolean relate,
                          boolean delete, boolean export, boolean create) {
        this.stepDefId = stepDefId;
        this.step = stepId;
        this.workflow = workflowId;
        this.mayRead = read;
        this.mayEdit = edit;
        this.mayRelate = relate;
        this.mayDelete = delete;
        this.mayExport = export;
        this.mayCreate = create;
    }

    /**
     * Protected default constructor.
     */
    protected StepPermission() {

    }


    /**
     * Returns the id of the step.
     *
     * @return the id of the step.
     */
    public long getStepId() {
        return this.step;
    }

    /**
     * Returns the id of the stepDefinition this step belongs to.
     *
     * @return the id of the stepDefinition this step belongs to.
     */
    public long getStepDefId() {
        return this.stepDefId;
    }

    /**
     * Returns the id of the workflow the step belongs to.
     *
     * @return the id of the workflow the step belongs to.
     */
    public long getWorkflowId() {
        return this.workflow;
    }

    /**
     * Return true if the StepPermissiom grants read permission.
     *
     * @return true if the StepPermissiom grants read permission.
     */
    public boolean getMayRead() {
        return this.mayRead;
    }

    /**
     * Return true if the StepPermissiom grants edit permission.
     *
     * @return true if the StepPermissiom grants edit permission.
     */
    public boolean getMayEdit() {
        return this.mayEdit;
    }

    /**
     * Return true if the StepPermissiom grants relate permission.
     *
     * @return true if the StepPermissiom grants relate permission.
     */
    public boolean getMayRelate() {
        return this.mayRelate;
    }

    /**
     * Return true if the StepPermissiom grants unassign permission.
     *
     * @return true if the StepPermissiom grants unassign permission.
     */
    public boolean getMayDelete() {
        return this.mayDelete;
    }

    /**
     * Return true if the StepPermissiom grants export permission.
     *
     * @return true if the StepPermissiom grants export permission.
     */
    public boolean getMayExport() {
        return this.mayExport;
    }

    /**
     * Return true if the StepPermission grants create permission.
     *
     * @return true if the StepPermission grants create permission.
     */
    public boolean getMayCreate() {
		return this.mayCreate;
	}
}
