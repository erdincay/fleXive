/**
 * A simple panel that renders the search queries (in the navigation bar).
 * The search queries are represented as JSON objects with the following properties:
 *
 *      name: the query name
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

var SearchQueryPanel = function(targetDiv, openQueryHandler) {
    this.items = [];
    this.targetDiv = targetDiv;
    this.openQueryHandler = openQueryHandler;
}

SearchQueryPanel.prototype = {
    reload: function() {
        try {
            this.items = eval("(" + getJsonRpc().SearchQueryEditor.renderSearchQueries() + ")");
            this.render();
        } catch (e) {
            alert(e);
        }
    },

    render: function() {
        var out = [];
        for (var i in this.items) {
            var item = this.items[i];
            var name = escapeQuotes(item.name);
            out.push("<a href=\"javascript:" + this.openQueryHandler + "('" + name + "')\">");
            out.push("<div class=\"searchQueryItem\" queryName=\"" + name + "\" title=\"" + name + "\">");
            out.push("<div class=\"searchQueryIcon\">");
            //out.push("<img border=\"0\" src=\"adm/images/queryIcons/savedQuery.png\"/>");
            out.push("</div>");
            out.push(item.name);
            out.push("</div></a>");
        }
        this.targetDiv.innerHTML = out.join("");
    }
};
