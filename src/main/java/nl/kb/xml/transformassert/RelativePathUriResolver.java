package nl.kb.xml.transformassert;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

public class RelativePathUriResolver implements URIResolver {
    private final File xsltDir;

    RelativePathUriResolver(String xsltDir) {
        this.xsltDir = new File(xsltDir);
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        try {
            if (Paths.get(href).isAbsolute()) {
                return new StreamSource(new FileInputStream(href));
            } else {

                return new StreamSource(new FileInputStream(new File(xsltDir, href)));
            }
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
