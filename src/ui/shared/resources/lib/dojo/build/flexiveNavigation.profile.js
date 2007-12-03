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
        "dojo.widget.TreeV3",
        "dojo.widget.TreeRpcControllerV3",
        "dojo.widget.TreeSelectorV3",
        "dojo.widget.TreeNodeV3",
        "dojo.widget.TreeContextMenuV3",
        "dojo.widget.TreeLinkExtension",
        "dojo.widget.TreeBasicControllerV3",
        "dojo.widget.TreeEmphasizeOnSelect",
        "dojo.widget.TreeDndControllerV3",
        "dojo.widget.TreeDragAndDropV3",
        "dojo.widget.HtmlDragAndDrop",
        "dojo.widget.HtmlDragManager",
        "dojo.widget.DragAndDrop",
        "dojo.widget.TreeEditor",
        "dojo.widget.RichText",
        "dojo.widget.TreeDocIconExtension",
        "dojo.widget.Button"
];

load("getDependencyList.js");
