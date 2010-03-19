/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.core.storage;

import com.flexive.shared.content.FxDelta;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPropertyData;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fulltext indexer
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1
 */
public interface FulltextIndexer {

    /**
     * Initialize the indexer
     *
     * @param pk  (optional) primary key, required when calling index(...)
     * @param con an open and valid connection
     */
    void init(FxPK pk, Connection con);

    /**
     * Index a property data entry.
     * The caller has to ensure that the properties data type is a text type!
     *
     * @param data property data entry
     */
    void index(FxPropertyData data);

    /**
     * Index a delta change
     *
     * @param change delte change
     */
    void index(FxDelta.FxDeltaChange change);

    /**
     * Remove a specific version as specified by the primary key
     */
    void remove();

    /**
     * Remove a specific version and a specific assignment specified by the assignment id
     *
     * @param assignmentId the assignment's id t.b. removed
     *
     * @since 3.1
     */
    void remove(long assignmentId);

    /**
     * Remove all versions of the primary key
     */
    void removeAllVersions();

    /**
     * Remove all indexed data for a type
     *
     * @param typeId type id
     */
    void removeType(long typeId);

    /**
     * Change the language for an indexed assignment
     *
     * @param assignmentId id of the assignment
     * @param oldLanguage  old language
     * @param newLanguage  new language
     */
    void changeLanguage(long assignmentId, long oldLanguage, long newLanguage);

    /**
     * Set the language for an assignment
     *
     * @param assignmentId assignment id
     * @param lang         language to set
     */
    void setLanguage(long assignmentId, long lang);

    /**
     * Commit changes, to be called prior to cleanup
     *
     * @throws SQLException on errors
     */
    void commitChanges() throws SQLException;

    /**
     * Allow the indexer to clean up used resources like prepared statements, etc.
     */
    void cleanup();

    /**
     * Clean and rebuild the complete fulltext index
     */
    void rebuildIndex();

    /**
     * Rebuild the fulltext index for the given property
     *
     * @param propertyId id of the property
     */
    void rebuildIndexForProperty(long propertyId);

    /**
     * Remove the fulltext index for the given property
     *
     * @param propertyId id of the property
     */
    void removeIndexForProperty(long propertyId);
}
