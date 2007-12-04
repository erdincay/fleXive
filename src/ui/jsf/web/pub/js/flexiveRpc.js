/**
 * JSON/RPC methods for querying flexive APIs.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

var JSON_RPC_CLIENT = null;
var getJsonRpc = function() {
    if (JSON_RPC_CLIENT == null) {
        // TODO retrieve context path
        JSON_RPC_CLIENT = new JSONRpcClient("/flexive/adm/JSON-RPC");
    }
    return JSON_RPC_CLIENT;
}
