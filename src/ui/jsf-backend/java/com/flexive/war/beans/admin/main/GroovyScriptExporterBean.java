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

package com.flexive.war.beans.admin.main;

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.structure.export.StructureExporterCallback;
import com.flexive.shared.scripting.groovy.GroovyScriptExporter;

/**
 * Bean to access the GroovyScriptExporter
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GroovyScriptExporterBean {

    private StructureExporterCallback callback;
    private String scriptCode;
    private boolean deleteStructures = false;
    private GroovyScriptExporter exporter;
    private boolean generateImportStatements = true;
    private boolean defaultsOnly = false;
    private boolean generateScriptAssignments = true;
    private boolean scriptOverride = true;
    private boolean addSystemTypes = false;
    private boolean withoutDependencies = false;
    private boolean addWorkflow = false; // generate code f. workflow names
    private boolean reset;
    private boolean convertToUnicode = false;

    /**
     * Default constructor (use setters)
     */
    public GroovyScriptExporterBean() {
    }

    /**
     * @param callback                 the structureexporter callback interface
     * @param deleteStructures         set to true if code for strucuture deletion should be generated
     * @param generateImportStatements set to true if package imports should be added to the code
     */
    public GroovyScriptExporterBean(StructureExporterCallback callback, boolean deleteStructures, boolean generateImportStatements) {
        this.callback = callback;
        this.deleteStructures = deleteStructures;
        this.generateImportStatements = generateImportStatements;
    }

    /**
     * @return the actual Groovy script code
     */
    public String getScriptCode() {
        generateCode();
        return scriptCode;
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
    }

    /**
     * Generate the script code - uses the GroovyScriptExporter's run method and applies the boolean flags.
     * Unicode conversion is performed after script generation is completed.
     */
    private void generateCode() {
        if (exporter == null)
            exporter = new GroovyScriptExporter(callback).run(generateImportStatements, deleteStructures, generateScriptAssignments,
                    scriptOverride, defaultsOnly, addSystemTypes, withoutDependencies, addWorkflow, reset);
        else
            exporter.run(generateImportStatements, deleteStructures, generateScriptAssignments, scriptOverride, defaultsOnly,
                    addSystemTypes, withoutDependencies, addWorkflow, reset);

        if(convertToUnicode) {
            char start = 0x00A1; // character range
            char end = 0x02B8;
            scriptCode = FxFormatUtils.convertToJavaUnicode(exporter.getScriptCode(), start, end, null, null);
        } else
            scriptCode = exporter.getScriptCode();
        reset = false; // always reset "reset" after a new run
    }

    public boolean isDeleteStructures() {
        return deleteStructures;
    }

    public void setDeleteStructures(boolean deleteStructures) {
        this.deleteStructures = deleteStructures;
    }

    public StructureExporterCallback getCallback() {
        return callback;
    }

    public void setCallback(StructureExporterCallback callback) {
        this.callback = callback;
    }

    public boolean isGenerateImportStatements() {
        return generateImportStatements;
    }

    public void setGenerateImportStatements(boolean generateImportStatements) {
        this.generateImportStatements = generateImportStatements;
    }

    public boolean isDefaultsOnly() {
        return defaultsOnly;
    }

    public boolean isGenerateScriptAssignments() {
        return generateScriptAssignments;
    }

    public void setGenerateScriptAssignments(boolean generateScriptAssignments) {
        this.generateScriptAssignments = generateScriptAssignments;
    }

    public boolean isScriptOverride() {
        return scriptOverride;
    }

    public void setScriptOverride(boolean scriptOverride) {
        this.scriptOverride = scriptOverride;
    }

    public boolean isAddSystemTypes() {
        return addSystemTypes;
    }

    public void setAddSystemTypes(boolean addSystemTypes) {
        reset = true;
        this.addSystemTypes = addSystemTypes;
    }

    public boolean isAddWorkflow() {
        return addWorkflow;
    }

    public void setAddWorkflow(boolean addWorkflow) {
        this.addWorkflow = addWorkflow;
    }

    public boolean isWithoutDependencies() {
        return withoutDependencies;
    }

    public void setWithoutDependencies(boolean withoutDependencies) {
        this.withoutDependencies = withoutDependencies;
    }

    public boolean isConvertToUnicode() {
        return convertToUnicode;
    }

    public void setConvertToUnicode(boolean convertToUnicode) {
        this.convertToUnicode = convertToUnicode;
    }

    /**
     * @param defaultsOnly set to true if only the default GTB structure options should be exported
     */
    public void setDefaultsOnly(boolean defaultsOnly) {
        reset = true; // must be adjusted s.t. code can be regenerated
        this.defaultsOnly = defaultsOnly;
    }

    /**
     * Call this method to clear the exporter (in order to regenerate code)
     *
     * @return this itself for chained calls
     */
    public GroovyScriptExporterBean resetExporter() {
        exporter = null;
        callback = null;
        return this;
    }
}
