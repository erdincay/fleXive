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
package com.flexive.war.webdav;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Unlock operation.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class OperationUnlock extends Operation {

    public OperationUnlock(HttpServletRequest req, HttpServletResponse resp, boolean readonly) {
        super(req, resp, readonly);
    }

    void writeResponse() throws IOException {
        if (readonly) {
            response.sendError(FxWebDavStatus.SC_FORBIDDEN);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
    }

    /**
     * UNLOCK Method.

     protected void doUnlock(HttpServletRequest req, HttpServletResponse resp)
     throws ServletException, IOException {

     if (readOnly) {
     resp.sendError(FxWebDavStatus.SC_FORBIDDEN);
     return;
     }

     if (isLocked(req)) {
     resp.sendError(FxWebDavStatus.SC_LOCKED);
     return;
     }

     String path = getRelativePath(req);

     String lockTokenHeader = req.getHeader("Lock-Token");
     if (lockTokenHeader == null)
     lockTokenHeader = "";

     // Checking resource locks

     LockInfo lock = resourceLocks.get(path);
     Enumeration tokenList;
     if (lock != null) {

     // At least one of the tokens of the locks must have been given

     tokenList = lock.tokens.elements();
     while (tokenList.hasMoreElements()) {
     String token = (String) tokenList.nextElement();
     if (lockTokenHeader.indexOf(token) != -1) {
     lock.tokens.removeElement(token);
     }
     }

     if (lock.tokens.isEmpty()) {
     resourceLocks.remove(path);
     // Removing any lock-null resource which would be present
     lockNullResources.remove(path);
     }

     }

     // Checking inheritable collection locks

     Enumeration collectionLocksList = collectionLocks.elements();
     while (collectionLocksList.hasMoreElements()) {
     lock = (LockInfo) collectionLocksList.nextElement();
     if (path.equals(lock.path)) {

     tokenList = lock.tokens.elements();
     while (tokenList.hasMoreElements()) {
     String token = (String) tokenList.nextElement();
     if (lockTokenHeader.indexOf(token) != -1) {
     lock.tokens.removeElement(token);
     break;
     }
     }

     if (lock.tokens.isEmpty()) {
     collectionLocks.removeElement(lock);
     // Removing any lock-null resource which would be present
     lockNullResources.remove(path);
     }

     }
     }

     resp.setStatus(FxWebDavStatus.SC_NO_CONTENT);

     }*/
}
