/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Jul 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.icu.dev.tool.cldr;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

// DOM imports
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

// Needed JAXP classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

// SAX2 imports
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author ram
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LDMLUtilities {

    /**
     * Creates a fully resolved locale starting with root and 
     * @param sourceDir
     * @param locale
     * @return
     */
    public static Document getFullyResolvedLDML(String sourceDir, String locale, boolean ignoreRoot){
    	Document full =null;
        try{
        	full = parse(sourceDir+File.separator+ "root.xml");
        }catch(RuntimeException ex){
        	if(!ignoreRoot){
        		throw ex;
            }
        }
        String[] constituents = locale.split("_");
        String loc=null;
        for(int i=0; i<constituents.length; i++){
        	if(loc==null){
        		loc = constituents[i];
            }else{
            	loc = loc +"_"+ constituents[i];
            }
            Document doc = parse(sourceDir+File.separator+loc+".xml");
            StringBuffer xpath = new StringBuffer();
            mergeLDMLDocuments(full, doc, xpath);
        }
        return full;
    }
    private static String TYPE = "type";
    private static String IDENTITY = "identity";
    /**
     *   Resolved Data File
     *   <p>To produce fully resolved locale data file from CLDR for a locale ID L, you start with root, and 
     *   replace/add items from the child locales until you get down to L. More formally, this can be 
     *   expressed as the following procedure.</p>
     *   <ol>
     *     <li>Let Result be an empty LDML file.</li>
     *   
     *     <li>For each Li in the locale chain for L<ol>
     *       <li>For each element pair P in the LDML file for Li:<ol>
     *         <li>If Result has an element pair Q with an equivalent element chain, remove Q.</li>
     *         <li>Add P to Result.</li>
     *       </ol>
     *       </li>
     *     </ol>
     *   
     *     </li>
     *   </ol>
     *   <p>Note: when adding an element pair to a result, it has to go in the right order for it to be valid 
     *  according to the DTD.</p>
     *
     * @param source
     * @param overide
     * @return the merged document
     */
    public static Node mergeLDMLDocuments(Document source, Node overide, StringBuffer xpath){
    	if(source==null){
    		return overide;
        }
        if(xpath.length()==0){
        	xpath.append("/");
        }
        // we know that every child xml file either adds or 
        // overrides the elements in parent
        // so we traverse the child, at every node check if 
        // if the node is present in the source,  
        // if (present)
        //    recurse to replace any nodes that need to be overridded
        // else
        //    import the node into source
        Node child = overide.getFirstChild();
        while( child!=null){
            // we are only concerned with element nodes
            if(child.getNodeType()!=Node.ELEMENT_NODE){
                child = child.getNextSibling();
                continue;
            }   
            String childName = child.getNodeName();
            int savedLength=xpath.length();
            xpath.append("/");
            xpath.append(childName);
            appendXPathAttribute(child,xpath);
            Node nodeInSource = getNode(source, xpath.toString());
            if(nodeInSource==null){
            	// the child xml has a new node
            	// that should be added to parent
                String parentXpath = xpath.substring(0, savedLength);
                Node parentNodeInSource = getNode(source, parentXpath);
                if(parentNodeInSource==null){
                	throw new RuntimeException("Internal Error");
                }
                if(xpath.indexOf(IDENTITY)>-1){
                    String delXPath= xpath.substring(0,savedLength)+"/"+childName;
                    Node delNode = getNode(source, delXPath);
                    if(delNode!=null){
                        parentNodeInSource.removeChild(delNode);
                    }
                }
                Node childToImport = source.importNode(child,true);
                parentNodeInSource.appendChild(childToImport);

            }else{
                if(areChildrenElementNodes(child)){
                    //recurse to pickup any children!
                    mergeLDMLDocuments(source, child, xpath);
                }else{
                	// we have reached a leaf node now get the 
                    // replace to the source doc
                    Node parentNodeInSource = nodeInSource.getParentNode();
                    Node childToImport = source.importNode(child,true);
                    parentNodeInSource.replaceChild(childToImport, nodeInSource);       
                }
            }
            xpath.delete(savedLength,xpath.length());
            child= child.getNextSibling();
        }
        return source;
    }
    private static final String ALT      = "alt";
    private static final String KEY      = "key";
    private static final String REGISTRY = "registry";
    public static void appendXPathAttribute(Node node, StringBuffer xpath){
        boolean terminate = false;
    	String val = getAttributeValue(node, TYPE);
        if(val!=null){
        	xpath.append("[@type='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, ALT);
        if(val!=null){
            xpath.append("and @alt='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, KEY);
        if(val!=null){
            xpath.append("and @key='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, REGISTRY);
        if(val!=null){
            xpath.append("and @registry='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        if(terminate){
        	xpath.append("]");
        }
    }
    public static boolean areChildrenElementNodes(Node node){
        NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            if(list.item(i).getNodeType()==Node.ELEMENT_NODE){
                return true;
            }
        }
        return false;  
    }
    
    public static NodeList getNodeList( Document doc, String xpath){
        try{
            return XPathAPI.selectNodeList(doc, xpath);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }   
    }
    /**
     * 
     * @param doc
     * @param xpath
     * @return
     */
    public static Node getNode(Document doc, String xpath){
        try{
            NodeList nl = XPathAPI.selectNodeList(doc, xpath);
            int len = nl.getLength();
            //TODO watch for attribute "alt"
            if(len>1){
              throw new IllegalArgumentException("The XPATH returned more than 1 node!. Check XPATH: "+xpath);   
            }
            if(len==0){
            	return null;
            }
            return nl.item(0);

        }catch(TransformerException ex){
        	throw new RuntimeException(ex.getMessage());
        }
    }
    
    public static Node getNode(Node node, String xpath){
        try{
            NodeList nl = XPathAPI.selectNodeList(node, xpath);
            int len = nl.getLength();
            //TODO watch for attribute "alt"
            if(len>1){
              throw new IllegalArgumentException("The XPATH returned more than 1 node!. Check XPATH: "+xpath);   
            }
            if(len==0){
                return null;
            }
            return nl.item(0);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
    public static Node getNode(Node context, String resToFetch, Document fullyResolved, String xpath){
        String ctx = "./"+ resToFetch;
        Node node = getNode(context, ctx);
        if(node == null && fullyResolved!=null){
            // try from fully resolved
            String path = xpath+"/"+resToFetch;
            node = getNode(fullyResolved, path);
        }
        return node;
    }
    /** Decide if the node is text, and so must be handled specially */
    static boolean isTextNode(Node n) {
      if (n == null)
        return false;
      short nodeType = n.getNodeType();
      return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE;
    }   
    
    public static String getAttributeValue(Node sNode, String attribName){
        String value=null;
        Node node = sNode;

        NamedNodeMap attributes = node.getAttributes();
        Node attr = attributes.getNamedItem(attribName);
        if(attr!=null){
            value = attr.getNodeValue();
        }

        return value;
    }
    public static String getNodeValue(Node node){
        for(Node child=node.getFirstChild(); child!=null; child=child.getNextSibling() ){
            if(child.getNodeType()==Node.TEXT_NODE){
                return child.getNodeValue();
            }
        }
        return null;
    }

    /**
     * Simple worker method to parse filename to a Document.  
     *
     * Attempts XML parse, then HTML parse (when parser available), 
     * then just parses as text and sticks into a text node.
     *
     * @param filename to parse as a local path
     *
     * @return Document object with contents of the file; 
     * otherwise throws an unchecked RuntimeException if there 
     * is any fatal problem
     */
    public static Document parse(String filename)
    {
        // Force filerefs to be URI's if needed: note this is independent of any other files
        String docURI = filenameToURL(filename);
        return parse(new InputSource(docURI),filename);
    }
    
    public static Document parse(InputSource docSrc, String filename){
        
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        // Always set namespaces on
        dfactory.setNamespaceAware(true);
        dfactory.setValidating(true);
        // Set other attributes here as needed
        //applyAttributes(dfactory, attributes);
        
        // Local class: cheap non-printing ErrorHandler
        // This is used to suppress validation warnings
        ErrorHandler nullHandler = new ErrorHandler() {
            public void warning(SAXParseException e) throws SAXException {System.err.println("Warning: " + e.getMessage());}
            public void error(SAXParseException e) throws SAXException {System.err.println("Error: " + e.getMessage());}
            public void fatalError(SAXParseException e) throws SAXException 
            {
                throw e;
            }
        };

        Document doc = null;
        try
        {
            // First, attempt to parse as XML (preferred)...
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            docBuilder.setErrorHandler(nullHandler);
            doc = docBuilder.parse(docSrc);
        }
        catch (Throwable se)
        {
            // ... if we couldn't parse as XML, attempt parse as HTML...
            System.out.println("ERROR :" + se.getMessage());
            try
            {
                // @todo need to find an HTML to DOM parser we can use!!!
                // doc = someHTMLParser.parse(new InputSource(filename));
                throw new RuntimeException("XMLComparator no HTML parser!");
            }
            catch (Exception e)
            {
                if(filename!=null)
                {
                    // ... if we can't parse as HTML, then just parse the text
                    try
                    {
    
                        // Parse as text, line by line
                        //   Since we already know it should be text, this should 
                        //   work better than parsing by bytes.
                        FileReader fr = new FileReader(filename);
                        BufferedReader br = new BufferedReader(fr);
                        StringBuffer buffer = new StringBuffer();
                        for (;;)
                        {
                            String tmp = br.readLine();
    
                            if (tmp == null)
                            {
                                break;
                            }
    
                            buffer.append(tmp);
                            buffer.append("\n");  // Put in the newlines as well
                        }
    
                        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
                        doc = docBuilder.newDocument();
                        Element outElem = doc.createElement("out");
                        Text textNode = doc.createTextNode(buffer.toString());
    
                        // Note: will this always be a valid node?  If we're parsing 
                        //    in as text, will there ever be cases where the diff that's 
                        //    done later on will fail becuase some really garbage-like 
                        //    text has been put into a node?
                        outElem.appendChild(textNode);
                        doc.appendChild(outElem);
                    }
                    catch (Throwable throwable)
                    {

                        //throwable.printStackTrace();
                    }
                }
            }
        }
        return doc;
    }  // end of parse()

    /*
     * Utility method to translate a String filename to URL.  
     *
     * Note: This method is not necessarily proven to get the 
     * correct URL for every possible kind of filename; it should 
     * be improved.  It handles the most common cases that we've 
     * encountered when running Conformance tests on Xalan.
     * Also note, this method does not handle other non-file:
     * flavors of URLs at all.
     *
     * If the name is null, return null.
     * If the name starts with a common URI scheme (namely the ones 
     * found in the examples of RFC2396), then simply return the 
     * name as-is (the assumption is that it's already a URL)
     * Otherwise we attempt (cheaply) to convert to a file:/// URL.
     */
    public static String filenameToURL(String filename){
        // null begets null - something like the commutative property
        if (null == filename){
            return null;
        }

        // Don't translate a string that already looks like a URL
        if (filename.startsWith("file:")
            || filename.startsWith("http:")
            || filename.startsWith("ftp:")
            || filename.startsWith("gopher:")
            || filename.startsWith("mailto:")
            || filename.startsWith("news:")
            || filename.startsWith("telnet:")
           ){
               return filename;
           }
        

        File f = new File(filename);
        String tmp = null;
        try{
            // This normally gives a better path
            tmp = f.getCanonicalPath();
        }catch (IOException ioe){
            // But this can be used as a backup, for cases 
            //  where the file does not exist, etc.
            tmp = f.getAbsolutePath();
        }

        // URLs must explicitly use only forward slashes
        if (File.separatorChar == '\\') {
            tmp = tmp.replace('\\', '/');
        }
        // Note the presumption that it's a file reference
        // Ensure we have the correct number of slashes at the 
        //  start: we always want 3 /// if it's absolute
        //  (which we should have forced above)
        if (tmp.startsWith("/")){
            return "file://" + tmp;
        }
        else{
            return "file:///" + tmp;
        }
    }

    /** Prints the specified node, recursively. */
    public static void printDOMTree(Node node, PrintWriter out) 
    {
      int type = node.getNodeType();
      switch (type)
      {
        // print the document element
        case Node.DOCUMENT_NODE: 
          {
            printDOMTree(((Document)node).getDocumentElement(), out);
            break;
          }

          // print element with attributes
        case Node.ELEMENT_NODE: 
          {
            out.print("<");
            out.print(node.getNodeName());
            NamedNodeMap attrs = node.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++)
            {
              Node attr = attrs.item(i);
              out.print(" " + attr.getNodeName() + 
                        "=\"" + attr.getNodeValue() + 
                        "\"");
            }
            out.print(">");

            NodeList children = node.getChildNodes();
            if (children != null)
            {
              int len = children.getLength();
              for (int i = 0; i < len; i++)
                printDOMTree(children.item(i), out);
            }

            break;
          }

          // handle entity reference nodes
        case Node.ENTITY_REFERENCE_NODE: 
          {
            out.print("&");
            out.print(node.getNodeName());
            out.print(";");
            break;
          }

          // print cdata sections
        case Node.CDATA_SECTION_NODE: 
          {
            out.print("<![CDATA[");
            out.print(node.getNodeValue());
            out.print("]]>");
            break;
          }

          // print text
        case Node.TEXT_NODE: 
          {
            out.print(node.getNodeValue());
            break;
          }

          // print processing instruction
        case Node.PROCESSING_INSTRUCTION_NODE: 
          {
            out.print("<?");
            out.print(node.getNodeName());
            String data = node.getNodeValue();
            {
              out.print(" ");
              out.print(data);
            }
            out.print("?>");
            break;
          }
      }

      if (type == Node.ELEMENT_NODE)
      {
        out.print("</");
        out.print(node.getNodeName());
        out.print('>');
      }
    } // printDOMTree(Node, PrintWriter)

}
