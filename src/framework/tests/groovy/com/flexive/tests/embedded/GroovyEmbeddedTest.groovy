package com.flexive.tests.embedded
import org.testng.annotations.*
import com.flexive.shared.EJBLookup
/**
 * Sample embedded groovy test.
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */

class GroovyEmbeddedTest {
    @Test(groups = ["ejb"])
    void groovyEmbeddedTest() {
        assert "de" == EJBLookup.languageEngine.load("de").getIso2digit()
    }
}