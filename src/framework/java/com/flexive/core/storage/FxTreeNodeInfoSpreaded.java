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
package com.flexive.core.storage;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.tree.FxTreeMode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Information about a tree node that implementation specific and provide information about parameters
 * relevant to the nested set model with spreading.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTreeNodeInfoSpreaded extends FxTreeNodeInfo implements Serializable {

    private static final long serialVersionUID = -2090532401288230681L;

    private final static BigDecimal THREE = new BigDecimal(3);
    private BigDecimal innerSpace;
    private BigDecimal defaultSpacing;
    private BigDecimal left;
    private BigDecimal right;
    private BigDecimal parentLeft;
    private BigDecimal parentRight;
    private BigDecimal maxChildRight;

    /**
     * Ctor
     *
     * @param left             left position
     * @param right            right position
     * @param parentLeft       parent left
     * @param parentRight      parent right
     * @param maxChildRight    max. number of children right
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
     * @param mayEdit          edit permission of the referenced ACL
     * @param mayCreate        create permission of the referenced ACL
     * @param mayDelete        delete permission of the referenced ACL
     * @param mayRelate        relate permission of the referenced ACL
     * @param mayExport        export permission of the referenced ACL
     */
    public FxTreeNodeInfoSpreaded(BigDecimal left, BigDecimal right,
                                  BigDecimal parentLeft, BigDecimal parentRight, BigDecimal maxChildRight,
                                  int totalChildCount, int directChildCount, int depth, long parentId, long id, String name,
                                  FxPK reference, long ACLId, FxTreeMode mode, int position, String template, long modifiedAt,
                                  boolean mayEdit, boolean mayCreate, boolean mayDelete, boolean mayRelate, boolean mayExport) {
        super(totalChildCount, directChildCount, depth, parentId, id, name, reference, ACLId,
                mode, position, template, modifiedAt, mayEdit, mayDelete, mayRelate, mayExport, mayCreate);
        this.left = left;
        this.right = right;
        this.parentLeft = parentLeft;
        this.parentRight = parentRight;
        this.maxChildRight = maxChildRight;
        // Compute the space between the node left and right border
        innerSpace = (right.subtract(left)).subtract(BigDecimal.ONE);
        // Compute the default spacing between borders inside this node
        defaultSpacing = getSpacing(totalChildCount);
    }

    /**
     * Get the inner space (space between left and right border)
     *
     * @return inner space
     */
    public BigDecimal getInnerSpace() {
        return innerSpace;
    }

    /**
     * Get the default spacing.
     *
     * @return default spacing
     */
    public BigDecimal getDefaultSpacing() {
        return defaultSpacing;
    }

    /**
     * Get the left position
     *
     * @return left position
     */
    @Override
    public BigDecimal getLeft() {
        return left;
    }

    /**
     * Get the right position
     *
     * @return right position
     */
    @Override
    public BigDecimal getRight() {
        return right;
    }

    /**
     * Get the parent left position
     *
     * @return parent left position
     */
    @Override
    public BigDecimal getParentLeft() {
        return parentLeft;
    }

    /**
     * Get the parent right position
     *
     * @return parent right position
     */
    @Override
    public BigDecimal getParentRight() {
        return parentRight;
    }

    /**
     * The maximum rgt value of all childs of the node, or if no child exists the rgt of
     * the node itself.
     * <p/>
     * Null if no childs are present.
     *
     * @return The maximum rgt value of all childs of the node
     */
    public BigDecimal getMaxChildRight() {
        if (directChildCount == 0 || maxChildRight == null) {
            return right;
        } else {
            return maxChildRight;
        }
    }


    /**
     * Get the spacing needed to hold requested number of children
     *
     * @param childCount number of children to get the spacing for
     * @return spacing
     */
    public BigDecimal getSpacing(int childCount) {
        // Every node needs 2 positions for its lft and rgt borders
        BigDecimal result = innerSpace.subtract(new BigDecimal(childCount * 2));
        // Compute the spaces needed in and between the nodes
        BigDecimal spacesNeeded = new BigDecimal((childCount * 2) + 1);
        // Devide the space available and floor it
        return result.divide(spacesNeeded, RoundingMode.FLOOR);
    }

    /**
     * Returns true if the spaces used within the node and its children can be optimized.
     *
     * @return true if the spaces used within the node and its children can be optimized
     */
    public boolean isSpaceOptimizable() {
        // defaultSpacing > 3
        return defaultSpacing.compareTo(THREE) == 1;
    }

    /**
     * Returns true if the node can hold the given amount of children between its left and right
     * borders without the parent(s) being reorganized.
     *
     * @param childCount the child count to check for
     * @param spacing    the spacing between the nodes
     * @return true if the node can hold the given amount of children without beeing reorganized
     */
    public boolean hasSpaceFor(int childCount, int spacing) {
        // Every child node needs at least 2 positions (the left and right border)
        BigDecimal spaceNeeded = new BigDecimal(childCount * 2);
        // Compute needed spacing
        BigDecimal totalSpacing = spacing <= 0 ? BigDecimal.ZERO : new BigDecimal(((childCount * 2) + 1) * spacing);
        // See if we can fit them all in
        spaceNeeded = spaceNeeded.add(totalSpacing);
        return ((this.getInnerSpace().subtract(spaceNeeded)).compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Is this node capable of holding <code>childCount</code> child nodes?
     *
     * @param childCount number of childnodes this node should be able to hold
     * @return if there is enough space to hold <code>childCount</code> child nodes
     */
    public boolean hasSpaceFor(int childCount) {
        return getSpacing(childCount).compareTo(BigDecimal.ZERO) >= 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParentOf(FxTreeNodeInfo node) {
        if (node instanceof FxTreeNodeInfoSpreaded)
            return this.getLeft().compareTo(((FxTreeNodeInfoSpreaded) node).getLeft()) < 0 &&
                    this.getRight().compareTo(((FxTreeNodeInfoSpreaded) node).getRight()) > 0;
        else
            return node.getLeft().longValue() < getLeft().longValue() &&
                    node.getRight().longValue() > getRight().longValue();
    }
}
