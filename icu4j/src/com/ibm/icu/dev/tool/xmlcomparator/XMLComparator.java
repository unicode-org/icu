/*
 *******************************************************************************
 * Copyright (C) 2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source:   $
 * $Date:  Aug 12, 2003 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
*/
package com.ibm.icu.dev.tool.xmlcomparator;

/**
 * @author ram
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
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
    private static final short OPT_LINUX   = 0x160;
    private static final short OPT_AIX     = 0x180;
    private static final short OPT_UNKNOWN = 0x2000;
    
    private static final String POSIX= "0";/* NO inheritence of locale data */
    private static final String ICU  = "1";/* Supports inheritence of locale data*/
    private static final String USER_OPTIONS[][] = {
        {"-sun_jdk",        "sun_jdk",ICU}, 
        {"-ibm_jdk",        "ibm_jdk",ICU},
        {"-windows",        "windows", POSIX},
        {"-hp_ux",          "hp_ux",POSIX }, 
        {"-solaris",        "solaris",POSIX },
        {"-ibm_tor",        "ibm_toronto",POSIX},
        {"-apple",          "apple", POSIX},
        {"-icu",            "icu",ICU},
        {"-open_office",    "open_office", POSIX},
        {"-aix",            "aix", POSIX},
        {"-linux",          "linux", POSIX},
        {"-s",null,null},
        {"-d",null, null},
    };
    
    Hashtable optionTable = new Hashtable();
    private String sourceFolder = ".";
    private String destFolder = ".";
    
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
    Hashtable colorHash = new Hashtable();
    XMLComparator(){
        //initialize the color hash
        colorHash.put("icu","#AD989D");        
        colorHash.put("ibm_toronto","#FF7777");
        colorHash.put("windows","#98FB98");
        colorHash.put("sun_jdk","#FF6633");
        colorHash.put("ibm_jdk","#CCFFFF");
        colorHash.put("hp_ux","#FFE4B5");
        colorHash.put("apple","#FFBBBB");
        colorHash.put("solaris","#CC9966");
        colorHash.put("open_office","#FFFF33");
        colorHash.put("aix", "#EB97FE");
        colorHash.put("linux", "#1191F1");
    }
    /**
     * A helper function to convert a string of the form
     * aa_BB_CC to a locale object.  Why isn't this in Locale?
     */
    public static Locale getLocaleFromName(String name) {
        String language = "";
        String country = "";
        String variant = "";

        int i1 = name.indexOf('_');
        if (i1 < 0) {
            language = name;
        } else {
            language = name.substring(0, i1);
            ++i1;
            int i2 = name.indexOf('_', i1);
            if (i2 < 0) {
                country = name.substring(i1);
            } else {
                country = name.substring(i1, i2);
                variant = name.substring(i2+1);
            }
        }

        return new Locale(language, country, variant);
    }
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
       
            locale = getLocaleFromName(localeStr);
            
            OutputStreamWriter os1 = new OutputStreamWriter(new FileOutputStream(destFolder+File.separator+locale+".xml"),encoding);
            OutputStreamWriter os2 = new OutputStreamWriter(new FileOutputStream(destFolder+File.separator+locale+".html"),encoding);
            
            writeToResultDoc(goldFileName, goldKey);
            for(;enum.hasMoreElements();){
                String key = (String)enum.nextElement();
                String fileName = (String) optionTable.get(key);
                writeToResultDoc(fileName,key);
                
            }
            PrintWriter writer1 = new PrintWriter(os1);
            PrintWriter writer2 = new PrintWriter(os2);
            print(writer1,resultDocument);
            printHTML(writer2);
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
                           " [-icu] filename [-open_office] filename" +
                           " [-aix] filename [-linux] filename" 
                           );
    }
    private String goldFileName; 
    private String goldKey;
    private String goldType;
    
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
                        if(i==11){
                            sourceFolder = options[++j];
                        }else if(i==12){
                            destFolder = options[++j];
                        }else{
                            if(!isGold){
                                optionTable.put(USER_OPTIONS[i][1],options[++j]);
                            }else{
                                goldFileName = options[++j];
                                goldKey      = USER_OPTIONS[i][1];
                                goldType     = USER_OPTIONS[i][2];
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
    private Hashtable platformNumber = new Hashtable();
    private int numPlatforms = 0;
    private void printTableHeader(PrintWriter writer, Node node){
        Node firstChildElement=null;
        NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            firstChildElement =list.item(i);
            if(firstChildElement.getNodeType()==Node.ELEMENT_NODE && firstChildElement.getNodeName().equals("element")){
                break;
            }
        }
        writer.println("            <tr>\n" +
                       "                <th>N.</th>\n"+
                       "                <th>ParentNode</th>\n"+
                       "                <th>Name</th>\n"+
                       "                <th>ID</th>");
        do{
            NamedNodeMap map = firstChildElement.getAttributes();
            String name = map.getNamedItem("platform").getNodeValue();
            String printName;
            String folder;
            if(name.equals("icu")){
                printName = "common";
            }else{
                printName = name;
                
            }
            folder = printName+"/xml/";
            writer.println("                <th bgcolor=\""+
                           (String)colorHash.get(name)+ "\">" +
                           printName.toUpperCase()+
                           " (<a href=\"http://oss.software.ibm.com/cvs/icu/~checkout~/locale/"+folder+localeStr+".xml\">xml</a>)"+
                           "</th>");
            numPlatforms++;
            platformNumber.put(name,new Integer(numPlatforms));
        }while((firstChildElement=firstChildElement.getNextSibling())!=null);
        writer.println("            </tr>");
    }

    private int numWritten = 0;
    private void printValue(Node firstChildNode, Node currentNode, PrintWriter writer){
        String platform = currentNode.getAttributes().getNamedItem("platform").getNodeValue();
        String color = (String)colorHash.get(platform);
        String currentValue = currentNode.getFirstChild().getNodeValue();
        boolean caseDiff = false;
        int mynum = ((Integer)platformNumber.get(platform)).intValue(); 
        boolean colorPicked = false;
        boolean isEqual = false;
        do{
            if(firstChildNode==currentNode){
                
                if(numWritten != mynum-1 && numWritten < numPlatforms && mynum>0){
                    for(int i=numWritten; i<mynum-1; i++){
                        writer.println("                <td>&nbsp;</td>");
                    }
                    numWritten = mynum-1;
                }
                //print and break
                if(isEqual){
                    currentValue = "=";
                }
                if(caseDiff==true){
                    writer.println("                <td bgcolor="+color+">"+currentValue+"&#x2020;</td>");
                }else{
                    writer.println("                <td bgcolor="+color+">"+currentValue+"</td>");
                }
                numWritten++;
                break;
            }
            if(colorPicked == false){
                
                String value = firstChildNode.getFirstChild().getNodeValue();
                
                if(Normalizer.compare(currentValue,value,0)==0){
                    String nodeName = firstChildNode.getAttributes().getNamedItem("platform").getNodeValue();
                    isEqual = true;
                    color = (String)colorHash.get(nodeName);
                    colorPicked = true;
                }else{
                    //System.out.println("Value1: "+value1+ "  Value2: "+ value2);
                    if(Normalizer.compare(currentValue,value,Normalizer.COMPARE_IGNORE_CASE)==0){
                        caseDiff=true;
                        color = (String)colorHash.get(firstChildNode.getAttributes().getNamedItem("platform").getNodeValue());
                        colorPicked = true;
                    }
                }
                
            }
        }while(((firstChildNode=firstChildNode.getNextSibling())!=null));
            
    }
    private String mapToAbbr(String source){
        if(source.equals("icu:ruleBasedNumberFormat")){
            return "icu:rbnf";
        }
        if(source.equals("icu:ruleBasedNumberFormats")){
            return "icu:rbnfs";
        }
        if(source.equals("exemplarCharacters")){
            return "exemplarC";
        }
        if(source.equals("localizedPatternChars")){
            return "lpc";
        }
        return source;
    }
    private int serialNumber =0;
    private void comparePrintElementData(Node node, PrintWriter writer){
        
        Node firstChildElement = null;
        NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            firstChildElement =list.item(i);
            if(firstChildElement.getNodeType()==Node.ELEMENT_NODE && firstChildElement.getNodeName().equals("element")){
                break;
            }
        }
        Node s1   = firstChildElement;
        
        // print the index nodeName and parent node
        NamedNodeMap attributes = node.getAttributes();
        String nodeName="&nbsp;", parentNode="&nbsp;", index="&nbsp;";
        if(attributes!=null){
            nodeName   = attributes.getNamedItem("nodeName").getNodeValue();
            parentNode = attributes.getNamedItem("parentNode").getNodeValue();
            index      = attributes.getNamedItem("index").getNodeValue();
            if(index.equals("")){
                index="&nbsp;";
            }
        }

        writer.println("            <tr>");
        writer.println("                <td><a NAME=\""+serialNumber+"\" href=\"#"+serialNumber+"\">"+serialNumber+"</a></td>");
        writer.println("                <td>"+mapToAbbr(parentNode)+"</td>");
        writer.println("                <td>"+mapToAbbr(nodeName)+"</td>");
        writer.println("                <td>"+index+"</td>");
        serialNumber++;
        do{
            printValue(firstChildElement, s1, writer);
            
        }while((s1=s1.getNextSibling())!=null);
        int numExisting = optionTable.size()-doesNotExist.size()+1;
        for(;numWritten<numExisting;numWritten++){
            writer.println("                <td>&nbsp;</td>");
        }
        numWritten = 0;     
        writer.println("            </tr>");
    }
    private void   printHTML(PrintWriter writer){
        NodeList list= resultDocument.getElementsByTagName("elements");
        String displayLang = locale.getDisplayLanguage();
        String dispCountry = locale.getDisplayCountry();
        String dispVariant = locale.getDisplayVariant();
        String displayName = localeStr+" ("+displayLang+"_"+dispCountry;
        if(dispVariant.length()>0){
            displayName += "_"+dispVariant+") ";
        }else{
            displayName += ") ";
        }
        
        writer.println("<html>\n"+
                           "    <head>\n"+
                           "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"+
                           "        <title>"+localeStr+"</title>\n"+
                           "    </head>\n"+
                           "    <style>\n"+
                           "         <!--\n" +
                           "         table        { border-spacing: 0; border-collapse: collapse; width: 100%; \n" +
                           "                        border: 1px solid black }\n" +
                           "         td, th       { width: 10%; border-spacing: 0; border-collapse: collapse; color: black; \n" +
                           "                        vertical-align: top; border: 1px solid black }\n" +
                           "         -->\n" +
                           "     </style>"+
                           "     <body bgcolor=\"#FFFFFF\">\n"+
                           "        <p><b>"+displayName+
                                    "<a href=\"http://oss.software.ibm.com/cgi-bin/icu/lx/en/?_="+localeStr+"\">Demo</a>, "+
                                    "<a href=\"http://oss.software.ibm.com/cvs/icu/~checkout~/locale/all_diff_xml/comparison_charts.html\">Cover Page</a>, "+
                                    "<a href=\"http://oss.software.ibm.com/cvs/icu/~checkout~/locale/all_diff_xml/index.html\">Index</a>"+
                                    "</b></p>\n"+
                           "        <table>\n");
        
         
        Node firstNode = resultDocument.getElementsByTagName("elements").item(1);// we are passig item(1) since item(0) has no children
        printTableHeader(writer, firstNode);
        
        //TODO comparison and tree walking code goes here!
        Node firstElementsNode = list.item(1);
        do{
            comparePrintElementData(firstElementsNode, writer);
            
        }while((firstElementsNode=firstElementsNode.getNextSibling()) != null);
        
        writer.println( "        </table>\n");

        // writer.println("&#x2020; Indicates a case difference<br>");
        // writer.println("= Indicates the data is same as the data in ICU<br>");
        writer.println("        <p>Created on: " + cal.getTime() +"</p>\n"+
                            "    </body>\n"+
                            "</html>");
        writer.flush();

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
         Document doc = null;
         String temp = fileName;    
         File file = new File(fileName);
         if(file.exists()){
             System.out.println("INFO: Parsing file: "+ fileName);
             doc = parse(fileName);
             // OK we have a posix style locale data
             // merge all data in inheritence tree into goldDoc
             int index = -1;
             while((index = temp.indexOf("_")) >0 && index > fileName.lastIndexOf("\\")){
                 temp = temp.substring(0,index)+".xml";
                 file = new File(temp);
                 if(file.exists()){
                     System.out.println("INFO: Parsing file: "+ temp);
                     Document parentDoc = parse(temp);
                     if(parentDoc!=null && doc!=null){
                        mergeElements(doc.getDocumentElement(),parentDoc.getDocumentElement());
                     }
                 }
             }
             String rootFileName="" ;
             if(fileName.lastIndexOf(File.separatorChar)!=-1){
                 rootFileName = fileName.substring(0,fileName.lastIndexOf(File.separatorChar)+1);
             }
             rootFileName = rootFileName+"root.xml"; 
             file = new File(rootFileName);
             if(file.exists()){ 
                 System.out.println("INFO: Parsing file: "+ rootFileName); 
                 Document rootDoc = parse(rootFileName);
                 if(rootDoc != null && doc!=null){
                     mergeElements(doc.getDocumentElement(),rootDoc.getDocumentElement());
                    /*
                    try{
                        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destFolder+File.separator+locale+"_debug.xml"),encoding);
                        print(new PrintWriter(writer),doc);
                    }catch( Exception e){
                        //throw the exception away .. this is for debugging
                    }
                    */
                 } 
             }   
         }
         return doc;
     }

     public boolean writeToResultDoc(String fileName, String key)
     {
        
 
         // parse the test doc only if gold doc was parsed OK  
         Document testDoc = getFullyResolvedLocale(key,fileName);
         if (null == testDoc)
         {
             doesNotExist.add(key);
             return false;
         }
         return extractMergeData(testDoc,key);

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
     private int numIndent = 0;
     private void indent(PrintWriter out){
         numIndent++;
         out.print("\n");
         for(int i=0; i < numIndent; i++){
             out.print("    ");
         }
     }
     private void outdent(PrintWriter out){
         for(int i=0; i < numIndent; i++){
             out.print("    ");
         }
         numIndent--;
     }
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
                 indent(out);
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
                 outdent(out);
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
    


     private static Node elementNode=null;
     private static Node elementsNode=null;
    private void addNodeValue(String nodeName, String value, 
                              Node resultParentNode){
        // clone the element node                          
        Node resultNode = elementNode.cloneNode(true);
        NamedNodeMap attrib= resultNode.getAttributes();
        attrib.getNamedItem("platform").setNodeValue(nodeName);
        resultNode.getFirstChild().setNodeValue(value);
        resultParentNode.appendChild(resultNode);
    }
    
    
    private void addAttributes(Node element, NamedNodeMap attributes){
        Node attributeElement = null;
        NodeList childNodes = elementNode.getChildNodes();
        for(int i=0; i<childNodes.getLength();i++){
            attributeElement = childNodes.item(i);
            if(attributeElement.getNodeName().equals("attribute")){
                break;
            }
        }
        //remove the attribute element 
        NodeList children = element.getChildNodes();
        for(int i=0; i<children.getLength(); i++){
            Node item = children.item(i);
            if(item.getNodeName().equals("attribute")){
                element.removeChild(item);
            }
        }
        for(int i=0; i < attributes.getLength(); i++){
            Node valuesToSet = attributes.item(i);
            String nameToSet = valuesToSet.getNodeName();
            if(nameToSet.equals("type")){
                continue;
            }
            Node clonedNode = attributeElement.cloneNode(true);
            NamedNodeMap map = clonedNode.getAttributes();
            Node name = map.getNamedItem("name");
            name.setNodeValue(nameToSet);
            clonedNode.getFirstChild().setNodeValue(valuesToSet.getNodeValue());
            element.appendChild(clonedNode);
        }
        
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
            //String dNodeVal = dest.getFirstChild().getNodeValue();
            //String sNodeVal = childOfSource.getNodeValue();
            if (childOfSource.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
           
            boolean didMerge = false;
            for (childOfDest = dest.getFirstChild(); childOfDest != null; childOfDest = childOfDest.getNextSibling()) {
                //String childNodeName = childOfDest.getNodeName();
                //String childNodeVal = childOfDest.getNodeValue();
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
        //TODO: just return for now .. this is useful only 
        //when comparing data from toronto
        /*
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
        */
        return true;
    }
    private String trim(String source){
        char[] src = source.toCharArray();
        char[] dest = new char[src.length];

        int start=0;
        while(start<(src.length) && (UCharacter.isWhitespace(src[start]))){start++;}
        int stop=src.length-1;
        while(stop>0 && (UCharacter.isWhitespace(src[stop])||(src[stop]==0xA0))){stop--;}
        if(stop!=-1 && start!=src.length){
            System.arraycopy(src,start,dest,0,(stop-start)+1);
            return new String(dest,0,(stop-start)+1);
        }else{
            return new String();
        }
      
    }
    private void addElement(String childNode, String parentNode, String id, String index, 
                            String nodeValue, String nodeName){

        if(elementNode == null){
            elementNode = resultDocument.getElementsByTagName("element").item(0);
        }
        if(elementsNode == null){
            elementsNode = resultDocument.getElementsByTagName("elements").item(0);
        }  
        Node resultParentNode = null;
            
        //get "elements" node if it already exists with the given id
        Node mainElements = resultDocument.getElementById(id);
            
        //did not find the elements node
        if(mainElements == null){

            NodeList list = elementsNode.getChildNodes();
            for(int i=0;i<list.getLength(); i++){
                Node item = list.item(i);
                if(item.getNodeName().equals("element")){
                    elementsNode.removeChild(item);
                }
            }
            resultParentNode = elementsNode.cloneNode(true);
            NamedNodeMap attribMap=resultParentNode.getAttributes();
            Node parent_node = attribMap.getNamedItem("parentNode");
            Node node_name   = attribMap.getNamedItem("nodeName");
            Node id_attrib   = attribMap.getNamedItem("id");
            Node indexNode   = attribMap.getNamedItem("index");
            parent_node.setNodeValue(parentNode);
            node_name.setNodeValue(childNode);
            id_attrib.setNodeValue(id);
            indexNode.setNodeValue((index==null) ? "&nbsp;" : index);
            //append the newly created node to the document
            elementsNode.getParentNode().appendChild(resultParentNode);
            addNodeValue(nodeName, nodeValue, resultParentNode);
        }else{
            resultParentNode = mainElements;
            addNodeValue(nodeName, nodeValue, resultParentNode);
        }
    }
    private boolean childrenAreElements(Node node){
        NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            if(list.item(i).getNodeType()==Node.ELEMENT_NODE){
                return true;
            }
        }
        return false;  
    }
    private boolean extractMergeData(Node node,String key){
        Node childOfSource;

        for(childOfSource = node.getFirstChild(); childOfSource != null; childOfSource = childOfSource.getNextSibling()) {
             if (childOfSource.getNodeType() != Node.ELEMENT_NODE) {
                 continue;
             }
             String childOfSourceName = childOfSource.getNodeName();
             //Ignore collation and special tags
             if(childOfSourceName.equals("collations")|| childOfSource.equals("special")){
                 continue;
             }
             if(childrenAreElements(childOfSource)==false){
                 NamedNodeMap attr = childOfSource.getAttributes();
                 Node typeNode = attr.getNamedItem("type");
                 String index="";
                 if(typeNode!=null){
                     String temp =typeNode.getNodeValue();
                     if(!temp.equals("standard")){
                         index = temp;
                     }
                     
                 }
                 String nodeValue = "";
                 Node valueNode = childOfSource.getFirstChild();
                 if(valueNode != null){
                    String temp = trim(valueNode.getNodeValue());
                    if(!temp.equals("standard")){
                        nodeValue = temp;
                    }
                 }
                 Node parentNode = childOfSource.getParentNode();
                 String parentNodeName = trim(parentNode.getNodeName());
                 String childNodeName  = trim(childOfSource.getNodeName());
                 Node grandParentNode = childOfSource.getParentNode().getParentNode();
                 String grandParentNodeName = grandParentNode.getNodeName();
                 NamedNodeMap parentAttrib = parentNode.getAttributes();
                 String type ="";
                 if(parentAttrib != null){
                     Node mytypeNode = parentAttrib.getNamedItem("type");
                     if(mytypeNode!=null){
                         String mytype = mytypeNode.getNodeValue();
                         if(!mytype.equals("standard")){
                             type = mytype;
                         }
                     }
                 }
                 if(childNodeName.equals("pattern") ||grandParentNodeName.equals("zone")){
                     NamedNodeMap at = grandParentNode.getAttributes();
                     Node mytypeNode = at.getNamedItem("type");
                     if(mytypeNode!=null){
                         String mytype = mytypeNode.getNodeValue();
                         if(!mytype.equals("standard")){
                             if(type.equals("")){
                                 type = mytype;
                             }else{
                                 type = type+"_"+mytype;
                             }
                             
                         }
                     }
                 }
                 if(grandParentNodeName.equals("special") || parentNodeName.equals("special") || childNodeName.equals("special")
                    || grandParentNodeName.indexOf(":")>0){
                     continue;
                 }
                 if(!nodeValue.equals("") && 
                    !childOfSource.getNodeName().equals("version")){
                     
                     String id = grandParentNodeName+"_"+parentNodeName+"_"+childNodeName+"_"+type+"_"+index;
                     if(!index.equals("")){
                         if(!index.equals(nodeValue) && !index.equals("Fallback")){
                            addElement(childNodeName, parentNodeName, id, index, nodeValue, key);
                         } 
                     }else{
                         if(!type.equals(nodeValue) && !type.equals("Fallback")){
                            addElement(childNodeName, parentNodeName, id, type, nodeValue, key);
                         }
                     }
                 }
                 if(attr.getLength()>0 && typeNode==null){
                     // add an element for each attribute different for each attribute
                     for(int i=0; i<attr.getLength(); i++){
                         Node item = attr.item(i);
                         String attrName =item.getNodeName();
                         if(attrName.equals("type")){
                             continue;
                         }
                         String id =grandParentNodeName+"_"+parentNodeName+"_"+childNodeName+"_"+type+"_"+attrName;
                         if(!index.equals("")){
                             addElement(childNodeName, parentNodeName, id, index, item.getNodeValue(), key);
                         }else{
                             addElement(childNodeName, parentNodeName, id, type, item.getNodeValue(), key);
                         }
                     }
                 }
             }else{
                 //the element has more children .. recurse to pick them all
                 extractMergeData(childOfSource,key);
             }
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
