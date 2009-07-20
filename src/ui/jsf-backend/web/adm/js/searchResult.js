/**
 * Supporting Javascripts for the search result page (search/searchResult.xhtml)
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

var contextMenuPK = null;   // current PK if the context menu was openend

function getTypeIdSelect() {
    return document.getElementById("frm:typeId");
}

function onShowContextMenu() {
    try {
        // if contextEventTarget is null, we're in a submenu
        if (this.contextEventTarget != null) {
            var perms = flexive.yui.datatable.getPermissions(resultTable, this.contextEventTarget);
            var selectedIds = getSelectedIds();
            var pk = tryGetPk(this.contextEventTarget);
            var hasBinary = flexive.yui.datatable.getRecordValue(resultTable, this.contextEventTarget, "hasBinary") == "true";
            contextMenuPK = pk;
            var noItemUnderCursor = pk == null;
            var noSelection = selectedIds.length == 0;
            flexive.yui.setMenuItems(["show", "showScreenview", "edit"], "disabled", noItemUnderCursor);
            flexive.yui.setMenuItems(["copy", "copy_briefcases"], "disabled", noSelection && noItemUnderCursor);
            flexive.yui.setMenuItem("edit", "disabled", !perms["edit"]);
            flexive.yui.setMenuItem("delete", "disabled", !perms["delete"]);
            flexive.yui.setMenuItem("download", "disabled", !hasBinary);
            if (getBriefcaseId() != -1) {
                flexive.yui.setMenuItem("deleteBriefcase", "disabled", noItemUnderCursor);
                flexive.yui.setMenuItem("move_briefcases", "disabled", noSelection && noItemUnderCursor);
            }
            if (parent.getNavFrameWnd().briefcasePanel) {
                // sync briefcase submenu with briefcases from the briefcase panel if it's open
                syncWithBriefcasePanel("copy_briefcases", "copy_briefcase");
                if (getBriefcaseId() != -1) {
                    syncWithBriefcasePanel("move_briefcases", "move_briefcase");
                }
            }
        }
    } catch (e) {
        alertDialog(e);
    }
}

function tryGetPk(element) {
    try {
        return flexive.yui.datatable.getPk(resultTable, element);
    } catch (e) {
        return null;
    }
}

function getSelectedIds() {
    return flexive.util.getPkIds(getSelectedPks());
}

function getSelectedPks() {
    return flexive.yui.datatable.getSelectedPks(resultTable);
}

function syncWithBriefcasePanel(rootMenuItemId, itemIdPrefix) {
    // get briefcase submenu
    var menu = getSubMenu(rootMenuItemId);

    // count existing briefcases in the second menu item group
    var remainingOld = menu.getItemGroups()[1].length;

    // copy briefcases from briefcase panel, which is supposed to be up-to-date
    var briefcases = parent.getNavFrameWnd().briefcasePanel.items;
    for (var i = 0; i < briefcases.length; i++) {
        var briefcase = briefcases[i];
        var menuItemId = getBriefcaseMenuItemId(itemIdPrefix, briefcase);

        // remove old corresponding item as we populate the list because it's apparently not possible
        // to create menu item groups programmatically (YUI 2.7.0)
        var oldMenuItem = YAHOO.widget.MenuManager.getMenuItem(menuItemId);
        if (oldMenuItem != null) {
            menu.removeItem(oldMenuItem, 1);
            remainingOld--;
        }

        addBriefcaseMenuItem(menu, itemIdPrefix, briefcase);
    }
    // remove remaining "old" briefcase items (in case some briefcases were deleted)
    while (remainingOld > 0) {
        menu.removeItem(menu.getItemGroups()[1][0], 1);
        remainingOld--;
    }
}

function getSubMenu(menuItemId) {
    return YAHOO.widget.MenuManager.getMenuItem(menuItemId).cfg.getProperty("submenu");
}

function getBriefcaseMenuItem(itemIdPrefix, briefcase) {
    return YAHOO.widget.MenuManager.getMenuItem(getBriefcaseMenuItemId(itemIdPrefix, briefcase));
}

function getBriefcaseMenuItemId(itemIdPrefix, briefcase) {
    return itemIdPrefix + "_" + briefcase.id;
}

function addBriefcaseMenuItem(menu, itemIdPrefix, briefcase) {
    var fn = itemIdPrefix.indexOf("move") == 0 ? moveToBriefcase : copyToBriefcase;
    var newItem = new YAHOO.widget.MenuItem(briefcaseMenuText(briefcase), {
        "id": getBriefcaseMenuItemId(itemIdPrefix, briefcase),
        "onclick": {
            "fn": function(_name, _event, _menuItem) { fn(briefcase.id, _menuItem); }
        }
    });
    menu.addItem(newItem, 1);
    return newItem;
}


function onContextMenu(type, args) {
    // extract PK
    var pk = tryGetPk(this.contextEventTarget);
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
                            deleteRowsForIds(selectedIds);
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
            break;
        case "deleteBriefcase":
            if (getSelectedIds().length > 0) {
                try {
                    flexive.util.getJsonRpc().BriefcaseEditor.removeItems(briefcaseId, selectedIds);
                    if (getViewType() == "LIST") {
                        deleteRowsForIds(selectedIds);
                        if (parent.reloadBriefcases) {
                            parent.reloadBriefcases();
                        }
                    } else {
                        reload();
                    }
                } catch (e) {
                    alertDialog(e);
                }
            } else {
                deleteFromBriefcase(pk);
            }
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
        case "download":
            window.location = getBase() + "download/pk" + pk.toString() + "/binary";
            break;
        default:
            // ignore, since a custom click handler could have been specified
            //alert("Unknown action: " + menuItem.id);
    }
}

/**
 * Generic method for copying or moving items to briefcases
 *
 * @param fnAction  a function taking the briefcaseId and an array of IDs that performs the desired action,
 *                       returning one or more briefcase info objects (as returned by the BriefcaseEditor's methods)
 * @param fnStatusMessage - a function that takes the number of items and the result from fnAction
 *                       and returns a localized status message to be displayed in the toolbar
 * */
function itemsToBriefcase(briefcaseId, menuItem, fnAction, fnStatusMessage) {
    var selectedIds = getSelectedIds();
    var ids = (selectedIds.length > 0 ? selectedIds :
             (contextMenuPK != null ? [contextMenuPK.id] : [])
            );
    try {
        if (briefcaseId == -1) {
            // prompt for new briefcase name
            promptDialog(MESSAGES["Briefcase.dialog.prompt.briefcaseName"], MESSAGES["Briefcase.dialog.prompt.briefcaseName.default"],
                    function (name) {
                        if (name == null || name.length == 0) {
                            return;
                        }
                        // create briefcase
                        var result = eval("(" + flexive.util.getJsonRpc().BriefcaseEditor.create(name) + ")");
                        var newItem = addBriefcaseMenuItem(getSubMenu("copy_briefcases"), "copy_briefcase", result);
                        if (getBriefcaseId() != -1) {
                            // also add to "move to briefcase" submenu
                            addBriefcaseMenuItem(getSubMenu("move_briefcases"), "move_briefcase", result);
                        }

                        // call us again using the created briefcases ID
                        itemsToBriefcase(result.id, newItem, fnAction, fnStatusMessage);
                    }
            );
            return; // transfer control to popup dialog
        }
        // add items to briefcase
        var result = fnAction(briefcaseId, ids);
        if (result == null) {
            return;
        }

        // update briefcase menuitems
        var briefcases = YAHOO.lang.isArray(result) ? result : [result];
        for (var i = 0; i < briefcases.length; i++) {
            var briefcase = briefcases[i];
            getBriefcaseMenuItem("copy_briefcase", briefcase).cfg.setProperty("text", briefcaseMenuText(briefcase));
            if (getBriefcaseId() != -1) {
                getBriefcaseMenuItem("move_briefcase", briefcase).cfg.setProperty("text", briefcaseMenuText(briefcase));
            }
        }

        if (parent.reloadBriefcases) {
            parent.reloadBriefcases();
        }
        parent.showStatusMessage(fnStatusMessage(ids.length, result));
    } catch (e) {
        alertDialog(e);
    }
}

function copyToBriefcase(briefcaseId, menuItem) {
    itemsToBriefcase(briefcaseId, menuItem,
            function(briefcaseId, ids) {
                return eval("(" + flexive.util.getJsonRpc().BriefcaseEditor.add(briefcaseId, ids) + ")");
            },
            function(numItems, result) {
                // delay briefcase flash until status message animation completes
                window.setTimeout(function() {
                    flashBriefcase(briefcaseId);
                }, 500);
                return MESSAGES["Briefcase.status.addedSelection"]
                        .replace("{0}", numItems)
                        .replace("{1}", result.name);
            }
    );
}

function moveToBriefcase(briefcaseId, menuItem) {
    itemsToBriefcase(briefcaseId, menuItem,
            function(briefcaseId, ids) {
                var result = eval("(" + flexive.util.getJsonRpc().BriefcaseEditor.move(getBriefcaseId(), briefcaseId, ids) + ")");
                if (getViewType() == "LIST") {
                    deleteRowsForIds(ids);
                    return [result["source"], result["destination"]];
                } else {
                    reload();
                    return null;
                }
            },
            function(numItems, result) {
                // delay briefcase flash until status message animation completes
                window.setTimeout(function() {
                    flashBriefcase(getBriefcaseId());
                    flashBriefcase(briefcaseId);
                }, 500);
                return MESSAGES["Briefcase.status.movedSelection"]
                    .replace("{0}", numItems)
                    .replace("{1}", result[0].name)   // from
                    .replace("{2}", result[1].name);  // to
            }
    );
}

function briefcaseMenuText(briefcaseInfo) {
    return briefcaseInfo.name + " (" + briefcaseInfo.size + ")";
}

function flashBriefcase(briefcaseId) {
    if (parent.getNavFrameWnd().briefcasePanel) {
        return parent.getNavFrameWnd().briefcasePanel.flash(briefcaseId);
    }
    return null;
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
    if (pk != null) {
        document.getElementById("frm:contentEditorId").value = pk.id;
        document.getElementById("frm:contentEditorVersion").value = pk.version;
    }
}

function openContent(/* PK */ pk) {
    storePk(pk);
    document.getElementById("frm:showButton").onclick();
}

function editContent(/* PK */ pk) {
    storePk(pk);
    document.getElementById("frm:editButton").onclick();
}


function deleteRowsForPks(pks) {
    var pkStrings = {};
    for (var i = 0; i < pks.length; i++) {
        pkStrings[pks[i].toString()] = true;
    }
    var rowsDeleted = flexive.yui.datatable.deleteMatchingRows(
            resultTable,
            function(record) { return pkStrings[record.getData("pk")] != null; }
    );
    modifyTypeCount(-rowsDeleted);
    invalidateSessionCache();
}

function deleteRowsForIds(ids) {
    var lookup = {};
    for (var i = 0; i < ids.length; i++) {
        lookup[ids[i]] = true;
    }
    var rowsDeleted = flexive.yui.datatable.deleteMatchingRows(
            resultTable,
            function(record) { return lookup[flexive.util.parsePk(record.getData("pk")).id] != null; }
    );
    modifyTypeCount(-rowsDeleted);
    invalidateSessionCache();
}

function deleteContent(/* PK */ pk) {
    confirmDialog(MESSAGES["SearchResult.dialog.confirm.deleteRow"], function() {
        try {
            flexive.util.getJsonRpc().ContentEditor.remove(pk.id);
            if (getViewType() == "LIST") {
                deleteRowsForPks([pk]);
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
            deleteRowsForPks([pk]);
            if (parent.reloadBriefcases) {
                parent.reloadBriefcases();
            }
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


function subscribeResultTableEvents(/* YAHOO.widget.DataTable */ resultTable, sortColumnKey, sortDirection) {
    // store sort information in form variables
    resultTable.subscribe("columnSortEvent", function(args) {
        document.getElementById("frm:sortColumnKey").value = args.column.key;
        document.getElementById("frm:sortDirection").value = args.dir;
    });
    // store new column order in result preferences
    resultTable.subscribe("columnReorderEvent", function(args) {
        if (args.column.getKey() == "__actions") {
            return; // reordered the actions column, do nothing
        }
        // skip actions column
        var index = args.column.getKeyIndex();
        for (var i = 0; i < index; i++) {
            if (resultTable.getColumnSet().getColumn(i).getKey() == "__actions") {
                // skip actions column for the result preferences index
                index -= 1;
                break;
            }
        }
        flexive.util.getJsonRpc().YahooResultProvider.reorderResultColumn(
                function(result, exception) {
                    if (exception != null) {
                        alertDialog(exception.message);
                        return;
                    }
                    parent.showStatusMessage(
                            MESSAGES["SearchResult.status.column.reordered"]
                                    .replace("{0}", args.column.label)
                                    .replace("{1}", (index + 1))
                    );
                    invalidateSessionCache();
                },
                getSelectedOptionValue(getTypeIdSelect()),
                getEncodedViewType(),
                getEncodedLocation(),
                args.column.getKey(),
                index
        );
    });
    // fetch old sort information, apply it if the column still exists
    var paginator = resultTable.get("paginator");
    if (sortColumnKey != "null" && sortColumnKey.length > 0) {
        var col = resultTable.getColumn(sortColumnKey);
        if (col != null) {
            var page = paginator.getCurrentPage();  // remember current page
            resultTable.sortColumn(col, sortDirection);
            paginator.setPage(page, false);
        } else {
            // reset sort information
            document.getElementById("frm:sortColumnKey").value = "";
        }
    }
    // subscribe to paginator changes
    paginator.subscribe("pageChange", function(event) {
        document.getElementById("frm:paginatorPage").value = event.newValue;
        window.scrollTo(0, 0);
    });
    paginator.subscribe("rowsPerPageChange", function(event) {
        document.getElementById("frm:fetchRows").value = event.newValue;
        window.scrollTo(0, 0);
    });
}