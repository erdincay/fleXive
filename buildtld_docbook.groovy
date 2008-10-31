import groovy.util.XmlParser
import groovy.util.Node

/**
 * Extracts the parameter documentation from our docbook documentation (jsfsupport.xml) for a tag and
 * prints out XML to be used in a TLD file (e.g. flexive.tld).
 *
 * Must be called from the docbook/src directory!
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

// load XML, skip DTD (because the facelets-dtd cannot be retrieved from the given URL)
final File docbookFile = new File("jsfsupport.xml")
final String docbookXml = docbookFile.text

def process(Node node) {
    final String id = node["@xml:id"]
    if (id?.endsWith("_parameters")) {
        println "*" * 50
        println "Parameters for: " + id
        println "*" * 50
        println ""
        processParameters(node)
    } else {
        node.children().each { it ->
            if (it instanceof Node) {
                process((Node) it)
            }
        }
    }
}

String collectText(node) {
    if (node instanceof String) {
        return node
    } else if (node instanceof Node) {
        final StringBuilder out = new StringBuilder()
        node.children().each { child ->
            out.append(collectText(child))
        }
        return " " + out.toString() + " "
    } else {
        return ""
    }
}
def processParameters(Node node) {
    // get parameter list, which may be embedded in a <para>
    final Node list = node.variablelist[0] ?: node.para[0].variablelist[0]

    final def xmlOut = new StringWriter()
    final def xml = new groovy.xml.MarkupBuilder(xmlOut)

    list.varlistentry.each { entry ->
        final String _description = collectText(entry.listitem[0]).replaceAll("  +", " ").replace("\r\n", " ").replace("\n", " ").trim()
        final String _attribute = entry.term.text()
        //println attribute + " --> " + description.trim().replaceAll("  +", " ").replace("\r\n", " ").replace("\n", " ")
        xml.attribute {
            name(_attribute)
            required("false")
            rtexprvalue("true")
            description(_description)
        }
    }
    println xmlOut
}

new XmlParser(false, false).parseText(docbookXml).children().each { Node it ->
    process(it)
}
