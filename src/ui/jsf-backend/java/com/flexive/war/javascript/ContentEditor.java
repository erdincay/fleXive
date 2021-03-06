/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.javascript;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLockType;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeRemoveOp;
import com.flexive.war.JsonWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

/**
 * Content editor actions invoked via JSON/RPC.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ContentEditor implements Serializable {
    private static final long serialVersionUID = 8418186403482323113L;
    private static final Log LOG = LogFactory.getLog(ContentEditor.class);

    /**
     * Delete the given content.
     *
     * @param id the content ID
     * @return an empty result
     * @throws Exception if an error occured
     */
    public String remove(long id) throws Exception {
        return removeMultiple(new long[]{id});
    }

    /**
     * Delete the given contents.
     *
     * @param ids the content IDs
     * @return an empty result
     * @throws Exception if an error occured
     */
    public String removeMultiple(long[] ids) throws Exception {
        try {
            for (long id : ids) {
                EJBLookup.getContentEngine().remove(new FxPK(id));
            }
            return "[]";
        } catch (Exception e) {
            LOG.error("Failed to remove content: " + e.getMessage());
            throw e;
        }
    }

    public String lockMultiple(String[] pks) throws IOException {
        int count = 0;
        for (String pk : pks) {
            try {
                EJBLookup.getContentEngine().lock(FxLockType.Permanent, FxPK.fromString(pk));
                count++;
            } catch (FxApplicationException e) {
                // failed to lock, ignore but don't count instance
            }
        }
        return writeUpdateCount(count);
    }

    public String unlockMultiple(String[] pks) throws IOException {
        int count = 0;
        for (String pk : pks) {
            try {
                EJBLookup.getContentEngine().unlock(FxPK.fromString(pk));
                count++;
            } catch (FxApplicationException e) {
                // failed to unlock, ignore but don't count instance
            }
        }
        return writeUpdateCount(count);
    }

    public String detach(long folderId, long contentId, boolean live) throws Exception {
        return detachMultiple(folderId, new long[] { contentId }, live);
    }

    public String detachMultiple(long folderId, long[] contentIds, boolean live) throws Exception {
        if (folderId == -1) {
            return writeUpdateCount(0);
        }
        try {
            int count = 0;
            for (long contentId : contentIds) {
                try {
                    final FxTreeNode child = EJBLookup.getTreeEngine().findChild(live ? FxTreeMode.Live : FxTreeMode.Edit, folderId, contentId);
                    EJBLookup.getTreeEngine().remove(child, FxTreeRemoveOp.Unfile, true);
                    count++;
                } catch (FxNotFoundException e) {
                    // ignore
                }
            }
            return writeUpdateCount(count);
        } catch (Exception e) {
            LOG.error("Failed to detach content: " + e.getMessage());
            throw e;
        }
    }

    private String writeUpdateCount(int count) throws IOException {
        StringWriter out = new StringWriter();
        return new JsonWriter(out).startMap().writeAttribute("count", count)
                .closeMap().finishResponse().toString();
    }


}
