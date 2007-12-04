<?xml version='1.0'?>
<!--
     Customized docbook stylesheet for print output
     @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     @version $Rev$
-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:import href="fo/docbook.xsl"/>

    <xsl:param name="hyphenate.verbatim" select="1"/> <!-- Add hyphenation characters in listings, not supported by FOP -->

    <!-- Enable line wrapping in program listings -->
    <xsl:attribute-set name="monospace.verbatim.properties">
        <xsl:attribute name="wrap-option">wrap</xsl:attribute>
        <xsl:attribute name="hyphenation-character">&#x21B5;</xsl:attribute>
        <xsl:attribute name="font-size">8pt</xsl:attribute>
    </xsl:attribute-set>

    <!-- Decrease relative monospace font size, otherwise it appears too big compared to the surrounding text  -->
    <xsl:attribute-set name="monospace.properties">
        <xsl:attribute name="font-size">0.8em</xsl:attribute>
    </xsl:attribute-set>

</xsl:stylesheet>