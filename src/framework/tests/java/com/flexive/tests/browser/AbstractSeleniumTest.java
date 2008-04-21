/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.tests.browser;

import com.flexive.shared.FxSharedUtils;
import com.thoughtworks.selenium.HttpCommandProcessor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Base class for selenium browser tests. Note that a selenium server must
 * be started externally. The server address and port can be set with the
 * properties
 * <code>tests.browser.selenium.host</code> and
 * <code>tests.browser.selenium.port</code> in the <code>browser.properties</code> file.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = "browser")
public abstract class AbstractSeleniumTest {
    private static final transient Log LOG = LogFactory.getLog(AbstractSeleniumTest.class);

    /**
     * Property for setting the selenium server port
     */
    public static final String PROP_PORT = "tests.browser.selenium.port";
    /**
     * Property for setting the selenium server host IP
     */
    public static final String PROP_HOST = "tests.browser.selenium.host";
    /**
     * The base URL for all tests
     */
    public static final String PROP_BASEURL = "tests.browser.selenium.baseUrl";

    protected String initialUrl;
    protected final String configName;
    protected final Properties defaultProperties;
    protected FlexiveSelenium selenium;
    protected HttpCommandProcessor commandProcessor;

    /**
     * Create a browser test for the given initial URL.
     *
     * @param initialUrl The initial URL of the browser. All subsequent open(..) requests will be appended to this URL.
     */
    protected AbstractSeleniumTest(String initialUrl) {
        this(initialUrl, null, null);
    }

    /**
     * Creates a browser test for the given URL and default properties.
     *
     * @param initialUrl        The initial URL of the browser. All subsequent open(..) requests will be appended to this URL.
     * @param defaultProperties Default properties to be used if browser.properties is not available.
     */
    protected AbstractSeleniumTest(String initialUrl, Properties defaultProperties) {
        this(initialUrl, null, defaultProperties);
    }

    /**
     * Creates a browser test for the given URL and the given properties file name.
     *
     * @param initialUrl The initial URL of the browser. All subsequent open(..) requests will be appended to this URL.
     *                   If null, it has to be specified in the properties file specified in the next parameter.
     * @param configName The properties file name to be used for configuring the run-time parameters (default: browser.properties).
     */
    protected AbstractSeleniumTest(String initialUrl, String configName) {
        this(initialUrl, configName, null);
    }

    private AbstractSeleniumTest(String initialUrl, String configName, Properties defaultProperties) {
        this.initialUrl = initialUrl;
        this.configName = configName;
        this.defaultProperties = defaultProperties;
    }

    /**
     * Selenium setup code. Override this method in your actual test class and don't forget
     * to add the @BeforeClass annotation.
     */
    @BeforeClass
    public void beforeClass() {
        final Properties properties = new Properties(defaultProperties);
        if (StringUtils.isNotBlank(configName)) {
            try {
                InputStream inStream = this.getClass().getClassLoader().getResourceAsStream(configName);
                if (inStream == null) {
                    String msg = "Missing " + configName + " file in src/framework/tests/java/com/flexive/tests/browser - copy from provided sample";
                    LOG.error(msg);
                    throw new RuntimeException(msg);
                }
                properties.load(inStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (this.initialUrl == null) {
            this.initialUrl = (String) properties.get(PROP_BASEURL);
        }
        final int port = Integer.parseInt(properties.getProperty(PROP_PORT, "80"));
        final String host = properties.getProperty(PROP_HOST, "localhost");
        if (LOG.isInfoEnabled()) {
            LOG.info("Connecting to Selenium Server at " + host + ":" + port + "...");
            LOG.info("Base URL: " + this.initialUrl);
        }
        commandProcessor = new HttpCommandProcessor(host, port, "*firefox", initialUrl);
        selenium = new FlexiveSelenium(commandProcessor);
        selenium.start();
        selenium.createCookie(FxSharedUtils.COOKIE_FORCE_TEST_DIVISION + "=true", "path=/");
    }

    /**
     * Selenium shutdown code. Override this method in your actual test class and don't forget
     * to add the @BeforeClass annotation.
     */
    @AfterClass
    public void afterClass() {
        selenium.stop();
    }
}
