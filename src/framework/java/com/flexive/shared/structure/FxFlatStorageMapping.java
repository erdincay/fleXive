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
    private long groupAssignmentId;
    private String storage;
    private String column;
    private int level;

    /**
     * Ctor
     *
     * @param assignmentId id of the assignment
     * @param groupAssignmentId the group assignment when the assignment is in "group storage" mode, otherwise -1
     * @param storage      storage name
     * @param column       column name
     * @param level        nesting level
     */
    public FxFlatStorageMapping(long assignmentId, long groupAssignmentId, String storage, String column, int level) {
        this.assignmentId = assignmentId;
        this.groupAssignmentId = groupAssignmentId;
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

    /**
     * @return  the group assignment used for "group storage", -1 if the mapping is in normal flat storage
     */
    public long getGroupAssignmentId() {
        return groupAssignmentId;
    }

    public boolean isGroupStorageMode() {
        return groupAssignmentId > 0;
    }
}