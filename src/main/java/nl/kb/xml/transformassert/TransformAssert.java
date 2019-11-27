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

public class TransformAssert {

    private TransformAssert() {

    }

    public static TransformAssertWithTransformer describe(File xsltFile, Consumer<String> logBack, Consumer<String> transformationOutput)
            throws TransformerException {
        final TransformAssertWithTransformer transformAssertWithTransformer =
                new TransformAssertWithTransformer(logBack, transformationOutput);

        transformAssertWithTransformer.setXsltSource(new StreamSource(xsltFile));
        transformAssertWithTransformer.setXsltPath(xsltFile.getAbsolutePath());

        return transformAssertWithTransformer;
    }

    public static TransformAssertWithTransformer describe(String xslt, Consumer<String> logBack, Consumer<String> transformationOutput)
            throws UnsupportedEncodingException, TransformerException {

        final TransformAssertWithTransformer transformAssertWithTransformer =
                new TransformAssertWithTransformer(logBack, transformationOutput);
        final Reader reader = new InputStreamReader(new ByteArrayInputStream(xslt.getBytes()), StandardCharsets.UTF_8.name());

        transformAssertWithTransformer.setXsltSource(new StreamSource(reader));
        transformAssertWithTransformer.setXsltString(xslt);

        return transformAssertWithTransformer;
    }

    public static TransformAssertWithTransformer describe(File xsltFile, Consumer<String> logBack)
            throws TransformerException {

        return describe(xsltFile, logBack, null);
    }

    public static TransformAssertWithTransformer describe(String xslt, Consumer<String> logBack)
            throws UnsupportedEncodingException, TransformerException {

        return describe(xslt, logBack, null);
    }

    public static TransformAssertWithTransformer describe(File xsltFile)
            throws TransformerException {

        return describe(xsltFile, System.out::println);
    }

    public static TransformAssertWithTransformer describe(String xslt)
            throws UnsupportedEncodingException, TransformerException {
        return describe(xslt, System.out::println);
    }


}
