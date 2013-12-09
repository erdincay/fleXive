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
package com.flexive.shared.interfaces;

import com.flexive.shared.CustomSequencer;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;

import javax.ejb.Remote;
import java.util.List;

/**
 * Interface for the sequencer beans.
 * <p/>
 * The beans generates Id's for any data-table.
 * It should be used instead of database spezific autoincrements or sequences.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface SequencerEngine {

    /**
     * Return the maximum possible id that can be created
     *
     * @return maximum possible id that can be created
     * @throws FxNotFoundException if no sequencer engine could be found
     */
    long getMaxId() throws FxNotFoundException;

    /**
     * Get a new unique id for the requested system sequencer.
     * Internal method!! Safe to use but of no use to 'end-users'!
     *
     * @param sequencer the system sequencer to get an id for
     * @return new id
     * @throws FxApplicationException on errors
     */
    long getId(FxSystemSequencer sequencer) throws FxApplicationException;

    /**
     * Get a new unique id from a registered custom sequencer
     *
     * @param sequencer name of the sequencer to use
     * @return new id
     * @throws FxApplicationException on errors
     */
    long getId(String sequencer) throws FxApplicationException;

    /**
     * Read the current value of the given sequencer without incrementing it.
     * Warning: getCurrentId() is not reliable if called after the sequence has been reset or if
     *          the database caches sequencers!
     *
     * @param sequencer name of the sequencer to use
     * @return current value
     * @throws FxApplicationException on errors
     */
    long getCurrentId(String sequencer) throws FxApplicationException;

    /**
     * Read the current value of the given system internal sequencer without incrementing it
     * Internal method!! Safe to use but of no use to 'end-users'!
     * Warning: getCurrentId() is not reliable if called after the sequence has been reset or if
     *          the database caches sequencers!
     *
     * @param sequencer the system sequencer to use
     * @return current value
     * @throws FxApplicationException on errors
     */
    long getCurrentId(FxSystemSequencer sequencer) throws FxApplicationException;

    /**
     * Create a new sequencer
     *
     * @param name          desired name
     * @param allowRollover allow id's that reach the limit (MAX_ID) to be reset to zero?
     * @param startNumber   the number to start at
     * @throws FxApplicationException on errors
     */
    void createSequencer(String name, boolean allowRollover, long startNumber) throws FxApplicationException;

    /**
     * Remove an existing sequencer
     *
     * @param name name of the sequencer
     * @throws FxApplicationException on errors
     */
    void removeSequencer(String name) throws FxApplicationException;

    /**
     * Check existance of a sequencer
     *
     * @param name name of the sequencer to check
     * @return if the sequencer exists
     * @throws FxApplicationException on errors
     */
    boolean sequencerExists(String name) throws FxApplicationException;

    /**
     * Get a list of all known (user-created) sequencer
     *
     * @return list of all known (user-created) sequencer
     * @throws FxApplicationException on errors
     */
    List<CustomSequencer> getCustomSequencers() throws FxApplicationException;

    /**
     * Get the names of all known user-created sequencers. Use this over {@link #getCustomSequencers()}
     * when you only need the sequencer IDs, which is considerably faster on some DBs.
     *
     * @return  all sequencer names
     * @throws FxApplicationException    on errors
     * @since 3.2.0
     */
    List<String> getCustomSequencerNames() throws FxApplicationException;


}
