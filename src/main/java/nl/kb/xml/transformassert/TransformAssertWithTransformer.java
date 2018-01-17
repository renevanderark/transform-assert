package nl.kb.xml.transformassert;

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
import java.util.function.Consumer;

public class TransformAssertWithTransformer {

    private final Consumer<String> logBack;
    private String sourceXmlPath;
    private String sourceXmlString;

    private String xsltPath;
    private String xsltString;
    private StreamSource xsltSource;
    private Templates templates;
    private RelativePathUriResolver uriResolver;

    TransformAssertWithTransformer(Consumer<String> logBack) {
        this.logBack = logBack;
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


    private TransformAssertWithTransformResult transform(Reader reader, String... parameters) throws TransformerException {
        assert parameters.length % 2 == 0;

        final StreamSource sourceXml = new StreamSource(reader);
        final Templates templates = getTemplates();
        final Transformer transformer = templates.newTransformer();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (uriResolver != null) {
            transformer.setURIResolver(this.uriResolver);
        }
        for (int i = 0; i < parameters.length; i += 2) {
            transformer.setParameter(parameters[i], parameters[i + 1]);
        }
        transformer.transform(sourceXml, new StreamResult(out));

        return new TransformAssertWithTransformResult(this, out.toByteArray());
    }

    private Templates getTemplates() throws TransformerConfigurationException {
        if (this.templates == null) {
            final TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
            if (xsltPath != null) {
                final String xsltDir = xsltPath
                        .replaceAll(System.getProperty("user.dir") + "/", "")
                        .replaceAll("[^/]*$", "");

                uriResolver = new RelativePathUriResolver(xsltDir);
                factory.setURIResolver(uriResolver);
            }

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
}
