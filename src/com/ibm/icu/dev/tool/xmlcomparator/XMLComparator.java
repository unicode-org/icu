/*
******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /usr/cvs/icu4j/icu4j/src/com/ibm/icu/impl/ByteTrie.java,v $ 
* $Date: 2002/03/02 02:20:01 $ 
* $Revision: 1.5 $
*
******************************************************************************
*/

package com.ibm.icu.dev.tool.xmlcomparator;

/**
 * @author ram
 *
 * This tool compares locale data in XML format and generates HTML and XML reports
 */
import java.io.*;
import java.util.*;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.text.DecimalFormat;
import java.util.Calendar;


import java.util.Locale;

// DOM imports
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

// Needed JAXP classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

// SAX2 imports
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.lang.UCharacter;

public class XMLComparator {
    String[] fileNames; 
    private static final short OPT_SUN_JDK = 0x001;
    private static final short OPT_IBM_JDK = 0x002;
    private static final short OPT_WINDOWS = 0x004;
    private static final short OPT_HP_UX   = 0x008;
    private static final short OPT_SOLARIS = 0x010;
    private static final short OPT_IBM_TOR = 0x020;
    private static final short OPT_APPLE   = 0x040;
    private static final short OPT_ICU     = 0x080;
    private static final short OPT_OTHER   = 0x100;
    private static final short OPT_SOURCE  = 0x120;
    private static final short OPT_DEST    = 0x140;
    private static final short OPT_OUT1    = 0x160;
    private static final short OPT_OUT2    = 0x180;
    private static final short OPT_UNKNOWN = 0x1000;
    
    private static final String POSIX= "0";/* NO inheritence of locale data */
    private static final String ICU  = "1";/* Supports inheritence of locale data*/
    private static final String USER_OPTIONS[][] = {
        {"-sun_jdk", "sun_jdk",ICU}, 
        {"-ibm_jdk", "ibm_jdk",ICU},
        {"-windows", "windows", POSIX},
        {"-hp_ux",   "hp_ux",POSIX }, 
        {"-solaris", "solaris",POSIX },
        {"-ibm_tor", "ibm_toronto",POSIX},
        {"-apple",   "apple", POSIX},
        {"-icu",     "icu",ICU},
        {"-other",   "other", POSIX},
        {"-s",null,null},
        {"-d",null, null},
        {"-out1",null,null},
    };
    
    Hashtable optionTable = new Hashtable();
    private String sourceFolder = ".";
    private String destFolder = ".";
    private boolean out1 = false;
    private boolean out2 = false;
    
    public static void main(String[] args){
        XMLComparator comparator = new XMLComparator();
        comparator.processArgs(args);    
        
    }
    
    ArrayList doesNotExist = new ArrayList();
    ArrayList notAvailable = new ArrayList();
      
    /*
     * This application will compare different locale data xml files
     * conforming to localeElements.dtd and produces an xml file file
     * in the format
     */ 
                        
    Document resultDocument;
    String localeStr;    
    Locale locale;
    Calendar cal = Calendar.getInstance();
                 
    private void processArgs(String[] args){
        short options = identifyOptions(args);
        if ((args.length < 2) || ((options & OPT_UNKNOWN) != 0)) {
            printUsage();
            return;
        }
        boolean warning[] = new boolean[1];
        warning[0] = false;
        Enumeration enum = optionTable.keys();
        try{
            
            resultDocument = parse(sourceFolder+File.separator+"ResultXML.xml");
            
	        localeStr  = goldFileName.substring(goldFileName.lastIndexOf(File.separatorChar)+1,goldFileName.indexOf('.'));
	        locale = new Locale(localeStr.substring(0,localeStr.indexOf('_')),localeStr.substring(localeStr.indexOf('_')+1,localeStr.length()));
            OutputStreamWriter os1 = new OutputStreamWriter(new FileOutputStream(destFolder+File.separator+locale+".xml"),encoding);
	        OutputStreamWriter os2 = new OutputStreamWriter(new FileOutputStream(destFolder+File.separator+locale+".html"),encoding);
	            
            for(;enum.hasMoreElements();){
                String key = (String)enum.nextElement();
                String fileName = (String) optionTable.get(key);
                compare(goldFileName,goldKey, fileName,key,out1);
                
            }
           
	
            Iterator iter =doesNotExist.iterator();
            while(iter.hasNext()){
                  String key = (String)iter.next();
                  NodeList list = resultDocument.getElementsByTagName(key);
                  addNodeValue(list,key,"S.N.A",null);
            }
            notAvailable.add("solaris");
            notAvailable.add("apple");
            notAvailable.add("other");
            notAvailable.add("open_office");
            iter =notAvailable.iterator();
            while(iter.hasNext()){
                  String key = (String)iter.next();
                  NodeList list = resultDocument.getElementsByTagName(key);
                  addNodeValue(list,key,"S.N.A",null);
            }

            PrintWriter writer1 = new PrintWriter(os1);
            PrintWriter writer2 = new PrintWriter(os2);
            print(writer1,resultDocument);
            if(out1){
                printHTML_1(writer2);
            }else{
                printHTML(writer2);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
            
    }
    
    private void printUsage() {
        System.err.println("Usage: XMLComparator [<option>:<gold>] filename1 [option] filename2 ... \n"+
                           " XMLComparator [-sun_jdk:gold]  filename" +
                           " [-ibm_jdk] filename [-windows] filename" +
                           " [-hp_ux]  filename [-solaris] filename"  +
                           " [-ibm_tor] filename [-apple] filename"   +
                           " [-icu] filename [-other] filename"  
                           );
    }
    private String goldFileName; 
    private String goldKey;

    private short identifyOptions(String[] options) {
        short result = 0;
        for (int j = 0; j < options.length; j++) {
            String option = options[j];
            boolean isGold = false;
            if (option.startsWith("-")) {
                if(option.indexOf(":gold")>0){
                    option = option.substring(0,option.indexOf(":"));
                    isGold = true;    
                }
                boolean optionRecognized = false;
                for (short i = 0; i < USER_OPTIONS.length; i++) {
                   
                    if (USER_OPTIONS[i][0].equals(option)) {
                        result |= (short)(1 << i);
                        optionRecognized = true;
                        if(i==9){
                            sourceFolder = options[++j];
                        }else if(i==10){
                            destFolder = options[++j];
                        }else if(i==11){
                            out1=true;
                        }else{
                            if(!isGold){
                                optionTable.put(USER_OPTIONS[i][1],options[++j]);
                            }else{
                                goldFileName = options[++j];
                                goldKey      = USER_OPTIONS[i][1];
                            }
                        }
                        break;
                    }
                }
                if (!optionRecognized) {
                    result |= OPT_UNKNOWN;
                }
            }
        }
        return result;
    }
    
    private void   printHTML(PrintWriter writer){
        NodeList list= resultDocument.getElementsByTagName("difference_element");
        writer.println("<html>\n"+
                           "    <head>\n"+
                           "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"+
                           "        <title>"+locale+"</title>\n"+
                           "    </head>\n"+
                           "    <body>\n"+
                           "        <p><b>"+locale+"</b></p>\n"+
                           "        <table border=\"1\" cellspacing=\"0\">\n"+
                           "            <tr>\n"+
                           "                <th>ParentNode</th>\n"+
                           "                <th>Name</th>\n"+
                           "                <th>ID</th>\n"+
                           "                <th>ICU 2.1</th>\n"+
                           "                <th>IBM Toronto</th>\n"+
                           "                <th>Windows 2000</th>\n"+
                           "                <th>Sun JDK 1.3</th>\n"+
                           "                <th>IBM JDK 1.3</th>\n"+
                           "                <th>Solaris</th>\n"+
                           "                <th>Apple</th>\n"+
                           "                <th>HPUX 11</th>\n"+
                           "                <th>Open Office</th>\n"+
                           "                <th>Other</th>\n"+
                           "            </tr>\n");
                           
        for(int i =0; i<list.getLength();i++){
             Node diffElem = list.item(i);
             writer.println("            <tr>\n");
             NamedNodeMap attrb = diffElem.getAttributes();
             if(attrb.item(0).getNodeValue().equals("test")) continue;
             writer.println("                <td>" + attrb.item(0).getNodeValue()+"</td>");
             writer.println("                <td>" + attrb.item(1).getNodeValue()+"</td>");
             //attribute 2 is ignored
             writer.println("                <td>" + attrb.item(3).getNodeValue()+"</td>");
             
             NodeList childList = diffElem.getChildNodes();
             for(int j=0; j<childList.getLength();j++){
                 Node current = childList.item(j);
                 if(current!=null && current.getNodeType()==Node.ELEMENT_NODE){
                      //System.out.println(current.getNodeName());
                      String val = current.getFirstChild().getNodeValue();
                      if(val==null || (val=trim(val)).equals("")) val="&nbsp;";
                      writer.println("                <td>"+val+"</td>");
                 }
             }       

              writer.println("            </tr>\n");
        }   
        writer.println( "        </table>\n"+
                            "    </body>\n"+
                            "</html>");
        writer.flush();
    }
    private void   printHTML_1(PrintWriter writer){
        NodeList list= resultDocument.getElementsByTagName("difference_element");
        writer.println("<html>\n"+
                           "    <head>\n"+
                           "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"+
                           "        <title>"+localeStr+"</title>\n"+
                           "    </head>\n"+
                           "    <body>\n"+
                           "        <p><b>"+localeStr+"("+locale.getDisplayLanguage()+"_"+locale.getDisplayCountry()+")</b></p>\n"+
                           "        <table border=\"1\" cellspacing=\"0\">\n"+
                           "            <tr>\n"+
                           "                <th>ParentNode</th>\n"+
                           "                <th>Name</th>\n"+
                           "                <th>ID</th>\n"+
                           "                <th>ICU 2.1</th>\n"+
                           "                <th>IBM Toronto</th>\n"+
                           "                <th>Windows 2000</th>\n"+
                           "                <th>Sun JDK 1.3</th>\n"+
                           "                <th>IBM JDK 1.3</th>\n"+
                           "                <th>Solaris</th>\n"+
                           "                <th>Apple</th>\n"+
                           "                <th>HPUX 11</th>\n"+
                           "                <th>Open Office</th>\n"+
                           "                <th>Other</th>\n"+
                           "            </tr>\n");
        
        Hashtable colorHash = new Hashtable();
        colorHash.put("icu","#D3D3D3");        
        colorHash.put("ibm_toronto","#FF7777");
        colorHash.put("windows","#98FB98");
        colorHash.put("sun_jdk","#DDDDFF");
        colorHash.put("ibm_jdk","#FFBBBB");
        colorHash.put("hp_ux","#FFE4B5");
        colorHash.put("apple","#FFBBBB");
        colorHash.put("solaris","#E0FFFF");
        colorHash.put("other","#CCCCFF");
        boolean hasData =false;          
        for(int i =0; i<list.getLength();i++){
             Node diffElem = list.item(i);
             NamedNodeMap attrb = diffElem.getAttributes();
            
             if(attrb.item(0).getNodeValue().equals("test") || attrb.item(0).getNodeValue().equals("identity")) continue;
             
             writer.println("            <tr>\n");
             writer.println("                <td>" + attrb.item(0).getNodeValue()+"</td>");
             writer.println("                <td>" + attrb.item(1).getNodeValue()+"</td>");
             //attribute 2 is ignored
             writer.println("                <td>" + attrb.item(3).getNodeValue()+"</td>");
             
             NodeList childList = diffElem.getChildNodes();
             for(int j=0; j<childList.getLength();j++){
                 Node current = childList.item(j);
                 if(current!=null && current.getNodeType()==Node.ELEMENT_NODE){
                      
                      //System.out.println(current.getNodeName());
                      String val = current.getFirstChild().getNodeValue();
                      if(val==null || (val=trim(val)).equals("")) val="&nbsp;";
                      String color="#FFFFFF";
                      if(!val.equals("S.N.A") && !val.equals("&nbsp;")){
	                      for(int k=0; k<childList.getLength();k++){
	                         Node n = childList.item(k);
	                         if(n!=null && n.getNodeType()==Node.ELEMENT_NODE){
                                String nVal =n.getFirstChild().getNodeValue();
                                //-ibm_jdk c:\NLTC\IBMJDK\XML\cs_CZ.xml -sun_jdk c:\NLTC\Java\xml\cs_CZ.xml -windows c:\NLTC\windows\xml\cs_CZ.xml -hp_ux c:\NLTC\hp\xml\cs_CZ.xml
                                
                                if( (!nVal.equals("S.N.A")&& !val.equals("&nbsp;"))&&
                                    (
                                     val.equals(nVal) ||
                                     ((attrb.item(1).getNodeValue().equals("pattern"))&&
                                     comparePatterns(val,nVal))||
                                     ((attrb.item(0).getNodeValue().equals("patterns"))&&
                                     comparePatterns(val,nVal))
                                    )
                                  ){ 
		                            String str = (String)colorHash.get(n.getNodeName());
		                            if(str!=null){
		                                color=str;
		                            }
                                   
		                            break;
                                   
                                }
	                         }
	                      }
                      }
                      if(!val.equals("S.N.A")){
	                      NamedNodeMap attList = current.getAttributes();
	                      if(attList.getNamedItem("case_diff").getNodeValue().equals("false")){
	                        writer.println("                <td bgcolor="+color+">"+val+"</td>");
	                      }else{
	                        writer.println("                <td bgcolor="+color+">"+
	                                       val+
	                                       "<font color=#A52A2A> (case difference only)</font>"+
	                                       "</td>");
	                      }
	                      hasData=true;
                      }
                 }
             }       

              writer.println("            </tr>\n");
        }
  
        writer.println( "        </table>\n");
        
        if(!hasData){
            writer.println( "       <p><font size=16 color=#FF0000> Data exists only in ICU </font></p>\n");
        } 
        writer.println("        <p>Created on: " + cal.getTime() +"</p>\n"+
                            "    </body>\n"+
                            "</html>");
        writer.flush();
    }


    /**
     * Compare two files by parsing into DOMs and comparing trees.
     * @param goldFileName expected file
     * @param testFileName actual file
     * @param reporter PrintWriter to dump status info to
     * @param array of warning flags (for whitespace diffs, I think?)
     * NEEDSDOC @param warning
     * @param attributes to attempt to set onto parsers
     * @return true if they match, false otherwise
     */
    private Document goldDoc=null;
    
    public Document getFullyResolvedLocale(String localeName,String fileName){
        // here we assume that "_" is the delimiter
        Document doc = parse(fileName);
        String temp = fileName;    
		if((temp.indexOf("icu")>=0 ) || (temp.indexOf("jdk")>=0)){
		    // OK we have a posix style locale data
		    // merge all data in inheritence tree into goldDoc
		    int index = -1;
		    while((index = temp.indexOf("_")) >0 && index > fileName.lastIndexOf("\\")){
		        Document parentDoc = parse((temp = temp.substring(0,index)+".xml"));
		        if(parentDoc!=null){
		           mergeElements(doc.getDocumentElement(),parentDoc.getDocumentElement());
		        }
		    }
            String rootFileName="" ;
            if(fileName.lastIndexOf(File.separatorChar)!=-1){
                rootFileName = fileName.substring(0,fileName.lastIndexOf(File.separatorChar)+1);
            }
            rootFileName = rootFileName+"root.xml";        
            Document rootDoc = parse(rootFileName);

            mergeElements(doc.getDocumentElement(),rootDoc.getDocumentElement());
		}
        return doc;
    }
    public boolean compare(String goldFileName, String goldKey, 
                           String testFileName, String testKey, boolean mergeData)
    {
        
        // parse the gold doc only if it is null
        if(goldDoc==null){
            goldDoc = getFullyResolvedLocale(goldKey,goldFileName);
        }

        // parse the test doc only if gold doc was parsed OK
        
        Document testDoc = null;
        if(goldDoc!=null)
         testDoc = getFullyResolvedLocale(testKey,testFileName);
        if (null == goldDoc)
        {
            doesNotExist.add(goldKey);
            return false;
        }
        else if (null == testDoc)
        {
            doesNotExist.add(testKey);
            return false;
        }
        if(mergeData){
            return extractMergeData(goldDoc,goldKey,testDoc,testKey);
        }else{   
            return compareElementsAndData(goldDoc,goldKey,testDoc,testKey);
        }
    }
    boolean canonical  = false;
    
    String  encoding   = "UTF-8"; // default encoding
    
    /** Returns a sorted list of attributes. */
    protected Attr[] sortAttributes(NamedNodeMap attrs) {

        int len = (attrs != null) ? attrs.getLength() : 0;
        Attr array[] = new Attr[len];
        for ( int i = 0; i < len; i++ ) {
            array[i] = (Attr)attrs.item(i);
        }
        for ( int i = 0; i < len - 1; i++ ) {
            String name  = array[i].getNodeName();
            int    index = i;
            for ( int j = i + 1; j < len; j++ ) {
                String curName = array[j].getNodeName();
                if ( curName.compareTo(name) < 0 ) {
                    name  = curName;
                    index = j;
                }
            }
            if ( index != i ) {
                Attr temp    = array[i];
                array[i]     = array[index];
                array[index] = temp;
            }
        }

        return(array);

    } // sortAttributes(NamedNodeMap):Attr[]
    
        /** Normalizes the given string. */
    protected String normalize(String s) {
        StringBuffer str = new StringBuffer();

        int len = (s != null) ? s.length() : 0;
        for ( int i = 0; i < len; i++ ) {
            char ch = s.charAt(i);
            switch ( ch ) {
            case '<': {
                    str.append("&lt;");
                    break;
                }
            case '>': {
                    str.append("&gt;");
                    break;
                }
            case '&': {
                    str.append("&amp;");
                    break;
                }
            case '"': {
                    str.append("&quot;");
                    break;
                }
            case '\'': {
                    str.append("&apos;");
                    break;
                }
            case '\r':
            case '\n': {
                    if ( canonical ) {
                        str.append("&#");
                        str.append(Integer.toString(ch));
                        str.append(';');
                        break;
                    }
                    // else, default append char
                }
            default: {
                    str.append(ch);
                }
            }
        }

        return(str.toString());

    } // normalize(String):String


    /** Prints the specified node, recursively. */
    public void print(PrintWriter out, Node node) {

        // is there anything to do?
        if ( node == null ) {
            return;
        }

        int type = node.getNodeType();
        
        switch ( type ) {
        // print document
        case Node.DOCUMENT_NODE: {
               if ( !canonical ) {
                    out.println("<?xml version=\"1.0\" encoding=\""+
                                encoding + "\"?>");
                }
                //print(((Document)node).getDocumentElement());

                NodeList children = node.getChildNodes();
                for ( int iChild = 0; iChild < children.getLength(); iChild++ ) {
                    print(out,children.item(iChild));
                }
                out.flush();
                break;
            }

            // print element with attributes
        case Node.ELEMENT_NODE: {
                out.print('<');
                out.print(node.getNodeName());
                Attr attrs[] = sortAttributes(node.getAttributes());
                for ( int i = 0; i < attrs.length; i++ ) {
                    Attr attr = attrs[i];
                    out.print(' ');
                    out.print(attr.getNodeName());
                    out.print("=\"");
                    out.print(normalize(attr.getNodeValue()));
                    out.print('"');
                }
                out.print('>');
                NodeList children = node.getChildNodes();
                if ( children != null ) {
                    int len = children.getLength();
                    for ( int i = 0; i < len; i++ ) {
                        print(out,children.item(i));
                    }
                }
                break;
            }

            // handle entity reference nodes
        case Node.ENTITY_REFERENCE_NODE: {
                if ( canonical ) {
                    NodeList children = node.getChildNodes();
                    if ( children != null ) {
                        int len = children.getLength();
                        for ( int i = 0; i < len; i++ ) {
                            print(out,children.item(i));
                        }
                    }
                } else {
                    out.print('&');
                    out.print(node.getNodeName());
                    out.print(';');
                }
                break;
            }

            // print cdata sections
        case Node.CDATA_SECTION_NODE: {
                if ( canonical ) {
                    out.print(normalize(node.getNodeValue()));
                } else {
                    out.print("<![CDATA[");
                    out.print(node.getNodeValue());
                    out.print("]]>");
                }
                break;
            }

            // print text
        case Node.TEXT_NODE: {
                out.print(normalize(node.getNodeValue()));
                break;
            }

            // print processing instruction
        case Node.PROCESSING_INSTRUCTION_NODE: {
                out.print("<?");
                out.print(node.getNodeName());
                String data = node.getNodeValue();
                if ( data != null && data.length() > 0 ) {
                    out.print(' ');
                    out.print(data);
                }
                out.println("?>");
                break;
            }
        }

        if ( type == Node.ELEMENT_NODE ) {
            out.print("</");
            out.print(node.getNodeName());
            out.print('>');
        }

        out.flush();
    } // print(Node)
    
    // Reporter format:
    // REASON_CONSTANT;gold val;test val;reason description
    private void addNodeValue(NodeList list,String nodeName,String nodeValue,String caseDiff){
        for(int i=0;i<list.getLength();i++){
            Node node = list.item(i);
            if(nodeName.equals(node.getNodeName())){   
                node.getFirstChild().setNodeValue(nodeValue);
                if(caseDiff!=null){
	                NamedNodeMap attr = node.getAttributes();
	                Node case_diff = attr.getNamedItem("case_diff");
	                case_diff.setNodeValue(caseDiff);
                }
            }
        }
    }
    private void addElement(String parentNode, String childNode, String attr,
                               String nodeName1,  String value1,
                               String nodeName2, String value2){
            String id ;
            String caseDiff=null;
            if(Normalizer.compare(value1,value2,0)!=0){
                //System.out.println("Value1: "+value1+ "  Value2: "+ value2);
	            if(Normalizer.compare(value1,value2,Normalizer.COMPARE_IGNORE_CASE)==0){
	                caseDiff="true";
	            }
            }
            if(attr!=null){
                id = parentNode+"_"+childNode+"_"+attr;
            }else{
                id = parentNode+"_"+childNode;
            }
                
            Element element = resultDocument.getElementById(id);
            NodeList nodes=null;
            NamedNodeMap attribMap=null;
            Node rootNode =resultDocument.getElementsByTagName("difference_xml").item(0);
            if(element!=null){
                nodes = element.getChildNodes();
                attribMap=element.getAttributes();              
            }else{
               NodeList nodeList=resultDocument.getElementsByTagName("difference_element");
               // the first node is always the empty one in the result document
               // so get that node and clone it
               Node node = nodeList.item(0).cloneNode(true);
               attribMap=node.getAttributes();
               nodes= node.getChildNodes();
            }
            addNodeValue(nodes,nodeName1,value1,(nodeName1.equals(goldKey))?null:caseDiff);
            addNodeValue(nodes,nodeName2,value2,(nodeName2.equals(goldKey))?null:caseDiff);
            Node parent_node = attribMap.getNamedItem("parent_node");
            Node node_name   = attribMap.getNamedItem("node_name");
            Node id_attrib   = attribMap.getNamedItem("id");
            Node index       = attribMap.getNamedItem("index");
            parent_node.setNodeValue(parentNode);
            node_name.setNodeValue(childNode);
            id_attrib.setNodeValue(id);
            index.setNodeValue((attr==null) ? "&nbsp;" : attr);
            rootNode.appendChild(nodes.item(0).getParentNode());       
    }
    private void addDifference(String parentNode, String childNode, String attr,
                               String nodeName1,  String value1,
                               String nodeName2, String value2){
            String id ;
            // Decompose the values and find out if they are equal
            if(Normalizer.compare(value1,value2,0)==0){
                // the values are equal just return
                return;
            }
            String caseDiff= null;
            if(Normalizer.compare(value1,value2,Normalizer.COMPARE_IGNORE_CASE)==0){
                caseDiff="true";
            }
            if(attr!=null){
                id = parentNode+"_"+childNode+"_"+attr;
            }else{
                id = parentNode+"_"+childNode;
            }
                
            Element element = resultDocument.getElementById(id);
            NodeList nodes=null;
            NamedNodeMap attribMap=null;
            Node rootNode =resultDocument.getElementsByTagName("difference_xml").item(0);
            if(element!=null){
                nodes = element.getChildNodes();
                attribMap=element.getAttributes();              
            }else{
               NodeList nodeList=resultDocument.getElementsByTagName("difference_element");
               // the first node is always the empty one in the result document
               // so get that node and clone it
               Node node = nodeList.item(0).cloneNode(true);
               attribMap=node.getAttributes();
               nodes= node.getChildNodes();
            }
            addNodeValue(nodes,nodeName1,value1,caseDiff);
            addNodeValue(nodes,nodeName2,value2,caseDiff);
            Node parent_node = attribMap.getNamedItem("parent_node");
            Node node_name   = attribMap.getNamedItem("node_name");
            Node id_attrib   = attribMap.getNamedItem("id");
            Node index       = attribMap.getNamedItem("index");
            parent_node.setNodeValue(parentNode);
            node_name.setNodeValue(childNode);
            id_attrib.setNodeValue(id);
            index.setNodeValue((attr==null) ? "&nbsp;" : attr);
            rootNode.appendChild(nodes.item(0).getParentNode());
            
            
            
    }
    
    // ---------------------------------------------------------------------------
    //
    //   Merge element nodes.  dest and source are Element nodes of the same type.
    //                         Move any children Elements that exist in the source but not in the
    //                         destination into the destination
    //
    // ---------------------------------------------------------------------------
    private void  mergeElements(Node dest, Node source) {
        Node   childOfSource;
        Node   childOfDest;
        
        Document destDoc = dest.getOwnerDocument();
        Node spaces = destDoc.createTextNode("\n       ");
        
        for (childOfSource = source.getFirstChild(); childOfSource != null; childOfSource = childOfSource.getNextSibling()) {
            String dNodeVal = dest.getFirstChild().getNodeValue();
            String sNodeVal = childOfSource.getNodeValue();
            if (childOfSource.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if(childOfSource.getNodeName().equals("collation") 
                || childOfSource.getNodeName().equals("timeZoneNames")
                ){
                continue;
            }
            boolean didMerge = false;
            for (childOfDest = dest.getFirstChild(); childOfDest != null; childOfDest = childOfDest.getNextSibling()) {
                String childNodeName = childOfDest.getNodeName();
                String childNodeVal = childOfDest.getNodeValue();
                if (childOfDest.getNodeType() == Node.ELEMENT_NODE  &&
                       childOfDest.getNodeName().equals(childOfSource.getNodeName())) {
                    // The destination document already has an element of this type at this level.
                    //   Recurse to pick up any extra children that the source node may have.
                    mergeElements(childOfDest, childOfSource);
                    didMerge = true;
                    break;
                }
            }
    
            if (didMerge == false) {
                // destination document had no corresponding node to the current childOfSource.  Add it.
                Node importedNode = destDoc.importNode(childOfSource, true);
                dest.appendChild(spaces);
                dest.appendChild(importedNode);
            }
        }
        //dest.appendChild(spaces);
        
    }
    
    double args1  = 10000000000.00;
    double args2  = -10000000000.00;
    
    DecimalFormat fmt = new DecimalFormat(); 
    
    private boolean comparePatterns(String pat1,String pat2){
        fmt.applyPattern(pat1);
        String s1 = fmt.format(args1);
        String s3 = fmt.format(args2);
        fmt.applyPattern(pat2);
        String s2 = fmt.format(args1);
        String s4 = fmt.format(args2);
        if(s1.equals(s2) && s3.equals(s4)){
            return true;
        }
        return false;
    }
    private boolean compareElementsAndData(Node goldNode,String goldKey,
                                           Document testDoc,String testKey){
    
        Node childOfSource;

        for(childOfSource = goldNode.getFirstChild(); childOfSource != null; childOfSource = childOfSource.getNextSibling()) {
            if (childOfSource.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if(childOfSource.getNodeName().equals("collation") 
                || childOfSource.getNodeName().equals("timeZoneNames")
                || childOfSource.getNodeName().equals("versioning")){
                continue;
            }
            NamedNodeMap attr = childOfSource.getAttributes();       
            NodeList destList = testDoc.getElementsByTagName(childOfSource.getNodeName());
            Node sourceID = attr.getNamedItem("id");
           
    
            for(int i =0 ; i<destList.getLength(); i++){
                Node destNode =destList.item(i);
                NamedNodeMap destAttr = destNode.getAttributes();
                /* get the source and destination nodes with attribute id */
                Node destID = destAttr.getNamedItem("id");
                if(destID!=null && sourceID!=null){
                    String sourceVal =sourceID.getNodeValue();
                    String destVal = destID.getNodeValue();
                    
                    if(sourceVal.equals(destVal)){
                        //System.out.println(sourceVal);
                        //System.out.println(destVal);
                        if(destNode.hasChildNodes() && childOfSource.hasChildNodes()){
                            if(destNode.getParentNode().getNodeName().equals(childOfSource.getParentNode().getNodeName())){
                                String destNodeVal = trim(destNode.getFirstChild().getNodeValue());
                                String sourceNodeVal = trim(childOfSource.getFirstChild().getNodeValue());

                                //System.out.println(childOfSource.getFirstChild().getNodeName());
                                //System.out.println(destNode.getFirstChild().getNodeName());
                                if(!destNodeVal.equals(sourceNodeVal)){
                                    boolean write = true;
                                    String parentNodeName = trim(childOfSource.getParentNode().getNodeName());
                                    String childNodeName  = trim(childOfSource.getNodeName());
                                    if(childNodeName.equals("pattern")){
                                        if(comparePatterns(sourceNodeVal,destNodeVal)){
                                            write = false;
                                        }
                                    }
                                    if(write){
                                        addDifference(parentNodeName,
                                                      childNodeName,
                                                      destVal,
                                                      testKey,
                                                      destNodeVal,
                                                      goldKey,
                                                      sourceNodeVal);
                                    }
    
                                }
                            }
                        }
                    }
                }else{
                    if(destNode.getNodeName().equals(childOfSource.getNodeName())&&
                        destNode.getParentNode().getNodeName().equals(childOfSource.getParentNode().getNodeName())
                       ){
                        if(destNode.hasChildNodes() && childOfSource.hasChildNodes()){
    
                            String destNodeVal = trim(destNode.getFirstChild().getNodeValue());
                            String sourceNodeVal = trim(childOfSource.getFirstChild().getNodeValue());
    
                            String attID = null;
                            if(!destNodeVal.equals(sourceNodeVal)){
                                boolean write = true;
                                String parentNodeName = childOfSource.getParentNode().getNodeName();
                                String childNodeName  = childOfSource.getNodeName();
                                if(childNodeName.equals("pattern")){
                                        if(comparePatterns(sourceNodeVal,destNodeVal)){
                                            write = false;
                                        }
                                }
                                if(write){
                                    addDifference(parentNodeName,
                                                  childNodeName,
                                                  attID,
                                                  testKey,
                                                  destNodeVal,
                                                  goldKey,
                                                  sourceNodeVal);
                                }
                            }
                        }
                    }
                
                }
            }
      
            compareElementsAndData(childOfSource,goldKey,testDoc,testKey);
        }
       return true;
    }    
    private String trim(String source){
        char[] src = source.toCharArray();
        char[] dest = new char[src.length];

        int start=0;
        while(start<(src.length) && (UCharacter.isWhitespace(src[start])|| src[start]==0xA0)){start++;}
        int stop=src.length-1;
        while(stop>0 && (UCharacter.isWhitespace(src[stop])||(src[stop]==0xA0))){stop--;}
        if(stop!=-1 && start!=src.length){
	        System.arraycopy(src,start,dest,0,(stop-start)+1);
	        return new String(dest,0,(stop-start)+1);
        }else{
            return new String();
        }
      
    }
    private boolean extractMergeData(Node goldNode,String goldKey,
                                           Document testDoc,String testKey){
    
        Node childOfSource;

        for(childOfSource = goldNode.getFirstChild(); childOfSource != null; childOfSource = childOfSource.getNextSibling()) {
            if (childOfSource.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if(childOfSource.getNodeName().equals("collation") 
                || childOfSource.getNodeName().equals("timeZoneNames")
                || childOfSource.getNodeName().equals("versioning")){
                continue;
            }
            NamedNodeMap attr = childOfSource.getAttributes();       
            NodeList destList = testDoc.getElementsByTagName(childOfSource.getNodeName());
            Node sourceID = attr.getNamedItem("id");
    
            for(int i =0 ; i<destList.getLength(); i++){
                Node destNode =destList.item(i);
                NamedNodeMap destAttr = destNode.getAttributes();
                /* get the source and destination nodes with attribute id */
                Node destID = destAttr.getNamedItem("id");
                if(destID!=null && sourceID!=null){
                    String sourceIDVal =sourceID.getNodeValue();
                    String destIDVal = destID.getNodeValue();
                    
                    if(sourceIDVal.equals(destIDVal)){
                        //System.out.println(sourceVal);
                        //System.out.println(destVal);
                        if(destNode.hasChildNodes() && childOfSource.hasChildNodes()){
                            if(destNode.getParentNode().getNodeName().equals(childOfSource.getParentNode().getNodeName())){
                                String destNodeVal = trim(destNode.getFirstChild().getNodeValue());
                                String sourceNodeVal = trim(childOfSource.getFirstChild().getNodeValue());
                                String parentNodeName = trim(childOfSource.getParentNode().getNodeName());
                                String childNodeName  = trim(childOfSource.getNodeName());
                                if(sourceNodeVal.length()!=0){
                                    addElement(parentNodeName,
                                                      childNodeName,
                                                      destIDVal,
                                                      testKey,
                                                      destNodeVal,
                                                      goldKey,
                                                      sourceNodeVal);
                                }
                                //System.out.println(childOfSource.getFirstChild().getNodeName());
                                //System.out.println(destNode.getFirstChild().getNodeName());
                            }
                        }
                    }
                }else{
                    if(destNode.getNodeName().equals(childOfSource.getNodeName())&&
                        destNode.getParentNode().getNodeName().equals(childOfSource.getParentNode().getNodeName())
                       ){
                        if(destNode.hasChildNodes() && childOfSource.hasChildNodes()){
    
                            String destNodeVal = trim(destNode.getFirstChild().getNodeValue());
                            String sourceNodeVal = trim(childOfSource.getFirstChild().getNodeValue());
    
                            String parentNodeName = trim(childOfSource.getParentNode().getNodeName());
                            String childNodeName  = trim(childOfSource.getNodeName());
                            if(sourceNodeVal.length()!=0){
                                addElement(parentNodeName,
                                                  childNodeName,
                                                  null,
                                                  testKey,
                                                  destNodeVal,
                                                  goldKey,
                                                  sourceNodeVal);
                            }
                        }
                    }
                
                }
            }
      
            extractMergeData(childOfSource,goldKey,testDoc,testKey);
        }
       return true;
    } 

    /**
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
     * 
     * @param String local path\filename of a file
     * @return a file:/// URL, the same string if it appears to 
     * already be a URL, or null if error
     */
    public static String filenameToURL(String filename)
    {
        // null begets null - something like the commutative property
        if (null == filename)
            return null;

        // Don't translate a string that already looks like a URL
        if (filename.startsWith("file:")
            || filename.startsWith("http:")
            || filename.startsWith("ftp:")
            || filename.startsWith("gopher:")
            || filename.startsWith("mailto:")
            || filename.startsWith("news:")
            || filename.startsWith("telnet:")
           )
            return filename;

        File f = new File(filename);
        String tmp = null;
        try
        {
            // This normally gives a better path
            tmp = f.getCanonicalPath();
        }
        catch (IOException ioe)
        {
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
        if (tmp.startsWith("/"))
            return "file://" + tmp;
        else
            return "file:///" + tmp;

    }


    /**
     * Simple worker method to parse filename to a Document.  
     *
     * Attempts XML parse, then HTML parse (when parser available), 
     * then just parses as text and sticks into a text node.
     *
     * @param filename to parse as a local path
     * @param attributes name=value pairs to set on the 
     * DocumentBuilderFactory that we use to parse
     *
     * @return Document object with contents of the file; 
     * otherwise throws an unchecked RuntimeException if there 
     * is any fatal problem
     */
    Document parse(String filename)
    {
        // Force filerefs to be URI's if needed: note this is independent of any other files
        String docURI = filenameToURL(filename);
        return parse(new InputSource(docURI),filename);
    }
    
    Document parse(InputSource docSrc, String filename){
        
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        // Always set namespaces on
        dfactory.setNamespaceAware(true);
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
            System.out.println("ERROR :" + se.toString());
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
}
