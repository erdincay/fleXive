package com.flexive.shared.structure;

import java.io.Serializable;

/**
 * Storage information about an assignment in a flatstore
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxFlatstoreMapping implements Serializable {
    private static final long serialVersionUID = -4876287678910187513L;
    private long assignmentId;
    private String table;
    private String column;
    private int level;

    /**
     * Ctor
     *
     * @param assignmentId id of the assignment
     * @param table        table name
     * @param column       column name
     * @param level        nesting level
     */
    public FxFlatstoreMapping(long assignmentId, String table, String column, int level) {
        this.assignmentId = assignmentId;
        this.table = table;
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
     * Get the table name
     *
     * @return table name
     */
    public String getTable() {
        return table;
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