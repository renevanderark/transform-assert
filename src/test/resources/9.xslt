<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:strip-space elements="*"/>
    <xsl:output method="text" indent="no" omit-xml-declaration="yes"/>

    <xsl:template match="/">
        <xsl:copy-of select="document('notfound.xml')"/>
    </xsl:template>
</xsl:stylesheet>