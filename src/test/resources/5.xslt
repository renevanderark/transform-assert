<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="param1"/>
    <xsl:param name="param2"/>

    <xsl:template match="/">
        <output>
            <one>
                <xsl:value-of select="normalize-space(//foo)" />
            </one>
            <two>
                <xsl:value-of select="$param1" />
            </two>
            <two>
                <xsl:value-of select="$param2" />
            </two>
        </output>
    </xsl:template>
</xsl:stylesheet>