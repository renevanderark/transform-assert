package nl.kb.xml.transformassert;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class TransformCompareWithTransformers {
    private final TransformAssertWithTransformer underTest;
    private final TransformAssertWithTransformer baseline;
    private String sourceXmlPath;
    private String sourceXmlString;

    TransformCompareWithTransformers(TransformAssertWithTransformer underTest, TransformAssertWithTransformer baseline) {
        this.underTest = underTest;
        this.baseline = baseline;
    }

    public TransformCompareWithTransformResults whenTransforming(File xmlFile, String... parameters) throws FileNotFoundException, UnsupportedEncodingException, TransformerException {
        final Reader reader1 = new InputStreamReader(new FileInputStream(xmlFile), StandardCharsets.UTF_8.name());
        final Reader reader2 = new InputStreamReader(new FileInputStream(xmlFile), StandardCharsets.UTF_8.name());
        this.sourceXmlPath = xmlFile.getAbsolutePath();

        final byte[] resultUnderTest = underTest.getTransformResult(reader1, parameters);
        final byte[] resultFromBaseline = baseline.getTransformResult(reader2, parameters);

        return new TransformCompareWithTransformResults(this, resultFromBaseline, resultUnderTest);
    }

    public TransformCompareWithTransformResults whenTransforming(String xml, String... parameters) throws UnsupportedEncodingException, TransformerException {
        final Reader reader1 = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()), StandardCharsets.UTF_8.name());
        final Reader reader2 = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()), StandardCharsets.UTF_8.name());
        this.sourceXmlString = xml;

        final byte[] resultUnderTest = underTest.getTransformResult(reader1, parameters);
        final byte[] resultFromBaseline = baseline.getTransformResult(reader2, parameters);

        return new TransformCompareWithTransformResults(this, resultFromBaseline, resultUnderTest);
    }

    Consumer<String> getLogBack() {
        return underTest.getLogBack();
    }

    Consumer<String> getTransformationOutput() {
        return underTest.getTransformationOutput();
    }

    List<TransformerException> getErrorsAndWarnings() {
        return underTest.getErrorsAndWarnings();
    }

    public TransformAssertWithTransformer getUnderTest() {
        return underTest;
    }

    public String getSourceXmlPath() {
        return sourceXmlPath;
    }

    public String getSourceXmlString() {
        return sourceXmlString;
    }

    public TransformAssertWithTransformer getBaseline() {
        return baseline;
    }
}
