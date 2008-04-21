/*
   Generic selection routines for selecting HTML elements. Usually table rows or table cells,
   but it basically works with any uniform collection of HTML elements.

   Also supports shift- and ctrl-selection, possibly across page boundaries.

   by Daniel Lichtenberger, UCS
   $Rev$

   Usage notes:


   1. Render the current view of "rows". A row may be a HTML tr element, or any other container
      around a single item (e.g. a div).
      The row must have additional attributes in order to be recognized as "selectable".
      The attribute names are passed to the RowSelectionContainer constructor.

        "positionId": the id uniquely identifying a single item.
            Note: a id CAN be used in several rows on a page, but those rows will
            be grouped for selections (i.e. either all rows sharing a single ID are selected, or none.)
        "rowNum": Consecutively numbered row number (0..numRows-1).
        "colorSet" (hardcoded): index number in the colorSets array passed to the constructor,
            determining the background colors of the row. Example format: see below.
        "id": contains unique HTML element id, must be "row<rowNum>", e.g. "row5" for the 6th row.

     Additionally, you have to specify onclick/onmouseover events:
        "onclick": in our example calls rowSelection.onRowClicked(event)
        "onmouseover": in our example calls rowSelection.onMouseOver(event)
        "onmouseout": in our example calls rowSelection.onMouseOut(event)

   2. Declare and initialize a RowSelectionContainer in your JSP page:

        var rowSelection = new RowSelectionContainer("positionId", "rowNum", colorSets);
        rowSelection.rowOffset = [currentRow];
        rowSelection.numRows = [rows per page];
        rowSelection.selectedRow = [selected row, or -1 if none is selected];
        rowSelection.onMultiPageSelect = [js handler for shift-selects across page boundaries];

   3. On a page using server-side selection, initialize current selection
      by adding all selected IDs to RowSelectionContainer.selectedItems, e.g.:

        rowSelection.selectedItems["1234"] = true;

      Be careful to use string values as keys.

   4. Initialize selection. Updates the rendered rows according to the current selection.
      You have to call this method to ensure proper initialization.

        rowSelection.init();

*/

// sample colorset:
/*
// ------------- color themes for item selection -------------
// order of items: normal, normal[mo], selected, selected[mo]
// mo ... mouseover
var colorSet1 = new Array("#FFFFFF", "#D8DEE6", "#C0D2EC", "#B2C4DF");   // list view - even rows
var colorSet2 = new Array("#F5F5F5", "#D8DEE6", "#C0D2EC", "#B2C4DF");   // list view - odd rows

var colorSets = new Array(colorSet1, colorSet2);
*/

function RowSelectionContainer(fieldId, fieldRowNum, colorSets, dndEnabled) {
    // public function "delegates"
    this.onMultiPageSelect = null;  // function accepting a single integer parameter
                                    // that selects all rows between this.selectedRow and the
                                    // given row number (shift-select across page boundaries).
    this.onUpdateRowColor = null;   // additional (optional) user handler for updating row colors
                                    // prototype: onUpdateRowColor(rowNum, rowElement, selected, mouseover, color)
    // data fields
    this.fieldId = fieldId;               // name of the ID field containing the element ID of a row
    this.fieldRowNum = fieldRowNum;       // name of the field containing the row number
    this.colorSets = colorSets;
    this.dndEnabled = dndEnabled;         // drag'n'drop enabled? (via Dojo)
    this.rowOffset = 0;                   // offset of the first displayed row
    this.numRows = 25;                    // number of (displayed) rows
    this.keepRowBorder = false;           // if true, the original HTML borders are restored when unselecting an item

    this.selectedItems = new Object();    // associative array containing all selected IDs
    this.selectedRow = -1;                // number of currently selected row

    // private data fields
    this._ids2rows = new Object();         // associative array containing all rows for a given id
}


/* Event handler called when a position (row) was clicked */
RowSelectionContainer.prototype.onRowClicked = function(event) {
    if (event == null) {
        event = window.event;
    }
    var element = findParentWithAttribute(event, this.fieldId);
    var id = "" + element.getAttribute(this.fieldId);
    var rowNum = parseInt(element.getAttribute(this.fieldRowNum));

    if (!event.ctrlKey && !event.metaKey) {
        this.clear();
    }
    if (this.selectedRow != -1) {
        this._createBorder(this.selectedRow - this.rowOffset, false);
    }

    if (event.shiftKey) {
        // shift select
        if (this.selectedRow == -1) // if no row is selected, use first displayed row as default
            this.selectedRow = this.rowOffset;
        if (this.selectedRow < this.rowOffset || this.selectedRow > this.rowOffset + this.numRows) {
            this.onMultiPageSelect(this.rowOffset + rowNum);
            return;
        } else {
            // shift-select "inside" a page
            var index = this.selectedRow - this.rowOffset;
            this.selectedRow = rowNum + this.rowOffset; // update selected row now for correct border display
            for (var incr = rowNum < index ? -1 : 1; index != rowNum; index += incr) {
                this.selectedItems["" + this._getRow(index).getAttribute(this.fieldId)] = true;
                this._updateRowColor(index, true);
            }
        }
    } else if (event.ctrlKey || event.metaKey) {
        // toggle current row
        this.selectedItems[id] = !this.selectedItems[id];
    }

    // select this row
    // TODO use this.selectRow
    if (!event.ctrlKey && !event.metaKey)
        this.selectedItems[id] = true;
    this.selectedRow = rowNum + this.rowOffset;
    this._updateRowColor(rowNum, this.selectedItems[id]);
    this._createBorder(rowNum, true);
}

/* To be called after the table containing all rows has been rendered.
   Initializes all selected rows. */
RowSelectionContainer.prototype.init = function() {
    var i;
    var ctr = 0;
    for (i = 0; i < this.numRows; i++) {
        var row = this._getRow(i);
        if (row == null)
            continue;
        ctr++;
        var id = row.getAttribute(this.fieldId);
        if (this._ids2rows[id] == null) {
            this._ids2rows[id] = new Array();
        }
        this._ids2rows[id].push(row);
        this._updateRowColor(i, this.selectedItems[id]);
        if (this.selectedRow == i + this.rowOffset)
            this._createBorder(i, true);
        if (this.dndEnabled) {
            // this only works inside the content frame
            new dojo.dnd.HtmlDragSource(row, "*");
        }
    }
}

/* Updates the currently shown selection for all rows */
RowSelectionContainer.prototype.update = function() {
    var i;
    for (i = 0; i < this.numRows; i++) {
        var row = this._getRow(i);
        if (row == null)
            continue;
        var id = row.getAttribute(this.fieldId);
        this._updateRowColor(i, this.selectedItems[id]);
        if (this.selectedRow == i + this.rowOffset)
            this._createBorder(i, true);
    }
}

/* Optional mouseover event handler for a single item/row. */
RowSelectionContainer.prototype.onMouseOver = function(event) {
    this._handleMouseOver(event, true);
}

/* Optional mouseout event handler for a single item/row. */
RowSelectionContainer.prototype.onMouseOut = function(event) {
    this._handleMouseOver(event, false);
}

/* Returns a comma-separated string containing all selected IDs. */
RowSelectionContainer.prototype.asString = function() {
    this.getSelected().join(",");
}

/* Returns an array of all selected IDs. */
RowSelectionContainer.prototype.getSelected = function() {
    var selected = new Array();
    for (var id in this.selectedItems) {
        if (this.selectedItems[id]) selected.push(id);
    }
    return selected;
}

/* Selects a row, does not clear selection before */
RowSelectionContainer.prototype.selectRow = function(rowNum) {
    var row = this._getRow(rowNum);
    if (row == null) return;
    if (this.selectedRow != -1) {
        this._createBorder(this.selectedRow - this.rowOffset, false);
    }
    var id = row.getAttribute(this.fieldId);
    this.selectedItems[id] = true;
    this.selectedRow = rowNum + this.rowOffset;
    this._updateRowColor(rowNum, true);
    this._createBorder(rowNum, true);
}

/* Clear selection */
RowSelectionContainer.prototype.clear = function() {
    var i;
    this.selectedItems = new Object();
    for (i = 0; i < this.numRows; i++) {
        this._updateRowColor(i, false);
    }
}

/* Returns length of selection */
RowSelectionContainer.prototype.length = function() {
    var ctr = 0;
    for (var id in this.selectedItems)
        if (this.selectedItems[id]) ctr++;
    return ctr;
}


// ----------------------- private functions --------------------

RowSelectionContainer.prototype._handleMouseOver = function(event, mo) {
    var element = findParentWithAttribute(event, this.fieldId);
    var id = "" + element.getAttribute(this.fieldId);
    var rowNum = parseInt(element.getAttribute(this.fieldRowNum));
    this._updateRowColor(rowNum, this.selectedItems[id], mo);
}

RowSelectionContainer.prototype._getActiveCellColor = function(cell, active, mouseover) {
    var cs = cell.getAttribute("colorSet");
    if (cs == null) return "";
    var mo = mouseover ? 1 : 0;
    return active ? this.colorSets[cs][2 + mo] : this.colorSets[cs][0 + mo];
}

/* Selects or deselects the given position row*/
RowSelectionContainer.prototype._updateRowColor = function(rowNum, selected, mouseover) {
    if (this._getRow(rowNum) == null) return;
    // get all affected rows - items may be split over several rows
    var rows = this._getItemRows(this._getRow(rowNum).getAttribute(this.fieldId));
    var i;
    for (i = 0; i < rows.length; i++) {
        var element = rows[i];
        var elementRowNum = parseInt(element.getAttribute(this.fieldRowNum));
        var color = this._getActiveCellColor(element, selected, mouseover);
        element.style.backgroundColor = color;
        this._createBorder(elementRowNum, (elementRowNum + this.rowOffset) == this.selectedRow);
//        this._createBorder(elementRowNum, selected);
        if (this.onUpdateRowColor)
            this.onUpdateRowColor(elementRowNum, element, selected, mouseover, color);
        this._setChildImageOpacity(element, selected || mouseover ? 0.8 : 1.0);
    }
}

// recursively update the opacity of every child image for the given element
RowSelectionContainer.prototype._setChildImageOpacity = function(element, opacity) {
    if (element.nodeName == "IMG") {
        element.style.opacity = opacity;
        return;
    }
    for (var i = 0; i < element.childNodes.length; i++) {
        this._setChildImageOpacity(element.childNodes[i], opacity);
    }
}

RowSelectionContainer.prototype._getRow = function(rowNum) {
    return document.getElementById("row" + rowNum);
}

// get all rows associated with the given item
RowSelectionContainer.prototype._getItemRows = function(id) {
    return this._ids2rows[id] != null ? this._ids2rows[id] : new Array();
}

RowSelectionContainer.prototype._createBorder = function(rowNum, selected) {
    var i;
    var row = this._getRow(rowNum);
    if (row == null) return;
    //var color = selected ? "#555555" : getActiveCellColor(row, oidsRev[row.getAttribute("oid")] != null, false);
    var color = selected ? "#66676A" :
        this._getActiveCellColor(row, this.selectedItems[row.getAttribute(this.fieldId)], false);
    var style = color + " 1px solid";
    if (row.nodeName == "TR") {
        for (i = 0; i < row.childNodes.length; i++) {   // make border around each child
            if (!row.childNodes[i].style)
                continue;
            row.childNodes[i].style.borderTop = row.childNodes[i].style.borderBottom = style;
            if (i == 0) row.childNodes[i].style.borderLeft = style;
            if (i == row.childNodes.length - 1) row.childNodes[i].style.borderRight = style;
        }
    } else { // not a table row, set border around element
        if (this.keepRowBorder && !selected) {
            // if an old border style is available, restore it - otherwise this method is
            // called for the first time, thus the border should be left untouched
            style = row.getAttribute("border_old") != null ? row.getAttribute("border_old") : null;
        }
        if (row.getAttribute("border_old") == null) {
            row.setAttribute("border_old", row.style.border);
        }
        if (style != null && (!document.all || style.length > 0)) {     // See FX-172 for the second clause
            row.style.border = style;
        }
    }
}

