package nl.kb.xml.transformassert;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nl.kb.xml.transformassert.LogUtil.indent;
import static nl.kb.xml.transformassert.LogUtil.mkRule;
import static nl.kb.xml.transformassert.ResultStatus.FAILED;
import static nl.kb.xml.transformassert.ResultStatus.OK;

public class TransformCompareWithTransformResults {
    private final TransformCompareWithTransformers transformCompareWithTransformers;
    private final byte[] resultFromBaseline;
    private final byte[] resultUnderTest;
    private final Consumer<String> logBack;
    private final Consumer<String> outputConsumer;
    private final List<TransformerException> errorsAndWarnings;
    private List<AssertionError> errors = new ArrayList<>();
    private Map<String, String> namespaces = new HashMap<>();


    public TransformCompareWithTransformResults(TransformCompareWithTransformers transformCompareWithTransformers,
                                                byte[] resultFromBaseline, byte[] resultUnderTest) {

        this.transformCompareWithTransformers = transformCompareWithTransformers;
        this.resultFromBaseline = resultFromBaseline;
        this.resultUnderTest = resultUnderTest;

        this.logBack = transformCompareWithTransformers.getLogBack();
        this.outputConsumer = transformCompareWithTransformers.getTransformationOutput();
        this.errorsAndWarnings = transformCompareWithTransformers.getErrorsAndWarnings();

        initialize(transformCompareWithTransformers);

    }

    private void initialize(TransformCompareWithTransformers transformCompareWithTransformers) {
        logBack.accept("DESCRIBE:");
        indent(transformCompareWithTransformers.getUnderTest().getXsltPath() != null
                ? transformCompareWithTransformers.getUnderTest().getXsltPath()
                : transformCompareWithTransformers.getUnderTest().getXsltString(), 2, logBack);

        logBack.accept("WHEN COMPARING TO:");
        indent(transformCompareWithTransformers.getBaseline().getXsltPath() != null
                ? transformCompareWithTransformers.getBaseline().getXsltPath()
                : transformCompareWithTransformers.getBaseline().getXsltString(), 2, logBack);

        logBack.accept(System.lineSeparator() + "WHEN TRANSFORMING:");
        indent(transformCompareWithTransformers.getSourceXmlPath() != null
                ? transformCompareWithTransformers.getSourceXmlPath()
                : transformCompareWithTransformers.getSourceXmlString(), 2, logBack);

        logBack.accept(System.lineSeparator() + "IT SHOULD:");
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
        LogUtil.indent(new String(resultUnderTest, StandardCharsets.UTF_8.name()), indent, outConsumer);

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

    public TransformCompareWithTransformResults hasEqualOutputs(String... rule) throws UnsupportedEncodingException {
        final String expected = new String(resultFromBaseline, StandardCharsets.UTF_8.name());
        final String stringResult = new String(resultUnderTest, StandardCharsets.UTF_8.name());

        final String report = mkRule("EQUAL: " + expected, rule);

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


    public TransformCompareWithTransformResults usingNamespace(String key, String value) {
        this.namespaces.put(key, value);
        return this;
    }


    public TransformCompareWithTransformResults hasMatchingXPathResultsFor(String xPath, String... rule) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        final String stringResult = XpathUtil.getXpathResult(xPath, namespaces, resultUnderTest);
        final String expected = XpathUtil.getXpathResult(xPath, namespaces, resultFromBaseline);

        final String report = LogUtil.mkRule(
                "MATCH XPATH " + xPath + "='" + expected + "'"
                , rule);

        if (!stringResult.equals(expected)) {
            errors.add(new AssertionError(String.format(
                    report + System.lineSeparator() +
                            "  Expected xpath %s to match: '%s'" + System.lineSeparator() +
                            "  But got: '%s'" + System.lineSeparator()
                    , xPath, expected, stringResult
            )));

            LogUtil.indent(String.format("%s (%s)", report, FAILED), 2, logBack);
        } else {
            LogUtil.indent(String.format("%s (%s)", report, OK), 2, logBack);
        }


        return this;
    }
}
