// Javascript methods for the JSON-RPC-based resultView template
// @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
// @version $Rev$

var ResultView = function(id, formName, tableWidget, dataScroller, rowSelection, viewType) {
    this.id = id;
    this.formName = formName;
    this.tableWidget = tableWidget;
    this.dataScroller = dataScroller;
    this.rowSelection = rowSelection;
    this.viewType = viewType;
    this.sortIndex = -1;
    this.sortAscending = true;
}

ResultView.prototype = {
    // Render the result rows contained in response, update the table columns and total row counts
    renderResultCallback: function(response, exception) {
        if (exception != null) {
            alert(exception.message);
            return;
        }
        var result = eval("(" + response + ")");
        var dataRows = result.rows;
        this.tableWidget.fxTableHandler.addActionLinks(dataRows);

        // update columns
        // remember column for the actionlinks in list mode
        var actionColumn = this.viewType == "LIST" ? this.tableWidget.columns[this.tableWidget.columns.length - 1] : null;
        this.tableWidget.columns = [];
        for (var i = 0; i < result.columns.length; i++) {
            this.tableWidget.columns.push(this.tableWidget.createMetaData(result.columns[i]));
        }
        if (actionColumn != null) {
            // add action column again (in list mode)
            this.tableWidget.columns.push(actionColumn);
        }

        // clear table
        var table = this.tableWidget.domNode;
        table.deleteTHead();
        while (table.rows.length > 0) {
            table.deleteRow(0);
        }
        table.createTHead();
        if (this.viewType == "THUMBNAILS") {
            // insert an empty row to prevent dojo from rendering the table header
            table.tHead.insertRow(0);
        }
        this.tableWidget.init();

        // update rows
        this.tableWidget.store.setData(dataRows);

        // update result properties
        this.dataScroller.totalRows = result.totalRows;
        this.dataScroller.fetchRows = result.fetchRows;
        this.dataScroller.updateDisplay();
        this.rowSelection.numRows = result.fetchRows;
        this.rowSelection.init();

        // update type content counts
        var options = document.getElementById(this.formName + ":" + this.id + "_typeId").options;
        var typeCounts = result.typeCounts;
        for (var i = 0; i < options.length; i++) {
            var option = options[i];
            if (option.value == -1) {
                continue;
            }
            if (typeCounts[option.value] == null) {
                options[i] = null;
                i--;
                continue;
            }
            option.text = option.text.substring(0, option.text.lastIndexOf("("))
                + "(" + typeCounts[option.value] + ")";
        }
    }
};