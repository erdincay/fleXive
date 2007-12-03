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
package com.flexive.war.beans.admin.content;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxEnvironment;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Helper class for the ContentEditor.
 * <p/>
 * It provides lookups for a property assignment's display name.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
class CeAddElementOptions extends Hashtable<FxData, List<SelectItem>> {

    private ContentEditorBean parent;

    /**
     * Constructor.
     *
     * @param parent the associated content editor
     */
    protected CeAddElementOptions(ContentEditorBean parent) {
        super(0);
        this.parent = parent;
    }

    public List<SelectItem> get(Object element) {

        // Avoid null pointer
        if (element == null) {
            return new ArrayList<SelectItem>(0);
        }

        try {
            ArrayList<SelectItem> result = new ArrayList<SelectItem>(10);

            // Get the group data to work on
            FxGroupData gd;
            if (element instanceof FxPropertyData) {
                gd = ((FxPropertyData) element).getParent();
            } else {
                gd = (FxGroupData) element;
            }

            // Determine the available options
            if (gd != null) {
                List<String> createable = gd.getCreateableChildren(true);
                final FxEnvironment environment = CacheAdmin.getFilteredEnvironment();
                for (String createable_xpath : createable) {
                    String display = createable_xpath;
                    try {
                        FxAssignment ass = environment.getType(parent.getType()).getAssignment(createable_xpath);
                        display = ContentEditorBean.getDisplay(ass).getBestTranslation();
                    } catch (Throwable t) { /*ignore*/ }
                    result.add(new SelectItem(createable_xpath, display));
                }
                result.trimToSize();
            }

            return result;
        } catch (Throwable t) {
            System.err.println(t.getMessage());
            t.printStackTrace();
            return new ArrayList<SelectItem>(0);
        }

    }
}
