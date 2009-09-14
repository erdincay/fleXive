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
package com.flexive.shared.interfaces;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxLock;
import com.flexive.shared.FxLockType;
import com.flexive.shared.content.*;
import com.flexive.shared.exceptions.*;

import javax.ejb.Remote;
import java.util.List;

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
     * Load a content container (all versions of a content)
     *
     * @param id requested content id
     * @return container with all versions of the content
     * @throws FxApplicationException on errors
     */
    FxContentContainer loadContainer(long id) throws FxApplicationException;

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
     * <p>
     * Returns all primary keys for the given type. Since this method does not implement security,
     * it may only be called by the global supervisor. You can use the search engine to achieve
     * the same, just with security enabled, with
     * </p>
     * <code>
     * final List&lt;FxPK> folderPks = new SqlQueryBuilder().select("@pk").type("FOLDER").getResult().collectColumn(1);
     * </code>
     *
     * @param typeId           the type to request the primary keys for
     * @param onePkPerInstance return one primary key per instance (with max version) or one per actual version?
     * @return list containing the primary keys
     * @throws FxApplicationException on errors
     */
    List<FxPK> getPKsForType(long typeId, boolean onePkPerInstance) throws FxApplicationException;

    /**
     * Get the binary id for the given XPath.
     * The root ("/") XPath will return the contents preview binary id.
     * An invalid XPath or no associated binary id will throw an FxNotFoundException or
     * FxInvalidParameterException.
     * Security will be checked and FxNoAccessException thrown if restricted.
     *
     * @param pk       primary key
     * @param xpath    XPath
     * @param language the language (if null, the user ticket language will be used)
     * @return binary id
     * @throws FxApplicationException on errors
     */
    long getBinaryId(FxPK pk, String xpath, FxLanguage language) throws FxApplicationException;

    /**
     * Get the binary id for the given XPath.
     * If no binary exists for the requested language, the default language will be tried.
     * The root ("/") XPath will return the contents preview binary id.
     * An invalid XPath or no associated binary id will throw an FxNotFoundException or
     * FxInvalidParameterException.
     * Security will be checked and FxNoAccessException thrown if restricted.
     *
     * @param pk                primary key
     * @param xpath             XPath
     * @param language          the language (if null, the user ticket language will be used)
     * @param fallbackToDefault fall back to the default language if the requested language is not null and not found?
     * @return binary id
     * @throws FxApplicationException on errors
     */
    long getBinaryId(FxPK pk, String xpath, FxLanguage language, boolean fallbackToDefault) throws FxApplicationException;

    /**
     * Load binary meta data
     *
     * @param id id of the binary
     * @return metadata
     */
    String getBinaryMetaData(long id);

    /**
     * Import a content from XML
     *
     * @param xml         the content as XML
     * @param newInstance modify the content to be treated like a new instance
     * @return FxContent
     * @throws FxApplicationException on errors
     */
    FxContent importContent(String xml, boolean newInstance) throws FxApplicationException;

    /**
     * Export a content instance
     *
     * @param content content instance to export
     * @return content as XML
     * @throws FxApplicationException on errors
     */
    String exportContent(FxContent content) throws FxApplicationException;

    /**
     * Lock an instance
     *
     * @param lockType type of the lock
     * @param pk       primary key
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock lock(FxLockType lockType, FxPK pk) throws FxLockException;

    /**
     * Lock an instance
     *
     * @param lockType type of the lock
     * @param pk       primary key
     * @param duration duration in [ms] of the lock
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock lock(FxLockType lockType, FxPK pk, long duration) throws FxLockException;

    /**
     * Take over a lock held by another user (if permitted)
     *
     * @param lock the lock to take over
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock takeOverLock(FxLock lock) throws FxLockException;

    /**
     * Take over a lock held by another user (if permitted)
     *
     * @param pk primary key of the instance whose lock should be taken over
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock takeOverLock(FxPK pk) throws FxLockException;

    /**
     * Extend an existing lock for the given duration (duration will be added to current expire time).
     * If the lock is expired, a new one will be created.
     *
     * @param lock     the lock to extend
     * @param duration duration in [ms] to extend the original expire time
     * @return extended lock
     * @throws FxLockException on errors
     */
    FxLock extendLock(FxLock lock, long duration) throws FxLockException;

    /**
     * Extend an existing lock for the given duration (duration will be added to current expire time).
     * If the lock is expired, a new one will be created.
     *
     * @param pk       primary key of the instance whose lock should be extended
     * @param duration duration in [ms] to extend the original expire time
     * @return extended lock
     * @throws FxLockException on errors
     */
    FxLock extendLock(FxPK pk, long duration) throws FxLockException;

    /**
     * Get the lock for a primary key. If the instance is not locked a lock with <code>FxLockType.None</code> is returned
     *
     * @param pk primary key
     * @return FxLock, if not locked a lock with <code>FxLockType.None</code> is returned
     */
    FxLock getLock(FxPK pk);

    /**
     * Unlock a locked instance. If the instance is not locked, no exception will be thrown
     *
     * @param pk primary key
     * @throws FxLockException on errors
     */
    void unlock(FxPK pk) throws FxLockException;

    /**
     * Query locks
     *
     * @param lockType type of the locks to return, if <code>null</code> all types will be returned
     * @param userId   id of the lock owner, if &lt; 0 locks for all users will be returned,
     *                 if the calling user is not global or mandator supervisor, only locks held by the
     *                 calling user will be returned
     * @param typeId   only return locks for contents of this type, if &lt; 0 type is ignored
     * @param resource name of the locked resources to find, will be ignored if empty or <code>null</code>.
     *                 Queries will be pre- and postfixed by wildcards (%)
     * @return matching locks
     * @throws FxLockException on errors
     */
    List<FxLock> getLocks(FxLockType lockType, long userId, long typeId, String resource) throws FxLockException;
}
