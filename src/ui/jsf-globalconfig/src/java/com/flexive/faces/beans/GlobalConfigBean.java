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
package com.flexive.faces.beans;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.faces.messages.FxFacesMsgWarn;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.DBVendor;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.configuration.DivisionDataEdit;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.GlobalConfigurationEngine;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles global configuration authentication and setup.
 * Note that this beans runs completely outside standard flexive security, with a custom user name
 * and password retrieved from the
 * {@link com.flexive.shared.interfaces.GlobalConfigurationEngine GlobalConfigurationEngine}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GlobalConfigBean {
    private String username;
    private String password;
    private boolean authenticated = false;

    // division table
    private List<DivisionDataEdit> divisions;
    private int editIndex = -1;

    // url checker
    private String checkUrl;

    // update password
    private String oldPassword;
    private String newPassword;
    private String repeatNewPassword;

    /**
     * Perform the user login against the username and password delivered by the configuration engine.
     *
     * @return the outcome
     */
    public String login() {
        final GlobalConfigurationEngine globalConfig = EJBLookup.getGlobalConfigurationEngine();
        authenticated = false;
        try {
            if (!globalConfig.getRootLogin().equals(username) || !globalConfig.isMatchingRootPassword(password)) {
                new FxFacesMsgErr("GlobalConfig.err.wrongLogin").addToContext();
                return null;
            }
            authenticated = true;
            return "success";
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return null;
        }
    }

    public String logout() {
        authenticated = false;
        divisions = null;
        return "login";
    }

    /**
     * Add a division to the division table.
     *
     * @throws FxApplicationException if the divisions could not be initialized
     */
    public void addDivision() throws FxApplicationException {
        try {
            int maxId = 0;
            for (DivisionData data : getDivisions()) {
                if (data.getId() > maxId) {
                    maxId = data.getId();
                }
            }
            getDivisions().add(new DivisionDataEdit(new DivisionData(maxId + 1, false, "", "", DBVendor.Unknown, "")));
            editIndex = getDivisions().size() - 1;
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Removes the division at editIndex.
     */
    public void removeDivision() {
        try {
            if (getDivisions().get(editIndex).getId() == FxContext.get().getDivisionId()) {
                new FxFacesMsgErr("GlobalConfig.err.removeOwnDivision").addToContext();
                return;
            }
            final DivisionDataEdit removedDivision = getDivisions().remove(editIndex);
            new FxFacesMsgInfo("GlobalConfig.nfo.removedDivision", removedDivision.getId()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        } finally {
            editIndex = -1;
        }
    }

    /**
     * Tests the database connection of the division at editIndex.
     */
    public void testConnection() {
        try {
            final DivisionDataEdit division = getDivisions().get(editIndex);
            final DivisionData testedDivision = EJBLookup.getGlobalConfigurationEngine().createDivisionData(division.getId(), division.getDataSource(), division.getDomainRegEx());
            if (testedDivision.isAvailable()) {
                new FxFacesMsgInfo("GlobalConfig.nfo.divisionTestSuccess", testedDivision.getId()).addToContext();
            } else {
                new FxFacesMsgErr("GlobalConfig.err.divisionUnavailable", testedDivision.getId()).addToContext();
            }
            getDivisions().set(editIndex, testedDivision.asEditable());
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("GlobalConfig.err.divisionTest", divisions.get(editIndex).getId(), e).addToContext();
        } finally {
            editIndex = -1;
        }
    }

    public void checkUrl() {
        try {
            if (checkUrl != null && (checkUrl.toLowerCase().startsWith("http://") || checkUrl.toLowerCase().startsWith("https://"))) {
                new FxFacesMsgWarn("GlobalConfig.wng.url.protocol").addToContext();
                checkUrl = checkUrl.substring(checkUrl.indexOf("://") + 3);
            }
            int divisionId = -1;
            for (DivisionData data : getDivisions()) {
                if (data.isMatchingDomain(checkUrl)) {
                    if (divisionId == -1) {
                        divisionId = data.getId();
                        new FxFacesMsgInfo("GlobalConfig.nfo.divisionMatch", data.getId()).addToContext();
                    } else {
                        new FxFacesMsgInfo("GlobalConfig.nfo.divisionMatchesTooLate", data.getId(), divisionId).addToContext();
                    }
                }
            }
            if (divisionId == -1) {
                new FxFacesMsgInfo("GlobalConfig.nfo.divisionNoMatch").addToContext();
            }
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Return from edit mode.
     */
    public void leaveEditMode() {
        editIndex = -1;
    }

    public void resetDivisions() {
        divisions = null;
        editIndex = -1;
    }

    public void updateDivisions() {
        ensureLoggedIn();
        FxContext.get().setGlobalAuthenticated(true);
        try {
            EJBLookup.getGlobalConfigurationEngine().saveDivisions(getDivisions());
            new FxFacesMsgInfo("GlobalConfig.nfo.updatedDivisions").addToContext();
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        } finally {
            FxContext.get().setGlobalAuthenticated(false);
        }
    }

    public void updatePassword() {
        ensureLoggedIn();
        FxContext.get().setGlobalAuthenticated(true);
        try {
            if (!EJBLookup.getGlobalConfigurationEngine().isMatchingRootPassword(oldPassword)) {
                new FxFacesMsgErr("GlobalConfig.err.password.old").addToContext();
                return;
            }
            if (!StringUtils.equals(newPassword, repeatNewPassword)) {
                new FxFacesMsgErr("GlobalConfig.err.password.repeat").addToContext();
                return;
            }
            if (StringUtils.isBlank(newPassword)) {
                new FxFacesMsgErr("GlobalConfig.err.password.empty").addToContext();
                return;
            }
            EJBLookup.getGlobalConfigurationEngine().setRootPassword(newPassword);
            new FxFacesMsgInfo("GlobalConfig.nfo.passwordUpdated").addToContext();
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        } finally {
            FxContext.get().setGlobalAuthenticated(false);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getEditIndex() {
        return editIndex;
    }

    public void setEditIndex(int editIndex) {
        this.editIndex = editIndex;
    }

    public String getCheckUrl() {
        return checkUrl;
    }

    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getRepeatNewPassword() {
        return repeatNewPassword;
    }

    public void setRepeatNewPassword(String repeatNewPassword) {
        this.repeatNewPassword = repeatNewPassword;
    }

    public List<DivisionDataEdit> getDivisions() throws FxApplicationException {
        ensureLoggedIn();
        if (divisions == null) {
            divisions = new ArrayList<DivisionDataEdit>();
            for (DivisionData data : EJBLookup.getGlobalConfigurationEngine().getDivisions()) {
                divisions.add(data.asEditable());
            }
        }
        return divisions;
    }

    private void ensureLoggedIn() {
        if (!authenticated) {
            throw new IllegalStateException("Please login first before accessing the global configuration pages.");
        }
    }
}
