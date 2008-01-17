/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.shared.security;

import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxNotFoundException;
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
     * Max. id for internal ACL's (needed for internal checks)
     */
    public final static long MAX_INTERNAL_ID = 7;

    private String color;
    private long id;
    private long mandatorId;
    private Category category;
    private String description;
    private String name;
    private FxString label;
    private String mandator;
    private LifeCycleInfo lifeCycleInfo = null;

    /**
     * ACL categories and their defaults
     */
    public enum Category implements ObjectWithLabel {
        INSTANCE(1, 2),
        STRUCTURE(2, 7),
        WORKFLOW(3, 3),
        BRIEFCASE(4, 4),
        SELECTLIST(5, 5),
        SELECTLISTITEM(6, 6);

        private int id;
        private long defaultId;

        Category(int id, long defaultId) {
            this.id = id;
            this.defaultId = defaultId;
        }

        /**
         * Getter for the internal id
         *
         * @return internal id
         */
        public int getId() {
            return id;
        }

        /**
         * Get the Id of the default ACL for given category
         *
         * @return Id of the default ACL for given category
         */
        public long getDefaultId() {
            return defaultId;
        }

        /**
         * Get a TypeMode by its id
         *
         * @param id the id
         * @return TypeMode the type
         */
        public static Category getById(int id) {
            for (Category cat : Category.values())
                if (cat.id == id)
                    return cat;
            throw new FxNotFoundException("ex.acl.category.notFound.id", id).asRuntimeException();
        }

        /**
         * {@inheritDoc}
         */
        public FxString getLabel() {
            return FxSharedUtils.getEnumLabel(this);
        }
    }

    /**
     * ACL permissions
     */
    public enum Permission implements ObjectWithLabel {
        CREATE, READ, EDIT, DELETE, RELATE, EXPORT,
        NOT_CREATE, NOT_READ, NOT_EDIT, NOT_DELETE, NOT_RELATE, NOT_EXPORT;

        /**
         * Check if <code>check</code> is contained in perms
         *
         * @param check Perm to check
         * @param perms array of Perm's
         * @return if <code>check</code> is contained in perms
         */
        public static boolean contains(Permission check, Permission... perms) {
            for (Permission perm : perms)
                if (perm == check)
                    return true;
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public FxString getLabel() {
            return FxSharedUtils.getEnumLabel(this);
        }
    }

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
               String color, Category category, LifeCycleInfo lifeCycleInfo) {
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
    public Category getCategory() {
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
    public void setCategory(Category cat) {
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
