package com.flexive.tests.embedded.benchmark.logger;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultLoggerTest {

    @Test(groups = {"shared", "benchmark"}, dataProvider = "resultLoggerImplementations")
    public void testResultLogger(ResultLogger logger) {
        logger.logTime("test100", System.currentTimeMillis() - 100, 1, "contents");
        logger.logTime("test100_10", System.currentTimeMillis() - 100, 10, "contents");
        logger.getOutput();
    }

    @DataProvider(name = "resultLoggerImplementations")
    public Object[][] getResultLoggerImplementations() {
        return new Object[][] {
                { new PlainTextLogger() },
                { new XmlLogger() }
        };
    }
}
