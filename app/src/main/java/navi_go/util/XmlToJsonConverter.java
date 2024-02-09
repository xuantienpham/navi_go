package navi_go.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlToJsonConverter {

    /**
     * convert a string xml to JSONObject
     * @param xmlString string in xml format
     * @return  a JSONObject
     */
    public static JSONObject convertXmlToJson (String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes());
            org.w3c.dom.Document doc = builder.parse(inputStream);
            inputStream.close();
            JSONObject jsonObject = new JSONObject();
            convertXmlToJson(doc.getDocumentElement(), jsonObject);
            return jsonObject;
        } catch (ParserConfigurationException | JSONException |
                 IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }


    private static void convertXmlToJson(org.w3c.dom.Node node, JSONObject jsonObject) throws JSONException {
        int nodeType = node.getNodeType();
        if (nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) node;
            org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
            if (attributes.getLength() > 0) {
                JSONObject attrObject = new JSONObject();
                for (int i = 0; i < attributes.getLength(); i++) {
                    org.w3c.dom.Attr attr = (org.w3c.dom.Attr) attributes.item(i);
                    attrObject.put(attr.getNodeName(), attr.getNodeValue());
                }
                jsonObject.put(element.getNodeName(), attrObject);
            } else {
                jsonObject.put(element.getNodeName(), "");
            }
            org.w3c.dom.NodeList childNodes = element.getChildNodes();
            if (childNodes.getLength() > 0) {
                if (childNodes.getLength() == 1 && childNodes.item(0).getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                    jsonObject.put(element.getNodeName(), element.getTextContent());
                } else {
                    JSONObject childObject = new JSONObject();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        convertXmlToJson(childNodes.item(i), childObject);
                    }
                    jsonObject.put(element.getNodeName(), childObject);
                }
            }
        }
    }
}
