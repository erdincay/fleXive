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
package com.flexive.shared.interfaces;

import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxContentSecurityInfo;
import com.flexive.shared.content.FxContentVersionInfo;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;

import javax.ejb.Remote;

/**
 * ContentEngine
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface ContentEngine {

    /**
     * Initialize a new FxContent instance with preferred acl, step and language
     *
     * @param typeId     type to use
     * @param mandatorId mandator to assign this content (has to be available for the type!)
     * @param prefACL    preferred acl, if not applicable the best fit will be used
     * @param prefStep   preferred step, if not applicable the best fit will be used
     * @param prefLang   preferred language, if not applicable the best fit will be used
     * @return FxContent
     * @throws FxApplicationException TODO
     * @throws FxLoadException        on initialization errors
     */
    FxContent initialize(long typeId, long mandatorId, long prefACL, long prefStep, long prefLang) throws FxApplicationException;

    /**
     * Initialize a new FxContent instance for a type with default values.
     * mandator, ACL, Step and Lang will be taken on a "first-useable" basis
     *
     * @param typeId type to use
     * @return content instance
     * @throws FxApplicationException on errors
     */
    FxContent initialize(long typeId) throws FxApplicationException;

    /**
     * Initialize a new FxContent instance for a type with default values.
     * mandator, ACL, Step and Lang will be taken on a "first-useable" basis
     *
     * @param typeName type to use
     * @return content instance
     * @throws FxApplicationException on errors
     */
    FxContent initialize(String typeName) throws FxApplicationException;

    /**
     * Load a content
     *
     * @param pk primary key to load
     * @return FxContent
     * @throws FxApplicationException TODO
     * @throws FxLoadException
     * @throws FxNoAccessException
     * @throws FxNotFoundException    if no instance for this primary key was found
     */
    FxContent load(FxPK pk) throws FxApplicationException;

    /**
     * Store an existing content or create a new one
     *
     * @param content the content to persist
     * @return the primary key of the content
     * @throws FxApplicationException TODO
     * @throws FxCreateException      on errors
     * @throws FxUpdateException      on errors
     * @throws FxNoAccessException
     */
    FxPK save(FxContent content) throws FxApplicationException;

    /**
     * Prepare a content for a save or create operation (resolves binaries for script processing, etc.).
     * This is <b>optional</b> and is not required to be called prior to saving, it exists to have
     * metadata available in GUI's prior to saving.
     *
     * @param content the content to prepare
     * @return the prepared content
     * @throws FxInvalidParameterException on errors
     * @throws FxDbException               on errors
     */
    FxContent prepareSave(FxContent content) throws FxApplicationException;

    /**
     * Create a new version for an existing content instance
     *
     * @param content the content to persist
     * @return the primary key of the contents new version
     * @throws FxApplicationException TODO
     * @throws FxCreateException      on errors
     * @throws FxUpdateException      on errors
     * @throws FxNoAccessException
     */
    FxPK createNewVersion(FxContent content) throws FxApplicationException;

    /**
     * Remove a content
     *
     * @param pk primary key of the content to remove
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException
     *                                on errors
     * @throws FxNoAccessException    on errors
     */
    void remove(FxPK pk) throws FxApplicationException;

    /**
     * Remove a content's version.
     * If the content consists only of this specific version the whole content is removed
     *
     * @param pk primary key of a distinct version to remove
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException
     *                                on errors
     * @throws FxNoAccessException    on errors
     */
    void removeVersion(FxPK pk) throws FxApplicationException;

    /**
     * Remove all instances of the given type.
     * Beans using this method should apply very strict security restrictions!
     *
     * @param typeId affected FxType
     * @return number of instances removed
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException
     *                                on errors
     */
    int removeForType(long typeId) throws FxApplicationException;


    /**
     * Get all security relevant information about a content instance identified by its primary key
     *
     * @param pk primary key to query security information for
     * @return FxContentSecurityInfo
     * @throws FxApplicationException TODO
     */
    FxContentSecurityInfo getContentSecurityInfo(FxPK pk) throws FxApplicationException;

    /**
     * Get information about the versions used for a content id
     *
     * @param id the id to query version information for
     * @return FxContentVersionInfo
     * @throws FxApplicationException on errors
     */
    FxContentVersionInfo getContentVersionInfo(FxPK id) throws FxApplicationException;

    /**
     * Get the number of references that exist for the requested content
     *
     * @param pk primary key of the requested content
     * @return number of references that exist for the requested content
     * @throws FxApplicationException on errors
     */
    int getReferencedContentCount(FxPK pk) throws FxApplicationException;

    /**
     * Get the binary id for the given XPath.
     * The root ("/") XPath will return the contents preview binary id.
     * An invalid XPath or no associated binary id will throw an FxNotFoundException or
     * FxInvalidParameterException.
     * Security will be checked and FxNoAccessException thrown if restricted.
     *
     * @param pk    primary key
     * @param xpath XPath
     * @return binary id
     * @throws FxApplicationException on errors
     */
    long getBinaryId(FxPK pk, String xpath) throws FxApplicationException;
}
