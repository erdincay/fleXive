/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.shared.exceptions.FxInvalidParameterException;

/**
 * Editable FxScriptInfo
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class FxScriptInfoEdit extends FxScriptInfo {

    public void setEvent(FxScriptEvent event) {
        this.event = event;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setActive(boolean active) {
        this.active = active;
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
     * @throws FxInvalidParameterException on errors
     * @see FxScriptEvent
     */
    public FxScriptInfoEdit(long id, FxScriptEvent event, String name, String description, String code, boolean active) throws FxInvalidParameterException {
        super(id, event, name, description, code, active);
    }

    /**
     * Constructs a editable script info object from FxScriptInfo.
     *
     * @param si the script info object
     */
    public FxScriptInfoEdit(FxScriptInfo si) {
        this.id = si.id;
        this.name = si.name;
        this.event = si.event;
        this.description = si.description;
        this.code = si.code;
        this.active = si.active;
    }
}
