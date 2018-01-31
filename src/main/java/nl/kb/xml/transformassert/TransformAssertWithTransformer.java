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


    public TransformAssertWithTransformResult whenTransforming(File xmlFile, String... parameters) throws FileNotFoundException, UnsupportedEncodingException, TransformerException {
        final Reader reader = new InputStreamReader(new FileInputStream(xmlFile), StandardCharsets.UTF_8.name());
        this.sourceXmlPath = xmlFile.getAbsolutePath();
        return transform(reader, parameters);
    }

    public TransformAssertWithTransformResult whenTransforming(String xml, String... parameters) throws FileNotFoundException, UnsupportedEncodingException, TransformerException {
        final Reader reader = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()), StandardCharsets.UTF_8.name());
        this.sourceXmlString = xml;
        return transform(reader, parameters);
    }

    public TransformCompareWithTransformers whenComparingTo(File xsltFile) throws FileNotFoundException, UnsupportedEncodingException {
        final TransformAssertWithTransformer transformAssertWithTransformer =
                new TransformAssertWithTransformer(logBack, transformationOutput);
        final Reader reader = new InputStreamReader(new FileInputStream(xsltFile), StandardCharsets.UTF_8.name());

        transformAssertWithTransformer.setXsltSource(new StreamSource(reader));
        transformAssertWithTransformer.setXsltPath(xsltFile.getAbsolutePath());
        return new TransformCompareWithTransformers(this, transformAssertWithTransformer);
    }

    public TransformCompareWithTransformers whenComparingTo(String xslt) throws UnsupportedEncodingException {

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
        final Templates templates = getTemplates();
        final Transformer transformer = templates.newTransformer();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < parameters.length; i += 2) {
            transformer.setParameter(parameters[i], parameters[i + 1]);
        }

        ((net.sf.saxon.Controller)transformer).setErrorListener(new ErrorListener() {
            @Override
            public void warning(TransformerException exception) throws TransformerException {
                errorsAndWarnings.add(exception);
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
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

    private Templates getTemplates() throws TransformerConfigurationException {
        if (this.templates == null) {
            final TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
/*
            if (xsltPath != null) {
                final String xsltDir = xsltPath
                        .replace(System.getProperty("user.dir"), "")
                        .replaceAll("^[/\\\\]", "")
                        .replaceAll("[^/\\\\]*$", "");

                uriResolver = new RelativePathUriResolver(xsltDir);
                factory.setURIResolver(uriResolver);
            }
*/

            templates = factory.newTemplates(xsltSource);
            return templates;
        } else {
            return templates;
        }
    }



    void setXsltPath(String xsltPath) {
        this.xsltPath = xsltPath;
    }

    void setXsltString(String xsltString) {
        this.xsltString = xsltString;
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
