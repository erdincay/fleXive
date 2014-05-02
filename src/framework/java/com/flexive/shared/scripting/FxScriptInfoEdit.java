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
package com.flexive.shared.scripting;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;

/**
 * Editable FxScriptInfo
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class FxScriptInfoEdit extends FxScriptInfo {

    protected String code;

    public void setEvent(FxScriptEvent event) {
        this.event = event;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Implementation detail: cached groovy scripts cannot be run concurrently, but
     * are only compiled once. Groovy scripts that are not cached are compiled every
     * time before execution (cached = false is suitable for long running groovy scripts
     * that are likely to be executed concurrently).
     *
     * @param cached if a script should be cached
     * @since 3.1.1
     *
     */
    public void setCached(boolean cached) {
        this.cached = cached;
    }

    /**
     * Constructor
     *
     * @param id          script id
     * @param event       script type
     * @param name        (unique) name of the script
     * @param description description
     * @param code        the script code
     * @param active      if the script is active
     * @param cached      if the script is cached
     *
     * @throws FxInvalidParameterException on errors
     * @see FxScriptEvent
     * @since 3.1.1
     */
    public FxScriptInfoEdit(long id, FxScriptEvent event, String name, String description, String code, boolean active, boolean cached) throws FxInvalidParameterException {
        super(id, event, name, description, active, cached);
        this.code = code;
    }

    /**
     * Constructs a editable script info object from FxScriptInfo.
     *
     * @param si the script info object
     * @throws FxApplicationException if the code could not be loaded
     */
    public FxScriptInfoEdit(FxScriptInfo si) throws FxApplicationException {
        this.id = si.id;
        this.name = si.name;
        this.event = si.event;
        this.description = si.description;
        this.code = si.getId() > 0 ? EJBLookup.getScriptingEngine().loadScriptCode(si.getId()) : "";
        this.active = si.active;
        this.cached = si.cached;
    }
}
