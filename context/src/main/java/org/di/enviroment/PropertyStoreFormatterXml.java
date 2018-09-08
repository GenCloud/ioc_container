package org.di.enviroment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Store generator for xml-files format (DTD http://java.sun.com/dtd/properties.dtd).
 * <p>
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class PropertyStoreFormatterXml extends PropertyStoreFormatterImpl {
    /**
     * Partial copy of java.util.XMLUtils.emitDocument(Document doc, OutputStream os, String encoding).
     * Re-implemented to avoid Properties keys auto-sort.
     * All exceptions are thrown through IOException exception.
     */
    @Override
    public String generate() throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pe) {
            throw new IOException(pe);
        }

        Document doc = db.newDocument();

        Element properties = (Element) doc.appendChild(doc.createElement("properties"));

        for (Map.Entry<String, String> pair : pairs.entrySet()) {
            Element entry = (Element) properties.appendChild(doc.createElement("entry"));
            entry.setAttribute("key", pair.getKey());
            entry.appendChild(doc.createTextNode(pair.getValue()));
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://java.sun.com/dtd/properties.dtd");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        } catch (TransformerConfigurationException tce) {
            throw new IOException(tce);
        }

        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult streamResult = new StreamResult(writer);

        try {
            transformer.transform(source, streamResult);
        } catch (TransformerException te) {
            throw new IOException(te);
        }

        return writer.toString();
    }
}
