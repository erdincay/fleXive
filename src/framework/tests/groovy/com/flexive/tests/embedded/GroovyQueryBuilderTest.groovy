package com.flexive.tests.embedded
import com.flexive.shared.scripting.groovy.GroovyQueryBuilder
import com.flexive.shared.search.query.SqlQueryBuilder
import org.testng.annotations.*

/**
 * Test scripts for the query builder test bean
 * @author Daniel Lichtenberger, UCS
 */
class GroovyQueryBuilderTest {

    @Test(groups = ["scripting", "ejb", "search"])
    def simpleBuilderTest() {
        def root = new GroovyQueryBuilder().select(["@pk", "caption", "*"]) {
          eq("caption", "bla")
          not_empty("filename")
          or {
             gt("id", 0)
             lt("id", 100)
          }
          lt("created_at", new Date(0))
        }
        def cond = "(co.CAPTION = 'bla' AND co.FILENAME IS NOT NULL AND (co.ID > 0 OR co.ID < 100) AND co.CREATED_AT < '1970-01-01')"
        def result = root.sqlQuery
        assert result.indexOf(cond) > 0 : "Expected condition: " + cond + ", \ngot:\n" + result
    }

    @Test(groups = ["scripting", "ejb", "search"])
    def repeatableSqlQueryTest() {
        def root = new GroovyQueryBuilder().select(["caption"]) {
            like("caption", "test%")
            gt("id", 0)
        }
        def query = root.sqlQuery
        def query2 = root.sqlQuery
        assert query == query2 : "Second call to getSqlQuery() returned a different result, \n[1]: " + query + "\n[2]: " + query2
    }

    @Test(groups = ["scripting", "ejb", "search"])
    def filterBriefcaseTest() {
        def root = new GroovyQueryBuilder().select(["@pk"]) {
            filterBriefcase(21)
        }
        def query = root.sqlQuery
        assert query.indexOf("briefcase=21") > 0 : "Query should contain briefcase=21 filter: " + query
        // check if search can be submitted
        root.queryBuilder.getResult()
    }

    @Test(groups = ["scripting", "ejb", "search"])
    def filterBriefcaseTestExternalBuilder() {
        def builder = new SqlQueryBuilder()
        new GroovyQueryBuilder(builder).select(["@pk"]) {
            filterBriefcase(21)
        }
        assert builder.query.indexOf("briefcase=21") > 0 : "Query should contain briefcase=21 filter: " + builder.query
        // check if search can be submitted
        builder.getResult()
    }
}
