package nl.kb.xml.transformassert;

import java.io.UnsupportedEncodingException;

public interface TransformResults {

    TransformResults usingNamespace(String key, String value);
    TransformResults andUsingNamespace(String key, String value);
    void evaluate() throws UnsupportedEncodingException;
    void evaluate(boolean listXsltWarnings) throws UnsupportedEncodingException;
}
