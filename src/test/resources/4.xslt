<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="1.xslt"/>

    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" indent="yes" />

    <xsl:template match="/">
        <data>
            <xsl:apply-imports/>


            <xsl:copy-of select="document('2.xml')" />


        </data>
    </xsl:template>
</xsl:stylesheet>