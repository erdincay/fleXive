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
package com.flexive.war.webdav;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Lock operation.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class OperationLock extends Operation {

    public OperationLock(HttpServletRequest req, HttpServletResponse resp, boolean readonly) {
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
     * Repository of the locks put on single resources.
     * <p/>
     * Key : path <br>
     * Value : LockInfo
     */
    //private Hashtable<String, LockInfo> resourceLocks = new Hashtable<String, LockInfo>();

    /**
     * Repository of the lock-null resources.
     * <p/>
     * Key : path of the collection containing the lock-null resource<br>
     * Value : Vector of lock-null resource which are members of the
     * collection. Each element of the Vector is the path associated with
     * the lock-null resource.
     */
    //private Hashtable<String, Vector<String>> lockNullResources = new Hashtable<String, Vector<String>>();

    /**
     * Vector of the heritable locks.
     * <p/>
     * Key : path <br>
     * Value : LockInfo
     */
    //private Vector<LockInfo> collectionLocks = new Vector<LockInfo>();

    //  Create a new lock.
    //private static final int LOCK_CREATION = 0;
    // Refresh lock
    //private static final int LOCK_REFRESH = 1;
    // Default lock timeout
    //private static final int DEFAULT_TIMEOUT = 3600;
    // Maximum lock timeout.
    //private static final int MAX_TIMEOUT = 604800;

    /*
    **
     * LOCK Method.
     *
    protected void doLock(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (readOnly) {
            resp.sendError(FxWebDavStatus.SC_FORBIDDEN);
            return;
        }

        if (isLocked(req)) {
            resp.sendError(FxWebDavStatus.SC_LOCKED);
            return;
        }

        LockInfo lock = new LockInfo();

        // Parsing lock request

        // Parsing depth header

        String depthStr = req.getHeader("Depth");

        if (depthStr == null) {
            lock.depth = INFINITY;
        } else {
            if (depthStr.equals("0")) {
                lock.depth = 0;
            } else {
                lock.depth = INFINITY;
            }
        }

        // Parsing timeout header

        int lockDuration;
        String lockDurationStr = req.getHeader("Timeout");
        if (lockDurationStr == null) {
            lockDuration = DEFAULT_TIMEOUT;
        } else {
            int commaPos = lockDurationStr.indexOf(",");
            // If multiple timeouts, just use the first
            if (commaPos != -1) {
                lockDurationStr = lockDurationStr.substring(0, commaPos);
            }
            if (lockDurationStr.startsWith("Second-")) {
                lockDuration = Integer.parseInt(lockDurationStr.substring(7));
            } else {
                if (lockDurationStr.equalsIgnoreCase("infinity")) {
                    lockDuration = MAX_TIMEOUT;
                } else {
                    try {
                        lockDuration = Integer.parseInt(lockDurationStr);
                    } catch (NumberFormatException e) {
                        lockDuration = MAX_TIMEOUT;
                    }
                }
            }
            if (lockDuration == 0) {
                lockDuration = DEFAULT_TIMEOUT;
            }
            if (lockDuration > MAX_TIMEOUT) {
                lockDuration = MAX_TIMEOUT;
            }
        }
        lock.expiresAt = System.currentTimeMillis() + (lockDuration * 1000);

        int lockRequestType = LOCK_CREATION;

        Node lockInfoNode = null;

        DocumentBuilder documentBuilder = getDocumentBuilder();

        try {
            Document document = documentBuilder.parse(new InputSource
                    (req.getInputStream()));

            // Get the root element of the document
            lockInfoNode = document.getDocumentElement();
        } catch (Exception e) {
            lockRequestType = LOCK_REFRESH;
        }

        if (lockInfoNode != null) {

            // Reading lock information

            NodeList childList = lockInfoNode.getChildNodes();
            StringWriter strWriter;
            DOMWriter domWriter;

            Node lockScopeNode = null;
            Node lockTypeNode = null;
            Node lockOwnerNode = null;

            for (int i = 0; i < childList.getLength(); i++) {
                Node currentNode = childList.item(i);
                switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        String nodeName = currentNode.getNodeName();
                        if (nodeName.endsWith("lockscope")) {
                            lockScopeNode = currentNode;
                        }
                        if (nodeName.endsWith("locktype")) {
                            lockTypeNode = currentNode;
                        }
                        if (nodeName.endsWith("owner")) {
                            lockOwnerNode = currentNode;
                        }
                        break;
                }
            }

            if (lockScopeNode != null) {

                childList = lockScopeNode.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                        case Node.TEXT_NODE:
                            break;
                        case Node.ELEMENT_NODE:
                            String tempScope = currentNode.getNodeName();
                            if (tempScope.indexOf(':') != -1) {
                                lock.scope = tempScope.substring
                                        (tempScope.indexOf(':') + 1);
                            } else {
                                lock.scope = tempScope;
                            }
                            break;
                    }
                }

                if (lock.scope == null) {
                    // Bad request
                    resp.setStatus(FxWebDavStatus.SC_BAD_REQUEST);
                }

            } else {
                // Bad request
                resp.setStatus(FxWebDavStatus.SC_BAD_REQUEST);
            }

            if (lockTypeNode != null) {

                childList = lockTypeNode.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                        case Node.TEXT_NODE:
                            break;
                        case Node.ELEMENT_NODE:
                            String tempType = currentNode.getNodeName();
                            if (tempType.indexOf(':') != -1) {
                                lock.type =
                                        tempType.substring(tempType.indexOf(':') + 1);
                            } else {
                                lock.type = tempType;
                            }
                            break;
                    }
                }

                if (lock.type == null) {
                    // Bad request
                    resp.setStatus(FxWebDavStatus.SC_BAD_REQUEST);
                }

            } else {
                // Bad request
                resp.setStatus(FxWebDavStatus.SC_BAD_REQUEST);
            }

            if (lockOwnerNode != null) {

                childList = lockOwnerNode.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                        case Node.TEXT_NODE:
                            lock.owner += currentNode.getNodeValue();
                            break;
                        case Node.ELEMENT_NODE:
                            strWriter = new StringWriter();
                            domWriter = new DOMWriter(strWriter, true);
                            domWriter.setQualifiedNames(false);
                            domWriter.print(currentNode);
                            lock.owner += strWriter.toString();
                            break;
                    }
                }

                if (lock.owner == null) {
                    // Bad request
                    resp.setStatus(FxWebDavStatus.SC_BAD_REQUEST);
                }

            } else {
                lock.owner = "";
            }

        }

        String path = getRelativePath(req);

        lock.path = path;

        boolean exists = true;
        Object object = null;
        try {
            object = resources.lookup(path);
        } catch (NamingException e) {
            exists = false;
        }

        Enumeration locksList;

        if (lockRequestType == LOCK_CREATION) {

            // Generating lock id
            String lockTokenStr = req.getServletPath() + "-" + lock.type + "-"
                    + lock.scope + "-" + req.getUserPrincipal() + "-"
                    + lock.depth + "-" + lock.owner + "-" + lock.tokens + "-"
                    + lock.expiresAt + "-" + System.currentTimeMillis() + "-"
                    + secret;
            String lockToken = FxWebDavUtils.MD5encode(md5Helper.digest(lockTokenStr.getBytes()));

            if ((exists) && (object instanceof DirContext) &&
                    (lock.depth == INFINITY)) {

                // Locking a collection (and all its member resources)

                // Checking if a child resource of this collection is
                // already locked
                Vector<String> lockPaths = new Vector<String>();
                locksList = collectionLocks.elements();
                while (locksList.hasMoreElements()) {
                    LockInfo currentLock = (LockInfo) locksList.nextElement();
                    if (currentLock.hasExpired()) {
                        resourceLocks.remove(currentLock.path);
                        continue;
                    }
                    if ((currentLock.path.startsWith(lock.path)) &&
                            ((currentLock.isExclusive()) ||
                                    (lock.isExclusive()))) {
                        // A child collection of this collection is locked
                        lockPaths.addElement(currentLock.path);
                    }
                }
                locksList = resourceLocks.elements();
                while (locksList.hasMoreElements()) {
                    LockInfo currentLock = (LockInfo) locksList.nextElement();
                    if (currentLock.hasExpired()) {
                        resourceLocks.remove(currentLock.path);
                        continue;
                    }
                    if ((currentLock.path.startsWith(lock.path)) &&
                            ((currentLock.isExclusive()) ||
                                    (lock.isExclusive()))) {
                        // A child resource of this collection is locked
                        lockPaths.addElement(currentLock.path);
                    }
                }

                if (!lockPaths.isEmpty()) {

                    // One of the child paths was locked
                    // We generate a multistatus error report

                    Enumeration lockPathsList = lockPaths.elements();

                    resp.setStatus(FxWebDavStatus.SC_CONFLICT);

                    XMLWriter generatedXML = new XMLWriter();
                    generatedXML.writeXMLHeader();

                    generatedXML.writeElement
                            (null, "multistatus" + generateNamespaceDeclarations(),
                                    XMLWriter.OPENING);

                    while (lockPathsList.hasMoreElements()) {
                        generatedXML.writeElement(null, "response",
                                XMLWriter.OPENING);
                        generatedXML.writeElement(null, "href",
                                XMLWriter.OPENING);
                        generatedXML
                                .writeText((String) lockPathsList.nextElement());
                        generatedXML.writeElement(null, "href",
                                XMLWriter.CLOSING);
                        generatedXML.writeElement(null, "status",
                                XMLWriter.OPENING);
                        generatedXML
                                .writeText("HTTP/1.1 " + FxWebDavStatus.SC_LOCKED
                                        + " " + FxWebDavStatus
                                        .getStatusText(FxWebDavStatus.SC_LOCKED));
                        generatedXML.writeElement(null, "status",
                                XMLWriter.CLOSING);

                        generatedXML.writeElement(null, "response",
                                XMLWriter.CLOSING);
                    }

                    generatedXML.writeElement(null, "multistatus",
                            XMLWriter.CLOSING);

                    Writer writer = resp.getWriter();
                    writer.write(generatedXML.toString());
                    writer.close();

                    return;

                }

                boolean addLock = true;

                // Checking if there is already a shared lock on this path
                locksList = collectionLocks.elements();
                while (locksList.hasMoreElements()) {

                    LockInfo currentLock = (LockInfo) locksList.nextElement();
                    if (currentLock.path.equals(lock.path)) {

                        if (currentLock.isExclusive()) {
                            resp.sendError(FxWebDavStatus.SC_LOCKED);
                            return;
                        } else {
                            if (lock.isExclusive()) {
                                resp.sendError(FxWebDavStatus.SC_LOCKED);
                                return;
                            }
                        }

                        currentLock.tokens.addElement(lockToken);
                        lock = currentLock;
                        addLock = false;

                    }

                }

                if (addLock) {
                    lock.tokens.addElement(lockToken);
                    collectionLocks.addElement(lock);
                }

            } else {

                // Locking a single resource

                // Retrieving an already existing lock on that resource
                LockInfo presentLock = resourceLocks.get(lock.path);
                if (presentLock != null) {

                    if ((presentLock.isExclusive()) || (lock.isExclusive())) {
                        // If either lock is exclusive, the lock can't be
                        // granted
                        resp.sendError(FxWebDavStatus.SC_PRECONDITION_FAILED);
                        return;
                    } else {
                        presentLock.tokens.addElement(lockToken);
                        lock = presentLock;
                    }

                } else {

                    lock.tokens.addElement(lockToken);
                    resourceLocks.put(lock.path, lock);

                    // Checking if a resource exists at this path
                    exists = true;
                    try {
                        resources.lookup(path);
                    } catch (NamingException e) {
                        exists = false;
                    }
                    if (!exists) {

                        // "Creating" a lock-null resource
                        int slash = lock.path.lastIndexOf('/');
                        String parentPath = lock.path.substring(0, slash);

                        Vector<String> lockNulls = lockNullResources.get(parentPath);
                        if (lockNulls == null) {
                            lockNulls = new Vector<String>();
                            lockNullResources.put(parentPath, lockNulls);
                        }

                        lockNulls.addElement(lock.path);

                    }
                    // Add the Lock-Token header as by RFC 2518 8.10.1
                    // - only do this for newly created locks
                    resp.addHeader("Lock-Token", "<opaquelocktoken:"
                            + lockToken + ">");
                }

            }

        }

        if (lockRequestType == LOCK_REFRESH) {

            String ifHeader = req.getHeader("If");
            if (ifHeader == null)
                ifHeader = "";

            // Checking resource locks

            LockInfo toRenew = resourceLocks.get(path);
            Enumeration tokenList;
            if (lock != null) {

                // At least one of the tokens of the locks must have been given

                tokenList = toRenew.tokens.elements();
                while (tokenList.hasMoreElements()) {
                    String token = (String) tokenList.nextElement();
                    if (ifHeader.indexOf(token) != -1) {
                        toRenew.expiresAt = lock.expiresAt;
                        lock = toRenew;
                    }
                }

            }

            // Checking inheritable collection locks

            Enumeration collectionLocksList = collectionLocks.elements();
            while (collectionLocksList.hasMoreElements()) {
                toRenew = (LockInfo) collectionLocksList.nextElement();
                if (path.equals(toRenew.path)) {

                    tokenList = toRenew.tokens.elements();
                    while (tokenList.hasMoreElements()) {
                        String token = (String) tokenList.nextElement();
                        if (ifHeader.indexOf(token) != -1) {
                            toRenew.expiresAt = lock.expiresAt;
                            lock = toRenew;
                        }
                    }

                }
            }

        }

        // Set the status, then generate the XML response containing
        // the lock information
        XMLWriter generatedXML = new XMLWriter();
        generatedXML.writeXMLHeader();
        generatedXML.writeElement(null, "prop" + generateNamespaceDeclarations(), XMLWriter.OPENING);

        generatedXML.writeElement(null, "lockdiscovery", XMLWriter.OPENING);

        lock.toXML(generatedXML);

        generatedXML.writeElement(null, "lockdiscovery", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "prop", XMLWriter.CLOSING);

        resp.setStatus(FxWebDavStatus.SC_OK);
        resp.setContentType("text/xml; charset=UTF-8");
        Writer writer = resp.getWriter();
        writer.write(generatedXML.toString());
        writer.close();

    }

    */

    /**
     * Check to see if a resource is currently write locked. The method
     * will look at the "If" header to make sure the client
     * has give the appropriate lock tokens.
     *
     * @param req Servlet request
     * @return boolean true if the resource is locked (and no appropriate
     *         lock token has been found for at least one of the non-shared locks which
     *         are present on the resource).
     *
    private boolean isLocked(HttpServletRequest req) {

    String path = getRelativePath(req);

    String ifHeader = req.getHeader("If");
    if (ifHeader == null)
    ifHeader = "";

    String lockTokenHeader = req.getHeader("Lock-Token");
    if (lockTokenHeader == null)
    lockTokenHeader = "";

    return isLocked(path, ifHeader + lockTokenHeader);

    }   */

    /**
     * Check to see if a resource is currently write locked.
     *
     * @param path     Path of the resource
     * @param ifHeader "If" HTTP header which was included in the request
     * @return boolean true if the resource is locked (and no appropriate
     *         lock token has been found for at least one of the non-shared locks which
     *         are present on the resource).
     *
    private boolean isLocked(String path, String ifHeader) {

    // Checking resource locks

    LockInfo lock = resourceLocks.get(path);
    Enumeration tokenList;
    if ((lock != null) && (lock.hasExpired())) {
    resourceLocks.remove(path);
    } else if (lock != null) {

    // At least one of the tokens of the locks must have been given

    tokenList = lock.tokens.elements();
    boolean tokenMatch = false;
    while (tokenList.hasMoreElements()) {
    String token = (String) tokenList.nextElement();
    if (ifHeader.indexOf(token) != -1)
    tokenMatch = true;
    }
    if (!tokenMatch)
    return true;

    }

    // Checking inheritable collection locks

    Enumeration collectionLocksList = collectionLocks.elements();
    while (collectionLocksList.hasMoreElements()) {
    lock = (LockInfo) collectionLocksList.nextElement();
    if (lock.hasExpired()) {
    collectionLocks.removeElement(lock);
    } else if (path.startsWith(lock.path)) {

    tokenList = lock.tokens.elements();
    boolean tokenMatch = false;
    while (tokenList.hasMoreElements()) {
    String token = (String) tokenList.nextElement();
    if (ifHeader.indexOf(token) != -1)
    tokenMatch = true;
    }
    if (!tokenMatch)
    return true;

    }
    }

    return false;

    }
     */
}
