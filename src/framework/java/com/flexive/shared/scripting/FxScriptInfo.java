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

import com.flexive.shared.AbstractSelectableObjectWithName;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Information about a script
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxScriptInfo extends AbstractSelectableObjectWithName implements Serializable {
    private static final long serialVersionUID = -2845241882558637595L;
    protected long id;
    protected FxScriptEvent event;
    protected String name;
    protected String description;
    protected boolean active =false;

    public FxScriptInfo() {
        /* empty constructor */
    }

    /**
     * Constructor
     *
     * @param id          script id
     * @param event       script type
     * @param name        (unique) name of the script
     * @param description description
     * @param active      if the script is active
     * @throws FxInvalidParameterException on errors
     * @see FxScriptEvent
     */
    public FxScriptInfo(long id, FxScriptEvent event, String name, String description, boolean active) throws FxInvalidParameterException {
        this.id = id;
        this.event = event;
        this.name = name;
        this.description = (description == null ? "" : description);
        this.active=active;
        if (StringUtils.isEmpty(this.name) || this.name.length() > 255) {
            throw new FxInvalidParameterException("NAME", "ex.scripting.name.invalid", name);
        }
    }

    /**
     * Return this script info as editable object.
     *
     * @return  script info as editable
     */

    public FxScriptInfoEdit asEditable()  {
        try {
            return new FxScriptInfoEdit(this);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * Get the id of this script
     *
     * @return id of this script
     */
    public long getId() {
        return id;
    }

    /**
     * Get the event type of this script
     *
     * @return event type of this script
     * @see FxScriptEvent
     */
    public FxScriptEvent getEvent() {
        return event;
    }

    /**
     * Get the (unique) name of this script
     *
     * @return name of this script
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description of this script
     *
     * @return description of this script
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns if the script is set to active.
     *
     * @return  active
     */
    public boolean isActive() {
        return active;
    }


}
