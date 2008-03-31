// Javascripts for the admin query editor pages

var form = document.frm;
var isQueryEditor = true;

// Add a query for a structure element of the given type (see StructureTreeWriter for possible values)
function addQueryNode(id, nodeDocType) {
    if (nodeDocType.indexOf("Assignment") == 0) {
        addAssignment(id);
    } else if (nodeDocType == "Type") {
        addTypeQuery(id);
    } else {
        // do nothing
    }
}

function addPropertyQueryNode(propertyId) {
    var button = document.getElementById("frm:addPropertyButton");
    form["frm:addAssignmentId"].value = propertyId;
    button.onclick();
}

function addAssignment(assignmentId) {
    var button = document.getElementById("frm:addAssignmentButton");
    form["frm:addAssignmentId"].value = assignmentId;
    button.onclick();
}

function addTreeNode(nodeId, liveMode) {
    var button = document.getElementById("frm:addTreeNodeButton");
    form["frm:addAssignmentId"].value = nodeId;
    form["frm:addNodeLive"].value = liveMode;
    button.onclick();
}

function addTypeQuery(typeId) {
    var button = document.getElementById("frm:addTypeButton");
    form["frm:addAssignmentId"].value = typeId;
    button.onclick();
}

function toggleBriefcase(element) {
    var bd = document.getElementById("frm:briefcaseData");
    bd.style.display = element.checked ? 'inline' : 'none';
}

function toggleSaveQuery(element) {
    var bd = document.getElementById("frm:saveQueryData");
    bd.style.display = element.checked ? 'inline' : 'none';
}

dojo.addOnLoad(function() {
    // toggle search mode in content/structure frames
    if (parent.getContentNavFrame().enableSearchMode) {
        parent.getContentNavFrame().enableSearchMode();
    }
    if (parent.getStructureNavFrame().enableSearchMode) {
        parent.getStructureNavFrame().enableSearchMode();
    }
});

dojo.addOnUnload(function() {
    // toggle search mode in content/structure frames
    if (parent.getContentNavFrame().disableSearchMode) {
        parent.getContentNavFrame().disableSearchMode();
    }
    if (parent.getStructureNavFrame().disableSearchMode) {
        parent.getStructureNavFrame().disableSearchMode();
    }
});
