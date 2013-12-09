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

import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;

import java.util.List;

/**
 * A selected table in the CMIS query.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public interface TableReference {

    /**
     * Returns the table reference identified by the given alias. For a normal table reference, this
     * returns the instance itself when {@code this.getAlias().equals(alias) == true},
     * for JOIN references the search includes all tables that are part of the JOIN.
     *
     * @param alias the requested table alias
     * @return      the table reference with the given alias, or null if none was found
     */
    TableReference findByAlias(String alias);

    /**
     * Return the table alias of this table. If no alias was chosen by the user, the table name
     * will be used as alias.
     *
     * @return  the table alias of this table
     */
    String getAlias();

    /**
     * Return all aliases that are represented by this table reference. For JOIN tables this includes the aliases of
     * the tables that are part of the JOIN.
     *
     * @return  all aliases that are represented by this table reference
     */
    List<String> getReferencedAliases();

    /**
     * Return all types that are referenced by this table. This includes subtypes that are derived from
     * the type selected by the user.
     *
     * @return  all types that are referenced by this table.
     */
    List<FxType> getReferencedTypes();

    /**
     * Return the root type of the table, i.e. the type that was specified as the table name. The root type
     * is only defined for {@link com.flexive.core.search.cmis.model.SingleTableReference SingleTableReferences},
     * not for compound (JOIN) tables.
     *
     * @return  the root type of the table, i.e. the type that was specified as the table name.
     */
    FxType getRootType();

    /**
     * Return the assignment(s) that the given name references, including derived assignments.
     * The base assignment (i.e. the assignment referencing the type of this table) will be returned
     * as the first element.
     *
     * @param environment   the environment to be used for resolving the assignment
     * @param name  the assignment name
     * @return      the assignment(s) that the given name references
     */
    List<FxPropertyAssignment> resolvePropertyAssignment(FxEnvironment environment, String name);

    /**
     * Returns the (unique) name of the ID filter column, e.g. "table01_id". This column is selected
     * for representing results from this table and will be used for JOINs between tables.
     *
     * @return  the (unique) name of the ID filter columns
     */
    String getIdFilterColumn();

    /**
     * Returns the (unique) name of the version filter column, e.g. "table01_ver". This column is selected
     * for representing results from this table and will be used for JOINs between tables.
     *
     * @return  the (unique) name of the ID filter columns
     */
    String getVersionFilterColumn();

    /**
     * Returns the link condition between a filter table and a subselect. The columns selected from
     * the filter table are {@link #getIdFilterColumn()} and {@link #getVersionFilterColumn()}, i.e.
     * the columns provided for selection.
     * <p>
     * For example:
     * <code>
     * getIdVersionLink("filter", "sub")
     * -> filter.article_id=sub.id AND filter.article_ver=sub.ver
     * </code>
     * </p>
     *
     * @param filterTableAlias      the filter table alias
     * @param subSelectTableAlias   the subselect table alias
     * @return  the link condition between a filter table and a subselect.
     */
    String getIdVersionLink(String filterTableAlias, String subSelectTableAlias);

    /**
     * Returns all subtables of this table (for JOIN table references).
     *
     * @return  all subtables of this table (for JOIN table references).
     */
    List<TableReference> getSubTables();

    /**
     * Returns all "leaf" subtables, i.e. all subtables that are not JOIN tables.
     *
     * @return  all subtables that are not JOIN tables
     */
    Iterable<TableReference> getLeafTables();

    /**
     * Returns true if property-level security is enabled for at least one referenced type.
     *
     * @return  true if property-level security is enabled for at least one referenced type.
     */
    boolean isPropertySecurityEnabled();

    /**
     * Accept a {@link com.flexive.core.search.cmis.model.TableReferenceVisitor} for this instance.
     *
     * @param visitor   the visitor instance
     */
    void accept(TableReferenceVisitor visitor);
}
