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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

// DOM imports
import org.apache.xpath.XPathAPI;
import org.apache.xalan.serialize.DOMSerializer;
import org.apache.xalan.serialize.Serializer;
import org.apache.xalan.serialize.SerializerFactory;
import org.apache.xalan.templates.OutputProperties;
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
    public static Document getFullyResolvedLDML(String sourceDir, String locale, 
                                                boolean ignoreRoot, boolean ignoreUnavailable, 
                                                boolean ignoreIfNoneAvailable){
    	Document full =null;
        try{
        	full = parse(sourceDir+File.separator+ "root.xml");
            if(full!=null){
                full = resolveAliases(full, sourceDir, "root");
            }
           /*
            * Debugging
            *
            Node[] list = getNodeArray(full, LDMLConstants.ALIAS);
            if(list.length>0){
                System.err.println("Aliases not resolved!. list.getLength() returned "+ list.length);
            }*/
            
        }catch(RuntimeException ex){
        	if(!ignoreRoot){
        		throw ex;
            }
        }
        int index = locale.indexOf(".xml");
        if(index > -1){
            locale = locale.substring(0,index);
        }
        if(locale.equals("root")){
            return full;
        }
        String[] constituents = locale.split("_");
        String loc=null;
        boolean isAvailable = false;
        for(int i=0; i<constituents.length; i++){
        	if(loc==null){
        		loc = constituents[i];
            }else{
            	loc = loc +"_"+ constituents[i];
            }
            Document doc = null;
            
            String fileName = sourceDir+File.separator+loc+".xml";
            File file = new File(fileName);
            if(file.exists()){
                isAvailable = true;
                doc = parse(fileName);
                doc = resolveAliases(doc, sourceDir, loc);
                /*
                 * Debugging
                 *
                Node[] list = getNodeArray(doc, LDMLConstants.ALIAS);
                if(list.length>0){
                    System.err.println("Aliases not resolved!. list.getLength() returned "+ list.length);
                }
                */
                if(full==null){
                    full = doc;
                }else{
                    StringBuffer xpath = new StringBuffer();
                    mergeLDMLDocuments(full, doc, xpath, loc, sourceDir);
                }
                /*
                 * debugging
                 *
                Node ec = getNode(full, "//ldml/characters/exemplarCharacters");
                if(ec==null){
                    System.err.println("Could not find exemplarCharacters");
                }else{
                    System.out.println("The chars are: "+ getNodeValue(ec));
                }   
                */
            }else{
                if(!ignoreUnavailable){
                    throw new RuntimeException("Could not find: " +fileName);
                }
            }
            // TODO: investigate if we really need to revalidate the DOM tree!
            // full = revalidate(full, locale);
        }
       if(ignoreIfNoneAvailable==true && isAvailable==false){
           return null ;
       }
       
       return full;
    }
    
    public static Document revalidate(Document doc, String fileName){
        // what a waste!!
        // to revalidate an in-memory DOM tree we need to first
        // serialize it to byte array and read it back again.
        // in DOM level 3 implementation there is API to validate
        // in-memory DOM trees but the latest implementation of Xerces
        // can only validate against schemas not DTDs!!!
        try{
            // revalidate the document
            Serializer serializer = SerializerFactory.getSerializer(OutputProperties.getDefaultMethodProperties("xml"));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            serializer.setOutputStream(os);
            DOMSerializer ds = serializer.asDOMSerializer();
            ds.serialize(doc);
            os.flush();
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            doc = parse(new InputSource(is),"Fully resolved: "+fileName);
            return doc;
        }catch(IOException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
    public static String convertXPath2ICU(Node alias, Node namespaceNode, StringBuffer fullPath)
        throws TransformerException{
        Node context = alias.getParentNode();
        StringBuffer icu = new StringBuffer();
        String source = getAttributeValue(alias, LDMLConstants.SOURCE);
        String xpath = getAttributeValue(alias, LDMLConstants.PATH);
        
        // make sure that the xpaths are valid
        if(namespaceNode==null){
            XPathAPI.eval(context, fullPath.toString());
            if(xpath!=null){
                XPathAPI.eval(context,xpath);
            }
        }else{
            XPathAPI.eval(context, fullPath.toString(), namespaceNode);
            if(xpath!=null){
                XPathAPI.eval(context, xpath, namespaceNode);
            }
        }
        icu.append(source);
        if(xpath!=null){
            StringBuffer resolved = XPathTokenizer.relativeToAbsolute(xpath, fullPath);
            // make sure that fullPath is not corrupted!
            XPathAPI.eval(context, fullPath.toString());
            
            //TODO .. do the conversion
            XPathTokenizer tokenizer = new XPathTokenizer(resolved);
            
            String token = tokenizer.nextToken();
            while(token!=null){
                if(!token.equals("ldml")){
                    String equiv = getICUEquivalent(token);
                    if(equiv==null){
                        throw new IllegalArgumentException("Could not find ICU equivalent for token: " +token);
                    }
                    if(equiv.length()>0){
                        icu.append("/");
                        icu.append(equiv);
                    }
                }
                token = tokenizer.nextToken();
            }
        }
        return icu.toString();
    }
    public static String getDayIndexAsString(String type){
        if(type.equals("sun")){
            return "0";
        }else if(type.equals("mon")){
            return "1";
        }else if(type.equals("tue")){
            return "2";
        }else if(type.equals("wed")){
            return "3";
        }else if(type.equals("thu")){
            return "4";
        }else if(type.equals("fri")){
            return "5";
        }else if(type.equals("sat")){
            return "6";
        }else{
            throw new IllegalArgumentException("Unknown type: "+type);
        }
    }
    public static String getMonthIndexAsString(String type){
        return Integer.toString(Integer.parseInt(type)-1);
    }
    private static String getICUEquivalent(String token){
        int index = 0;
        if(token.indexOf(LDMLConstants.LDN) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.LANGUAGES) > -1){
            return "Languages";
        }else if(token.indexOf(LDMLConstants.LANGUAGE) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.TERRITORIES) > -1){
            return "Countries";
        }else if(token.indexOf(LDMLConstants.TERRITORY) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.SCRIPTS) > -1){
            return "Scripts";
        }else if(token.indexOf(LDMLConstants.SCRIPT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.VARIANTS) > -1){
            return "Variants";
        }else if(token.indexOf(LDMLConstants.VARIANT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.KEYS) > -1){
            return "Keys";
        }else if(token.indexOf(LDMLConstants.KEY) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.TYPES) > -1){
            return "Types";
        }else if((index=token.indexOf(LDMLConstants.TYPE)) > -1 && token.charAt(index-1)!='@'){
            String type = getAttributeValue(token, LDMLConstants.TYPE);
            String key = getAttributeValue(token, LDMLConstants.KEY);
            return type+"/"+key;
        }else if(token.indexOf(LDMLConstants.LAYOUT) > -1){
            return "Layout";
        }else if(token.indexOf(LDMLConstants.ORIENTATION) > -1){
            //TODO fix this
        }else if(token.indexOf(LDMLConstants.CHARACTERS) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.EXEMPLAR_CHARACTERS) > -1){
            return "ExemplarCharacters";
        }else if(token.indexOf(LDMLConstants.MEASUREMENT) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.MS) > -1){
            return "MeasurementSystem";
        }else if(token.indexOf(LDMLConstants.PAPER_SIZE) > -1){
            return "PaperSize";
        }else if(token.indexOf(LDMLConstants.HEIGHT) > -1){
            return "0";
        }else if(token.indexOf(LDMLConstants.WIDTH) > -1){
            return "1";
        }else if(token.indexOf(LDMLConstants.DATES) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.LPC) > -1){
            return "localPatternCharacters";
        }else if(token.indexOf(LDMLConstants.CALENDARS) > -1){
            return "calendar";
        }else if(token.indexOf(LDMLConstants.DEFAULT) > -1){
            return "default";
        }else if(token.indexOf(LDMLConstants.CALENDAR) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.ERAS) > -1){
            return "eras";
        }else if(token.indexOf(LDMLConstants.ERAABBR) > -1){
            return "abbreviated";
        }else if(token.indexOf(LDMLConstants.ERA) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.DATE_FORMATS) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DFL) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DATE_FORMAT) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.TIME_FORMATS) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.TFL) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.TIME_FORMAT) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DATE_TIME_FORMATS) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DTFL) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DATE_TIME_FORMAT) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.MONTHS) > -1){
            return "monthNames";
        }else if(token.indexOf(LDMLConstants.MONTH_CONTEXT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.MONTH_WIDTH) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.MONTH) > -1){
            String valStr = getAttributeValue(token, LDMLConstants.TYPE);
            return getMonthIndexAsString(valStr);
        }else if(token.indexOf(LDMLConstants.DAYS) > -1){
            return "dayNames";
        }else if(token.indexOf(LDMLConstants.DAY_CONTEXT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.DAY_WIDTH) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.DAY) > -1){
            String dayName = getAttributeValue(token, LDMLConstants.TYPE);
            return getDayIndexAsString(dayName);   
        }else if(token.indexOf(LDMLConstants.COLLATIONS) > -1){
            return "collations";
        }else if(token.indexOf(LDMLConstants.COLLATION) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }
        
        // TODO: this method is not finished yet 
        // the conversion of Xpath to ICU alias path
        // is not as straight forward as I thought
        // need to cater to idiosynchracies of each
        // element node :(
        throw new IllegalArgumentException("Unknown Xpath fragment: " + token);
    }
    /**
     * 
     * @param token XPath token fragment
     * @param attrib attribute whose value must be fetched
     * @return
     */
    private static String getAttributeValue(String token, String attrib){
        int attribStart = token.indexOf(attrib);
        int valStart = token.indexOf('=', attribStart)+1/*skip past the separtor*/;
        int valEnd = token.indexOf('@', valStart);
        if(valEnd <0){
            valEnd = valStart + (token.length()-valStart-1);
        }else{
            valEnd = token.length() - 1 /*valEnd should be index*/;
        }
        String value = token.substring(valStart, valEnd);
        int s = value.indexOf('\'');
        if(s>-1){
            s++;
            int e = value.lastIndexOf('\'');
            return value.substring(s,e);
        }
        return value;
    }
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
    private static Node mergeLDMLDocuments(Document source, Node overide, StringBuffer xpath, 
                                          String thisName, String sourceDir){
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
            Node nodeInSource = null;
            
            if(childName.indexOf(":")>-1){ 
                nodeInSource = getNode(source, xpath.toString(), child);
            }else{
                nodeInSource =  getNode(source, xpath.toString());
            }
            
            Node parentNodeInSource = null;
            if(nodeInSource==null){
            	// the child xml has a new node
            	// that should be added to parent
                String parentXpath = xpath.substring(0, savedLength);

                if(childName.indexOf(":")>-1){ 
                    parentNodeInSource = getNode(source, parentXpath, child);
                }else{
                    parentNodeInSource =  getNode(source,parentXpath);
                }
                if(parentNodeInSource==null){
                	throw new RuntimeException("Internal Error");
                }
                
                Node childToImport = source.importNode(child,true);
                parentNodeInSource.appendChild(childToImport);
            }else if( childName.equals(LDMLConstants.IDENTITY) ||
                      childName.equals(LDMLConstants.COLLATION)){
                // replace the source doc
                // none of the elements under collations are inherited
                // only the node as a whole!!
                parentNodeInSource = nodeInSource.getParentNode();
                Node childToImport = source.importNode(child,true);
                parentNodeInSource.replaceChild(childToImport, nodeInSource);
            }else{
                if(areChildrenElementNodes(child) &&  areChildrenElementNodes(nodeInSource)){
                    //recurse to pickup any children!
                    mergeLDMLDocuments(source, child, xpath, thisName, sourceDir);
                }else{
                	// we have reached a leaf node now get the 
                    // replace to the source doc
                    parentNodeInSource = nodeInSource.getParentNode();
                    Node childToImport = source.importNode(child,true);
                    parentNodeInSource.replaceChild(childToImport, nodeInSource);       
                }
            }
            xpath.delete(savedLength,xpath.length());
            child= child.getNextSibling();
        }
        return source;
    }
    private static Node[] getNodeArray(Document doc, String tagName){
        NodeList list =  doc.getElementsByTagName(tagName);
        // node list is dynamic .. if a node is deleted, then 
        // list is immidiately updated.
        // so first cache the nodes returned and do stuff 
        Node[] array = new Node[list.getLength()];
        for(int i=0; i<list.getLength(); i++){
            array[i] = list.item(i);
        }
        return array;
    }
    /**
     * Utility to create abosolute Xpath from 1.1 style alias element
     * @param node
     * @param type
     * @return
     */
    public static String getAbsoluteXPath(Node node, String type){
        StringBuffer xpath = new StringBuffer();
        StringBuffer xpathFragment = new StringBuffer();
        node = node.getParentNode(); // the node is alias node .. get its parent
        if(node==null){
            throw new IllegalArgumentException("Alias node's parent is null!");
        }
        xpath.append(node.getNodeName());
        if(type!=null){
            xpath.append("[@type='"+type+"']");
        }
        Node parent = node;
        while((parent = parent.getParentNode())!=null){
            xpathFragment.setLength(0);
            xpathFragment.append(parent.getNodeName());
            if(parent.getNodeType()!= Node.DOCUMENT_NODE){
                appendXPathAttribute(parent, xpathFragment);
                xpath.insert(0,"/");
                xpath.insert(0, xpathFragment);
            }
        }
        xpath.insert(0, "//");
        return xpath.toString();
    }
    /**
     * 
     * @param doc
     * @param sourceDir
     * @param thisLocale
     */
    // TODO guard against circular aliases
    public static Document resolveAliases(Document doc, String sourceDir, String thisLocale){
       Node[] array = getNodeArray(doc, LDMLConstants.ALIAS);
       
       // resolve all the aliases by iterating over
       // the list of nodes
       Node[] replacementList = null;
       Node parent = null;   
       String source = null;
       String path = null;
       String type = null;
       for(int i=0; i < array.length ; i++){
           Node node = array[i];
           if(node==null){
               System.err.println("list.item("+i+") returned null!. The list reports it's length as: "+array.length);
               //System.exit(-1);
               continue;
           }
           source = getAttributeValue(node, LDMLConstants.SOURCE);
           path = getAttributeValue(node, LDMLConstants.PATH);
           type = getAttributeValue(node, LDMLConstants.TYPE);
           
           parent = node.getParentNode();
           
           if(source!=null && path==null){
               //this LDML 1.1 style alias parse it
               path = getAbsoluteXPath(node, type);
           }
           
           if(source!=null && !source.equals(thisLocale)){
               // if source is defined then path should not be 
               // relative 
               if(path.indexOf("..")>0){
                   throw new IllegalArgumentException("Cannot parse relative xpath: " + path + 
                                                      " in locale: "+ source + 
                                                      " from source locale: "+thisLocale);
               }
               // this is a is an absolute XPath
               Document newDoc = parse(sourceDir + File.separator + source + ".xml" );
               replacementList = getNodeListAsArray(newDoc, path);
           }else{
               // path attribute is referencing another node in this DOM tree
               replacementList = getNodeListAsArray(parent, path);
           }
           if(replacementList != null){
               parent.removeChild(node);
               int listLen = replacementList.length;
               if(listLen > 1){
                   // check if the whole locale is aliased
                   // if yes then remove the identity from
                   // the current document!
                   if(path!=null && path.equals("//ldml/*")){
                       Node[] identity = getNodeArray(doc, LDMLConstants.IDENTICAL);
                       for(int j=0; j<identity.length; j++){
                           parent.removeChild(node);
                       }
                   }
                   for(int j=0; j<listLen; j++){
                       // found an element node in the aliased resource
                       // add to the source
                       Node child = replacementList[j];
                       Node childToImport = doc.importNode(child,true);
                       parent.appendChild(childToImport);
                   }
               }else{
                   Node replacement = replacementList[0];
                   for(Node child = replacement.getFirstChild(); child!=null; child=child.getNextSibling()){
                       // found an element node in the aliased resource
                       // add to the source
                       Node childToImport = doc.importNode(child,true);
                       parent.appendChild(childToImport);
                   }
               }
              
           }else{
               throw new IllegalArgumentException("Could not find node for xpath: " + path + 
                       " in locale: "+ source + 
                       " from source locale: "+thisLocale);

           }
       }
       return doc;
    }
    //TODO add funtions for fetching legitimate children
    // for ICU 
    public boolean isParentDraft(Document fullyResolved, String xpath){
        Node node = getNode(fullyResolved, xpath);
        Node parentNode ;
        while((parentNode = node.getParentNode())!=null){
            String draft = getAttributeValue(parentNode, LDMLConstants.DRAFT);
            if(draft!=null ){
                if(draft.equals("true")){
                    return true;
                }else{
                    return false;
                }
            }
        }
        // the default value is false if none specified
        return false;
    }
    public static boolean isNodeDraft(Node node){
        String draft = getAttributeValue(node, LDMLConstants.DRAFT);
        if(draft!=null ){
            if(draft.equals("true")){
                return true;
            }else{
                return false;
            }
        }
        return false;
    }
    public static boolean isDraft(Node fullyResolved, StringBuffer xpath){
        Node current = getNode(fullyResolved, xpath.toString());
        String draft = null;
        while(current!=null){
            draft = getAttributeValue(current, LDMLConstants.DRAFT);
            if(draft!=null){
                if(draft.equals("true")){
                    return true;
                }else{
                    return false;
                }
            }
            current = current.getParentNode();
        }
        return false;
    }
    /**
     * Appends the attribute values that make differentiate 2 siblings
     * in LDML
     * @param node
     * @param xpath
     */
    public static void appendXPathAttribute(Node node, StringBuffer xpath){
        boolean terminate = false;
    	String val = getAttributeValue(node, LDMLConstants.TYPE);
        if(val!=null){
        	xpath.append("[@type='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, LDMLConstants.ALT);
        if(val!=null){
            xpath.append("and @alt='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, LDMLConstants.KEY);
        if(val!=null){
            xpath.append("and @key='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, LDMLConstants.REGISTRY);
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
    /**
     * Ascertains if the children of the given node are element
     * nodes.
     * @param node
     * @return
     */
    public static boolean areChildrenElementNodes(Node node){
        NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            if(list.item(i).getNodeType()==Node.ELEMENT_NODE){
                return true;
            }
        }
        return false;  
    }
    public static Node[] getNodeListAsArray( Node doc, String xpath){
        try{
            NodeList list = XPathAPI.selectNodeList(doc, xpath);
            int length = list.getLength();
            if(length>0){
                Node[] array = new Node[length];
                for(int i=0; i<length; i++){
                    array[i] = list.item(i);
                }
                return array;
            }
            return null;
        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        } 
    }
    /**
     * Fetches the list of nodes that match the given xpath
     * @param doc
     * @param xpath
     * @return
     */
    public static NodeList getNodeList( Document doc, String xpath){
        try{
            return XPathAPI.selectNodeList(doc, xpath);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }   
    }
    /**
     * Fetches the node from the document that matches the given xpath.
     * The context namespace node is required if the xpath contains 
     * namespace elments
     * @param doc
     * @param xpath
     * @param namespaceNode
     * @return
     */
    public static Node getNode(Document doc, String xpath, Node namespaceNode){
        try{
            NodeList nl = XPathAPI.selectNodeList(doc, xpath, namespaceNode);
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
    /**
     * Fetches the node from the document which matches the xpath
     * @param node
     * @param xpath
     * @return
     */
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
    /**
     * 
     * @param context
     * @param resToFetch
     * @param fullyResolved
     * @param xpath
     * @return
     */
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
    /**
     * Decide if the node is text, and so must be handled specially 
     * @param n
     * @return
     */
    private static boolean isTextNode(Node n) {
      if (n == null)
        return false;
      short nodeType = n.getNodeType();
      return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE;
    }   
    /**
     * Utility method to fetch the attribute value from the given 
     * element node
     * @param sNode
     * @param attribName
     * @return
     */
    public static String getAttributeValue(Node sNode, String attribName){
        String value=null;
        Node attr = sNode.getAttributes().getNamedItem(attribName);
        if(attr!=null){
            value = attr.getNodeValue();
        }
        return value;
    }
    /**
     * Utility method to fetch the value of the element node
     * @param node
     * @return
     */
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
    public static Document parseAndResolveAliases(String locale, String sourceDir, boolean ignoreError){
        try{
            Document full = parse(sourceDir+File.separator+ "root.xml");
            if(full!=null){
                full = resolveAliases(full, sourceDir, "root");
            }
           /*
            * Debugging
            *
            Node[] list = getNodeArray(full, LDMLConstants.ALIAS);
            if(list.length>0){
                System.err.println("Aliases not resolved!. list.getLength() returned "+ list.length);
            }*/
            return full;
        }catch(RuntimeException ex){
            if(!ignoreError){
                throw ex;
            }
        }
        return null;

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
    /**
     * Debugging method for printing out the DOM Tree
     * Prints the specified node, recursively. 
     * @param node
     * @param out
     */
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
