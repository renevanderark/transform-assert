<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="ns2:urn" xmlns:ns1="ns1:urn">

    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/">
        <ns1:foo>
            <ns2:bar>
                <xsl:value-of select="normalize-space(//foo)" />
            </ns2:bar>
        </ns1:foo>
    </xsl:template>
</xsl:stylesheet>