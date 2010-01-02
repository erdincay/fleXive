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
package com.flexive.cmis.webdav;

import com.bradmcevoy.http.*;
import static com.flexive.shared.EJBLookup.getContentEngine;
import static com.flexive.shared.EJBLookup.getAccountEngine;
import com.flexive.shared.FxLockType;
import com.flexive.shared.FxLock;
import com.flexive.shared.FxContext;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxLockException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.cmis.spi.SPIUtils;
import com.flexive.cmis.spi.FlexiveFolder;
import org.apache.chemistry.CMISObject;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Until CMIS supports explicit locking, we support a wrapper that does explicit locking with the flexive APIs.
 * This class implements all methods needed for implemented LockableResource. Since we already use
 * different classes with different interfaces for different resources, we cannot simply
 * create a generic resource wrapper.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveLockSupport {
    private static final Log LOG = LogFactory.getLog(FlexiveLockSupport.class);

    @SuppressWarnings({"UnusedDeclaration"})
    public static LockResult lock(CMISObject object, LockTimeout timeout, LockInfo lockInfo) {
        try {
            return LockResult.success(getLockToken(
                    getContentEngine().lock(
                            FxLockType.Permanent,
                            getContentPK(object),
                            timeout.getSeconds() != null
                                    ? timeout.getSeconds() * 1000
                                    : 60L*60*1000    // 1 hour
                    )
            ));
        } catch (FxLockException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lock object " + object.getId() + ": " + e.getMessage(), e);
            }
            // TODO: may not always be a suitable response
            return LockResult.failed(LockResult.FailureReason.PRECONDITION_FAILED);
        }
    }

    private static FxPK getContentPK(CMISObject object) {
        if (SPIUtils.isFolderId(object.getId())) {
            // return PK of folder instance
            return ((FlexiveFolder) object).getNode().getReference();
        } else {
            // return PK of document
            return SPIUtils.getDocumentId(object.getId());
        }
    }

    public static LockResult refreshLock(CMISObject object, String token) {
        final FxLock lock = getContentEngine().getLock(getContentPK(object));
        if (lock.getLockType() == FxLockType.None) {
            // attempt to lock again
            return lock(object,  new LockTimeout(null), null);
        } else {
            checkToken(lock, token);
            final LockTimeout timeout = LockTimeout.parseTimeout(HttpManager.request());
            final long duration = timeout.getSeconds() != null ? timeout.getSeconds() : 3600;
            final long remainingSeconds = (lock.getExpiresTimestamp() - System.currentTimeMillis()) / 1000;
            if (remainingSeconds < duration) {
                // renew if lock lasts for less than the requested lock duration
                try {
                    return LockResult.success(
                            getLockToken(
                                getContentEngine().extendLock(lock, (duration - remainingSeconds) * 1000L)
                            )
                    );
                } catch (FxLockException e) {
                    throw e.asRuntimeException();
                }
            }
            // return existing lock, which is valid for at least the time that was requested
            return LockResult.success(
                    getLockToken(getContentEngine().getLock(lock.getLockedPK()))
            );
        }
    }

    public static void unlock(CMISObject object, String tokenId) {
        final FxPK pk = getContentPK(object);
        final FxLock lock = getContentEngine().getLock(pk);
        checkToken(lock, tokenId);
        try {
            getContentEngine().unlock(pk);
        } catch (FxLockException e) {
            throw e.asRuntimeException();
        }
    }

    public static LockToken getCurrentLock(CMISObject object) {
        return getLockToken(
                getContentEngine().getLock(getContentPK(object))
        );
    }

    private static void checkToken(FxLock lock, String tokenId) {
        if (tokenId == null || !tokenId.equals(getTokenId(lock))) {
            throw new IllegalArgumentException("Invalid or expired lock token for resource " + lock.getLockedPK() + ".");
        }
    }

    private static String getTokenId(FxLock lock) {
        return String.valueOf(lock.getCreatedTimestamp());
    }

    private static LockToken getLockToken(FxLock lock) {
        if (lock.getLockType() == FxLockType.None) {
            // not locked
            return null;
        } else {
            try {
                return new LockToken(
                        getTokenId(lock),
                        new LockInfo(
                                LockInfo.LockScope.EXCLUSIVE,
                                LockInfo.LockType.WRITE,
                                getAccountEngine().load(lock.getUserId()).getName().toLowerCase(),
                                LockInfo.LockDepth.ZERO
                        ),
                        new LockTimeout((lock.getExpiresTimestamp() - System.currentTimeMillis()) / 1000)
                );
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
    }
}
