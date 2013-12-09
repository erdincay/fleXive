/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.shared.scripting.groovy;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.FxSelectListEdit;
import com.flexive.shared.structure.FxSelectListItemEdit;
import com.flexive.shared.value.FxString;
import groovy.util.BuilderSupport;

import java.io.Serializable;
import java.util.Map;

/**
 * SelectListBuilder
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GroovySelectListBuilder extends BuilderSupport implements Serializable {
    private static final long serialVersionUID = -7856524643709225006L;

    private FxSelectListEdit list = null;

    /**
     * {@inheritDoc}
     */
    protected void setParent(Object parent, Object child) {
        if (parent instanceof FxSelectListItemEdit && child instanceof FxSelectListItemEdit) {
            try {
                ((FxSelectListItemEdit) child).setParentItem((FxSelectListItemEdit) parent);
            } catch (FxInvalidParameterException e) {
                throw e.asRuntimeException();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Object createNode(Object node) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object node, Object value) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object node, Map map) {
        if (list == null) {
            list = new FxSelectListEdit(null,
                    String.valueOf(getMapValue(map, "name", false, String.valueOf(node))),
                    (FxString) getMapValue(map, "label", true, null),
                    (FxString) getMapValue(map, "description", false, new FxString("")),
                    Boolean.valueOf(String.valueOf(getMapValue(map, "allowDynamicItemCreation", false, false))),
                    (ACL) getMapValue(map, "createItemACL", false, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.SELECTLISTITEM)),
                    (ACL) getMapValue(map, "newItemACL", false, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.SELECTLISTITEM)),
                    null);
            list.setOnlySameLevelSelect(Boolean.valueOf(String.valueOf(getMapValue(map, "selectOnlySameLevel", false, false))));
            list.setSortEntries(Boolean.valueOf(String.valueOf(getMapValue(map, "sortEntries", false, false))));
            list.setBreadcrumbSeparator((String) getMapValue(map, "breadcrumbSeparator", false, " > "));
            return list;
        } else {
            return FxSelectListItemEdit.createNew(
                    (String) getMapValue(map, "name", false, String.valueOf(node)),
                    (ACL) getMapValue(map, "acl", false, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.SELECTLISTITEM)),
                    list,
                    (FxString) getMapValue(map, "label", true, null),
                    (String) getMapValue(map, "data", false, ""),
                    (String) getMapValue(map, "color", false, "#000000")
            );
        }
    }

    /**
     * Get an item from the parameter map
     *
     * @param map          the map
     * @param key          key of the item
     * @param required     is the item required?
     * @param defaultValue default value if not set
     * @return item
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Object getMapValue(Map map, String key, boolean required, Object defaultValue) {
        if (!map.containsKey(key)) {
            if (!required)
                return defaultValue;
            throw new FxInvalidParameterException(key, "ex.selectlist.builder.missingParameter", key).asRuntimeException();
        }
        Object value = map.get(key);
        if (defaultValue != null && !value.getClass().isInstance(defaultValue)) {
            throw new FxInvalidParameterException(key, "ex.selectlist.builder.invalidParameterClass", key,
                    defaultValue.getClass().getCanonicalName(),
                    value.getClass().getCanonicalName()).asRuntimeException();
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object node, Map map, Object value) {
        return null;
    }
}
