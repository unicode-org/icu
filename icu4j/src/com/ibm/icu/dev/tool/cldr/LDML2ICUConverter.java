/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package com.ibm.icu.dev.tool.cldr;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.*;

/**
 * @author ram
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class LDML2ICUConverter {
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int SOURCEDIR = 2;
    private static final int DESTDIR = 3;
    private static final int ICU = 4;
       
    private static final UOption[] options = new UOption[] {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR(),
        UOption.DESTDIR(),
        // UOption.create("icu", 'i', UOption.OPTIONAL_ARG),
        // UOption.create("posix", 'p', UOption.OPTIONAL_ARG),
        // UOption.create("open-office", 'o', UOption.NO_ARG)
    };
    private String    sourceDir      = null;
    private String    fileName       = null;
    private String    destDir        = null;
    private boolean   icu            = false;
    private static final String LINESEP         = System.getProperty("line.separator");
    private static final String BOM             = "\uFEFF";
    private static final String CHARSET         = "UTF-8";
    private static final String ALIAS           = "alias";
    private static final String COLON           = ":";
    
    private Document fullyResolved = null;
    private static final boolean DEBUG = false;
    
    public static void main(String[] args) {
        LDML2ICUConverter cnv = new LDML2ICUConverter();
        cnv.processArgs(args);
    }
    
    private void usage() {
        System.out.println("\nUsage: LDMLConverter [OPTIONS] [FILES]\n\n"+
            "This program is used to convert XLIFF files to ICU ResourceBundle TXT files.\n"+
            "Please refer to the following options. Options are not case sensitive.\n"+
            "Options:\n"+
            "-s or --sourcedir          source directory for files followed by path, default is current directory.\n" +
            "-d or --destdir            destination directory, followed by the path, default is current directory.\n" +
            "-h or -? or --help         this usage text.\n"+
            "example: org.unicode.ldml.LDML2ICUConverter -s xxx -d yyy en.xml");
        System.exit(-1);
    }
    
    private void processArgs(String[] args) {
        int remainingArgc = 0;
        try{
            remainingArgc = UOption.parseArgs(args, options);
        }catch (Exception e){
            System.err.println("ERROR: "+ e.toString());
            usage();
        }
        if(args.length==0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
            usage();
        }
        if(remainingArgc==0){
            System.err.println("ERROR: Either the file name to be processed is not "+
                               "specified or the it is specified after the -t/-c \n"+
                               "option which has an optional argument. Try rearranging "+
                               "the options.");
            usage();
        }
        if(options[SOURCEDIR].doesOccur) {
            sourceDir = options[SOURCEDIR].value;
        }
        if(options[DESTDIR].doesOccur) {
            destDir = options[DESTDIR].value;
        }
        //if(options[ICU].doesOccur){
        //    icu = true;
        //}
        if(destDir==null){
            destDir = ".";
        }

        for (int i = 0; i < remainingArgc; i++) {
            int lastIndex = args[i].lastIndexOf(File.separator, args[i].length()) + 1 /* add  1 to skip past the separator */; 
            fileName = args[i].substring(lastIndex, args[i].length());
            String xmlfileName = getFullPath(false,args[i]);
            System.out.println("Processing file: "+xmlfileName);
            /*
             * debugging code
             * 
             * try{ 
             *      Document doc = LDMLUtilities.getFullyResolvedLDML(sourceDir,
             *      args[i], false); 
             *      OutputStreamWriter writer = new
             *      OutputStreamWriter(new FileOutputStream("./"+File.separator+args[i]+"_debug.xml"),"UTF-8");
             *      LDMLUtilities.printDOMTree(doc,new PrintWriter(writer));
             *      writer.flush(); 
             * }catch( IOException e){ 
             *      //throw the exceptionaway .. this is for debugging 
             * }
             */ 
            fullyResolved =  LDMLUtilities.getFullyResolvedLDML(sourceDir, args[i], false);
            createResourceBundle(xmlfileName);
        }
    }
    private String getFullPath(boolean fileType, String fName){
        String str;
        int lastIndex1 = fName.lastIndexOf(File.separator, fName.length()) + 1/* add  1 to skip past the separator */; 
        int lastIndex2 = fName.lastIndexOf('.', fName.length());
        if (fileType == true) {
            if(lastIndex2 == -1){
                fName = fName.trim() + ".txt";
            }else{
                if(!fName.substring(lastIndex2).equalsIgnoreCase(".txt")){
                    fName =  fName.substring(lastIndex1,lastIndex2) + ".txt";
                }
            }
            if (destDir != null && fName != null) {
                str = destDir + File.separator + fName.trim();                   
            } else {
                str = System.getProperty("user.dir") + File.separator + fName.trim();
            }
        } else {
            if(lastIndex2 == -1){
                fName = fName.trim() + ".xml";
            }else{
                if(!fName.substring(lastIndex2).equalsIgnoreCase(".xml") && fName.substring(lastIndex2).equalsIgnoreCase(".xlf")){
                    fName = fName.substring(lastIndex1,lastIndex2) + ".xml";
                }
            }
            if(sourceDir != null && fName != null) {
                str = sourceDir + File.separator + fName;
            } else if (lastIndex1 > 0) {
                str = fName;
            } else {
                str = System.getProperty("user.dir") + File.separator + fName;
            }
        }
        return str;
    } 

    private void createResourceBundle(String xmlfileName) {
       
         try {
             Document doc = LDMLUtilities.parse(xmlfileName);
             // Create the Resource linked list which will hold the
             // data after parsing
             // The assumption here is that the top
             // level resource is always a table in ICU
             ICUResourceWriter.Resource res =  parseBundle(doc);
             
             // write out the bundle
             writeResource(res, xmlfileName);
          }
         catch (Throwable se) {
             System.err.println("ERROR: " + se.toString());

             System.exit(1);
         }        
    }
    
    private static String LDML           = "ldml";
    private static String IDENTITY       = "identity";
    private static String LDN            = "localeDisplayNames";
    private static String LAYOUT         = "layout";
    private static String CHARACTERS     = "characters";
    private static String DELIMITERS     = "delimiters";
    private static String MEASUREMENT    = "measurement";
    private static String DATES          = "dates";
    private static String NUMBERS        = "numbers";
    private static String COLLATIONS     = "collations";
    private static String POSIX          = "posix";
    private static String SPECIAL        = "special";
    
    private ICUResourceWriter.Resource setNext(ICUResourceWriter.Resource current, ICUResourceWriter.ResourceTable table, ICUResourceWriter.Resource toSet){
        if(current == null){
            current = table.first = toSet;
        }else{
            current.next = toSet;
        }
        return current.next;
    }
    private ICUResourceWriter.Resource parseBundle(Node root){
        ICUResourceWriter.ResourceTable table = null;
        ICUResourceWriter.Resource current = null;
        StringBuffer xpath = new StringBuffer();
        xpath.append("//ldml");
        int savedLength = xpath.length();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
             	continue;
            }
    		String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(IDENTITY)){
            	table = (ICUResourceWriter.ResourceTable) parseIdentity(node);
                ICUResourceWriter.Resource now = table.first;
                while(now!=null){
                    current = now;
                    now = now.next;
                }
                continue;
            }else if(name.equals(LDML) || name.equals(SPECIAL)/*
                                                               * IGNORE SPECIALS
                                                               * FOR NOW
                                                               */){
                node=node.getFirstChild();
                continue;
            }else if(name.equals(LDN)){
                res = parseLocaleDisplayNames(node, xpath);
            }else if(name.equals(LAYOUT)){
                //TODO res = parseLayout(node, xpath);
            }else if(name.equals(CHARACTERS)){
                res = parseCharacters(node, xpath);
            }else if(name.equals(DELIMITERS)){
                res = parseDelimiters(node, xpath);
            }else if(name.equals(MEASUREMENT)){
                res = parseMeasurement(node, xpath);
            }else if(name.equals(DATES)){
                res = parseDates(node, xpath);
            }else if(name.equals(NUMBERS)){
                res = parseNumbers(node, xpath);
            }else if(name.equals(COLLATIONS)){
                res = parseCollations(node, xpath);
            }else if(name.equals(POSIX)){
                res = parsePosix(node, xpath);
            }else if(name.indexOf("icu:")>-1|| name.indexOf("openOffice:")>-1){
                //TODO: these are specials .. ignore for now ... figure out
                // what to do later
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    table.first = res;
                    current = findLast(res);
                }else{
                    current.next = res;
                    current = findLast(res);
                }
                res = null;
            }
            xpath.delete(savedLength,xpath.length());
        }
        return table;
    }
    private ICUResourceWriter.Resource findLast(ICUResourceWriter.Resource res){
        ICUResourceWriter.Resource current = res;
        while(current!=null){
            if(current.next==null){
                return current;
            }
            current = current.next;
        }
        return current;
    }
    private static final String SOURCE = "source";
    private static final String ALT = "alt";
    private ICUResourceWriter.Resource parseAliasResource(Node node){
        if(node!=null){
            ICUResourceWriter.ResourceAlias alias = new ICUResourceWriter.ResourceAlias();
            String val = LDMLUtilities.getAttributeValue(node, SOURCE);
            // String xpathVal = LDMLUtilities.getAttributeValue(node, XPATH);
            alias.val = val;
            alias.name = node.getParentNode().getNodeName();
            return alias;
        }
        // TODO update when XPATH is integrated into LDML
        return null;
    }
    private static String VERSION    = "version";
    private static String LANGUAGE   = "language";
    private static String SCRIPT     = "script";
    private static String TERRITORY  = "territory";
    private static String VARIANT    = "variant";
    private static String TYPE       = "type";
    private static String NUMBER     = "number";
    private static String GENERATION = "generation";
    
    private void getXPath(Node node, StringBuffer xpath){
        xpath.append("/");
        xpath.append(node.getNodeName());
        LDMLUtilities.appendXPathAttribute(node,xpath);
    }
    private ICUResourceWriter.Resource parseIdentity(Node root){
        String localeID="", temp;
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource res = null;
        ICUResourceWriter.Resource current = null;
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            if(name.equals(VERSION)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.val = LDMLUtilities.getAttributeValue(node, NUMBER);
                str.name = VERSION;
                res = str;
            }else if(name.equals(LANGUAGE)|| 
                    name.equals(SCRIPT) ||
                    name.equals(TERRITORY)||
                    name.equals(VARIANT)){
            	// here we assume that language, script, territory, variant
                // are siblings are ordered. The ordering is enforced by the DTD
                temp = LDMLUtilities.getAttributeValue(node, TYPE);
                if(temp!=null && temp.length()!=0){
                    if(localeID.length()!=0){
                    	localeID += "_";
                    }
                	localeID += temp;
                }
            }else if(name.equals(GENERATION)){
                continue;
            }else if(name.equals(ALIAS)){
                 res = parseAliasResource(node);
                 res.name = table.name;
                 return res;
            }else{
               System.err.println("Unknown element found: "+name);
               System.exit(-1);
            }
            if(res!=null){
                if(current==null ){
                    current = table.first = res;
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
                
        }
        if(localeID.length()==0){
        	localeID="root";
        }
        table.name = localeID;
       // table.sort();
        
        return table;
    }
    private static final String LANGUAGES   = "languages";
    private static final String SCRIPTS     = "scripts";
    private static final String TERRITORIES = "territories";
    private static final String VARIANTS    = "variants";
    private static final String TYPES       = "types";
    private static final String KEYS        = "keys";
    private static final String[] registeredKeys = new String[]{
    		"collation",
            "calendar",
            "currency"
    };
    private ICUResourceWriter.Resource parseLocaleDisplayNames(Node root, StringBuffer xpath){
        ICUResourceWriter.Resource first = null;
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root,xpath);
        int oldLength = xpath.length();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(LANGUAGES)|| name.equals(SCRIPTS) || name.equals(TERRITORIES) || name.equals(KEYS) || name.equals(VARIANTS)){
            	res = parseList(node, xpath);
                //res.sort();
            }else if(name.equals(TYPES)){
                res = parseDisplayTypes(node);
            }else if(name.equals(ALIAS)){
                res = parseAliasResource(node);
            }else{
                 System.err.println("Unknown element found: "+name);
                 System.exit(-1);
            }
            if(res!=null){
                if(current==null ){
                    current = first = res;   
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }    
        xpath.delete(savedLength, xpath.length());
        return first;
    }
    
    private ICUResourceWriter.Resource parseDisplayTypes(Node root){
        StringBuffer xpath = new StringBuffer();
        xpath.append("//ldml/localeDisplayNames/types/type[@key='");
        int savedLength = xpath.length();
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        table.name = TYPES;
        ICUResourceWriter.ResourceTable current = null;
        for(int i=0; i<registeredKeys.length; i++){
            xpath.append(registeredKeys[i]);
            xpath.append("']");
        	NodeList list = LDMLUtilities.getNodeList(root.getOwnerDocument(), xpath.toString());
            if(list.getLength()!=0){
                ICUResourceWriter.ResourceTable subTable = new ICUResourceWriter.ResourceTable();
                subTable.name = registeredKeys[i];
                if(i==0){
                    table.first = current = subTable;
                }else{
                    current.next = subTable;
                    current = (ICUResourceWriter.ResourceTable)current.next;
                }
                ICUResourceWriter.ResourceString currentString = null;
                for(int j=0; j<list.getLength(); j++){
                    Node item = list.item(j);
                    String type = LDMLUtilities.getAttributeValue(item, TYPE);
                    String value = LDMLUtilities.getNodeValue(item);
                    
                    String altVal = LDMLUtilities.getAttributeValue(item, ALT);
                    // the alt atrribute is set .. so ignore the resource
                    if(altVal!=null){
                        continue;
                    }
                    //TODO now check if this string is draft
                    
                    ICUResourceWriter.ResourceString string = new ICUResourceWriter.ResourceString();
                    string.name = type;
                    string.val  = value;
                    if(j==0){
                        subTable.first = currentString = string;
                    }else{
                        currentString.next = string;
                        currentString = (ICUResourceWriter.ResourceString)currentString.next;
                    }
                }
            }
            xpath.delete(savedLength, xpath.length());
        }
        if(table.first!=null){
            return table;
        }
        return null;
    }
    private ICUResourceWriter.Resource parseList(Node root, StringBuffer xpath){
    	ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        table.name=root.getNodeName();
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
        	if(node.getNodeType()!=Node.ELEMENT_NODE){
        		continue;
            }
            getXPath(node, xpath);
            String altVal = LDMLUtilities.getAttributeValue(node, ALT);
            // the alt atrribute is set .. so ignore the resource
            if(altVal!=null){
                continue;
            }
            if(current==null){
            	current = table.first = new ICUResourceWriter.ResourceString();
            }else{
            	current.next = new ICUResourceWriter.ResourceString();
                current = current.next;
            }
            current.name = LDMLUtilities.getAttributeValue(node, TYPE);

            ((ICUResourceWriter.ResourceString)current).val  = LDMLUtilities.getNodeValue(node);
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        return table;
    }
    private static final String EXEMPLAR_CHARACTERS ="exemplarCharacters";
    private static final String MAPPING ="mapping";
    
    private ICUResourceWriter.Resource parseCharacters(Node root, StringBuffer xpath){
        ICUResourceWriter.Resource current = null, first=null;
        int savedLength = xpath.length();
        getXPath(root,xpath);
        int oldLength = xpath.length();
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            getXPath(node, xpath);
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(EXEMPLAR_CHARACTERS)){
                res = parseStringResource(node);
            }else if(name.equals(ALIAS)){
                res = parseAliasResource(node);
            }else if(name.equals(MAPPING)){
                //TODO: Currently we dont have a way to represent this data in ICU!
            }else{
                 System.err.println("Unknown element found: "+name);
                 System.exit(-1);
            }
            if(res!=null){
                if(current==null ){
                    current = first = res;   
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        return first;
    }
    private ICUResourceWriter.Resource parseStringResource(Node node){
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.val = LDMLUtilities.getNodeValue(node);
        str.name = node.getNodeName();
        return str;
    }
    private static final String QS  = "quotationStart";
    private static final String QE  = "quotationEnd";
    private static final String AQS = "alternateQuotationStart";
    private static final String AQE = "alternateQuotationEnd";
    private ICUResourceWriter.Resource parseDelimiters(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        table.name = root.getNodeName();
        getXPath(root,xpath);
        ICUResourceWriter.Resource res = parseDelimiter(root, xpath);
        if(res!=null){
            table.first = res;
            return table;
        }
        return null;
    }
    private ICUResourceWriter.Resource parseDelimiter(Node root, StringBuffer xpath){
        ICUResourceWriter.Resource current = null,first=null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            getXPath(node, xpath);
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(QS) || name.equals(QE)||
               name.equals(AQS)|| name.equals(AQE)){
                res = parseStringResource(node);
            }else if(name.equals(ALIAS)){
                res = parseAliasResource(node);
            }else if(name.equals(MAPPING)){
                //TODO: Currently we dont have a way to represent this data in ICU!
            }else{
                 System.err.println("Unknown element found: "+name);
                 System.exit(-1);
            }
            if(res!=null){
                if(current==null ){
                    current = first = res;   
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength,xpath.length());
        return first;
    }
    private static final String MS          = "measurementSystem";
    private static final String HEIGHT      = "height";
    private static final String WIDTH       = "width";
    private static final String PAPER_SIZE  = "paperSize";
    
    private ICUResourceWriter.Resource parsePaperSize(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceIntVector vector = new ICUResourceWriter.ResourceIntVector();
        vector.name = root.getNodeName();
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            getXPath(node, xpath);
            // here we assume that the DTD enforces the correct order
            // of elements
            if(name.equals(HEIGHT)||name.equals(WIDTH)){
                ICUResourceWriter.ResourceInt resint = new ICUResourceWriter.ResourceInt();
                resint.val = LDMLUtilities.getNodeValue(node);
                res = resint;
            }else if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                //We know that paperSize element can only contain either alias or (height and width)
                return res; 
            }else{
                System.err.println("Unknown element found: "+name);
                System.exit(-1);              
            }
            if(res != null){
                if(current == null){
                    current = vector.first = (ICUResourceWriter.ResourceInt) res;
                }else{
                    current.next = res;
                    current = current.next;
                }
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        return vector;
    }
    private ICUResourceWriter.Resource parseMeasurement (Node root, StringBuffer xpath){
        ICUResourceWriter.Resource current = null,first=null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            getXPath(node, xpath);
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(MS)){
                String altVal = LDMLUtilities.getAttributeValue(node, ALT);
                // the alt atrribute is set .. so ignore the resource
                if(altVal!=null){
                    continue;
                }
                ICUResourceWriter.ResourceInt resint = new ICUResourceWriter.ResourceInt();
                String sys = LDMLUtilities.getAttributeValue(node,TYPE);
                if(sys.equals("US")){
                    resint.val = "1";
                }else{
                    resint.val = "0";
                }
                resint.name = MS;
                res = resint;
            }else if(name.equals(ALIAS)){
                res = parseAliasResource(node);
            }else if(name.equals(PAPER_SIZE)){
                res = parsePaperSize(node, xpath);
            }else{
                 System.err.println("Unknown element found: "+name);
                 System.exit(-1);
            }
            if(res!=null){
                if(current==null ){
                    current = first = res;   
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        return first;
    }
    
    private static final String LPC ="localizedPatternChars";
    private static final String DEFAULT = "default";
    private static final String CALENDARS = "calendars";
    private static final String MONTHS  = "months";
    private static final String DAYS    = "days";
    private static final String TZN     = "timeZoneNames";
    
    private ICUResourceWriter.Resource parseDates(Node root, StringBuffer xpath){
        ICUResourceWriter.Resource first = null;
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                //dont compute xpath
                res = parseAliasResource(node);
            }else if(name.equals(DEFAULT) ){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node, TYPE);
                res = str;
            }else if(name.equals(LPC)){
                getXPath(node, xpath);
                if(xpath.indexOf(ALT)<0){
                    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                    str.name = name;
                    str.val = LDMLUtilities.getNodeValue(node);
                    res = str;
                }
            }else if(name.equals(CALENDARS)){
                res = parseCalendars(node, xpath);
            }else if(name.equals(TZN)){
                res = parseTimeZoneNames(node, xpath);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = first = res;
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        return first;
    }
    private static final String CALENDAR = "calendar";
    private ICUResourceWriter.Resource parseCalendars(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        table.name = root.getNodeName();
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name =table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(CALENDAR)){
                res = parseCalendar(node, xpath);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first = res;
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(table.first!=null){
            return table;
        }
        return null;
    }
    private ICUResourceWriter.Resource parseTimeZoneNames(Node root, StringBuffer xpath){
        //TODO
        return null;
    }
    private static final String WEEK = "week";
    private static final String AM   = "am";
    private static final String PM   = "pm";
    private static final String ERAS = "eras";
    private static final String DATE_FORMATS      = "dateFormats";
    private static final String TIME_FORMATS      = "timeFormats";
    private static final String DATE_TIME_FORMATS = "dateTimeFormats";
    
    private ICUResourceWriter.Resource parseCalendar(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        boolean writtenAmPm = false;
        boolean writtenDTF = false;
        table.name = LDMLUtilities.getAttributeValue(root,TYPE);
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name =table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(MONTHS)|| name.equals(DAYS)){
                res = parseMonthsAndDays(node, xpath);
            }else if(name.equals(WEEK)){
                ICUResourceWriter.Resource temp = parseWeek(node, xpath);
                if(temp!=null){
                    res = temp;
                }
            }else if(name.equals(AM)|| name.equals(PM)){
                //TODO: figure out the tricky parts .. basically get the missing element from
                // fully resolved locale!
                if(writtenAmPm==false){
                    res = parseAmPm(node, xpath);
                    writtenAmPm = true;
                }
            }else if(name.equals(ERAS)){
                res = parseEras(node, xpath);
            }else if(name.equals(DATE_FORMATS)||name.equals(TIME_FORMATS)|| name.equals(DATE_TIME_FORMATS)){
                // TODO
                if(writtenDTF==false){
                    res = parseDTF(node, xpath);
                    writtenDTF = true;
                }
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first = res;
                }else{
                    current.next = res;
                    current = findLast(res);
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(table.first!=null){
            return table;
        }
        return null;
    }
    private static String MONTH_CONTEXT = "monthContext";
    private static String DAY_CONTEXT   = "dayContext";
    private ICUResourceWriter.Resource parseMonthsAndDays(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        table.name = root.getNodeName();
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name=table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(MONTH_CONTEXT)|| name.equals(DAY_CONTEXT)){
                res = parseContext(node, xpath);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first = res;
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(table.first!=null){
            return table;
        }
        return null;
    }
    private ICUResourceWriter.Resource parseContext(Node root, StringBuffer xpath ){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        table.name = LDMLUtilities.getAttributeValue(root,TYPE);
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        String resName = root.getNodeName();
        resName = resName.substring(0, resName.lastIndexOf("Context"));
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name = table.name;
                return res; // an alias if for the resource
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(resName+"Width")){
                res = parseWidth(node, resName, xpath);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first = res;
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(table.first!=null){
            return table;
        }
        return null;
    }
    private String getDayIndexAsString(String type){
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
    private String getMonthIndexAsString(String type){
        return Integer.toString(Integer.parseInt(type)-1);
    }
    private static final String MONTH = "month";
    private static final String DAY   = "day";
    
    private ICUResourceWriter.Resource parseWidth(Node root, String resName, StringBuffer xpath){
        ICUResourceWriter.ResourceArray array = new ICUResourceWriter.ResourceArray();
        ICUResourceWriter.Resource current = null;
        array.name = LDMLUtilities.getAttributeValue(root,TYPE);

        int savedLength = xpath.length();
        getXPath(root, xpath);
        //int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        
        HashMap map = getElementsMap(root);
        if((resName.equals(DAY) && map.size()<7) ||
            (resName.equals(MONTH)&& map.size()<12)){
            root = LDMLUtilities.getNode(fullyResolved,xpath.toString() );
            map = getElementsMap(root);
        }
        if(map.size()>0){
            for(int i=0; i<map.size(); i++){
               String key = Integer.toString(i);
               ICUResourceWriter.ResourceString res = new ICUResourceWriter.ResourceString();
               res.val = (String)map.get(key);
               // array of unnamed strings
               if(current == null){
                   current = array.first = res;
               }else{
                   current.next = res;
                   current = current.next;
               }
            }
        }
        if(array.first!=null){
            return array;
        }
        xpath.delete(savedLength, xpath.length());
        return null;
    }
    private HashMap getElementsMap(Node root){
        HashMap map = new HashMap();
        // first create the hash map;
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            String val = LDMLUtilities.getNodeValue(node);
            String type = LDMLUtilities.getAttributeValue(node,TYPE);
            String alt = LDMLUtilities.getAttributeValue(node,ALT);
            if(alt!=null){
                // ignore elements with alt attribute set
                continue;
            }
            if(name.equals(DAY)){
                map.put(getDayIndexAsString(type), val);
            }else if(name.equals(MONTH)){
                map.put(getMonthIndexAsString(type), val);
            }else if( name.equals(ERA)){
                map.put(type, val);
            }else{
            
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
        }
        return map;
    }
    private static final String COUNT = "count";
    private static final String DTE   = "DateTimeElements";
    private static final String MINDAYS = "minDays";
    private static final String FIRSTDAY = "firstDay" ;
    private static final String WENDSTART = "weekendStart";
    private static final String WENDEND   = "weekendEnd";
    private static final String WEEKEND   = "weekend";
    private static final String TIME      =  "time";
    private static final String ERA       = "era";
    private ICUResourceWriter.Resource parseWeek(Node root, StringBuffer xpath){
        ICUResourceWriter.Resource dte = parseDTE(root, xpath);
        ICUResourceWriter.Resource wkend = parseWeekend(root, xpath);
        if(dte!=null){
            dte.next = wkend;
            return dte;
        }
        return wkend;
    }
    private int getMillis(String time){
       String[] strings = time.split(":"); // time is in hh:mm format
       int hours = Integer.parseInt(strings[0]);
       int minutes = Integer.parseInt(strings[1]);
       return  (hours * 60  + minutes ) * 60 * 1000;
    }
    private ICUResourceWriter.Resource parseWeekend(Node root, StringBuffer xpath){
        Node wkendStart = LDMLUtilities.getNode(root, WENDSTART, fullyResolved, xpath.toString());
        Node wkendEnd = LDMLUtilities.getNode(root, WENDEND, fullyResolved, xpath.toString());
        ICUResourceWriter.ResourceIntVector wkend = null;
        
        if(wkendStart!=null && wkendEnd!=null){
            try{
                wkend =  new ICUResourceWriter.ResourceIntVector();
                wkend.name = WEEKEND;
                ICUResourceWriter.ResourceInt startday = new ICUResourceWriter.ResourceInt();
                startday.val = getDayIndexAsString(LDMLUtilities.getAttributeValue(wkendStart, DAY));
                ICUResourceWriter.ResourceInt starttime = new ICUResourceWriter.ResourceInt();
                starttime.val = Integer.toString(getMillis(LDMLUtilities.getAttributeValue(wkendStart, TIME)));
                ICUResourceWriter.ResourceInt endday = new ICUResourceWriter.ResourceInt();
                endday.val = getDayIndexAsString(LDMLUtilities.getAttributeValue(wkendEnd, DAY));
                ICUResourceWriter.ResourceInt endtime = new ICUResourceWriter.ResourceInt();
                endtime.val = Integer.toString(getMillis(LDMLUtilities.getAttributeValue(wkendEnd, TIME)));
                wkend.first = startday;
                startday.next = starttime;
                starttime.next = endday;
                endday.next = endtime;
            }catch(NullPointerException ex){
                throw new RuntimeException(ex);
            }
        }
 
        return wkend; 
    }
    private ICUResourceWriter.Resource parseDTE(Node root, StringBuffer xpath){
        Node minDays = LDMLUtilities.getNode(root, MINDAYS, fullyResolved, xpath.toString());
        Node firstDay = LDMLUtilities.getNode(root, FIRSTDAY, fullyResolved, xpath.toString());
        ICUResourceWriter.ResourceIntVector dte = null;
        if(minDays!=null && firstDay!=null){
            dte =  new ICUResourceWriter.ResourceIntVector();
            ICUResourceWriter.ResourceInt int1 = new ICUResourceWriter.ResourceInt();
            int1.val = LDMLUtilities.getAttributeValue(minDays, COUNT);
            ICUResourceWriter.ResourceInt int2 = new ICUResourceWriter.ResourceInt();
            int2.val = getDayIndexAsString(LDMLUtilities.getAttributeValue(firstDay, DAY));
            
            dte.name = DTE;
            dte.first = int1;
            int1.next = int2;
        }
        if((minDays==null && firstDay!=null) || minDays!=null && firstDay==null){
            throw new RuntimeException("Could not find "+minDays+" or "+firstDay +" from fullyResolved locale!!");
        }
        return dte;
    }
    
    private static final String ERAABBR ="eraAbbr";
    private static final String ERANAMES ="eraNames";
    private static final String ABBREVIATED ="abbreviated";
    private static final String WIDE = "wide";
    private ICUResourceWriter.Resource parseEras(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        table.name = ERAS;
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name =table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(ERAABBR)){
                res = parseEra(node, xpath, ABBREVIATED);
            }else if( name.equals(ERANAMES)){
                res = parseEra(node, xpath, WIDE);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first = res;
                }else{
                    current.next = res;
                    current = findLast(res);
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(table.first!=null){
            return table;
        }
        return null;
              
    }
    private ICUResourceWriter.Resource parseEra( Node root, StringBuffer xpath, String name){
        ICUResourceWriter.ResourceArray array = new ICUResourceWriter.ResourceArray();
        ICUResourceWriter.Resource current = null;
        array.name = name;

        int savedLength = xpath.length();
        getXPath(root, xpath);
       // int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        
        HashMap map = getElementsMap(root);

        if(map.size()>0){
            for(int i=0; i<map.size(); i++){
               String key = Integer.toString(i);
               ICUResourceWriter.ResourceString res = new ICUResourceWriter.ResourceString();
               res.val = (String)map.get(key);
               //TODO: fix this!!
               if(res.val==null){
                   continue;
               }
               // array of unnamed strings
               if(current == null){
                   current = array.first = res;
               }else{
                   current.next = res;
                   current = current.next;
               }
            }
        }
        xpath.delete(savedLength,xpath.length());
        if(array.first!=null){
            return array;
        }
        return null;
   
    }
    private static final String AM_PM_MARKERS = "AmPmMarkers";
    private ICUResourceWriter.Resource parseAmPm(Node root, StringBuffer xpath){
        Node parent =root.getParentNode();
        Node amNode = LDMLUtilities.getNode(parent, AM, fullyResolved, xpath.toString());
        Node pmNode = LDMLUtilities.getNode(parent, PM, fullyResolved, xpath.toString());
        ICUResourceWriter.ResourceArray arr = null;
        if(amNode!=null && pmNode!= null){
            arr = new ICUResourceWriter.ResourceArray();
            arr.name = AM_PM_MARKERS;
            ICUResourceWriter.ResourceString am = new ICUResourceWriter.ResourceString();
            ICUResourceWriter.ResourceString pm = new ICUResourceWriter.ResourceString();
            am.val = LDMLUtilities.getNodeValue(amNode);
            pm.val = LDMLUtilities.getNodeValue(pmNode);
            arr.first = am;
            am.next = pm;
        }
        if((amNode==null && pmNode!=null) || amNode!=null && pmNode==null){
            throw new RuntimeException("Could not find "+amNode+" or "+pmNode +" from fullyResolved locale!!");
        }
        return arr;
    }
    private static final String DTP ="DateTimePatterns";
    
    private ICUResourceWriter.Resource parseDTF(Node root, StringBuffer xpath){
        // TODO change the ICU format to reflect LDML format
        /*
         * The prefered ICU format would be
         * timeFormats{
         *      default{}
         *      full{}
         *      long{}
         *      medium{}
         *      short{}
         *      ....
         * }
         * dateFormats{
         *      default{}
         *      full{}
         *      long{}
         *      medium{}
         *      short{}
         *      .....
         * }
         * dateTimeFormats{
         *      standard{}
         *      ....
         * }   
         */
        
        // here we dont add stuff to XPATH since we are querying the parent 
        // with the hardcoded XPATHS!
        
        //TODO figure out what to do for alias
        Node parent = root.getParentNode();
        ArrayList list = new ArrayList();
        list.add(LDMLUtilities.getNode(parent, "timeFormats/timeFormatLength[@type='full']/timeFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "timeFormats/timeFormatLength[@type='long']/timeFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "timeFormats/timeFormatLength[@type='medium']/timeFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "timeFormats/timeFormatLength[@type='short']/timeFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "dateFormats/dateFormatLength[@type='full']/dateFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "dateFormats/dateFormatLength[@type='long']/dateFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "dateFormats/dateFormatLength[@type='medium']/dateFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "dateFormats/dateFormatLength[@type='short']/dateFormat[@type='standard']/pattern", fullyResolved, xpath.toString()));
        //TODO guard this against possible failure 
        list.add(LDMLUtilities.getNode(parent, "dateTimeFormats/dateTimeFormatLength/dateTimeFormat/pattern", fullyResolved, xpath.toString()));
        
        if(list.size()<9){
            throw new RuntimeException("Did not get expected output for Date and Time patterns!!");
        }
        ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
        arr.name = DTP;
        ICUResourceWriter.Resource current = null;
        for(int i= 0; i<list.size(); i++){
            ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
            Node temp = (Node)list.get(i);
            if(temp==null){
                throw new RuntimeException("Did not get expected output for Date and Time patterns!!");
            }
            str.val = LDMLUtilities.getNodeValue(temp);
            if(str.val!=null){
                if(current==null){
                    current = arr.first = str;
                }else{
                    current.next = str;
                    current = current.next;
                }
            }else{
                throw new RuntimeException("the node value for Date and Time patterns is null!!");
            }     
        }
        
        if(arr.first!=null){
            return arr;
        }
        return null;
    }
    private static final String DECIMAL_FORMATS = "decimalFormats";
    private static final String SCIENTIFIC_FORMATS = "scientificFormats";
    private static final String CURRENCY_FORMATS = "currencyFormats";
    private static final String PERCENT_FORMATS = "percentFormats";
    private static final String SYMBOLS        = "symbols";
    private static final String CURRENCIES    = "currencies";
    private ICUResourceWriter.Resource parseNumbers(Node root, StringBuffer xpath){
        ICUResourceWriter.Resource current = null, first =null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        boolean writtenFormats = false;
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name = name;
                return res;
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(SYMBOLS)){
                res = parseSymbols(node, xpath);
            }else if( name.equals(DECIMAL_FORMATS) || name.equals(PERCENT_FORMATS)|| 
                     name.equals(SCIENTIFIC_FORMATS)||name.equals(CURRENCY_FORMATS) ){
                if(writtenFormats==false){
                    res = parseNumberFormats(node, xpath);
                    writtenFormats = true;
                }
            }else if(name.equals(CURRENCIES)){
                res = parseCurrencies(node, xpath);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = first = res;
                }else{
                    current.next = res;
                    current = findLast(res);
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(first!=null){
            return first;
        }
        return null;
    }
    private static final String NUMBER_ELEMENTS = "NumberElements";
    private static final String NUMBER_PATTERNS = "NumberPatterns";
    
    private ICUResourceWriter.Resource parseSymbols(Node root, StringBuffer xpath){
        int savedLength = xpath.length();
        getXPath(root, xpath);
        //int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        //TODO figure out what to do for alias
        ArrayList list = new ArrayList();
        list.add(LDMLUtilities.getNode(root, "decimal", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "group", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "list", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "percentSign", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "nativeZeroDigit", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "patternDigit", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "plusSign", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "minusSign", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "exponential", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "perMille", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "infinity", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(root, "nan", fullyResolved, xpath.toString()));
        ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
        arr.name = NUMBER_ELEMENTS;
        ICUResourceWriter.Resource current = null;
        for(int i= 0; i<list.size(); i++){
            ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
            Node temp = (Node)list.get(i);
            if(temp==null){
                throw new RuntimeException("Did not get expected output for Date and Time patterns!!");
            }
            str.val = LDMLUtilities.getNodeValue(temp);
            if(str.val!=null){
                if(current==null){
                    current = arr.first = str;
                }else{
                    current.next = str;
                    current = current.next;
                }
            }else{
                throw new RuntimeException("the node value for Date and Time patterns is null!!");
            }     
        }

        xpath.delete(savedLength, xpath.length());
        
        if(arr.first!=null){
            return arr;
        }
        return null;
        
    }
    private ICUResourceWriter.Resource parseNumberFormats(Node root, StringBuffer xpath){
       
//      here we dont add stuff to XPATH since we are querying the parent 
        // with the hardcoded XPATHS!
        
        //TODO figure out what to do for alias
        Node parent = root.getParentNode();
        ArrayList list = new ArrayList();
        list.add(LDMLUtilities.getNode(parent, "decimalFormats/decimalFormatLength/decimalFormat/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "currencyFormats/currencyFormatLength/currencyFormat/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "percentFormats/percentFormatLength/percentFormat/pattern", fullyResolved, xpath.toString()));
        list.add(LDMLUtilities.getNode(parent, "scientificFormats/scientificFormatLength/scientificFormat/pattern", fullyResolved, xpath.toString()));
        
        if(list.size()<4){
            throw new RuntimeException("Did not get expected output for number patterns!!");
        }
        ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
        arr.name = NUMBER_PATTERNS;
        ICUResourceWriter.Resource current = null;
        for(int i= 0; i<list.size(); i++){
            ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
            Node temp = (Node)list.get(i);
            if(temp==null){
                throw new RuntimeException("Did not get expected output for number patterns!!");
            }
            str.val = LDMLUtilities.getNodeValue(temp);
            if(str.val!=null){
                if(current==null){
                    current = arr.first = str;
                }else{
                    current.next = str;
                    current = current.next;
                }
            }else{
                throw new RuntimeException("the node value for number patterns is null!!");
            }     
        }
        
        if(arr.first!=null){
            return arr;
        }
        return null;

    }
    private static final String CURRENCY = "currency";

    private ICUResourceWriter.Resource parseCurrencies(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        table.name = root.getNodeName();
        
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name = name;
                return res;
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(CURRENCY)){
                res = parseCurrency(node, xpath);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first = res;
                }else{
                    current.next = res;
                    current = findLast(res);
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(table.first!=null){
            return table;
        }
        return null;
    }
    private static final String SYMBOL = "symbol";
    private static final String DISPLAY_NAME ="displayName";
    private static final String PATTERN ="pattern";
    private static final String DECIMAL = "decimal";
    private static final String GROUP = "group";
    
    private ICUResourceWriter.Resource parseCurrency(Node root, StringBuffer xpath){
        int savedLength = xpath.length();
        getXPath(root, xpath);
        //int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        Node alias = LDMLUtilities.getNode(root, ALIAS, fullyResolved, xpath.toString());
        if(alias!=null){
            ICUResourceWriter.Resource res = parseAliasResource(alias);
            res.name = LDMLUtilities.getAttributeValue(root, TYPE);
            xpath.delete(savedLength, xpath.length());
            return res;
        }
        Node symbolNode = LDMLUtilities.getNode(root, SYMBOL , fullyResolved, xpath.toString());
        Node displayNameNode = LDMLUtilities.getNode(root, DISPLAY_NAME , fullyResolved, xpath.toString());
        ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
        arr.name = LDMLUtilities.getAttributeValue(root, TYPE);
        if(symbolNode==null||displayNameNode==null){
            throw new RuntimeException("Could not get dispaly name and symbol from currency resource!!");
        }
        ICUResourceWriter.ResourceString symbol = new ICUResourceWriter.ResourceString();
        symbol.val = LDMLUtilities.getNodeValue(symbolNode);
        ICUResourceWriter.ResourceString displayName = new ICUResourceWriter.ResourceString();
        displayName.val = LDMLUtilities.getNodeValue(displayNameNode);
        
        arr.first = symbol;
        symbol.next = displayName;
        
        Node patternNode = LDMLUtilities.getNode(root, PATTERN , fullyResolved, xpath.toString());
        Node decimalNode = LDMLUtilities.getNode(root, DECIMAL , fullyResolved, xpath.toString());
        Node groupNode   = LDMLUtilities.getNode(root, GROUP , fullyResolved, xpath.toString());
        if(patternNode!=null || decimalNode!=null || groupNode!=null){
            if(patternNode==null || decimalNode==null || groupNode==null){
                throw new RuntimeException("Could not get pattern or decimal or group currency resource!!");
            }
            ICUResourceWriter.ResourceArray elementsArr = new ICUResourceWriter.ResourceArray();
            
            ICUResourceWriter.ResourceString pattern = new ICUResourceWriter.ResourceString();
            pattern.val = LDMLUtilities.getNodeValue(patternNode);
            
            ICUResourceWriter.ResourceString decimal = new ICUResourceWriter.ResourceString();
            decimal.val = LDMLUtilities.getNodeValue(decimalNode);
            
            ICUResourceWriter.ResourceString group = new ICUResourceWriter.ResourceString();
            group.val = LDMLUtilities.getNodeValue(groupNode);
            
            elementsArr.first = pattern;
            pattern.next = decimal;
            decimal.next = group;
            
            displayName.next = elementsArr;
        }
        xpath.delete(savedLength, xpath.length());
        if(arr.first!=null){
            return arr;
        }
        return arr;      
    }
    private static final String MESSAGES = "messages";
    private ICUResourceWriter.Resource parsePosix(Node root, StringBuffer xpath){ 
        ICUResourceWriter.Resource first = null;
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root,xpath);
        int oldLength = xpath.length();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(MESSAGES)){
                res = parseMessages(node, xpath);
            }else if(name.equals(ALIAS)){
                res = parseAliasResource(node);
            }else{
                 System.err.println("Unknown element found: "+name);
                 System.exit(-1);
            }
            if(res!=null){
                if(current==null ){
                    current = first = res;   
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }    
        xpath.delete(savedLength, xpath.length());
        return first;
    }
    private static final String YESSTR = "yesstr";
    private static final String YESEXPR = "yesexpr";
    private static final String NOSTR   = "nostr";
    private static final String NOEXPR  = "noexpr";
    private ICUResourceWriter.Resource parseMessages(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        int savedLength = xpath.length();
        getXPath(root,xpath);
        int oldLength = xpath.length();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(YESSTR)||name.equals(YESEXPR)||name.equals(NOSTR)||name.equals(NOEXPR)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getNodeValue(node);
                res = str;
            }else if(name.equals(ALIAS)){
                res = parseAliasResource(node);
            }else{
                 System.err.println("Unknown element found: "+name);
                 System.exit(-1);
            }
            if(res!=null){
                if(current==null ){
                    current = table.first = res;   
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }    
        xpath.delete(savedLength, xpath.length());
        return table.first;
    }
    private static final String COLLATION = "collation";
    private ICUResourceWriter.Resource parseCollations(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        table.name = root.getNodeName();
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name =table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(COLLATION)){
                res = parseCollation(node, xpath);
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first= res;
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(table.first!=null){
            return table;
        }
        return null;

    }
    private static final String SEQUENCE = "Sequence";

    private static final String RULES = "rules";
    private static final String SETTINGS = "settings";
    private static final String SUPPRESS_CONTRACTIONS = "suppress_contractions";
    private static final String OPTIMIZE = "optimize";
    private static final String BASE = "base";
    private static final String STRENGTH        = "strength";
    private static final String ALTERNATE       = "alternate";
    private static final String BACKWARDS       = "backwards";
    private static final String NORMALIZATION   = "normalization";
    private static final String CASE_LEVEL      = "caseLevel";
    private static final String CASE_FIRST      = "caseFirst";
    private static final String HIRAGANA_Q      = "hiraganaQuarternary";
    private static final String NUMERIC         = "numeric";
    private ICUResourceWriter.Resource parseCollation(Node root, StringBuffer xpath){
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;
        table.name = LDMLUtilities.getAttributeValue(root, TYPE);
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        StringBuffer rules = new StringBuffer();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name =table.name;
                return res;
            }else if(name.equals(RULES)){
                Node alias = LDMLUtilities.getNode(node, ALIAS , fullyResolved, xpath.toString());
                if(alias!=null){
                    res = parseAliasResource(alias);
                }else{
                    rules.append( parseRules(node, xpath));
                }
            }else if(name.equals(SETTINGS)){
                //TODO
                rules.append(parseSettings(node));
            }else if(name.equals(SUPPRESS_CONTRACTIONS)){
                rules.append("[suppressContractions");
                rules.append(LDMLUtilities.getNodeValue(node));
                rules.append("]");
            }else if(name.equals(OPTIMIZE)){
                rules.append("[optimize");
                rules.append(LDMLUtilities.getNodeValue(node));
                rules.append("]");
            }else if (name.equals(BASE)){
                //TODO Dont know what to do here
            }else{
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
            if(res!=null){
                if(current == null){
                    current = table.first= res;
                }else{
                    current.next = res;
                    current = current.next;
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if(rules.length()>0){
            ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
            str.name = SEQUENCE;
            str.val = rules.toString();
            if(current == null){
                current = table.first= str;
            }else{
                current.next = str;
                current = current.next;
            }
            str = new ICUResourceWriter.ResourceString();
            str.name = "Version";
            str.val = "1.1.1";
            current.next = str;
            
        }
        if(table.first!=null){
            return table;
        }
        return null;        
    }
    private StringBuffer parseSettings(Node node){
        String strength = LDMLUtilities.getAttributeValue(node, STRENGTH);
        StringBuffer rules= new StringBuffer();
        if(strength!=null){
            rules.append(" [strength ");
            rules.append(getStrength(strength));
            rules.append(" ]");
        }
        String alternate = LDMLUtilities.getAttributeValue(node, ALTERNATE);
        if(alternate!=null){
            rules.append(" [alternate ");
            rules.append(alternate);
            rules.append(" ]");
        }
        String backwards = LDMLUtilities.getAttributeValue(node, BACKWARDS);
        if(backwards!=null && backwards.equals("on")){
            rules.append(" [backwards 2]");
        }
        String normalization = LDMLUtilities.getAttributeValue(node, NORMALIZATION);
        if(normalization!=null){
            rules.append(" [normalization ");
            rules.append(normalization);
            rules.append(" ]");
        }
        String caseLevel = LDMLUtilities.getAttributeValue(node, CASE_LEVEL);
        if(caseLevel!=null){
            rules.append(" [caseLevel ");
            rules.append(caseLevel);
            rules.append(" ]");
        }
        
        String caseFirst = LDMLUtilities.getAttributeValue(node, CASE_FIRST);
        if(caseFirst!=null){
            rules.append(" [caseFirst ");
            rules.append(caseFirst);
            rules.append(" ]");
        }
        String hiraganaQ = LDMLUtilities.getAttributeValue(node, HIRAGANA_Q);
        if(hiraganaQ!=null){
            rules.append(" [hiraganaQ ");
            rules.append(hiraganaQ);
            rules.append(" ]");
        }
        String numeric = LDMLUtilities.getAttributeValue(node, NUMERIC);
        if(numeric!=null){
            rules.append(" [numeric ");
            rules.append(numeric);
            rules.append(" ]");
        }
        return rules;
    }
    private static final HashMap collationMap = new HashMap();
    static{
        collationMap.put("first_tertiary_ignorable", "[first tertiary ignorable ]");
        collationMap.put("last_tertiary_ignorable",  "[last tertiary ignorable ]");
        collationMap.put("first_secondary_ignorable","[first secondary ignorable ]");
        collationMap.put("last_secondary_ignorable", "[last secondary ignorable ]");
        collationMap.put("first_primary_ignorable",  "[first primary ignorable ]");
        collationMap.put("last_primary_ignorable",   "[last primary ignorable ]");
        collationMap.put("first_variable",           "[first variable ]");
        collationMap.put("last_variable",            "[last variable ]");
        collationMap.put("first_non_ignorable",      "[first regular]");
        collationMap.put("last_non_ignorable",       "[last regular ]");
        //TODO check for implicit
        //collationMap.put("??",      "[first implicit]");
        //collationMap.put("??",       "[last implicit]");
        collationMap.put("first_trailing",           "[first trailing ]");
        collationMap.put("last_trailing",            "[last trailing ]");
    }
    private static final String RESET = "reset";
    private static final String PC = "pc";
    private static final String SC = "sc";
    private static final String TC = "tc";
    private static final String QC = "qc";
    private static final String IC = "ic";
    private static final String P = "p";
    private static final String S = "s";
    private static final String T = "t";
    private static final String Q = "q";
    private static final String I = "i";
    private static final String X = "x";
    private static final String LAST_VARIABLE ="last_variable";
    
    private StringBuffer parseRules(Node root, StringBuffer xpath){

        StringBuffer rules = new StringBuffer();
        
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            if(name.equals(PC) || name.equals(SC) || name.equals(TC)|| name.equals(QC) || name.equals(IC)){
                Node lastVariable = LDMLUtilities.getNode(node, LAST_VARIABLE , null, null);
                if(lastVariable!=null){
                    if(DEBUG)rules.append(" ");
                     rules.append(collationMap.get(lastVariable.getNodeName()));
                }else{
                    String data = getData(node,name);
                    rules.append(data);
                }
            }else if(name.equals(P) || name.equals(S) || name.equals(T)|| name.equals(Q) || name.equals(I)){
                Node lastVariable = LDMLUtilities.getNode(node, LAST_VARIABLE , null, null);
                if(lastVariable!=null){
                    if(DEBUG) rules.append(" ");
                    rules.append(collationMap.get(lastVariable.getNodeName()));
                }else{

                    String data = getData(node, name);
                    rules.append(data);
                }
            }else if(name.equals(X)){
                    rules.append(parseExtension(node));
            }else if(name.equals(RESET)){
                rules.append(parseReset(node));
            }else{
            
                System.err.println("Encountered unknown element: "+name);
                System.exit(-1);
            }
        }
        return rules;
    }
    private static final UnicodeSet needsQuoting = new UnicodeSet("[[:whitespace:][:c:][:z:][[:ascii:]-[a-zA-Z0-9]]]");
    private static StringBuffer quoteOperandBuffer = new StringBuffer(); // faster
    private static final String quoteOperand(String s) {
        
        s = Normalizer.normalize(s, Normalizer.NFC);
        quoteOperandBuffer.setLength(0);
        boolean noQuotes = true;
        boolean inQuote = false;
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (!needsQuoting.contains(cp)) {
                if (inQuote) {
                    quoteOperandBuffer.append('\'');
                    inQuote = false;
                }
                quoteOperandBuffer.append(UTF16.valueOf(cp));
            } else {
                noQuotes = false;
                if (cp == '\'') {
                    quoteOperandBuffer.append("''");
                } else {
                    if (!inQuote) {
                        quoteOperandBuffer.append('\'');
                        inQuote = true;
                    }
                    if (cp > 0xFFFF) {
                        quoteOperandBuffer.append("\\U").append(Utility.hex(cp,8));
                    } else if (cp <= 0x20 || cp > 0x7E) {
                        quoteOperandBuffer.append("\\u").append(Utility.hex(cp));
                    } else {
                        quoteOperandBuffer.append(UTF16.valueOf(cp));
                    }
                }
            }
        }
        if (inQuote) {
            quoteOperandBuffer.append('\'');
        }
        if (noQuotes) return s; // faster
        return quoteOperandBuffer.toString();
    }

    private static final String CP = "cp";
    private static final String HEX = "hex";
    private String getData(Node root, String strength){
        StringBuffer data = new StringBuffer();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()==Node.ELEMENT_NODE){
                String name = node.getNodeName();
                if(name.equals(CP)){
                    String hex = LDMLUtilities.getAttributeValue(node, HEX);
                    if(DEBUG)data.append(" ");
                    data.append(getStrengthSymbol(strength));
                    if(DEBUG)data.append(" ");
                    String cp = UTF16.valueOf(Integer.parseInt(hex, 16));
                    data.append(quoteOperand(cp));
                }
            }
            if(node.getNodeType()==Node.TEXT_NODE){
                String val = node.getNodeValue();
                if(val!=null){
                    if(strength.equals(PC) || strength.equals(SC) || strength.equals(TC)|| 
                            strength.equals(QC) ||strength.equals(IC)){
                        data.append(getExpandedRules(val, strength));
                    }else{
                        if(DEBUG)data.append(" ");
                        data.append(getStrengthSymbol(strength));
                        if(DEBUG)data.append(" ");
                        data.append(quoteOperand(val));
                    }
                }
            }
        }
        return data.toString();
    }
    private String getStrengthSymbol(String name){
        if(name.equals(PC) || name.equals(P)){
            return "<"; 
        }else if (name.equals(SC)||name.equals(S)){
            return "<<";
        }else if(name.equals(TC)|| name.equals(T)){
            return "<<<";
        }else if(name.equals(QC) || name.equals(Q)){
            return "<<<<";
        }else if(name.equals(IC) || name.equals(I)){
            return "=";
        }else{
            System.err.println("Encountered strength: "+name);
            System.exit(-1);
        }
        return null;
    }
    private static final String PRIMARY = "primary";
    private static final String SECONDARY = "secondary";
    private static final String TERTIARY = "tertiary";
    private static final String QUARTERNARY  = "quarternary";
    private static final String IDENTICAL  = "identical";
    private String getStrength(String name){
        if(name.equals(PRIMARY)){
            return "1"; 
        }else if (name.equals(SECONDARY)){
            return "2";
        }else if( name.equals(TERTIARY)){
            return "3";
        }else if( name.equals(QUARTERNARY)){
            return "4";
        }else if(name.equals(IDENTICAL)){
            return "5";
       
        }else{
            System.err.println("Encountered strength: "+name);
            System.exit(-1);
        }
        return null;
    }
    private static final String BEFORE = "before";
    
    private StringBuffer parseReset(Node root){
        /* variableTop   at      & x= [last variable]              <reset>x</reset><i><last_variable/></i>
         *               after   & x  < [last variable]            <reset>x</reset><p><last_variable/></p>
         *               before  & [before 1] x< [last variable]   <reset before="primary">x</reset><p><last_variable/></p>
         */
        /*
         * & [first tertiary ignorable] << à    <reset><first_tertiary_ignorable/></reset><s>à</s>
         */
        StringBuffer ret = new StringBuffer();
        
        if(DEBUG) ret.append(" ");
        ret.append("&");
        if(DEBUG) ret.append(" ");
        
        String val = LDMLUtilities.getAttributeValue(root, BEFORE);
        if(val!=null){
            if(DEBUG) ret.append(" ");
            ret.append("[before ");
            ret.append(getStrength(val));
            ret.append("]");
        }
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            short type = node.getNodeType();
            if(type==Node.ELEMENT_NODE){
 
                String key = node.getNodeName();
                if(DEBUG) ret.append(" ");
                ret.append(collationMap.get(key));
            }
            if(type==Node.TEXT_NODE){
                ret.append(quoteOperand(node.getNodeValue()));
            }
        }
        return ret;
    }
    private StringBuffer getExpandedRules(String data, String name){
        UCharacterIterator iter = UCharacterIterator.getInstance(data);
        StringBuffer ret = new StringBuffer();
        String strengthSymbol = getStrengthSymbol(name);
        int ch;
        while((ch = iter.nextCodePoint() )!= UCharacterIterator.DONE){
            if(DEBUG) ret.append(" ");
            ret.append(strengthSymbol);
            if(DEBUG) ret.append(" ");
            ret.append(quoteOperand(UTF16.valueOf(ch)));
        }
        return ret;
    }
    private static final String CONTEXT = "context";
    private static final String EXTEND =  "extend";
    private StringBuffer parseExtension(Node root){
        /*  
         * strength context string extension
         * <strength>  <context> | <string> / <extension>
         * < a | [last variable]      <x><context>a</context><p><last_variable/></p></x>
         * < [last variable]    / a   <x><p><last_variable/></p><extend>a</extend></x>
         * << k / h                   <x><s>k</s> <extend>h</extend></x>
         * << d | a                   <x><context>d</context><s>a</s></x>
         * =  e | a                   <x><context>e</context><i>a</i></x>
         * =  f | a                   <x><context>f</context><i>a</i></x>
         */
         StringBuffer rules = new StringBuffer();
         Node contextNode  =  null;
         Node extendNode   =  null;
         Node strengthNode =  null;
         
         
         String strength = null;
         String string = null;
         String context  = null;
         String extend = null;
         
         for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
             if(node.getNodeType()!=Node.ELEMENT_NODE){
                 continue;
             }
             String name = node.getNodeName();
             if(name.equals(CONTEXT)){
                 contextNode = node;
             }else if(name.equals(P) || name.equals(S) || name.equals(T)|| name.equals(I)){
                 strengthNode = node;
             }else if(name.equals(EXTEND)){
                 extendNode = node;   
             }else{
                 System.err.println("Encountered unknown element: "+name);
                 System.exit(-1);
             }
         }
         if(contextNode != null){
            context = LDMLUtilities.getNodeValue(contextNode);
         }
         if(strengthNode!=null){
             Node lastVariable = LDMLUtilities.getNode(strengthNode, LAST_VARIABLE , null, null);
             if(lastVariable!=null){
                 string = (String)collationMap.get(lastVariable.getNodeName());
             }else{
                 strength = getStrengthSymbol(strengthNode.getNodeName());
                 string = LDMLUtilities.getNodeValue(strengthNode);
             }
         }
         if(extendNode!=null){
             extend = LDMLUtilities.getNodeValue(extendNode);
         }
         if(DEBUG) rules.append(" ");
         rules.append(strength);
         if(DEBUG) rules.append(" ");
         if(context!=null){
             rules.append(quoteOperand(context));
             if(DEBUG) rules.append(" ");
             rules.append("|");
             if(DEBUG) rules.append(" ");
         }
         rules.append(string);
         
         if(extend!=null){
             if(DEBUG) rules.append(" ");
             rules.append("/");
             if(DEBUG) rules.append(" ");
             rules.append(quoteOperand(extend));
         }
         return rules;
    }
    private void writeResource(ICUResourceWriter.Resource set, String sourceFileName){
        try {
            String outputFileName = null;
            outputFileName = destDir+File.separator+set.name+".txt";
            
            FileOutputStream file = new FileOutputStream(outputFileName);
            BufferedOutputStream writer = new BufferedOutputStream(file);

            //TODO: fix me
            writeHeader(writer,sourceFileName);
            //set.sort();
            ICUResourceWriter.Resource current = set;
            while(current!=null){
                current.sort();
                current = current.next;
            }
            //Now start writing the resource;
            current = set;
            while(current!=null){
                current.write(writer, 0, false);
                current = current.next;
            }
            writer.flush();
            writer.close();
        } catch (Exception ie) {
            System.err.println("ERROR :" + ie.toString());
            return;
        }
    }
    
 
    private static final String DRAFT = "draft";
    private boolean isDraft(Node node){
        String val = LDMLUtilities.getAttributeValue(node, DRAFT);
        if(val.equals("true")){
        	return true;
        }
        return false;
    }
    private boolean isAlternate(Node node){
        NamedNodeMap attributes = node.getAttributes();
        Node attr = attributes.getNamedItem(ALT);
        if(attr!=null){
            return true;
        }
        return false;
    }
    private void writeLine(OutputStream writer, String line) {
        try {
            byte[] bytes = line.getBytes(CHARSET);
            writer.write(bytes, 0, bytes.length);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }
    private void writeHeader(OutputStream writer, String fileName){
        writeBOM(writer);
        Calendar c = Calendar.getInstance();
        StringBuffer buffer =new StringBuffer();
        buffer.append("// ***************************************************************************" + LINESEP);
        buffer.append("// *" + LINESEP);
        buffer.append("// * Copyright (C) "+c.get(Calendar.YEAR) +" International Business Machines" + LINESEP);
        buffer.append("// * Corporation and others.  All Rights Reserved."+LINESEP);
        buffer.append("// * Tool: com.ibm.icu.dev.tool.cldr.LDML2ICUConverter.java" + LINESEP);
        buffer.append("// * Date & Time: " + c.get(Calendar.YEAR) + "/" + (c.get(Calendar.MONTH)+1) + "/" + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + COLON + c.get(Calendar.MINUTE)+ LINESEP);
        buffer.append("// * Source File: " + fileName + LINESEP);
        buffer.append("// *" + LINESEP);                    
        buffer.append("// ***************************************************************************" + LINESEP);
        writeLine(writer, buffer.toString());

    }
    
    private  void writeBOM(OutputStream buffer) {
        try {
            byte[] bytes = BOM.getBytes(CHARSET);
            buffer.write(bytes, 0, bytes.length);
        } catch(Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }
}
