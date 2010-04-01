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
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * FxGroup used for structure editing
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxGroupEdit extends FxGroup {

    private static final long serialVersionUID = -889437501601209068L;
    private boolean isNew;
    private GroupMode mode = GroupMode.AnyOf;
    private int defaultMultiplicity = 1;

    /**
     * Make an editable instance of an existing group
     *
     * @param group existing group
     */
    public FxGroupEdit(FxGroup group) {
        super(group.getId(), group.getName(), group.getLabel().copy(), group.getHint().copy(),
                group.mayOverrideBaseMultiplicity(), new FxMultiplicity(group.getMultiplicity()),
                FxStructureOption.cloneOptions(group.options));
        this.isNew = false;
    }

    /**
     * Create a clone of an existing group with a new name
     *
     * @param base    group to clone
     * @param newName new name to assign
     * @return FxGroupEdit
     */
    public static FxGroupEdit createNew(FxGroup base, String newName) {
        FxGroupEdit ret = new FxGroupEdit(base).setName(newName);
        ret.isNew = true;
        return ret;
    }

    /**
     * Constructor
     *
     * @param name                     group name
     * @param label                    label
     * @param hint                     hint message
     * @param overrideBaseMultiplicity allow base multiplicity override?
     * @param multiplicity             multiplicity
     */
    private FxGroupEdit(String name, FxString label, FxString hint, boolean overrideBaseMultiplicity, FxMultiplicity multiplicity) {
        super(-1, name, label, hint, overrideBaseMultiplicity, multiplicity, FxStructureOption.getEmptyOptionList(2));
        setName(name);
        isNew = true;
    }

    /**
     * Create a new FxGroupEdit instance
     *
     * @param name                     group name
     * @param label                    label
     * @param hint                     hint message
     * @param overrideBaseMultiplicity allow base multiplicity override?
     * @param multiplicity             multiplicity
     * @return FxGroupEdit
     */
    public static FxGroupEdit createNew(String name, FxString label, FxString hint, boolean overrideBaseMultiplicity, FxMultiplicity multiplicity) {
        return new FxGroupEdit(name, label, hint, overrideBaseMultiplicity, multiplicity).setName(name);
    }

    /**
     * Is this a new group
     *
     * @return if this is a new group
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Set the name for the group
     *
     * @param name group name
     * @return this
     * @throws com.flexive.shared.exceptions.FxRuntimeException
     *          on errors
     */
    public FxGroupEdit setName(String name) {
        if (StringUtils.isEmpty(name))
            throw new FxInvalidParameterException("NAME", "ex.general.parameter.empty", "name").asRuntimeException();
        name = name.trim().toUpperCase();
        this.name = name;
        return this;
    }

    /**
     * Set the group mode. Only possible for new groups to reflect it to the new assignment
     *
     * @param mode group mode
     * @return this
     * @throws FxInvalidParameterException if group is not new
     */
    public FxGroupEdit setAssignmentGroupMode(GroupMode mode) throws FxInvalidParameterException {
        if (!isNew())
            throw new FxInvalidParameterException("mode", "ex.structure.group.notAllowed.mode");
        this.mode = mode;
        return this;
    }

    /**
     * Get the group mode if this GroupEdit is for a new group and assignment is to be created
     *
     * @return group mode
     */
    public GroupMode getAssignmentGroupMode() {
        return mode;
    }

    /**
     * Set the label
     *
     * @param label the label
     * @return this
     */
    public FxGroupEdit setLabel(FxString label) {
        this.label = label;
        return this;
    }

    /**
     * Set the hint message
     *
     * @param hint hint message
     * @return this
     */
    public FxGroupEdit setHint(FxString hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Set the multiplicity
     *
     * @param multiplicity multiplicity
     * @return this
     */
    public FxGroupEdit setMultiplicity(FxMultiplicity multiplicity) {
        this.multiplicity = multiplicity;
        return this;
    }

    /**
     * May the base multiplicity be overridden?
     *
     * @param overrideMultiplicity may the base multiplicity be overridden?
     * @return this
     */
    public FxGroupEdit setOverrideMultiplicity(boolean overrideMultiplicity) {
        this.overrideMultiplicity = overrideMultiplicity;
        return this;
    }

    /**
     * Set an option
     *
     * @param key          option key
     * @param overrideable is the option overrideable from assignments?
     * @param value        value of the option
     * @return the group itself, useful for chained calls
     */
    public FxGroupEdit setOption(String key, boolean overrideable, String value) {
        FxStructureOption.setOption(options, key, overrideable, value);
        return this;
    }

    /**
     * Set a boolean option
     *
     * @param key          option key
     * @param overrideable is the option overrideable from assignments?
     * @param value        value of the option
     * @return the group itself, useful for chained calls
     */
    public FxGroupEdit setOption(String key, boolean overrideable, boolean value) {
        FxStructureOption.setOption(options, key, overrideable, value);
        return this;
    }

    /**
     * Assign options
     *
     * @param options options to assign
     *
     * @since 3.1
     */
    public void setOptions(List<FxStructureOption> options) {
        this.options = options;
    }

    /**
     * Clear an option entry
     *
     * @param key option name
     */
    public void clearOption(String key) {
        FxStructureOption.clearOption(options, key);
    }

    /**
     * <b>This value set will only be used when creating a new property and will be set for the created assignment!</b>
     * Set the default multiplicity (used i.e. in user interfaces editors and determines the amount of values that will
     * be initialized when creating an empty element).
     * <p/>
     * If the set value is &lt; min or &gt; max multiplicity of this assignment it will
     * be auto adjusted to the next valid value without throwing an exception
     *
     * @param defaultMultiplicity the default multiplicity
     * @return this
     */
    public FxGroupEdit setAssignmentDefaultMultiplicity(int defaultMultiplicity) {
        this.defaultMultiplicity = defaultMultiplicity;
        return this;
    }

    /**
     * Get the created assignments default multiplicity
     *
     * @return default multiplicity
     */
    public int getAssignmentDefaultMultiplicity() {
        return defaultMultiplicity;
    }
}
