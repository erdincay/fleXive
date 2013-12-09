/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
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
package com.flexive.faces.plugin;

/**
 * <p>
 * The base interface for flexive plugins. A plugin acts as a singleton that is attached
 * to one (or more) {@link ExtensionPoint ExtensionPoints}. The extension point is parameterized
 * with a concrete interface or implementation of a {@link PluginExecutor}. When some code
 * wants to use the plugins registered with an ExtensionPoint, it creates a PluginExecutor
 * matching the ExtensionPoint's type parameter, and calls each registered plugin passing its
 * PluginExecutor.
 * </p>
 * <p>
 * A basic example, taken from the plugin test cases:
 * <pre>
 * // PluginExecutor interface
 * private static interface TestExecutor extends PluginExecutor {
 *     void pushResult(int result);
 * }
 * 
 * // TestExecutor implementation
 * private static class TestExecutorImpl implements TestExecutor {
 *     private final Stack&lt;Integer> results = new Stack&lt;Integer>();
 *
 *     public void pushResult(int result) {
 *         results.push(result);
 *     }
 *
 *     public int popResult() {
 *         return results.isEmpty() ? -1 : results.pop();
 *     }
 * }
 *
 * // An extension point
 * public static final ExtensionPoint&lt;TestExecutor> EXTENSIONPOINT =
 *     new ExtensionPoint&lt;TestExecutor>("Unique extension point name") { }</b>;
 *
 * // A sample plugin, provided by the plugin developer
 * private static class TestPlugin implements Plugin&lt;TestExecutor> {
 *     public void apply(TestExecutor executor) {
 *         executor.pushResult(42);
 *     }
 * }
 * </pre>
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface Plugin<PEX extends PluginExecutor> {
    /**
     * Execute the plugin callback on the given extension point.
     *
     * @param executor the executor that the plugin is applied to
     */
    void apply(PEX executor);
}
