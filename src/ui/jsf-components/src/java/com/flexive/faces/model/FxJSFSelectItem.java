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
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;
import org.apache.commons.lang.StringUtils;

import javax.faces.model.SelectItem;

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
    private boolean fxSelectListItem = false;
    private boolean forceDisplay = false;

    /**
     * Empty constructor to create an empty element
     */
    public FxJSFSelectItem() {
        super(-1L, "");
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
        super(item.getId(), item.getName());
        applyStyle(item);
    }

    /**
     * Ctor for SelectableObjectWithLabel
     *
     * @param item SelectableObjectWithLabel
     */
    public FxJSFSelectItem(SelectableObjectWithLabel item) {
        super(item.getId(), item.getLabel().getBestTranslation());
        if (item instanceof FxSelectListItem)
            fxSelectListItem = true;
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
     * Ctor for SelectableObjectWithName with a special preformatted display name
     *
     * @param item        SelectableObjectWithName
     * @param displayName display name
     */
    public FxJSFSelectItem(SelectableObjectWithName item, String displayName) {
        super(item.getId(), displayName);
        applyStyle(item);
    }

    /**
     * Ctor for FxSelectList, translation is chosen based on the users preferred language
     *
     * @param list FxSelectList
     */
    public FxJSFSelectItem(FxSelectList list) {
        super(list.getId(), list.getLabel().getBestTranslation(), list.getDescription().getBestTranslation());
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
            if (!StringUtils.isEmpty(color)) {
                this.style = "color:" + color;
                //check if no contrast to background
                if (FxFormatUtils.lackOfContrast(color)) {
                    this.style += ";background-color:" + FxFormatUtils.CONTRAST_BACKGROUND_COLOR;
                }
            } else
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

    /**
     * Is this item a FxSelectListItem?
     *
     * @return item is a FxSelectListItem
     */
    public boolean isFxSelectListItem() {
        return fxSelectListItem;
    }

    /**
     * Force display of this item (with the label provided)?
     * Items can be suppressed in multiselect mode if they have children. Setting this flag to <code>true</code> will
     * force them to be displayed anyways as separate entries.
     *
     * @param forceDisplay force display of this item
     * @return this
     */
    public FxJSFSelectItem forceDisplay(boolean forceDisplay) {
        this.forceDisplay = forceDisplay;
        return this;
    }

    /**
     * Force display of this item (as is)?
     * <p/>
     * If forced this item will be displayed regardless of any children that might be attached to it (which would
     * normally cause this item to be rendered with breadcrumbs) and using the label provided
     * (no lookups for selectlist item labels will be made).
     *
     * @return force display
     */
    public boolean isForceDisplay() {
        return forceDisplay;
    }
}
