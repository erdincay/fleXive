/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.Role;
import com.flexive.faces.messages.FxFacesMsgErr;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Serializable;
import java.util.Formatter;

/**
 * A simple groovy console for testing (war-layer) groovy scripts.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ScriptConsoleBean implements Serializable {
    private static final long serialVersionUID = -938270211808872146L;

    /**
     * Session key to store the last inserted code
     */
    private static final String SESSION_LASTCODE = "__FXLASTNCODE__";

    private String code;
    private long executionTime;
    private boolean web;
    private String language = "groovy"; // initial setting for script syntax check
    private boolean verifyButtonEnabled = false;
    private String userLang = "en";
    private String toggleEditor = "onload";
    private boolean activateEditor = true; // edit area switch

    private transient Object result;
    
    public String getCode() {
        if(code == null) {
            code = (String) FxJsfUtils.getSessionAttribute(SESSION_LASTCODE);
            if(code == null || code.trim().length() == 0){
                code = ScriptBean.getClassImports(language);
            }
        }
        return code;
    }

    public void setCode(String code) {
        FxJsfUtils.setSessionAttribute(SESSION_LASTCODE, code);
        this.code = code;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public boolean isWeb() {
        return web;
    }

    public void setWeb(boolean web) {
        this.web = web;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * Runs the given script code
     */
    public void runScript() {
        if (StringUtils.isBlank(code))
            new FxFacesMsgErr("Script.err.noCodeProvided").addToContext();
        else {
            long start = System.currentTimeMillis();
            try {
                result = runScript(code, "console." + language, web);
            } catch (Throwable t) {
                final StringWriter writer = new StringWriter();
                t.printStackTrace(new PrintWriter(writer));
                final String msg = t.getCause() != null ? t.getCause().getMessage() : t.getMessage();
                result = new Formatter().format("Exception caught: %s%n%s", msg, writer.getBuffer());
            } finally {
                executionTime = System.currentTimeMillis() - start;
            }
        }
    }

    /**
     * Static refactorisation to run scripts
     *
     * @param code       the given script code
     * @param scriptName the script's name
     * @param web        run at web layer?
     * @return Object the result
     * @throws FxApplicationException on errors
     */
    static Object runScript(String code, String scriptName, boolean web) throws FxApplicationException {
        Object result;
        if (web && FxSharedUtils.isGroovyScript(scriptName)) {
            if (!FxContext.getUserTicket().isInRole(Role.ScriptExecution))
                result = "No permission to execute scripts!";
            else {
                GroovyShell shell = new GroovyShell();
                Script script = shell.parse(code);
                result = script.run();
            }
        } else {
            result = EJBLookup.getScriptingEngine().runScript(scriptName, null, code).getResult();
        }
        return result;
    }

    /**
     * @param verifyButtonEnabled Sets the boolean value for verifyButtonEnabled
     */
    public void setVerifyButtonEnabled(boolean verifyButtonEnabled) {
        this.verifyButtonEnabled = verifyButtonEnabled;
    }

    /**
     * @return Returns true if the current selected scripting language is "Groovy"
     */
    public boolean isVerifyButtonEnabled() {
        return verifyButtonEnabled = FxSharedUtils.isGroovyScript("console." + language);
    }

    /**
     * Verifies the syntax of a given groovy script
     */
    public void checkScriptSyntax() {
        ScriptBean.checkScriptSyntax("dummyName.groovy", getCode());
    }

    /**
     * Adds the default [fleXive] imports for a given script
     * (Configured in Script.properties and Script_de.properties: Script.defaultImports.[script extension],
     * e.g. "Script.defaultImports.groovy")
     */
    public void addDefaultImports() {
        if (code == null)
            code = "";
        code = ScriptBean.getClassImports(language) + code;
    }

    /**
     * @param userLang set the current user's language
     */
    public void setUserLang(String userLang) {
        this.userLang = userLang;
    }

    /**
     * @return returns the current user's language
     */
    public String getUserLang() {
        userLang = FxContext.getUserTicket().getLanguage().getIso2digit();
        return userLang;
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