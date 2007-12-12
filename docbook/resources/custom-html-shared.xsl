<?xml version='1.0'?>
<!--
     Shared docbook parameters for all HTML output vairants

     @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     @version $Rev: 4 $
-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:param name="ignore.image.scaling" select="1"/> <!-- Don't (down-)scale bitmaps -->

    <!-- Add custom head tags -->
    <xsl:template name="user.head.content">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    </xsl:template>
</xsl:stylesheet>