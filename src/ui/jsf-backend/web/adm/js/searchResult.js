/**
 * Supporting Javascripts for the search result page (search/searchResult.xhtml)
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

function getTypeIdSelect() {
    return document.getElementById("frm:typeId");
}

function onShowContextMenu() {
    try {
        var perms = flexive.yui.datatable.getPermissions(resultTable, this.contextEventTarget);
        var pk = flexive.yui.datatable.getPk(resultTable, this.contextEventTarget);
        var selectedIds = getSelectedIds();
        var briefcaseId = getBriefcaseId();
        flexive.yui.setMenuItem("copy", "disabled", selectedIds.length == 0 && pk == null);
        flexive.yui.setMenuItem("edit", "disabled", !perms.edit);
        flexive.yui.setMenuItem("delete", "disabled", !perms["delete"]);
        if (getBriefcaseId() != -1) {
            flexive.yui.setMenuItem("deleteBriefcase", "disabled", briefcaseId == -1);
        }
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
            parent.getContentClipboard().set(selectedIds.length > 0 ? selectedIds : [pk.id]);
            break;
        case "delete":
            if (getSelectedIds().length > 0) {
                confirmDialog(MESSAGES["SearchResult.dialog.confirm.deleteSelection"], function() {
                    try {
                        flexive.util.getJsonRpc().ContentEditor.removeMultiple(selectedIds);
                        if (getViewType() == "LIST") {
                            var rowsDeleted = flexive.yui.datatable.deleteSelectedRows(resultTable);
                            modifyTypeCount(-rowsDeleted);
                        } else {
                            reload();
                        }
                    } catch (e) {
                        alertDialog(e);
                    }
                });
            } else {
                deleteContent(pk);
            }
            invalidateSessionCache();
            break;
        case "deleteBriefcase":
            if (getSelectedIds().length > 0) {
                try {
                    flexive.util.getJsonRpc().BriefcaseEditor.removeItems(briefcaseId, selectedIds);
                    if (getViewType() == "LIST") {
                        var rowsDeleted = flexive.yui.datatable.deleteSelectedRows(resultTable);
                        modifyTypeCount(-rowsDeleted);
                    } else {
                        reload();
                    }
                } catch (e) {
                    alertDialog(e);
                }
            } else {
                deleteFromBriefcase(pk);
            }
            invalidateSessionCache();
            break;
        case "selectAll":
            flexive.yui.datatable.selectAllRows(resultTable);
            parent.showStatusMessage(MESSAGES["SearchResult.status.selected"].replace("{0}", getSelectedIds().length));
            break;
        case "selectAllOnPage":
            if (getViewType() == "LIST") {
                flexive.yui.datatable.selectAllPageRows(resultTable);
            } else {
                flexive.yui.datatable.selectAllPageCells(resultTable);
            }
            parent.showStatusMessage(MESSAGES["SearchResult.status.selected"].replace("{0}", getSelectedIds().length));
            break;
        default:
            alert("Unknown action: " + menuItem.id);
    }
}

function invalidateSessionCache() {
    document.getElementById("frm:invalidateCacheButton").click();
}

function reload() {
    parent.lockScreen();
    document.getElementById("frm:clearCache").value = true;
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
            if (getViewType() == "LIST") {
                var rowsDeleted = flexive.yui.datatable.deleteMatchingRows(
                        resultTable,
                        function(record) { return record.getData("pk") == pk.toString(); }
                );
                modifyTypeCount(-rowsDeleted);
            } else {
                reload();
            }
        } catch (e) {
            alertDialog(e);
            reload();
        }
    });
}

function deleteFromBriefcase(/* PK */ pk) {
    try {
        flexive.util.getJsonRpc().BriefcaseEditor.removeItems(getBriefcaseId(), [pk.id]);
        if (getViewType() == "LIST") {
            var rowsDeleted = flexive.yui.datatable.deleteMatchingRows(
                    resultTable,
                    function(record) { return record.getData("pk") == pk.toString(); }
            );
            modifyTypeCount(-rowsDeleted);
        } else {
            reload();
        }
    } catch (e) {
        alertDialog(e);
    }
}

function modifyTypeCount(/* int */ delta) {
    var option = getSelectedOption(getTypeIdSelect());
    if (option.value == -1) {
        return; // cannot adapt infotype count on shared page
    }
    var text = option.text;
    var match = text.match(/(.*)\((\d+)\)/);
    if (!match) {
        alert("Invalid label format, failed to modify type count: " + text);
        return;
    }
    var newCount = parseInt(RegExp.$2) + delta;
    option.text = RegExp.$1 + " (" + newCount + ")";
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
