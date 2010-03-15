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

import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to handle structure options for FxTypes. Extends FxStructureOption
 * Overridable structure options: the flag has no impact whatsoever if the options is not passed on to a derived type
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTypeOption extends FxStructureOption {

    public final static String OPTION_MIMETYPE = "MIMETYPE";

    private boolean passedOn;
    private static final long serialVersionUID = 7709132491233564725L;

    public FxTypeOption(String key, boolean overridable, boolean passedOn, boolean set, String value) {
        super(key, overridable, set, value);
        this.passedOn = passedOn;
    }

    public FxTypeOption(String key, boolean overrideable, boolean set, String value) {
        super(key, overrideable, set, value);
        this.passedOn = false;
    }

    /**
     * Copy Constructor
     *
     * @param o an FxTypeOption
     */
    public FxTypeOption(FxTypeOption o) {
        super(o);
        this.passedOn = o.isPassedOn();
    }

    /**
     * Type option only: Will the option be passed on to derived types?
     *
     * @return option passed on (to derived types)?
     */
    public boolean isPassedOn() {
        return passedOn;
    }

    /**
     * Get a default entry for an unknown option
     *
     * @param key the key
     * @return an unknown option
     */
    private static FxTypeOption getUnknownOption(String key) {
        return new FxTypeOption(key, false, false, false, "");
    }

    /**
     * Get an option entry for the given key, if the key is invalid or not found a <code>FxTypeOption</code> object
     * will be returned with <code>set</code> set to <code>false</code>, overrideable set to <code>false</code> and value
     * set to an empty String.
     *
     * @param key     option key
     * @param options the available options
     * @return the found option or an object that indicates that the option is not set
     */
    public static FxTypeOption getOption(String key, List<FxTypeOption> options) {
        if (key != null)
            key = key.trim().toUpperCase();
        if (key == null || key.length() == 0 || options == null || options.size() == 0)
            return getUnknownOption(StringUtils.defaultString(key));
        for (FxTypeOption option : options)
            if (key.equals(option.getKey()))
                return option;
        return getUnknownOption(key);
    }

    /**
     * Get a list of empty type options
     *
     * @param capacity desired capacity
     * @return list of empty options
     */
    public static List<FxTypeOption> getEmptyTypeOptionList(int capacity) {
        return new ArrayList<FxTypeOption>(capacity);
    }

    /**
     * Set or add a String value in a list of options
     *
     * @param options     list of existing options
     * @param key         option key
     * @param overridable should the option be overrideable?
     * @param value       String value to set for the option
     */
    public static void setOption(List<FxTypeOption> options, String key, boolean overridable, String value) {
        setOption(options, key, overridable, false, value);
    }

    /**
     * Set or add a String value in a list of options
     *
     * @param options      list of existing options
     * @param key          option key
     * @param overrideable should the option be overrideable?
     * @param passedOn     FxTypes only: should the option be passed on to derived types?
     * @param value        String value to set for the option
     */
    public static void setOption(List<FxTypeOption> options, String key, boolean overrideable, boolean passedOn, String value) {
        synchronized (options) {
            if (StringUtils.isEmpty(key))
                throw new FxInvalidParameterException("key", "ex.structure.option.key.empty", value).asRuntimeException();
            if (hasOption(key, options)) {
                FxTypeOption opt = getOption(key, options);
                opt.overrideable = overrideable;
                opt.value = value;
                opt.set = true;
                opt.passedOn = passedOn;
                return;
            }
            FxTypeOption opt = new FxTypeOption(key, overrideable, passedOn, true, value);
            options.add(opt);
        }
    }

    /**
     * Check if an option is set for the requested key
     *
     * @param key     option key
     * @param options the available options
     * @return if an option is set for the requested key
     */
    public static boolean hasOption(String key, List<FxTypeOption> options) {
        if (key != null)
            key = key.trim().toUpperCase();
        if (key == null || key.length() == 0 || options == null || options.size() == 0)
            return false;
        for (FxTypeOption option : options)
            if (key.equals(option.getKey()))
                return true;
        return false;
    }

    /**
     * Set or add a boolean value in a list of options
     *
     * @param options      list of existing options
     * @param key          option key
     * @param overrideable should the option be overrideable?
     * @param value        boolean value to set for the option (will be converted internally to a String)
     */
    public static void setOption(List<FxTypeOption> options, String key, boolean overrideable, boolean value) {
        setOption(options, key, overrideable, false, value ? VALUE_TRUE : VALUE_FALSE);
    }

    /**
     * Set or add a boolean value in a list of options
     *
     * @param options      list of existing options
     * @param key          option key
     * @param overrideable should the option be overrideable?
     * @param passedOn     FxTypes only: should the option be passed on to derived types?
     * @param value        boolean value to set for the option (will be converted internally to a String)
     */
    public static void setOption(List<FxTypeOption> options, String key, boolean overrideable, boolean passedOn, boolean value) {
        setOption(options, key, overrideable, passedOn, value ? VALUE_TRUE : VALUE_FALSE);
    }

    /**
     * Clear the option with the given key - removing it from the list if it exists
     *
     * @param options list options to clear the option for
     * @param key     key of the option to remove
     */
    public static void clearOption(List<FxTypeOption> options, String key) {
        if (key != null)
            key = key.trim().toUpperCase();
        if (key == null || key.length() == 0 || options == null || options.size() == 0)
            return;
        synchronized (options) {
            for (FxTypeOption option : options) {
                if (key.equals(option.getKey())) {
                    options.remove(option);
                    return;
                }
            }
        }
    }

    /**
     * Get a copy of the a list of options
     *
     * @param options list to clone
     * @return cloned list of options
     */
    public static List<FxTypeOption> cloneOptions(List<FxTypeOption> options) {
        if (options == null)
            return null;
        else {
            ArrayList<FxTypeOption> clone = new ArrayList<FxTypeOption>(options.size());
            for (FxTypeOption o : options) {
                clone.add(new FxTypeOption(o));
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
    public static List<FxTypeOption> getUnmodifiableOptions(List<FxTypeOption> options) {
        return Collections.unmodifiableList(options);
    }

    /**
     * Checks for equality
     *
     * @return if the option's values equal this
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof FxTypeOption))
            return false;
        if (o == this)
            return true;
        final FxTypeOption other = (FxTypeOption) o;
        return !(!this.key.equals(other.key) || !this.value.equals(other.value)
                || this.overrideable != other.overrideable || this.passedOn != other.passedOn
                || this.set != other.set);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result;
        result = (key != null ? key.hashCode() : 0);
        result = 31 * result + (overrideable ? 1 : 0);
        result = 31 * result + (passedOn ? 1 : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (set ? 1 : 0);
        return result;
    }
}
