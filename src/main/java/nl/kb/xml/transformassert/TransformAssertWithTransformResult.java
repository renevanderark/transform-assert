package nl.kb.xml.transformassert;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TransformAssertWithTransformResult {

    private static final String FAILED = "FAILED";
    private static final String OK = "OK";

    private final byte[] transformationOutput;
    private final Map<String, String> namespaces = new HashMap<>();
    private final List<AssertionError> errors = new ArrayList<>();
    private final Consumer<String> logBack;
    private final Consumer<String> outputConsumer;
    private final List<TransformerException> errorsAndWarnings;


    TransformAssertWithTransformResult(TransformAssertWithTransformer transformAssertWithTransformer, byte[] transformationOutput) {
        this.transformationOutput = transformationOutput;
        this.logBack = transformAssertWithTransformer.getLogBack();
        this.outputConsumer = transformAssertWithTransformer.getTransformationOutput();
        this.errorsAndWarnings = transformAssertWithTransformer.getErrorsAndWarnings();
        initialize(transformAssertWithTransformer);
    }


    public TransformAssertWithTransformResult usingNamespace(String key, String value) {
        this.namespaces.put(key, value);
        return this;
    }


    public TransformAssertWithTransformResult isEqualto(String expected, String... rule) throws UnsupportedEncodingException {
        final String stringResult = new String(transformationOutput, StandardCharsets.UTF_8.name());

        final String report = mkRule("EQUAL: " + expected, rule);

        if (!stringResult.equals(expected)) {
            errors.add(new AssertionError(String.format(
                    report + System.lineSeparator() +
                            "  Expected output to equal: '%s'" + System.lineSeparator() +
                            "  But got: '%s'" + System.lineSeparator()
                    , expected, stringResult
            )));
            indent(String.format("%s (%s)", report, FAILED), 2, logBack);
        } else {
            indent(String.format("%s (%s)", report, OK), 2, logBack);
        }

        return this;
    }

    private TransformAssertWithTransformResult matchXPath(String xPath, String expected, boolean negate, String... rule)
            throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {

            final String report = mkRule(
                    (negate ? "NOT MATCH XPATH " : "MATCH XPATH ") + xPath + "='" + expected + "'"
                    , rule);

            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (!namespaces.keySet().isEmpty()) {
                dbf.setNamespaceAware(true);
            }

            final XPathFactory xPathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();
            final XPath xpath = xPathFactory.newXPath();
            final DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            final Document doc = documentBuilder.parse(new ByteArrayInputStream(transformationOutput));
            if (!namespaces.keySet().isEmpty()) {
                xpath.setNamespaceContext(new NamespaceContext() {
                    @Override
                    public String getNamespaceURI(String prefix) {
                        if (namespaces.containsKey(prefix)) {
                            return namespaces.get(prefix);
                        }
                        return null;
                    }

                    @Override
                    public String getPrefix(String namespaceURI) {
                        return null;
                    }

                    @Override
                    public Iterator getPrefixes(String namespaceURI) {
                        return null;
                    }
                });
            }

            final XPathExpression expression = xpath.compile(xPath);

            final String stringResult = expression.evaluate(doc).trim();
            if (stringResult.equals(expected) == negate) {
                errors.add(new AssertionError(String.format(
                        report + System.lineSeparator() +
                                "  Expected xpath %s%sto match: '%s'" + System.lineSeparator() +
                                "  But got: '%s'" + System.lineSeparator()
                        , xPath, negate ? " NOT " : " ", expected, stringResult
                )));

                indent(String.format("%s (%s)", report, FAILED), 2, logBack);
            } else {
                indent(String.format("%s (%s)", report, OK), 2, logBack);
            }


            return this;
    }


    public TransformAssertWithTransformResult hasXpathContaining(String xPath, String expected, String... rule)
            throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {

        return matchXPath(xPath, expected, false, rule);
    }

    public TransformAssertWithTransformResult doesNothaveXpathContaining(String xPath, String expected, String... rule)
            throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {

        return matchXPath(xPath, expected, true, rule);
    }


    public TransformAssertWithTransformResult validatesAgainstXSD(File xsd, String... rule) throws UnsupportedEncodingException, FileNotFoundException, SAXException {
        final Reader xmlReader = new InputStreamReader(new ByteArrayInputStream(transformationOutput), StandardCharsets.UTF_8.name());
        final Reader xsdReader = new InputStreamReader(new FileInputStream(xsd), StandardCharsets.UTF_8.name());
        final Source xmlSource = new StreamSource(xmlReader);
        final Source xsdSource = new StreamSource(xsdReader);
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = schemaFactory.newSchema(xsdSource);
        final Validator validator = schema.newValidator();

        final String report = mkRule("VALIDATE AGAINST XSD: " + xsd.getAbsolutePath(), rule);

        try {
            validator.validate(xmlSource);
            indent(String.format("%s (%s)", report, OK), 2, logBack);
        } catch (Exception e) {
            errors.add(new AssertionError(String.format(
                    report + System.lineSeparator() +
                            "  Expected output to validate against XSD: %s" + System.lineSeparator() +
                            "  But got: %s" + System.lineSeparator(),
                    xsd.getAbsolutePath(),
                    e.getMessage()
            )));
            indent(String.format("%s (%s)", report, FAILED), 2, logBack);
        }
        return this;
    }

    public TransformAssertWithTransformResult andIsEqualTo(String expected, String... rule) throws UnsupportedEncodingException {
        return isEqualto(expected, rule);
    }

    public TransformAssertWithTransformResult andHasXpathContaining(String xPath, String expected, String... rule) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {

        return hasXpathContaining(xPath, expected, rule);
    }

    public TransformAssertWithTransformResult andDoesNotHaveXpathContaining(String xPath, String expected, String... rule) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {

        return doesNothaveXpathContaining(xPath, expected, rule);
    }


    public TransformAssertWithTransformResult andUsingNamespace(String key, String value) {
        return usingNamespace(key, value);
    }

    public TransformAssertWithTransformResult andValidatesAgainstXSD(File xsd, String... rule) throws FileNotFoundException, UnsupportedEncodingException, SAXException {
        return validatesAgainstXSD(xsd, rule);
    }

    public void evaluate() throws UnsupportedEncodingException {
        evaluate(false);
    }

    public void evaluate(boolean listXsltWarnings) throws UnsupportedEncodingException {
        if (outputConsumer == null) {
            logBack.accept(System.lineSeparator() + "OUTPUT:");
        }

        final Consumer<String> outConsumer = outputConsumer == null ? logBack : outputConsumer;
        final int indent = outputConsumer == null ? 2 : 0;
        indent(new String(transformationOutput, StandardCharsets.UTF_8.name()), indent, outConsumer);

        logBack.accept(String.format("===================================================%s", System.lineSeparator()));

        if (listXsltWarnings && !errorsAndWarnings.isEmpty()) {
            logBack.accept("XSLT WARNINGS:");
            for (TransformerException ex : errorsAndWarnings) {
                indent(ex.getMessage(), 2, logBack);
            }
            logBack.accept(String.format("===================================================%s", System.lineSeparator()));
        }


        if (!errors.isEmpty()) {
            logBack.accept("FAILURES:");
            for (AssertionError assertionError : errors) {
                indent(assertionError.getMessage(), 2, logBack);
            }
            logBack.accept(String.format("===================================================%s", System.lineSeparator()));

            throw errors.get(0);
        }

    }


    private void initialize(TransformAssertWithTransformer transformAssertWithTransformer) {

        logBack.accept("DESCRIBE:");
        indent(transformAssertWithTransformer.getXsltPath() != null
                ? transformAssertWithTransformer.getXsltPath()
                : transformAssertWithTransformer.getXsltString(), 2, logBack);

        logBack.accept(System.lineSeparator() + "WHEN TRANSFORMING:");
        indent(transformAssertWithTransformer.getSourceXmlPath() != null
                ? transformAssertWithTransformer.getSourceXmlPath()
                : transformAssertWithTransformer.getSourceXmlString(), 2, logBack);

        logBack.accept(System.lineSeparator() + "IT SHOULD:");
    }


    private String mkRule(String defaultRule, String[] rule) {
        return rule.length > 0 ? rule[0] : defaultRule;
    }

    private void indent(String lines, int whitespace, Consumer<String> logBack) {

        for(String line : lines.split("\\r\\n|\\n|\\r")) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < whitespace; i++) {
                sb.append(" ");
            }
            logBack.accept(sb.append(line).toString());
        }
    }
}
