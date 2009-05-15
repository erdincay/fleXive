package com.flexive.example.component;

import com.flexive.faces.plugin.PluginFactory;
import com.flexive.faces.plugin.AdmExtensionPoints;
import com.flexive.faces.plugin.TreePluginExecutor;
import com.flexive.faces.plugin.Plugin;
import com.flexive.faces.beans.PluginRegistryBean;
import com.flexive.faces.javascript.tree.TreeNodeWriter;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class ExamplePluginFactory implements PluginFactory {

    private static class NavigationMenuPlugin implements Plugin<TreePluginExecutor> {
        public void apply(TreePluginExecutor executor) {
            executor.addNode(null,
                    new TreeNodeWriter.Node(
                            "exampleComponentPlugin",
                            "Example component plugin",
                            null,
                            null,
                            "javascript:alertDialog('Example component plugin for the backend application.')"
                    )
            );
        }
    }

    public void initialize(PluginRegistryBean registry) {
        registry.registerPlugin(
                AdmExtensionPoints.ADM_MAIN_NAVIGATION,
                new NavigationMenuPlugin()
        );

    }
}
