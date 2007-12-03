/*
 * Flexive RPC tree controller. Based on DOJO's RPC controller
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
*/

dojo.provide("flexive.widget.FxTreeRpcController");

dojo.require("dojo.widget.*");
dojo.require("dojo.widget.TreeCommon");
dojo.require("dojo.widget.TreeNodeV3");
dojo.require("dojo.widget.TreeV3");
dojo.require("dojo.widget.TreeBasicControllerV3");
dojo.require("dojo.widget.TreeLoadingControllerV3");
dojo.require("dojo.widget.TreeRpcControllerV3");

dojo.widget.defineWidget(
    "flexive.widget.FxTreeRpcController",
    [dojo.widget.TreeLoadingControllerV3, dojo.widget.TreeRpcControllerV3],
    function() {
        this.expandedNodes = [];
        this.liveMode = false;
        this.pathMode = false;  // show node paths instead of labels?
    }, {

    // TODO get context path

    ns: "flexive",
    widgetType: "FxTreeRpcController",
    rootNodeId: 1,

    methodMappings: {
        "getChildren": {
            name: "ContentTreeWriter.renderContentTree",
            params: ["kw.params.node.objectId", "kw.params.node.objectId == this.rootNodeId ? this.maxExpandedLevel : 2", "this.liveMode", "this.pathMode"]
        },
        "editLabelSave": {
            name: "ContentTreeEditor.saveLabel",
            params: ["kw.params.node.objectId", "kw.params.newContent", "this.liveMode", "this.pathMode"]
        },
        "reloadTree": {
            name: "ContentTreeWriter.renderContentTree",
            params: ["this.rootNodeId", "kw.params.controller.getMaxExpandedLevel()", "kw.params.controller.liveMode", "kw.params.controller.pathMode"]
        },
        "move": {
            name: "ContentTreeEditor.move",
            params: ["kw.params.child.objectId", "kw.params.newParent.objectId", "kw.params.newIndex", "this.liveMode"]
        },
        "destroyChild": {
            name: "ContentTreeEditor.remove",
            params: ["kw.params.node.objectId", "this.removeContent", "this.liveMode", "false"]
        }
    },
    listenNodeFilter: function(elem) { return elem instanceof dojo.widget.Widget}, 

    expand: function(node) {
        if (node.children.length > 0 ||
          (node.state == node.loadStates.UNCHECKED  && node.isFolder && !node.children.length)) {
            this.expandedNodes.push(node.widgetId);
            //dojo.debug("Expand --> " + this.expandedNodes);
        }
        return dojo.widget.TreeRpcControllerV3.prototype.expand.apply(this, arguments);
    },

    collapse: function(node) {
        if (node.children.length > 0) {
            // TODO: check why assignment to this.expandedNodes does not work
            // (thus the somewhat strange way of deleting an element)
            for (var i in this.expandedNodes) {
                if (this.expandedNodes[i] == node.widgetId) {
                    this.expandedNodes.splice(i, 1);
                    break;
                }
            }
        }
        return dojo.widget.TreeRpcControllerV3.prototype.collapse.apply(this, arguments);
    },

    getNodeLevel: function(node) {
        var depth = 0;
        while (node != null && node.parent != null) {
            depth++;
            node = node.parent;
        }
        return depth;
    },

    // get the maximum expanded level
    getMaxExpandedLevel: function() {
        var maxDepth = 1;
        for (var i in this.expandedNodes) {
            var depth = this.getNodeLevel(dojo.widget.byId(this.expandedNodes[i]));
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        return maxDepth;
    },

    // Expands all nodes in the internal expandedNodes array.
    // Used to restore the tree view after a full reload.
    expandOldView: function() {
        // copy to internal list - clear old nodes
        var nodes = ["node_" + this.rootNodeId];
        while (this.expandedNodes.length > 0) {
            nodes.push(this.expandedNodes.pop());
        }
       //alert("Restore: " + nodes);
        // "multi-pass" expand nodes because of lazy node initalization
        var somethingDone = false;
        var maxIterations = 1000;   // safety fallback if something breaks...
        var expandedNodes = {};
        do {
            somethingDone = false;
            for (var i in nodes) {
                var id = nodes[i];
                var node = dojo.widget.byId(id);
                if (node != null && !node.isExpanded && !expandedNodes[id]) {
                    //dojo.debug("Expanding: " + node + " with " + node.children + " children");
                    somethingDone = true;
                    expandedNodes[id] = true;
                    this.expand(node);
                }
            }
        } while (somethingDone && maxIterations-- > 0);
    },

    editLabelStart: function(node) {
        // use plain text label for editing
        node.labelNode.innerHTML = node.nodeText;
        return dojo.widget.TreeRpcControllerV3.prototype.editLabelStart.apply(this, arguments);
    },

    editLabelFinish: function(save, sync) {
        var node = this.editor.node;
        var result = dojo.widget.TreeRpcControllerV3.prototype.editLabelFinish.apply(this, arguments);
        if (!save) {
            // restore old label (with child count)
            node.labelNode.innerHTML = node.title;
        }
        return result;
    },

    // FIXME: dnd ceases to work when the root node children are replaced, so process them manually
    // possibly a dojo bug (0.4), so check this again for dojo 0.5
    fixDndAfterReload: function(node) {
        var refreshDnd = function(node) {
            node.tree.dndController.unlistenNode(node);
            node.tree.dndController.listenNode(node);
        }
        this.processDescendants(node, this.listenNodeFilter, refreshDnd);
    },

    reloadAndKeepExpanded: function() {
        var rootNode = dojo.widget.byId('node_' + this.rootNodeId);
        // save expanded level before the tree is collapsed
        this.maxExpandedLevel = this.getMaxExpandedLevel();
        if (!rootNode.tree.expand) {
            // DOJO allows to refresh the whole tree, but as of 0.4.1 fails to define
            // the expand method that gets called by the tree RPC controller
            //
            // By defining this dummy method, we allow the DOJO call to proceed successfully
            // and then our own expansion method gets called
            rootNode.tree.expand = function() { };
        }
        var deferred = this.refreshChildren(rootNode.tree, false);
        deferred.addCallback(dojo.lang.hitch(this, function() {
            this.maxExpandedLevel = null;
            this.expandOldView();
            this.fixDndAfterReload(rootNode);
        }));
    },

    _reloadTree: function() {
        var _this = this;
        var params = {
            controller: this
        };
        deferred = this.runRpc({
            url: this.getRpcUrl('reloadTree'),
            sync: false,
            params: params
        });

        deferred.addCallback(function(res) { return _this.loadProcessResponse(node,res) });
        return deferred;
    },


    // --------------- JSON/RPC wrapper ----------------------
    getResponseNodes: function(obj) {
        var response;
        eval("response = " + obj);
        var result = eval(response.result);
        if (dojo.lang.isArray(result) && result.length > 0) {
            if (result[0].objectId == this.rootNodeId) {
                return [result[0]];           // return root node
            } else if (result[0].children != null) {
                return result[0].children;    // return nodes
            } else if (result.length == 1) {
                // a single object wrapped in an array for correct JSON parsing
                return result[0];
            }
        }
        // don't know result - return object
        return result;
    },

    evaluateResponse: function(result) {
        return this.getResponseNodes(result);
    },

    // Override bind handler to evaluate and filter the JSON/RPC response
    getDeferredBindHandler: function(deferred) {
        return dojo.lang.hitch(this,
            function(type, obj){
                //dojo.debug("getDeferredBindHandler "+obj.toSource());
                obj = this.evaluateResponse(obj);
                var error = this.checkValidRpcResponse.apply(this, arguments);

                if (error) {
                    deferred.errback(error);
                    return;
                }
                deferred.callback(obj);
            }
        );
    },

    // Use POST instead of GET and use JSON-RPC-Java's request syntax
    runRpc: function(kw) {
        var _this = this;
        var deferred = new dojo.Deferred();
        var action = kw.url.substring(kw.url.lastIndexOf('=') + 1);
        var mapping = this.methodMappings[action];
        if (mapping == null) {
            alert("Action not mapped: " + action);
        }
        //kw.params.maxExpandedLevel = this.maxExpandedLevel;
        // evaluate params
        var params = [];
        for (var i = 0; i < mapping.params.length; i++) {
            var value = eval(mapping.params[i]);
            if (typeof value == "string") {
                value = "'" + value + "'";
            }
            params.push(value);
        }
        dojo.io.bind({
            url: kw.url,
            method: "post",
            handle: this.getDeferredBindHandler(deferred),
            mimetype: "text/plain",
            preventCache: this.preventCache,
            sync: kw.sync,
            postContent: "{method:'" + mapping.name + "', params: [" + params.join(",") + "]}"
        });

        return deferred;
    }
});
