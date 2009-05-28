package com.flexive.war.beans.admin.test;

import java.io.Serializable;

/**
 * Test methods for the navigationTest input page
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class FxNavigationTestBean implements Serializable {
    private static final long serialVersionUID = 5804872222426010712L;

    private long selectedNodeId1;
    private long selectedNodeId2;
    private long selectedNodeId3;

    public long getSelectedNodeId1() {
        return selectedNodeId1;
    }

    public void setSelectedNodeId1(long selectedNodeId1) {
        this.selectedNodeId1 = selectedNodeId1;
    }

    public long getSelectedNodeId2() {
        return selectedNodeId2;
    }

    public void setSelectedNodeId2(long selectedNodeId2) {
        this.selectedNodeId2 = selectedNodeId2;
    }

    public long getSelectedNodeId3() {
        return selectedNodeId3;
    }

    public void setSelectedNodeId3(long selectedNodeId3) {
        this.selectedNodeId3 = selectedNodeId3;
    }
}
