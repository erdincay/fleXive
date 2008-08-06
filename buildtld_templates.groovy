/**
 * Extracts the parameter documentation from Facelets templates (for the format, see e.g. formRow.xhtml) and
 * prints out XML to be used in a TLD file (e.g. flexive.tld).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

if (!args || args.length != 1) {
    println "Usage: buildtld_templates <taglib.xml>\n"
    return
}

// load XML, skip DTD (because the facelets-dtd cannot be retrieved from the given URL)
final File taglibFile = new File(args[0])
final String taglibXml = taglibFile.text.replace("\r\n", "").replace("\n", "").replaceAll("<!DOCTYPE[^>]*>", "")
final def xmlOut = new StringWriter()
final def xml = new groovy.xml.MarkupBuilder(xmlOut)

new XmlSlurper(false, false).parseText(taglibXml).tag.list().each { it ->
    final String tag = it."tag-name".toString()
    final String source = it."source".toString()
    if (!source.isEmpty()) {
        // template component, source = source file name relative to the taglib file
        boolean inParams = false        // in Parameters: block
        boolean inDescription = false   // in Description: block
        final def tagDescription = new StringBuffer()
        xml.tag {
            name(tag)
            "body-content"("JSP")

            new File(taglibFile.getParent() + File.separator + source).eachLine { String line ->
                if (line.startsWith("Description:")) {
                    inDescription = true
                    return
                }
                if (line.startsWith("Parameters:")) {
                    description(tagDescription.toString())
                    inParams = true
                    inDescription = false
                    return
                }
                if (line.contains("ui:composition")) {
                    inParams = false
                    return
                }
                if (inDescription) {
                    tagDescription.append(line).append(" ")
                }
                if (inParams) {
                    //println line
                    def matcher = line =~ /\s*([a-zA-Z]+)\s+[-=]\s*(.*)/
                    if (matcher.matches()) {
                        attribute {
                            name(matcher.group(1))
                            required(false)
                            rtexprvalue(false)
                            description(matcher.group(2))
                        }
                    }
                }
            }
        }
    }
}

println xmlOut