/**
 * This file is part of the [fleXive](R) framework.
 *
 * Copyright (c) 1999-2013
 * UCS - unique computing solutions gmbh (http://www.ucs.at)
 * All rights reserved
 *
 * The [fleXive](R) project is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public
 * License version 2.1 or higher as published by the Free Software Foundation.
 *
 * The GNU Lesser General Public License can be found at
 * http://www.gnu.org/licenses/lgpl.html.
 * A copy is found in the textfile LGPL.txt and important notices to the
 * license from the author are found in LICENSE.txt distributed with
 * these libraries.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For further information about UCS - unique computing solutions gmbh,
 * please see the company website: http://www.ucs.at
 *
 * For further information about [fleXive](R), please see the
 * project website: http://www.flexive.org
 *
 *
 * This copyright notice MUST APPEAR in all copies of the file!
 */
package com.flexive.rest;

import com.flexive.rest.shared.FxRestApiResponse;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

/**
 * Base class for the tree endpoints (for both edit and live trees).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
abstract class TreeService implements FxRestApiService {
    private final FxTreeMode mode;

    protected TreeService() {
        // make CDI happy and provide a no-args constructor
        this.mode = FxTreeMode.Edit;
    }

    protected TreeService(FxTreeMode mode) {
        this.mode = mode;
    }

    protected Object getTree(long id) throws FxApplicationException {
        return doGetTree(id);
    }

    protected Object getTree(String idOrPath) throws FxApplicationException {
        if (StringUtils.isNumeric(idOrPath)) {
            return doGetTree(Long.parseLong(idOrPath));
        } else {
            final long nodeId = EJBLookup.getTreeEngine().getIdByPath(mode, idOrPath);
            return doGetTree(nodeId);
        }
    }

    private Object doGetTree(long id) throws FxApplicationException {
        final FxTreeNode tree = EJBLookup.getTreeEngine().getTree(mode, id, Integer.MAX_VALUE);

        return FxRestApiResponse.ok(
                ImmutableMap.of("node", serializeTreeNode(tree))
        );
    }

    private Object serializeTreeNode(FxTreeNode node) {
        return FxRestApiUtils.responseMapBuilder()
                .put("id", node.getId())
                .put("name", node.getName())
                .put("label", node.getLabel().getBestTranslation())
                .put("reference", ImmutableMap.of(
                        "id", node.getReference().getId(),
                        "version", node.getReference().getVersion()
                ))
                .put("path", node.getPath())
                .put("parentId", node.getParentNodeId())
                .put("children", Lists.transform(node.getChildren(), FN_SERIALIZE), "node")
                .build();
    }

    private final Function<FxTreeNode, Object> FN_SERIALIZE = new Function<FxTreeNode, Object>() {
        public Object apply(@Nullable FxTreeNode fxTreeNode) {
            return serializeTreeNode(fxTreeNode);
        }
    };
}
