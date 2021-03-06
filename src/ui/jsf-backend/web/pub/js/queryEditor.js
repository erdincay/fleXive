/**
 * Query editor javascript
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

var QueryRowSelection = function(document, addAssignmentInput, selectionInput, rowPrefix) {
    this.document = document;
    this.rowPrefix = rowPrefix;
    this.addAssignmentInput = addAssignmentInput;
    this.selectionInput = selectionInput;
    this.selectedIds = [];
    this.selectedRowStyle = "selection";
    this.activeNodeId = -1;
}

QueryRowSelection.prototype = {

    handleRowClick: function(event, nodeId) {
        var oldActiveNodeId = this.activeNodeId;
        this.updateActiveRow(nodeId);
        if (event.shiftKey) {
            // select range
            this.deselectAllRows();
            var selection = this.getRowRange(oldActiveNodeId, nodeId);
            for (var i = 0; i < selection.length; i++) {
                this.selectRow(selection[i]);
            }
        } else if (event.ctrlKey) {
            if (this.isSelected(nodeId)) {
                this.deselectRow(nodeId);
            } else {
                this.selectRow(nodeId);
            }
        } else {
            this.deselectAllRows();
            // select single row
            this.selectRow(nodeId);
        }
    },

    selectRow: function(nodeId) {
        if (this.isSelected(nodeId)) {
            return;
        }
        var element = this.getRow(nodeId);
        element.className = element.className + " " + this.selectedRowStyle;
        this.selectedIds.push(nodeId);
        this.updateFormValue();
    },

    deselectRow: function(nodeId) {
        if (!this.isSelected(nodeId)) {
            return;
        }
        var element = this.getRow(nodeId);
        element.removeAttribute("selected");
        element.className = element.className.replace(" " + this.selectedRowStyle, "");

        // remove node from selected node list
        var selected = [];
        for (var i = 0; i < this.selectedIds.length; i++) {
            var id = this.selectedIds[i];
            if (id != nodeId) {
                selected.push(id);
            }
        }
        this.selectedIds = selected;
        this.updateFormValue();
    },

    updateFormValue: function() {
        this.selectionInput.value = this.selectedIds.join(",");
    },

    deselectAllRows: function() {
        while (this.selectedIds.length > 0) {
            this.deselectRow(this.selectedIds[0]);
        }
    },

    updateActiveRow: function(nodeId) {
        if (this.activeNodeId == nodeId) {
            return;
        }
        if (this.activeNodeId != -1) {
            // remove border from old row
            var oldElement = this.getRow(this.activeNodeId);
            oldElement.style.border = oldElement.oldBorderStyle;
            oldElement.oldBorderStyle = null;
        }
        var element = this.getRow(nodeId);
        element.oldBorderStyle = element.style.border;
        element.style.border = "1px solid black";
        this.activeNodeId = nodeId;
        this.addAssignmentInput.value = nodeId;
    },

    getRow: function(nodeId) {
        return this.document.getElementById(this.rowPrefix + nodeId);
    },

    getNodeId: function(row) {
        return row.getAttribute && row.getAttribute("id") != null
                ? row.getAttribute("id").replace(this.rowPrefix, "") : null;
    },

    getRowRange: function(nodeId1, nodeId2) {
        var row1 = this.getRow(nodeId1);
        var row2 = this.getRow(nodeId2);
        var result = [];
        var inRange = false;
        var nodes = this.document.getElementsByTagName(row1.nodeName);
        for (var i = 0; i < nodes.length; i++) {
            var child = nodes[i];
            var childNodeId = this.getNodeId(child);
            if (child.nodeType != row1.nodeType || childNodeId == null) {
                continue;
            }
            if (child == row1 || child == row2) {
                result.push(childNodeId);
                if (!inRange && row1 != row2) {
                    // begin selection
                    inRange = true;
                } else {
                    // end selection
                    inRange = false;
                    break;
                }
            } else if (inRange) {
                result.push(childNodeId);
            }
        }
        return result;
    },

    isSelected: function(nodeId) {
        for (var i = 0; i < this.selectedIds.length; i++) {
            if (this.selectedIds[i] == nodeId) {
                return true;
            }
        }
        return false;
    }
}