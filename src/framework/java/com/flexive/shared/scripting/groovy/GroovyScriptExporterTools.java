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
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/

package com.flexive.shared.scripting.groovy;

import com.flexive.shared.structure.*;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.scripting.FxScriptMappingEntry;
import com.flexive.shared.value.FxValue;

import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.stripToEmpty;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import static com.flexive.shared.structure.export.StructureExporterTools.*;

/**
 * Tools and utilities for the GroovyScriptExporter
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public final class GroovyScriptExporterTools {

    static class Indent {
        static final String TAB = "\t";

        static String tabs(int count) {
            StringBuilder b = new StringBuilder(10);
            for (int i = 0; i < count; i++)
                b.append(TAB);

            return b.toString();
        }
    }

    /**
     * Enumeration of SYSTEM TYPES which can optionally be ignored
     */
    protected enum IgnoreTypes {
        FOLDER, ROOT, IMAGE, ARTICLE, CONTACTDATA, DOCUMENTFILE
    }

    protected final static StringBuilder GROOVYPACKAGEIMPORTS = new StringBuilder("import com.flexive.shared.*\nimport com.flexive.shared.interfaces.*")
            .append("\nimport com.flexive.shared.value.*\nimport com.flexive.shared.content.*")
            .append("\nimport com.flexive.shared.search.*\nimport com.flexive.shared.tree.*")
            .append("\nimport com.flexive.shared.workflow.*\nimport com.flexive.shared.media.*")
            .append("\nimport com.flexive.shared.scripting.groovy.*\nimport com.flexive.shared.structure.*")
            .append("\nimport com.flexive.shared.exceptions.*\nimport com.flexive.shared.scripting.*")
            .append("\nimport com.flexive.shared.security.*\nimport java.text.SimpleDateFormat;\n\n");

    protected final static String SCRIPTHEADER = "def builder\n"; // init Groovy variable
    protected final static String STRUCTHEADER = "// *******************************\n// Structure Creation\n// *******************************\n\n";
    protected final static String DEPHEADER = "// *******************************\n// (Mutual) Dependencies\n// *******************************\n\n";
    protected final static String DELHEADER = "// *******************************\n// Delete Content / Types\n// *******************************\n\n";
    protected final static String SCRIPTASSHEADER = "// *******************************\n// Script Assignments\n// *******************************\n\n";
    public final static Log LOG = LogFactory.getLog(GroovyScriptExporterTools.class);
    protected final static String[] JAVA_KEYWORDS = {"abstract", "continue", "for", "new", "switch", "assert", "default", "goto",
            "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected",
            "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch",
            "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp",
            "volatile", "const", "float", "native", "super", "while"};
    protected final static String[] GROOVY_KEYWORDS = {"as", "def", "in", "property"};

    private GroovyScriptExporterTools() {
        // no instantiation
    }

    /**
     * Iterates throught the enum IgnoreTypes
     *
     * @param typeName the type's name
     * @return returns true if the type is a system type
     */
    public static boolean isSystemType(String typeName) {
        for (IgnoreTypes i : IgnoreTypes.values()) {
            if (typeName.equals(i.toString()))
                return true;
        }
        return false;
    }

    /**
     * This method generates the script code for a type
     *
     * @param type         the FxType which should be exported
     * @param defaultsOnly use defaults only, do not analyse / script options
     * @return String returns the script code for a type
     */
    public static String createType(FxType type, boolean defaultsOnly) {
        StringBuilder script = new StringBuilder(500);
        final int tabCount = 1;

        script.append("builder = new GroovyTypeBuilder().");
        final String typeName = keyWordNameCheck(type.getName(), true);
        script.append(typeName.toLowerCase())
                .append("( "); // opening parenthesis + 1x \s

        if (!defaultsOnly) {
            script.append("\n");
            // LABEL
            final long[] langs = type.getLabel().getTranslatedLanguages();
            final long defLang = type.getLabel().getDefaultLanguage();
            script.append("\tlabel: new FxString(true, ")
                    .append(defLang)
                    .append(", \"")
                    .append(type.getLabel())
                    .append("\")");

            if (langs.length > 1) { // we have more than one language assignment
                for (long id : langs) {
                    if (id != defLang) {
                        script.append(".setTranslation(")
                                .append(id)
                                .append(", \"")
                                .append(type.getLabel().getBestTranslation(id))
                                .append("\")");
                    }
                }
            }
            script.append(",\n");

            // sopts - a map for "simple" GroovyTypeBuilder options
            Map<String, String> sopts = new LinkedHashMap<String, String>();
            final String aclCategory = type.getACL().getCategory().getLabel().getBestTranslation(1).toUpperCase();
            sopts.put("acl", "CacheAdmin.environment.getACL(ACLCategory." + aclCategory + ".getDefaultId())");
            sopts.put("languageMode", type.getLanguage() == LanguageMode.Multiple ? "LanguageMode.Multiple" : "LanguageMode.Single");
            sopts.put("trackHistory", type.isTrackHistory() + "");
            if (type.isTrackHistory())
                sopts.put("historyAge", type.getHistoryAge() + "L");
            sopts.put("typeMode", "TypeMode." + type.getMode().name() + "");
            // sopts.put("workFlow", ",\n") /* Left out for now, also needs to be added in GroovyTypeBuilder */
            sopts.put("maxVersions", type.getMaxVersions() + "L");
            sopts.put("storageMode", "TypeStorageMode." + type.getStorageMode().name() + ""); // not supported in FxTypeEdit, needs to be added to groovy builder
            sopts.put("useInstancePermissions", type.isUseInstancePermissions() + "");
            sopts.put("usePropertyPermissions", type.isUsePropertyPermissions() + "");
            sopts.put("useStepPermissions", type.isUseStepPermissions() + "");
            sopts.put("useTypePermissions", type.isUseTypePermissions() + "");
            sopts.put("usePermissions", type.isUsePermissions() + "");
            if (type.isDerived()) { // take out of !defaultsOnly option?
                sopts.put("parentTypeName", "\"" + type.getParent().getName() + "\"");
            }

            // append options to script
            for (String option : sopts.keySet()) {
                script.append(simpleOption(option, sopts.get(option), tabCount));
            }

            script.trimToSize();
            if (script.indexOf(",\n", script.length() - 2) != -1)
                script.delete(script.length() - 2, script.length());

        }

        script.append(")\n\n");
        script.trimToSize();
        return script.toString();
    }

    /**
     * Contains the logic to generate the script code for a a type's assignments
     *
     * @param type             a given type
     * @param assignments      a List of the given types immediate assignments
     * @param groupAssignments the Map of GroupAssignments (keys) and their child assignments (List of values)
     * @param defaultsOnly     only use the defaults provided by the GroovyTypeBuilder
     * @param callOnlyGroups   a List of FxGroupAssignments for which no options should be generated
     * @return the script code
     */
    public static String createTypeAssignments(FxType type, List<FxAssignment> assignments, Map<FxGroupAssignment,
            List<FxAssignment>> groupAssignments, boolean defaultsOnly, List<FxGroupAssignment> callOnlyGroups) {

        if (assignments != null && assignments.size() > 0) {
            final StringBuilder script = new StringBuilder(2000);
            script.append("builder = new GroovyTypeBuilder(\"")
                    .append(type.getName())
                    .append("\")\n")
                    .append("builder {\n"); // opening curly brackets

            // assignment walk-through
            final int tabCount = 1;
            script.append(createChildAssignments(assignments, groupAssignments, defaultsOnly, callOnlyGroups, tabCount));

            script.append("}\n\n"); // closing curly brackets
            return script.toString();
        }
        return "";
    }

    /**
     * Write the script code to create a property from a given FxPropertyAssignment
     *
     * @param pa           the FxPropertyAssignment to be scripted
     * @param defaultsOnly use only default settings provided by the GTB, no analysis of assignments will be performed
     * @param tabCount     the number of tabs to be added to the code's left hand side
     * @return returns the partial script as a StringBuilder instance
     */
    public static String createProperty(FxPropertyAssignment pa, boolean defaultsOnly, int tabCount) {
        final FxProperty prop = pa.getProperty();
        StringBuilder script = new StringBuilder(1000);

        // NAME
        script.append(Indent.tabs(tabCount));
        tabCount++;
        final String propName = keyWordNameCheck(prop.getName().toLowerCase(), true);
        script.append(propName)
                .append("( "); // opening parenthesis + 1x \s

        if (!defaultsOnly) {
            script.append("\n");
            // label and hint
            script.append(getLabelAndHintStructure(prop, true, true, tabCount));

            final String dataType = prop.getDataType() + "";
            // sopts - a map for "simple" GroovyTypeBuilder options
            Map<String, String> sopts = new LinkedHashMap<String, String>();
            // ALIAS --> set if different from name
            if (!prop.getName().equals(pa.getAlias())) {
                sopts.put("alias", "\"" + pa.getAlias() + "\"");
            }
            // def multiplicity for the assignment
            sopts.put("defaultMultiplicity", pa.getDefaultMultiplicity() + "");
            sopts.put("multilang", prop.isMultiLang() + "");
            sopts.put("dataType", "FxDataType." + dataType + "");
            sopts.put("acl", "CacheAdmin.getEnvironment().getACL(ACLCategory." + prop.getACL().getCategory() + ".getDefaultId())");
            sopts.put("multiplicity", "new FxMultiplicity(" + prop.getMultiplicity().getMin() + "," + prop.getMultiplicity().getMax() + ")");
            sopts.put("overrideACL", prop.mayOverrideACL() + "");
            sopts.put("overrideMultiplicity", prop.mayOverrideBaseMultiplicity() + "");
            sopts.put("overrideInOverview", prop.mayOverrideInOverview() + "");
            sopts.put("overrideMaxLength", prop.mayOverrideMaxLength() + "");
            sopts.put("overrideMultiline", prop.mayOverrideMultiLine() + "");
            sopts.put("overrideSearchable", prop.mayOverrideSearchable() + "");
            sopts.put("overrideUseHtmlEditor", prop.mayOverrideUseHTMLEditor() + "");
            sopts.put("overrideMultilang", prop.mayOverrideMultiLang() + "");
            if (prop.getMaxLength() != 0) {// means that maxLength is not set
                sopts.put("maxLength", prop.getMaxLength() + "");
            }
            sopts.put("searchable", prop.isSearchable() + "");
            sopts.put("fullTextIndexed", prop.isFulltextIndexed() + "");
            sopts.put("multiline", prop.isMultiLine() + "");
            sopts.put("inOverview", prop.isInOverview() + "");
            sopts.put("useHtmlEditor", prop.isUseHTMLEditor() + "");
            sopts.put("uniqueMode", "UniqueMode." + prop.getUniqueMode());
            sopts.put("enabled", pa.isEnabled() + "");
            // REFERENCE
            if ("Reference".equals(dataType)) {
                final String refType = "CacheAdmin.getEnvironment().getType(\"" + prop.getReferencedType().getName() + "\")";
                sopts.put("referencedType", refType + "");
            }
            if (prop.getReferencedList() != null) {
                final String refList = "CacheAdmin.getEnvironment().getSelectList(\"" + prop.getReferencedList().getName() + "\")";
                sopts.put("referencedList", refList + "");
            }

            // FxStructureOptions
            sopts.putAll(getStructureOptions(prop));

            // DEFAULT VALUES
            if (prop.isDefaultValueSet()) {
                final FxValue val = prop.getDefaultValue();
                String defaultValue = val.toString();
                StringBuilder out = new StringBuilder(100);
                final String multiLang = prop.isMultiLang() + "";

                if (DATATYPES.contains(dataType)) {
                    // SELECT LIST DATATYPES
                    if ("SelectOne".equals(dataType) || "SelectMany".equals(dataType)) {
                        final String refListName = "CacheAdmin.getEnvironment().getSelectList(\"" + prop.getReferencedList().getName();
                        sopts.put("REFERENCEDLIST", refListName + "\"),\n");
                        final FxSelectList list = prop.getReferencedList();
                        if ("SelectOne".equals(dataType)) {
                            for (FxSelectListItem item : list.getItems()) {
                                if (defaultValue.equals(item.getLabel().toString())) {
                                    defaultValue = item.getName(); // reassign
                                }
                            }
                            out.append("new FxSelectOne(")
                                    .append(multiLang)
                                    .append(", CacheAdmin.getEnvironment().getSelectListItem(")
                                    .append(refListName)
                                    .append("\"), \"")
                                    .append(defaultValue)
                                    .append("\"))");

                        } else if ("SelectMany".equals(dataType)) {
                            String[] defaults = FxSharedUtils.splitLiterals(defaultValue);
                            for (int i = 0; i < defaults.length; i++) {
                                for (FxSelectListItem item : list.getItems()) {
                                    if (defaults[i].equals(item.getLabel().toString())) {
                                        defaults[i] = item.getName(); // reassign
                                    }
                                }
                            }
                            out.append("new FxSelectMany(")
                                    .append(multiLang)
                                    .append(", new SelectMany(")
                                    .append(refListName)
                                    .append("\"))");
                            // traverse renamed defaults and append them to the script
                            for (String d : defaults) {
                                out.append(".selectItem(CacheAdmin.getEnvironment().getSelectListItem(")
                                        .append(refListName)
                                        .append("\"), \"")
                                        .append(d)
                                        .append("\"))");
                            }
                            out.append(")");
                        }
                    } else if ("Date".equals(dataType) || "DateTime".equals(dataType) || "DateRange".equals(dataType) || "DateTimeRange".equals(dataType)) {
                        final String df = "\"MMM dd, yyyy\"";
                        final String dtf = "\"MMM dd, yyyy h:mm:ss a\"";
                        if ("Date".equals(dataType)) {
                            out.append("new FxDate(")
                                    .append(multiLang)
                                    .append(", new SimpleDateFormat(")
                                    .append(df)
                                    .append(").parse(\"")
                                    .append(defaultValue)
                                    .append("\"))");
                        } else if ("DateTime".equals(dataType)) {
                            out.append("new FxDateTime(")
                                    .append(multiLang)
                                    .append(", new SimpleDateFormat(")
                                    .append(dtf)
                                    .append(").parse(\"")
                                    .append(defaultValue)
                                    .append("\"))");
                        } else if ("DateRange".equals(dataType)) {
                            final String lower = stripToEmpty(defaultValue.substring(0, defaultValue.indexOf("-")));
                            final String upper = stripToEmpty(defaultValue.substring(defaultValue.indexOf("-") + 1));
                            out.append("new FxDateRange(")
                                    .append(multiLang)
                                    .append(", new DateRange(new SimpleDateFormat(")
                                    .append(df)
                                    .append(").parse(\"")
                                    .append(lower)
                                    .append("\"), new SimpleDateFormat(")
                                    .append(df)
                                    .append(").parse(\"")
                                    .append(upper)
                                    .append("\")))");
                        } else if ("DateTimeRange".equals(dataType)) {
                            final String lower = stripToEmpty(defaultValue.substring(0, defaultValue.indexOf("-")));
                            final String upper = stripToEmpty(defaultValue.substring(defaultValue.indexOf("-") + 1));
                            out.append("new FxDateTimeRange(")
                                    .append(multiLang)
                                    .append(", new DateRange(new SimpleDateFormat(")
                                    .append(dtf)
                                    .append(").parse(\"")
                                    .append(lower)
                                    .append("\"), new SimpleDateFormat(")
                                    .append(dtf)
                                    .append(").parse(\"")
                                    .append(upper)
                                    .append("\")))");
                        }
                    } else if ("Reference".equals(dataType)) {
                        final long pk = Long.parseLong(defaultValue.substring(0, defaultValue.indexOf(".")));
                        final int version = Integer.parseInt(defaultValue.substring(defaultValue.indexOf(".") + 1));
                        out.append("new FxReference(")
                                .append(multiLang)
                                .append(", new ReferencedContent(")
                                .append(pk)
                                .append(", ")
                                .append(version)
                                .append("))");

                    } else if ("InlineReference".equals(dataType)) {
                        // ignore for now, doesn't work properly as of yet
                    } else if ("Binary".equals(dataType)) {
                        // uses a new BinaryDescriptor( .. )
                    }
                }

                // "SIMPLE" dataTYPES
                if (DATATYPESSIMPLE.keySet().contains(dataType)) {
                    for (String d : DATATYPESSIMPLE.keySet()) {
                        if (d.equals(dataType)) {
                            out.append(DATATYPESSIMPLE.get(d))
                                    .append("(")
                                    .append(multiLang)
                                    .append(", ");
                            if (d.equals("Float") || d.equals("Double") || d.equals("LargeNumber") || d.equals("Boolean")) {
                                out.append(defaultValue);
                            } else {
                                out.append("\"")
                                        .append(defaultValue)
                                        .append("\"");
                            }
                            out.append(")");
                        }
                    }
                }

                out.trimToSize();
                // add the computed value to the "simpleOtions"
                sopts.put("defaultValue", out.toString() + ",");
            }

            // append options to script
            for (String option : sopts.keySet()) {
                script.append(simpleOption(option, sopts.get(option), tabCount));
            }

            script.trimToSize();
            if (script.indexOf(",\n", script.length() - 2) != -1)
                script.delete(script.length() - 2, script.length());
            if (script.indexOf(",", script.length() - 1) != -1)
                script.delete(script.length() - 1, script.length());
        }

        script.append(")\n"); // closing parenthesis

        // if the difference analyser yields any data, change the assignment in the next line (only launch if defaultsOnly = false)
        if (!defaultsOnly) {
            final List<String> differences = AssignmentDifferenceAnalyser.analyse(pa, false);
            if (differences.size() > 0) {
                script.append(updatePropertyAssignment(pa, false, differences, defaultsOnly, --tabCount));
            }
        }
        script.trimToSize();
        return script.toString();
    }

    /**
     * Write the script code to update a property assignment, or to create a derived assignment
     * <p/>
     * "acl", "defaultValue", "hint", "label", "multilang", "multiline", "multiplicity"
     *
     * @param pa           the FxPropertyAssignment to be updated
     * @param isDerived    the Assignment is derived
     * @param differences  the List of differences (map keys f. the builder)
     * @param defaultsOnly use only default settings provided by the GTB, no analysis of assignments will be performed
     * @param tabCount     the number of tabs to be added to the code's left hand side
     * @return returns the partial script as a StringBuilder instance
     */
    public static String updatePropertyAssignment(FxPropertyAssignment pa, boolean isDerived, List<String> differences,
                                                  boolean defaultsOnly, int tabCount) {
        StringBuilder script = new StringBuilder(500);
        final FxProperty prop = pa.getProperty();
        final String dataType = pa.getProperty().getDataType() + "";

        // use the alias as the reference name
        script.append(Indent.tabs(tabCount));
        final String propAlias = keyWordNameCheck(pa.getAlias().toLowerCase(), true);
        script.append(propAlias)
                .append("( "); // opening parenthesis + 1x \s

        // ASSIGNMENT
        if (isDerived) {
            final String assignmentPath = CacheAdmin.getEnvironment().getAssignment(pa.getBaseAssignmentId()).getXPath();
            script.append("assignment: \"")
                    .append(assignmentPath)
                    .append("\",");
        }

        if (!defaultsOnly) {
            tabCount++;
            script.append("\n");
            // label and hint
            if (differences.contains("hint") || differences.contains("label"))
                script.append(getLabelAndHintAssignment(pa, differences.contains("label"), differences.contains("hint"), tabCount));

            // sopts - a map for "simple" GroovyTypeBuilder options
            Map<String, String> sopts = new LinkedHashMap<String, String>();
            final String multiLang = pa.isMultiLang() + "";

            if (differences.contains("multilang")) {
                if (prop.mayOverrideMultiLang()) {
                    sopts.put("multilang", multiLang + "");
                }
            }

            if (differences.contains("acl")) {
                if (prop.mayOverrideACL())
                    sopts.put("acl", "CacheAdmin.getEnvironment().getACL(ACLCategory." + pa.getACL().getCategory() + ".getDefaultId())");
            }

            if (differences.contains("multiplicity")) {
                if (prop.mayOverrideBaseMultiplicity())
                    sopts.put("multiplicity", "new FxMultiplicity(" + pa.getMultiplicity().getMin() + "," + pa.getMultiplicity().getMax() + ")");
            }

            if (differences.contains("maxLength")) {
                if (prop.mayOverrideMaxLength())
                    sopts.put("maxLength", pa.getMaxLength() + "");
            }

            if (differences.contains("inOverview")) {
                if (prop.mayOverrideInOverview())
                    sopts.put("inOverview", pa.isInOverview() + "");
            }

            if (differences.contains("useHtmlEditor")) {
                if (prop.mayOverrideUseHTMLEditor())
                    sopts.put("useHtmlEditor", pa.isUseHTMLEditor() + "");
            }

            if (differences.contains("multilang")) {
                if (pa.isMultiLang()) {
                    sopts.put("", pa.getDefaultLanguage() + "");
                }
            }

            if (differences.contains("multiline")) {
                if (prop.mayOverrideMultiLine())
                    sopts.put("multiline", pa.isMultiLine() + "");
            }

            // structure options
            if (differences.contains("structureoptions"))
                sopts.putAll(getStructureOptions(pa));

            if (differences.contains("searchable")) {
                if (prop.mayOverrideSearchable())
                    sopts.put("searchable", pa.isSearchable() + "");
            }

            // options valid for derived assignments only **********************
            if (differences.contains("defaultMultiplicity"))
                sopts.put("defaultMultiplicity", pa.getDefaultMultiplicity() + "");

            if (differences.contains("alias"))
                sopts.put("alias", "\"" + pa.getAlias() + "\"");

            if (differences.contains("enabled"))
                sopts.put("enabled", pa.isEnabled() + "");

            if (differences.contains("defaultLanguage"))
                sopts.put("defaultLanguage", pa.getDefaultLanguage() + "L");
            // *****************************************************************

            // DEFAULT VALUES
            if (differences.contains("defaultValue")) {
                if (pa.getDefaultValue() != null) {
                    final FxValue val = pa.getDefaultValue();
                    String defaultValue = val.toString();
                    StringBuilder out = new StringBuilder(100);

                    if (DATATYPES.contains(dataType)) {
                        // SELECT LIST DATATYPES
                        if ("SelectOne".equals(dataType) || "SelectMany".equals(dataType)) {
                            final FxSelectList list = pa.getProperty().getReferencedList();
                            final String refListName = "CacheAdmin.getEnvironment().getSelectList(\"" + list.getName();
                            sopts.put("REFERENCEDLIST", refListName + "\"),\n");

                            if ("SelectOne".equals(dataType)) {
                                for (FxSelectListItem item : list.getItems()) {
                                    if (defaultValue.equals(item.getLabel().toString())) {
                                        defaultValue = item.getName(); // reassign
                                    }
                                }
                                out.append("new FxSelectOne(")
                                        .append(multiLang)
                                        .append(", CacheAdmin.getEnvironment().getSelectListItem(")
                                        .append(refListName)
                                        .append("\"), \"")
                                        .append(defaultValue)
                                        .append("\"))");

                            } else if ("SelectMany".equals(dataType)) {
                                String[] defaults = FxSharedUtils.splitLiterals(defaultValue);
                                for (int i = 0; i < defaults.length; i++) {
                                    for (FxSelectListItem item : list.getItems()) {
                                        if (defaults[i].equals(item.getLabel().toString())) {
                                            defaults[i] = item.getName(); // reassign
                                        }
                                    }
                                }
                                out.append("new FxSelectMany(")
                                        .append(multiLang)
                                        .append(", new SelectMany(")
                                        .append(refListName)
                                        .append("\"))");
                                // traverse renamed defaults and append them to the script
                                for (String d : defaults) {
                                    out.append(".selectItem(CacheAdmin.getEnvironment().getSelectListItem(")
                                            .append(refListName)
                                            .append("\"), \"")
                                            .append(d)
                                            .append("\"))");
                                }
                                out.append(")");
                            }
                        } else if ("Date".equals(dataType) || "DateTime".equals(dataType) || "DateRange".equals(dataType) || "DateTimeRange".equals(dataType)) {
                            final String df = "\"MMM dd, yyyy\"";
                            final String dtf = "\"MMM dd, yyyy h:mm:ss a\"";
                            if ("Date".equals(dataType)) {
                                out.append("new FxDate(")
                                        .append(multiLang)
                                        .append(", new SimpleDateFormat(")
                                        .append(df)
                                        .append(").parse(\"")
                                        .append(defaultValue)
                                        .append("\"))");
                            } else if ("DateTime".equals(dataType)) {
                                out.append("new FxDateTime(")
                                        .append(multiLang)
                                        .append(", new SimpleDateFormat(")
                                        .append(dtf)
                                        .append(").parse(\"")
                                        .append(defaultValue)
                                        .append("\"))");
                            } else if ("DateRange".equals(dataType)) {
                                final String lower = stripToEmpty(defaultValue.substring(0, defaultValue.indexOf("-")));
                                final String upper = stripToEmpty(defaultValue.substring(defaultValue.indexOf("-") + 1));
                                out.append("new FxDateRange(")
                                        .append(multiLang)
                                        .append(", new DateRange(new SimpleDateFormat(")
                                        .append(df)
                                        .append(").parse(\"")
                                        .append(lower)
                                        .append("\"), new SimpleDateFormat(")
                                        .append(df)
                                        .append(").parse(\"")
                                        .append(upper)
                                        .append("\")))");
                            } else if ("DateTimeRange".equals(dataType)) {
                                final String lower = stripToEmpty(defaultValue.substring(0, defaultValue.indexOf("-")));
                                final String upper = stripToEmpty(defaultValue.substring(defaultValue.indexOf("-") + 1));
                                out.append("new FxDateTimeRange(")
                                        .append(multiLang)
                                        .append(", new DateRange(new SimpleDateFormat(")
                                        .append(dtf)
                                        .append(").parse(\"")
                                        .append(lower)
                                        .append("\"), new SimpleDateFormat(")
                                        .append(dtf)
                                        .append(").parse(\"")
                                        .append(upper)
                                        .append("\")))");
                            }
                        } else if ("Reference".equals(dataType)) {
                            final long pk = Long.parseLong(defaultValue.substring(0, defaultValue.indexOf(".")));
                            final int version = Integer.parseInt(defaultValue.substring(defaultValue.indexOf(".") + 1));
                            out.append("new FxReference(")
                                    .append(multiLang)
                                    .append(", new ReferencedContent(")
                                    .append(pk)
                                    .append(", ")
                                    .append(version)
                                    .append("))");

                        } else if ("InlineReference".equals(dataType)) {
                            // ignore for now, doesn't work paerly as of yet
                        } else if ("Binary".equals(dataType)) {
                            // TODO: impl!
                            // uses a new BinaryDescriptor( .. )
                        }
                    }

                    // "SIMPLE" dataTYPES
                    if (DATATYPESSIMPLE.keySet().contains(dataType)) {
                        for (String d : DATATYPESSIMPLE.keySet()) {
                            if (d.equals(dataType)) {
                                out.append(DATATYPESSIMPLE.get(d))
                                        .append("(")
                                        .append(multiLang)
                                        .append(", ");
                                if (d.equals("Float") || d.equals("Double") || d.equals("LargeNumber") || d.equals("Boolean")) {
                                    out.append(defaultValue);
                                } else {
                                    out.append("\"")
                                            .append(defaultValue)
                                            .append("\"");
                                }
                                out.append(")");
                            }
                        }
                    }
                    out.trimToSize();
                    sopts.put("defaultValue", out.toString() + ",");
                }
            }

            // append options to script
            if (sopts.size() > 0) {
                for (String option : sopts.keySet()) {
                    script.append(simpleOption(option, sopts.get(option), tabCount));
                }
            }

            script.trimToSize();
            if (script.indexOf(",\n", script.length() - 2) != -1)
                script.delete(script.length() - 2, script.length());
        }
        script.trimToSize();
        if (script.indexOf(",", script.length() - 1) != -1) // remove last "," if written
            script.delete(script.length() - 1, script.length());
        script.append(")\n"); // closing parenthesis

        return script.toString();
    }

    /**
     * A method for setting simple GroovyTypeBuilder options
     * with a trailing CR
     *
     * @param option   The name of the option, e.g. "searchable"
     * @param value    the value of the option, e.g. "true"
     * @param tabCount the number of tabs to be added to the code's left hand side
     * @return returns the partial script as a StringBuilder instance
     */
    private static StringBuilder simpleOption(String option, String value, int tabCount) {
        StringBuilder s = new StringBuilder(500);
        s.append(Indent.tabs(tabCount))
                .append(option)
                .append(": ")
                .append(value)
                .append(",\n");
        s.trimToSize();
        return s;
    }

    /**
     * Write the script code to create a group
     *
     * @param ga               the FxGroupAssignment to be scripted
     * @param childAssignments a List of child assignments for the given group
     * @param groupAssignments the map of FxGroupAssignments (keys) and their respective Lists of FxAssignments (values)
     * @param isDerived        set to "true" if the assignment to be written is derived from another property
     * @param defaultsOnly     use only default settings provided by the GTB, no analysis of assignments will be performed
     * @param callOnlyGroups   a List of FxGroupAssignments for which no options should be generated
     * @param tabCount         the number of tabs to be added to the code's left hand side
     * @return returns the partial script as a String
     */
    public static String createGroup(FxGroupAssignment ga, List<FxAssignment> childAssignments,
                                     Map<FxGroupAssignment, List<FxAssignment>> groupAssignments,
                                     boolean isDerived, boolean defaultsOnly, List<FxGroupAssignment> callOnlyGroups,
                                     int tabCount) {
        final StringBuilder script = new StringBuilder(200);

        if (!isDerived) {
            final FxGroup group = ga.getGroup();

            // NAME
            script.append(Indent.tabs(tabCount));
            final String groupName = keyWordNameCheck(group.getName().toUpperCase(), true);
            script.append(groupName)
                    .append("( "); // opening parenthesis + 1x \s

            // CHECK IF THIS GROUP WAS CREATED BEFOREHAND AND ONLY NEEDS TO BE CALLED FOR STRUCTURE CREATION
            if (callOnlyGroups == null || (callOnlyGroups != null && !callOnlyGroups.contains(ga))) {

                if (!defaultsOnly) {
                    tabCount++; // add tabs for options
                    script.append("\n");
                    // label and hint
                    script.append(getLabelAndHintStructure(group, true, true, tabCount));

                    Map<String, String> sopts = new LinkedHashMap<String, String>();
                    sopts.put("alias", "\"" + ga.getAlias() + "\"");
                    sopts.put("defaultMultiplicity", ga.getDefaultMultiplicity() + "");
                    sopts.put("overrideMultiplicity", group.mayOverrideBaseMultiplicity() + "");
                    sopts.put("multiplicity", "new FxMultiplicity(" + group.getMultiplicity().getMin() + "," + group.getMultiplicity().getMax() + ")");
                    sopts.put("groupMode", "GroupMode." + ga.getMode().name());

                    // remaining structure options
                    sopts.putAll(getStructureOptions(group));

                    // append options to script
                    for (String option : sopts.keySet()) {
                        script.append(simpleOption(option, sopts.get(option), tabCount));
                    }

                    script.trimToSize();
                    if (script.indexOf(",\n", script.length() - 2) != -1)
                        script.delete(script.length() - 2, script.length());

                    --tabCount; // remove tab again
                }
            }

            script.append(") "); // closing parenthesis + 1x \s
            // if the difference analyser yields any data, change the assignment in the next line (only launch if defaultsOnly = false)
            if (!defaultsOnly) {
                final List<String> differences = AssignmentDifferenceAnalyser.analyse(ga, false);
                if (differences.size() > 0) {
                    script.append("\n"); // closing parenthesis
                    script.append(updateGroupAssignment(ga, false, differences, defaultsOnly, tabCount));
                }
            }
        } else { // DERIVED GROUP ASSIGNMENTS
            final List<String> differences = AssignmentDifferenceAnalyser.analyse(ga, true);
            script.append(updateGroupAssignment(ga, true, differences, defaultsOnly, tabCount));
        }

        // add child assignments ******************************
        if (childAssignments != null && childAssignments.size() > 0) {
            script.append("{\n"); // closing parenthesis and curly bracket
            script.append(createChildAssignments(childAssignments, groupAssignments, defaultsOnly, callOnlyGroups, ++tabCount));
            script.append(Indent.tabs(--tabCount));
            script.append("}"); // closing curly bracket
        }

        script.append("\n");
        script.trimToSize();
        return script.toString();
    }

    /**
     * Write the script code to create a group assignment
     *
     * @param ga           the FxGroupAssignment to be scripted
     * @param isDerived    the Assignment is derived
     * @param differences  the List of differences (map keys f. the builder)
     * @param defaultsOnly use only default settings provided by the GTB, no analysis of assignments will be performed
     * @param tabCount     the number of tabs to be added to the code's left hand side
     * @return returns the partial script as a String
     */
    public static String updateGroupAssignment(FxGroupAssignment ga, boolean isDerived, List<String> differences,
                                               boolean defaultsOnly, int tabCount) {
        StringBuilder script = new StringBuilder(200);
        final FxGroup group = ga.getGroup();

        // name = alias
        script.append(Indent.tabs(tabCount));
        final String groupAlias = keyWordNameCheck(ga.getAlias().toUpperCase(), true);
        script.append(groupAlias)
                .append("( "); // opening parenthesis + 1x \s

        // ASSIGNMENT
        if (isDerived) {
            final String assignmentPath = CacheAdmin.getEnvironment().getAssignment(ga.getBaseAssignmentId()).getXPath();
            script.append("assignment: \"")
                    .append(assignmentPath)
                    .append("\",");
        }

        if (!defaultsOnly) {
            tabCount++;
            script.append("\n");
            // label and hint
            if (differences.contains("hint") || differences.contains("label"))
                script.append(getLabelAndHintStructure(group, differences.contains("label"), differences.contains("hint"), tabCount));

            Map<String, String> sopts = new LinkedHashMap<String, String>();

            if (differences.contains("defaultMultiplicity"))
                sopts.put("defaultMultiplicity", ga.getDefaultMultiplicity() + "");

            if (differences.contains("multiplicity")) {
                if (group.mayOverrideBaseMultiplicity())
                    sopts.put("multiplicity", "new FxMultiplicity(" + ga.getMultiplicity().getMin() + "," + ga.getMultiplicity().getMax() + ")");
            }

            if (differences.contains("groupMode"))
                sopts.put("groupMode", "GroupMode." + ga.getMode().name());

            if (differences.contains("enabled"))
                sopts.put("enabled", ga.isEnabled() + "");

            // Structure options
            if (differences.contains("structureoptions"))
                sopts.putAll(getStructureOptions(ga));

            // append options to script
            for (String option : sopts.keySet()) {
                script.append(simpleOption(option, sopts.get(option), tabCount));
            }

            script.trimToSize();
            if (script.indexOf(",\n", script.length() - 2) != -1)
                script.delete(script.length() - 2, script.length());

        }
        script.trimToSize();
        if (script.indexOf(",", script.length() - 1) != -1)
            script.delete(script.length() - 1, script.length());
        script.append(") "); // closing parenthesis + 1x \s

        return script.toString();
    }

    /**
     * This method is used to "route" the given child assignments to their respective evaluation methods
     * The method's first call comes from #generateTypeAssignments"
     * This method is subsequently called from #createGroup
     *
     * @param childAssignments the List of FxAssignments (children of a given group)
     * @param groupAssignments the map of FxGroupAssignments (keys) and their respective Lists of FxAssignments (values)
     * @param defaultsOnly     use only default settings provided by the GTB, no analysis of assignments will be performed
     * @param callOnlyGroups   a List of FxGroupAssignments for which no options should be generated
     * @param tabCount         the number of tabs to be added to the code's left hand side
     * @return returns the script code or an empty String if the childassigments list is empty
     */
    public static String createChildAssignments(List<FxAssignment> childAssignments,
                                                Map<FxGroupAssignment, List<FxAssignment>> groupAssignments, boolean defaultsOnly,
                                                List<FxGroupAssignment> callOnlyGroups, int tabCount) {
        final StringBuilder script = new StringBuilder(2000);

        if (childAssignments != null && childAssignments.size() > 0) {
            for (FxAssignment a : childAssignments) {
                final boolean isDerived = a.isDerivedAssignment();
                // PROPERTIES
                if (a instanceof FxPropertyAssignment) {
                    if (isDerived) {
                        final List<String> differences = AssignmentDifferenceAnalyser.analyse(a, true);
                        script.append(updatePropertyAssignment((FxPropertyAssignment) a, true, differences, defaultsOnly, tabCount));
                    } else {
                        script.append(createProperty((FxPropertyAssignment) a, defaultsOnly, tabCount));
                    }
                    // GROUPS
                } else if (a instanceof FxGroupAssignment) {
                    // retrieve the child assignments for the given group and pass them on
                    final List<FxAssignment> currentChildren = groupAssignments.get((FxGroupAssignment) a);
                    script.append(createGroup((FxGroupAssignment) a, currentChildren, groupAssignments, isDerived, defaultsOnly, callOnlyGroups, tabCount));
                }
            }
        }

        script.trimToSize();
        return script.toString();
    }

    /**
     * Retrieves the label and the hint of an FxAssignment (and the available translations)
     *
     * @param a        the FxAssignment
     * @param label    set to true to generate the label code
     * @param hint     set to true to generate the hint code
     * @param tabCount the number of tabs to be added to the code's left hand side
     * @return returns the partial Groovy Script as a StringBuilder instance
     */
    private static <T extends FxAssignment> StringBuilder getLabelAndHintAssignment(T a, boolean label, boolean hint, int tabCount) {
        StringBuilder script = new StringBuilder(200);

        // LABEL
        long[] langs = a.getLabel().getTranslatedLanguages();
        long defLang = a.getLabel().getDefaultLanguage();
        if (label) {
            script.append(Indent.tabs(tabCount));
            script.append("label: new FxString(true, ") // label and hint are always multilang
                    .append(defLang)
                    .append(", \"")
                    .append(a.getLabel())
                    .append("\")");

            if (langs.length > 1) { // we have more than one language assignment
                for (long id : langs) {
                    if (id != defLang) {
                        script.append(".setTranslation(")
                                .append(id)
                                .append(", \"")
                                .append(a.getLabel().getBestTranslation(id))
                                .append("\")");
                    }
                }
            }
            script.append(",\n");
        }

        // HINT
        if (hint) {
            langs = a.getHint().getTranslatedLanguages();
            defLang = a.getHint().getDefaultLanguage();
            String hintAsString = a.getHint().toString();
            if (isBlank(hintAsString) || "null".equals(hintAsString))
                hintAsString = "";
            script.append(Indent.tabs(tabCount));
            script.append("hint: new FxString(true, ");

            script.append(defLang)
                    .append(", \"")
                    .append(hintAsString)
                    .append("\")");

            if (langs.length > 1) { // we have more than one language assignment
                for (long id : langs) {
                    if (id != defLang) {
                        hintAsString = a.getHint().getBestTranslation(id);
                        if (isBlank(hintAsString) || "null".equals(hintAsString))
                            hintAsString = "";
                        script.append(".setTranslation(")
                                .append(id)
                                .append(", \"")
                                .append(hintAsString)
                                .append("\")");
                    }
                }
            }
            script.append(",\n");
        }

        script.trimToSize();
        return script;
    }

    /**
     * Retrieves the label and the hint of a property or group (and the available translations)
     *
     * @param a        the FxAssignment
     * @param label    set to true to generate the label code
     * @param hint     set to true to generate the hint code
     * @param tabCount the number of tabs to be added to the code's left hand side
     * @return returns the partial Groovy Script as a StringBuilder instance
     */
    private static <T extends FxStructureElement> StringBuilder getLabelAndHintStructure(T a, boolean label, boolean hint, int tabCount) {
        StringBuilder script = new StringBuilder(200);

        // LABEL
        long[] langs = a.getLabel().getTranslatedLanguages();
        long defLang = a.getLabel().getDefaultLanguage();

        if (label) {
            script.append(Indent.tabs(tabCount));
            script.append("label: new FxString(true, ") // label and hint are always multilang
                    .append(defLang)
                    .append(", \"")
                    .append(a.getLabel())
                    .append("\")");

            if (langs.length > 1) { // we have more than one language assignment
                for (long id : langs) {
                    if (id != defLang) {
                        script.append(".setTranslation(")
                                .append(id)
                                .append(", \"")
                                .append(a.getLabel().getBestTranslation(id))
                                .append("\")");
                    }
                }
            }
            script.append(",\n");
        }

        // HINT
        if (hint) {
            langs = a.getHint().getTranslatedLanguages();
            defLang = a.getHint().getDefaultLanguage();
            String hintAsString = a.getHint().toString();
            if (isBlank(hintAsString) || "null".equals(hintAsString))
                hintAsString = "";
            script.append(Indent.tabs(tabCount));
            script.append("hint: new FxString(true, ");

            script.append(defLang)
                    .append(", \"")
                    .append(hintAsString)
                    .append("\")");

            if (langs.length > 1) { // we have more than one language assignment
                for (long id : langs) {
                    if (id != defLang) {
                        hintAsString = a.getHint().getBestTranslation(id);
                        if (isBlank(hintAsString) || "null".equals(hintAsString))
                            hintAsString = "";
                        script.append(".setTranslation(")
                                .append(id)
                                .append(", \"")
                                .append(hintAsString)
                                .append("\")");
                    }
                }
            }
            script.append(",\n");
        }

        script.trimToSize();
        return script;
    }

    /**
     * Retrieve all set structure options for an FxStructureElement
     *
     * @param element the FxStructureElement (e.g. FxGroup)
     * @return returns a Map<String, String> containing the FxStructureOption --> Value mappings
     */
    private static <T extends FxStructureElement> Map<String, String> getStructureOptions(T element) {
        Map<String, String> opts = new HashMap<String, String>();
        for (FxStructureOption o : element.getOptions()) {
            if (o.isSet())
                opts.put("\"" + o.getKey() + "\"", o.getValue());
        }
        return opts;
    }

    /**
     * Retrieve all set structure options for an FxAssignment
     *
     * @param a the FxAssignment
     * @return returns a Map<String, String> containing the FxStructureOption --> Value mappings
     */
    private static <T extends FxAssignment> Map<String, String> getStructureOptions(T a) {
        Map<String, String> opts = new HashMap<String, String>();
        for (FxStructureOption o : a.getOptions()) {
            if (o.isSet())
                opts.put("\"" + o.getKey() + "\"", o.getValue());
        }

        return opts;
    }

    /**
     * Difference Analyser - Records any differences between a base property / group and an FxAssignment (if within the
     * same type)
     * The analysis will always return the (GTB) values of the property's / group's assignments which differ from the base
     * (i.e. --> write "base" first, then change the assignment if different)
     */
    static class AssignmentDifferenceAnalyser {

        static List<String> analyse(FxAssignment a, boolean isDerived) {
            if (a instanceof FxPropertyAssignment) {
                if (isDerived)
                    return derivedPropComparison((FxPropertyAssignment) a);
                else
                    return propComparison((FxPropertyAssignment) a);

            } else if (a instanceof FxGroupAssignment) {
                if (isDerived)
                    return derivedGroupComparison((FxGroupAssignment) a);
                else
                    return groupComparison((FxGroupAssignment) a);
            }
            return null;
        }

        /**
         * Performs a comparison between a given FxPropertyAssignment and its base property
         *
         * @param pa the FxPropertyAssignment
         * @return returns a List of String containing the builder map keys of differences
         */
        private static List<String> propComparison(FxPropertyAssignment pa) {
            final FxProperty prop = pa.getProperty();
            final List<String> result = new ArrayList<String>();

            if (prop.getACL() != pa.getACL())
                result.add("acl");

            final FxValue propDefault = prop.getDefaultValue();
            final FxValue paDefault = pa.getDefaultValue();
            if (propDefault != null && paDefault != null) {
                if (!propDefault.getBestTranslation().equals(paDefault.getBestTranslation()))
                    result.add("defaultValue");
            } else if (propDefault == null && paDefault != null) {
                result.add("defaultValue");
            }

            final String propHint = prop.getHint().getDefaultTranslation();
            final String paHint = pa.getHint().getDefaultTranslation();
            if (propHint != null && paHint != null) {
                if (!propHint.equals(paHint))
                    result.add("hint");
            } else if (propHint == null && paHint != null) {
                result.add("hint");
            }

            if (!prop.getLabel().getDefaultTranslation().equals(pa.getLabel().getDefaultTranslation()))
                result.add("label");

            if (prop.getMultiLines() != pa.getMultiLines())
                result.add("multiline");

            final int min = pa.getMultiplicity().getMin();
            final int max = pa.getMultiplicity().getMax();
            if (prop.getMultiplicity().getMin() != min
                    || prop.getMultiplicity().getMax() != max)
                result.add("multiplicity");

            if (prop.isMultiLang() != pa.isMultiLang())
                result.add("multilang");

            if (prop.getMaxLength() != pa.getMaxLength())
                result.add("maxLength");

            if (prop.isInOverview() != pa.isInOverview())
                result.add("inOverview");

            if (prop.isSearchable() != pa.isSearchable()) {
                result.add("searchable");
            }

            if (prop.isUseHTMLEditor() != pa.isUseHTMLEditor()) {
                result.add("useHtmlEditor");
            }

            // FxStructureOption differences
            List<FxStructureOption> propOptions = prop.getOptions();
            List<FxStructureOption> aOptions = pa.getOptions();

            Outer:
            for (FxStructureOption propOpt : propOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (propOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != propOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!propOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }

            return result;
        }

        /**
         * Compares a given FxPropertyAssignment with the settings of the assignment it was derived from
         *
         * @param pa the given FxPropertyAssignment
         * @return the List of builder map keys which are different
         */
        private static List<String> derivedPropComparison(FxPropertyAssignment pa) {
            final List<String> result = new ArrayList<String>();

            final FxPropertyAssignment base = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(pa.getBaseAssignmentId());

            if (base.getACL() != pa.getACL())
                result.add("acl");

            final FxValue baseDefault = base.getDefaultValue();
            final FxValue paDefault = pa.getDefaultValue();
            if (baseDefault != null && paDefault != null) {
                if (!baseDefault.getBestTranslation().equals(paDefault.getBestTranslation()))
                    result.add("defaultValue");
            } else if (baseDefault == null && paDefault != null) {
                result.add("defaultValue");
            }

            final String baseHint = base.getHint().getDefaultTranslation();
            final String paHint = pa.getHint().getDefaultTranslation();
            if (baseHint != null && paHint != null) {
                if (!baseHint.equals(paHint))
                    result.add("hint");
            } else if (baseHint == null && paHint != null) {
                result.add("hint");
            }

            if (!base.getLabel().getDefaultTranslation().equals(pa.getLabel().getDefaultTranslation()))
                result.add("label");

            if (base.getMultiLines() != pa.getMultiLines())
                result.add("multiline");

            final int min = pa.getMultiplicity().getMin();
            final int max = pa.getMultiplicity().getMax();
            if (base.getMultiplicity().getMin() != min
                    || base.getMultiplicity().getMax() != max)
                result.add("multiplicity");

            if (base.isMultiLang() != pa.isMultiLang())
                result.add("multilang");

            if (base.getMaxLength() != pa.getMaxLength())
                result.add("maxLength");

            if (base.isInOverview() != pa.isInOverview())
                result.add("inOverview");

            if (base.isSearchable() != pa.isSearchable()) {
                result.add("searchable");
            }

            if (base.isUseHTMLEditor() != pa.isUseHTMLEditor()) {
                result.add("useHtmlEditor");
            }

            // comparisons unique to assignments
            if (base.getDefaultMultiplicity() != pa.getDefaultMultiplicity()) {
                result.add("defaultMultiplicity");
            }

            if (!base.getAlias().equalsIgnoreCase(pa.getAlias())) {
                result.add("alias");
            }

            if (base.isEnabled() != pa.isEnabled()) {
                result.add("enabled");
            }

            if (base.getDefaultLanguage() != pa.getDefaultLanguage()) {
                result.add("defaultLanguage");
            }

            // FxStructureOption differences
            List<FxStructureOption> propOptions = base.getOptions();
            List<FxStructureOption> aOptions = pa.getOptions();

            Outer:
            for (FxStructureOption propOpt : propOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (propOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != propOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!propOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }

            return result;

        }

        /**
         * Performs a comparison between a given FxGroupAssignment and its base group
         *
         * @param ga the FxGroupAssignment
         * @return returns a List of String containing the builder map keys of differences
         */
        private static List<String> groupComparison(FxGroupAssignment ga) {
            final FxGroup group = ga.getGroup();
            List<String> result = new ArrayList<String>();

            final String groupHint = group.getHint().getDefaultTranslation();
            final String gaHint = ga.getHint().getDefaultTranslation();
            if (groupHint != null && gaHint != null) {
                if (!groupHint.equals(gaHint))
                    result.add("hint");
            } else if (groupHint == null && gaHint != null) {
                result.add("hint");
            }

            if (!group.getLabel().getDefaultTranslation().equals(ga.getLabel().getDefaultTranslation()))
                result.add("label");

            final int min = ga.getMultiplicity().getMin();
            final int max = ga.getMultiplicity().getMax();
            if (group.getMultiplicity().getMin() != min
                    || group.getMultiplicity().getMax() != max)
                result.add("multiplicity");

            List<FxStructureOption> groupOptions = group.getOptions();
            List<FxStructureOption> aOptions = ga.getOptions();

            Outer:
            for (FxStructureOption groupOpt : groupOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (groupOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != groupOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!groupOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }

            return result;
        }

        /**
         * Compares a given FxGroupAssignment with the settings of the assignment it was derived from
         *
         * @param ga the given FxPropertyAssignment
         * @return the List of builder map keys which are different
         */
        private static List<String> derivedGroupComparison(FxGroupAssignment ga) {
            final List<String> result = new ArrayList<String>();
            final FxGroupAssignment base = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(ga.getBaseAssignmentId());

            final String groupHint = base.getHint().getDefaultTranslation();
            final String gaHint = ga.getHint().getDefaultTranslation();
            if (groupHint != null && gaHint != null) {
                if (!groupHint.equals(gaHint))
                    result.add("hint");
            } else if (groupHint == null && gaHint != null) {
                result.add("hint");
            }

            if (!base.getLabel().getDefaultTranslation().equals(ga.getLabel().getDefaultTranslation()))
                result.add("label");

            final int min = ga.getMultiplicity().getMin();
            final int max = ga.getMultiplicity().getMax();
            if (base.getMultiplicity().getMin() != min
                    || base.getMultiplicity().getMax() != max)
                result.add("multiplicity");

            if (base.getDefaultMultiplicity() != ga.getDefaultMultiplicity())
                result.add("defaultMultiplicity");

            if (!base.getAlias().equalsIgnoreCase(ga.getAlias()))
                result.add("alias");

            if (base.isEnabled() != ga.isEnabled())
                result.add("enabled");

            if (base.getMode().getId() != ga.getMode().getId())
                result.add("groupMode");

            List<FxStructureOption> groupOptions = base.getOptions();
            List<FxStructureOption> aOptions = ga.getOptions();

            Outer:
            for (FxStructureOption groupOpt : groupOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (groupOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != groupOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!groupOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }

            return result;
        }
    }

    /**
     * Generate code to delete ALL content instances for the given types, their assignments and the types themselves
     *
     * @param types the input List of FxTypes t.b. deleted
     * @return the script code as a String
     */
    public static String createDeleteCode(List<FxType> types) {
        final StringBuilder script = new StringBuilder(500);

        // generate list of type ids
        script.append("def te = EJBLookup.getTypeEngine()\n")
                .append("def ce = EJBLookup.getContentEngine()\n")
                .append("def ae = EJBLookup.getAssignmentEngine()\n\n");
        script.append("def types = [");
        for (int i = 0; i < types.size(); i++) {
            script.append("\"")
                    .append(types.get(i).getName())
                    .append("\"");
            if (i < types.size() - 1)
                script.append(", ");
            else
                script.append("]\n\n");
        }

        // check if the types exist (1), remove the content instances (2), remove the types themselves (3)
        script.append("types.each {\n")
                .append("\tif (CacheAdmin.getEnvironment().typeExists(it)) {\n") // (1)
                .append("\t\tdef t = CacheAdmin.getEnvironment().getType(it)\n")
                .append("\t\ttry {\n")
                .append("\t\t\tdef pkList = ce.getPKsForType(t.getId(), false)\n")
                .append("\t\t\tpkList.each() {\n")
                .append("\t\t\t\tce.remove(it)\n") // (2)
                .append("\t\t\t}\n")
                .append("\t\t} catch(FxApplicationException e) {\n")
                .append("\t\t\treturn e.getCause().getMessage()\n\t\t}\n\n")
                .append("\t\ttry {\n")
                .append("\t\t\tte.remove(t.getId())\n") // (3)
                .append("\t\t} catch (FxApplicationException e) {\n")
                .append("\t\t\tif (e instanceof FxRemoveException) {\n")
                .append("\t\t\t\tt.getAssignedProperties().each {\n")
                .append("\t\t\t\t\tae.removeAssignment(it.getId())\n")
                .append("\t\t\t\t}\n")
                .append("\t\t\t\tt.getAssignedGroups().each {\n")
                .append("\t\t\t\t\tae.removeAssignment(it.getId())\n")
                .append("\t\t\t\t}\n")
                .append("\t\t\t\tte.remove(t.getId())\n")
                .append("\t\t\t}\n")
                .append("\t\t}\n")
                .append("\t}\n")
                .append("}\n\n");

        script.trimToSize();
        return script.toString();
    }

    /**
     * Checks if a given input String represents a Java or Groovy keyword
     *
     * @param input        the input String
     * @param doubleQuotes true = double quotes, false = single quotes
     * @return returns a quoted version of the same String if input == keyword
     */
    private static String keyWordNameCheck(String input, boolean doubleQuotes) {
        if (ArrayUtils.contains(JAVA_KEYWORDS, input) || ArrayUtils.contains(GROOVY_KEYWORDS, input)) {
            if (doubleQuotes)
                return "\"" + input + "\"";
            return "'" + input + "'";
        }
        return input;
    }

    /**
     * Generate code for the script assignment mappings
     *
     * @param typeScriptMapping       the type script mapping from the StructureExporterCallback
     * @param assignmentScriptMapping the assignment script mapping from the StructureExporterCallback
     * @return returns the Groovy code as a String
     */
    public static String createScriptAssignments(Map<Long, Map<String, List<Long>>> typeScriptMapping,
                                                 Map<Long, Map<String, List<Long>>> assignmentScriptMapping) {

        final StringBuilder script = new StringBuilder(5000);
        // TYPE SCRIPTS
        if (typeScriptMapping != null && typeScriptMapping.size() > 0) {
            script.append("\n// SCRIPTS ATTACHED TO TYPE EVENTS\n\n")
                    .append("def FxType currentType\n")
                    .append("def FxScriptInfo siType\n")
                    .append("def scriptCode\n"); // header f. types

            // traverse types and retrieve the scripts
            for (Long typeId : typeScriptMapping.keySet()) {
                final FxType t = CacheAdmin.getEnvironment().getType(typeId);
                final String typeName = t.getName();

                // traverse events for the given type
                final Map<String, List<Long>> eventScripts = typeScriptMapping.get(typeId);
                for (String event : eventScripts.keySet()) {
                    // retrieve the scripts for the given event
                    final List<Long> scriptsForEvent = eventScripts.get(event);

                    // traverse the script ids, retrieve the scripts and write out the code
                    for (Long scriptId : scriptsForEvent) {
                        final FxScriptInfo si = CacheAdmin.getEnvironment().getScript(scriptId);
                        script.append(writeTypeScriptCode(event, si, typeName));
                    }
                }
            }
        }

        // ASSIGNMENT SCRIPTS
        if (assignmentScriptMapping != null && assignmentScriptMapping.size() > 0) {
            script.append("\n// SCRIPTS ATTACHED TO ASSIGNMENT EVENTS\n\n")
                    .append("def FxAssignment currentAssignment\n")
                    .append("def FxScriptInfo siAss\n")
                    .append("def scriptCode\n"); // header f. assignments
            // traverse assignments and retrieve the scripts
            for (Long assId : assignmentScriptMapping.keySet()) {
                final FxAssignment a = CacheAdmin.getEnvironment().getAssignment(assId);
                final String XPath = a.getXPath();

                // traverse events for the given type
                final Map<String, List<Long>> eventScripts = assignmentScriptMapping.get(assId);
                for (String event : eventScripts.keySet()) {
                    // retrieve the scripts fo einer the given event
                    final List<Long> scriptsForEvent = eventScripts.get(event);

                    // traverse the script ids, retrieve the scripts and write out the code
                    for (Long scriptId : scriptsForEvent) {
                        final FxScriptInfo si = CacheAdmin.getEnvironment().getScript(scriptId);
                        script.append(writeAssignmentScriptCode(event, si, XPath));
                    }
                }
            }
        }

        script.trimToSize();
        return script.toString();
    }

    /**
     * @param event    the script event
     * @param si       the FxScriptInfo
     * @param typeName the type's name
     * @return returns the script assignment as a Groovy script
     */
    private static String writeTypeScriptCode(String event, FxScriptInfo si, String typeName) {
        final StringBuilder script = new StringBuilder(500);
        final FxScriptMapping sm = CacheAdmin.getEnvironment().getScriptMapping(si.getId());
        boolean derivedUsage = false;
        for (FxScriptMappingEntry sme : sm.getMappedTypes()) {
            if (sme.getScriptId() == si.getId()) {
                derivedUsage = sme.isDerivedUsage();
                break;
            }
        }

        final String scriptCode = processScriptCode(si.getCode());

        // load type in script, then append type name
        script.append("\n// ***** SCRIPT START ***** \n")
                .append("currentType = CacheAdmin.getEnvironment().getType(\"")
                .append(typeName)
                .append("\")\n\n")
                .append("scriptCode = \"\"\"")
                .append(scriptCode)
                .append("\"\"\"\n\n")
                .append("siType = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.")
                .append(event)
                .append(", \"")
                .append(si.getName())
                .append("\", \"")
                .append(si.getDescription())
                .append("\", scriptCode)\n\n")
                .append("EJBLookup.getScriptingEngine().createTypeScriptMapping(FxScriptEvent.")
                .append(event)
                .append(", siType.id, currentType.getId(), ")
                .append(si.isActive())
                .append(", ")
                .append(derivedUsage)
                .append(")\n***** SCRIPT END *****\n");

        return script.toString();
    }

    /**
     * @param event the event
     * @param si    the FxScriptInfo
     * @param XPath the XPath of the affected assignment
     * @return the script assignment code as a Groovy script
     */
    private static String writeAssignmentScriptCode(String event, FxScriptInfo si, String XPath) {
        final StringBuilder script = new StringBuilder(500);
        final FxScriptMapping sm = CacheAdmin.getEnvironment().getScriptMapping(si.getId());
        boolean derivedUsage = false;
        for (FxScriptMappingEntry sme : sm.getMappedAssignments()) {
            if (sme.getScriptId() == si.getId()) {
                derivedUsage = sme.isDerivedUsage();
                break;
            }
        }

        final String scriptCode = processScriptCode(si.getCode());

        // load assignment, then attach script to event and assignment id
        script.append("\n// ***** SCRIPT START ***** \n")
                .append("currentAssignment = CacheAdmin.getEnvironment().getAssignment(\"")
                .append(XPath)
                .append("\")\n\n")
                .append("scriptCode = \"\"\"")
                .append(scriptCode)
                .append("\"\"\"\n\n")
                .append("siAss = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.")
                .append(event)
                .append(", \"")
                .append(si.getName())
                .append("\", \"")
                .append(si.getDescription())
                .append("\", scriptCode)\n\n")
                .append("EJBLookup.getScriptingEngine().createAssignmentScriptMapping(FxScriptEvent.")
                .append(event)
                .append(", siAss.id, currentAssignment.getId(), ")
                .append(si.isActive())
                .append(", ")
                .append(derivedUsage)
                .append(")\n***** SCRIPT END *****\n");

        script.trimToSize();
        return script.toString();
    }

    /**
     * Escape special characters from the script code
     *
     * @param inputCode the input code
     * @return returns the escaped text
     */
    private static String processScriptCode(String inputCode) {
        inputCode = inputCode.replaceAll("\"", "\\\\\"");
        inputCode = inputCode.replaceAll("\r", "");
        return inputCode;
    }
}
