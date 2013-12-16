/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.war.beans.admin.structure;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.structure.FxStructureOption;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Conveniently wraps FxStructureOptions to simplify GUI Manipulaiton.
 * The OptionWrapper wraps the options of a structure element (group or property) and
 * its assignment. Options of the structure element override those of the assignment.
 * Provides Maps to verify if an option is valid or may be overwritten to enhance GUI
 * presentaiton.
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class OptionWrapper implements Serializable {
    private static final long serialVersionUID = -5173344480449973475L;
    private static String[] standardOptionKeys = {FxStructureOption.OPTION_HTML_EDITOR,
            FxStructureOption.OPTION_MULTILINE,
            FxStructureOption.OPTION_SEARCHABLE,
            FxStructureOption.OPTION_SHOW_OVERVIEW};

    private static String[] blacklistedOptionKeys = {
            FxStructureOption.OPTION_MULTILANG
    };

    private static String[] standardTypeOptionKeys = {
            // empty for now
    };

    private static String[] blacklistedTypeOptionKeys = {
            FxStructureOption.OPTION_MULTILANG,
            FxStructureOption.OPTION_MULTILINE,
            FxStructureOption.OPTION_SEARCHABLE,
            FxStructureOption.OPTION_SHOW_OVERVIEW
    };

    private List<WrappedOption> structureOptions = null;
    private List<WrappedOption> assignmentOptions = null;
    private List<WrappedOption> unoverridableAssignmentOptions = null;
    private List<WrappedOption> typeOptions = null;
    private Map<String, Boolean> assignmentOptionValidMap = null;
    private Map<String, Boolean> assignmentOptionOverridableMap = null;
    // private Map<String, Boolean> derivedAssignmentOptionOverridableMap = null;
    private Map<String, Boolean> structureOptionValidMap = null;
    private Map<String, Boolean> typeOptionValidMap = null;
    private boolean isDerivedAssignment = false;

    /**
     * Creates the OptionWrapper (For FxTypes)
     *
     * @param options            optionlist of the structure element (e.g. group or property)
     * @param addStandardOptions if the standard options (FxStructureOption.OPTION_...) should be added to the
     *                           structureOptions, if not alaready present
     */
    public OptionWrapper(List<FxStructureOption> options, boolean addStandardOptions) {
        this.typeOptions = new ArrayList<WrappedOption>();

        List<String> blacklisted = Arrays.asList(blacklistedTypeOptionKeys);

        if (options != null)
            for (FxStructureOption o : options) {
                if (!blacklisted.contains(o.getKey()))
                    this.typeOptions.add(new WrappedOption(o));
            }

        if (addStandardOptions) {
            List<WrappedOption> standardOptions = new ArrayList<WrappedOption>();
            for (String standardOptionKey : standardTypeOptionKeys) {
                standardOptions.add(new WrappedOption(standardOptionKey, FxStructureOption.VALUE_FALSE, true, true, true));
            }
            for (WrappedOption o : standardOptions) {
                if (!this.typeOptions.contains(o))
                    this.typeOptions.add(o);
            }
        }
    }

    /**
     * Creates the OptionWrapper
     *
     * @param structureOptions   optionlist of the structure element (e.g. group or property)
     * @param assignmentOptions  optionlist of the according assignment (e.g. groupassignment or propertyassignment)
     * @param addStandardOptions if the standard options (FxStructureOption.OPTION_...) should be added to the
     *                           structureOptions, if not alaready present
     */
    public OptionWrapper(List<FxStructureOption> structureOptions, List<FxStructureOption> assignmentOptions, boolean addStandardOptions) {
        this(structureOptions, assignmentOptions, addStandardOptions, false);
    }

    /**
     * Creates the OptionWrapper
     *
     * @param structureOptions   optionlist of the structure element (e.g. group or property)
     * @param assignmentOptions  optionlist of the according assignment (e.g. groupassignment or propertyassignment)
     * @param addStandardOptions if the standard options (FxStructureOption.OPTION_...) should be added to the
     *                           structureOptions, if not alaready present
     * @param isDerivedAssignment set this flag to indicate that all inherited, non-overridable options should NOT be added to the assignmentOptionOverridableMap
     */
    public OptionWrapper(List<FxStructureOption> structureOptions, List<FxStructureOption> assignmentOptions, boolean addStandardOptions, boolean isDerivedAssignment) {
        this.structureOptions = new ArrayList<WrappedOption>();
        this.assignmentOptions = new ArrayList<WrappedOption>();
        this.isDerivedAssignment = isDerivedAssignment;

        List<String> blacklisted = Arrays.asList(blacklistedOptionKeys);

        if (structureOptions != null)
            for (FxStructureOption o : structureOptions) {
                if (!blacklisted.contains(o.getKey()))
                    this.structureOptions.add(new WrappedOption(o));
            }
        if (assignmentOptions != null)
            for (FxStructureOption o : assignmentOptions) {
                if (!blacklisted.contains(o.getKey()))
                    this.assignmentOptions.add(new WrappedOption(o));
            }

        if (addStandardOptions) {
            List<WrappedOption> standardOptions = new ArrayList<WrappedOption>();
            for (String standardOptionKey : standardOptionKeys) {
                standardOptions.add(new WrappedOption(standardOptionKey, FxStructureOption.VALUE_FALSE, true, true, true));
            }
            for (WrappedOption o : standardOptions) {
                if (!this.structureOptions.contains(o))
                    this.structureOptions.add(o);
            }
        }
    }

    /**
     * Creates the OptionWrapper
     *
     * @param structureOptions      optionlist of the structure element (e.g. group or property)
     * @param assignmentOptions     optionlist of the according assignment (e.g. groupassignment or propertyassignment)
     * @param standardOptionKeyList a List of option keys from which new options will be created and added,
     *                              if not alaready present
     */
    public OptionWrapper(List<FxStructureOption> structureOptions, List<FxStructureOption> assignmentOptions, List<String> standardOptionKeyList) {
        this(structureOptions, assignmentOptions, false, false);
        List<WrappedOption> standardOptions = new ArrayList<WrappedOption>();
        for (String key : standardOptionKeyList) {
            standardOptions.add(new WrappedOption(key, FxStructureOption.VALUE_FALSE, false, true, true));
        }
        for (WrappedOption o : standardOptions) {
            if (!this.structureOptions.contains(o))
                this.structureOptions.add(o);
        }
    }

    /**
     * Sets an option with Key key to Value value. If isStructure==true, the option will be set
     * in the structureOptions list, else in the assignmentOptions list.
     * If the option is not present a new option will be created and set accordingly.
     *
     * @param isStructureOption if the option should be set/created in the structureOptions or assignmentOptions list
     * @param key               the Key
     * @param value             the Value as String
     */
    public void setOption(boolean isStructureOption, String key, String value) {
        WrappedOption o = new WrappedOption(key, value, false, true, false);
        if (isStructureOption) {
            if (!structureOptions.contains(o))
                structureOptions.add(new WrappedOption(key, value, false, true, true));
            else {
                List<WrappedOption> options = getAll(structureOptions, key);
                for (WrappedOption wo : options)
                    wo.setValue(value);
            }
        } else {
            if (!assignmentOptions.contains(o))
                assignmentOptions.add(new WrappedOption(key, value, false, true, true));
            else {
                List<WrappedOption> options = getAll(assignmentOptions, key);
                for (WrappedOption wo : options)
                    wo.setValue(value);
            }
        }
    }

    /**
     * Sets an option with Key key to Value value. If isStructure==true, the option will be set
     * in the structureOptions list, else in the assignmentOptions list.
     * If the option is not present a new option will be created and set accordingly
     *
     * @param isStructureOption if the option should be set/created in the structureOptions or assignmentOptions list
     * @param key               the Key
     * @param value             the Value as boolean
     */
    public void setOption(boolean isStructureOption, String key, boolean value) {
        WrappedOption o = new WrappedOption(key, value, false, true, true);
        if (isStructureOption) {
            if (!structureOptions.contains(o))
                structureOptions.add(new WrappedOption(key, value, false, true, true));
            else {
                List<WrappedOption> options = getAll(structureOptions, key);
                for (WrappedOption wo : options)
                    wo.setValue(value);
            }
        } else {
            if (!assignmentOptions.contains(o))
                assignmentOptions.add(new WrappedOption(key, value, false, true, true));
            else {
                List<WrappedOption> options = getAll(assignmentOptions, key);
                for (WrappedOption wo : options)
                    wo.setValue(value);
            }
        }
    }

    /**
     * Gets an option with Key key. If isStructure==true, the option will be from the
     * structureOptions list, else from the assignmentOptions list.
     * If the option is not present a new option will be created and set with default values.
     *
     * @param isStructureOption if the option should be set/created in the structureOptions or assignmentOptions list
     * @param key               the Key
     * @return the option with the matching key.
     */
    public WrappedOption getOption(boolean isStructureOption, String key) {
        WrappedOption o = new WrappedOption(key, FxStructureOption.VALUE_FALSE, false, true, true);
        if (isStructureOption) {
            if (!structureOptions.contains(o)) {
                WrappedOption newOption = new WrappedOption(key, FxStructureOption.VALUE_FALSE, false, true, true);
                structureOptions.add(newOption);
                return newOption;
            } else
                return getFirst(structureOptions, key);
        } else {
            if (!assignmentOptions.contains(o)) {
                WrappedOption newOption = new WrappedOption(key, FxStructureOption.VALUE_FALSE, false, true, true);
                assignmentOptions.add(newOption);
                return newOption;
            } else
                return getFirst(assignmentOptions, key);
        }
    }

    /**
     * Gets an option with Key key. If isStructure==true, the option will be from the
     * structureOptions list, else from the assignmentOptions list.
     * If the option is not present, null will be returned.
     *
     * @param isStructureOption if the option should be set/created in the structureOptions or assignmentOptions list
     * @param key               the Key
     * @return the option with the matching key or null if the option is not present.
     */
    public WrappedOption getOptionNoCreate(boolean isStructureOption, String key) {
        WrappedOption o = new WrappedOption(key, FxStructureOption.VALUE_FALSE, false, true, true);
        if (isStructureOption) {
            if (!structureOptions.contains(o)) {
                return null;
            } else
                return getFirst(structureOptions, key);
        } else {
            if (!assignmentOptions.contains(o)) {
                return null;
            } else
                return getFirst(assignmentOptions, key);
        }
    }

    /**
     * Adds a new option to the specified options list.
     *
     * @param options     the option list to add the new option to
     * @param key         the option key
     * @param value       the option value
     * @param overridable the option overridable flag
     * @throws FxInvalidParameterException on errors (empty key or empty value)
     */
    public void addOption(List<WrappedOption> options, String key, String value, boolean overridable) throws FxInvalidParameterException {
        addOption(options, key, value, overridable, true);
    }

    /**
     * Adds a new option to the specified options list.
     *
     * @param options     the option list to add the new option to
     * @param key         the option key
     * @param value       the option value
     * @param overridable the option overridable flag
     * @param isInherited the option isInherited flag
     * @throws FxInvalidParameterException on errors (empty key or empty value)
     */
    public void addOption(List<WrappedOption> options, String key, String value, boolean overridable, boolean isInherited) throws FxInvalidParameterException {
        if (!StringUtils.isEmpty(key)) {
            if (!StringUtils.isEmpty(value)) {
                options.add(new WrappedOption(key, value, overridable, true, isInherited));
            } else
                throw new FxInvalidParameterException("value", "ex.optionWrapper.noValue");
        } else
            throw new FxInvalidParameterException("key", "ex.optionWrapper.noKey");
    }

    public void deleteOption(List<WrappedOption> options, WrappedOption o) {
        int deleteIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsCompletely(o)) {
                deleteIndex = i;
                break;
            }
        }
        if (deleteIndex != -1)
            options.remove(deleteIndex);
        else
            options.remove(o);
    }

    public List<WrappedOption> getStructureOptions() {
        return structureOptions;
    }

    public List<WrappedOption> getAssignmentOptions() {
        return assignmentOptions;
    }

    public List<WrappedOption> getTypeOptions() {
        return typeOptions;
    }

    public List<FxStructureOption> asFxStructureOptionList(List<WrappedOption> options) {
        List<FxStructureOption> converted = new ArrayList<FxStructureOption>(options.size());
        for (WrappedOption o : options) {
            converted.add(o.asFxStructureOption());
        }
        return converted;
    }

    private boolean mayOverrideOption(String key, boolean isDerivedAssignment) {
        boolean mayOverride = true;
        for (WrappedOption o : structureOptions) {
            if (o.key.equals(key) && !o.isOverridable()) {
                mayOverride = false;
                break;
            }
        }
        if (isDerivedAssignment) {
            for (WrappedOption o : assignmentOptions) {
                if (o.key.equals(key) && !o.isOverridable() && o.getIsInherited()) {
                    mayOverride = false;
                    break;
                }
            }
        }
        return mayOverride;
    }

    private boolean mayOverrideAssignmentOption(String key) { // TODO obsolete?
        for (WrappedOption o : assignmentOptions) {
            if (o.key.equals(key) && !o.isOverridable() && o.getIsInherited())
                return false;
        }
        return true;
    }

    private boolean isRedundant(String key) {
        WrappedOption o = getFirst(structureOptions, key);
        return o != null && o.equalsKeyAndValue(getFirst(assignmentOptions, key));
    }

    private WrappedOption getFirst(List<WrappedOption> options, String key) {
        WrappedOption result = null;
        for (WrappedOption o : options) {
            if (o.key.equals(key))
                return o;
        }
        return result;
    }

    /**
     * @param options the options list
     * @param key     the key
     * @return all options with Key key
     */
    private List<WrappedOption> getAll(List<WrappedOption> options, String key) {
        List<WrappedOption> result = new ArrayList<WrappedOption>();
        for (WrappedOption o : options) {
            if (o.key.equals(key))
                result.add(o);
        }
        return result;
    }

    public boolean hasOption(List<WrappedOption> options, String key) {
        return countKeyOccurence(options, key) > 0;
    }

    private int countKeyOccurence(List<WrappedOption> options, String key) {
        int c = 0;
        for (WrappedOption o : options) {
            if (o.getKey().toUpperCase().equals(key.toUpperCase()))
                c++;
        }
        return c;
    }

    /**
     * A Map to indicate if an assignment option for a given key is valid.
     *
     * @return a Map for JSF pages which indicates if an assignment option for a given key is valid.
     */
    public Map<String, Boolean> getIsAssignmentOptionValidMap() {
        if (assignmentOptionValidMap == null) {
            assignmentOptionValidMap = new HashMap<String, Boolean>() {
                @Override
                public Boolean get(Object key) {
                    return !(key == null || "".equals(key.toString().trim()) || countKeyOccurence(assignmentOptions, (String) key) > 1
                            || isRedundant((String) key));
                }
            };
        }
        return assignmentOptionValidMap;
    }

    /**
     * A Map to indicate if an assignment option for a given key may be overridden.
     *
     * @return a Map for JSF pages which indicates if an assignment option for a given key may be overridden.
     */
    public Map<String, Boolean> getIsAssignmentOptionOverridableMap() {
        if (assignmentOptionOverridableMap == null) {
            assignmentOptionOverridableMap = new HashMap<String, Boolean>() {
                @Override
                public Boolean get(Object key) {
                    return mayOverrideOption((String) key, isDerivedAssignment);
                }
            };
        }
        return assignmentOptionOverridableMap;
    }

    /**
     * A Map to indicate if a derived assignment's option for a given key may be overridden.
     * // TODO
     * @return a Map for JSF pages which indicates if a derived assignment option for a given key may be overridden.
     */
//    public Map<String, Boolean> getIsDerivedAssignmentOptionOverridableMap() {
//        if(derivedAssignmentOptionOverridableMap == null) {
//            derivedAssignmentOptionOverridableMap = new HashMap<String, Boolean>() {
//                private static final long serialVersionUID = -5743838788953000568L;
//
//                @Override
//                public Boolean get(Object key) {
//                    return mayOverrideAssignmentOption((String)key);
//                }
//            };
//        }
//        return derivedAssignmentOptionOverridableMap;
//    }

    /**
     * A Map to indicate if a structure option for a given key is valid.
     *
     * @return a Map for JSF pages which indicates if a structure option for a given key is valid.
     */
    public Map<String, Boolean> getIsStructureOptionValidMap() {
        if (structureOptionValidMap == null) {
            structureOptionValidMap = new HashMap<String, Boolean>() {
                @Override
                public Boolean get(Object key) {
                    return !(key == null || "".equals(key.toString().trim()) || countKeyOccurence(structureOptions, (String) key) > 1);
                }
            };
        }
        return structureOptionValidMap;
    }

    /**
     * A Map to indicate if a structure option (for types) for a given key is valid.
     *
     * @return a Map for JSF pages which indicates if a structure option for a given key is valid.
     */
    public Map<String, Boolean> getIsTypeOptionValidMap() {
        if (typeOptionValidMap == null) {
            typeOptionValidMap = new HashMap<String, Boolean>() {
                @Override
                public Boolean get(Object key) {
                    return !(key == null || "".equals(key.toString().trim()) || countKeyOccurence(typeOptions, (String) key) > 1);
                }
            };
        }
        return typeOptionValidMap;
    }

    /**
     * Wraps the GUI relevant information of FxStructureOption Objects and provides convenient setters and getters
     */
    public static class WrappedOption implements Serializable {
        private static final long serialVersionUID = -2746571577512522976L;

        private String key;
        private String value;
        private boolean overridable;
        private boolean set;
        private boolean isInherited;

        public WrappedOption(String key, String value, boolean overridable, boolean set, boolean isInherited) {
            setKey(key);
            this.value = value;
            this.overridable = overridable;
            this.set = set;
            this.isInherited = isInherited;
        }

        public WrappedOption(String key, boolean value, boolean overridable, boolean set, boolean isInherited) {
            setKey(key);
            this.value = asStringValue(value);
            this.overridable = overridable;
            this.set = set;
            this.isInherited = isInherited;
        }

        public WrappedOption(FxStructureOption option) {
            this.key = option.getKey().toUpperCase();
            this.value = option.getValue();
            this.overridable = option.isOverridable();
            this.set = option.isSet();
            this.isInherited = option.getIsInherited();
        }

//        /**
//         * Ctor w/ an additional flag for derived assignments and their respective inherited options
//         * Creates a sep. list of unoverridable derived options
//         *
//         * @param option the FxStructureOption
//         * @param checkDerivedAssignmentOptions set to true if // TODO: bla
//         */
//        public WrappedOption(FxStructureOption option, boolean checkDerivedAssignmentOptions) {
//            if(checkDerivedAssignmentOptions) {
//                
//            } else
//               new WrappedOption(option);
//        }

        private String asStringValue(boolean value) {
            if (value)
                return FxStructureOption.VALUE_TRUE;
            else return FxStructureOption.VALUE_FALSE;
        }

        public FxStructureOption asFxStructureOption() {
            return new FxStructureOption(this.key, this.overridable, this.set, this.isInherited, this.value);
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setValue(boolean value) {
            this.value = asStringValue(value);
        }

        public void setKey(String k) {
            if (k == null)
                k = "";
            this.key = k.toUpperCase();
        }

        public String getKey() {
            return key;
        }

        /**
         * Hack used for commandButtons to concatenate id's and gain "unique" id's for buttons in
         * &lt;ui:repeat&gt; tags
         *
         * @return id
         */
        public String getId() {
            int result;
            result = (key != null ? key.hashCode() : 0);
            result = 31 * result + (overridable ? 1 : 0);
            result = 31 * result + (value != null ? value.hashCode() : 0);
            result = 31 * result + (set ? 1 : 0);
            if (result < 0)
                result = result * -1;
            return String.valueOf(result);
        }

        public String getValue() {
            return value;
        }

        public boolean getBooleanValue() {
            return FxStructureOption.VALUE_TRUE.equals(value);
        }

        public boolean isOverridable() {
            return overridable;
        }

        public void setOverridable(boolean b) {
            this.overridable = b;
        }

        public boolean isSet() {
            return set;
        }

        public boolean getIsInherited() {
            return isInherited;
        }

        public void setIsInherited(boolean inherited) {
            isInherited = inherited;
        }

        @Override
        public boolean equals(Object o) {
            return !(o == null || !(o instanceof WrappedOption)) && this.key.equals(((WrappedOption) o).getKey().toUpperCase());
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        public boolean equalsCompletely(WrappedOption o) {
            if (o == null)
                return false;
            if (this.key.equals(o.key) && this.set == o.set && this.overridable == o.overridable && this.isInherited == o.isInherited)
                if (this.value != null && this.value.equals(o.value) || this.value == null && o.value == null)
                    return true;
            return false;
        }

        public boolean equalsKeyAndValue(WrappedOption o) {
            if (o == null)
                return false;
            if (this.key.equals(o.key))
                if (this.value != null && this.value.equals(o.value) || this.value == null && o.value == null)
                    return true;
            return false;
        }
    }
}

