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
package com.flexive.faces.model;

import com.flexive.shared.*;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxSelectList;
import org.apache.commons.lang.StringUtils;

import javax.faces.model.SelectItem;
import java.io.Serializable;

/**
 * An extended SelectItem class that provides constructors for selectable [fleXive] items and
 * support for styles (currently color).
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see javax.faces.model.SelectItem
 */
public class FxJSFSelectItem extends SelectItem implements java.io.Serializable {
    private static final long serialVersionUID = 676788316415654999L;
    private String style = null;

    /**
     * Default empty element
     */
    private static class EmptySelectableObjectWithName extends AbstractSelectableObjectWithName implements Serializable {
        private static final long serialVersionUID = 7808775494956188839L;

        public EmptySelectableObjectWithName() {
            // nothing
        }

        public long getId() {
            return -1;
        }

        public String getName() {
            return "";
        }
    }

    /**
     * Empty constructor to create an empty element
     */
    public FxJSFSelectItem() {
        super(new EmptySelectableObjectWithName(), "");
        this.style = "";
    }

    /**
     * Default ctor for id and value as String
     *
     * @param id    id
     * @param value value
     */
    public FxJSFSelectItem(String id, String value) {
        super(id, value);
        this.style = "";
    }

    /**
     * Ctor for SelectableObjectWithName
     *
     * @param item SelectableObjectWithName
     */
    public FxJSFSelectItem(SelectableObjectWithName item) {
        super(item, item.getName());
        applyStyle(item);
    }

    /**
     * Ctor for SelectableObjectWithLabel, optionally as id instead of the item
     *
     * @param item SelectableObjectWithLabel
     * @param asId use the id of the item or the item itself?
     */
    public FxJSFSelectItem(SelectableObjectWithName item, boolean asId) {
        super(asId ? item.getId() : item, item.getName());
        applyStyle(item);
    }

    /**
     * Ctor for SelectableObjectWithLabel
     *
     * @param item SelectableObjectWithLabel
     */
    public FxJSFSelectItem(SelectableObjectWithLabel item) {
        super(item, item.getLabel().getBestTranslation());
        applyStyle(item);
    }

    /**
     * Ctor for SelectableObjectWithLabel, optionally as id instead of the item
     *
     * @param item SelectableObjectWithLabel
     * @param asId use the id of the item or the item itself?
     */
    public FxJSFSelectItem(SelectableObjectWithLabel item, boolean asId) {
        super(asId ? item.getId() : item, item.getLabel().getBestTranslation());
        applyStyle(item);
    }

    /**
     * Ctor for SelectableObjectWithLabel, translation if chosen based on the users preferred language
     *
     * @param item   SelectableObjectWithLabel
     * @param ticket current users ticket to choose the language
     */
    public FxJSFSelectItem(SelectableObjectWithLabel item, UserTicket ticket) {
        super(item, item.getLabel().getBestTranslation(ticket));
        applyStyle(item);
    }

    /**
     * Ctor for Enum
     *
     * @param item Enum
     */
    public FxJSFSelectItem(Enum item) {
        super(item, item instanceof ObjectWithLabel ? ((ObjectWithLabel) item).getLabel().getBestTranslation() : item.name());
        applyStyle(item);
    }


    /**
     * Ctor for user group with a special preformatted display name
     *
     * @param group       user group
     * @param displayName display name
     */
    public FxJSFSelectItem(UserGroup group, String displayName) {
        super(group, displayName);
        applyStyle(group);
    }

    /**
     * Ctor for FxSelectList, translation if chosen based on the users preferred language
     *
     * @param item   FxSelectList
     * @param ticket current users ticket to choose the language
     */
    public FxJSFSelectItem(FxSelectList item, UserTicket ticket) {
        super(item.getId(), item.getLabel().getBestTranslation(ticket), item.getDescription().getBestTranslation(ticket));
        this.style = "";
    }

    /**
     * Apply the style based on the items class
     *
     * @param item item
     */
    private void applyStyle(Object item) {
        if (item instanceof ObjectWithColor) {
            String color = ((ObjectWithColor) item).getColor();
            if (!StringUtils.isEmpty(color))
                this.style = "color:" + color;
            else
                this.style = "";
        } else
            this.style = "";
    }

    /**
     * Getter for the style
     *
     * @return style
     */
    public String getStyle() {
        return style;
    }
}
