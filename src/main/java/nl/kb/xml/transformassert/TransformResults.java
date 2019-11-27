package nl.kb.xml.transformassert;

import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;

/**
 * This interface
 */
public interface TransformResults {

    /**
     * Declares a namespace to enable xpath assertions
     * @param key the namespace key
     * @param value the namespace URI
     * @return instance of {@link TransformResults}
     */
    TransformResults usingNamespace(String key, String value);

    /**
     * Declares a namespace to enable xpath assertions
     * @param key the namespace key
     * @param value the namespace URI
     * @return instance of {@link TransformResults}
     */
    TransformResults andUsingNamespace(String key, String value);

    /**
     * Executes all evaluations<br>
     * Do <i>not</i> print xslt warnings!<br>
     * Print failures to standard output
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     */
    void evaluate() throws UnsupportedEncodingException;

    /**
     * Executes all evaluations<br>
     * Do <i>not</i> print xslt warnings!
     * @param failureConsumer print failures to custom {@link String} {@link Consumer}
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     */
    void evaluate(Consumer<String> failureConsumer) throws UnsupportedEncodingException;

    /**
     * Executes all evaluations<br>
     * Print failures to standard output
     * @param listXsltWarnings pass true to list xslt warnings!
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     */
    void evaluate(boolean listXsltWarnings) throws UnsupportedEncodingException;

    /**
     * Executes all evaluations
     * @param listXsltWarnings pass true to list xslt warnings!
     * @param failureConsumer print failures to custom {@link String} {@link Consumer}
     * @throws UnsupportedEncodingException when UTF-8 is not supported
     */
    void evaluate(boolean listXsltWarnings, Consumer<String> failureConsumer) throws UnsupportedEncodingException;
}
