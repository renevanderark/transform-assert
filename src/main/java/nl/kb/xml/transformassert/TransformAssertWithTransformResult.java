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
import java.util.List;
import java.util.function.Consumer;

import static nl.kb.xml.transformassert.ResultStatus.FAILED;
import static nl.kb.xml.transformassert.ResultStatus.OK;

/**
 * An instance of this class is returned by {@link TransformAssertWithTransformer#whenTransforming(File, String...)}
 * <p>It exposes methods to do assertions on the contents of the output of the XSLT under test</p>
 */
public class TransformAssertWithTransformResult implements TransformResults {

    private final byte[] transformationOutput;
    private final List<AssertionError> errors = new ArrayList<>();
    private final Consumer<String> logBack;
    private final Consumer<String> outputConsumer;
    private final List<TransformerException> errorsAndWarnings;
    private final XpathEvaluator xpathEvaluator;


    TransformAssertWithTransformResult(TransformAssertWithTransformer transformAssertWithTransformer, byte[] transformationOutput) {
        this.transformationOutput = transformationOutput;
        this.logBack = transformAssertWithTransformer.getLogBack();
        this.outputConsumer = transformAssertWithTransformer.getTransformationOutput();
        this.errorsAndWarnings = transformAssertWithTransformer.getErrorsAndWarnings();
        xpathEvaluator = new XpathEvaluator(transformationOutput);
        initialize(transformAssertWithTransformer);
    }

    private TransformAssertWithTransformResult(byte[] xml, Consumer<String> logBack) {
        transformationOutput = xml;
        this.logBack = logBack;
        outputConsumer = null;
        errorsAndWarnings = new ArrayList<>();
        xpathEvaluator = new XpathEvaluator(transformationOutput);
        logBack.accept("DESCRIBING XML");
        logBack.accept(System.lineSeparator() + "IT SHOULD:");
    }

    /**
     * Declares an XML (as byte array) to do assertions on directly
     * @param xml the xml as {@link byte[]}
     * @param logBack custom {@link String} {@link Consumer}
     * @return instance of self exposing assertion methods and evaluate
     */
    public static TransformAssertWithTransformResult describeXml(byte[] xml, Consumer<String> logBack) {
        return new TransformAssertWithTransformResult(xml, logBack);
    }

    /**
     * Declares an XML (as byte array) to do assertions on directly<br>
     * Prints output to standard output
     * @param xml the xml as {@link byte[]}
     * @return instance of self exposing assertion methods and evaluate
     */
    public static TransformAssertWithTransformResult describeXml(byte[] xml) {
        return new TransformAssertWithTransformResult(xml, System.out::println);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TransformAssertWithTransformResult usingNamespace(String key, String value) {
        xpathEvaluator.addNamespace(key, value);
        return this;
    }


    /**
     * Asserts that the output of the xslt transformation is equal to {@link String}
     * @param expected the expected {@link String} value
     * @param rule the name of this assertion
     * @return instance of self exposing assertion methods and {@link #evaluate()}
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     */
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
            throws XPathExpressionException {

        final String report = LogUtil.mkRule(
                (negate ? "NOT MATCH XPATH " : "MATCH XPATH ") + xPath + "='" + expected + "'"
                , rule);
        try {
            xpathEvaluator.loadDocument();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            errors.add(new AssertionError("Got unparsable XML output from stylesheet"));
            return this;
        }

        final List<String> stringResult = xpathEvaluator.getXpathResult(xPath);
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
    }

    /**
     * Asserts that the resulting XML matches expected {@link String} value for given xpath
     * @param xPath the xpath pointing to the expected value
     * @param expected the expected value
     * @param rule name of the assertion
     * @return instance of self exposing assertion methods and evaluate
     * @throws XPathExpressionException when the xpath is not valid, or namespace is not declared in {@link #usingNamespace(String, String)}
     */
    public TransformAssertWithTransformResult hasXpathContaining(String xPath, String expected, String... rule)
            throws XPathExpressionException {

        return matchXPath(xPath, expected, false, rule);
    }

    /**
     * Asserts that the resulting XML is not equal to unexpected {@link String} value for given xpath
     * @param xPath the xpath pointing to the unexpected value
     * @param expected the expected value
     * @param rule name of the assertion
     * @return instance of self exposing assertion methods and evaluate
     * @throws XPathExpressionException when the xpath is not valid, or namespace is not declared in {@link #usingNamespace(String, String)}
     */
    public TransformAssertWithTransformResult doesNothaveXpathContaining(String xPath, String expected, String... rule)
            throws XPathExpressionException {

        return matchXPath(xPath, expected, true, rule);
    }

    /**
     * Asserts that the resulting XML validates against the given xsd {@link File}
     * @param xsd the xsd {@link File}
     * @param rule name of the assertion
     * @return instance of self exposing assertion methods and evaluate
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     * @throws FileNotFoundException when the xsd file is not found
     * @throws SAXException when the xsd file cannot be parsed by Saxon
     */
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

    /**
     * Asserts that the output of the xslt transformation is equal to {@link String}
     * @param expected the expected {@link String} value
     * @param rule the name of this assertion
     * @return instance of self exposing assertion methods and {@link #evaluate()}
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     */
    public TransformAssertWithTransformResult andIsEqualTo(String expected, String... rule) throws UnsupportedEncodingException {
        return isEqualto(expected, rule);
    }

    /**
     * Asserts that the resulting XML matches expected {@link String} value for given xpath
     * @param xPath the xpath pointing to the expected value
     * @param expected the expected value
     * @param rule name of the assertion
     * @return instance of self exposing assertion methods and evaluate
     * @throws XPathExpressionException when the xpath is not valid, or namespace is not declared in {@link #usingNamespace(String, String)}
     */
    public TransformAssertWithTransformResult andHasXpathContaining(String xPath, String expected, String... rule) throws XPathExpressionException {

        return hasXpathContaining(xPath, expected, rule);
    }

    /**
     * Asserts that the resulting XML is not equal to unexpected {@link String} value for given xpath
     * @param xPath the xpath pointing to the unexpected value
     * @param expected the expected value
     * @param rule name of the assertion
     * @return instance of self exposing assertion methods and evaluate
     * @throws XPathExpressionException when the xpath is not valid, or namespace is not declared in {@link #usingNamespace(String, String)}
     */
    public TransformAssertWithTransformResult andDoesNotHaveXpathContaining(String xPath, String expected, String... rule) throws XPathExpressionException {

        return doesNothaveXpathContaining(xPath, expected, rule);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformAssertWithTransformResult andUsingNamespace(String key, String value) {
        return usingNamespace(key, value);
    }

    /**
     * Asserts that the resulting XML validates against the given xsd {@link File}
     * @param xsd the xsd {@link File}
     * @param rule name of the assertion
     * @return instance of self exposing assertion methods and evaluate
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     * @throws FileNotFoundException when the xsd file is not found
     * @throws SAXException when the xsd file cannot be parsed by Saxon
     */
    public TransformAssertWithTransformResult andValidatesAgainstXSD(File xsd, String... rule) throws FileNotFoundException, UnsupportedEncodingException, SAXException {
        return validatesAgainstXSD(xsd, rule);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evaluate() throws UnsupportedEncodingException {
        evaluate(false, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evaluate(Consumer<String> failureConsumer) throws UnsupportedEncodingException {
        evaluate(false, failureConsumer);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evaluate(boolean listXsltWarnings) throws UnsupportedEncodingException {
        evaluate(listXsltWarnings, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evaluate(boolean listXsltWarnings, Consumer<String> failureConsumer) throws UnsupportedEncodingException {
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
