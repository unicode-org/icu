/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
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
import java.util.Calendar;
import java.util.Locale;

// DOM imports
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
    /*
     * This application will compare different locale data xml files
     * conforming to localeElements.dtd and produces an xml file file
     * in the format
     */ 
    private static final short OPT_SUN_JDK = 0x001; /*2exp0*/
    private static final short OPT_IBM_JDK = 0x002; /*2exp1*/
    private static final short OPT_WINDOWS = 0x004; /*2exp2*/
    private static final short OPT_HP_UX   = 0x008; /*2exp3*/
    private static final short OPT_SOLARIS = 0x010; /*2exp4*/
    private static final short OPT_IBM_TOR = 0x020; /*2exp5*/
    private static final short OPT_APPLE   = 0x040; /*2exp6*/
    private static final short OPT_ICU     = 0x080; /*2exp7*/
    private static final short OPT_OTHER   = 0x100; /*2exp8*/
    private static final short OPT_SOURCE  = 0x200; /*2exp9*/
    private static final short OPT_DEST    = 0x400; /*2exp10*/
    private static final short OPT_LINUX   = 0x800; /*2exp11*/
    private static final short OPT_AIX     = 0x1000; /*2exp12*/
    private static final short OPT_COMMON  = 0x2000; /*2exp13*/
    private static final short OPT_UNKNOWN = 0x4000; /*2exp15*/
    
    private static final String COMMON      = "common";     
    private static final String ICU         = "icu";    
    private static final String IBM_TOR     = "ibm_tor";
    private static final String WINDOWS     = "windows";    
    private static final String SUNJDK      = "sun_jdk";    
    private static final String IBMJDK      = "ibm_jdk";    
    private static final String HPUX        = "hpux";      
    private static final String APPLE       = "apple";      
    private static final String SOLARIS     = "solaris";    
    private static final String OPEN_OFFICE = "open_office";
    private static final String AIX         = "aix";        
    private static final String LINUX       = "linux";    
    
    private static final String[] PLATFORM_PRINT_ORDER ={
        COMMON,     
        ICU,
        WINDOWS,
        SUNJDK,     
        IBMJDK,         
        IBM_TOR,          
        APPLE,      
        SOLARIS,    
        OPEN_OFFICE,
        AIX,        
        LINUX, 
        HPUX,        
    };
    
    private static final String USER_OPTIONS[] = {
        "-"+COMMON,
        "-"+ICU,
        "-"+IBM_TOR,
        "-"+WINDOWS,
        "-"+SUNJDK,
        "-"+IBMJDK,
        "-"+HPUX,
        "-"+APPLE,
        "-"+SOLARIS,
        "-"+OPEN_OFFICE,
        "-"+AIX,
        "-"+LINUX,        
        "-s",              
        "-d",              
    };
   

    
    public static void main(String[] args){
        XMLComparator comparator = new XMLComparator();
        comparator.processArgs(args);    
        
    }
                        
    Hashtable optionTable = new Hashtable();
    private String sourceFolder = ".";
    private String destFolder = ".";  
    private String localeStr;    
    private Calendar cal = Calendar.getInstance();
    private Hashtable colorHash = new Hashtable();
    private String goldFileName; 
    private String goldKey;
    private int numPlatforms = 0;
    private int serialNumber =0;
    private TreeMap compareMap = new TreeMap();
    private Hashtable doesNotExist = new Hashtable();
    private Hashtable requested = new Hashtable();
    private String  encoding   = "UTF-8"; // default encoding
    
    private class CompareElement{
        String node;
        String index;
        String parentNode;
        Hashtable platformData = new Hashtable();
    }
    
    XMLComparator(){
        //initialize the color hash
        colorHash.put( COMMON,      "#AD989D");
        colorHash.put( ICU,         "#CCFF00");
        colorHash.put( IBM_TOR,     "#FF7777");
        colorHash.put( WINDOWS,     "#98FB98");
        colorHash.put( SUNJDK,      "#FF6633");
        colorHash.put( IBMJDK,      "#CCFFFF");
        colorHash.put( HPUX,        "#FFE4B5");
        colorHash.put( APPLE,       "#FFBBBB");
        colorHash.put( SOLARIS,     "#CC9966");
        colorHash.put( OPEN_OFFICE, "#FFFF33");
        colorHash.put( AIX,         "#EB97FE");
        colorHash.put( LINUX,       "#1191F1");
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
        Enumeration en = optionTable.keys();
        
        try{
                        
            localeStr  = goldFileName.substring(goldFileName.lastIndexOf(File.separatorChar)+1,goldFileName.indexOf('.'));
       
            String fileName = destFolder+File.separator+localeStr+".html";
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(fileName),encoding);
            System.out.println("INFO: Creating file named: " + fileName);
            
            addToCompareMap(goldFileName, goldKey);
            for(;en.hasMoreElements();){
                String key = (String)en.nextElement();
                String compFile = (String) optionTable.get(key);
                addToCompareMap(compFile,key);
                
            }
            PrintWriter writer = new PrintWriter(os);
            printHTML(writer, localeStr);
        }catch(Exception e){
            e.printStackTrace();
        }
            
    }
    
    private void printUsage() {
        System.err.println("Usage: XMLComparator [<option>:<gold>] filename1 [option] filename2 ... \n"+
                           " XMLComparator [-common:<gold>] filename [-icu] filename" +
                           " [-ibm_jdk] filename [-windows] filename" +
                           " [-hpux]  filename [-solaris] filename"  +
                           " [-ibm_tor] filename [-apple] filename"   +
                           " [-sun_jdk]  filename [-open_office] filename" +
                           " [-aix] filename [-linux] filename" 
                           );
    }
   
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
                   
                    if (USER_OPTIONS[i].equals(option)) {
                        result |= (short)(1 << i);
                        optionRecognized = true;
                        if(USER_OPTIONS[i].equals("-s")){
                            sourceFolder = options[++j];
                        }else if(USER_OPTIONS[i].equals("-d")){
                            destFolder = options[++j];
                        }else{
                            if(!isGold){
                                optionTable.put(option.substring(1,option.length()),options[++j]);
                            }else{
                                goldFileName = options[++j];
                                goldKey      = option.substring(1,option.length());
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
  
    private void printTableHeader(PrintWriter writer){

        writer.print(  "            <tr>\n" +
                       "                <th>N.</th>\n"+
                       "                <th>ParentNode</th>\n"+
                       "                <th>Name</th>\n"+
                       "                <th>ID</th>\n");
        
        for(int i=0; i< PLATFORM_PRINT_ORDER.length && PLATFORM_PRINT_ORDER[i]!=null; i++ ){
            String name = PLATFORM_PRINT_ORDER[i];
            String folder;
            
            Object obj = requested.get(name);
            if(obj!=null && doesNotExist.get(name)==null ){
                folder = name+"/xml/";
                writer.print("                <th bgcolor=\""+
                               (String)colorHash.get(name)+ "\">" +
                               name.toUpperCase()+
                               " (<a href=\"../"+folder+localeStr+".xml\">xml</a>)"+
                               "</th>\n");
                numPlatforms++;
               
            }
        }        
        writer.print("            </tr>\n");
    }

    private void printValue(CompareElement element, PrintWriter writer){
        
        
        writer.print("            <tr>\n");
        writer.print("                <td><a NAME=\""+serialNumber+"\" href=\"#"+serialNumber+"\">"+serialNumber+"</a></td>\n");
        writer.print("                <td>"+mapToAbbr(element.parentNode)+"</td>\n");
        writer.print("                <td>"+mapToAbbr(element.node)+"</td>\n");
        writer.print("                <td>"+element.index+"</td>\n");
        serialNumber++;
        
        for(int i=0; i<PLATFORM_PRINT_ORDER.length; i++){
            String value = (String)element.platformData.get(PLATFORM_PRINT_ORDER[i]);
            String color = (String)colorHash.get(PLATFORM_PRINT_ORDER[i]);
            boolean caseDiff = false;
            boolean isEqual = false;
            // the locale exists for the given platform but there is no data
            // so just write non breaking space and continue
            // else the object contains value to be written .. so write it
            if(value == null ){
                if(requested.get(PLATFORM_PRINT_ORDER[i])!=null && doesNotExist.get(PLATFORM_PRINT_ORDER[i])==null){
                    writer.print("                <td>&nbsp;</td>\n");
                }
            }else{
                //pick the correct color
                for(int j=0; j<i; j++){
                    String compareTo = (String)element.platformData.get(PLATFORM_PRINT_ORDER[j]);
                    if(compareTo==null){
                        continue;
                    }else if(Normalizer.compare(compareTo,value,0)==0){
                        color = (String)colorHash.get(PLATFORM_PRINT_ORDER[j]);
                        isEqual = true;
                        break;
                    }else if(Normalizer.compare(compareTo,value,Normalizer.COMPARE_IGNORE_CASE)==0){
                        caseDiff=true;
                        color = (String)colorHash.get(PLATFORM_PRINT_ORDER[j]);
                        break;
                    }
                }
                if(isEqual){
                    value = "=";
                }
                if(caseDiff==true){
                    writer.print("                <td bgcolor="+color+">"+value+"&#x2020;</td>\n");
                }else{
                    writer.print("                <td bgcolor="+color+">"+value+"</td>\n");
                }
            }
        }
        writer.print("            </tr>\n");
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

    private void   printHTML(PrintWriter writer, String localeStr){
        System.out.println("INFO: Creating the comparison chart ");
        Locale locale = getLocaleFromName(localeStr);
        String displayLang = locale.getDisplayLanguage();
        String dispCountry = locale.getDisplayCountry();
        String dispVariant = locale.getDisplayVariant();
        String displayName = localeStr+" ("+displayLang+"_"+dispCountry;
        if(dispVariant.length()>0){
            displayName += "_"+dispVariant+") ";
        }else{
            displayName += ") ";
        }
        
        writer.print("<html>\n"+
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
                                    "<a href=\"../comparison_charts.html\">Cover Page</a>, "+
                                    "<a href=\"./index.html\">Index</a>, "+
                                    "<a href=\"../collation_diff/"+localeStr+"_collation.html\">Collation</a> "+
                                    "</b></p>\n"+
                           "        <table>\n");
        

        printTableHeader(writer);
        
        // walk down the compare map and print the data
        Iterator iter = compareMap.keySet().iterator();
        while(iter.hasNext()){
            Object obj = iter.next();
            CompareElement element;
            if(obj != null){
                Object value = compareMap.get(obj);
                if(value instanceof CompareElement){
                    element = (CompareElement)value;
                }else{
                    throw new RuntimeException("The object stored in the compare map is not an instance of CompareElement");
                }
                printValue(element,writer);
            }else{
                throw new RuntimeException("No objects stored in the compare map!");
            }

        }
        writer.print( "        </table>\n");

        writer.print( "        <p>Created on: " + cal.getTime() +"</p>\n"+
                      "    </body>\n"+
                      "</html>\n");
        writer.flush();

        writer.flush();
    }
    
     private Document getFullyResolvedLocale(String localeName,String fileName){
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
                     * debugging code
                     
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

     private boolean addToCompareMap(String fileName, String key)
     {
         // parse the test doc only if gold doc was parsed OK  
         Document testDoc = getFullyResolvedLocale(key,fileName);
         requested.put(key,"");
         if (null == testDoc)
         {
             doesNotExist.put(key, "");
             return false;
         }
         return extractMergeData(testDoc,key);
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
        childOfSource = source.getFirstChild();
        childOfDest = dest.getFirstChild();
        
        if(childOfSource != null){
            for (childOfSource = source.getFirstChild(); childOfSource != null; childOfSource = childOfSource.getNextSibling()) {
                //String dNodeVal = dest.getFirstChild().getNodeValue();
                //String sNodeVal = childOfSource.getNodeValue();
                if (childOfSource.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
               
                boolean didMerge = false;
                for (childOfDest = dest.getFirstChild(); childOfDest != null; childOfDest = childOfDest.getNextSibling()) {
                    String childNodeName = childOfDest.getNodeName();
                    String childOfSourceNN = childOfSource.getNodeName();
                    if (childOfDest.getNodeType() == Node.ELEMENT_NODE  &&
                           childOfDest.getNodeName().equals(childOfSource.getNodeName())) {
                        // The destination document already has an element of this type at this level.
                        //   Recurse to pick up any extra children that the source node may have.
                        
                        // is the type attribute same on 
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
        }else{
            // now we have an element node with data .. get the attributes of source and dest and check
            // and then get the node value and merge if they are not equal
            if(childOfSource != null){
            
                NamedNodeMap childOfSourceAttr = childOfSource.getAttributes();
                Node childOfSourceTypeNode = childOfSourceAttr.getNamedItem("type");
                String type = childOfSourceTypeNode.getNodeValue();
                
                Node node = childOfDest.getParentNode();
                boolean foundNodeInDest = false;
                for (node = source.getFirstChild(); node != null; node = node.getNextSibling()) {
                    //String dNodeVal = dest.getFirstChild().getNodeValue();
                    //String sNodeVal = childOfSource.getNodeValue();
                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                }
            }
                

        }
    }
    
    private boolean comparePatterns(String pat1,String pat2){
        //TODO: just return for now .. this is useful only 
        //when comparing data from toronto
        /*
        double args1  = 10000000000.00;
        double args2  = -10000000000.00;
    
        DecimalFormat fmt = new DecimalFormat(); 
        
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
                            String platformValue, String platformName){
                                
        Object obj = compareMap.get(id);
        CompareElement element;
        if(obj==null){
            element = new CompareElement();
            //initialize the values
            element.index = index;
            element.parentNode = parentNode;
            element.node = childNode;
            // add the element to the compare map
            compareMap.put(id, element);
        }else{
            if(obj instanceof CompareElement){
                element = (CompareElement) obj;
            }else{
                throw new RuntimeException("The object stored in the compareMap is not a CompareElement object!");
            }
        }
        
        if((!element.index.equals(index)) ||
            (!element.node.equals(childNode)) ||
            (!element.parentNode.equals(parentNode))){
              throw new RuntimeException("The retrieved object is not the same as the one trying to be saved");  
        }
        
        element.platformData.put(platformName, platformValue);
        
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
                                 type = type+"\u200b_"+mytype;
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
             
                     if(grandParentNodeName.equals("zone")){
                        parentNodeName = grandParentNodeName+"\u200b_"+parentNodeName;    
                     } 
                     String id = parentNodeName+"_"+childNodeName+"_"+type+"_"+index+"_"+grandParentNodeName;
                           
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
                         if(grandParentNodeName.equals("zone")){
                            parentNodeName = grandParentNodeName+"\u200b_"+parentNodeName;    
                         } 
                         String id = parentNodeName+"_"+childNodeName+"_"+type+"_"+attrName+"_"+grandParentNodeName;
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
    private Document parse(String filename)
    {
        // Force filerefs to be URI's if needed: note this is independent of any other files
        String docURI = filenameToURL(filename);
        return parse(new InputSource(docURI),filename);
    }
    
    private Document parse(InputSource docSrc, String filename){
        
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
