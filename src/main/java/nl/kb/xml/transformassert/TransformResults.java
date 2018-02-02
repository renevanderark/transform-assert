package nl.kb.xml.transformassert;

import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;

public interface TransformResults {

    TransformResults usingNamespace(String key, String value);
    TransformResults andUsingNamespace(String key, String value);
    void evaluate() throws UnsupportedEncodingException;
    void evaluate(Consumer<String> failureConsumer) throws UnsupportedEncodingException;
    void evaluate(boolean listXsltWarnings) throws UnsupportedEncodingException;
    void evaluate(boolean listXsltWarnings, Consumer<String> failureConsumer) throws UnsupportedEncodingException;
}
