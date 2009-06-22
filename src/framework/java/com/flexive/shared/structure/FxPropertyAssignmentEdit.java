/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
                pa.getHint() == null ? null : pa.getHint().copy(), pa.hasAssignmentDefaultValue() ? pa.getDefaultValue().copy() : null,
                pa.getProperty().asEditable(), new ACL(pa.getACL()), pa.getDefaultLanguage(),
                FxStructureOption.cloneOptions(pa.options), pa.getFlatstoreMapping());
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
                pa.getHint().copy(), pa.hasAssignmentDefaultValue() ? pa.getDefaultValue().copy() : null,
                pa.getProperty().asEditable(), new ACL(pa.getACL()), pa.getDefaultLanguage(),
                FxStructureOption.cloneOptions(pa.options), pa.getFlatstoreMapping());
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

    /**
     * Create a new FxPropertyAssignmentEdit from an existing property for a given type with a
     * new alias and a given parentXPath
     *
     * @param property    the property name
     * @param type        type to assign it
     * @param alias       new alias
     * @param parentXPath parent XPath within the type to assign
     */
    public FxPropertyAssignmentEdit(FxProperty property, FxType type, String alias, String parentXPath) {
        super(-1, true, type, alias, XPathElement.buildXPath(false, parentXPath, alias), 0, property.getMultiplicity(), 1,
                null, FxAssignment.NO_BASE, property.getLabel(), property.getHint(), null, property, property.getACL(),
                property.getLabel().getDefaultLanguage(), FxStructureOption.cloneOptions(property.options), null);
        isNew = true;
    }

    public boolean isNew() {
        return isNew;
    }

    /**
     * Clear the default value
     */
    public void clearDefaultValue() {
        this.defaultValue = null;
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
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public FxPropertyAssignmentEdit setDefaultValue(FxValue defaultValue) {
        if (defaultValue != null && defaultValue.isMultiLanguage() != this.isMultiLang()) {
            if (defaultValue.isMultiLanguage())
                throw new FxInvalidParameterException("value", "ex.content.value.invalid.multilanguage.prop.single", getXPath()).asRuntimeException();
            else
                throw new FxInvalidParameterException("value", "ex.content.value.invalid.multilanguage.prop.multi", getXPath()).asRuntimeException();
        }
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
            throw new FxInvalidParameterException(key, "ex.structure.override.property.forbidden", key, getProperty().getName());

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
            throw new FxInvalidParameterException(key, "ex.structure.override.property.forbidden", key, getProperty().getName());

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
     * Assign options
     *
     * @param options options to assign
     */
    public void setOptions(List<FxStructureOption> options) {
        this.options = options;
    }

    /**
     * Should this assignment support multilingual data?
     *
     * @param multiLang multi lingual data supported?
     * @return this
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setMultiLang(boolean multiLang) throws FxInvalidParameterException {
        if (!getProperty().mayOverrideMultiLang())
            throw new FxInvalidParameterException("MULTILANG", "ex.structure.override.property.forbidden", "MULTILANG", getProperty().getName());

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
        if (!getProperty().mayOverrideSearchable())
            throw new FxInvalidParameterException("SEARCHABLE", "ex.structure.override.property.forbidden", "SEARCHABLE", getProperty().getName());
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
        if (!getProperty().mayOverrideInOverview())
            throw new FxInvalidParameterException("INOVERVIEW", "ex.structure.override.property.forbidden", "INOVERVIEW", getProperty().getName());
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
        if (!getProperty().mayOverrideUseHTMLEditor())
            throw new FxInvalidParameterException("USEHTMLEDITOR", "ex.structure.override.property.forbidden", "USEHTMLEDITOR", getProperty().getName());
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
        if (!getProperty().mayOverrideMultiLine())
            throw new FxInvalidParameterException("MULTILINE", "ex.structure.override.property.forbidden", "MULTILINE", getProperty().getName());
        return setOption(FxStructureOption.OPTION_MULTILINE, multiLine);
    }

    /**
     * Shortcut to set the maximum input length (if applicable to the component)
     *
     * @param maxLength desired maximum input length
     * @return the property itself, useful for chained calls
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setMaxLength(int maxLength) throws FxInvalidParameterException {
        if (!getProperty().mayOverrideMaxLength())
            throw new FxInvalidParameterException("MAXLENGTH", "ex.structure.override.property.forbidden", "MAXLENGTH", getProperty().getName());
        return setOption(FxStructureOption.OPTION_MAXLENGTH, String.valueOf(maxLength));
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
     *
     * @param defaultMultiplicity the default multiplicity
     * @return this
     * @throws com.flexive.shared.exceptions.FxInvalidParameterException
     *          if the defaultMultiplicity is not within the range of min and max
     */
    public FxPropertyAssignmentEdit setDefaultMultiplicity(int defaultMultiplicity) throws FxInvalidParameterException {
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
     * Will affect the XPath as well.
     *
     * @param alias the alias of this assignment
     * @return this
     * @throws FxInvalidParameterException on errors
     */
    public FxPropertyAssignmentEdit setAlias(String alias) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(alias))
            throw new FxInvalidParameterException("ALIAS", "ex.structure.assignment.noAlias");
        //only react to alias changes
        if (!this.alias.trim().toUpperCase().equals(alias.trim().toUpperCase())) {
            this.alias = alias.trim().toUpperCase();
            List<XPathElement> xpe = XPathElement.split(this.XPath);
            xpe.set(xpe.size() - 1, new XPathElement(this.alias, 1, true));
            this.XPath = XPathElement.toXPathNoMult(xpe);
        }
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
        if (this.getAssignedType().isXPathValid(XPath, true))
            throw new FxInvalidParameterException("XPATH", "ex.structure.assignment.exists", XPath, getAssignedType().getName());
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
     * Create a new FxPropertyAssignmentEdit from an existing property for a given type with a
     * new alias and a given parentXPath
     *
     * @param property    the property name
     * @param type        type to assign it
     * @param alias       new alias
     * @param parentXPath parent XPath within the type to assign
     * @return new FxPropertyAssignmentEdit
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    public static FxPropertyAssignmentEdit createNew(String property, FxType type, String alias, String parentXPath) throws FxNotFoundException, FxInvalidParameterException {
        return new FxPropertyAssignmentEdit(CacheAdmin.getEnvironment().getProperty(property), type, alias, parentXPath);
    }

    /**
     * Convenience method to create a new FxPropertyAssignmentEdit from an existing FxPropertyAssignment as a new one for a given type with a
     * new alias and a given parentXPath
     *
     * @param originalAssignment the original assignments XPath (like ROOT/CAPTION)
     * @param type               name of the type to assign it
     * @param parentXPath        parent XPath within the type to assign
     * @param alias              new alias
     * @return new FxPropertyAssignmentEdit
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    public static FxPropertyAssignmentEdit reuse(String originalAssignment, String type, String parentXPath, String alias) throws FxNotFoundException, FxInvalidParameterException {
        return createNew(
                (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(originalAssignment),
                CacheAdmin.getEnvironment().getType(type),
                alias,
                parentXPath).setEnabled(true);
    }

    /**
     * Convenience method to create a new FxPropertyAssignmentEdit from an existing FxPropertyAssignment as a new one for a given type with a
     * new alias and a given parentXPath
     *
     * @param originalAssignment the original assignments XPath (like ROOT/CAPTION)
     * @param type               name of the type to assign it
     * @param parentXPath        parent XPath within the type to assign
     * @return new FxPropertyAssignmentEdit
     * @throws FxNotFoundException         if parentXPath is invalid
     * @throws FxInvalidParameterException if parentXPath is invalid
     */
    public static FxPropertyAssignmentEdit reuse(String originalAssignment, String type, String parentXPath) throws FxNotFoundException, FxInvalidParameterException {
        FxPropertyAssignment propertyAssignment = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(originalAssignment);
        return createNew(
                propertyAssignment,
                CacheAdmin.getEnvironment().getType(type),
                propertyAssignment.getAlias(),
                parentXPath).setEnabled(true);
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
            //noinspection ThrowableInstanceNeverThrown
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
