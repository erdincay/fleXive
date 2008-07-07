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

    /**
     * Parses the given PK and returns a PK object.
     *
     * @param pk     a primary key (e.g. "125.1")
     * @returns      the parsed PK, e.g. { id: 125, version: 1 }
     */
    this.parsePk = function(/* String */ pk) {
        if (pk.indexOf(".") == -1) {
            throw "Not a valid PK: " + pk;
        }
        return { id: parseInt(pk.substr(0, pk.indexOf("."))),
                 version: parseInt(pk.substr(pk.indexOf(".") + 1)) };
    }
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

    /**
     * Updates a property of a menu item.
     *
     * @param id    the menu item ID
     * @param property  the property (e.g. "disabled")
     * @param value the new value
     */
    this.setMenuItem = function(/* String */ id, /* String */ property, value) {
        var item = YAHOO.widget.MenuManager.getMenuItem(id);
        if (item == null) {
            alert("Menu item not found: " + id);
            return;
        }
        item.cfg.setProperty(property, value);
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

    /**
     * <p>Return the primary key of the result table row/column of the given element (e.g. an event target).
     * Supports both list and thumbnail view.</p>
     *
     * <p>Note that the primary key is only available if it was selected in the FxSQL query (@pk).</p>
     *
     * @param dataTable the datatable variable (set with the 'var' attribute of fx:resultTable)
     * @param element   the table element (i.e. an element nested in a table cell/row)
     * @return  a PK object, e.g. {id: 21, version: 10}
     */
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
            return flexive.util.parsePk(data.pk);
        } else {
            // one PK per column, get column index and return PK
            var elCol = dataTable.getTdEl(element);
            var col = dataTable.getColumn(elCol);
            return flexive.util.parsePk(data.pks[col.getKeyIndex()]);
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


flexive.input = new function() {
    this.fxValueInputList = [];   // a global list of all registered FxValueInput elements on the current page

    /**
     * Updates the current language of all multilanguage inputs on the current page.
     *
     * @param languageId    the language ID
     */
    this.setLanguageOnAllInputs = function(languageId) {
        var newList = [];
        var oldList = flexive.input.fxValueInputList;
        for (var i = 0; i < oldList.length; i++) {
            if (oldList[i].isValid()) {
                oldList[i].showLanguage(languageId);
                newList.push(oldList[i]);
            }
        }
        flexive.input.fxValueInputList = newList;     // store new list without defunct inputs
        flexive.input._fixHtmlEditorsIE();
    }

    // trigger tinyMCE repaint on IE
    this._fixHtmlEditorsIE = function() {
        if (typeof(window.tinyMCE) != 'undefined' && tinyMCE.isIE) {
            for (var instanceName in tinyMCE.instances) {
                if (tinyMCE.isInstance(tinyMCE.instances[instanceName])) {
                    tinyMCE.execInstanceCommand(instanceName, "mceCleanup");
                }
            }
        }
    }

    // initialize the TinyMCE HTML editor
    this.initHtmlEditor = function(autoPopulate) {
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

    // TODO: implement for usage outside the backend
    this.openReferenceQueryPopup = function(xpath, updateInputId, formName) {
        var win = window.open(getBase() + "adm/search/browseReferencesPopup.jsf?xPath=" + xpath + "&inputName="
                + updateInputId + "&formName=" + formName,
                "searchReferences", "scrollbars=yes,width=800,height=600,toolbar=no,menubar=no,location=no");
        win.focus();
    }

}

flexive.input.FxMultiLanguageValueInput = function(id, baseRowId, rowIds, languageSelectId) {
    this.id = id;
    this.baseRowId = baseRowId;
    this.rowIds = rowIds;
    this.languageSelectId = languageSelectId;
    flexive.input.fxValueInputList.push(this);
};

flexive.input.FxMultiLanguageValueInput.prototype = {
    onLanguageChanged: function(languageSelect) {
        this.showLanguage(languageSelect.options[languageSelect.selectedIndex].value);
        flexive.input._fixHtmlEditorsIE();
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

flexive.dojo = new function() {
    /** Creates a DOJO menu from a JSON object. Based on Dojo example code. */
    this.makeMenu = function(id, menuClass, itemClass, items, isTop, contextMenuTarget) {
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
                var submenu = flexive.dojo.makeMenu(null, menuClass, itemClass, itemJson.submenu, false);
                itemJson.submenuId = submenu.widgetId;
            }
            var item = dojo.widget.createWidget(itemJson.createAsType == null ? itemClass : itemJson.createAsType,  itemJson);
            menu2.addChild(item);
        });
        return menu2;
    }
}
