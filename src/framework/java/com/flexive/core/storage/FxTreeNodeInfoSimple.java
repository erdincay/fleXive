/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.core.storage;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.security.PermissionSet;

import java.io.Serializable;

/**
 * Information about a tree node that implementation specific and provide information about parameters
 * relevant to the nested set model <b>without</b> spreading.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTreeNodeInfoSimple extends FxTreeNodeInfo implements Serializable {

    private static final long serialVersionUID = 382572487617746922L;
    private long left;
    private long right;
    private long parentLeft;
    private long parentRight;

    /**
     * Ctor
     *
     * @param left             left position
     * @param right            right position
     * @param parentLeft       parent left
     * @param parentRight      parent right
     * @param totalChildCount  total number of children
     * @param directChildCount number of direct children
     * @param depth            depth of this node
     * @param parentId         parent id
     * @param id               node id
     * @param name             name
     * @param reference        referenced content
     * @param ACLId            ACL of the referenced content
     * @param mode             tree mode
     * @param position         position
     * @param template         template
     * @param modifiedAt       last modified at
     * @param permissions      the node permissions of the calling user
     */
    public FxTreeNodeInfoSimple(long left, long right, long parentLeft, long parentRight,
                                int totalChildCount, int directChildCount, int depth, long parentId, long id, String name,
                                FxPK reference, long ACLId, FxTreeMode mode, int position, String template, long modifiedAt,
                                PermissionSet permissions) {
        super(totalChildCount, directChildCount, depth, parentId, id, name, reference, ACLId,
                mode, position, template, modifiedAt, permissions);
        this.left = left;
        this.right = right;
        this.parentLeft = parentLeft;
        this.parentRight = parentRight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getLeft() {
        return left;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getRight() {
        return right;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getParentLeft() {
        return parentLeft;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getParentRight() {
        return parentRight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParentOf(FxTreeNodeInfo node) {
        return node.getLeft().longValue() < getLeft().longValue() &&
                node.getRight().longValue() > getRight().longValue();
    }
}
