package nl.kb.xml.transformassert;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class XpathEvaluator {
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private final byte[] transformationOutput;
    private final Map<String, String> namespaces = new HashMap<>();
    private Document doc = null;
    private final XPathFactoryImpl xPathFactory;

    static {
        dbf.setNamespaceAware(true);
    }

    XpathEvaluator(byte[] transformationOutput)  {
        this.xPathFactory = new XPathFactoryImpl();
        this.transformationOutput = transformationOutput;

    }

    void loadDocument() throws IOException, SAXException, ParserConfigurationException {
        if (doc == null) {
            final DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            doc = documentBuilder.parse(new ByteArrayInputStream(transformationOutput));
        }
    }

    void addNamespace(String key, String value) {
        namespaces.put(key, value);
    }

    List<String> getXpathResult(String xPath) throws XPathExpressionException {

        final XPath xpath = xPathFactory.newXPath();


        if (!namespaces.keySet().isEmpty()) {
            xpath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if (namespaces.containsKey(prefix)) {
                        return namespaces.get(prefix);
                    }
                    return null;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });
        }

        final XPathExpression expression = xpath.compile(xPath);
        final NodeList nodes = (NodeList) expression.evaluate(doc, XPathConstants.NODESET);
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            result.add(nodes.item(i).getTextContent().trim());
        }

        return result;
    }
}
