package com.flexive.tests.embedded

import com.flexive.shared.scripting.groovy.GroovyContentBuilder
import com.flexive.shared.value.FxString
import org.testng.Assert
import org.testng.annotations.Test

/**
* GroovyContentBuilder test cases.

* @author Daniel Lichtenberger, UCS
* @version $Rev$
*/
class GroovyContentBuilderTest {
    @Test(groups = ["content", "ejb", "scripting"])
    void createContentTest() {
        def builder = new GroovyContentBuilder("ARTICLE")
        builder {
            title("Test article")
            Abstract("My abstract text")
            teaser {
                teaser_title("Teaser title")
                teaser_text("Teaser text")
            }
            box {
                box_title(new FxString(false, "Box title 1"))
            }
            box {
                box_title("Box title 2")
                box_text("Some box text")
            }
        }
        def content = builder.getContent()
        Assert.assertEquals(content.getValue("/title").defaultTranslation, "Test article")
        Assert.assertEquals(content.getValue("/abstract").defaultTranslation, "My abstract text")
        Assert.assertEquals(content.getValue("/teaser[1]/teaser_title").defaultTranslation, "Teaser title")
        Assert.assertEquals(content.getValue("/teaser[1]/teaser_text").defaultTranslation, "Teaser text")
        Assert.assertEquals(content.getValue("/box[1]/box_title").defaultTranslation, "Box title 1")
        Assert.assertEquals(content.getValue("/box[2]/box_title").defaultTranslation, "Box title 2")
        Assert.assertEquals(content.getValue("/box[2]/box_text").defaultTranslation, "Some box text")
    }
}