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

import com.flexive.shared.AbstractSelectableObjectWithName;
import com.flexive.shared.FxContext;
import com.flexive.shared.SelectableObjectWithName;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.UserTicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflow data beans.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Workflow extends AbstractSelectableObjectWithName implements Serializable, SelectableObjectWithName {
    private static final long serialVersionUID = 7057943902512439668L;

    protected String name = null;
    protected String description = null;
    protected long id = -1;
    protected List<Step> steps = new ArrayList<Step>();
    protected List<Route> routes = new ArrayList<Route>();

    /**
     * Constructor.
     *
     * @param id          the id of the workflow
     * @param name        the name of the workflow
     * @param description a description
     * @param steps       the workflow steps
     * @param routes      the workflow routes
     */
    public Workflow(long id, String name, String description, List<Step> steps, List<Route> routes) {
        this.id = id;
        this.name = name;
        this.description = (description == null) ? "" : description;
        this.steps = new ArrayList<Step>(steps == null ? 0 : steps.size());
        if (steps != null)
            this.steps.addAll(steps);
        this.routes = new ArrayList<Route>(routes == null ? 0 : routes.size());
        if (routes != null)
            this.routes.addAll(routes);
    }

    /**
     * Default constructor.
     */
    protected Workflow() {
    }

    /**
     * Returns this workflow as an editable object.
     *
     * @return this workflow as an editable object.
     */
    public WorkflowEdit asEditable() {
        return new WorkflowEdit(this);
    }

    /**
     * Returns the name of the workflow.
     *
     * @return the name of the workflow
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the description of the workflow.
     *
     * @return the description of the workflow, may be a empty String but is never null.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the id of the workflow.
     *
     * @return the id of the workflow
     */
    public long getId() {
        return this.id;
    }

    /**
     * Returns the individual steps of the workflow.
     *
     * @return the individual steps of the workflow.
     */
    public List<Step> getSteps() {
        return steps;
    }

    /**
     * Return the workflow routes (connections between steps).
     *
     * @return the workflow routes (connections between steps).
     */
    public List<Route> getRoutes() {
        return routes;
    }

    /**
     * Check if the given step is valid for this workflow
     *
     * @param stepId the step ID to be checked
     * @return if the given step is valid for this workflow
     */
    public boolean isStepValid(long stepId) {
        for (Step step : steps)
            if (step.getId() == stepId)
                return true;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName() + "(Id=" + getId() + ")";
    }

    /**
     * Get the live step of this workflow, will throw an exception if no live step is defined
     *
     * @return live step
     * @throws FxApplicationException on errors
     */
    public Step getLiveStep() throws FxApplicationException {
        for (Step s : steps)
            if (s.isLiveStep())
                return s;
        throw new FxApplicationException("ex.workflow.noLiveStepDefined", getName());
    }

    /**
     * Is a Live step contained in this workflow?
     *
     * @return if a Live step exists
     */
    public boolean hasLiveStep() {
        for (Step s : steps)
            if (s.isLiveStep())
                return true;
        return false;
    }

    /**
     * Get a list of all possible targets for the given step id
     *
     * @param stepId source step id to get all targets for
     * @return target steps
     */
    public List<Step> getTargets(long stepId) {
        List<Step> res = new ArrayList<Step>(5);
        UserTicket ticket = FxContext.getUserTicket();
        for (Route r : getRoutes()) {
            if (r.getFromStepId() == stepId) {
                Step step = getStep(r.getToStepId());
                if (ticket.isGlobalSupervisor() || ticket.isInGroup(r.getGroupId()))
                    if (!res.contains(step))
                        res.add(step);
            }
        }
        return res;
    }

    /**
     * Get the requested step from the internal list of steps
     *
     * @param stepId requested step id
     * @return Step instance
     */
    private Step getStep(long stepId) {
        for (Step s : steps)
            if (s.getId() == stepId)
                return s;
        throw new FxApplicationException("ex.workflow.route.referencedStep", stepId).asRuntimeException();
    }


    /**
     * Check if a route from source to dest exists for the calling user
     *
     * @param source source step id
     * @param dest   destination step id
     * @return does a valid route exist?
     */
    public boolean isRouteValid(long source, long dest) {
        UserTicket ticket = FxContext.getUserTicket();
        for (Route r : getRoutes()) {
            if (r.getFromStepId() == source && r.getToStepId() == dest) {
                if (ticket.isGlobalSupervisor() || ticket.isInGroup(r.getGroupId()))
                    return true;
            }
        }
        return false;
    }
}
