<?xml version='1.0'?>
<!--
     Customized docbook stylesheet for website output

     @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     @version $Rev$
-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:import href="xhtml/chunk.xsl"/>
    <xsl:import href="custom-html-shared.xsl"/>

    <!-- Use section IDs as filenames -->
    <xsl:param name="use.id.as.filename" select="1"/>

    <xsl:template name="footer.copyright">
        <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'Copyright'"/>
        </xsl:call-template>
        <xsl:call-template name="gentext.space"/>
        <xsl:call-template name="dingbat">
            <xsl:with-param name="dingbat">copyright</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="gentext.space"/>
        <xsl:text>1999-2008 UCS - unique computing solutions gmbh</xsl:text>
        <xsl:call-template name="gentext.space"/>
    </xsl:template>

    <xsl:template name="footer.navigation">
        <xsl:param name="prev" select="/foo"/>
        <xsl:param name="next" select="/foo"/>
        <xsl:param name="nav.context"/>

        <xsl:variable name="home" select="/*[1]"/>
        <xsl:variable name="up" select="parent::*"/>

        <xsl:variable name="row1"
                      select="count($prev) &gt; 0                                     or count($up) &gt; 0                                     or count($next) &gt; 0"/>

        <xsl:variable name="row2"
                      select="($prev and $navig.showtitles != 0)                                     or (generate-id($home) != generate-id(.)                                         or $nav.context = 'toc')                                     or ($chunk.tocs.and.lots != 0                                         and $nav.context != 'toc')                                     or ($next and $navig.showtitles != 0)"/>

        <xsl:if test="$suppress.navigation = '0' and $suppress.footer.navigation = '0'">
            <div class="navfooter">
                <xsl:if test="$footer.rule != 0">
                    <hr/>
                </xsl:if>

                <xsl:if test="$row1 or $row2">

                    <xsl:if test="$row1">
                        <table width="100%" summary="Navigation footer">
                            <tr>
                                <td width="20%" align="left">
                                    <xsl:if test="count($prev)&gt;0">
                                        <a accesskey="p">
                                            <xsl:attribute name="href">
                                                <xsl:call-template name="href.target">
                                                    <xsl:with-param name="object" select="$prev"/>
                                                </xsl:call-template>
                                            </xsl:attribute>
                                            <xsl:call-template name="navig.content">
                                                <xsl:with-param name="direction" select="'prev'"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:if>
                                    <xsl:text>&#160;</xsl:text>
                                </td>
                                <td width="60%" align="center">
                                    <xsl:call-template name="footer.copyright"/>
                                </td>
                                <td width="20%" align="right">
                                    <xsl:text>&#160;</xsl:text>
                                    <xsl:if test="count($next)&gt;0">
                                        <a accesskey="n">
                                            <xsl:attribute name="href">
                                                <xsl:call-template name="href.target">
                                                    <xsl:with-param name="object" select="$next"/>
                                                </xsl:call-template>
                                            </xsl:attribute>
                                            <xsl:call-template name="navig.content">
                                                <xsl:with-param name="direction" select="'next'"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:if>
                                </td>
                            </tr>
                        </table>
                    </xsl:if>

                    <xsl:if test="$row2">
                        <table width="100%" summary="Navigation footer">
                            <tr>
                                <td width="40%" align="left" valign="top">
                                    <xsl:if test="$navig.showtitles != 0">
                                        <xsl:apply-templates select="$prev" mode="object.title.markup"/>
                                    </xsl:if>
                                    <xsl:text>&#160;</xsl:text>
                                </td>
                                <td width="20%" align="center">
                                    <xsl:choose>
                                        <xsl:when test="$home != . or $nav.context = 'toc'">
                                            <a accesskey="h">
                                                <xsl:attribute name="href">
                                                    <xsl:call-template name="href.target">
                                                        <xsl:with-param name="object" select="$home"/>
                                                    </xsl:call-template>
                                                </xsl:attribute>
                                                <xsl:call-template name="navig.content">
                                                    <xsl:with-param name="direction" select="'home'"/>
                                                </xsl:call-template>
                                            </a>
                                            <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                                                <xsl:text>&#160;|&#160;</xsl:text>
                                            </xsl:if>
                                        </xsl:when>
                                        <xsl:otherwise>&#160;</xsl:otherwise>
                                    </xsl:choose>

                                    <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                                        <a accesskey="t">
                                            <xsl:attribute name="href">
                                                <xsl:apply-templates select="/*[1]" mode="recursive-chunk-filename">
                                                    <xsl:with-param name="recursive" select="true()"/>
                                                </xsl:apply-templates>
                                                <xsl:text>-toc</xsl:text>
                                                <xsl:value-of select="$html.ext"/>
                                            </xsl:attribute>
                                            <xsl:call-template name="gentext">
                                                <xsl:with-param name="key" select="'nav-toc'"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:if>
                                </td>
                                <td width="40%" align="right" valign="top">
                                    <xsl:text>&#160;</xsl:text>
                                    <xsl:if test="$navig.showtitles != 0">
                                        <xsl:apply-templates select="$next" mode="object.title.markup"/>
                                    </xsl:if>
                                </td>
                            </tr>
                        </table>
                    </xsl:if>
                </xsl:if>
            </div>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>