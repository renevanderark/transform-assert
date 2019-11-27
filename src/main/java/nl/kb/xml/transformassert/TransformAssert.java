package nl.kb.xml.transformassert;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Entrypoint for the transform-assert library.<br>
 * To effectively walk through this documentation, start out at {@link #describe(File)} and follow
 * the return references from each method.
 */
public class TransformAssert {

    private TransformAssert() {

    }

    /**
     * Declares the xslt file to be tested
     * @param xsltFile the xslt {@link File}
     * @param logBack custom {@link String} {@link Consumer} for log messages
     * @param transformationOutput custom {@link String} {@link Consumer} for the xslt transformation output
     * @return instance of {@link TransformAssertWithTransformer}
     * @throws TransformerException when the xslt cannot be parsed by Saxon
     */
    public static TransformAssertWithTransformer describe(File xsltFile, Consumer<String> logBack, Consumer<String> transformationOutput)
            throws TransformerException {
        final TransformAssertWithTransformer transformAssertWithTransformer =
                new TransformAssertWithTransformer(logBack, transformationOutput);

        transformAssertWithTransformer.setXsltSource(new StreamSource(xsltFile));
        transformAssertWithTransformer.setXsltPath(xsltFile.getAbsolutePath());

        return transformAssertWithTransformer;
    }

    /**
     * Declares the xslt {@link String} to be tested
     * @param xslt the xslt {@link String}
     * @param logBack custom {@link String} {@link Consumer} for log messages
     * @param transformationOutput custom {@link String} {@link Consumer} for the xslt transformation output
     * @return instance of {@link TransformAssertWithTransformer}
     * @throws UnsupportedEncodingException when the character set of the xslt {@link String} ia not supported
     * @throws TransformerException when the xslt cannot be parsed by Saxon
     */
    public static TransformAssertWithTransformer describe(String xslt, Consumer<String> logBack, Consumer<String> transformationOutput)
            throws UnsupportedEncodingException, TransformerException {

        final TransformAssertWithTransformer transformAssertWithTransformer =
                new TransformAssertWithTransformer(logBack, transformationOutput);
        final Reader reader = new InputStreamReader(new ByteArrayInputStream(xslt.getBytes()), StandardCharsets.UTF_8.name());

        transformAssertWithTransformer.setXsltSource(new StreamSource(reader));
        transformAssertWithTransformer.setXsltString(xslt);

        return transformAssertWithTransformer;
    }

    /**
     * Declares the xslt file to be tested<br>
     * Prints xslt transformation output to standard output
     * @param xsltFile the xslt {@link File}
     * @param logBack custom {@link String} {@link Consumer} for log messages
     * @return instance of {@link TransformAssertWithTransformer}
     * @throws TransformerException when the xslt cannot be parsed by Saxon
     */
    public static TransformAssertWithTransformer describe(File xsltFile, Consumer<String> logBack)
            throws TransformerException {

        return describe(xsltFile, logBack, null);
    }

    /**
     * Declares the xslt {@link String} to be tested<br>
     * Prints xslt transformation output to standard output
     * @param xslt the xslt {@link String}
     * @param logBack custom {@link String} {@link Consumer} for log messages
     * @return instance of {@link TransformAssertWithTransformer}
     * @throws UnsupportedEncodingException when the character set of the xslt {@link String} ia not supported
     * @throws TransformerException when the xslt cannot be parsed by Saxon
     */
    public static TransformAssertWithTransformer describe(String xslt, Consumer<String> logBack)
            throws UnsupportedEncodingException, TransformerException {

        return describe(xslt, logBack, null);
    }

    /**
     * Declares the xslt file to be tested<br>
     * Logs messages to standard output<br>
     * Prints xslt transformation output to standard output
     * @param xsltFile the xslt {@link File}
     * @return instance of {@link TransformAssertWithTransformer}
     * @throws TransformerException when the xslt cannot be parsed by Saxon
     */
    public static TransformAssertWithTransformer describe(File xsltFile)
            throws TransformerException {

        return describe(xsltFile, System.out::println);
    }

    /**
     * Declares the xslt {@link String} to be tested<br>
     * Logs messages to standard output<br>
     * Prints xslt transformation output to standard output
     * @param xslt the xslt {@link String}
     * @return instance of {@link TransformAssertWithTransformer}
     * @throws UnsupportedEncodingException when the character set of the xslt {@link String} ia not supported
     * @throws TransformerException when the xslt cannot be parsed by Saxon
     */
    public static TransformAssertWithTransformer describe(String xslt)
            throws UnsupportedEncodingException, TransformerException {
        return describe(xslt, System.out::println);
    }


}
