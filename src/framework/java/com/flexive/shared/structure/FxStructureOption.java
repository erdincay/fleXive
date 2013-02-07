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
package com.flexive.shared.structure;

import com.flexive.shared.XPathElement;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Option for structure elements (groups, properties, assignments)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxStructureOption implements Serializable {

    private static final long serialVersionUID = 737017384585248182L;
    public final static String OPTION_MULTILANG = "MULTILANG";
    /**
     * Fulltext indexed?
     * @since 3.1
     */
    public final static String OPTION_FULLTEXT = "FULLTEXT";
    public final static String OPTION_SHOW_OVERVIEW = "SHOW.OVERVIEW";
    public final static String OPTION_HTML_EDITOR = "HTML.EDITOR";
    public final static String OPTION_SEARCHABLE = "SEARCHABLE";
    public final static String OPTION_MAXLENGTH = "MAXLENGTH";
    /**
     * Force an explicit select box with all existing instances for FxReference properties.
     *
     * @since 3.1 
     */
    public static final String OPTION_REFERENCE_SELECTONE = "REFERENCE.SELECTONE";
    /**
     * Use checkboxes instead of a multi-select list for FxSelectMany properties.
     *
     * @since 3.1
     */
    public static final String OPTION_SELECTMANY_CHECKBOXES = "SELECTMANY.CHECKBOXES";
    /**
     * Mime type option
     *
     * @since 3.1
     */
    public final static String OPTION_MIMETYPE = "MIMETYPE";
    public final static String OPTION_MULTILINE = "MULTILINE";
    public final static String VALUE_TRUE = "1";
    public final static String VALUE_FALSE = "0";
    
    private String key;
    protected boolean overridable;
    private String value;
    private boolean set;
    /**
     * @since 3.1
     */
    private boolean isInherited;

    /**
     * Ctor
     *
     * @param key          key identifying the option
     * @param overridable is the option overridable in assignments
     * @param set          is the option set? (non-existing options are returned as not-set options!)
     * @param value        the options value
     */
    public FxStructureOption(String key, boolean overridable, boolean set, String value) {
        this.key = XPathElement.xpToUpperCase(key);
        this.overridable = overridable;
        this.set = set;
        this.value = value;
        this.isInherited = false;
    }

    /**
     * Ctor w/ inheritance option
     *
     * @param key          key identifying the option
     * @param overridable is the option overridable in assignments
     * @param set          is the option set? (non-existing options are returned as not-set options!)
     * @param isInherited  option inherited by derived structures?
     * @param value        the options value
     *
     * @since 3.1
     */
    public FxStructureOption(String key, boolean overridable, boolean set, boolean isInherited, String value) {
        this.key = XPathElement.xpToUpperCase(key);
        this.overridable = overridable;
        this.set = set;
        this.value = value;
        this.isInherited = isInherited;
    }


    /**
     * Copy Constructor
     *
     * @param o an FxStructureOption
     */
    public FxStructureOption(FxStructureOption o) {
        this.key = o.key;
        this.overridable = o.overridable;
        this.set = o.set;
        this.value = o.value;
        this.isInherited = o.isInherited;
    }

    /**
     * Get the option key
     *
     * @return option key
     */
    public String getKey() {
        return key;
    }

    /**
     * Is the option overridable (in assignments)?
     *
     * @return option overridable (in assignments)?
     */
    public boolean isOverridable() {
        return overridable;
    }

    /**
     * Is the option set, will return <code>false</code> if an unknown option is requested
     *
     * @return if option is set, will return <code>false</code> if an unknown option is requested
     */
    public boolean isSet() {
        return set;
    }

    /**
     * Test if the option need to be saved
     * 
     * @return <code>true</code> if the option is set and the value is not empty
     * @since 3.1
     */
    public boolean isValid() {
        return set && !StringUtils.isEmpty(value);
    }

    /**
     * Get the value assigned to the option
     *
     * @return value assigned to the option
     */
    public String getValue() {
        return value;
    }

    /**
     * Is the option inherited by derived structures?
     *
     * @return true if the option is inherited by derived structures
     *
     * @since 3.1
     */
    public boolean getIsInherited() {
        return isInherited;
    }

    /**
     * Set to true if the option should be inherited by derived structures
     *
     * @param isInherited flag
     *
     * @since 3.1
     */
    public void setIsInherited(boolean isInherited) {
        this.isInherited = isInherited;
    }

    /**
     * Get the value as integer
     *
     * @return value as integer
     */
    public int getIntValue() {
        String value = getValue();
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Convenience method to check if value is set to true
     *
     * @return if value is set to true
     */
    public boolean isValueTrue() {
        return VALUE_TRUE.equals(value);
    }

    /**
     * Check if an option is set for the requested key
     *
     * @param key     option key
     * @param options the available options
     * @return if an option is set for the requested key
     */
    public static boolean hasOption(String key, List<FxStructureOption> options) {
        if (options == null || options.isEmpty()) {
            return false;
        }
        if (key == null || key.length() == 0)
            return false;
        key = XPathElement.xpToUpperCase(key.trim());
        for (FxStructureOption option : options)
            if (key.equals(option.getKey()))
                return true;
        return false;
    }

    /**
     * Get a default entry for an unknown option
     *
     * @param key the key
     * @return an unknown option
     */
    private static FxStructureOption getUnknownOption(String key) {
        return new FxStructureOption(key, true, false, false, "");
    }

    /**
     * Get an option entry for the given key, if the key is invalid or not found a <code>FxStructureOption</code> object
     * will be returned with <code>set</code> set to <code>false</code>, overridable set to <code>false</code> and value
     * set to an empty String.
     *
     * @param key     option key
     * @param options the available options
     * @return the found option or an object that indicates that the option is not set
     */
    public static FxStructureOption getOption(String key, List<FxStructureOption> options) {
        if (key != null)
            key = XPathElement.xpToUpperCase(key.trim());
        if (key == null || key.length() == 0 || options == null || options.size() == 0)
            return getUnknownOption(StringUtils.defaultString(key));
        for (FxStructureOption option : options)
            if (key.equals(option.getKey()))
                return option;
        return getUnknownOption(key);
    }

    /**
     * Get a list of empty options
     *
     * @param capacity desired capacity
     * @return list of empty options
     */
    public static List<FxStructureOption> getEmptyOptionList(int capacity) {
        return new ArrayList<FxStructureOption>(capacity);
    }

    /**
     * Set or add a String value in a list of options
     *
     * @param options      list of existing options
     * @param key          option key
     * @param overridable should the option be overridable?
     * @param value        String value to set for the option
     */
    public static void setOption(List<FxStructureOption> options, String key, boolean overridable, String value) {
        setOption(options, key, overridable, false, value);
    }

    /**
     * Set or add a String value in a list of options
     *
     * @param options      list of existing options
     * @param key          option key
     * @param overridable should the option be overridable?
     * @param isInherited  is the option inherited by derived structures?
     * @param value        String value to set for the option
     *
     * @since 3.1
     */
    public static void setOption(List<FxStructureOption> options, String key, boolean overridable, boolean isInherited, String value) {
        synchronized (options) {
            if (StringUtils.isEmpty(key))
                throw new FxInvalidParameterException("key","ex.structure.option.key.empty", value).asRuntimeException();
            if (hasOption(key, options)) {
                FxStructureOption opt = getOption(key, options);
                opt.overridable = overridable;
                opt.value = value;
                opt.set = true;
                opt.isInherited = isInherited;
                return;
            }
            FxStructureOption opt = new FxStructureOption(key, overridable, true, isInherited, value);
            options.add(opt);
        }
    }

    /**
     * Set or add a boolean value in a list of options
     *
     * @param options      list of existing options
     * @param key          option key
     * @param overridable should the option be overridable?
     * @param value        boolean value to set for the option (will be converted internally to a String)
     */
    public static void setOption(List<FxStructureOption> options, String key, boolean overridable, boolean value) {
        setOption(options, key, overridable, false, value ? VALUE_TRUE : VALUE_FALSE);
    }

    /**
     * Set or add a boolean value in a list of options
     *
     * @param options      list of existing options
     * @param key          option key
     * @param overridable should the option be overridable?
     * @param isInherited  should the option be inherited by derived structures?
     * @param value        boolean value to set for the option (will be converted internally to a String)
     */
    public static void setOption(List<FxStructureOption> options, String key, boolean overridable, boolean isInherited, boolean value) {
        setOption(options, key, overridable, isInherited, value ? VALUE_TRUE : VALUE_FALSE);
    }

    /**
     * Clear the option with the given key - removing it from the list if it exists
     *
     * @param options list options to clear the option for
     * @param key     key of the option to remove
     */
    public static void clearOption(List<FxStructureOption> options, String key) {
        if (key != null)
            key = key.trim().toUpperCase();
        if (key == null || key.length() == 0 || options == null || options.size() == 0)
            return;
        synchronized (options) {
            for (FxStructureOption option : options)
                if (key.equals(option.getKey())) {
                    options.remove(option);
                    return;
                }
        }
    }

    /**
     * Get a copy of a list of options
     *
     * @param options list to clone
     * @return cloned list of options
     */
    public static List<FxStructureOption> cloneOptions(List<FxStructureOption> options) {
        return cloneOptions(options, false);
    }

    /**
     * Get a copy of a list of options - optionally only those which have their isInherited flag set to true
     *
     * @param options list to clone
     * @param isInheritedOnly return only options whose isInherited flag is true
     * @return cloned list of options
     * @since 3.1.1
     */
    public static List<FxStructureOption> cloneOptions(List<FxStructureOption> options, boolean isInheritedOnly) {
         if (options == null)
            return null;
        else {
            ArrayList<FxStructureOption> clone = new ArrayList<FxStructureOption>(options.size());
            for (FxStructureOption o : options) {
                if(isInheritedOnly) {
                    if(o.getIsInherited())
                        clone.add(new FxStructureOption(o));
                } else
                    clone.add(new FxStructureOption(o));
            }
            return clone;
        }
    }

    /**
     * Convert a list of options to an unmodifieable list
     *
     * @param options list to convert
     * @return unmodifieable list of options
     */
    public static List<FxStructureOption> getUnmodifieableOptions(List<FxStructureOption> options) {
        return Collections.unmodifiableList(options);
    }

    /**
     * Internal option lookup that doesn't create temporary option instance and works only with upper-case option names
     *
     * @param opt        the option key (in upper case)
     * @param options    the options
     * @return  the option instance, when it exists
     */
    static FxStructureOption findOption(String opt, List<FxStructureOption> options) {
        if (options == null) {
            return null;
        }
        for (FxStructureOption option : options) {
            if (option.getKey().equals(opt)) {
                return option;
            }
        }
        return null;
    }

    /**
     * Checks for equality
     *
     * @return if the option's values equal this
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof FxStructureOption))
            return false;
        if (o == this)
            return true;
        final FxStructureOption other = (FxStructureOption) o;
        return !(!this.key.equals(other.key) || !this.value.equals(other.value) ||
                this.overridable != other.overridable || this.set != other.set ||
                this.isInherited != other.isInherited);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result;
        result = (key != null ? key.hashCode() : 0);
        result = 31 * result + (overridable ? 1 : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (set ? 1 : 0);
        result = 31 * result + (isInherited ? 1 : 0);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[key:" + key + "|value:" + value + "]";
    }
}
