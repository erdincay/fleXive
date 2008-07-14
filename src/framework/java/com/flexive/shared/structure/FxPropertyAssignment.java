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

import com.flexive.shared.FxLanguage;
import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxCreateException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

/**
 * Assignment of a property to a type or group
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPropertyAssignment extends FxAssignment implements Serializable {
    private static final long serialVersionUID = -4825188658392104371L;

    /**
     * The property assigned
     */
    protected FxProperty property;

    /**
     * Overridden ACL (if the embedded property permits)
     */
    protected ACL ACL;

    protected FxValue defaultValue;

    protected long defaultLang;

    /**
     * Constructor
     *
     * @param assignmentId          internal id of this assignment
     * @param enabled               is this assignment enabled?
     * @param assignedType          the FxType this assignment belongs to
     * @param alias                 an optional alias, if <code>null</code> the original name will be used
     * @param xpath                 XPath relative to the assigned FxType
     * @param position              position within the same XPath hierarchy
     * @param multiplicity          multiplicity (will only be used if the embedded property allows overriding)
     * @param defaultMultiplicity   default multiplicity
     * @param parentGroupAssignment (optional) parent FxGroupAssignment this property assignment belongs to
     * @param baseAssignment        base assignment (if derived the parent, if not the root assignment, if its a root assignment FxAssignment.ROOT_BASE)
     * @param label                 (optional) label
     * @param hint                  (optional) hint
     * @param defaultValue          (optional) default value
     * @param property              the assigned property
     * @param ACL                   the embedded property's ACL (will only be used if the embedded property allows overriding)
     * @param defaultLang           default language if multilingual (if 0==SYSTEM then not set)
     * @param options               options
     */
    public FxPropertyAssignment(long assignmentId, boolean enabled, FxType assignedType, String alias, String xpath, int position,
                                FxMultiplicity multiplicity, int defaultMultiplicity, FxGroupAssignment parentGroupAssignment,
                                long baseAssignment, FxString label, FxString hint, FxValue defaultValue,
                                FxProperty property, ACL ACL, long defaultLang, List<FxStructureOption> options) {
        super(assignmentId, enabled, assignedType, alias, xpath, position, multiplicity, defaultMultiplicity, parentGroupAssignment,
                baseAssignment, label, hint, options);
        this.defaultValue = defaultValue;
        if (defaultValue == null)
            this.defaultValue = property.getDefaultValue().copy();
        this.property = property;
        if (alias == null || alias.trim().length() == 0)
            this.alias = property.getName();
        this.defaultLang = defaultLang;
        this.ACL = ACL;
    }


    /**
     * Get the property this assignment relates to
     *
     * @return property this assignment relates to
     */
    public FxProperty getProperty() {
        return property;
    }

    /**
     * Get the ACL of the embedded property. If the property does not allow overriding
     * ACL the original property ACL will be returned
     *
     * @return the ACL
     */
    public ACL getACL() {
        return (getProperty().mayOverrideACL() && this.ACL != null ? this.ACL : getProperty().getACL());
    }

    /**
     * Check if an ACL is defined for this assignment which overrides the properties ACL
     *
     * @return <code>true</code> if an ACL is defined for this assignment which overrides the properties ACL
     */
    public boolean isOverridingPropertyACL() {
        return this.ACL != null;
    }

    /**
     * Get the multiplicity of this assignment.
     * Depending on if the assigned element allows overriding of its base multiplicity the base
     * elements multiplicity is returned or the multiplicity of the assignment
     *
     * @return multiplicity of this assignment
     */
    @Override
    public FxMultiplicity getMultiplicity() {
        return (getProperty().mayOverrideBaseMultiplicity() && this.multiplicity != null ? this.multiplicity : getProperty().getMultiplicity());
    }

    /**
     * Check if a multiplicity is defined for this assignment which overrides the properties multiplicity
     *
     * @return <code>true</code> if a multiplicity is defined for this assignment which overrides the properties multiplicity
     */
    public boolean isOverridingPropertyMultiplicity() {
        return this.multiplicity != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxStructureOption getOption(String key) {
        FxStructureOption pOpt = property.getOption(key);
        if (!pOpt.isSet())
            return super.getOption(key);
        if (!pOpt.isOverrideable())
            return pOpt;
        if (super.hasOption(key))
            return super.getOption(key);
        return pOpt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOption(String key) {
        return super.hasOption(key) || property.hasOption(key);
    }

    public boolean isMultiLang() {
        return getOption(FxStructureOption.OPTION_MULTILANG).isValueTrue();
    }

    public boolean isSearchable() {
        return getOption(FxStructureOption.OPTION_SEARCHABLE).isValueTrue();
    }

    public boolean isInOverview() {
        return getOption(FxStructureOption.OPTION_SHOW_OVERVIEW).isValueTrue();
    }

    public boolean isUseHTMLEditor() {
        return getOption(FxStructureOption.OPTION_HTML_EDITOR).isValueTrue();
    }

    /**
     * Shortcut to determine if a max. input length has been set
     *
     * @return has a max. input length been set?
     */
    public boolean hasMaxLength() {
        return hasOption(FxStructureOption.OPTION_MAXLENGTH);
    }

    /**
     * Shortcut to get the maximum input length
     *
     * @return maximum input length
     */
    public int getMaxLength() {
        return getOption(FxStructureOption.OPTION_MAXLENGTH).getIntValue();
    }

    /**
     * Show as multiple lines in editors?
     *
     * @return if this property appears in multiple lines
     */
    public boolean isMultiLine() {
        FxStructureOption opt = getOption(FxStructureOption.OPTION_MULTILINE);
        if (opt.isSet()) {
            try {
                return opt.getIntValue() > 0;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Get the number of multilines to display or 0 if multiline is not set
     *
     * @return number of multilines to display or 0 if multiline is not set
     */
    public int getMultiLines() {
        FxStructureOption opt = getOption(FxStructureOption.OPTION_MULTILINE);
        if (opt.isSet()) {
            try {
                return opt.getIntValue();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Get the default value for this assignment
     *
     * @return FxValue
     */
    public FxValue getDefaultValue() {
        defaultValue.setXPath(this.getXPath());
        return defaultValue;
    }

    /**
     * Get an empty FxValue object for this assignment
     *
     * @return empty FxValue object
     */
    public FxValue getEmptyValue() {
        FxValue value;
        if (hasDefaultLanguage())
            value = this.getProperty().getEmptyValue(this.isMultiLang(), this.getDefaultLanguage()).setXPath(getXPath());
        else
            value = this.getProperty().getEmptyValue(this.isMultiLang()).setXPath(getXPath());
        if (value.isMultiLanguage() == this.getDefaultValue().isMultiLanguage() && !this.getDefaultValue().isEmpty())
            value = this.getDefaultValue().copy();
        if (this.hasMaxLength()) {
            value.setMaxInputLength(this.getMaxLength());
            if (this.getProperty().getDataType() == FxDataType.String1024 && value.getMaxInputLength() > 1024)
                value.setMaxInputLength(1024);
        } else if (this.getProperty().getDataType() == FxDataType.String1024)
            value.setMaxInputLength(1024);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxData createEmptyData(FxGroupData parent, int index) throws FxCreateException {
        String XPathFull = (this.hasParentGroupAssignment() && parent != null ? parent.getXPathFull() : "") + "/" + this.getAlias();
        String XPath = (this.hasParentGroupAssignment() && parent != null ? parent.getXPath() : "") + "/" + this.getAlias();
        try {
            if (!this.getMultiplicity().isValid(index))
                throw new FxCreateException("ex.content.xpath.index.invalid", index, this.getMultiplicity(), this.getXPath()).
                        setAffectedXPath(parent != null ? parent.getXPathFull() : this.getXPath());
            final FxPropertyData data = new FxPropertyData(parent == null ? "" : parent.getXPathPrefix(), this.getAlias(), index, XPath, XPathElement.toXPathMult(XPathFull),
                    XPathElement.getIndices(XPathFull), this.getId(), this.getProperty().getId(), this.getMultiplicity(),
                    this.getPosition(), parent, this.getEmptyValue(), this.isSystemInternal());
            //Flag if the value is set from the assignments default value
            data.setContainsDefaultValue(!data.getValue().isEmpty());
            return data;
        } catch (FxInvalidParameterException e) {
            throw new FxCreateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxData createRandomData(Random rnd, FxEnvironment env, FxGroupData parent, int index, int maxMultiplicity) throws FxCreateException {
        String XPathFull = (this.hasParentGroupAssignment() && parent != null ? parent.getXPathFull() : "") + "/" + this.getAlias();
        String XPath = (this.hasParentGroupAssignment() && parent != null ? parent.getXPath() : "") + "/" + this.getAlias();
        try {
            if (!this.getMultiplicity().isValid(index))
                throw new FxCreateException("ex.content.xpath.index.invalid", index, this.getMultiplicity(), this.getXPath()).
                        setAffectedXPath(parent != null ? parent.getXPathFull() : this.getXPath());
            return new FxPropertyData(parent == null ? "" : parent.getXPathPrefix(), this.getAlias(), index, XPath, XPathElement.toXPathMult(XPathFull),
                    XPathElement.getIndices(XPathFull), this.getId(), this.getProperty().getId(), this.getMultiplicity(),
                    this.getPosition(), parent, this.getProperty().getDataType().getRandomValue(rnd, this), this.isSystemInternal());
        } catch (FxInvalidParameterException e) {
            throw new FxCreateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(Object value) {
        // TODO: add assignment-based validation here
        return property.getDataType().isValid(value);
    }


    /**
     * Is a default language defined for this property assignment?
     *
     * @return if a default language is defined for this property assignment
     */
    public boolean hasDefaultLanguage() {
        return defaultLang != FxLanguage.SYSTEM_ID && isMultiLang();
    }

    /**
     * Get the default language for this property assignment (if set)
     *
     * @return default language for this property assignment (if set)
     */
    public long getDefaultLanguage() {
        return defaultLang;
    }

    /**
     * Get this FxPropertyAssignment as editable
     *
     * @return FxPropertyAssignmentEdit
     */
    public FxPropertyAssignmentEdit asEditable() {
        return new FxPropertyAssignmentEdit(this);
    }

}
