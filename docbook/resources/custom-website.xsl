<?xml version='1.0'?>
<!--
     Customized docbook stylesheet for website output

     @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     @version $Rev: 4 $
-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:import href="xhtml/chunk.xsl"/>
    <xsl:import href="custom-html-shared.xsl"/>

    <!-- Use section IDs as filenames -->
    <xsl:param name="use.id.as.filename" select="1"/>
</xsl:stylesheet>