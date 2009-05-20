/**
 * A simple panel that renders a briefcase selection (in the navigation bar).
 * The briefcases are represented as JSON objects with the following properties:
 *
 *      name: the full briefcase name
 *      id: the briefcase id
 *      size: the number of items in the briefcase
 *      aclId: the ACL assigned to the briefcase (-1 for private briefcases)
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

var BriefcasePanel = function(targetDiv, showBriefcaseHandler) {
    this.items = [];
    this.targetDiv = targetDiv;
    this.showBriefcaseHandler = showBriefcaseHandler;
}

BriefcasePanel.prototype = {
    reload: function() {
        try {
            this.items = eval("(" + flexive.util.getJsonRpc().BriefcaseEditor.renderBriefcases() + ")");
            this.render();
        } catch (e) {
            alertDialog(e);
        }
    },

    render: function() {
        var out = [];
        for (var i in this.items) {
            var item = this.items[i];
            var linkId = "bc_i" + item.id;
            out.push("<a id=\"" + linkId + "\" href=\"javascript:" + this.showBriefcaseHandler + "(" + item.id + ")\" class=\"briefcaseItem\">");
            var escapedName = flexive.util.escapeQuotes(item.name);
            out.push("<div briefcaseId=\"" + item.id + "\" briefcaseName=\"" + escapedName 
                    + "\" title=\"" + escapedName + " (" + item.size + ")\">");
            out.push("<div class=\"briefcaseIcon\">");
            if (item.aclId != -1) {
                // render shared icon overlay - add onclick handler for IE7
                out.push("<img border=\"0\" src=\"adm/images/briefcaseIcons/normal_shared.gif\""
                        + " onclick=\"document.getElementById('" + linkId + "').click()\"/>");
            }
            out.push("</div>");
            var sizeText = item.size >= 1000 ? "<strong>" + (parseInt(item.size / 1000)) + "k</strong>" : item.size;
            out.push("<div class=\"briefcaseSize\">" + sizeText + "</div>");
            out.push(item.name);
            out.push("</div>");
            out.push("</a>");
        }
        this.targetDiv.innerHTML = out.join("");
    }
};
