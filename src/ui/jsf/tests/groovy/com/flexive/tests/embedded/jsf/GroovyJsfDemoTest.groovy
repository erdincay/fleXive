package com.flexive.tests.embedded.jsf
import org.testng.annotations.*
import com.flexive.faces.FxJsfUtils

/**
* Sample JSF/Groovy test.

* @author Daniel Lichtenberger, UCS
* @version $Rev$
*/
class GroovyJsfDemoTest {

    @Test(groups = ["jsf"])
    void groovyJsfTest() {
        assert 2 == com.flexive.faces.FxJsfUtils.evalInt("#{1+1}")
    }

}