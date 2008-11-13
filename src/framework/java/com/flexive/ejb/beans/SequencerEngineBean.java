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
package com.flexive.ejb.beans;

import com.flexive.core.storage.StorageManager;
import com.flexive.shared.CustomSequencer;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.interfaces.SequencerEngineLocal;

import javax.annotation.Resource;
import javax.ejb.*;
import java.util.List;

/**
 * Sequencer beans.
 * Generates Id's for data-tables or user defined sequencers.
 * This beans should be used instead of database specific autoincrements or sequences.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "SequencerEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SequencerEngineBean implements SequencerEngine, SequencerEngineLocal {
    @Resource
    javax.ejb.SessionContext ctx;

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getMaxId() throws FxNotFoundException {
        return StorageManager.getSequencerStorage().getMaxId();
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getId(final System type) throws FxApplicationException {
        try {
            return StorageManager.getSequencerStorage().getId(type);
        } catch (FxApplicationException e) {
            ctx.setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createSequencer(String name, boolean allowRollover, long startNumber) throws FxApplicationException {
        try {
            StorageManager.getSequencerStorage().createSequencer(name, allowRollover, startNumber);
        } catch (FxApplicationException e) {
            ctx.setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeSequencer(String name) throws FxApplicationException {
        try {
            StorageManager.getSequencerStorage().removeSequencer(name);
        } catch (FxApplicationException e) {
            ctx.setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getId(String name) throws FxApplicationException {
        try {
            return StorageManager.getSequencerStorage().getId(name);
        } catch (FxApplicationException e) {
            ctx.setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean sequencerExists(String name) throws FxApplicationException {
        try {
            return StorageManager.getSequencerStorage().sequencerExists(name);
        } catch (FxApplicationException e) {
            ctx.setRollbackOnly();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<CustomSequencer> getCustomSequencers() throws FxApplicationException {
        try {
            return StorageManager.getSequencerStorage().getCustomSequencers();
        } catch (FxApplicationException e) {
            ctx.setRollbackOnly();
            throw e;
        }
    }
}
