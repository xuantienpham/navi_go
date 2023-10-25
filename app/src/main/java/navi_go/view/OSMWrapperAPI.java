package navi_go.view;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class OSMWrapperAPI {
    private static final String OVERPASS_API = "http://www.overpass-api.de/api/interpreter";
    private static final String OPENSTREETMAP_API_O6 = "http://www.openstreetmap.org/api/0.6";

    private OSMWrapperAPI () {
        super();
    }


    public static OSMNode getNode (String nodeId) throws IOException, ParserConfigurationException, SAXException {
        String string = OSMWrapperAPI.OPENSTREETMAP_API_O6 + "/node/" + nodeId;
        URL osm = new URL(string);
        HttpURLConnection connection = (HttpURLConnection) osm.openConnection();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
        Document document = docBuilder.parse (connection.getInputStream());
        List<OSMNode> nodes = getNodes (document);
        if ( !nodes.isEmpty()) {
            return nodes.iterator().next();
        }
        return null;
    }

    private static Document getXML (double lon, double lat, double vicinityRange) throws IOException, SAXException, ParserConfigurationException {
        DecimalFormat format = new DecimalFormat("##0.000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));   // $NON-NLS-$
        String left = format.format(lat - vicinityRange);
        String bottom = format.format(lon - vicinityRange);
        String right = format.format(lat + vicinityRange);
        String top = format.format(lon + vicinityRange);

        String string = OSMWrapperAPI.OPENSTREETMAP_API_O6 + "map?bbox=" + left + "," + bottom + "," + right + "," + top;

        URL osm = new URL(string);
        HttpURLConnection connection = (HttpURLConnection) osm.openConnection();

        DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbFac.newDocumentBuilder();

        return documentBuilder.parse(connection.getInputStream());
    }

    private static List<OSMNode> getNodes (Document xmlDocument)  {
        List<OSMNode> osmNodes = new ArrayList<>();

        // Document xml = getXML (8.32, 49.001);
        Node osmRoot = xmlDocument.getFirstChild();
        NodeList osmXMLNodes = osmRoot.getChildNodes();
        for (int i=1; i < osmXMLNodes.getLength(); i++) {
            Node item = osmXMLNodes.item(i);
            if (item.getNodeName().equals("node")) {
                NamedNodeMap attributes = item.getAttributes();
                NodeList tagXMLNodes = item.getChildNodes();
                Map<String, String> tags = new HashMap<>();
                for (int j =1; j < tagXMLNodes.getLength(); j++) {
                    Node tagItem = tagXMLNodes.item (j);
                    NamedNodeMap tagAttributes = tagItem.getAttributes();
                    if (tagAttributes != null) {
                        tags.put (tagAttributes.getNamedItem("k").getNodeValue(), tagAttributes.getNamedItem("v").getNodeValue());
                    }
                } // enf for int j
                Node namedItemID = attributes.getNamedItem("id");
                Node namedItemLat = attributes.getNamedItem("lat");
                Node namedItemLon = attributes.getNamedItem("lon");
                Node namedItemVersion = attributes.getNamedItem("version");

                String id = namedItemID.getNodeValue();
                String latitude = namedItemLat.getNodeValue();
                String longitude = namedItemLon.getNodeValue();
                String version = "0";

                if (namedItemVersion != null)   version = namedItemVersion.getNodeValue();

                osmNodes.add (new OSMNode(id, latitude, longitude, tags, version));
            } // enf if
        } // enf for int i
        return osmNodes;
    }


    public static List<OSMNode> getOSMNodeInVicinity (double lat, double lon, double vicinityRange) throws IOException, SAXException, ParserConfigurationException {
        return OSMWrapperAPI.getNodes(getXML(lon, lat, vicinityRange));
    }

    public static Document getNodesViaOverpass (String query) throws IOException, ParserConfigurationException, SAXException {
        String queryString = readFileAsString (query);

        URL osm = new URL (OSMWrapperAPI.OVERPASS_API);
        HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        DataOutputStream printout = new DataOutputStream( connection.getOutputStream());
        printout.writeBytes( "data=" + URLEncoder.encode(queryString, "utf-8"));
        printout.flush();
        printout.close();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(connection.getInputStream());
    }

    private static String readFileAsString (String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char [1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf (buf, 0, numRead);
            fileData.append (readData);
            buf = new char [1024];
        }
        reader.close();
        return fileData.toString();
    }



}