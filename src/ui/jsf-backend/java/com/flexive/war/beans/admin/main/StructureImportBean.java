/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.ActionBean;
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Formatter;

/**
 * Structure import
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class StructureImportBean implements ActionBean, Serializable {
    private static final long serialVersionUID = 1763704615519844541L;

    private UploadedFile uploadContent;
    private String pasteContent;
    private String source;
    private boolean groovyImport = true;
    private transient Object result;
    private long executionTime;
    private String userLang = "en";
    private String toggleEditor = "onload";
    private boolean activateEditor = true;

    /**
     * {@inheritDoc}
     */
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

    public String getUserLang() {
        userLang = FxContext.getUserTicket().getLanguage().getIso2digit();
        return userLang;
    }

    public void setUserLang(String userLang) {
        this.userLang = userLang;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    /**
     * Action method for the Groovy script / xml type import
     *
     * @return returns the structureImport page
     */
    public String importType() {
        boolean ok = false;
        long id = -1;

        if (groovyImport) { // call the Groovy console
            if (uploadContent != null && uploadContent.getSize() > 0) {
                try {
                    ok = groovyUpload(new String(uploadContent.getBytes(), "UTF-8"));
                } catch (IOException e) {
                    new FxFacesMsgErr("Content.err.Exception", e.getMessage()).addToContext();
                }
            } else if (!StringUtils.isBlank(pasteContent)) {
                ok = groovyUpload(pasteContent);
            } else {
                new FxFacesMsgErr("Script.err.noCodeProvided").addToContext();
            }
        } else { // XML import
            if (uploadContent != null && uploadContent.getSize() > 0) {
                try {
                    // remove <export></export> tags
                    final String upload = new String(uploadContent.getBytes(), "UTF-8").replace("<export>", "").replace("</export", "");
                    id = performTypeImport(upload);
                    ok = id > 0;
                } catch (IOException e) {
                    new FxFacesMsgErr("Content.err.Exception", e.getMessage()).addToContext();
                }
            } else if (!StringUtils.isEmpty(pasteContent)) {
                // remove <export></export> tags
                id = performTypeImport(pasteContent.replace("<export>", "").replace("</export", ""));
                ok = id > 0;
            } else {
                new FxFacesMsgInfo("Content.nfo.import.noData").addToContext();
                ok = false;
            }
        }

        if (ok && !groovyImport) {
            final String typeName = "\"" + CacheAdmin.getEnvironment().getType(id).getName() + "\"";
            result = MessageBean.getInstance().getMessage("StructureImport.msg.importOK") + typeName;
        } else if (ok && groovyImport) {
            result = MessageBean.getInstance().getMessage("StructureImport.msg.groovyImportOK") + result;
        }

        return "structureImport";
    }

    /**
     * Refactorisation of the groovy upload
     *
     * @param code the script code
     * @return returns true if the import / running the script went ok
     */
    private boolean groovyUpload(String code) {
        final String fileName = "importConsole.groovy";
        long start = System.currentTimeMillis();

        try {
            result = ScriptConsoleBean.runScript(code, fileName, false);
            return true;
        } catch (Throwable t) {
            final StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            final String msg = t.getCause() != null ? t.getCause().getMessage() : t.getMessage();
            result = new Formatter().format("Exception caught: %s%n%s", msg, writer.getBuffer());
            return false;
        } finally {
            executionTime = System.currentTimeMillis() - start;
        }
    }

    /**
     * Perform the actual import for types in XML format
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

    public boolean isGroovyImport() {
        return groovyImport;
    }

    public void setGroovyImport(boolean groovyImport) {
        this.groovyImport = groovyImport;
    }

    /**
     * Verifies the syntax of a given groovy script
     */
    public void checkScriptSyntax() {
        if (uploadContent != null && uploadContent.getSize() > 0) {
            try {
                ScriptBean.checkScriptSyntax("dummyName.groovy", new String(uploadContent.getBytes(), "UTF-8"));
            } catch (IOException e) {
                new FxFacesMsgErr("Content.err.Exception", e.getMessage()).addToContext();
            }
        } else
            ScriptBean.checkScriptSyntax("dummyName.groovy", pasteContent);

        result = null; // reset
    }


    public String getToggleEditor() {
        return toggleEditor;
    }

    public void setToggleEditor(String toggleEditor) {
        this.toggleEditor = toggleEditor;
    }

    public boolean isActivateEditor() {
        return activateEditor;
    }

   /**
     * Activate the editor and also control the "toggleEditor" variable f. editarea
     *
     * @param activateEditor flag
     */
    public void setActivateEditor(boolean activateEditor) {
        toggleEditor = activateEditor ? "onload" : "later";
        this.activateEditor = activateEditor;
    }
}
