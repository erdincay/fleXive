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
package com.flexive.war.beans.admin.main;

import com.flexive.shared.FxLanguage;
import static com.flexive.shared.EJBLookup.getLanguageEngine;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.faces.messages.FxFacesMsgErr;

import java.util.List;
import java.io.Serializable;

/**
 * Languages settings
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class LanguagesBean implements Serializable {
    private static final long serialVersionUID = 3076818793559505451L;

    private boolean ignoreUsage;
    private List<FxLanguage> available = null;
    private List<FxLanguage> disabled = null;
    private FxLanguage language;

    public boolean isIgnoreUsage() {
        return ignoreUsage;
    }

    public void setIgnoreUsage(boolean ignoreUsage) {
        this.ignoreUsage = ignoreUsage;
    }

    public List<FxLanguage> getAvailable() throws FxApplicationException {
        if (available == null)
            available = getLanguageEngine().loadAvailable(true);
        return available;
    }

    public List<FxLanguage> getDisabled() throws FxApplicationException {
        if (disabled == null)
            disabled = getLanguageEngine().loadDisabled();
        return disabled;
    }

    public void setAvailable(List<FxLanguage> available) {
        this.available = available;
    }

    public void setDisabled(List<FxLanguage> disabled) {
        this.disabled = disabled;
    }

    public FxLanguage getLanguage() {
        return language;
    }

    public void setLanguage(FxLanguage language) {
        this.language = language;
    }

    public synchronized void moveLanguageUp() throws FxApplicationException {
        getAvailable();
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).getId() == language.getId()) {
                if (i == 0)
                    return;
                FxLanguage tmp = available.remove(i);
                available.add(i - 1, tmp);
                return;
            }
        }
    }

    public synchronized void moveLanguageDown() throws FxApplicationException {
        getAvailable();
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).getId() == language.getId()) {
                if (i == available.size() - 1)
                    return;
                FxLanguage tmp = available.remove(i);
                available.add(i + 1, tmp);
                return;
            }
        }
    }

    public long getFirst() throws FxApplicationException {
        getAvailable();
        if (available.size() == 0)
            return 0;
        return available.get(0).getId();
    }

    public long getLast() throws FxApplicationException {
        getAvailable();
        if (available.size() == 0)
            return 0;
        return available.get(available.size() - 1).getId();
    }

    public void addLanguage() throws FxApplicationException {
        getAvailable();
        getDisabled();
        for (int i = 0; i < disabled.size(); i++) {
            if (disabled.get(i).getId() == language.getId()) {
                available.add(disabled.remove(i));
                return;
            }
        }
    }

    public void removeLanguage() throws FxApplicationException {
        getAvailable();
        getDisabled();
        FxLanguage tmp = null;
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).getId() == language.getId()) {
                tmp = available.remove(i);
                break;
            }
        }
        if (tmp == null)
            return;
        for (int i = 0; i < disabled.size(); i++) {
            if (disabled.get(i).getIso2digit().compareTo(language.getIso2digit()) > 0) {
                disabled.add(i, tmp);
                return;
            }
        }
        disabled.add(tmp);
    }

    /**
     * Save language settings.
     *
     * @return the next page
     */
    public String saveSettings() {
        try {
            getLanguageEngine().setAvailable(available, ignoreUsage);
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "languages";
    }

}
