package nl.kb.xml.transformassert;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nl.kb.xml.transformassert.ResultStatus.FAILED;
import static nl.kb.xml.transformassert.ResultStatus.OK;

public class TransformAssertWithTransformResult implements TransformResults {

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

        final String report = LogUtil.mkRule("EQUAL: " + expected, rule);

        if (!stringResult.equals(expected)) {
            errors.add(new AssertionError(String.format(
                    report + System.lineSeparator() +
                            "  Expected output to equal: '%s'" + System.lineSeparator() +
                            "  But got: '%s'" + System.lineSeparator()
                    , expected, stringResult
            )));
            LogUtil.indent(String.format("%s (%s)", report, FAILED), 2, logBack);
        } else {
            LogUtil.indent(String.format("%s (%s)", report, OK), 2, logBack);
        }

        return this;
    }

    private TransformAssertWithTransformResult matchXPath(String xPath, String expected, boolean negate, String... rule)
            throws XPathExpressionException, ParserConfigurationException, IOException {

        final String report = LogUtil.mkRule(
                (negate ? "NOT MATCH XPATH " : "MATCH XPATH ") + xPath + "='" + expected + "'"
                , rule);

        try {
            final List<String> stringResult = XpathUtil.getXpathResult(xPath, namespaces, transformationOutput);
            if (stringResult.contains(expected) == negate) {
                final String actual = stringResult.size() == 1
                        ? stringResult.get(0)
                        : stringResult.size() == 0
                        ? ""
                        : "any of: " + stringResult;
                errors.add(new AssertionError(String.format(
                        report + System.lineSeparator() +
                                "  Expected xpath %s%sto match: '%s'" + System.lineSeparator() +
                                "  But got: '%s'" + System.lineSeparator()
                        , xPath, negate ? " NOT " : " ", expected, actual
                )));

                LogUtil.indent(String.format("%s (%s)", report, FAILED), 2, logBack);
            } else {
                LogUtil.indent(String.format("%s (%s)", report, OK), 2, logBack);
            }


            return this;
        } catch (SAXException e) {
            errors.add(new AssertionError("Got unparsable XML output from stylesheet"));
            return this;
        }
    }


    public TransformAssertWithTransformResult hasXpathContaining(String xPath, String expected, String... rule)
            throws XPathExpressionException, ParserConfigurationException, IOException {

        return matchXPath(xPath, expected, false, rule);
    }

    public TransformAssertWithTransformResult doesNothaveXpathContaining(String xPath, String expected, String... rule)
            throws XPathExpressionException, ParserConfigurationException, IOException {

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

        final String report = LogUtil.mkRule("VALIDATE AGAINST XSD: " + xsd.getAbsolutePath(), rule);

        try {
            validator.validate(xmlSource);
            LogUtil.indent(String.format("%s (%s)", report, OK), 2, logBack);
        } catch (Exception e) {
            errors.add(new AssertionError(String.format(
                    report + System.lineSeparator() +
                            "  Expected output to validate against XSD: %s" + System.lineSeparator() +
                            "  But got: %s" + System.lineSeparator(),
                    xsd.getAbsolutePath(),
                    e.getMessage()
            )));
            LogUtil.indent(String.format("%s (%s)", report, FAILED), 2, logBack);
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
        LogUtil.indent(new String(transformationOutput, StandardCharsets.UTF_8.name()), indent, outConsumer);

        logBack.accept(String.format("===================================================%s", System.lineSeparator()));

        if (listXsltWarnings && !errorsAndWarnings.isEmpty()) {
            logBack.accept("XSLT WARNINGS:");
            for (TransformerException ex : errorsAndWarnings) {
                LogUtil.indent(ex.getMessage(), 2, logBack);
            }
            logBack.accept(String.format("===================================================%s", System.lineSeparator()));
        }


        if (!errors.isEmpty()) {
            logBack.accept("FAILURES:");
            for (AssertionError assertionError : errors) {
                LogUtil.indent(assertionError.getMessage(), 2, logBack);
            }
            logBack.accept(String.format("===================================================%s", System.lineSeparator()));

            throw errors.get(0);
        }

    }


    private void initialize(TransformAssertWithTransformer transformAssertWithTransformer) {

        logBack.accept("DESCRIBE:");
        LogUtil.indent(transformAssertWithTransformer.getXsltPath() != null
                ? transformAssertWithTransformer.getXsltPath()
                : transformAssertWithTransformer.getXsltString(), 2, logBack);

        logBack.accept(System.lineSeparator() + "WHEN TRANSFORMING:");
        LogUtil.indent(transformAssertWithTransformer.getSourceXmlPath() != null
                ? transformAssertWithTransformer.getSourceXmlPath()
                : transformAssertWithTransformer.getSourceXmlString(), 2, logBack);

        logBack.accept(System.lineSeparator() + "IT SHOULD:");
    }


}
