package nl.kb.xml.transformassert;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * An instance of this class is returned by {@link TransformAssert#describe(File)}
 * <p>It exposes methods to either:</p>
 * <ul>
 * <li>Declare the XML file to be transformed by the XSLT declared there:
 * <ul>
 *     <li>{@link #whenTransforming(File, String...)}</li>
 *     <li>{@link #whenTransforming(String, String...)}</li>
 * </ul>
 *
 * </li>
 * <li>Compare the output of the XSLT declared in there to the output of another XSLT:
 * <ul>
 *     <li>{@link #whenComparingTo(File)}</li>
 *     <li>{@link #whenComparingTo(String)}</li>
 * </ul>
 * </li>
 * </ul>
 *
 */
public class TransformAssertWithTransformer {
    private final List<TransformerException> errorsAndWarnings = new ArrayList<>();
    private final Consumer<String> logBack;
    private final Consumer<String> transformationOutput;
    private String sourceXmlPath;
    private String sourceXmlString;

    private String xsltPath;
    private String xsltString;
    private StreamSource xsltSource;
    private Templates templates;

    TransformAssertWithTransformer(Consumer<String> logBack, Consumer<String> transformationOutput) {
        this.logBack = logBack;
        this.transformationOutput = transformationOutput;
    }

    /**
     * Declares the xml {@link File} to be transformed
     * @param xmlFile the xml {@link File}
     * @param parameters tuples of XSLT {@link String}-parameters
     * @return instance of {@link TransformAssertWithTransformResult}
     * @throws FileNotFoundException when the XML is not found
     * @throws UnsupportedEncodingException when the UTF-8 charset is not supported
     * @throws TransformerException when the XML file cannot be parsed by Saxon
     */
    public TransformAssertWithTransformResult whenTransforming(File xmlFile, String... parameters) throws FileNotFoundException, UnsupportedEncodingException, TransformerException {
        final Reader reader = new InputStreamReader(new FileInputStream(xmlFile), StandardCharsets.UTF_8.name());
        this.sourceXmlPath = xmlFile.getAbsolutePath();
        return transform(reader, parameters);
    }

    /**
     * Declares the xml {@link String} to be transformed
     * @param xml the xml {@link String}
     * @param parameters tuples of XSLT {@link String}-parameters
     * @return instance of {@link TransformAssertWithTransformResult}
     * @throws UnsupportedEncodingException when the UTF-8 charset is not supported
     * @throws TransformerException when the XML file cannot be parsed by Saxon
     */
    public TransformAssertWithTransformResult whenTransforming(String xml, String... parameters) throws UnsupportedEncodingException, TransformerException {
        final Reader reader = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()), StandardCharsets.UTF_8.name());
        this.sourceXmlString = xml;
        return transform(reader, parameters);
    }

    /**
     * Declares another xslt {@link File} of which the output will be compared to the xslt under test
     * @param xsltFile the xslt {@link File} to compare to
     * @return instance of {@link TransformCompareWithTransformers}
     * @throws FileNotFoundException when the xslt file cannout be found
     * @throws UnsupportedEncodingException when the UTF-8 charset is not supported
     * @throws TransformerConfigurationException when the xslt file cannot be parsed by Saxon
     */
    public TransformCompareWithTransformers whenComparingTo(File xsltFile) throws FileNotFoundException, UnsupportedEncodingException, TransformerConfigurationException {
        final TransformAssertWithTransformer transformAssertWithTransformer =
                new TransformAssertWithTransformer(logBack, transformationOutput);
        final Reader reader = new InputStreamReader(new FileInputStream(xsltFile), StandardCharsets.UTF_8.name());

        transformAssertWithTransformer.setXsltSource(new StreamSource(reader));
        transformAssertWithTransformer.setXsltPath(xsltFile.getAbsolutePath());
        return new TransformCompareWithTransformers(this, transformAssertWithTransformer);
    }

    /**
     * Declares another xslt {@link String} of which the output will be compared to the xslt under test
     * @param xslt the xslt {@link String} to compare to
     * @return instance of {@link TransformCompareWithTransformers}
     * @throws UnsupportedEncodingException when the UTF-8 charset is not supported
     * @throws TransformerConfigurationException when the xslt file cannot be parsed by Saxon
     */
    public TransformCompareWithTransformers whenComparingTo(String xslt) throws UnsupportedEncodingException, TransformerConfigurationException {

        final TransformAssertWithTransformer transformAssertWithTransformer =
                new TransformAssertWithTransformer(logBack, transformationOutput);
        final Reader reader = new InputStreamReader(new ByteArrayInputStream(xslt.getBytes()), StandardCharsets.UTF_8.name());

        transformAssertWithTransformer.setXsltSource(new StreamSource(reader));
        transformAssertWithTransformer.setXsltString(xslt);

        return new TransformCompareWithTransformers(this, transformAssertWithTransformer);
    }

    byte[] getTransformResult(Reader reader, String... parameters) throws TransformerException {
        assert parameters.length % 2 == 0;

        final StreamSource sourceXml = new StreamSource(reader);
        final Transformer transformer = templates.newTransformer();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < parameters.length; i += 2) {
            transformer.setParameter(parameters[i], parameters[i + 1]);
        }

        transformer.setErrorListener(new ErrorListener() {
            @Override
            public void warning(TransformerException exception) {
                errorsAndWarnings.add(exception);
            }

            @Override
            public void error(TransformerException exception) {
                errorsAndWarnings.add(exception);
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                throw exception;
            }
        });
        transformer.transform(sourceXml, new StreamResult(out));
        return out.toByteArray();
    }

    private TransformAssertWithTransformResult transform(Reader reader, String... parameters) throws TransformerException {
        return new TransformAssertWithTransformResult(this,
                getTransformResult(reader, parameters));
    }

    private void getTemplates() throws TransformerConfigurationException {
        final TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
        templates = factory.newTemplates(xsltSource);
    }


    void setXsltPath(String xsltPath) throws TransformerConfigurationException {

        this.xsltPath = xsltPath;
        getTemplates();
    }

    void setXsltString(String xsltString) throws TransformerConfigurationException {
        this.xsltString = xsltString;
        getTemplates();
    }

    String getSourceXmlPath() {
        return sourceXmlPath;
    }

    String getSourceXmlString() {
        return sourceXmlString;
    }

    String getXsltPath() {
        return xsltPath;
    }

    String getXsltString() {
        return xsltString;
    }

    void setXsltSource(StreamSource xsltSource) {
        this.xsltSource = xsltSource;
    }

    Consumer<String> getLogBack() {
        return logBack;
    }

    Consumer<String> getTransformationOutput() {
        return transformationOutput;
    }

    List<TransformerException> getErrorsAndWarnings() {
        return errorsAndWarnings;
    }
}
