package nl.kb.xml.transformassert;

import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.ElementSelectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static nl.kb.xml.transformassert.LogUtil.indent;
import static nl.kb.xml.transformassert.LogUtil.mkRule;
import static nl.kb.xml.transformassert.ResultStatus.FAILED;
import static nl.kb.xml.transformassert.ResultStatus.OK;

public class TransformCompareWithTransformResults implements TransformResults {
    private final byte[] resultFromBaseline;
    private final byte[] resultUnderTest;
    private final Consumer<String> logBack;
    private final Consumer<String> outputConsumer;
    private final List<TransformerException> errorsAndWarnings;
    private XpathEvaluator baselineEvaluator;
    private XpathEvaluator resultEvaluator;
    private List<AssertionError> errors = new ArrayList<>();


    TransformCompareWithTransformResults(TransformCompareWithTransformers transformCompareWithTransformers,
                                         byte[] resultFromBaseline, byte[] resultUnderTest) {

        this.resultFromBaseline = resultFromBaseline;
        this.resultUnderTest = resultUnderTest;

        this.logBack = transformCompareWithTransformers.getLogBack();
        this.outputConsumer = transformCompareWithTransformers.getTransformationOutput();
        this.errorsAndWarnings = transformCompareWithTransformers.getErrorsAndWarnings();
        baselineEvaluator = new XpathEvaluator(resultFromBaseline);
        resultEvaluator = new XpathEvaluator(resultUnderTest);
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
    public void evaluate(boolean listXsltWarnings) throws UnsupportedEncodingException {
        evaluate(listXsltWarnings, null);
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
    public void evaluate(boolean listXsltWarnings, Consumer<String> failureConsumer) throws UnsupportedEncodingException {
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

            if (failureConsumer != null) {
                for (AssertionError assertionError : errors) {
                    failureConsumer.accept(assertionError.getMessage());
                }
            } else {
                throw errors.get(0);
            }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformCompareWithTransformResults usingNamespace(String key, String value) {
        resultEvaluator.addNamespace(key, value);
        baselineEvaluator.addNamespace(key, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformCompareWithTransformResults andUsingNamespace(String key, String value) {
        return usingNamespace(key, value);
    }

    public TransformCompareWithTransformResults outputsIdenticalXml(String... rule) {
        final String report = LogUtil.mkRule(
                "SEMANTICALLY EQUAL BASELINE OUPUT"
                , rule);

        final Diff diff = DiffBuilder.compare(resultFromBaseline).withTest(resultUnderTest)
                .ignoreWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                .checkForSimilar().build();

        if (diff.getDifferences().iterator().hasNext()) {
            LogUtil.indent(String.format("%s (%s)", report, FAILED), 2, logBack);
        } else {
            LogUtil.indent(String.format("%s (%s)", report, OK), 2, logBack);
        }

        for (Difference difference : diff.getDifferences()) {
            errors.add(new AssertionError(report +
                    System.lineSeparator() +
                    difference
            ));
        }


        return this;
    }

    public TransformCompareWithTransformResults hasMatchingXPathResultsFor(String xPath, String... rule) throws XPathExpressionException {
        final List<String> stringResults;
        final List<String> expected;

        try {
            baselineEvaluator.loadDocument();
        } catch (SAXException | IOException | ParserConfigurationException e) {
            errors.add(new AssertionError("Got unparsable XML output from baseline stylesheet"));
            return this;
        }

        try {
            resultEvaluator.loadDocument();
        } catch (SAXException | IOException | ParserConfigurationException e) {
            errors.add(new AssertionError("Got unparsable XML output from stylesheet under test"));
            return this;
        }

        stringResults = resultEvaluator.getXpathResult(xPath);
        expected = baselineEvaluator.getXpathResult(xPath);


        if (stringResults.size() > expected.size()) {
            errors.add(new AssertionError("MATCH XPATH " + xPath + String.format(
                    System.lineSeparator() +
                            "  Expected xpath %s to result in: %d items" + System.lineSeparator() +
                            "  But got: %d items" + System.lineSeparator()
                    , xPath, expected.size(), stringResults.size()
            )));
        }


        for (String expectedResult : expected) {
            final String report = LogUtil.mkRule(
                    "MATCH XPATH " + xPath + "='" + expectedResult + "'"
                    , rule);

            if (!stringResults.contains(expectedResult)) {
                final String actual = stringResults.size() == 1
                        ? stringResults.get(0)
                        : stringResults.size() == 0
                        ? ""
                        : "any of: " + stringResults;


                errors.add(new AssertionError(report + String.format(
                        System.lineSeparator() +
                                "  Expected xpath %s to match: '%s'" + System.lineSeparator() +
                                "  But got: '%s'" + System.lineSeparator()
                        , xPath, expectedResult, actual
                )));

                LogUtil.indent(String.format("%s (%s)", report, FAILED), 2, logBack);
            } else {
                LogUtil.indent(String.format("%s (%s)", report, OK), 2, logBack);
            }
        }

        return this;
    }
}
