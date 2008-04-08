/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
import com.flexive.shared.EJBLookup;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import java.io.IOException;
import java.io.Serializable;

/**
 * Content import bean
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class ContentImportBean implements ActionBean, Serializable {

    private UploadedFile uploadContent;
    private String pasteContent;
    private long nodeId = -1;
    private String nodePath = "-";

    public String getParseRequestParameters() throws FxApplicationException {
        String action = FxJsfUtils.getParameter("action");
        if (StringUtils.isBlank(action)) {
            return null;
        }
        if ("importFromTree".equals(action)) {
            nodeId = Long.valueOf(FxJsfUtils.getParameter("nodeId"));
            System.out.println("Importing for nodeId " + nodeId);
        }
        return "";
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodePath() {
        try {
            if( nodeId > 0 )
                nodePath = EJBLookup.getTreeEngine().getPathById(FxTreeMode.Edit, nodeId);
        } catch (FxApplicationException e) {
            nodePath = e.getMessage();
        }
        return nodePath;
    }

    public UploadedFile getUploadContent() {
        return uploadContent;
    }

    public void setUploadContent(UploadedFile uploadContent) {
        this.uploadContent = uploadContent;
    }

    public String getPasteContent() {
        return pasteContent;
    }

    public void setPasteContent(String pasteContent) {
        this.pasteContent = pasteContent;
    }

    public String doImport() {
        FxPK pk = null;
        if (uploadContent != null && uploadContent.getSize() > 0) {
            System.out.println("Uploaded " + uploadContent.getSize() + " bytes for " + uploadContent.getName());
            try {
                pk = performImport(new String(uploadContent.getBytes(), "UTF-8"));
            } catch (IOException e) {
                System.out.println("IO-Error: " + e.getMessage());
            }
            System.out.println("Saved as PK: " + pk);
        } else if (!StringUtils.isEmpty(pasteContent)) {
            pk = performImport(pasteContent);
            System.out.println("Saved as PK: " + pk);
        } else {
            System.out.println("Nothing found to import!");
        }
        return "";
    }

    /**
     * Perform the actual import
     *
     * @param content content as XML
     * @return pk
     */
    private FxPK performImport(String content) {
        try {
            FxPK pk =  EJBLookup.getContentEngine().importContent(content, true);
            if( nodeId > 0 )
                EJBLookup.getTreeEngine().save(FxTreeNodeEdit.createNewChildNode(EJBLookup.getTreeEngine().getNode(FxTreeMode.Edit, nodeId)).setReference(pk));
            return pk;
        } catch (FxApplicationException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
