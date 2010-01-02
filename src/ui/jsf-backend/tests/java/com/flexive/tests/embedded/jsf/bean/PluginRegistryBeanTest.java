/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.tests.embedded.jsf.bean;

import com.flexive.faces.beans.PluginRegistryBean;
import com.flexive.faces.plugin.ExtensionPoint;
import com.flexive.faces.plugin.Plugin;
import com.flexive.faces.plugin.PluginExecutor;
import com.flexive.shared.exceptions.FxRuntimeException;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Stack;

/**
 * Plugin registry tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class PluginRegistryBeanTest {
    /**
     * A primitive PluginExecutor that collects plugin results in a stack.
     */
    private static interface TestExecutor extends PluginExecutor {
        /**
         * Push a result to the executor.
         *
         * @param result    the result to be added
         */
        void pushResult(int result);
    }

    /**
     * A "NOP-Executor" that simply won't take any commands.
     */
    private static interface NullExecutor extends PluginExecutor {
    }

    /**
     * TestExecutor implementation that stores the results in a stack.
     */
    private static class TestExecutorImpl implements TestExecutor {
        private final Stack<Integer> results = new Stack<Integer>();

        public void pushResult(int result) {
            results.push(result);
        }

        public int popResult() {
            return results.isEmpty() ? -1 : results.pop();
        }
    }

    /**
     * A demo plugin that always pushes "42" to the TestExecutor
     */
    private static class TestPlugin implements Plugin<TestExecutor> {
        public void apply(TestExecutor executor) {
            executor.pushResult(42);
        }
    }

    /**
     * A demo plugin that always pushes "21" to the TestExecutor
     */
    private static class TestPluginCallback2 implements Plugin<TestExecutor> {
        public void apply(TestExecutor executor) {
            executor.pushResult(21);
        }
    }

    /**
     * Our test extension point instance
     */
    private static class NamedTestExtension extends ExtensionPoint<TestExecutor> {
        public NamedTestExtension(String name) {
            super(name);
        }
    }

    /**
     * A generic extension point without a bound type parameter (shouldn't work
     * with the current PluginRegistry implementation). 
     */
    private static class UnboundExtension<P extends PluginExecutor> extends ExtensionPoint<P> {
        public UnboundExtension(String name) {
            super(name);
        }
    }

    private static NamedTestExtension EP = new NamedTestExtension("testExtension");
    private PluginRegistryBean pluginRegistry = new PluginRegistryBean();

    @Test(groups = "jsf")
    public void registerTestPlugin() {
        pluginRegistry.clearPlugins(EP);
        pluginRegistry.registerPlugin(EP, new TestPlugin());
        // register another plugin for another extension point of the same class
        pluginRegistry.registerPlugin(new ExtensionPoint<TestExecutor>("bla") {
        }, new TestPlugin());
        assertEquals(pluginRegistry.getPlugins(EP).size(), 1);
        final Plugin<TestExecutor> callback = pluginRegistry.getPlugins(EP).get(0);
        final TestExecutorImpl exec = new TestExecutorImpl();
        callback.apply(exec);
        assertEquals(exec.popResult(), 42);
    }

    @Test(groups = "jsf")
    public void heteroPluginList() {
        pluginRegistry.clearPlugins(EP);
        // register two callbacks of different classes
        pluginRegistry.registerPlugin(EP, new TestPlugin());
        // also test that lookup is implemented by name comparison, not be object/class identity
        pluginRegistry.registerPlugin(new ExtensionPoint<TestExecutor>("testExtension") {
        }, new TestPluginCallback2());
        assertEquals(pluginRegistry.getPlugins(EP).size(), 2);

        // execute plugins
        final TestExecutorImpl exec = new TestExecutorImpl();
        pluginRegistry.getPlugins(EP).get(0).apply(exec);
        pluginRegistry.getPlugins(new NamedTestExtension("testExtension")).get(1).apply(exec);
        checkHeteroResults(exec);

        // execute plugins via registry's execute method
        pluginRegistry.execute(EP, exec);
        checkHeteroResults(exec);
        pluginRegistry.execute(new NamedTestExtension("testExtension"), exec);
        checkHeteroResults(exec);
    }

    private void checkHeteroResults(TestExecutorImpl exec) {
        // check results
        assertEquals(exec.popResult(), 21);
        assertEquals(exec.popResult(), 42);
    }

    @Test(groups = "jsf")
    public void incompatiblePluginList() {
        pluginRegistry.clearPlugins(EP);
        pluginRegistry.registerPlugin(EP, new TestPlugin());
        try {
            pluginRegistry.registerPlugin(new ExtensionPoint<NullExecutor>("testExtension") {},
                    new Plugin<NullExecutor>() {
                        public void apply(NullExecutor executor) {
                        }
                    });
            assertEquals(pluginRegistry.getPlugins(EP).size(), 2);
            Assert.fail( "Plugin registry allowed to register incompatible types.");
        } catch (FxRuntimeException e) {
            // pass
            assertEquals(pluginRegistry.getPlugins(EP).size(), 1);
        }

    }

    @Test(groups = "jsf")
    public void unboundExecutorTest() {
        // due to generic handling it is not safe 
        // to use extension points without statically bound type variables
        try {
            pluginRegistry.registerPlugin(new UnboundExtension<NullExecutor>("bla"), new Plugin<NullExecutor>() {
                public void apply(NullExecutor executor) {
                }
            });
            Assert.fail( "Unbound type variables should be rejected");
        } catch (FxRuntimeException e) {
            // pass
        }
    }
}
