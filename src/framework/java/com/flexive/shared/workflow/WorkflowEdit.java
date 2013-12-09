/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Editable workflow class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class WorkflowEdit extends Workflow implements Serializable {
    private static final long serialVersionUID = 2063328609325310548L;

    /**
     * Copy constructor.
     *
     * @param workflow the source workflow
     */
    public WorkflowEdit(Workflow workflow) {
        super(workflow.getId(), workflow.getName(), workflow.getDescription(), workflow.getSteps(),
                workflow.getRoutes());
    }

    /**
     * Default constructor.
     */
    public WorkflowEdit() {
    }

    /**
     * Set the workflow ID.
     *
     * @param id the workflow ID.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Set the workflow name.
     *
     * @param name the workflow name.
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Set the workflow description.
     *
     * @param description the workflow description.
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * Set the workflow steps.
     *
     * @param steps the workflow steps.
     */
    public void setSteps(List<? extends StepEdit> steps) {
        List<Step> list = new ArrayList<Step>(steps.size());
        list.addAll(steps);
        this.steps = list;
    }

    /**
     * Set the workflow routes.
     *
     * @param routes the workflow routes.
     */
    public void setRoutes(List<? extends Route> routes) {
        List<Route> list = new ArrayList<Route>(routes.size());
        list.addAll(routes);
        this.routes = list;
    }

    /**
     * Ctor to create a new WorkflowEdit instance
     *
     * @param name workflow name
     */
    public WorkflowEdit(String name) {
        super(-1, name, "", null, null);
    }

    /**
     * Convenience method to create a new WorkflowEdit instance
     *
     * @param name workflow name
     * @return WorkflowEdit instance
     */
    public static WorkflowEdit createNew(String name) {
        return new WorkflowEdit(name);
    }
}
