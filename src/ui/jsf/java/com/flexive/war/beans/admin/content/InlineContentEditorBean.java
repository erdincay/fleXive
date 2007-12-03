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
package com.flexive.war.beans.admin.content;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.ActionBean;
import com.flexive.faces.messages.FxFacesMessage;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import static com.flexive.shared.tree.FxTreeMode.Edit;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeNodeEdit;

import javax.faces.application.FacesMessage;
import java.io.Serializable;
import java.net.URLEncoder;

/**
 * This Bean is used for the frontpage inline editing editor.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class InlineContentEditorBean extends ContentEditorBean implements ActionBean, Serializable {

    private static final String REQ_ATR_INITIALIZED = InlineContentEditorBean.class.getName() + ".pageIsInitialized";
    private static final String PAGE_CLOSE_EDITOR = "iceClose";
    private Long pageId;
    private boolean doReload = true;
    private FxTreeNode workingNode;
    private FxTreeNode clipboardNode;
    private Long clipboardPage;
    private FxPK ancorPk;
    private boolean ancoreBefore;
    private CLIPBOARD_FUCTION clipboardFunction;
    private String reloadUrl;

    private static enum CLIPBOARD_FUCTION {
        CUT, COPY
    }


    /**
     * Returns the PK in the clipboard (used for cut/copy/paste function).
     *
     * @return the PK in the clipboard
     */
    public long getClipboardContentId() {
        return clipboardNode == null ? -1 : clipboardNode.getReference().getId();
    }

    /**
     * Returns the PK in the clipboard as string (used for cut/copy/paste function).
     *
     * @return the PK in the clipboard as string
     */
    public String getClipboardContentIdAsString() {
        return clipboardNode == null ? "" : String.valueOf(getClipboardContentId());
    }

    /**
     * The page that the content was cut/copied from.
     *
     * @return The page that the content was cut/copied from.
     */
    public Long getClipboardPage() {
        return clipboardPage;
    }

    /**
     * Returns the tree node stored in the clipboard (used for cut/copy/paste function).
     *
     * @return the tree node stored in the clipboard
     */
    public FxTreeNode getClipboardNode() {
        return clipboardNode;
    }

    /**
     * Returns true if the paste option is available.
     *
     * @return true if the paste option is available
     */
    public boolean getPasteEnabled() {
        return clipboardNode != null;
    }

    public InlineContentEditorBean() {
        super();
    }

    @SuppressWarnings("UnusedDeclaration")
    InlineContentEditorBean(boolean dummy) {
        super(dummy);
    }

    public static InlineContentEditorBean getSingleton() {
        return new InlineContentEditorBean(true);
    }

    public String getEditorType() {
        return "INLINE";
    }

    public void setEditorType(String editorType) {
        super.setEditorType(editorType);
    }

    @Override
    protected String getEditorPage() {
        return "inlineContentEditor";
    }

    @Override
    protected String getSessionCacheId() {
        return super.getSessionCacheId();
    }


    /**
     * This getter returns true exactly one time every request, and is used to write some
     * javascipts to the page on the first access.
     *
     * @return true exactly one time every request
     */
    public boolean getPageIsInitialized() {
        boolean isIni = FxJsfUtils.getRequest().getAttribute(REQ_ATR_INITIALIZED) != null;
        if (!isIni) {
            FxJsfUtils.getRequest().setAttribute(REQ_ATR_INITIALIZED, Boolean.TRUE);
        }
        return isIni;
    }

    @Override
    public String saveInSession() {
        return super.saveInSession();
    }

    @Override
    public String save() {
        setDoReload(true);
        super.save();
        // Update the position of the node
        try {
            if (ancorPk != null) {
                FxTreeNode treeNodeId = resolveNodeId(workingNode.getId(), getPk().getId());
                int ancorPos = resolveAncorPosition(workingNode, null, ancorPk.getId());
                if (ancoreBefore) {
                    ancorPos = ancorPos - 1;
                }
                tree.move(treeNodeId.getMode(), treeNodeId.getId()/*node to move*/, workingNode.getId()/*parent*/, ancorPos/*new pos*/);
            }
        } catch (Throwable t) {
            System.out.println("ContentInlineEditor: failed to position then node after save: " + t.getMessage());
        }
        //
        return hasError() ? null : PAGE_CLOSE_EDITOR;
    }


    public void setDoReload(boolean doReload) {
        this.doReload = doReload;
        setReloadUrl();
    }

    @Override
    public String saveInNewVersion() {
        setDoReload(true);
        super.saveInNewVersion();
        return null;
    }

    @Override
    public String delete() {
        setDoReload(true);
        super.delete();
        return hasError() ? null : PAGE_CLOSE_EDITOR;
    }

    private boolean hasError() {
        for (FxFacesMessage msg : FxJsfUtils.getFxMessages()) {
            if (msg.getSeverity() != FacesMessage.SEVERITY_INFO) return true;
        }
        return false;
    }

    @Override
    public String reload() {
        setDoReload(false);
        return super.reload();
    }

    @Override
    public String cancel() {
        setDoReload(false);
        super.cancel();
        return PAGE_CLOSE_EDITOR;
    }


    protected String release() {
        String result = super.release();
        pageId = null;
        ancorPk = null;
        workingNode = null;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParseRequestParameters() throws FxApplicationException {
        String action = FxJsfUtils.getParameter("action");
        String result = super.getParseRequestParameters();
        if (action == null) {
            // Do not handle unknown actions (ajax4jsf, ...)
            return result;
        }

        if (FxJsfUtils.hasParameter("page")) {
            pageId = FxJsfUtils.getLongParameter("page");
        }


        String sNode = (FxJsfUtils.hasParameter("node")) ? FxJsfUtils.getParameter("node") : ".";
        if (sNode.length() > 0 && !sNode.equals(".")) {
            String path = FxJsfUtils.getParameter("node");
            if (path.equals("..")) {
                FxTreeNode _node = tree.getNode(Edit, pageId); // TODO: edit/live!
                workingNode = tree.getNode(Edit, _node.getParentNodeId());
            } else {
                FxTreeNode node = tree.getTree(Edit, pageId, 1); // TODO: edit/live
                for (FxTreeNode child : node.getChildren()) {
                    if (child.getName().equalsIgnoreCase(path)) {
                        workingNode = child;
                        break;
                    }
                }
            }
        } else {
            workingNode = tree.getNode(Edit, pageId);
        }

        if ("editInstance".equals(action)) {
            // nothing
        } else if (workingNode == null) {
            // TODO: Error handling
            System.err.println("Error: Empty working node!!");
        } else {
            if ("newInstance".equals(action)) {
                addTreeNode(workingNode.getId());
                ancoreBefore = isBeforeAncore();
                ancorPk = getPkFromRParam("ancorPk");
            } else if ("copy".equals(action)) {
                FxPK clipboardPK = getPkFromRParam("pk");
                clipboardFunction = CLIPBOARD_FUCTION.COPY;
                clipboardNode = resolveNodeId(workingNode.getId(), clipboardPK.getId());
                clipboardPage = pageId;
                setDoReload(true);
            } else if ("cut".equals(action)) {
                FxPK clipboardPK = getPkFromRParam("pk");
                clipboardFunction = CLIPBOARD_FUCTION.CUT;
                clipboardNode = resolveNodeId(workingNode.getId(), clipboardPK.getId());
                clipboardPage = pageId;
                setDoReload(true);
            } else if ("paste".equals(action)) {
                doPaste();
            }

        }
        return result;
    }


    private boolean isBeforeAncore() {
        return !FxJsfUtils.hasParameter("beforeAncor") || Boolean.valueOf(FxJsfUtils.getParameter("beforeAncor"));
    }

    /**
     * Helper functions, reads a pk from a request parameter.
     *
     * @param param the parameter name that contains the pk
     * @return the pk, or null if not set
     */
    private FxPK getPkFromRParam(String param) {
        try {
            String split[] = FxJsfUtils.getParameter(param).split("\\.");
            long id = Long.valueOf(split[0]);
            int ver = Integer.valueOf(split[1]);
            return new FxPK(id, ver);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Pastes the pk in the clipboard to the desired position (defined by the ancor pl).
     */
    private void doPaste() throws FxApplicationException {
        setDoReload(true);
        FxTreeNode newParent = tree.getTree(Edit, workingNode.getId(), 1);
        FxPK ancorPk = getPkFromRParam("ancorPk");
        int ancorPos = 0;
        if (ancorPk != null) {
            long ancorContentId = getPkFromRParam("ancorPk").getId();
            ancorPos = resolveAncorPosition(newParent, clipboardNode, ancorContentId);
        }
        if (!isBeforeAncore()) {
            ancorPos = ancorPos + 1;
        }
        if (clipboardFunction == CLIPBOARD_FUCTION.CUT) {
            // Move the node to its new position
            tree.move(clipboardNode.getMode(), clipboardNode.getId()/*node to move*/, workingNode.getId()/*parent*/, ancorPos/*new pos*/);
        } else {
            // Create a new node that references the instance in the clipboard
            tree.save(FxTreeNodeEdit.createNewChildNode(workingNode).setName(String.valueOf(clipboardNode.getId())).setReference(clipboardNode.getReference()));
        }
        // Clear clipboard
        clipboardFunction = null;
        clipboardNode = null;
        clipboardPage = null;
    }


    /**
     * Returns the id of the treenode that references the contentInstanceId in
     * the current page.
     *
     * @param contentInstanceId the instance if to look for
     * @param rootNode          the root node to look in
     * @return the tree node
     * @throws FxApplicationException on errors
     */
    private FxTreeNode resolveNodeId(long rootNode, long contentInstanceId) throws FxApplicationException {
        FxTreeNode node = tree.getTree(Edit, rootNode, 1);
        if (node.getReference().getId() == contentInstanceId) {
            return node;
        } else {
            for (FxTreeNode child : node.getChildren()) {
                if (child.getReference().getId() == contentInstanceId) {
                    return child;
                }
            }
        }
        return null;
    }

    private int resolveAncorPosition(FxTreeNode newParent, FxTreeNode nodeToMove, long ancorContentId) throws FxApplicationException {
        int pos = 0;
        int offset = 0;
        // Check if the childs of the new Parent are loaded
        if (newParent.getDirectChildCount() > 0 && newParent.getChildren().size() == 0) {
            newParent = tree.getTree(Edit, newParent.getId(), 1);  // TODO: EDIT/LIVE
        }
        for (FxTreeNode child : newParent.getChildren()) {
            if (nodeToMove != null && child.getId() == nodeToMove.getId()) {
                offset += 1;
            }
            if (child.getReference().getId() == ancorContentId) {
                break;
            }
            pos++;
        }
        return pos - offset;
    }

    private void setReloadUrl() {
        try {
            // TODO: toggle edit / live!!
            String path = EJBLookup.getTreeEngine().getPaths(Edit, new long[]{pageId}).get(0);
            // TODO: this is a fake!!
            if (path.startsWith("/a1.net")) {
                path = path.substring("/a1.net".length());
            } else if (path.startsWith("/bob.at")) {
                path = path.substring("/bob.at".length());
            }
            this.reloadUrl = URLEncoder.encode(path, "UTF-8");
        } catch (Throwable t) {
            // eg content does no longer exist
            this.reloadUrl = "/";
        }
    }

    public String getReloadUrl() {
        return this.reloadUrl;
    }

    public boolean getReloadPageOnClose() {
        return doReload;
    }

    /**
     * Returns the active content editor beans of the calling jsf user session,
     * or null id none is available at the call time
     *
     * @return the content editor beans, or null
     */
    public ContentEditorBean getInstance() {
        return super.getInstance();
    }

}
