function expandNode(nodeId, parentNodeId) {
    parent.gotoNavMenu(0);
    parent.expandContentTreeNode(nodeId > 0 ? nodeId : parentNodeId);
}

function exportContent(pk) {
    try {
        document.getElementById('exportFrame').src = getBase() + "export/content/" + pk;
    } catch(ex) {
        alertDialog(ex);
    }
}

function addTreeNode(nodeId) {
    var form = document.forms["frm"];
    form["frm:treeNodeParent"].value = nodeId;
    document.getElementById("frm:addTreeNodeButton").onclick();
}


