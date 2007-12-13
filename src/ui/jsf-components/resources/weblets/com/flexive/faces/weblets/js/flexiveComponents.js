/**
 * Miscellaneous classes and functions used by flexive JSF components.
 */

var FxMultiLanguageValueInput = function(id, baseRowId, rowIds) {
    this.id = id;
    this.baseRowId = baseRowId;
    this.rowIds = rowIds;
};

FxMultiLanguageValueInput.prototype = {
    onLanguageChanged: function(languageSelect) {
        this.showRow(this.baseRowId + languageSelect.options[languageSelect.selectedIndex].value);
    },

    showRow: function(rowId) {
        for (var i = 0; i < this.rowIds.length; i++) {
            var enabled = this.rowIds[i] == rowId;
            document.getElementById(this.rowIds[i]).style.display = enabled ? "block" : "none";
        }
    }
}

function openReferenceQueryPopup(xpath, updateInputId, formName) {
    window.open(getBase() + "adm/search/browseReferencesPopup.jsf?xPath=" + xpath + "&inputName="
            + updateInputId + "&formName=" + formName,
            "searchReferences", "scrollbars=yes,width=800,height=600,toolbar=no,menubar=no,location=no");
}

function storeFromFilterTable(filterTableName,destination) {
    var w=dojo.widget.byId(filterTableName);
    if(w){
        var s=w.getSelectedData();
        var selectedIds = "";

        for (i=(s.length-1);i>=0;i--) {
            if (selectedIds!='') {
                selectedIds+=",";
            }
            selectedIds+=s[i].Id;
        }
        document.getElementById(destination).value=selectedIds;
    }
    return true;
}

function initializeTreeReporter(widgetIds, _clickHandler) {
    for (var i = 0; i < widgetIds.length; i++) {
        var widget = dojo.widget.manager.getWidgetById(widgetIds[i]);
        var selectHandler = new TreeHandler({clickHandler: _clickHandler});
        selectHandler.subscribe(widget);
    }
}

var ResultSetTableHandler = function(name, tableComponent, menuHandler, viewType, clipboard) {
    this.name = name;
    this.table = document.getElementById(name);
    this.tableComponent = tableComponent;
    this.menuHandler = menuHandler;
    this.viewType = viewType;   // LIST or THUMBNAILS
    this.clipboard = clipboard; // the ContentClipboard instance to be used
    this.showEditButton = false;
    this.showDeleteButton = false;
    this.reference = name + ".fxTableHandler";
    this.rowSelection = null;   // set after table handler creation
    this.itemId = null;    // injected by menu handlers, contains the currently edited row
    this.itemVersion = null;
}

// "special" result set columns created by javascript
// or the result set JSON generator
ResultSetTableHandler.COL_ID = "__id";
ResultSetTableHandler.COL_VERSION = "__version";
ResultSetTableHandler.COL_ACTIONS = "__actions";

ResultSetTableHandler.prototype = {
    /**
     * Add edit/delete/... links to all data rows. 
     */
    addActionLinks: function(dataRows) {
		// insert action buttons
		if (this.hasActionButtons()) {
            if (this.formName == null) {
                return;
            }
            var editButton;
            if (this.showEditButton) {
                editButton = this.formName + ":" + this.name + "_editButton";
            }
			for (var i = 0; i < dataRows.length; i++) {
                var row = dataRows[i];
                var id = row[ResultSetTableHandler.COL_ID];
                var version = row[ResultSetTableHandler.COL_VERSION];
                if (id == null || version == null) {
                    // no buttons if not PK was selected
                    continue;
                }
				var actions = "";
				if (this.showEditButton) {
                    actions += "<a href=\"javascript:" + this.reference + ".menuHandler.edit(" + id + ", " + version + ");\">"
                        + this.menuHandler.messages.editButton + "</a>";
                }
				row[ResultSetTableHandler.COL_ACTIONS] = actions;
			}
		}
    },

    // refresh the current table data
    refresh: function() {
        alert("Refresh not implemented for this table");
    },

    // show row menu handler
    showRow: function() {
        this.menuHandler.show(this.itemId, this.itemVersion);
    },

    // edit row menu handler
    editRow: function() {
        this.menuHandler.edit(this.itemId, this.itemVersion);
    },

    // show screenview
    showScreenview: function() {
        this.menuHandler.showScreenview(this.itemId, this.itemVersion);
    },

    // delete row menu handler
    deleteRow: function() {
        this.menuHandler.deleteRow(this.itemId);
        this.refresh();
    },

    // delete selected rows menu handler
    deleteSelectedRows: function() {
        this.menuHandler.deleteRowSelection(this._getSelection());
        this.refresh();
    },

    // delete row menu handler
    deleteRowBriefcase: function() {
        this.menuHandler.deleteRowBriefcase(this.briefcaseId, this.itemId);
        this.refresh();
    },

    // delete selected rows menu handler
    deleteSelectedRowsBriefcase: function() {
        this.menuHandler.deleteRowSelectionBriefcase(this.briefcaseId, this._getSelection());
        this.refresh();
    },

    // Copy the current selection to the clipboard
    copySelection: function() {
        var selection = this._getSelection();
        this.clipboard.set(selection.length  > 0 ? selection : [this.itemId]);
    },


    onShowContextMenu: function(menu) {
        // set selection-based menu item states
        findMenuItem(menu, "deleteSelected").setDisabled(this.rowSelection.getSelected().length == 0);
        findMenuItem(menu, "deleteBriefcaseSelected").setDisabled(this.briefcaseId == -1 || this.rowSelection.getSelected().length == 0);
        findMenuItem(menu, "deleteBriefcase").setDisabled(this.briefcaseId == -1);
        // get data for this row from the filtering table
        if (this.viewType == "THUMBNAILS") {
            // find column PK in markup
            var positionPk = findParentWithAttribute(menu.openEvent, "positionId").getAttribute("positionId")
            this.itemId = parseInt(positionPk.substring(0, positionPk.indexOf(".")));
            this.itemVersion = parseInt(positionPk.substring(positionPk.indexOf(".") + 1));
        } else {
            // get row PK from table data
            var row = menu.openEvent.target;
            while (row.parentNode && row.nodeName != "TR") {
                row = row.parentNode;
            }
            var rowData = this.tableComponent.getDataByRow(row);
            this.itemId = rowData[ResultSetTableHandler.COL_ID]
            this.itemVersion = rowData[ResultSetTableHandler.COL_VERSION];
        }
        // call dojo's show method (alias created by the MenuWriter)
        menu.show_();
    },

    hasActionButtons: function() {
        return this.showEditButton || this.showDeleteButton;
    },

    _getSelection: function() {
        var data = this.rowSelection.getSelected();
        var selection = [];     // the selected ids
        for (var i = 0; i < data.length; i++) {
            selection.push(parseInt(data[i].substring(0, data[i].indexOf("."))));
        }
        return selection;
    }
}

var ResultMenuHandler = function(formName, name, menuId) {
    this.formName = formName;
    this.name = name;
    this.menuId = menuId;
    this.messages = {
        editButton: "Edit",
        deleteButton: "Delete",
        confirmDeleteRow: "Really delete this row?",
        confirmDeleteSelection: "Really delete selected rows?"
    };
}

ResultMenuHandler.prototype = {
    edit: function(id, version) {
        this._setContent(id, version);
        document.getElementById(this.formName + ":" + this.name + "_editButton").onclick();
    },

    show: function(id, version) {
        this._setContent(id, version);
        document.getElementById(this.formName + ":" + this.name + "_showButton").onclick();
    },

    showScreenview: function(id, version) {
        this._setContent(id, version);
        document.getElementById(this.formName + ":" + this.name + "_refreshScreenviewButton").onclick();
        //eval(this.name + "_screenviewDialog.show()");
    },

    deleteRow: function(id) {
        if (!confirm(this.messages.confirmDeleteRow)) {
            return;
        }
        try {
            getJsonRpc().ContentEditor.remove(id);
            this._reloadBriefcases();
        } catch (e) {
            alert(e.message);
        }
    },

    deleteRowSelection: function(ids) {
        if (!confirm(this.messages.confirmDeleteSelection)) {
            return;
        }
        try {
            getJsonRpc().ContentEditor.removeMultiple(ids);
            this._reloadBriefcases();
        } catch (e) {
            alert(e.message);
        }
    },

    deleteRowBriefcase: function(briefcaseId, itemId) {
        try {
            getJsonRpc().BriefcaseEditor.removeItems(briefcaseId, [itemId]);
            this._reloadBriefcases();
        } catch (e) {
            alert(e.message);
        }
    },

    deleteRowSelectionBriefcase: function(briefcaseId, itemIds) {
        try {
            getJsonRpc().BriefcaseEditor.removeItems(briefcaseId, itemIds);
            this._reloadBriefcases();
        } catch (e) {
            alert(e.message);
        }
    },

    _setContent: function(id, version) {
        eval(this._copyRowValue("contentEditorId", id));
        eval(this._copyRowValue("contentEditorVersion", version));
    },

    _copyRowValue: function(inputName, value) {
        return "document.forms['" + this.formName + "']['" + this.formName + ":" + inputName + "'].value = " + value;
    },

    _reloadBriefcases: function() {
        if (parent.reloadBriefcases) {
            parent.reloadBriefcases();
        }
    }
}

// Handler for the result set table. Handles selection using selection.js.
var ResultThumbnailsTableHandler = function(formName, name, menuId, rowSelection, menuHandler) {
    this.formName = formName;
    this.name = name;
    this.menuId = menuId;
    this.rowSelection = rowSelection;
    this.rowPk = null;
    this.rowId = -1;
    this.rowVersion = -1;
    this.menuHandler = menuHandler;
}

ResultThumbnailsTableHandler.prototype = {
    onShowContextMenu: function() {
        var menu = dojo.widget.byId(this.menuId);
        findMenuItem(menu, "deleteSelected").setDisabled(this.rowSelection.getSelected().length == 0);
        this.rowPk = findParentWithAttribute(menu.openEvent, "positionId").getAttribute("positionId");
        if (this.rowPk != null) {
            this.rowId = this.rowPk.substr(0, this.rowPk.indexOf("."));
            this.rowVersion = this.rowPk.substr(this.rowPk.indexOf(".") + 1);
            menu.show_();
        }
    },

    editRow: function() {
        this.menuHandler.edit(this.rowId, this.rowVersion);
    },

    showRow: function() {
        this.menuHandler.show(this.rowId, this.rowVersion);
    },

    deleteRow: function() {
        this.menuHandler.deleteRow(this.rowId);
        this.refresh();
    },

    deleteSelectedRows: function() {
        var selection = [];
        var selectedPks = this.rowSelection.getSelected();
        for (var i = 0; i < selectedPks.length; i++) {
            var pk = selectedPks[i];
            selection.push(pk.substr(0, pk.indexOf(".")))
        }
        this.menuHandler.deleteRowSelection(selection);
        this.refresh();
    },

    refresh: function() {
        document.forms[this.formName].submit();
    }
}

var JsDataScroller = function(id, startRow, totalRows, fetchRows, onUpdateHandler) {
    this.id = id;
    this.startRow = startRow;
    this.totalRows = totalRows;
    this.fetchRows = fetchRows;
    this.onUpdateHandler = onUpdateHandler; // js function that takes this instance as first parameter
    this.oldStartRow = this.startRow;
}

JsDataScroller.prototype = {
    first: function() {
        this.startRow = 0;
        this.updateDisplay();
    },
    
    previous: function() {
        this.startRow = Math.max(0, this.startRow - this.fetchRows);
        this.updateDisplay();
    },
    
    next: function() {
        if (this.startRow + this.fetchRows < this.totalRows - 1) {
            this.startRow = this.startRow + this.fetchRows;
        }
        this.updateDisplay();
    },
    
    last: function() {
        while (this.startRow + this.fetchRows < this.totalRows - 1) {
            this.startRow += this.fetchRows;
        }
        this.updateDisplay();
    },

    refresh: function() {
        this.oldStartRow = -1;
        this.first();
    },

    updateDisplay: function() {
        // callback handler
        if (this.oldStartRow != this.startRow) {
            this.oldStartRow = this.startRow;
            this.onUpdateHandler(this);
        }
        // update position text
        document.getElementById(this.id + "_position").value = this.getText();
        
        // update button visibility
        document.getElementById(this.id).style.display = this.isNecessary() ? "block" : "none";
        
        this._setVisibility(this.id + "_first", this.isNecessary() && this.startRow > 0);
        this._setVisibility(this.id + "_previous", this.isNecessary() && this.startRow > 0);
        var endRow = this.startRow + this.fetchRows;
        this._setVisibility(this.id + "_next", this.isNecessary() && endRow < this.totalRows - 1);
        this._setVisibility(this.id + "_last", this.isNecessary() && endRow < this.totalRows - 1);
    },

    forceUpdate: function() {
        this.oldStartRow = -1;
    },
    
    getText: function() {
        return (this.startRow + 1) + " - " + Math.min(this.totalRows, this.startRow + this.fetchRows) 
                + " / " + this.totalRows;
    },
    
    isNecessary: function() {
        return this.totalRows > 0;
    },
    
    _setVisibility: function(buttonId, visible) {
        document.getElementById(buttonId).style.opacity = visible ? 1 : 0.3;
    }
}


/** Creates a DOJO menu from a JSON object. Based on Dojo example code. */
function makeMenu(id, menuClass, itemClass, items, isTop, contextMenuTarget){
    var options = {
        templateCssPath: dojo.uri.dojoUri("../../css/dojoCustom/Menu2.css"),
        contextMenuForWindow: isTop,
        toggle: "fade"
    };
    if (id != null) {
        options.widgetId = id;
    }
    if (contextMenuTarget != null) {
        options.targetNodeIds = [contextMenuTarget];
    }
    var menu2 = dojo.widget.createWidget(menuClass, options);
    dojo.lang.forEach(items, function(itemJson){
        // if submenu is specified, create the submenu and then make submenuId point to it
        if( itemJson.submenu){
            var submenu = makeMenu(null, menuClass, itemClass, itemJson.submenu, false);
            itemJson.submenuId = submenu.widgetId;
        }
        var item = dojo.widget.createWidget(itemJson.createAsType == null ? itemClass : itemJson.createAsType,  itemJson);
        menu2.addChild(item);
    });
    return menu2;
}

/** Flexive (DOJO) Tree Handler base class */

var TreeHandler = function(params) {
    this.clickHandler = null;
    for (var param in params) {
        this[param] = params[param];
    }
}

TreeHandler.prototype = {
    /**
     * Attach this tree handler to the given (tree) widget.
     */
    subscribe: function(widget) {
        if (!widget.eventNames) {
            return;
        }
        dojo.event.topic.subscribe(widget.eventNames["select"], this, "onSelect");
        dojo.event.topic.subscribe(widget.eventNames["dblselect"], this, "onDblSelect");
    },

    /**
     * Standard select handler - called when a node is selected/clicked.
     */
    onSelect: function(messages) {
        for (var i in messages) {
            var node = messages[i];
            if (this.clickHandler) {
                this.clickHandler(node, false);
            } else if (node["link"]) {
                // open link in other frame
                parent.contentFrameObj.src = node["link"];
            } else if (node.children.length > 0) {
                // default action: toggle node
                if (node.isExpanded) {
                    node.tree.fxController.collapse(node);
                } else {
                    node.tree.fxController.expand(node);
                }
            }
        }
    },

    onDblSelect: function(messages) {
        for (var i in messages) {
            var node = messages[i];
            if (this.clickHandler) {
                this.clickHandler(node, true);
            }
        }
    }
}


// A simple clipboard for content objects
var ContentClipboard = function() {
    this.ids = [];
}

ContentClipboard.prototype = {
    // set the clipboard content to the given object ID array
    set: function(contentIds) {
        this.ids = [];
        // copy IDs to our own array to prevent aliasing and inter-frame issues
        for (var i = 0; i < contentIds.length; i++) {
            this.ids.push(contentIds[i]);
        }
    },

    // get the current clipboard contents as object ID array
    get: function() {
        return this.ids != null ? this.ids : [];
    },

    // clear the clipboard
    clear: function() {
        this.ids = [];
    },

    // returns true for empty clipboards
    isEmpty: function() {
        return this.get().length == 0;
    }
}