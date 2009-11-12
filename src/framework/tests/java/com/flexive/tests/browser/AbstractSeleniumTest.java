/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
    private static final Log LOG = LogFactory.getLog(AbstractSeleniumTest.class);

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
    /**
     * The startcommand of the browser
     */
    public static final String PROP_BROWSER_START = "tests.browser.selenium.browserStart";

    protected String initialUrl;
    protected final String configName;
    protected final Properties defaultProperties;

    private static FlexiveSelenium selenium;
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
        final String browserStart = properties.getProperty(PROP_BROWSER_START, "*firefox");
        if (LOG.isInfoEnabled()) {
            LOG.info("Connecting to Selenium Server at " + host + ":" + port + "...");
            LOG.info("Base URL: " + this.initialUrl);
        }

        /**
         *
         *
         *  *firefox
         *mock
         *firefoxproxy
         *pifirefox
         *chrome
         *iexploreproxy
         *iexplore
         *firefox3
         *safariproxy
         *googlechrome
         *konqueror
         *firefox2
         *safari
         *piiexplore
         *firefoxchrome
         *opera
         *iehta
         */
//        commandProcessor = new HttpCommandProcessor(host, port, "*iexploreproxy", initialUrl);
        commandProcessor = new HttpCommandProcessor(host, port, browserStart, initialUrl);
        selenium = new FlexiveSelenium(commandProcessor);
        LOG.info("using browser : " + browserStart.substring(1));
        selenium.start();
        selenium.createCookie(FxSharedUtils.COOKIE_FORCE_TEST_DIVISION + "=true", "path=/");
        try {
            LOG.info(selenium.getCookie());
        } catch (Throwable t) {
            LOG.error(t.getMessage());
        }
//        LOG.error("Create Test-cookie DISABLED!!!");
    }

    /**
     * Selenium shutdown code. Override this method in your actual test class and don't forget
     * to add the @BeforeClass annotation.
     */
    @AfterClass
    public void afterClass() {
        selenium.stop();
    }

    protected void clickAndWait(String clickTarget) {
        clickAndWait_(clickTarget, 30000);
    }

    protected void clickAndWait(String clickTarget, int ms) {
        clickAndWait_(clickTarget, ms);
    }

    /**
     * Clicks multiple times (in case of timeout) on a link
     * <p/>
     * to have always the right stacktrace
     *
     * @param clickTarget
     * @param ms
     */
    private void clickAndWait_(String clickTarget, int ms) {
        int trys = 20;
        ms >>= 3;
        boolean error = false;
        String code = "";
        int errLeft = 5;
        while (trys-- > 0) {
            try {
                selenium.click(clickTarget);
                selenium.waitForPageToLoad("" + ms);
                if (error) {
                    System.err.println("you ment \"" + clickTarget + "\", but I corrected it for you! [" + code + "]");
                }
                break;
            } catch (Throwable t) {
                if (t.getMessage().endsWith(" not found") && errLeft-- <= 0 && !error) {
                    clickTarget = "link=" + clickTarget;
                    error = true;
                    code = "" + t.getStackTrace()[4];
                }
                sleep(200);
            }
        }
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore it
        }
    }

    public static FlexiveSelenium getSeleniumInstance() {
        return selenium;
    }

}
