dojo.provide("flexive.manifest");
dojo.registerNamespace("flexive", "flexive.widget", function(name) {
    var map = {
        "flexive.widget.fxtreerpccontroller": "FxTreeRpcController"
    }
    alertDialog("Resolve: " + "flexive.widget."+map[name]);
    return "flexive.widget."+map[name];
});
