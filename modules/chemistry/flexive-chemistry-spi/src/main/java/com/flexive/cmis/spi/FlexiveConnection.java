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

import static com.flexive.cmis.spi.ListPageUtils.*;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.cmis.search.CmisResultRow;
import com.flexive.shared.cmis.search.CmisResultSet;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.google.common.collect.Maps;
import org.apache.chemistry.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.flexive.shared.EJBLookup.getCmisSearchEngine;
import static com.flexive.shared.EJBLookup.getTreeEngine;
import static com.google.common.collect.Lists.newArrayListWithCapacity;

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
    /**
     * Return documents as folders if they have children (e.g. for filebrowsing
     * contexts via WebDAV).
     */
    public static final String PARAM_DOCS_FOLDERS = "docs_as_folders";

    private static final Log LOG = LogFactory.getLog(FlexiveConnection.class);
    private final FlexiveRepositoryConfig repositoryConfig;
    private final ConnectionConfig config;
    private final PrivateWorkingCopyManager checkouts;
    private final Context context;
    private final Map<String, Serializable> parameters;

    FlexiveConnection(FlexiveRepositoryConfig repositoryConfig, Map<String, Serializable> parameters) {
        this.parameters = parameters == null ? null : new HashMap<String, Serializable>(parameters);
        this.repositoryConfig = repositoryConfig;
        this.config = new ConnectionConfig(parameters);
        this.checkouts = new PrivateWorkingCopyManager();
        // TODO: connection escapes here
        this.context = new Context(this,  checkouts);
        // TODO: hack to support authentication, remove when Chemistry adds support for this
        boolean loggedIn = false;

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
        return new FlexiveRepository(repositoryConfig.getContentStreamURI());
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
        final Collection<ObjectEntry> entries = query(statement, searchAllVersions, null, null);
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

    public ObjectId createDocumentFromSource(ObjectId source, ObjectId folder, Map<String, Serializable> properties, VersioningState versioningState) throws NameConstraintViolationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public Tree<ObjectEntry> getDescendants(ObjectId folder, int depth, String orderBy, Inclusion inclusion) {
        // TODO orderBy?
        return getFolder(folder).getDescendantTree(depth);
    }

    public ListPage<ObjectEntry> getChildren(ObjectId folder, Inclusion inclusion, String orderBy, Paging paging) {
        final List<CMISObject> children = new FlexiveFolder(context, SPIUtils.getNodeId(folder.getId())).getChildren(null);
        return page(children, paging, new Function<CMISObject, ObjectEntry>() {
            public ObjectEntry apply(CMISObject value) {
                // HACK! (FlexiveFolder#getChildren always returns ObjectEntries)
                return (FlexiveObjectEntry) value;
            }
        });
    }

    public ObjectEntry getFolderParent(ObjectId folder, String filter) {
        // HACK! (FlexiveFolder#getParent always returns ObjectEntries)
        return new FlexiveFolder(context, SPIUtils.getNodeId(folder.getId())).getParent();
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

    public ListPage<ObjectEntry> getCheckedOutDocuments(ObjectId folder, Inclusion inclusion, Paging paging) {
        throw new UnsupportedOperationException();
    }

    public ObjectId createDocument(Map<String, Serializable> properties, ObjectId folder, ContentStream contentStream, VersioningState versioningState) throws NameConstraintViolationException {
        final Document doc = newDocument(
                (String) properties.get(VirtualProperties.TYPE_ID.getId()),
                folder == null ? null : getFolder(folder)
        );
        try {
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
        } catch (UpdateConflictException e) {
            throw new IllegalStateException("Unexpected update error during document creation: " + e.getMessage(), e);
        }
        return doc;
    }

    public ObjectId createFolder(Map<String, Serializable> properties, ObjectId folder) throws NameConstraintViolationException {
        final Folder fold = newFolder(
                (String) properties.get(VirtualProperties.TYPE_ID.getId()),
                folder instanceof Folder
                ? (Folder) folder
                : new FlexiveFolder(context, SPIUtils.getNodeId(folder.getId())));
        try {
            fold.setValues(properties);
            fold.save();
        } catch (UpdateConflictException e) {
            throw new IllegalStateException("Unexpected update error during folder creation: " + e.getMessage(), e);
        }
        return fold;
    }

    public ObjectId createRelationship(Map<String, Serializable> properties) {
        throw new UnsupportedOperationException();
    }

    public ObjectId createPolicy(Map<String, Serializable> properties, ObjectId folder) {
        throw new UnsupportedOperationException();
    }

    public Set<QName> getAllowableActions(ObjectId object) {
        return getContent(object).getAllowableActions();
    }

    public ObjectEntry getProperties(ObjectId object, Inclusion inclusion) {
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

    public ObjectId setContentStream(ObjectId document, ContentStream contentStream, boolean overwrite) throws IOException, ContentAlreadyExistsException, UpdateConflictException {
        // TODO: what to do if overwrite == false?
        try {
            final Document doc = getDocument(document);
            doc.setContentStream(contentStream);
            doc.save();
            return doc;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);  // TODO
        } catch (NameConstraintViolationException e) {
            throw new ContentAlreadyExistsException(e); // TODO ?
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

    public ObjectId updateProperties(ObjectId object, String changeToken, Map<String, Serializable> properties) throws UpdateConflictException, NameConstraintViolationException {
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

    public ListPage<ObjectEntry> query(String statement, boolean searchAllVersions, Inclusion inclusion, Paging paging) {
        try {
            final CmisResultSet rs = getCmisSearchEngine().search(
                    statement, true, getSkipCount(paging),
                    getMaxItems(paging) + 1     // select one more row to check if there are more of them
            );
            return page(rs, new Paging(getMaxItems(paging), 0), new Function<CmisResultRow, ObjectEntry>() {
                public ObjectEntry apply(CmisResultRow from) {
                    return new FlexiveResultObject(context, from);
                }
            });
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

    public ObjectId checkIn(ObjectId document, Map<String, Serializable> properties, ContentStream contentStream, boolean major, String comment) throws UpdateConflictException {
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
            final long nodeId = getTreeEngine().getIdByLabelPath(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, path);
            if (nodeId == -1) {
                return null;
            }
            final FxTreeNode node = getTreeEngine().getNode(FxTreeMode.Edit, nodeId);
            if (SPIUtils.getFolderTypeIds().contains(node.getReferenceTypeId())) {
                // this is a valid folder
                return new FlexiveFolder(context, node);
            } else {
                // conform to BasicTestCasetestGetObjectByPath
                throw new IllegalArgumentException(path + " is not a folder.");
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    public Tree<ObjectEntry> getFolderTree(ObjectId folder, int depth, Inclusion inclusion) {
        return getFolder(folder).getFolderTree(depth);
    }

    public ObjectEntry getObjectByPath(String path, Inclusion inclusion) {
        try {
            final long nodeId = getTreeEngine().getIdByLabelPath(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, path);
            if (nodeId == -1) {
                return null;
            }
            final FxTreeNode node = getTreeEngine().getNode(FxTreeMode.Edit, nodeId);
            if (SPIUtils.treatDocumentAsFolder(SPIUtils.getFolderTypeIds(), context, node)) {
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


    public List<Rendition> getRenditions(ObjectId object, Inclusion inclusion, Paging paging) {
        throw new UnsupportedOperationException();
    }

    public ContentStream getContentStream(ObjectId object, String contentStreamId) throws IOException {
        return getDocument(object).getContentStream(contentStreamId);
    }

    public void deleteObject(ObjectId object, boolean allVersions) throws UpdateConflictException {
        if (allVersions && SPIUtils.isDocumentId(object.getId())) {
            getDocument(object).deleteAllVersions();
        } else {
            getObject(object).delete();
        }
    }

    public ListPage<ObjectEntry> getChangeLog(String changeLogToken, boolean includeProperties, Paging paging, String[] latestChangeLogToken) {
        throw new UnsupportedOperationException();
    }

    public List<ACE> getACL(ObjectId object, boolean onlyBasicPermissions, boolean[] exact) {
        throw new UnsupportedOperationException();
    }

    public List<ACE> applyACL(ObjectId object, List<ACE> addACEs, List<ACE> removeACEs, ACLPropagation propagation, boolean[] exact, String[] changeToken) {
        throw new UnsupportedOperationException();
    }

    public ListPage<ObjectEntry> getRelationships(ObjectId object, String typeId, boolean includeSubRelationshipTypes, Inclusion inclusion, Paging paging) {
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


    static class Context {
        private final PrivateWorkingCopyManager pwcManager;
        private final FlexiveConnection connection;

        Context(FlexiveConnection connection, PrivateWorkingCopyManager pwcManager) {
            this.connection = connection;
            this.pwcManager = pwcManager;
        }

        public FlexiveRepositoryConfig getRepositoryConfig() {
            return connection.repositoryConfig;
        }

        public PrivateWorkingCopyManager getPwcManager() {
            return pwcManager;
        }

        public FlexiveConnection getConnection() {
            return connection;
        }

        public ConnectionConfig getConnectionConfig() {
            return connection.config;
        }

    }

    static class ConnectionConfig {
        private final boolean documentsAsFolders;

        ConnectionConfig(Map<String, Serializable> parameters) {
            if (parameters != null) {
                this.documentsAsFolders = Boolean.TRUE.equals(parameters.get(PARAM_DOCS_FOLDERS));
            } else {
                // default configuration
                this.documentsAsFolders = false;
            }
        }

        public boolean isDocumentsAsFolders() {
            return documentsAsFolders;
        }
    }
}
