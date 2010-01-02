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
package com.flexive.chemistry.webdav;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.ConflictException;
import org.apache.chemistry.AllowableAction;
import org.apache.chemistry.CMISObject;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import static com.flexive.chemistry.webdav.AuthenticationFilter.getConnection;
import com.flexive.chemistry.webdav.extensions.CopyDocumentExtension;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Date;

import sun.util.LocaleServiceProviderPool;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class ObjectResource<T extends CMISObject>
        implements GetableResource, PropFindableResource, MoveableResource, DeletableResource, CopyableResource {
    private static final Log LOG = LogFactory.getLog(ObjectResource.class);

    /** The security realm for HTTP authentication */
    public static final String SECURITY_REALM = "WebDAV";
    
    /** Max age for contents retrieved by the guest user (10 days) */
    private static final long MAX_AGE_GUEST = 10L*24*60*60;
    /** Max age for contents retrieved by a logged in user (10 minutes) */
    private static final long MAX_AGE_USER = 10L*60;

    protected final ChemistryResourceFactory resourceFactory;
    protected final T object;
    protected final String path;

    protected ObjectResource(ChemistryResourceFactory resourceFactory, String path, T object) {
        this.resourceFactory = resourceFactory;
        this.path = path;
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public String getUniqueId() {
        return object.getId();
    }

    /**
     * {@inheritDoc}
     */
    public Object authenticate(String user, String password) {
        // indicate that the user is already logged in. The authentication is performed in
        // AuthenticationFilter because at this place, the resource resolution has already
        // happened 
        return "authenticated";
    }

    /**
     * {@inheritDoc}
     */
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        final Collection<QName> actions = getConnection().getSPI().getAllowableActions(object);
        switch (method) {
            case GET:
            case PROPFIND:
                return true;    // since we already loaded it, we can also read
            case PUT:
                return actions.contains(AllowableAction.CAN_CREATE_DOCUMENT)
                        || actions.contains(AllowableAction.CAN_CREATE_FOLDER)
                        || actions.contains(AllowableAction.CAN_ADD_OBJECT_TO_FOLDER);
            case POST:
            case PROPPATCH:
                return actions.contains(AllowableAction.CAN_UPDATE_PROPERTIES);
            case DELETE:
                return actions.contains(AllowableAction.CAN_DELETE_OBJECT);
            case LOCK:
                return actions.contains(AllowableAction.CAN_CHECK_OUT);
            case UNLOCK:
                return actions.contains(AllowableAction.CAN_CANCEL_CHECK_OUT)
                        || actions.contains(AllowableAction.CAN_CHECK_IN);
            case MOVE:
                return actions.contains(AllowableAction.CAN_MOVE_OBJECT);
            default:
                return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getRealm() {
        return SECURITY_REALM; // TODO?
    }

    /**
     * {@inheritDoc}
     */
    public String checkRedirect(Request request) {
        return null;        // TODO
    }

    /**
     * {@inheritDoc}
     */
    public Long getMaxAgeSeconds(Auth auth) {
        return MAX_AGE_GUEST;    
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return object.getName();
    }

    /**
     * {@inheritDoc}
     */
    public Date getModifiedDate() {
        return object.getLastModificationDate().getTime();
    }

    /**
     * {@inheritDoc}
     */
    public Date getCreateDate() {
        return object.getCreationDate().getTime();
    }


    /**
     * {@inheritDoc}
     */
    public void moveTo(CollectionResource rDest, String name) throws ConflictException {
        final com.flexive.chemistry.webdav.FolderResource target = (com.flexive.chemistry.webdav.FolderResource) rDest;
        final String oldName = object.getName();
        boolean done = false;
        try {
            if (isOverwriteRequest()) {
                final ObjectResource targetRes = (ObjectResource) resourceFactory.getResource(null, target.getChildPath(name));
                if (targetRes != null) {
                    try {
                        targetRes.delete();
                    } catch (Exception e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Failed to remove " + target.getChildPath(name) + ": " + e.getMessage(), e);
                        }
                        // try to move the object anyway...
                    }
                }
            }
            object.setName(name);
            object.save();      // save name change (TODO: cannot move *and* set name in one operation)
            if (!target.getObject().getId().equals(object.getParent().getId())) {
                object.move(target.getObject(), object.getParent());
            }
            done = true;
        } finally {
            if (!done) {
                object.setName(oldName);    // restore object name in case of errors
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void copyTo(CollectionResource toCollection, String name) {
        copyObject(toCollection, name, isOverwriteRequest());
    }

    protected void copyObject(CollectionResource toCollection, String name, boolean overwrite) {
        // get extension for direct copying
        final CopyDocumentExtension cde = getConnection().getRepository().getExtension(CopyDocumentExtension.class);
        if (cde == null) {
            throw new UnsupportedOperationException("Copy not supported, please implement "
                    + CopyDocumentExtension.class.getCanonicalName());
        }
        // perform copy and add to target folder
        cde.copy(getConnection(), object, ((FolderResource) toCollection).getObject(), name, overwrite, getDepthHeader() == 0);
    }

    protected boolean isOverwriteRequest() {
        final String overwriteFlag = getRequestHeader("overwrite");
        return "T".equalsIgnoreCase(overwriteFlag);
    }

    protected int getDepthHeader() {
        return HttpManager.request() != null ? HttpManager.request().getDepthHeader() :  AbstractRequest.INFINITY;
    }

    protected String getRequestHeader(String name) {
        return HttpManager.request() != null
                ? HttpManager.request().getHeaders().get(name)
                : null;
    }

    protected String getChildPath(String childName) {
        return path + (path.endsWith("/") ? "" : "/") + childName;
    }
}
