<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:srw_dc="info:srw/schema/1/dc-v1.1"
                xmlns:dcx="http://krait.kb.nl/coop/tel/handbook/telterms.html"
                xmlns:dc="http://purl.org/dc/elements/1.1/">
    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" indent="yes" />

    <xsl:template match="/">
        <srw_dc:dc>
            <dc:contributor>
                <xsl:if test="/Record/p002-/f0='x'">
                    <xsl:attribute name="dcx:role">illustrator</xsl:attribute>
                </xsl:if>
                <xsl:if test="/Record/p002-/f0='y'">
                    <xsl:attribute name="dcx:role">copiist</xsl:attribute>
                </xsl:if>

                <xsl:value-of select="/Record/p028-/fa" />
            </dc:contributor>
        </srw_dc:dc>
    </xsl:template>
</xsl:stylesheet>