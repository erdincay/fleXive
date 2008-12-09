package com.flexive.tests.embedded.benchmark;

import org.testng.annotations.*;

/**
 * Benchmark initialization and cleanup.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups="benchmark")
public class BenchmarkBootstrap {
    @BeforeSuite
    public void startup() {
    }

    @AfterSuite
    public void shutdown() {
        System.out.println("RESULTS:\n\n");
        System.out.println(FxBenchmarkUtils.getResultLogger().getOutput());
    }
}
