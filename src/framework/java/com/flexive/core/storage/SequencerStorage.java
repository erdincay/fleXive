package com.flexive.core.storage;

import com.flexive.shared.CustomSequencer;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCreateException;

import java.util.List;

/**
 * Sequencers
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface SequencerStorage {

    /**
     * Return the maximum possible id that can be created
     *
     * @return maximum possible id that can be created
     */
    long getMaxId();

    /**
     * Generates a new unique id for the given type.
     * Issues a ctx.setRollbackOnly if the DB failes to deliver a new ID
     *
     * @param name          the name
     * @param allowRollover if MAX_ID is reached throw exception or reset to zero?
     * @return the new id
     * @throws FxCreateException if the function fails
     */
    long fetchId(final String name, boolean allowRollover) throws FxCreateException;

    /**
     * Get a new unique id for the requested type.
     * Internal method!! Safe to use but of no use to 'end-users'!
     *
     * @param type the type (database table) to get an id for
     * @return new id
     * @throws FxApplicationException on errors
     */
    long getId(FxSystemSequencer type) throws FxApplicationException;

    /**
     * Get a new id from a registered sequencer
     *
     * @param sequencer name of the sequencer to use
     * @return new id
     * @throws FxApplicationException on errors
     */
    long getId(String sequencer) throws FxApplicationException;

    /**
     * Read the current value of the given sequencer without incrementing it
     *
     * @param sequencer name of the sequencer to use
     * @return current value
     * @throws FxApplicationException on errors
     */
    long getCurrentId(String sequencer) throws FxApplicationException;

    /**
     * Read the current value of the given system internal sequencer without incrementing it
     * Internal method!! Safe to use but of no use to 'end-users'!
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
     * Set a sequencer to the given id
     *
     * @param name  name of the sequencer
     * @param newId next id to deliver
     * @throws FxApplicationException on errors
     */
    void setSequencerId(String name, long newId) throws FxApplicationException;
}
