package nl.kb.xml.transformassert;

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
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class XpathUtil {
    static List<String> getXpathResult(String xPath, Map<String, String> namespaces, byte[] transformationOutput) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        if (!namespaces.keySet().isEmpty()) {
            dbf.setNamespaceAware(true);
        }

        final XPathFactory xPathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();
        final XPath xpath = xPathFactory.newXPath();
        final DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        final Document doc = documentBuilder.parse(new ByteArrayInputStream(transformationOutput));
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
