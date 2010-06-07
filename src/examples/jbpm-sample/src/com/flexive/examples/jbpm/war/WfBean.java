/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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

package com.flexive.examples.jbpm.war;

import org.jbpm.api.*;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.model.Transition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class WfBean implements Serializable {


    private static final long serialVersionUID = 446132848551592L;


    /**
     * Default constructor
     */
    public WfBean(){

    }



    /**
        * Deploys the process, starts a process execution, walks through the process states
        * until it reaches the end state.
        */
    public void startProcess(){
	    // build a process engine from the default configuration file (jbpm.cfg.xml) - make sure the resource file is located in your classpath!!!
        ProcessEngine processEngine = new Configuration().setResource("/jbpm.cfg.xml").buildProcessEngine();
        // deploy the process using its xml-decription - make sure the resource file is located in your classpath!!!
        String deploymentId = processEngine.getRepositoryService().createDeployment()
            .addResourceFromClasspath("test.jpdl.xml")
            .deploy();

        ProcessInstance pi = null;

        // start a new process instance
        ExecutionService es = processEngine.getExecutionService();
        /**
               * start the process instance and update the process instance variable.
               * note: be sure to update the process instance variable when passing
               * from one state to another as it is not automatically updated
               */
        pi = es.startProcessInstanceByKey("test");

        // get the current execution of the workflow
        Execution e1 = pi.findActiveExecutionIn(getActiveState(pi));
        // signal the execution of the workflow to take the specified transition (i.e. go to 'state2')
        pi = es.signalExecutionById(e1.getId(), getTrans(pi).get(0));

        // goto 'end'
        Execution e2 = pi.findActiveExecutionIn(getActiveState(pi));
        pi = es.signalExecutionById(e2.getId(), getTrans(pi).get(0));
    }


    // get the active state of the process instance, e.g. state1, state2
    public String getActiveState(ProcessInstance pi){
        try {
            java.util.Set<java.lang.String> names = pi.findActiveActivityNames();
            String name = (String) names.iterator().next();
            /* *
                      * find the workflow execution in the specified state; note: there can be
                      * several executions of the workflow, all in different states
                      */
            Execution ex = pi.findActiveExecutionIn(name);
            ExecutionImpl exi = (ExecutionImpl) ex;
            ActivityImpl ai = exi.getActivity();
            return ai.getName();
            } catch (Exception e){
                return "";
            }
    }

    // get all outgoing transitions for the current node/state
    public List<String> getTrans(ProcessInstance pi){
            List<String> out = new ArrayList<String>();
            try {
                java.util.Set<java.lang.String> names = pi.findActiveActivityNames();
                String name = (String) names.iterator().next();
                Execution ex = pi.findActiveExecutionIn(name);
                ExecutionImpl exi = (ExecutionImpl) ex;
                ActivityImpl ai = exi.getActivity();
                List<Transition> list = ai.getOutgoingTransitions();

                for(Transition t : list){
                    out.add(t.getName());
                }
            } catch(Exception e){
                // nothing
            }
            return out;
    }    
}
