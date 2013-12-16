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
package com.flexive.core.search.cmis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered list of conditions that are joined by a Boolean operator (AND, OR).
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class ConditionList implements Condition {
    public static enum Connective { AND, OR }

    private final ConditionList parent;
    private final Connective connective;
    private final List<Condition> conditions = new ArrayList<Condition>();

    /**
     * Creates a new (sub-)condition list with the given parent. Since any tree node must have at least
     * two conditions, the only way to build a condition tree is with ConditionList.
     *
     * @param parent    the parent list, or null if this is the root condition
     * @param connective    the connective to be used for joining the conditions.
     */
    public ConditionList(ConditionList parent, Connective connective) {
        this.parent = parent;
        this.connective = connective;
    }

    public Connective getConnective() {
        return connective;
    }

    public void addCondition(Condition condition) {
        if (condition == this) {
            throw new IllegalArgumentException("Cannot add condition list to itself.");
        }
        conditions.add(condition);
    }

    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /** {@inheritDoc} */
    @Override
    public ConditionList getParent() {
        return parent;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(ConditionNodeVisitor visitor) {
        visitor.enterSubCondition(connective);
        for (Condition condition : conditions) {
            condition.accept(visitor);
        }
        visitor.leaveSubCondition();
    }

}
