<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/">
        <output>
            <foo>fooa</foo>
            <bar>
                <xsl:attribute name="attrib"><xsl:value-of select="normalize-space(//foo)" /></xsl:attribute>
                <xsl:value-of select="normalize-space(//foo)" />
            </bar>
        </output>
    </xsl:template>
</xsl:stylesheet>