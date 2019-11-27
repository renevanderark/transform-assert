package nl.kb.xml.transformassert;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * An instance of this class is returned by {@link TransformAssertWithTransformer#whenComparingTo(File)}
 * <p>It exposes methods to declare the xml {@link File} or {@link String} to run both stylesheets against</p>
 */
public class TransformCompareWithTransformers {
    private final TransformAssertWithTransformer underTest;
    private final TransformAssertWithTransformer baseline;
    private String sourceXmlPath;
    private String sourceXmlString;

    TransformCompareWithTransformers(TransformAssertWithTransformer underTest, TransformAssertWithTransformer baseline) {
        this.underTest = underTest;
        this.baseline = baseline;
    }


    /**
     * Declares the xml {@link File} to be transformed by both XSTL stylesheets
     * @param xmlFile the xml {@link File}
     * @param parameters tuples of XSLT {@link String}-parameters
     * @return instance of {@link TransformCompareWithTransformResults}
     * @throws FileNotFoundException when the XML is not found
     * @throws UnsupportedEncodingException when the UTF-8 charset is not supported
     * @throws TransformerException when the XML file cannot be parsed by Saxon
     */
    public TransformCompareWithTransformResults whenTransforming(File xmlFile, String... parameters) throws FileNotFoundException, UnsupportedEncodingException, TransformerException {
        final Reader reader1 = new InputStreamReader(new FileInputStream(xmlFile), StandardCharsets.UTF_8.name());
        final Reader reader2 = new InputStreamReader(new FileInputStream(xmlFile), StandardCharsets.UTF_8.name());
        this.sourceXmlPath = xmlFile.getAbsolutePath();

        final byte[] resultUnderTest = underTest.getTransformResult(reader1, parameters);
        final byte[] resultFromBaseline = baseline.getTransformResult(reader2, parameters);

        return new TransformCompareWithTransformResults(this, resultFromBaseline, resultUnderTest);
    }

    /**
     * Declares the xml {@link String} to be transformed by both XSLT stylesheets
     * @param xml the xml {@link String}
     * @param parameters tuples of XSLT {@link String}-parameters
     * @return instance of {@link TransformCompareWithTransformResults}
     * @throws UnsupportedEncodingException when the UTF-8 charset is not supported
     * @throws TransformerException when the XML file cannot be parsed by Saxon
     */
    public TransformCompareWithTransformResults whenTransforming(String xml, String... parameters) throws UnsupportedEncodingException, TransformerException {

        Reader reader1 = null;
        Reader reader2 = null;
        try {
            reader1 = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()), StandardCharsets.UTF_8.name());
            reader2 = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()), StandardCharsets.UTF_8.name());
            this.sourceXmlString = xml;

            final byte[] resultUnderTest = underTest.getTransformResult(reader1, parameters);
            final byte[] resultFromBaseline = baseline.getTransformResult(reader2, parameters);

            return new TransformCompareWithTransformResults(this, resultFromBaseline, resultUnderTest);
        } finally {
            try {  if (reader1 != null) { reader1.close(); } } catch (IOException e) { e.printStackTrace(); }
            try {  if (reader2 != null) { reader2.close(); } } catch (IOException e) { e.printStackTrace(); }
        }
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

    TransformAssertWithTransformer getUnderTest() {
        return underTest;
    }

    String getSourceXmlPath() {
        return sourceXmlPath;
    }

    String getSourceXmlString() {
        return sourceXmlString;
    }

    TransformAssertWithTransformer getBaseline() {
        return baseline;
    }
}
