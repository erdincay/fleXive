<?xml version='1.0'?>
<!--
     Shared docbook parameters for all HTML output vairants

     @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     @version $Rev$
-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xslthl="http://xslthl.sf.net"
        xmlns="http://www.w3.org/1999/xhtml"
        exclude-result-prefixes="xslthl"
        version="1.0">

    <xsl:import href="highlighting/common.xsl"/>

    <xsl:param name="ignore.image.scaling" select="1"/>
    <xsl:param name="toc.section.depth" select="3"/>
    <!-- Don't (down-)scale bitmaps -->

    <!-- Add custom head tags -->
    <xsl:template name="user.head.content">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    </xsl:template>

    <!-- Force IE7 mode in IE8 (it seems that this must be before other meta tags, or at least before
         user.head.content is inserted) -->
    <xsl:template name="system.head.content">
        <meta http-equiv="X-UA-Compatible" content="IE=7" />
    </xsl:template>

    <xsl:template name="credits.div">
        <div>
            <xsl:apply-templates select="." mode="class.attribute"/>
            <xsl:if test="self::editor[position()=1] and not($editedby.enabled = 0)">
                <h4 class="editedby">
                    <xsl:call-template name="gentext.edited.by"/>
                </h4>
            </xsl:if>
            <xsl:choose>
                <xsl:when test="orgname">
                    <xsl:apply-templates/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="person.name"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="not($contrib.inline.enabled = 0)">
                <xsl:apply-templates mode="titlepage.mode" select="contrib"/>
            </xsl:if>
            <xsl:apply-templates mode="titlepage.mode" select="affiliation"/>
            <xsl:apply-templates mode="titlepage.mode" select="email"/>
            <xsl:if test="not($blurb.on.titlepage.enabled = 0)">
                <xsl:choose>
                    <xsl:when test="$contrib.inline.enabled = 0">
                        <xsl:apply-templates mode="titlepage.mode" select="contrib|authorblurb|personblurb"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates mode="titlepage.mode" select="authorblurb|personblurb"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
        </div>
    </xsl:template>


    <xsl:template name="inline.email">
        <xsl:param name="content">
            <xsl:call-template name="anchor"/>
            <xsl:call-template name="simple.xlink">
                <xsl:with-param name="content">
                    <xsl:apply-templates/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:param>

        <xsl:call-template name="dir"/>
        <xsl:call-template name="generate.html.title"/>
        <xsl:copy-of select="$content"/>
        <xsl:call-template name="apply-annotations"/>
    </xsl:template>

    <xsl:template match="email">
        <xsl:call-template name="inline.email">
            <xsl:with-param name="content">
                <xsl:if test="not($email.delimiters.enabled = 0)">
                    <xsl:text>  &lt;</xsl:text>
                </xsl:if>
                <a>
                    <xsl:apply-templates select="." mode="class.attribute"/>
                    <xsl:attribute name="href">
                        <xsl:text>mailto:</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                    <xsl:apply-templates/>
                </a>
                <xsl:if test="not($email.delimiters.enabled = 0)">
                    <xsl:text>&gt;</xsl:text>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- Configure syntax highlighting -->

    <!--<xsl:import href="xhtml/highlight.xsl"/>-->

    <xsl:template match="xslthl:keyword" mode="xslthl">
        <span class="hl-keyword">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:string" mode="xslthl">
        <span class="hl-string">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:comment" mode="xslthl">
        <span class="hl-comment">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:directive" mode="xslthl">
        <span class="hl-directive">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:tag" mode="xslthl">
        <span class="hl-tag">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:attribute" mode="xslthl">
        <span class="hl-attribute">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:value" mode="xslthl">
        <span class="hl-value">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:html" mode="xslthl">
        <span class="hl-tag">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:xslt" mode="xslthl">
        <span class="hl-xslt">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:number" mode="xslthl">
        <span class="hl-number">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:doctype" mode="xslthl">
        <span class="hl-doctype">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:doccomment" mode="xslthl">
        <span class="hl-doccomment">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>

    <xsl:template match="xslthl:annotation" mode="xslthl">
        <span class="hl-annotation">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>


</xsl:stylesheet>