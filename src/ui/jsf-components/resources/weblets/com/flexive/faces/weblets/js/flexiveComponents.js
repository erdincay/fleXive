/**
 * Miscellaneous classes and functions used by flexive JSF components.
 */


var flexive = new function() {
    /**
     * Enumeration of valid thumbnail preview sizes - see BinaryDescriptor#PreviewSizes
     */
    this.PreviewSizes = {
        PREVIEW1: { id: 1, size: 42 },
        PREVIEW2: { id: 2, size: 85 },
        PREVIEW3: { id: 3, size: 232 },
        ORIGINAL: { id: 0, size: -1}
    };

}

// miscellaneous utility functions
flexive.util = new function() {

    /**
     * Return the URL for the thumbnail of the given PK (relative to the flexive root path).
     *
     * @param pk    the object PK
     * @param size  the thumbnail size (one of FxComponents.PreviewSizes)
     * @param includeTimestamp  if true, the current timestamp will be included (disables caching)
     */
    this.getThumbnailURL = function(/* String */ pk, /* flexive.PreviewSizes */ size, /* boolean */ includeTimestamp) {
        return "thumbnail/pk" + pk
                + (size != null ? "/s" + size.id : "")
                + (includeTimestamp ? "/ts" + new Date().getTime() : "");
    };

    /**
     * Returns the preview size with the given ID.
     *
     * @param id    the preview size ID (0-3)
     */
    this.getPreviewSize = function(/* int */ id) {
        for (var ps in flexive.PreviewSizes) {
            if (flexive.PreviewSizes[ps].id == id) {
                return flexive.PreviewSizes[ps];
            }
        }
        return null;
    };
}

// Yahoo UI (YUI) helper methods and classes
flexive.yui = new function() {
    /** A list of all required components in the current page. Evaluated at the end of the page to initialize Yahoo.*/
    this.requiredComponents = [];
    /** A list of callback functions to be called when YUI has been fully loaded */
    this.onYahooLoadedFunctions = [];

    /**
     * Adds the given function to flexive.yui.onYahooLoaded.
     *
     * @param fn    the function to be called when Yahoo has been loaded
     */
    this.onYahooLoaded = function(fn) {
        this.onYahooLoadedFunctions.push(fn);
    }

    /**
     * Adds the given Yahoo component to the required components of this page.
     *
     * @param component the Yahoo component (e.g. "button")
     */
    this.requireComponent = function(component) {
        var found = false;
        for (var i = 0; i < this.requiredComponents.length; i++) {
            if (this.requiredComponents[i] == component) {
                found = true;
                break;
            }
        }
        if (!found) {
            this.requiredComponents.push(component);
        }
    }

    /**
     * Call setup methods of flexive components after yahoo has been initialized.
     */
    this.processOnYahoo = function() {
        for (var i = 0; i < this.onYahooLoadedFunctions.length; i++) {
            this.onYahooLoadedFunctions[i]();
        }
        this.onYahooLoadedFunctions = [];
    }
}

flexive.yui.datatable = new function() {
    /**
     * Return the datatable wrapper for the correct view (list or thumbnails).
     *
     * @param result    the search result object as returned by fxSearchResultBean.jsonResult
     */
    this.getViewWrapper = function(result) {
        return result.viewType == "THUMBNAILS"
                ? new flexive.yui.datatable.ThumbnailView(result)
                : new flexive.yui.datatable.ListView(result);
    }

    this.getPk = function(/* YAHOO.widget.DataTable */ dataTable, /* Element */ element) {
        var elRow = dataTable.getTrEl(element);
        var record = dataTable.getRecord(elRow);
        if (record == null) {
            YAHOO.log("No primary key found for element " + element, "warn");
            return null;
        }
        var data = record.getData();
        if (data.pk) {
            // only one pk for this column, return it
            return data.pk;
        } else {
            // one PK per column, get column index and return PK
            var elCol = dataTable.getTdEl(element);
            var col = dataTable.getColumn(elCol);
            return data.pks[col.getKeyIndex()];
        }
    }
}

/**
 * List result view - returns the linear result list of the search result
 * @param result    the search result object as returned by fxSearchResultBean.jsonResult
 */
flexive.yui.datatable.ListView = function(result) {
    this.result = result;
    this.rowsPerPage = 25;
}
flexive.yui.datatable.ListView.prototype = {
    getColumns: function() {
        return this.result.columns;
    },

    getResponseSchema: function() {
        return this.result.responseSchema;
    },

    getRows: function() {
        return this.result.rows;
    }
}

/**
 * Thumbnail view - projects the linear result list to a thumbnail grid
 * @param result    the search result object as returned by fxSearchResultBean.jsonResult
 */
flexive.yui.datatable.ThumbnailView = function(result) {
    this.result = result;
    this.previewSize = flexive.PreviewSizes.PREVIEW2;
    this.gridColumns = Math.max(1, Math.round(YAHOO.util.Dom.getViewportWidth() / (this.previewSize.size * 1.3)));
    this.rowsPerPage = 5;
}

flexive.yui.datatable.ThumbnailView.prototype = {
    // return the columns of the thumbnail grid
    getColumns: function() {
        var columns = [];
        for (var i = 0; i < this.gridColumns; i++) {
            columns.push({ key: "c" + i, label: "" })
        }
        return columns;
    },

    getRows: function() {
        // transpose the linear result rows according to the grid size
        var grid = [];
        var currentRow = { "pks" : [] };    // the columns of the current row
        var currentColumn = 0;
        for (var i = 0; i < this.result.rowCount; i++) {
            var resultRow = this.result.rows[i];
            var data = "";
            if (resultRow.pk != null) {
                data = "<img src=\"" + flexive.util.getThumbnailURL(resultRow.pk, this.previewSize, true) + "\"/>";
            }
            /*if (resultRow["c1"] != null) {
                data += "<br/>" + resultRow["c1"];
            }*/
            if (currentColumn >= this.gridColumns) {
                // grid row completed
                grid.push(currentRow);
                currentRow = { "pks" : [] };
                currentColumn = 0;
            }
            // store column
            currentRow["c" + currentColumn] = data;
            currentRow.pks.push(resultRow.pk);
            currentColumn++;
        }
        if (currentColumn > 0) {
            grid.push(currentRow);
        }
        return grid;
    },

    getResponseSchema: function() {
        var fields = ["pks"];
        for (var i = 0; i < this.gridColumns; i++) {
            fields.push("c" + i);
        }
        return { "fields": fields };
    }
}


var fxValueInputList = [];   // a global list of all registered FxValueInput elements on the current page

var FxMultiLanguageValueInput = function(id, baseRowId, rowIds, languageSelectId) {
    this.id = id;
    this.baseRowId = baseRowId;
    this.rowIds = rowIds;
    this.languageSelectId = languageSelectId;
    fxValueInputList.push(this);
};

FxMultiLanguageValueInput.prototype = {
    onLanguageChanged: function(languageSelect) {
        this.showLanguage(languageSelect.options[languageSelect.selectedIndex].value);
        _fixHtmlEditorsIE();
    },

    showRow: function(rowId) {
        for (var i = 0; i < this.rowIds.length; i++) {
            var enabled = rowId == null || this.rowIds[i] == rowId;
            document.getElementById(this.rowIds[i]).style.display = enabled ? "block" : "none";
            // all languages / show language icon for each row
            document.getElementById(this.rowIds[i] + "_language").style.display = (enabled && rowId == null) ? "inline" : "none"; 
        }
    },

    showLanguage: function(languageId) {
        // show row for this language
        this.showRow(languageId >= 0 ? this.baseRowId + languageId : null);
        // update language select
        var options = document.getElementById(this.languageSelectId).options
        for (var i = 0; i < options.length; i++) {
            options[i].selected = (options[i].value == languageId);
        }
    },

    onDefaultLanguageChanged: function(inputCheckbox) {
        if (inputCheckbox.checked) {
            // uncheck other languages since only one language may be the default
            var languageCheckboxes = document.getElementsByName(inputCheckbox.name);
            for (var i = 0; i < languageCheckboxes.length; i++) {
                if (languageCheckboxes[i] != inputCheckbox) {
                    languageCheckboxes[i].checked = false;
                }
            }
        }
    },

    /**
     * Returns true if the input elements assigned to this object are still present.
     */
    isValid: function() {
        return document.getElementById(this.languageSelectId) != null;
    }
}

/**
 * Updates the current language of all multilanguage inputs on the current page.
 *
 * @param languageId    the language ID
 */
function setLanguageOnAllInputs(languageId) {
    var newList = [];
    for (var i = 0; i < fxValueInputList.length; i++) {
        if (fxValueInputList[i].isValid()) {
            fxValueInputList[i].showLanguage(languageId);
            newList.push(fxValueInputList[i]);
        }
    }
    fxValueInputList = newList;     // store new list without defunct inputs
    _fixHtmlEditorsIE();
}

// trigger tinyMCE repaint on IE
function _fixHtmlEditorsIE() {
    if (typeof(window.tinyMCE) != 'undefined' && tinyMCE.isIE) {
        for (var instanceName in tinyMCE.instances) {
            if (tinyMCE.isInstance(tinyMCE.instances[instanceName])) {
                tinyMCE.execInstanceCommand(instanceName, "mceCleanup");
            }
        }
    }
}


function openReferenceQueryPopup(xpath, updateInputId, formName) {
    var win = window.open(getBase() + "adm/search/browseReferencesPopup.jsf?xPath=" + xpath + "&inputName="
            + updateInputId + "&formName=" + formName,
            "searchReferences", "scrollbars=yes,width=800,height=600,toolbar=no,menubar=no,location=no");
    win.focus();
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
        // call dojo's show method (alias created by the DojoMenuWriter)
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
// ajax4jsf call:
//        document.getElementById(this.formName + ":" + this.name + "_refreshScreenviewButton").onclick();
// lytebox call:
        var a = document.createElement("a");
        a.href = getBase()+"thumbnail/pk"+id+"/so";
        a.rel = "lytebox";
        a.title = "Screenview "+id+"."+version;
        myLytebox.start( a, false, false);

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
            var link = node["link"];
            if (this.clickHandler) {
                this.clickHandler(node, false);
            } else if (link) {
                if (link.indexOf("javascript:") == 0) {
                    eval(link.substr("javascript:".length));
                } else {
                    // open link in other frame
                    parent.contentFrameObj.src = link;
                }
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

// initialize the TinyMCE HTML editor
function initHtmlEditor(autoPopulate) {
    try {
        //tinyMCE.baseURL = getBase() + "pub/js/tiny_mce";
        tinyMCE.srcMode = ""; // "_src" to enable original sourcefiles
        tinyMCE.init({
                mode: autoPopulate ? "specific_textareas" : "exact",
                dialog_type: "modal",
                editor_selector: "fxValueTextAreaHtml",
                theme: "advanced",
                plugins: "paste,fullscreen,inlinepopups",
                language: tinyMCE.guiLanguage ? tinyMCE.guiLanguage : "en",
                entity_encoding: "raw",     // don't replace special characters (like umlaut) with HTML entities

                // general layout options
                theme_advanced_layout_manager: "SimpleLayout",
                theme_advanced_toolbar_location : "top",
                theme_advanced_buttons1: "pastetext,pasteword,separator,undo,redo"
                    + ",separator,link,unlink,separator,bullist,justifyleft,justifycenter,justifyright,justifyfull,separator,code,fullscreen",
                theme_advanced_buttons2: "bold,italic,underline,strikethrough,sub,sup,separator,forecolor,fontsizeselect,removeformat",
                theme_advanced_buttons3: "",
                theme_advanced_toolbar_align: "left",
                theme_advanced_resizing : true,
                theme_advanced_statusbar_location: "bottom",

                // fix alert when switching between multiple editors
                focus_alert: false,
                strict_loading_mode: true   /* Fixes Firefox HTML encoding problem with multiple editors */
                /*width: "100%"*/
        });
    } catch (e) {
        alert("initHtml exception: " + e);
        // HTML editor component not configured
    }
}

