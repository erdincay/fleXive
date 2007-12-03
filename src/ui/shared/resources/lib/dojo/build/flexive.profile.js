/**
 * How to build a flexive DOJO package:
 *
 * - Download and unzip a recent DOJO build
 * - copy this file to buildscripts/profiles
 * - cd buildscripts
 * - ant -Dversion=0.4.1 -Dprofile=[flexive|flexiveNavigation] clean release zip
 * - copy the resulting release/dojo_0.4.1.zip file to dojo-[flexive|flexiveNavigation].zip in this directory.
 */
var dependencies = [
        "dojo.debug",
        "dojo.debug.console",
        "dojo.widget.Tooltip",
        "dojo.widget.FilteringTable",
        "dojo.widget.Button",
        "dojo.widget.Dialog",
        "dojo.widget.Menu2",
        "dojo.dnd.DragAndDrop",
        "dojo.dnd.HtmlDragAndDrop"
];

load("getDependencyList.js");
