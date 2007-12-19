/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
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
package com.flexive.shared.structure;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.XPathElement;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * FxPropertyAssignment for editing
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPropertyAssignmentEdit extends FxPropertyAssignment {

    private static final long serialVersionUID = -4124525757694024524L;
    private boolean isNew;

    /**
     * Create an editable instance from an existing FxPropertyAssignment
     *
     * @param pa existing FxPropertyAssignment
     */
    public FxPropertyAssignmentEdit(FxPropertyAssignment pa) {
        super(pa.getId(), pa.isEnabled(), pa.getAssignedType(), pa.getAlias(), pa.getXPath(),
                pa.getPosition(), new FxMultiplicity(pa.getMultiplicity()), pa.getDefaultMultiplicity(),
                pa.getParentGroupAssignment(), pa.getBaseAssignmentId(), pa.getLabel() == null ? null : pa.getLabel().copy(),
                pa.getHint() == null ? null : pa.getHint().copy(), pa.getDefaultValue() == null ? null : pa.getDefaultValue().copy(),
                pa.getProperty().asEditable(), new ACL(pa.getACL()), pa.getDefaultLanguage(), FxStructureOption.cloneOptions(pa.options));
        if (pa.isSystemInternal())
            _setSystemInternal();
        this.isNew = false;
    }


    /**
     * Ctor to create a new FxPropertyAssignmentEdit from an existing FxPropertyAssignment as a new one for a given type with a
     * new alias and a given parentXPath (not validated)
     *
     * @param pa          original property assignment
     * @param type        type to assign it
     * @param alias       new alias
     * @param parentXPath parent XPath within the type to assign
     * @param parent      optional parent assignment if already known (prevents lookup of parentXPath if valid)
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    private FxPropertyAssignmentEdit(FxPropertyAssignment pa, FxType type, String alias, String parentXPath, FxAssignment parent) throws FxNotFoundException, FxInvalidParameterException {
        super(-1, pa.isEnabled(), type, alias, XPathElement.buildXPath(false, parentXPath, alias), pa.getPosition(),
                new FxMultiplicity(pa.getMultiplicity()), pa.getDefaultMultiplicity(), pa.getParentGroupAssignment(), pa.getId(), pa.getLabel().copy(),
                pa.getHint().copy(), pa.getDefaultValue().copy(), pa.getProperty().asEditable(), new ACL(pa.getACL()), pa.getDefaultLanguage(), FxStructureOption.cloneOptions(pa.options));
        if (pa.isSystemInternal())
            _setSystemInternal();
        if (parent == null) {
            //check parentXPath
            parent = type.getAssignment(parentXPath);
            if (parent != null && parent instanceof FxPropertyAssignment)
                throw new FxInvalidParameterException("parentXPath", "ex.structure.assignment.noGroup", parentXPath);
        }
        //check parentXPath
        if (parent == null)
            parentGroupAssignment = null;
        else
            parentGroupAssignment = (FxGroupAssignment) parent;
        isNew = true;
    }

    public boolean isNew() {
        return isNew;
    }

    public FxPropertyAssignmentEdit setACL(ACL ACL) throws FxInvalidParameterException {
        if (!getProperty().mayOverrideACL())
            throw new FxInvalidParameterException("ACL", "ex.structure.override.property.forbidden", "ACL", getProperty().getName());
        this.ACL = ACL;
        return this;
    }

    /**
     * Set the default value for this property assignment
     *
     * @param defaultValue default value
     * @return this
     */
    public FxPropertyAssignmentEdit setDefaultValue(FxValue defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Set an option
     *
     * @param key   option key
     * @param value value of the option
     * @return the assignment itself, useful for chained calls
     * @throws FxInvalidParameterException if the property does not allow overriding
     */
    public FxPropertyAssignmentEdit setOption(String key, String value) throws FxInvalidParameterException {

        FxStructureOption pOpt = getProperty().getOption(key);
        if (pOpt.isSet() && !pOpt.isOverrideable())
            throw new FxInvalidParameterException(key, "ex.structure.override.forbidden", key, getProperty().getName());

        FxStructureOption.setOption(options, key, true, value);
        return this;
    }

    /**
     * Set a boolean option
     *
     * @param key   option key
     * @param value value of the option
     * @return the assignemnt itself, useful for chained calls
     * @throws FxInvalidParameterException if the property does not allow overriding
     */
    public FxPropertyAssignmentEdit setOption(String key, boolean value) throws FxInvalidParameterException {

        FxStructureOption pOpt = getProperty().getOption(key);
        if (pOpt.isSet() && !pOpt.isOverrideable())
            throw new FxInvalidParameterException(key, "ex.structure.override.forbidden", key, getProperty().getName());

        FxStructureOption.setOption(options, key, true, value);
        return this;
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
     * Should this assignment support multilingual data?
     *
     * @param multiLang multi lingual data supported?
     * @return this
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setMultiLang(boolean multiLang) throws FxInvalidParameterException {
        return setOption(FxStructureOption.OPTION_MULTILANG, multiLang);
    }

    /**
     * Set a default language. Multilingual FxPropertyData instances will be initialized with this language
     *
     * @param language the default language
     * @return this
     */
    public FxPropertyAssignmentEdit setDefaultLanguage(long language) {
        if (this.isMultiLang())
            this.defaultLang = language;
        return this;
    }

    /**
     * Reset the default language to the system language
     *
     * @return this
     */
    public FxPropertyAssignmentEdit clearDefaultLanguage() {
        if (this.isMultiLang())
            this.defaultLang = FxLanguage.SYSTEM_ID;
        return this;
    }

    /**
     * Set if  this property be used in the visual query editor (UI hint)
     *
     * @param searchable property can be used in the visual query editor
     * @return the property itself, useful for chained calls
     * @throws FxInvalidParameterException if overriding is not allowed
     */
    public FxPropertyAssignmentEdit setSearchable(boolean searchable) throws FxInvalidParameterException {
        return setOption(FxStructureOption.OPTION_SEARCHABLE, searchable);
    }

    /**
     * Set overview appearance setting
     *
     * @param inOverview overview appearance setting
     * @return the property itself, useful for chained calls
     * @throws FxInvalidParameterException if not allowed to override
     */
    public FxPropertyAssignmentEdit setInOverview(boolean inOverview) throws FxInvalidParameterException {
        return setOption(FxStructureOption.OPTION_SHOW_OVERVIEW, inOverview);
    }

    /**
     * Set if to use an HTML editor to edit values of this property?
     *
     * @param useHTMLEditor use HTML editor to edit values of this property?
     * @return the property itself, useful for chained calls
     * @throws FxInvalidParameterException if not allowed to override
     */
    public FxPropertyAssignmentEdit setUseHTMLEditor(boolean useHTMLEditor) throws FxInvalidParameterException {
        return setOption(FxStructureOption.OPTION_HTML_EDITOR, useHTMLEditor);
    }

    /**
     * Set multiline display ability
     *
     * @param multiLine render property in multiple lines?
     * @return the property itself, useful for chained calls
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setMultiLine(boolean multiLine) throws FxInvalidParameterException {
        return setOption(FxStructureOption.OPTION_MULTILINE, multiLine);
    }

    /**
     * Set this property assignment as (temporary) disabled - it will not be initialized when creating new instances, etc.
     *
     * @param enabled enabled flag
     * @return this
     */
    public FxPropertyAssignmentEdit setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Set the multiplicity of this assignment
     *
     * @param multiplicity new multiplicity
     * @return this
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setMultiplicity(FxMultiplicity multiplicity) throws FxInvalidParameterException {
        if (!getProperty().mayOverrideBaseMultiplicity())
            throw new FxInvalidParameterException("MULTIPLICITY", "ex.structure.override.property.forbidden", "Multiplicity", getProperty().getName());
        this.multiplicity = multiplicity;
        return this;
    }

    /**
     * Set the default multiplicity (used i.e. in user interfaces editors and determines the amount of values that will
     * be initialized when creating an empty element).
     * <p/>
     * If the set value is &lt; min or &gt; max multiplicity of this assignment it will
     * be auto adjusted to the next valid value without throwing an exception
     *
     * @param defaultMultiplicity the default multiplicity
     * @return this
     */
    public FxPropertyAssignmentEdit setDefaultMultiplicity(int defaultMultiplicity) {
        if (this.getMultiplicity().isValid(defaultMultiplicity)) {
            this.defaultMultiplicity = defaultMultiplicity;
            return this;
        }
        if (defaultMultiplicity < this.getMultiplicity().getMin())
            this.defaultMultiplicity = this.getMultiplicity().getMin();
        if (defaultMultiplicity > this.getMultiplicity().getMax())
            this.defaultMultiplicity = this.getMultiplicity().getMax();
        return this;
    }

    /**
     * Set the position of this assignment (within the same parent group).
     * Changing an assignments position will be upate all affected other assignments within the same
     * group. Invalid values will be adjusted (to 0 or the max. possible position)
     *
     * @param position position within the parent group
     * @return this
     */
    public FxPropertyAssignmentEdit setPosition(int position) {
        this.position = position;
        return this;
    }

    /**
     * Set the alias of this property assignment.
     * Property assignments may define an alias to allow multiple use of the same property but
     * using a different name. The alias is the rightmost part of the XPath used to address an assignment.
     *
     * @param alias the alias of this assignment
     * @return this
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setAlias(String alias) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(alias))
            throw new FxInvalidParameterException("ALIAS", "ex.structure.assignment.noAlias");
        this.alias = alias;
        return this;
    }

    /**
     * Set the XPath of this assignment - this is used mainly internally and affects new assignments only
     *
     * @param XPath the XPath to set
     * @return this
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setXPath(String XPath) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(alias))
            throw new FxInvalidParameterException("XPATH", "ex.structure.assignment.noXPath");
        try {
            this.getAssignedType().getAssignment(XPath);
            throw new FxInvalidParameterException("XPATH", "ex.structure.assignment.exists", XPath, getAssignedType().getName());
        } catch (FxNotFoundException e) {
            //ok, it really is new
        }
        this.XPath = XPath;
        return this;
    }

    /**
     * Setter for the label
     *
     * @param label label to set
     * @return this
     */
    public FxPropertyAssignmentEdit setLabel(FxString label) {
        this.label = label;
        return this;
    }

    /**
     * Set the hint message
     *
     * @param hint hint message
     * @return the property itself, useful for chained calls
     */
    public FxPropertyAssignmentEdit setHint(FxString hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Create a new FxPropertyAssignmentEdit from an existing FxPropertyAssignment as a new one for a given type with a
     * new alias and a given parentXPath
     *
     * @param pa          original property assignment
     * @param type        type to assign it
     * @param alias       new alias
     * @param parentXPath parent XPath within the type to assign
     * @param parent      optional parent assignment if already known (prevents lookup of parentXPath if valid)
     * @return new FxPropertyAssignmentEdit
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    public static FxPropertyAssignmentEdit createNew(FxPropertyAssignment pa, FxType type, String alias, String parentXPath, FxAssignment parent) throws FxNotFoundException, FxInvalidParameterException {
        return new FxPropertyAssignmentEdit(pa, type, alias, parentXPath, parent);
    }

    /**
     * Create a new FxPropertyAssignmentEdit from an existing FxPropertyAssignment as a new one for a given type with a
     * new alias and a given parentXPath
     *
     * @param pa          original property assignment
     * @param type        type to assign it
     * @param alias       new alias
     * @param parentXPath parent XPath within the type to assign
     * @return new FxPropertyAssignmentEdit
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    public static FxPropertyAssignmentEdit createNew(FxPropertyAssignment pa, FxType type, String alias, String parentXPath) throws FxNotFoundException, FxInvalidParameterException {
        return new FxPropertyAssignmentEdit(pa, type, alias, parentXPath, null);
    }

    /**
     * Convenience method to create a new FxPropertyAssignmentEdit from an existing FxPropertyAssignment as a new one for a given type with a
     * new alias and a given parentXPath
     *
     * @param originalProperty the original properties XPath (like ROOT/CAPTION)
     * @param type             name of the type to assign it
     * @param parentXPath      parent XPath within the type to assign
     * @param alias            new alias
     * @return new FxPropertyAssignmentEdit
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    public static FxPropertyAssignmentEdit reuse(String originalProperty, String type, String parentXPath, String alias) throws FxNotFoundException, FxInvalidParameterException {
        return createNew(
                (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(originalProperty),
                CacheAdmin.getEnvironment().getType(type),
                alias,
                parentXPath).setEnabled(true);
    }

    /**
     * Convenience method to create a new FxPropertyAssignmentEdit from an existing FxPropertyAssignment as a new one for a given type with a
     * new alias and a given parentXPath
     *
     * @param originalProperty the original properties XPath (like ROOT/CAPTION)
     * @param type             name of the type to assign it
     * @param parentXPath      parent XPath within the type to assign
     * @return new FxPropertyAssignmentEdit
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    public static FxPropertyAssignmentEdit reuse(String originalProperty, String type, String parentXPath) throws FxNotFoundException, FxInvalidParameterException {
        FxPropertyAssignment propertyAssignment = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(originalProperty);
        return createNew(
                propertyAssignment,
                CacheAdmin.getEnvironment().getType(type),
                propertyAssignment.getAlias(),
                parentXPath).setEnabled(true);
    }

    /**
     * Get a (unmodifiable) list of all options set for this property assignment
     *
     * @return (unmodifiable) list of all options set for this property assignment
     */
    public List<FxStructureOption> getOptions() {
        return FxStructureOption.getUnmodifieableOptions(options);
    }

    /**
     * Sets the parent group assignment.
     *
     * @param parent the parent group assignment.
     */
    public void setParentGroupAssignment(FxGroupAssignment parent) {
        this.parentGroupAssignment = parent;
    }

    /**
     * Returns the property of this assignment as editable.
     *
     * @return the editable property object.
     */
    public FxPropertyEdit getPropertyEdit() {
        if (!(property instanceof FxPropertyEdit))
            throw new FxApplicationException("ex.structure.noEditableProperty").asRuntimeException();
        else return (FxPropertyEdit) property;
    }

    /**
     * Returns a new List of all available options.
     * The options include those of the class itsself and of its assigned FxProperty
     * To eliminate duplicate keys and determine the correct overriding oder,
     * the <code>FxPropertyAssignment.getOption(key)</code> method
     * is used. If an option is contained in the returned List then <code>hasOption(key)
     * ==true</code>, otherwise <code>false</code>.
     *
     * @return new List containing all availiable options.
     */
    protected List<FxStructureOption> getAllAvailableOptions() {
        List<FxStructureOption> allOptions = new ArrayList<FxStructureOption>(options);
        List<String> setOptions = new ArrayList<String>(2);
        List<FxStructureOption> result = new ArrayList<FxStructureOption>();

        allOptions.addAll(super.getProperty().options);
        allOptions.addAll(options);
        //iterate over all options and eliminate duplicate keys
        for (FxStructureOption o : allOptions) {
            if (hasOption(o.getKey())) {
                if (!setOptions.contains(o.getKey())) {
                    setOptions.add(o.getKey());
                    result.add(getOption(o.getKey()));
                }
            }
        }
        return result;
    }

}
