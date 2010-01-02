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
package com.flexive.cmis.spi;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import static com.flexive.shared.EJBLookup.getTreeEngine;
import com.flexive.shared.FxContext;
import com.flexive.shared.cmis.search.CmisResultRow;
import com.flexive.shared.cmis.search.CmisResultSet;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import org.apache.chemistry.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * The main facade for accessing flexive via Chemistry/CMIS APIs. The connection implements both the external
 * connection and the internal SPI interface, which currently leads to some overlap and awkward converisons
 * between calls.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveConnection implements Connection, SPI {
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_USE_SESSION_AUTH = "usesession";

    private static final Log LOG = LogFactory.getLog(FlexiveConnection.class);
    private final FlexiveRepositoryConfig config;
    private final PrivateWorkingCopyManager checkouts;
    private final Context context;
    private final Map<String, Serializable> parameters;

    public FlexiveConnection(FlexiveRepositoryConfig config, Map<String, Serializable> parameters) {
        this.config = config;
        this.checkouts = new PrivateWorkingCopyManager();
        this.context = new Context(config, checkouts);
        // TODO: hack to support authentication, remove when Chemistry adds support for this
        boolean loggedIn = false;
        this.parameters = parameters == null ? null : new HashMap<String, Serializable>(parameters);

        // perform authentication with credentials stored in parameters map
        if (this.parameters != null && this.parameters.containsKey(PARAM_USERNAME)) {
            try {
                final String username = (String) this.parameters.get(PARAM_USERNAME);
                if (!FxContext.getUserTicket().getLoginName().equalsIgnoreCase(username)) {
                    // login unless we're already logged in with this user
                    FxContext.get().login(username, (String) this.parameters.get(PARAM_PASSWORD), true);
                }
                loggedIn = true;
            } catch (FxLoginFailedException e) {
                throw new RuntimeException(e);
            } catch (FxAccountInUseException e) {
                // shouldn't happen because takeOver==true
                throw new RuntimeException(e);
            }
        }
        // when no username/password was specified, make sure that we actually run with guest privileges
        if (!loggedIn && FxContext.getUserTicket() != null && !FxContext.getUserTicket().isGuest() && !isUseSessionAuth()) {
            try {
                FxContext.get().logout();
            } catch (FxLogoutFailedException e1) {
                // fail hard, because we don't want to run with privileges that may have been left
                // from an unclosed connection
                throw new RuntimeException(e1);
            }
        }
    }

    private boolean isUseSessionAuth() {
        return parameters != null && parameters.containsKey(PARAM_USE_SESSION_AUTH);
    }

    public SPI getSPI() {
        return this;
    }

    public void close() {
        if (parameters == null || !isUseSessionAuth()) {
            // logout user
            try {
                FxContext.get().logout();
            } catch (FxLogoutFailedException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to logout user: " + e.getMessage(), e);
                }
            }
        }
    }

    public Repository getRepository() {
        return new FlexiveRepository(config.getContentStreamURI());
    }

    public Folder getRootFolder() {
        return new FlexiveFolder(context, FxTreeMode.Edit, FxTreeNode.ROOT_NODE);
    }

    public Document newDocument(String typeId, Folder folder) {
        return new FlexiveNewDocument(context, typeId, (FlexiveFolder) folder);
    }

    public Folder newFolder(String typeId, Folder folder) {
        return new FlexiveNewFolder(context, typeId, (FlexiveFolder) folder);
    }

    public Relationship newRelationship(String typeId) {
        throw new UnsupportedOperationException();
    }

    public Policy newPolicy(String typeId, Folder folder) {
        throw new UnsupportedOperationException();
    }

    public CMISObject getObject(ObjectId object) {
        return getContent(object);
    }

    public Collection<CMISObject> query(String statement, boolean searchAllVersions) {
        final Collection<ObjectEntry> entries = query(statement, searchAllVersions, true, true, true, -1, -1, null);
        // hack because query signatures are inconsistent
        final List<CMISObject> result = newArrayListWithCapacity(entries.size());
        for (ObjectEntry entry : entries) {
            result.add((CMISObject) entry);
        }
        return result;
    }

    public ObjectId newObjectId(final String id) {
        // TODO: not sure what to return here
        if (SPIUtils.isFolderId(id)) {
            return new FlexiveFolder(context, SPIUtils.getNodeId(id));
        } else {
            return new FlexiveDocument(context, SPIUtils.getDocumentId(id));
        }
    }

    public ObjectEntry newObjectEntry(String typeId) {
        final FxType type = CacheAdmin.getEnvironment().getType(SPIUtils.getFxTypeName(typeId));
        if (SPIUtils.getFolderTypeIds().contains(type.getId())) {
            return new FlexiveNewFolder(context, typeId, null);
        } else {
            return new FlexiveNewDocument(context, typeId, null);
        }
    }

    public List<ObjectEntry> getDescendants(ObjectId folder, int depth, String filter, boolean includeAllowableActions, boolean includeRelationships, boolean includeRenditions, String orderBy) {
        final List<CMISObject> children = new FlexiveFolder(context, SPIUtils.getNodeId(folder.getId())).getChildren(null, depth);
        final List<ObjectEntry> result = newArrayListWithCapacity(children.size());
        for (CMISObject child : children) {
            // HACK! (FlexiveFolder#getChildren always returns ObjectEntries)
            result.add((FlexiveObjectEntry) child);
        }
        return result;
    }

    public List<ObjectEntry> getChildren(ObjectId folder, String filter, boolean includeAllowableActions, boolean includeRelationships, boolean includeRenditions, int maxItems, int skipCount, String orderBy, boolean[] hasMoreItems) {
        final List<CMISObject> children = new FlexiveFolder(context, SPIUtils.getNodeId(folder.getId())).getChildren(null);
        final List<ObjectEntry> result = newArrayListWithCapacity(children.size());
        int row = 0;
        if (hasMoreItems != null) {
            hasMoreItems[0] = false;
        }
        for (CMISObject child : children) {
            row++;
            if (skipCount > 0 && row <= skipCount) {
                continue;
            }
            if (maxItems > 0 && result.size() == maxItems) {
                if (hasMoreItems != null) {
                    hasMoreItems[0] = true;
                }
                break;
            }
            // HACK! (FlexiveFolder#getChildren always returns ObjectEntries)
            result.add((FlexiveObjectEntry) child);
        }
        return result;
    }

    public ObjectEntry getFolderParent(ObjectId folder, String filter) {
        // HACK! (FlexiveFolder#getParent always returns ObjectEntries)
        return (ObjectEntry) new FlexiveFolder(context, SPIUtils.getNodeId(folder.getId())).getParent();
    }

    public Collection<ObjectEntry> getObjectParents(ObjectId object, String filter) {
        final Collection<Folder> parents = getContent(object).getParents();
        final List<ObjectEntry> result = newArrayListWithCapacity(parents.size());
        for (Folder parent : parents) {
            // HACK! (FlexiveDocument#getParents always returns ObjectEntries)
            result.add((FlexiveObjectEntry) parent);
        }
        return result;
    }

    public Collection<ObjectEntry> getCheckedOutDocuments(ObjectId folder, String filter, boolean includeAllowableActions, boolean includeRelationships, int maxItems, int skipCount, boolean[] hasMoreItems) {
        throw new UnsupportedOperationException();
    }

    public ObjectId createDocument(Map<String, Serializable> properties, ObjectId folder, ContentStream contentStream, VersioningState versioningState) {
        final Document doc = newDocument(
                (String) properties.get(VirtualProperties.TYPE_ID.getId()),
                folder == null ? null : getFolder(folder)
        );
        doc.setValues(properties);
        if (contentStream != null) {
            try {
                doc.setContentStream(contentStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // TODO: versioning state?
        doc.save();
        return doc;
    }

    public ObjectId createFolder(Map<String, Serializable> properties, ObjectId folder) {
        final Folder fold = newFolder(
                (String) properties.get(VirtualProperties.TYPE_ID.getId()),
                folder instanceof Folder
                ? (Folder) folder
                : new FlexiveFolder(context, SPIUtils.getNodeId(folder.getId())));
        fold.setValues(properties);
        fold.save();
        return fold;
    }

    public ObjectId createRelationship(Map<String, Serializable> properties) {
        throw new UnsupportedOperationException();
    }

    public ObjectId createPolicy(Map<String, Serializable> properties, ObjectId folder) {
        throw new UnsupportedOperationException();
    }

    public Collection<QName> getAllowableActions(ObjectId object) {
        return getContent(object).getAllowableActions().keySet();
    }

    public ObjectEntry getProperties(ObjectId object, String filter, boolean includeAllowableActions, boolean includeRelationships) {
        return getContent(object);
    }

    public boolean hasContentStream(ObjectId document) {
        try {
            return SPIUtils.isDocumentId(document.getId())
                    && getDocument(document).getContentStream() != null;
        } catch (IOException e) {
            LOG.error("Failed to get content stream: " + e.getMessage(), e);
            return false;
        }
    }

    public ObjectId setContentStream(ObjectId document, boolean overwrite, ContentStream contentStream) {
        // TODO: what to do if overwrite == false?
        try {
            final Document doc = getDocument(document);
            doc.setContentStream(contentStream);
            doc.save();
            return doc;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);  // TODO
        }
    }

    public Document deleteContentStream(ObjectId document) {
        try {
            final FlexiveDocument doc = getDocument(document);
            doc.setContentStream(null);
            return doc;
        } catch (IOException e) {
            throw new IllegalArgumentException(e); // TODO
        }
    }

    public ObjectId updateProperties(ObjectId object, String changeToken, Map<String, Serializable> properties) {
        // TODO: changeToken
        final CMISObject obj = getObject(object);
        obj.setValues(properties);
        obj.save();
        return obj;
    }

    public ObjectId moveObject(ObjectId object, ObjectId targetFolder, ObjectId sourceFolder) {
        // TODO: transaction support?
        final CMISObject obj = getObject(object);
        if (obj.getParents().size() > 1 && sourceFolder == null) {
            throw new IllegalArgumentException("Object " + object + " is multi-filed, but no source folder was specified.");
        }
        if (sourceFolder == null) {
            obj.unfile();
        }
        
        // add object to target folder
        final FlexiveFolder target = getFolder(targetFolder);
        target.add(obj);
        target.save();

        // remove object from source folder
        if (sourceFolder != null) {
            final FlexiveFolder src = getFolder(sourceFolder);
            src.remove(obj);
            src.save();
        }
        return newObjectId(obj.getId());    // force reload
    }

    public void deleteObject(ObjectId object) {
        getContent(object).delete();
    }

    public Collection<ObjectId> deleteTree(ObjectId folder, Unfiling unfiling, boolean continueOnFailure) {
        // TODO: continueOnFailure
        return getFolder(folder).deleteTree(unfiling);
    }

    public void addObjectToFolder(ObjectId object, ObjectId folder) {
        final FlexiveFolder dest = getFolder(folder);
        dest.add(getObject(object));
        dest.save();
    }

    public void removeObjectFromFolder(ObjectId object, ObjectId folder) {
        final FlexiveFolder dest = getFolder(folder);
        dest.remove(getObject(object));
        dest.save();
    }

    public Collection<ObjectEntry> query(String statement, boolean searchAllVersions, boolean includeAllowableActions, boolean includeRelationships, boolean includeRenditions, int maxItems, int skipCount, boolean[] hasMoreItems) {
        try {
            final CmisResultSet result = EJBLookup.getCmisSearchEngine().search(statement, true, skipCount, maxItems);
            final List<ObjectEntry> rows = newArrayListWithCapacity(result.getRowCount());
            for (CmisResultRow row : result) {
                rows.add(new FlexiveResultObject(context , row));
            }
            return rows;
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    public ObjectId checkOut(ObjectId document, boolean[] contentCopied) {
        if (contentCopied != null && contentCopied.length > 0) {
            contentCopied[0] = true;
        }
        if (checkouts.isCheckedOut(SPIUtils.getDocumentId(document.getId()))) {
            throw new IllegalArgumentException("Object " + document + " is already checked out.");
        }
        return newObjectId(
                checkouts.checkout(getDocument(document).getContent()).toString()
        );
    }

    public void cancelCheckOut(ObjectId document) {
        checkouts.cancelCheckout(SPIUtils.getDocumentId(document.getId()));
    }

    public ObjectId checkIn(ObjectId document, boolean major, Map<String, Serializable> properties, ContentStream contentStream, String comment) {
        final FlexiveDocument doc = getDocument(document);
        if (properties != null) {
            doc.setValues(properties);
        }
        return newObjectId(checkouts.checkin(doc.getContent()).toString());
    }

    public Map<String, Serializable> getPropertiesOfLatestVersion(String versionSeriesId, boolean majorVersion, String filter) {
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAllVersions(String versionSeriesId, String filter) {
        throw new UnsupportedOperationException();
    }

    public List<ObjectEntry> getRelationships(ObjectId object, RelationshipDirection direction, String typeId, boolean includeSubRelationshipTypes, String filter, String includeAllowableActions, int maxItems, int skipCount, boolean[] hasMoreItems) {
        throw new UnsupportedOperationException();
    }

    public void applyPolicy(ObjectId policy, ObjectId object) {
        throw new UnsupportedOperationException();
    }

    public void removePolicy(ObjectId policy, ObjectId object) {
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAppliedPolicies(ObjectId object, String filter) {
        throw new UnsupportedOperationException();
    }

    public Folder getFolder(String path) {
        try {
            // TODO: tree mode?
            return new FlexiveFolder(context, getTreeEngine().getIdByLabelPath(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, path));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    public List<ObjectEntry> getFolderTree(ObjectId folder, int depth, String filter, boolean includeAllowableActions) {
        throw new UnsupportedOperationException();
    }

    public ObjectEntry getObjectByPath(String path, String filter, boolean includeAllowableActions, boolean includeRelationships) {
        try {
            final long nodeId = getTreeEngine().getIdByLabelPath(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, path);
            if (nodeId == -1) {
                return null;
            }
            final FxTreeNode node = getTreeEngine().getNode(FxTreeMode.Edit, nodeId);
            if (SPIUtils.getFolderTypeIds().contains(node.getReferenceTypeId())) {
                return new FlexiveFolder(context, node);
            } else {
                return new FlexiveDocument(context, node, node.getParentNodeId());
            }
        } catch (FxNotFoundException e) {
            return null;
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    public List<Rendition> getRenditions(ObjectId object, String filter, int maxItems, int skipCount) {
        throw new UnsupportedOperationException();
    }

    public ContentStream getContentStream(ObjectId object, String contentStreamId) throws IOException {
        return getDocument(object).getContentStream(contentStreamId);
    }

    public void deleteObject(ObjectId object, boolean allVersions) {
        if (SPIUtils.isDocumentId(object.getId())) {
            getDocument(object).deleteAllVersions();
        } else {
            getObject(object).delete();
        }
    }

    public Iterator<ObjectEntry> getChangeLog(String changeLogToken, boolean includeProperties, int maxItems, boolean[] hasMoreItems, String[] lastChangeLogToken) {
        throw new UnsupportedOperationException();
    }

    public List<ACE> getACL(ObjectId object, boolean onlyBasicPermissions, boolean[] exact) {
        throw new UnsupportedOperationException();
    }

    public List<ACE> applyACL(ObjectId object, List<ACE> addACEs, List<ACE> removeACEs, ACLPropagation propagation, boolean[] exact, String[] changeToken) {
        throw new UnsupportedOperationException();
    }

    private FlexiveObjectEntry getContent(ObjectId object) {
        final String id = object.getId();
        if (object instanceof FlexiveDocument) {
            return (FlexiveDocument) object;
        } else if (object instanceof FlexiveFolder) {
            return (FlexiveFolder) object;
        } else if (SPIUtils.isFolderId(id)) {
            return new FlexiveFolder(context, SPIUtils.getNodeId(id));
        } else {
            FxPK pk = SPIUtils.getDocumentId(id);
            return new FlexiveDocument(context, pk);
        }
    }

    private FlexiveDocument getDocument(ObjectId object) {
        if (object instanceof FlexiveDocument) {
            return (FlexiveDocument) object;
        } else if (SPIUtils.isDocumentId(object.getId())) {
            return new FlexiveDocument(context, SPIUtils.getDocumentId(object.getId()));
        } else {
            throw new IllegalArgumentException("Object " + object + " is not a document.");
        }
    }

    private FlexiveFolder getFolder(ObjectId object) {
        if (object instanceof FlexiveFolder) {
            return (FlexiveFolder) object;
        } else if (SPIUtils.isFolderId(object.getId())) {
            return new FlexiveFolder(context, SPIUtils.getNodeId(object.getId()));
        } else {
            throw new IllegalArgumentException("Object " + object + " is not a folder.");
        }
    }

    public static class Context {
        private final FlexiveRepositoryConfig config;
        private final PrivateWorkingCopyManager pwcManager;

        public Context(FlexiveRepositoryConfig config, PrivateWorkingCopyManager pwcManager) {
            this.config = config;
            this.pwcManager = pwcManager;
        }

        public FlexiveRepositoryConfig getConfig() {
            return config;
        }

        public PrivateWorkingCopyManager getPwcManager() {
            return pwcManager;
        }
    }
}
