/**
 * Supporting Javascripts for the search result page (search/searchResult.xhtml)
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
function onShowContextMenu() {
    try {
        var perms = flexive.yui.datatable.getPermissions(resultTable, this.contextEventTarget);
        var selectedIds = getSelectedIds();
        var briefcaseId = getBriefcaseId();
        flexive.yui.setMenuItem("deleteSelected", "disabled", selectedIds.length == 0 || !perms["delete"]);
        flexive.yui.setMenuItem("copy", "disabled", selectedIds.length == 0);
        flexive.yui.setMenuItem("deleteBriefcaseSelected", "disabled", briefcaseId == -1 || selectedIds.length == 0);
        flexive.yui.setMenuItem("deleteBriefcase", "disabled", briefcaseId == -1);
        flexive.yui.setMenuItem("edit", "disabled", !perms.edit);
        flexive.yui.setMenuItem("delete", "disabled", !perms["delete"]);
    } catch (e) {
        alertDialog(e);
    }
}

function getSelectedIds() {
    return flexive.util.getPkIds(flexive.yui.datatable.getSelectedPks(resultTable));
}


function onContextMenu(type, args) {
    // extract PK
    var pk = flexive.yui.datatable.getPk(resultTable, this.contextEventTarget);
    storePk(pk);

    var selectedIds = getSelectedIds();
    var briefcaseId = getBriefcaseId();
    var menuItem = args[1];
    if (menuItem.cfg.getProperty("disabled")) {
        return;
    }
    switch (menuItem.id) {
        case "show":
            openContent(pk);
            break;
        case "edit":
            editContent(pk);
            break;
        case "showScreenview":
            var a = document.createElement("a");
            a.href = getBase()+"thumbnail/pk" + pk.id + "." + pk.version + "/so";
            a.rel = "lytebox";
            a.title = "Screenview " + pk.id + "." + pk.version;
            myLytebox.start(a, false, false);
            break;
        case "copy":
            parent.getContentClipboard().set(selectedIds);
            break;
        case "delete":
            deleteContent(pk);
            break;
        case "deleteSelected":
            confirmDialog(MESSAGES["SearchResult.dialog.confirm.deleteSelection"], function() {
                try {
                    flexive.util.getJsonRpc().ContentEditor.removeMultiple(selectedIds);
                    reload();
                } catch (e) {
                    alertDialog(e);
                }
            });
            break;
        case "deleteBriefcase":
            deleteFromBriefcase(pk);
            break;
        case "deleteBriefcaseSelected":
            try {
                flexive.util.getJsonRpc().BriefcaseEditor.removeItems(briefcaseId, selectedIds);
                reload();
            } catch (e) {
                alertDialog(e);
            }
            break;
        default:
    }
}

function reload() {
    parent.lockScreen();
    document.forms["frm"].submit();
}

function storePk(/* PK */ pk) {
    document.getElementById("frm:contentEditorId").value = pk.id;
    document.getElementById("frm:contentEditorVersion").value = pk.version;
}

function openContent(/* PK */ pk) {
    storePk(pk);
    document.getElementById("frm:showButton").onclick();
}

function editContent(/* PK */ pk) {
    storePk(pk);
    document.getElementById("frm:editButton").onclick();
}

function deleteContent(/* PK */ pk) {
    confirmDialog(MESSAGES["SearchResult.dialog.confirm.deleteRow"], function() {
        try {
            flexive.util.getJsonRpc().ContentEditor.remove(pk.id);
            reload();
        } catch (e) {
            alertDialog(e);
        }
    });
}

function deleteFromBriefcase(/* PK */ pk) {
    try {
        flexive.util.getJsonRpc().BriefcaseEditor.removeItems(getBriefcaseId(), [pk.id]);
        reload();
    } catch (e) {
        alertDialog(e);
    }
}

function openContextMenuAt(el) {
    var xy = YAHOO.util.Dom.getXY(el);
    resultsMenu.moveTo(xy[0], xy[1]);
    resultsMenu.contextEventTarget = el;
    resultsMenu.triggerContextMenuEvent.fire();
    resultsMenu.show();
}

/** Add action links in list mode */
var _actionColumnHandler = new flexive.yui.datatable.ActionColumnHandler();
_actionColumnHandler.getActionColumn = function(pk, permissions) {
    if (pk == null) {
        return "";
    }
    var out = [];
    var pkArg = "flexive.util.parsePk(\"" + pk.toString() + "\")";
    var moreContent;
    if (getViewType() == "LIST") {
        if (permissions == null || permissions["edit"]) {
            out.push("<a href='javascript:editContent(" + pkArg + ");'>" + MESSAGES["SearchResult.button.action.edit"] + "</a>");
        } else if (permissions == null || permissions["read"]) {
            out.push("<a href='javascript:openContent(" + pkArg + ");'>" + MESSAGES["SearchResult.button.action.open"] + "</a>");
        }
        if (permissions == null || permissions["delete"]) {
            out.push("<a href='javascript:deleteContent(" + pkArg + ");'>" + MESSAGES["SearchResult.button.action.delete"] + "</a>");
        }
        if (getBriefcaseId() != -1) {
            // remove from briefcase
            out.push("<a href='javascript:deleteFromBriefcase(" + pkArg + ");'>" + MESSAGES["SearchResult.button.action.deleteFromBriefcase"] + "</a>");
        }
        moreContent = MESSAGES["SearchResult.button.action.more"];
    } else {
        moreContent = "...";
    }
    var id = "more_link_" + pk.toString();
    out.push("<a id='" + id + "' href='javascript:openContextMenuAt(\"" + id + "\")'>" + moreContent + "</a>");
    return out.join(" | ");
}
