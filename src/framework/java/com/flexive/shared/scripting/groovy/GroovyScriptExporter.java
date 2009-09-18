/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/

package com.flexive.shared.scripting.groovy;

import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxGroupAssignment;
import com.flexive.shared.structure.export.StructureExporterCallback;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.CacheAdmin;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * GroovyScriptExporter - generate Groovy structure creation code from a given StructureExporterCallback instance
 * Utilises GroovyScriptExporterTools as the utility class to generate the script code
 * <p><b>Usage<b/><br/></p>
 * - Create a new GroovyScriptExporter object providing a StructureExporterCallback interface obj:<br/>
 * GroovyScriptExporter exporter = new GroovyScriptExporter(callback);<br/>
 * - Either use the setters to provide the exporter with the relevant (boolean) options:<br/>
 * generateImportStatements: set to true if the script code should contain package import statements<br/>
 * generateDeleteCode: set to true if the script code should contain content instance / structure deletion code for all
 * affected exported nodes<br/>
 * generateScriptAssignments: set to true if the script code should contain script assignments found during the export<br/>
 * defaultsOnly: set to true if the structure creation code should only contain the assignment names (using GroovyTypeBuilder defaults)<br/><br/>
 * addRoot: add the ROOT node to the generated script code
 * <p/>
 * run(..) methods:<br/>
 * run() .. generates the script code (use setters for options)<br/>
 * run(boolean reset) .. create the script code and optionally (reset = true will regenerate the script code if the run(...) method was invoked
 * beforehand<br/>
 * run(generateImportStatements, generateDeleteCode, generateScriptAssignments, defaultsOnly, addRoot, reset) --> this is the shorthand
 * method if you want to avoid using the setters<br/><br/>
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GroovyScriptExporter {

    private StructureExporterCallback callback;
    private StringBuilder deleteCode; // holds instance / structure deletion code
    private StringBuilder scriptCode; // holds the structure creation code
    private StringBuilder scriptAssignments; // holds the code for creating script assignments
    private String importStatements = ""; // holds the import statements
    private StringBuilder scriptCodeOnly = new StringBuilder(2000); // holds the script code ONLY
    private boolean generateImportStatements = false;
    private boolean generateDeleteCode = false;
    private boolean generateScriptAssignments = false;
    private boolean defaultsOnly = false;
    private boolean addSystemTypes = false;
    private static final Log LOG = LogFactory.getLog(GroovyScriptExporter.class);
    private Set<FxType> filteredTypes;

    public GroovyScriptExporter(StructureExporterCallback callback) {
        this.callback = callback;
    }

    /**
     * main method to perform script code generation (use setters to override def. options)
     *
     * @param reset set to true in order to regenerate any code
     * @return the GroovyScriptExporter itself for chained calls
     */
    public GroovyScriptExporter run(boolean reset) {
        generateScriptCode(reset);
        return this;
    }

    /**
     * main method to perform script code generation
     *
     * @param generateImportStatements  set to true to add package imports
     * @param generateDeleteCode        set to true to add structure / instance deletion code
     * @param generateScriptAssignments set to true to add script events for types / assignments
     * @param defaultsOnly              set to true if the script export should disregard any existing options and use the defaults only
     * @param addSystemTypes            set to true if the script export should include the [fleXive] system types
     * @param reset                     set to true in order to regenerate any code
     * @return the GroovyScriptExporter itself for chained calls
     */
    public GroovyScriptExporter run(boolean generateImportStatements, boolean generateDeleteCode, boolean generateScriptAssignments,
                                    boolean defaultsOnly, boolean addSystemTypes, boolean reset) {
        this.generateImportStatements = generateImportStatements;
        this.generateDeleteCode = generateDeleteCode;
        this.generateScriptAssignments = generateScriptAssignments;
        this.defaultsOnly = defaultsOnly;
        this.addSystemTypes = addSystemTypes;
        filteredTypes = filterExportTypes();
        generateScriptCode(reset);
        return this;
    }

    /**
     * Add content instance / structure deletion code
     *
     * @param reset true = re-generate deletion code
     */
    private void createDeleteCode(boolean reset) {
        if (deleteCode == null || reset) {
            deleteCode = new StringBuilder(1000);
            final Set<FxType> types = filterExportTypes();
            deleteCode.append(GroovyScriptExporterTools.DELHEADER)
                    .append(GroovyScriptExporterTools.createDeleteCode(new ArrayList<FxType>(types)));
            deleteCode.trimToSize();
        }
    }

    /**
     * Add type scripts / assignment scripts code
     *
     * @param reset true = re-generate script assignment code
     */
    private void createScriptAssignmentCode(boolean reset) {
        if (callback.getTypeScriptMapping().size() > 0 || callback.getAssignmentScriptMapping().size() > 0) {
            if (scriptAssignments == null || reset) {
                final Map<Long, Map<String, List<Long>>> typeScriptMapping = filterScriptAssignments(
                        callback.getTypeScriptMapping(), true);
                final Map<Long, Map<String, List<Long>>> assignmentScriptMapping = filterScriptAssignments(
                        callback.getTypeScriptMapping(), false);

                final boolean s1 = typeScriptMapping.size() > 0;
                final boolean s2 = assignmentScriptMapping.size() > 0;
                if (s1 || s2) {
                    scriptAssignments = new StringBuilder(5000);
                    scriptAssignments.append(GroovyScriptExporterTools.SCRIPTASSHEADER)
                            .append(GroovyScriptExporterTools.createScriptAssignments(typeScriptMapping, assignmentScriptMapping));
                    scriptAssignments.trimToSize();
                } else if(reset) // delete any existing script code
                    scriptAssignments = null;
            }
        }
    }

    /**
     * Generate the package import statements
     *
     * @param reset set to true if the statements should be regenerated
     */
    private void createImportStatements(boolean reset) {
        if (StringUtils.isBlank(importStatements) || reset) {
            importStatements = GroovyScriptExporterTools.GROOVYPACKAGEIMPORTS.toString();
        }
    }

    public String getDeleteCode() {
        return deleteCode.toString();
    }

    public void setDeleteCode(String deleteCode) {
        this.deleteCode.append(deleteCode);
    }

    public boolean isGenerateImportStatements() {
        return generateImportStatements;
    }

    public void setGenerateImportStatements(boolean generateImportStatements) {
        this.generateImportStatements = generateImportStatements;
    }

    public String getImportStatements() {
        return importStatements;
    }

    public void setImportStatements(String importStatements) {
        this.importStatements = importStatements;
    }

    public boolean isDefaultsOnly() {
        return defaultsOnly;
    }

    public void setDefaultsOnly(boolean defaultsOnly) {
        this.defaultsOnly = defaultsOnly;
    }

    public boolean isGenerateDeleteCode() {
        return generateDeleteCode;
    }

    public void setGenerateDeleteCode(boolean generateDeleteCode) {
        this.generateDeleteCode = generateDeleteCode;
    }

    public boolean isGenerateScriptAssignments() {
        return generateScriptAssignments;
    }

    public void setGenerateScriptAssignments(boolean generateScriptAssignments) {
        this.generateScriptAssignments = generateScriptAssignments;
    }

    public boolean isAddSystemTypes() {
        return addSystemTypes;
    }

    public void setAddSystemTypes(boolean addSystemTypes) {
        this.addSystemTypes = addSystemTypes;
    }

    /**
     * Add custom code to the generated structure / instance deletion code
     *
     * @param customCode custom code as a String
     * @param prepend    set to true if it should be prepended to the existing delete code, false = append
     */
    public void setCustomDeleteCode(String customCode, boolean prepend) {
        if (prepend)
            deleteCode.insert(0, customCode);
        else
            deleteCode.append(customCode);
    }

    public String getScriptCode() {
        return scriptCode.toString();
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode.append(scriptCode);
    }

    public String getScriptAssignments() {
        return this.scriptAssignments.toString();
    }

    public void setScriptAssignments(String scriptAssignments) {
        this.scriptAssignments.append(scriptAssignments);
    }

    /**
     * Add custom code to the generated structure creation code
     *
     * @param customCode custom code as a String
     * @param prepend    set to true if it should be prepended to the existing delete code, false = append
     */
    public void setScriptCode(String customCode, boolean prepend) {
        if (prepend)
            scriptCode.insert(0, customCode);
        else
            scriptCode.append(customCode);
    }

    /**
     * Generate the script code from (mostly) static methods in the relevant tool class for the given callback assignments
     *
     * @param reset set to true in order to regenerate any code
     */
    private void generateScriptCode(boolean reset) {
        scriptCode = new StringBuilder(2000); // always reset at a new run

        // import statements
        if (generateImportStatements) {
            createImportStatements(reset);
            scriptCode.append(importStatements);
        }

        if (generateDeleteCode) {
            createDeleteCode(reset);
            scriptCode.append(deleteCode);
        }

        // actual script / structure creation code
        if (StringUtils.isBlank(scriptCodeOnly.toString()) || reset) {
            // SCRIPT header
            scriptCodeOnly.delete(0, scriptCodeOnly.length()); // reset
            scriptCodeOnly.append(GroovyScriptExporterTools.STRUCTHEADER)
                    .append(GroovyScriptExporterTools.SCRIPTHEADER);

            writeTypeAssignments(callback, true, null);

            // dependencies
            if (callback.getHasDependencies()) {
                try {
                    final List<StructureExporterCallback> dependencyStructures = callback.getDependencyStructures();
                    if (dependencyStructures.size() > 0) {
                        scriptCodeOnly.append(GroovyScriptExporterTools.DEPHEADER);

                        for (StructureExporterCallback cb : dependencyStructures) {
                            List<FxGroupAssignment> callbackGroups;
                            if (callback.getGroupAssignments() == null)
                                callbackGroups = null;
                            else
                                callbackGroups = new ArrayList<FxGroupAssignment>(callback.getGroupAssignments().keySet());

                            writeTypeAssignments(cb, false, callbackGroups);
                        }
                    }
                } catch (FxInvalidStateException e) {
                    LOG.error("Invalid state exception for callback.getDependencyStructures()", e.getCause());
                }

            }
        }
        // assign to field
        scriptCode.append(scriptCodeOnly).trimToSize();

        // Script event assignments
        if (generateScriptAssignments) {
            createScriptAssignmentCode(reset);
            if (scriptAssignments != null) {
                scriptCode.append(scriptAssignments);
            }
        }
    }

    /**
     * Refactorisation of common task: write types, their immediate assignments and all groups
     *
     * @param callback       the currently used StructureExporterCallback interface
     * @param createType     set to true if the createType method should be called for a given type
     * @param callOnlyGroups a List of FxGroupAssignments which should only be called in the Groovy Script (no creation options): GROUPNAME () { .. }
     */
    private void writeTypeAssignments(StructureExporterCallback callback, boolean createType, List<FxGroupAssignment> callOnlyGroups) {
        // walk through the type assignments and create the types (and their immediate assignments)
        final Set<FxType> types = filterExportTypes();
        final Map<FxGroupAssignment, List<FxAssignment>> groupAssignments = callback.getGroupAssignments();

        // types / immed. assignments & groups
        for (FxType t : types) {
            if (createType) {
                scriptCodeOnly.append(GroovyScriptExporterTools.createType(t, defaultsOnly));
            }
            scriptCodeOnly.append(GroovyScriptExporterTools.createTypeAssignments(t, callback.getTypeAssignments().get(t),
                    groupAssignments, defaultsOnly, callOnlyGroups));
        }
    }

    /**
     * Filter out the ROOT type for the Groovy script export
     *
     * @return returns the set of exported types depending on 'addRoot'
     */
    private Set<FxType> filterExportTypes() {
        if (addSystemTypes)
            return callback.getTypeAssignments().keySet();

        final Set<FxType> filteredTypes;

        final Set<FxType> tmp = callback.getTypeAssignments().keySet();
        filteredTypes = new LinkedHashSet<FxType>(); // guarantee order f. export
        for (FxType t : tmp) {
            if (!GroovyScriptExporterTools.isSystemType(t.getName()))
                filteredTypes.add(t);
        }

        return filteredTypes;
    }

    /**
     * Filters out any script assignments which belong to a given Set of filtered types
     *
     * @param scriptAssignments the Map if script assignments
     * @param isTypeList        true if the input map are type assignments, false otherwise
     * @return returns a filtered map if script assignments
     */
    private Map<Long, Map<String, List<Long>>> filterScriptAssignments(Map<Long, Map<String, List<Long>>> scriptAssignments,
                                                                       boolean isTypeList) {

        if (addSystemTypes)
            return scriptAssignments;

        Map<Long, Map<String, List<Long>>> out = new HashMap<Long, Map<String, List<Long>>>();
        for (Long id : scriptAssignments.keySet()) {
            final long mapMainId;
            // determine the correct type id
            if (isTypeList)
                mapMainId = id;
            else {
                mapMainId = CacheAdmin.getEnvironment().getAssignment(id).getAssignedType().getId();
            }
            for (FxType t : filteredTypes) {
                if (t.getId() == mapMainId) {
                    out.put(id, scriptAssignments.get(id));
                }
            }
        }

        return out;
    }
}