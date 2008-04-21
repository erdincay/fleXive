/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.war.beans.admin.content;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.content.FxData;

import java.util.Hashtable;

/**
 * Helper class for the ContentEditor.
 * <p/>
 * It generates a unique id for a xpath that gets accepted by jsf components.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class CeIdGenerator extends Hashtable<String, Object> {

    /**
     * Constructor.
     */
    protected CeIdGenerator() {
        super(0);
    }

    /**
     * Fake put - does nothing expect returning the id for the given object.
     *
     * @param object the object
     * @param ignore this parameter is discarded
     * @return the description of the assignment id, or null if the assignment id could not be resolved
     */
    public String put(Object object, String ignore) {
        // Do not put anything
        return get(object);
    }

    /**
     * Returns the id of the object, which is generated from its XPath.
     *
     * @param object the object
     * @return the description, or null if the assignment id could not be resolved
     */
    public String get(Object object) {
        // Id String may only contain [a-z]|[A-Z]|'-'|'_' to be accepted by jsf components
        try {
            if (object instanceof FxData) {
                String result = ((FxData) object).getXPathFull();
                return FxJsfUtils.encodeJSFIdentifier(result);
            }
            return null;
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return null;
        }
    }

    /**
     * Decodes the id of an object.
     *
     * @param id the encoded id
     * @return the decoded id
     */
    public static String decodeToXPath(String id) {
        return FxJsfUtils.decodeJSFIdentifier(id);
    }

}
