/**
 * Plugin to execute the ctrl-enter keyboard shortcut to run scripts
 */
var EditArea_executeScript = {
    /**
     * Get called once this file is loaded (editArea still not initialized)
     *
     * @return nothing
     */
    init: function() {
        //	nothing to be done here
    }

    /**
     * Is called each time the user touch a keyboard key.
     *
     * @param (event) e: the keydown event
     * @return true - pass to next handler in chain, false - stop chain execution
     * @type boolean
     */
    ,onkeydown: function(e) {
        if (this._ctrlPressed(e) && e.keyCode == 13) {
            this.execCommand("captureKeyStroke");
            return false;
        }
        return true;
    }

    /**
     * Executes a specific command, this function handles plugin commands.
     *
     * @param {string} cmd: the name of the command being executed
     * @param {unknown} param: the parameter of the command
     * @return true - pass to next handler in chain, false - stop chain execution
     * @type boolean
     */
    ,execCommand: function(cmd, param) {
        if (cmd == "captureKeyStroke") {
            // copy contents first (somehow the onchange functionality doesn't work from here)
            var command;
            command = "parent.document.getElementById('" + editArea.id + "').value = parent.editAreaLoader.getValue('" + editArea.id +"');";
            eval(command);
            command = "parent.document.getElementById('" + parent.targetElementId + "')." + parent.targetElementAction + "();";
            eval(command);
        }
        return true;
    }

    /**
     * internal function: return true if Ctrl key is pressed
     * @param e the keydown event
     */
    ,_ctrlPressed : function(e) {
        if (window.event) {
            return (window.event.ctrlKey);
        } else {
            return (e.ctrlKey || (e.modifiers == 2) || (e.modifiers == 3) || (e.modifiers > 5));
        }
    }
};

// Adds the plugin class to the list of available EditArea plugins
editArea.add_plugin("executeScript", EditArea_executeScript);
