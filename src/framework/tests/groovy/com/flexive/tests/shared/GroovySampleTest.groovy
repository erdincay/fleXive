package com.flexive.tests.shared

import org.testng.annotations.*;
/**
 * A simple Groovy TestNG test.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
class GroovySampleTest {

    @Test(groups = ["shared"])
    void groovySharedTest() {
        assert 1 == 1
    }
}