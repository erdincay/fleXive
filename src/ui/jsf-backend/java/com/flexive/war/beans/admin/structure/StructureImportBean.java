/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.war.beans.admin.structure;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.ActionBean;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import java.io.IOException;
import java.io.Serializable;

/**
 * Structure import
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class StructureImportBean implements ActionBean, Serializable {

    private static final Log LOG = LogFactory.getLog(StructureImportBean.class);

    private UploadedFile uploadContent;
    private String pasteContent;
    private String source;
    private boolean loadType;
    private long typeId;

    public String getParseRequestParameters() throws FxApplicationException {
        String action = FxJsfUtils.getParameter("action");
        if (StringUtils.isBlank(action)) {
            return null;
        } else if ("source".equals(action)) {
            setSource(FxJsfUtils.getParameter("source"));
        }
        return "";
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public boolean isLoadType() {
        return loadType;
    }

    public void setLoadType(boolean loadType) {
        this.loadType = loadType;
    }

    public long getTypeId() {
        return typeId;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    public String importType() {
        boolean ok = false;
        long id = -1;
        if (uploadContent != null && uploadContent.getSize() > 0) {
            try {
                id = performTypeImport(new String(uploadContent.getBytes(), "UTF-8"));
                ok = id > 0;
            } catch (IOException e) {
                new FxFacesMsgErr("Content.err.Exception", e.getMessage()).addToContext();
            }
        } else if (!StringUtils.isEmpty(pasteContent)) {
            id = performTypeImport(pasteContent);
            ok = id > 0;
        } else {
            new FxFacesMsgInfo("Content.nfo.import.noData").addToContext();
            ok = false;
        }
        if (ok) {
//            TypeEditorBean tedit = (TypeEditorBean)FxJsfUtils.getManagedBean("typeEditorBean");
//            tedit.editType(id);
//            tedit.setReloadStructureTree(true);
//            return "typeEditor";
            setLoadType(true);
            setTypeId(id);
        }
        return "structureImport";
    }

    /**
     * Perform the actual import
     *
     * @param typeXML type as XML
     * @return if > 0 the id, else its an error
     */
    private long performTypeImport(String typeXML) {
        try {
            return EJBLookup.getTypeEngine().importType(typeXML).getId();
        } catch (FxRuntimeException r) {
            new FxFacesMsgErr(r).addToContext();
            return -1;
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return -1;
        }
    }
}
