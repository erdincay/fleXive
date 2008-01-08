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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.MandatorEngine;
import com.flexive.shared.security.Mandator;

import java.util.*;

/**
 * This Bean provides access to the mandator functionality.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class MandatorBean {

    private Hashtable<Long, Mandator> mandatorsById = null;

    private long id = -1;
    private boolean active = false;
    private String name;
    private Mandator mandator;
    private MandatorEngine mandatorInterface;

    private static final String ID_CACHE_KEY = MandatorBean.class + "_id";

    // constructor
    public MandatorBean() {
        this.mandatorsById = new Hashtable<Long, Mandator>();
        this.mandatorInterface = EJBLookup.getMandatorEngine();
        this.mandator = new Mandator();
    }


    /**
     * Returns a hashtable holding all mandators with their ID (String) as key.
     *
     * @return a hastable holding all mandators with their ID (String) as key
     */
    public Map getMandatorsById() {
        if (mandatorsById == null || mandatorsById.get(id) == null) {
            // get active and inactive mandators
            List<Mandator> mandators = CacheAdmin.getEnvironment().getMandators(true, true);
            mandatorsById = new Hashtable<Long, Mandator>(mandators.size());
            for (Mandator mand : mandators) {
                mandatorsById.put(mand.getId(), mand);
            }
        }
        return Collections.unmodifiableMap(mandatorsById);
    }

    /**
     * Returns all mandators (active and inactive) in a list
     *
     * @return all mandators
     */
    public List<Mandator> getMandators() {
        if (mandatorsById == null || mandatorsById.get(id) == null) {
            // get active and inactive mandators
            List<Mandator> mandators = CacheAdmin.getEnvironment().getMandators(true, true);
            mandatorsById = new Hashtable<Long, Mandator>(mandators.size());
            for (Mandator mand : mandators) {
                mandatorsById.put(mand.getId(), mand);
            }
            return mandators;
        } else {
            List<Mandator> mandators = new ArrayList<Mandator>(mandatorsById.size());
            Set set = mandatorsById.entrySet();
            Iterator it = set.iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                mandators.add((Mandator) entry.getValue());
            }
            return mandators;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
        FxJsfUtils.setSessionAttribute(ID_CACHE_KEY, this.id);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Mandator getMandator() {
        return mandator;
    }

    public void setMandator(Mandator mandator) {
        this.mandator = mandator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void ensureMandatorIdSet() {
        if (this.id <= 0) {
            this.id = (Long) FxJsfUtils.getSessionAttribute(ID_CACHE_KEY);
        }
    }

    /**
     * Loads the mandator specified by the parameter id.
     *
     * @return the next page to render
     */
    public String editMandator() {
        ensureMandatorIdSet();
        setMandator(CacheAdmin.getEnvironment().getMandator(id));
        setName(mandator.getName());
        setActive(mandator.isActive());
        return "mandatorEdit";

    }

    /**
     * Creates a new mandator from the beans data.
     *
     * @return the next jsf page to render
     */
    public String createMandator() {

        // do not accept an empty string as name...
        if (mandator.getName().length() < 1) {
            new FxFacesMsgErr("Mandator.err.nameEmpty").addToContext();
            return "mandatorCreate";
        }
        try {
            setId(mandatorInterface.create(mandator.getName(), mandator.isActive()));
            setMandator(CacheAdmin.getEnvironment().getMandator(id));
            // make sure the variable holding the mandator data will be updated by setting it to null
            updateMandatorList();
            new FxFacesMsgInfo("Mandator.nfo.created", mandator.getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return "mandatorCreate";
        }

        return "mandatorOverview";
    }


    /**
     * Deletes a mandator, with the id specified by id.
     *
     * @return the next pageto render
     */
    public String deleteMandator() {

        ensureMandatorIdSet();

        try {
            mandatorInterface.remove(id);
            // make sure the variable holding the mandator data will be updated by setting it to null
            updateMandatorList();
            new FxFacesMsgInfo("Mandator.nfo.deleted").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "mandatorOverview";
    }

    /**
     * Saves the edited mandator
     *
     * @return the next page to render
     */
    public String saveMandator() {

        ensureMandatorIdSet();

        try {
            // do not accept an empty string as name...
            if (mandator.getName().length() < 1) {
                new FxFacesMsgErr("Mandator.err.nameEmpty").addToContext();
                return "mandatorEdit";
            }
            // check if the value changed - if yes, call the corresponding method to activate or deactivate the mandator
            if (active != mandator.isActive()) {
                if (mandator.isActive()) {
                    mandatorInterface.activate(id);
                } else {
                    mandatorInterface.deactivate(id);
                }
            }
            if (!name.equals(mandator.getName())) {
                mandatorInterface.changeName(id, mandator.getName());
            }
            // make sure the variable holding the mandator data will be updated by setting it to null
            updateMandatorList();
            new FxFacesMsgInfo("Mandator.nfo.updated").addToContext();
            return "mandatorOverview";
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return "mandatorOverview";
        }
    }

    /**
     * setting the mandatorsById variable to null ensures that it will be filled with the updated mandators data
     * upon the next access
     */
    private void updateMandatorList() {
        mandatorsById = null;
    }
}
