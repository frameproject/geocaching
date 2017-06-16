package mag.linux.android_sensors;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;



/**
 * @author fredericamps@gmail.com - 2017
 *
 * Data manger XML
 *
 */

public class DataManager {

    static final String TAG="XML";
    static final String NODE_GEO = "GEO";
    static final String xmlFile = "Data.xml";
    static final String xmlFileOther = "Other.xml";

    Context mContext;
    Document doc;


    DataManager(Context context) {
        mContext = context;

     //  reset();
     //   addGeoPoint("geo1", "000", "111", "infos");
     //   addGeoPoint("geo2", "000", "111", "infos");
     //   addGeoPoint("geo3", "000", "111", "infos");
     //   printContent();
     //   delGeoPoint("geo2");

        String filePath = mContext.getFilesDir().getPath().toString() + "/" + xmlFile;

        File file = new File(filePath);
        if(!file.exists())
        {
            init();
        }

        try {
            prettyPrint(getDocument(xmlFile));

            prettyPrint(getDocument(xmlFileOther));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    void init()
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            //add elements to Document
            Element rootElement = doc.createElement("data");

            //append root element to document
            doc.appendChild(rootElement);

            //for output to file, console
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            //for pretty print
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);

            //write to file
            String filePath = mContext.getFilesDir().getPath().toString() + "/" + xmlFile;

            StreamResult file = new StreamResult(new File(filePath));

            //write data
            transformer.transform(source, file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public GeoPoint [] getContent(String myXmlFile) {

        AssetManager manager = mContext.getAssets();
        InputStream stream;
        GeoPoint [] tabGeo=null;

        try {
            stream = mContext.openFileInput(myXmlFile);
            doc =getDocument(stream);

            // Get elements
            NodeList nodeList = doc.getElementsByTagName(NODE_GEO);

            if(nodeList.getLength() >0) {
                tabGeo = new GeoPoint[nodeList.getLength()];

                Log.v(TAG, "taille de tab = " + nodeList.getLength());

            /*
             * for each Geo element get designation
             */
                for (int i = 0; i < nodeList.getLength(); i++) {

                    Element e = (Element) nodeList.item(i);

                    tabGeo[i] = new GeoPoint(getValue(e, "name"), Double.parseDouble(getValue(e, "lat")), Double.parseDouble(getValue(e, "long")), getValue(e, "info"));

                    Log.v(TAG, getValue(e, "name"));
                    Log.v(TAG, getValue(e, "lat"));
                    Log.v(TAG, getValue(e, "long"));
                    Log.v(TAG, getValue(e, "info"));
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return tabGeo;
    }


    /**
     *
     * @param inputStream
     * @return
     */
    public Document getDocument(InputStream inputStream) {
        Document document = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder db = null;
        try {
            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        InputSource inputSource = new InputSource(inputStream);
        try {
            document = db.parse(inputSource);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    /**
     *
     * @param name
     * @param lat
     * @param lgt
     * @param info
     */
    public void addGeoPoint(String name, String lat, String lgt, String info) {
        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputStream stream = null;
        DocumentBuilder db = null;

        Log.v(TAG, "addGeoPoint");

        try {

            try {
                stream = mContext.openFileInput(xmlFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        InputSource inputSource = new InputSource(stream);

        try {
            doc = db.parse(inputSource);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        NodeList root = doc.getElementsByTagName("data");
        Node elem = root.item(0);

        elem.appendChild(getGeoPoint(doc, name, lat, lgt, info));


        //for output to file, console
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        //for pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);

        //write to file
        String filePath = mContext.getFilesDir().getPath().toString() + "/" + xmlFile;
        StreamResult file = new StreamResult(new File(filePath));

        //write data
        try {
            transformer.transform(source, file);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param geoName
     */
    public void delGeoPoint(String geoName, String mXMLFile)
    {
        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputStream stream = null;
        DocumentBuilder db = null;


        String mFilePath = mContext.getFilesDir().getPath().toString() + "/" + mXMLFile;

        File mFile = new File(mFilePath);
        if(!mFile.exists())
        {
            return;
        }


        try {

            try {
                stream = mContext.openFileInput(mXMLFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        InputSource inputSource = new InputSource(stream);

        try {
            doc = db.parse(inputSource);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        NodeList nodes = doc.getElementsByTagName(NODE_GEO);

        for (int i = 0; i < nodes.getLength(); i++) {
            Element person = (Element) nodes.item(i);
            // <name>
            Element name = (Element) person.getElementsByTagName("name").item(0);
            String pName = name.getTextContent();

            if (pName.equals(geoName)) {
                person.getParentNode().removeChild(person);

                Log.v(TAG, "XML : NODE DELETE " + pName);
            }
        }

        //for output to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        //for pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);

        //write to file
        String filePath = mContext.getFilesDir().getPath().toString() + "/" + mXMLFile;
        StreamResult file = new StreamResult(new File(filePath));

        //write data
        try {
            transformer.transform(source, file);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        System.out.println("DONE");

        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @return
     */
    public Document getDocument(String xmlFileName){
        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputStream stream = null;
        DocumentBuilder db = null;

        try {

            try {
                stream = mContext.openFileInput(xmlFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        InputSource inputSource = new InputSource(stream);

        try {
            doc = db.parse(inputSource);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return doc;
    }

    /**
     *
     * @param xml
     * @throws Exception
     */
    public static final void prettyPrint(Document xml) throws Exception {

        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(xml), new StreamResult(out));
        Log.v(TAG, out.toString());
    }

    /**
     *
     * @param doc
     * @param name
     * @param lat
     * @param longitude
     * @param info
     * @return
     */
    private static Node getGeoPoint(Document doc, String name, String lat, String longitude,
                                    String info) {
        Element geo = doc.createElement(NODE_GEO);
        //create name element
        geo.appendChild(getGeoPoint(doc, "name", name));

        //create lat element
        geo.appendChild(getGeoPoint(doc, "lat", lat));

        //create longitude element
        geo.appendChild(getGeoPoint(doc, "long", longitude));

        //create info element
        geo.appendChild(getGeoPoint(doc, "info", info));

        return geo;
    }


    /**utility method to create text node
     *
     * @param doc
     * @param name
     * @param value
     * @return
     */
    private static Node getGeoPoint(Document doc, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }


    /**
     *  Return value
     * @param item
     * @param name
     * @return
     */
    public String getValue(Element item, String name) {
        NodeList nodes = item.getElementsByTagName(name);
        return this.getTextNodeValue(nodes.item(0));
    }

    /**
     *
     * @param node
     * @return
     */
    private final String getTextNodeValue(Node node) {
        Node child;
        if (node != null) {
            if (node.hasChildNodes()) {
                child = node.getFirstChild();
                while(child != null) {
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        return child.getNodeValue();
                    }
                    child = child.getNextSibling();
                }
            }
        }
        return "";
    }


}






