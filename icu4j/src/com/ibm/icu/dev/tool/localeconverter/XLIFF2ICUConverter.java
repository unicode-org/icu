/*
******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/localeconverter/XLIFF2ICUConverter.java,v $ 
* $Date: 2003/05/19 
* $Revision: 1.3 $
*
******************************************************************************
*/

package com.ibm.icu.dev.tool.localeconverter;

import com.ibm.icu.dev.tool.UOption;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.*;
import java.util.*;

public class XLIFF2ICUConverter {
    protected String    sourceDir = null;
    protected String    fileName = null;
    protected String    packageName = null;
    protected String    destDir = null;
    protected String    xmlfileName = null;
    protected Document  doc;
    
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int SOURCEDIR = 2;
    private static final int DESTDIR = 3;
    private static final int PACKAGE_NAME = 4;
    private static final int FILENAME = 5;
       
    private static final UOption[] options = new UOption[] {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR(),
        UOption.DESTDIR(),
        UOption.PACKAGE_NAME(),
    };
    
    private static int tabCount = 0;
    private static ArrayList[] buf; 
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
    
    private static final String LINESEP = System.getProperty("line.separator");
    private static final String BOM = "\uFEFF";
    
    public static void main(String[] args) {
        XLIFF2ICUConverter cnv = new XLIFF2ICUConverter();
        cnv.processArgs(args);
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
        if(options[PACKAGE_NAME].doesOccur) {
            packageName = options[PACKAGE_NAME].value;
        }
        
        for (int i = 0; i < remainingArgc; i++) {
            tabCount = 0;
            int lastIndex = args[i].lastIndexOf(File.separator, args[i].length()) + 1; /* add 1 to skip past the separator */
            fileName = args[i].substring(lastIndex, args[i].length());
            convert(args[i]);
        }
    }
    
    private void convert(String fileName) {
        xmlfileName = getFullPath(false,fileName);

        bundleName = new ArrayList();
        bundleLang = new ArrayList();
        bundleList = new ArrayList();

        createRB(xmlfileName);

        bundleLen = bundleList.size();

        if(bundleLen == 0) {
            boolean b;
            b = bundleList.add(getFullPath(true, fileName));
            if(b==false){
                throw new RuntimeException("Could add "+fileName + "to bundleList");
            }
            int lastIndex = fileName.lastIndexOf('.', fileName.length());
            b = bundleName.add(fileName.substring(0, lastIndex));
            if(b==false){
                throw new RuntimeException("Could add "+fileName.substring(0, lastIndex) + "to bundleList");
            }
            bundleLen++;
        }
        
        buf = new ArrayList[bundleLen];
        for(int s = 0; s < bundleLen; s++) {
            buf[s] =  new ArrayList();
        }
        doc = parse(xmlfileName);
        wirteAll();
    }
    
    private void usage() {
        System.out.println("\nUsage: XLIFF2ICUConverter [OPTIONS] [FILES]\n\n"+
            "This program is used to convert XML files to TXT files.\n"+
            "Please refer to the following options. Options are not \n"+
            "case sensitive.\n"+
            "Options:\n"+
            "-s or --sourcedir    source directory for files followed by path, default is current directory.\n" +
            "-d or --destdir      destination directory, followed by the path, default is current directory.\n" +
            "-p or --package-name user may specify the package name explicitly.\n" +
            "-h or -? or --help   this usage text.\n"+
            "example: XLIFF2ICUConverter -s xxx -d yyy myResources.xml");
    }
    
    private String getFullPath(boolean fileType, String fName){
        String str;
        int lastIndex1 = fName.lastIndexOf(File.separator, fName.length()) + 1; /*add 1 to skip past the separator*/
        int lastIndex2 = fName.lastIndexOf('.', fName.length());
        if (fileType == true) {
            if (destDir != null && fName != null) {
                str = destDir + File.separator + fName.substring(lastIndex1, lastIndex2) + ".txt";                   
            } else {
                str = System.getProperty("user.dir") + File.separator + fName.substring(lastIndex1, lastIndex2) + ".txt";
            }
        } else {
            if(sourceDir != null && fName != null) {
                str = sourceDir + File.separator + fName.substring(lastIndex1, lastIndex2) + ".xlf";
            } else if (lastIndex1 > 0) {
                str = fName;
            } else {
                str = System.getProperty("user.dir") + File.separator + fName.substring(lastIndex1, lastIndex2) + ".xlf";
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
                        if (resType.equals(INTVECTOR)) {
                            isIntvector = true;
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
                        buf[s].add("// ***************************************************************************" + LINESEP);
                        buf[s].add("// *" + LINESEP);
                        buf[s].add("// * Tool: com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter.java" + LINESEP);
                        buf[s].add("// * Date & Time: " + c.get(Calendar.YEAR) + "/" + (c.get(Calendar.MONTH)+1) + "/" + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE)+ LINESEP);
                        buf[s].add("// * Source File: " + fileName + LINESEP);
                        buf[s].add("// *" + LINESEP);                    
                        buf[s].add("// ***************************************************************************" + LINESEP);
                        buf[s].add(LINESEP);
                        buf[s].add(LINESEP);                                        
                        buf[s].add((String)bundleName.get(s) + "{" + LINESEP);
                        
                    }
                } else if (doc.getNodeName().equals(GROUPS)){
                    transID = -1;
                    if (!isIntvector) {
                        writeTabs();
                        tabCount++;
                        for(int s = 0; s < bundleLen; s++){
                            buf[s].add(resName + ":" + resType + "{" + LINESEP);
                        }
                    } else {
                        writeTabs();
                        tabCount++;
                        for(int s = 0; s < bundleLen; s++){
                            buf[s].add(resName + ":" + INTVECTOR + "{" + LINESEP);
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
                            buf[s].add(resName + ":" + resType + "{\"");    
                        }
                    } else if (resType.equals(INTS) && isIntvector){
                    } else {
                        for(int s = 0; s < bundleLen; s++){
                            buf[s].add(resName + ":" + resType + "{");
                        }
                    }
                } else if (doc.getNodeName().equals(EXTERNALFILE)) {
                    for(int s = 0; s < bundleLen; s++){
                        buf[s].add("\"" + resValue + "\"");     
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
                        buf[s].add("}" + LINESEP);     
                    }
                    isIntvector = false;
                }else if (doc.getNodeName().equals(TRANSUNIT)||doc.getNodeName().equals(BINUNIT)){
                    if (resType.equals(STRINGS)||resType.equals(BIN)) {
                        for(int s = 0; s < bundleLen; s++){
                           buf[s].add("\"}" + LINESEP);     
                        }
                    } else if (resType.equals(INTS)&&isIntvector){
                        for(int s = 0; s < bundleLen; s++){
                           buf[s].add("," + LINESEP);   
                        }
                    } else {
                        for(int s = 0; s < bundleLen; s++){
                           buf[s].add("}" + LINESEP);   
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
                    if (!crc.equals("")){
                        if ((int)(resValue.charAt(0))!= 10){
                            if(Integer.parseInt(crc, 10) != CalculateCRC32.computeCRC32(resValue)) {
                                System.out.println("crc error! Please check.");
                                System.exit(1);
                              } else {
                                  crc = "";
                              }
                        }
                    }
                    for(int s = 0; s < bundleLen; s++){
                        if ((int)(resValue.charAt(0))!= 10){
                            buf[s].add(resValue);
                        }
                    }
                } else {
                    if ((int)(resValue.charAt(0))!= 10){
                        buf[transID].add(resValue);
                        transID++;
                    }
                }
                break;
            }
            default:
                System.err.println("Unrecongized node type");
        }
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
    private static String filenameToURL(String filename)
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

    private void createRB(String docsrc) {
        boolean tag = false;
        
        String urls = filenameToURL(docsrc);
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
            if(packageName != null) {
                pkg = packageName;
            }
            if(pkg.equals("")) {
                NodeList nlist = doc.getElementsByTagName(TARGET);
                if(nlist.getLength()> 0) {
                    System.out.println("There are translation items in the file. Please specify the package name using \"-p packageName\" in the command line.");
                    System.exit(0);
                }
                boolean b;
                b = bundleLang.add(lg);
                if(b==false){
                    throw new RuntimeException("Could not add "+lg+" to bundleLang");
                }
                int lastIndex = f.lastIndexOf('.', f.length());
                b = bundleName.add(f.substring(0, lastIndex));
                if(b==false){
                    throw new RuntimeException("Could not add "+f.substring(0, lastIndex)+" to bundleName");
                }
                b = bundleList.add(getFullPath(true,f));
                if(b==false){
                    throw new RuntimeException("Could not add "+getFullPath(true,f)+" to bundleList");
                }
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
                                if(b==false){
                                    throw new RuntimeException("Could not add "+lang+" to bundleLang");
                                }
                                b = bundleName.add(name);
                                if(b==false){
                                    throw new RuntimeException("Could not add "+name+" to bundleName");
                                }
                                b = bundleList.add(getFullPath(true, name + ".txt"));
                                if(b==false){
                                    throw new RuntimeException("Could not add "+getFullPath(true, name + ".txt")+" to bundleList");
                                }
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
        String urls = filenameToURL(docsrc);
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
    
    private static void writeTabs(){
        int i=0;
        for(;i<=tabCount;i++){
            for(int s = 0; s < bundleLen; s++){
                buf[s].add("    ");
            }
        }
    }
    
    private static void wirteAll() {
        FileOutputStream file = null;
        BufferedOutputStream buffer = null;

        for(int s = 0; s < bundleLen; s++){        
            try {
                file = new FileOutputStream((String)bundleList.get(s));
                buffer = new BufferedOutputStream(file);
            } catch (Exception ie) {
                System.out.println("ERROR :" + ie.toString());
                return;
            }
            writeBOM(buffer, "UTF-8");
            for(int t = 0; t < buf[s].size(); t++){
                WriteLine(buffer, (String)(buf[s].get(t)), "UTF-8");
            }
            try {
                buffer.flush();
                buffer.close();
                file.close();
            } catch (IOException ie) {
                System.err.println(ie);
                return;
            }
        }
    }
    
    private static void WriteLine(BufferedOutputStream buffer, String line, String charSet) {
        try {
            byte[] bytes = line.getBytes(charSet);
            buffer.write(bytes, 0, bytes.length);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }
    
    private static void writeBOM(BufferedOutputStream buffer, String charSet) {
        try {
            byte[] bytes = BOM.getBytes(charSet);
            buffer.write(bytes, 0, bytes.length);
        } catch(Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }
}