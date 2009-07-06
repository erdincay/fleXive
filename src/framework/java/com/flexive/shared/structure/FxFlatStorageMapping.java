package com.flexive.shared.structure;

import java.io.Serializable;

/**
 * Storage information about an assignment in a flat storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxFlatStorageMapping implements Serializable {
    private static final long serialVersionUID = -4876287678910187513L;
    private long assignmentId;
    private String storage;
    private String column;
    private int level;

    /**
     * Ctor
     *
     * @param assignmentId id of the assignment
     * @param storage      storage name
     * @param column       column name
     * @param level        nesting level
     */
    public FxFlatStorageMapping(long assignmentId, String storage, String column, int level) {
        this.assignmentId = assignmentId;
        this.storage = storage;
        this.column = column;
        this.level = level;
    }

    /**
     * Get the assignment id
     *
     * @return assignment id
     */
    public long getAssignmentId() {
        return assignmentId;
    }

    /**
     * Get the storage name
     *
     * @return storage name
     */
    public String getStorage() {
        return storage;
    }

    /**
     * Get the column name
     *
     * @return column name
     */
    public String getColumn() {
        return column;
    }

    /**
     * Get the nesting level
     *
     * @return nesting level
     */
    public int getLevel() {
        return level;
    }
}