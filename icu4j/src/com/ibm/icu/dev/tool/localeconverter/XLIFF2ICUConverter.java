/*
******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/localeconverter/XLIFF2ICUConverter.java,v $ 
* $Date: 2003/05/19 
* $Revision: 1.1 $
*
******************************************************************************
*/

package com.ibm.icu.dev.tool.localeconverter;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.dev.tool.xmlcomparator.XMLComparator;

// DOM imports
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

// SAX2 imports
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// Needed JAXP classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.*;
import java.util.*;

public class XLIFF2ICUConverter {
    // Command-line options set these:
    protected String    sourceDir;
    protected String    fileName;
    protected String    encoding;
    protected String    destDir;
    protected String    xmlfileName;
    protected Document  doc;
    
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int SOURCEDIR = 2;
    private static final int DESTDIR = 3;
    private static final int ENCODING = 4;
    private static final int FILENAME = 5;
    
    private static final UOption[] options = new UOption[] {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR(),
        UOption.DESTDIR(),
        UOption.ENCODING(),
        UOption.DEF( "fileName", 'f', UOption.REQUIRES_ARG),
    };
    
    private static int tabCount = 0;
    private static PrintWriter[] writer2;
    private static OutputStreamWriter[] os;
    private static ArrayList bundleLang;
    private static ArrayList bundleName;
    private static ArrayList bundleList;
    private static int bundleLen = 0;
    
    private static final String RESTYPE = "restype";
    private static final String RESNAME = "resname";
    
    private static final String BODY = "body";
    private static final String GROUPS = "group";
    private static final String FILES = "file";
    private static final String TRANSUNIT = "trans-unit";
    private static final String BINUNIT = "bin-unit";
    private static final String TS = "ts";
    private static final String ORIGINAL = "original";
    private static final String SOURCELANGUAGE = "source-language";
    private static final String TARGET = "target";
    private static final String SOURCE = "source";
    
    private static final String INTVECTOR = "intvector";
    private static final String ARRAYS = "array";
    private static final String STRINGS = "string";
    private static final String BIN = "bin";
    private static final String INTS = "int";

    private static final String HREF = "href";
    private static final String EXTERNALFILE = "external-file";
    private static final String CRC = "crc";
    
    public static void main(String[] args) {
        XLIFF2ICUConverter convert = new XLIFF2ICUConverter();
        convert.processArgs(args);
    }
    
    private void processArgs(String[] args) {
        int remainingArgc = UOption.parseArgs(args, options);
        if(args.length==0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
            usage();
        }
        if(options[SOURCEDIR].doesOccur) {
            sourceDir = options[SOURCEDIR].value;
        }
        if(options[DESTDIR].doesOccur) {
            destDir = options[DESTDIR].value;
        }
        if(options[ENCODING].doesOccur) {
            encoding = options[ENCODING].value;
        }
        if(options[FILENAME].doesOccur){
            fileName = options[FILENAME].value;
        } else {
            System.out.println("\nPlease use \"-f filename\" to specify the file.");
            usage();
            System.exit(0);
        }
        
        xmlfileName = getFullPath(false,fileName);

        bundleName = new ArrayList();
        bundleLang = new ArrayList();
        bundleList = new ArrayList();
        
        createRB(xmlfileName);
        
        bundleLen = bundleList.size();
        
        if(bundleLen == 0) {
            boolean b;
            b = bundleList.add(getFullPath(true, fileName));
            int lastIndex = fileName.lastIndexOf('.', fileName.length());
            b = bundleName.add(fileName.substring(0, lastIndex));
            bundleLen++;
        }
        
        os = new OutputStreamWriter[bundleLen];
        writer2 = new PrintWriter[bundleLen];
        try {
            for(int s = 0; s < bundleLen; s++) {
                os[s] = new OutputStreamWriter(new FileOutputStream((String)bundleList.get(s)), "UTF-8");
                writer2[s] =  new PrintWriter((OutputStreamWriter)os[s]);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        doc = parse(xmlfileName);
        
        for(int s = 0; s < bundleLen; s++){
            try{
                writer2[s].close();
                os[s].close();
            } catch(Exception e) {
                e.printStackTrace();      
            }
        }
    }
    
    private void usage() {
        System.out.println("\nUsage: XLIFF2ICUConverter [OPTIONS] [FILES]\n\n"+
            "This program is used to convert XML files to TXT files.\n"+
            "Please refer to the following options. Options are not \n"+
            "case sensitive.\n"+
            "Options:\n"+
            "-s or --sourcedir    source directory for files followed by path, default is current directory.\n" +
            "-d or --destdir      destination directory, followed by the path, default is current directory.\n" +
            "-f                   file name e.g. myResources.xml" +
            "-h or -? or --help   this usage text.\n"+
            "example: XLIFF2ICUConverter -s xxx -d yyy -f myResources.xml");
    }
    
    /*true: The file returned is txt*/
    /*false:The file returned is xml*/
    private String getFullPath(boolean fileType, String fName){
        String str;
        int lastIndex = fName.lastIndexOf('.', fName.length());
        if (fileType == true) {
            if (destDir != null && fName != null) {
                str = destDir + "\\" + fName.substring(0, lastIndex) + ".txt";                   
            } else {
                str = System.getProperty("user.dir") + "\\" + fName.substring(0, lastIndex) + ".txt";
            }
        } else {
            if(sourceDir != null && fName != null) {
                str = sourceDir + "\\" + fName.substring(0, lastIndex) + ".xml";
            } else {
                str = System.getProperty("user.dir") + "\\" + fName.substring(0, lastIndex) + ".xml";
            }
        }
        return str;
    }
    
    private static String crc = "";
    private static int transID = -1;
    private static boolean isIntvector = false;
    
    private void writeRB(Node doc) {
        String resType = "string";
        String resName = "";
        String resValue = "";
        switch(doc.getNodeType()){
            case Node.ATTRIBUTE_NODE: {
                break;
            }
            case Node.CDATA_SECTION_NODE: {
                break;
            }
            case Node.COMMENT_NODE: {
            }
            case Node.DOCUMENT_FRAGMENT_NODE: {
                break;
            }
            case Node.DOCUMENT_NODE: {
                Node child = doc.getFirstChild();
                while( child != null) {
                    writeRB(child);
                    child = child.getNextSibling();
                }
                break;
            }
            case Node.DOCUMENT_TYPE_NODE: {
                break;
            }
            
            case Node.ELEMENT_NODE: {
                crc = "";
                NamedNodeMap attributes = doc.getAttributes();
                int attrCount = attributes.getLength();
                
                for (int i = 0; i < attrCount; i++) {
                    Node  attribute = attributes.item(i);
                    if (attribute.getNodeName().equals(RESTYPE)) {
                        resType = attribute.getNodeValue();
                        if (resType.equals(ARRAYS)) {
                            isIntvector = checkIV(doc);
                        }
                    } else if (attribute.getNodeName().equals(RESNAME)) {
                        resName = attribute.getNodeValue();
                    } else if (attribute.getNodeName().equals(HREF)) {
                        resValue = attribute.getNodeValue();
                    } else if (attribute.getNodeName().equals(CRC)) {
                        crc = attribute.getNodeValue();
                    }
                }             
                
                if(doc.getNodeName().equals(GROUPS) && doc.getParentNode().getNodeName().equals(BODY)) {
                    transID = -1;
                    for(int s = 0; s < bundleLen; s++){
                        Calendar c = Calendar.getInstance();
                        Date d = c.getTime();
                        writer2[s].print('\uFEFF');
                        /*Output the information about which tool generates the file, 
                        /*when it file is generated and from which source it is generated*/
                        writer2[s].println("// ***************************************************************************");
                        writer2[s].println("// *");
                        writer2[s].println("// * Tool: com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter.java");
                        writer2[s].println("// * Date & Time: " + c.get(Calendar.YEAR) + "/" + (c.get(Calendar.MONTH)+1) + "/" + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE));
                        writer2[s].println("// * Source File: " + fileName);
                        writer2[s].println("// *");                    
                        writer2[s].println("// ***************************************************************************");
                        writer2[s].println("");
                        writer2[s].println("");                                        
                        writer2[s].println((String)bundleName.get(s) + "{");
                        writer2[s].flush();
                    }
                } else if (doc.getNodeName().equals(GROUPS)){
                    transID = -1;
                    if (!isIntvector) {
                        writeTabs();
                        tabCount++;
                        for(int s = 0; s < bundleLen; s++){
                            writer2[s].println(resName + ":" + resType + "{");
                            writer2[s].flush();
                        }
                    } else {
                        writeTabs();
                        tabCount++;
                        for(int s = 0; s < bundleLen; s++){
                            writer2[s].println(resName + ":" + INTVECTOR + "{");
                            writer2[s].flush();
                        }
                    }
                } else if (doc.getNodeName().equals(TRANSUNIT)||doc.getNodeName().equals(BINUNIT)){
                    writeTabs();
                    if (resType.equals(STRINGS)) {
                        transID = 0;
                    } else{
                        transID = -1;
                    }
                    
                    if (resType.equals(STRINGS)||resType.equals(BIN)) {
                        for(int s = 0; s < bundleLen; s++){
                            writer2[s].print(resName + ":" + resType + "{\"");    
                            writer2[s].flush();
                        }
                    } else if (resType.equals(INTS) && isIntvector){
                    } else {
                        for(int s = 0; s < bundleLen; s++){
                            writer2[s].print(resName + ":" + resType + "{");     
                            writer2[s].flush();
                        }
                    }
                } else if (doc.getNodeName().equals(EXTERNALFILE)) {
                    for(int s = 0; s < bundleLen; s++){
                        writer2[s].print("\"" + resValue + "\"");     
                        writer2[s].flush();
                    }
                }
                
                Node child = doc.getFirstChild();
                if (child != null) {
                    while( child != null) {
                        writeRB(child);
                        child = child.getNextSibling();
                    }
                }
                if(doc.getNodeName().equals(GROUPS)) {
                    tabCount--;
                    writeTabs();
                    for(int s = 0; s < bundleLen; s++){
                        writer2[s].println("}");     
                        writer2[s].flush();
                    }
                    isIntvector = false;
                }else if (doc.getNodeName().equals(TRANSUNIT)||doc.getNodeName().equals(BINUNIT)){
                    if (resType.equals(STRINGS)||resType.equals(BIN)) {
                        for(int s = 0; s < bundleLen; s++){
                           writer2[s].println("\"}");     
                           writer2[s].flush();
                        }
                    } else if (resType.equals(INTS)&&isIntvector){
                        for(int s = 0; s < bundleLen; s++){
                           writer2[s].println(",");   
                           writer2[s].flush();
                        }
                    } else {
                        for(int s = 0; s < bundleLen; s++){
                           writer2[s].println("}");   
                           writer2[s].flush();
                        }
                    }
                }
                break;
            }
            case Node.ENTITY_NODE: {
                break;
            }
            case Node.ENTITY_REFERENCE_NODE: {
                break;
            }
            case Node.NOTATION_NODE: {
                break;
            }
            case Node.PROCESSING_INSTRUCTION_NODE: {
                break;
            }
            case Node.TEXT_NODE: {
                try {   
                    int index = doc.getNodeValue().indexOf("\"");
                    if(index != -1) {
                        resValue = doc.getNodeValue().replaceAll("\"", "\\\\\"");
                    } else {
                        resValue = doc.getNodeValue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                
                if (transID == -1) {
                    //CRC32 check
                    if (!crc.equals("")){
                        if ((int)(resValue.charAt(0))!= 10){
                            if(Integer.parseInt(crc, 10) != CalculateCRC32.computeCRC32(resValue)) {
                                System.out.println("crc error! Please check.");
                                
                                for(int s = 0; s < bundleLen; s++){
                                    try{
                                       writer2[s].close();
                                       os[s].close();
                                    } catch(Exception e) {
                                        e.printStackTrace();      
                                    }
                                    new File((String)bundleList.get(s)).delete();
                                }
                                System.exit(0);
                              } else {
                                  crc = "";
                              }
                        }
                    }
                    for(int s = 0; s < bundleLen; s++){
                        if ((int)(resValue.charAt(0))!= 10){
                            writer2[s].print(resValue);
                            writer2[s].flush();
                        }
                    }
                } else {
                    if ((int)(resValue.charAt(0))!= 10){
                        writer2[transID].print(resValue);
                        writer2[transID].flush();
                        transID++;
                    }
                }
                break;
            }
            default:
                System.err.println("Unrecongized node type");
        }
    }    
    
    //check intvector
    private boolean checkIV(Node doc){
        NodeList nList = doc.getChildNodes();
        int len = nList.getLength();
        boolean flag = true; //true for intvector, false for others
        String resType = "";
        String resName = "";
        
        if(len == 1 && nList.item(0).getNodeType()==Node.TEXT_NODE){
            flag = false;
        } else {
            for (int i = 0; i < len; i++) {
                Node n = nList.item(i);
                if(n.getNodeType() == Node.ELEMENT_NODE) {
                    NamedNodeMap attributes = n.getAttributes();
                    int attrCount = attributes.getLength();
                    for (int j = 0; j < attrCount; j++) {
                        Node  attribute = attributes.item(j);
                        if (attribute.getNodeName().equals(RESTYPE)) {
                            resType = attribute.getNodeValue();
                        }
                        if (attribute.getNodeName().equals(RESNAME)) {
                            resName = attribute.getNodeValue();
                        }
                    }
                    if(resType.equals(INTS)&&resName.equals("")) {
                        flag = flag & true;
                    } else {
                        flag = flag & false;
                    }
                }
            }
        }
        return flag;
    }
    
    private void createRB(String docsrc) {
        boolean tag = false;
        
        String urls = XMLComparator.filenameToURL(docsrc);
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);
        
        // This is used to suppress validation warnings
        ErrorHandler nullHandler = new ErrorHandler() {
            public void warning(SAXParseException e) throws SAXException {/*System.err.println("Warning: " + e.getMessage());*/}
            public void error(SAXParseException e) throws SAXException {System.err.println("Error: " + e.getMessage());}
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        };
        
        try {
            // First, attempt to parse as XML (preferred)...
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            docBuilder.setErrorHandler(nullHandler);
            doc = docBuilder.parse(new InputSource(urls));
            
            //if (ts == null), it indicates that there is no transltion in the xml file. use "original" as the name of the output file.
            //if (ts != null), it indicates that there are translation items in the xml file. use "package_xx_YY" as the name of the output file.
            String pkg = "";
            String f = "";
            String lg = "";
            NodeList nlist1 = doc.getElementsByTagName(FILES);
            for (int i = 0; i < nlist1.getLength(); i++) {
                Node n = nlist1.item(i);
                NamedNodeMap attributes = n.getAttributes();
                int attrCount = attributes.getLength();
                for (int k = 0; k < attrCount; k++) {
                    Node  attribute = attributes.item(k);
                    if (attribute.getNodeName().equals(TS)) {
                        pkg = attribute.getNodeValue();
                    }
                    if (attribute.getNodeName().equals(ORIGINAL)) {
                        f = attribute.getNodeValue();
                    }
                    if (attribute.getNodeName().equals(SOURCELANGUAGE)) {
                        lg = attribute.getNodeValue();
                    }
                }
            }
            if(pkg.equals("")) {
                boolean b;
                b = bundleLang.add(lg);
                int lastIndex = f.lastIndexOf('.', f.length());
                b = bundleName.add(f.substring(0, lastIndex));
                b = bundleList.add(getFullPath(true,f));
            } else {
                NodeList nlist = doc.getElementsByTagName(TRANSUNIT);
                for(int i = 0; i < nlist.getLength(); i++) {
                    String resType = "string";
                    Node n = nlist.item(i);
                    
                    NamedNodeMap attributes = n.getAttributes();
                    int attrCount = attributes.getLength();
                    for (int k = 0; k < attrCount; k++) {
                        Node  attribute = attributes.item(k);
                        if (attribute.getNodeName().equals(RESTYPE)) {
                            resType = attribute.getNodeValue();
                        }
                    }
                    
                    if (resType.equals(STRINGS)) {
                        NodeList nlist2 = n.getChildNodes();
                        for(int j = 0; j < nlist2.getLength(); j++) {
                            Node n2 = nlist2.item(j);
                            if(n2.getNodeName().equals(TARGET)||n2.getNodeName().equals(SOURCE)) {
                                NamedNodeMap att =  n2.getAttributes();
                                String lang = att.item(0).getNodeValue();
                                String name = pkg + "_" + lang;
                                boolean b;
                                b = bundleLang.add(lang);
                                b = bundleName.add(name);
                                b = bundleList.add(getFullPath(true, name + ".txt"));
                                tag = true;
                            }
                        }
                    }
                    if(tag){
                        break;
                    }
                }                    
            }
        }
        catch (Throwable se) {
            System.out.println("ERROR :" + se.toString());
        }        
    }
    
    private Document parse(String docsrc) {
        String urls = XMLComparator.filenameToURL(docsrc);
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);
        
        ErrorHandler nullHandler = new ErrorHandler() {
            public void warning(SAXParseException e) throws SAXException {/*System.err.println("Warning: " + e.getMessage());*/}
            public void error(SAXParseException e) throws SAXException {System.err.println("Error: " + e.getMessage());}
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        };
        
        try {
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            docBuilder.setErrorHandler(nullHandler);
            doc = docBuilder.parse(new InputSource(urls));
            writeRB(doc);
        }
        catch (Throwable se) {
            System.out.println("ERROR :" + se.toString());
        }        
        return doc;
    }
    
    //Utilities
    private static void writeTabs(){
        int i=0;
        for(;i<=tabCount;i++){
            for(int s = 0; s < bundleLen; s++){
                writer2[s].print("    ");
                writer2[s].flush();
            }
        }
    }
}