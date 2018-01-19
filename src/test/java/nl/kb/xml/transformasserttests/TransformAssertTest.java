package nl.kb.xml.transformasserttests;

import nl.kb.xml.transformassert.TransformAssertWithTransformer;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.kb.xml.transformassert.TransformAssert.describe;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

public class TransformAssertTest {

    private static final String XSLT = "<?xml version=\"1.0\"?>\n" +
            "\n" +
            "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
            "\n" +
            "    <xsl:strip-space elements=\"*\"/>\n" +
            "    <xsl:output method=\"text\" indent=\"no\" omit-xml-declaration=\"yes\"/>\n" +
            "\n" +
            "    <xsl:template match=\"/\">\n" +
            "        <output>\n" +
            "            <xsl:value-of select=\"normalize-space(//foo)\" />\n" +
            "        </output>\n" +
            "    </xsl:template>\n" +
            "</xsl:stylesheet>";

    private static final String XML = "<root><foo>bar</foo></root>";


    @Test(expected = AssertionError.class)
    public void isEqualToAssertsStringInequalityOfOutput() throws UnsupportedEncodingException, TransformerException, FileNotFoundException {
        describe(XSLT)
                .whenTransforming(XML)
                .isEqualto("not bar", "het moet 'not bar' zijn")
                .evaluate();

    }

    @Test(expected = AssertionError.class)
    public void isEqualToAssertsStringInequalityOfOutputForFiles() throws UnsupportedEncodingException, TransformerException, FileNotFoundException {
        describe(new File("./src/test/resources/1.xslt"))
                .whenTransforming(new File("./src/test/resources/1.xml"))
                .isEqualto("not bar")
                .evaluate();
    }




    // Dit test op gelijkheid op de volledige output van een stylesheet transformatie (isEqualTo)
    // hier wordt dus verwacht dat de stylesheet als output slechts de waarde 'bar' heeft
    @Test
    public void transformerShouldSupportImports() throws UnsupportedEncodingException, FileNotFoundException, TransformerException {
        describe(new File("./src/test/resources/2.xslt"))
                .whenTransforming(XML)
                .isEqualto("bar")
                .evaluate();
    }

    // Deze test controleert of het test framework zowel een string als een file als bron accepteert
    @Test
    public void isEqualToAssertsStringEqualityOfOutput() throws UnsupportedEncodingException, TransformerException, FileNotFoundException {
        describe(XSLT)
                .whenTransforming(XML)
                .isEqualto("bar")
                .evaluate();

        describe(XSLT)
                .whenTransforming(new File("./src/test/resources/1.xml"))
                .isEqualto("bar")
                .evaluate();

        describe(new File("./src/test/resources/1.xslt"))
                .whenTransforming(XML)
                .isEqualto("bar")
                .evaluate();
    }

    // Dit test op waardes die voortkomen uit het volgen van een xpath over de output van de transformatie (hasXpathContaining)
    @Test
    public void hasXpathContainingShouldAssertMatches() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        describe(new File("./src/test/resources/3.xslt"))
                .whenTransforming(XML)
                .hasXpathContaining("//foo/text()", "foo")
                .andHasXpathContaining("//bar/@attrib", "bar")
                .andHasXpathContaining("//bar/text()", "bar")
                .evaluate();
    }

    // Deze test geeft parameters mee aan de stylesheet
    // Bij de 'xpath assertions' wordt een eigen geformuleerde regel meegegeven
    @Test
    public void transformerShouldSupportParameters() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        describe(new File("./src/test/resources/5.xslt"))
                .whenTransforming(XML, "param1", "param1-value","param2", "param2-value")
                .hasXpathContaining("/output/one/text()", "bar", "Node <one> moet de tekst binnen <foo> bevatten")
                .andHasXpathContaining("/output/two[1]/text()", "param1-value", "Eerste node <two> moet de waarde van param1 bevatten")
                .andHasXpathContaining("/output/two[2]/text()", "param2-value", "Tweede node <two> moet de waarde van param2 bevatten")
                .evaluate();
    }


    // Deze test is bedoeld om te controleren of alle fouten die zijn aangetroffen ook worden teruggerapporteerd
    @Test(expected = AssertionError.class)
    public void hasXpathContainingShouldAssertMismatches() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        describe(new File("./src/test/resources/3.xslt"))
                .whenTransforming(XML)
                .hasXpathContaining("//foo/text()", "not foo", "deze zal falen")
                .hasXpathContaining("//bar/text()", "not bar", "en deze ook")
                .evaluate();
    }

    @Test(expected = AssertionError.class)
    public void hasXpathContainingShouldFailCorrectlyOnMissingNode() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        describe(new File("./src/test/resources/3.xslt"))
                .whenTransforming(XML)
                .hasXpathContaining("//foo/text()", "foo")
                .andHasXpathContaining("//barza/text()", "not there")
                .evaluate();
    }

    @Test
    public void transformerShouldResolveXmlContent() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        describe(new File("./src/test/resources/4.xslt"))
                .whenTransforming(XML)
                .hasXpathContaining("/data/text()", "bar")
                .andHasXpathContaining("/data/xml2/text()", "content")
                .evaluate();
    }


    // Deze test heeft namespaces in de 'xpath assertions', die moeten gedeclareeerd worden (usingNamespace).
    @Test
    public void evaluatorsShouldSupportNamespaces() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        describe(new File("./src/test/resources/6.xslt"))
                .whenTransforming(XML)
                .usingNamespace("ns1", "ns1:urn")
                .andUsingNamespace("ns2", "ns2:urn")
                .hasXpathContaining("/ns1:foo/ns2:bar/text()", "bar")
                .evaluate();
    }

    // Deze test valideert de output tegen een XSD (validatesAgainstXSD) en slaagt
    @Test
    public void itShouldValidateAgainstAnXSD() throws FileNotFoundException, UnsupportedEncodingException, TransformerException, SAXException {
        describe(new File("./src/test/resources/5.xslt"))
                .whenTransforming(XML,
                        "param1", "param1-value",
                        "param2", "param2-value"
                ).validatesAgainstXSD(new File("src/test/resources/1.xsd"))
                .evaluate();
    }

    // Deze test valideert de output tegen een XSD (validatesAgainstXSD) en faalt
    @Test(expected = AssertionError.class)
    public void validateAgainstAnXSDShouldThrowWhenInvalid() throws FileNotFoundException, UnsupportedEncodingException, TransformerException, SAXException {
        describe(new File("./src/test/resources/5.xslt"))
                .whenTransforming(XML,
                        "param1", "param1-value",
                        "param2", "param2-value"
                ).validatesAgainstXSD(new File("src/test/resources/2.xsd"), "node <one> wordt niet verwacht door deze XSD")
                .evaluate();
    }

    @Test
    public void reportingLogsExpectedMessages() throws IOException, TransformerException, SAXException, XPathExpressionException, ParserConfigurationException {
        final List<String> messages = new ArrayList<>();

        try {
            describe(new File("./src/test/resources/5.xslt"), messages::add)
                    .whenTransforming(XML, "param1", "param1-value", "param2", "param2-value")
                    .isEqualto("niet gelijk aan dit", "moet gelijk zijn aan dit")
                    .hasXpathContaining("/output/one/text()", "bar", "Node <one> moet de tekst binnen <foo> bevatten")
                    .andDoesNotHaveXpathContaining("/output/two[1]/text()", "param1-value", "Eerste node <two> mag de waarde van param1 niet bevatten")
                    .andHasXpathContaining("/output/two[2]/text()", "param1-value", "Tweede node <two> moet de waarde van param1 bevatten")
                    .andValidatesAgainstXSD(new File("src/test/resources/1.xsd"), "valideert tegen 1.xsd")
                    .andValidatesAgainstXSD(new File("src/test/resources/2.xsd"), "valideert niet tegen 2.xsd")
                    .evaluate();
        } catch (AssertionError e) {
            // assert error
        }

        final List<String> trimmedMessages = messages.stream().map(String::trim).collect(toList());

        messages.forEach(System.out::println);
        assertThat(trimmedMessages, hasItems(
                is("moet gelijk zijn aan dit (FAILED)"),
                is("Node <one> moet de tekst binnen <foo> bevatten (OK)"),
                is("Eerste node <two> mag de waarde van param1 niet bevatten (FAILED)"),
                is("Tweede node <two> moet de waarde van param1 bevatten (FAILED)"),
                is("valideert tegen 1.xsd (OK)"),
                is("valideert niet tegen 2.xsd (FAILED)"),
                containsString("FAILURES"),
                is("Expected output to equal: 'niet gelijk aan dit'"),
                is("Expected xpath /output/two[1]/text() NOT to match: 'param1-value'"),
                is("But got: 'param1-value'"),
                is("Expected xpath /output/two[2]/text() to match: 'param1-value'"),
                is("But got: 'param2-value'"),
                containsString("Expected output to validate against XSD")
        ));
    }

    @Test
    public void moeilijk() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        final TransformAssertWithTransformer moeilijk = describe(new File("./src/test/resources/moeilijk.xslt"));
        moeilijk
                .whenTransforming("<Record>" +
                        "<p002-><f0>y</f0></p002->" +
                        "<p028-><fa>inhoud van p028-/fa</fa></p028->" +
                        "</Record>")
                .usingNamespace("dc", "http://purl.org/dc/elements/1.1/")
                .andUsingNamespace("dcx", "http://krait.kb.nl/coop/tel/handbook/telterms.html")
                .hasXpathContaining("//dc:contributor/@dcx:role", "copiist", "Als p002- is y dan moet dc:contributor de rol copiist hebben")


                .evaluate();

        moeilijk
                .whenTransforming("<Record>" +
                        "<p002-><f0>x</f0></p002->" +
                        "<p028-><fa>inhoud van p028-/fa</fa></p028->" +
                        "</Record>")
                .usingNamespace("dc", "http://purl.org/dc/elements/1.1/")
                .andUsingNamespace("dcx", "http://krait.kb.nl/coop/tel/handbook/telterms.html")
                .hasXpathContaining("//dc:contributor/@dcx:role", "illustrator", "Als p002- is x dan moet dc:contributor de rol illustrator hebben")
                .evaluate();
    }

    @Test
    public void negatedXpathMatch() throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        describe(new File("./src/test/resources/5.xslt"))
            .whenTransforming(XML, "param1", "param1-value", "param2", "param2-value")
            .hasXpathContaining("/output/one/text()", "bar" )
            .andHasXpathContaining("/output/two[1]/text()", "param1-value")
            .andDoesNotHaveXpathContaining("/output/two[2]/text()", "param1-value")
            .evaluate();
    }

}
