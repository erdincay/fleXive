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
package com.flexive.shared.security;

import com.flexive.shared.*;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * Data class for the access control lists
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ACL extends AbstractSelectableObjectWithName implements Serializable, SelectableObjectWithLabel, ObjectWithColor {
    private static final long serialVersionUID = -8177665165523382984L;

    /**
     * ID for an empty placeholder ACL.
     */
    public static final long NULL_ACL_ID = 0;

    /**
     * Max. id for internal ACL's (needed for internal checks)
     */
    public final static long MAX_INTERNAL_ID = 8;

    /**
     * Contact Data ACL
     */
    public final static long ACL_CONTACTDATA = 8;

    private String color;
    private long id;
    private long mandatorId;
    private ACLCategory category;
    private String description;
    private String name;
    private FxString label;
    private String mandator;
    private LifeCycleInfo lifeCycleInfo = null;

    /**
     * Constructor
     */
    public ACL() {
        this.label = new FxString("");
    }

    /**
     * Copy Constructor
     *
     * @param acl ACL to copy
     */
    public ACL(ACL acl) {
        this.color = acl.color;
        this.id = acl.id;
        this.mandatorId = acl.mandatorId;
        this.category = acl.category;
        this.description = acl.description;
        this.name = acl.name;
        this.label = acl.label.copy();
        this.mandator = acl.mandator;
        this.lifeCycleInfo = acl.lifeCycleInfo;
    }

    /**
     * Constructor.
     *
     * @param id            the unique id
     * @param name          the name
     * @param label         display label
     * @param mandatorId    the id of the mandator the acl belongs to
     * @param mandator      the name of the mandator
     * @param description   the description
     * @param color         the color (RGB code or style class)
     * @param category      the category
     * @param lifeCycleInfo lifecycle information
     */
    public ACL(long id, String name, FxString label, long mandatorId, String mandator, String description,
               String color, ACLCategory category, LifeCycleInfo lifeCycleInfo) {
        this.color = color;
        this.id = id;
        this.mandatorId = mandatorId;
        this.category = category;
        this.description = description;
        this.name = name;
        this.label = label;
        this.mandator = mandator;
        this.lifeCycleInfo = lifeCycleInfo;
    }

    /**
     * Returns the unique id of the ACL.
     *
     * @return the unique id of the ACL
     */
    public long getId() {
        return this.id;
    }

    /**
     * Returns the unique name of the ACL.
     *
     * @return the unique name of the ACL.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the description of the ACL.
     * The desciption is never null, but may be a empty String.
     *
     * @return the description of the ACL.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the color of the ACL.
     * A empty String may be returned if the default color should be used.
     *
     * @return the color of the ACL
     */
    public String getColor() {
        return this.color;
    }

    /**
     * Returns the category of the ACL.
     *
     * @return the category of the ACL
     */
    public ACLCategory getCategory() {
        return this.category;
    }

    /**
     * Get lifecycle information
     *
     * @return lifecycle information
     */
    public LifeCycleInfo getLifeCycleInfo() {
        return lifeCycleInfo;
    }

    /**
     * Returns the mandator the ACL belongs to.
     *
     * @return the mandator the ACL belongs to
     */
    public long getMandatorId() {
        return this.mandatorId;
    }

    /**
     * Sets the unique name of the ACL.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the ACL name as it should be displayed to the user.
     *
     * @return  the ACL name as it should be displayed to the user.
     */
    public String getDisplayName() {
        return label != null && !label.isEmpty() ? label.getBestTranslation() : name;
    }

    /**
     * Get the display label of this ACL
     *
     * @return display label
     */
    public FxString getLabel() {
        return label;
    }

    /**
     * Set the display label of this ACL
     *
     * @param label display label
     */
    public void setLabel(FxString label) {
        this.label = label;
    }

    /**
     * Sets the  the description of the ACL.
     *
     * @param desc the new description
     */
    public void setDescription(String desc) {
        this.description = desc;
    }

    /**
     * Sets the color of the ACL.
     *
     * @param color the color of the ACL
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Returns the category of the ACL.
     *
     * @param cat the category of the ACL
     */
    public void setCategory(ACLCategory cat) {
        this.category = cat;
    }

    /**
     * Returns the name of the mandator.
     *
     * @return the name of the mandator
     */
    public String getMandatorName() {
        return mandator;
    }

    @Override
    public String toString() {
        return "ACL[id=" + id + ";name=" + name + ";mandator=" + mandator + ";label=" + label + ";category=" + category.name() + "]";
    }


}
