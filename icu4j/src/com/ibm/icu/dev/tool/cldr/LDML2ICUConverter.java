
package com.ibm.icu.dev.tool.cldr;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

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
    private static final String OPENBRACE       = "{";
    private static final String CLOSEBRACE      = "}";
    private static final String COLON           = ":";
    private static final String COMMA           = ",";
    private static final String QUOTE           = "\"";
    private static final String COMMENTSTART    = "/**";
    private static final String COMMENTEND      = " */";
    private static final String COMMENTMIDDLE   = " * ";
    private static final String SPACE           = " ";
    private static final String INDENT          = "    ";
    private static final String EMPTY           = "";
    private static final String STRINGS         = "string";
    private static final String BIN             = "bin";
    private static final String INTS            = "int";
    private static final String TABLE           = "table";
    private static final String IMPORT          = "import";
    private static final String ALIAS           = "alias";
    private static final String INTVECTOR       = "intvector";
    private static final String ARRAYS          = "array";
    
    private Document fullyResolved = null;
    
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
             * try{ Document doc = LDMLUtilities.getFullyResolvedLDML(sourceDir,
             * args[i], false); OutputStreamWriter writer = new
             * OutputStreamWriter(new
             * FileOutputStream("./"+File.separator+args[i]+"_debug.xml"),"UTF-8");
             * LDMLUtilities.printDOMTree(doc,new PrintWriter(writer));
             * writer.flush(); }catch( IOException e){ //throw the exception
             * away .. this is for debugging }
             */
            //TODO: uncomment 
            //fullyResolved =  LDMLUtilities.getFullyResolvedLDML(sourceDir, args[i], false);
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
             Resource res =  parseBundle(doc);
             
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
    
    private Resource setNext(Resource current, ResourceTable table, Resource toSet){
        if(current == null){
            current = table.first = toSet;
        }else{
            current.next = toSet;
        }
        return current.next;
    }
    private Resource parseBundle(Node root){
        ResourceTable table = null;
        Resource current = null;
        StringBuffer xpath = new StringBuffer();
        xpath.append("//ldml");
        int savedLength = xpath.length();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
             	continue;
            }
    		String name = node.getNodeName();
            Resource res = null;
            if(name.equals(IDENTITY)){
            	table = (ResourceTable) parseIdentity(node);
                Resource now = table.first;
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
                //TODO
                // res = parseNumbers(node, xpath);
            }else if(name.equals(COLLATIONS)){
                //TODO
                // res = parseCollations(node, xpath);
            }else if(name.equals(POSIX)){
                //TODO
                // res = parsePosix(nde, xpath);
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
    private Resource findLast(Resource res){
        Resource current = res;
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
    private Resource parseAliasResource(Node node){
        if(node!=null){
            ResourceAlias alias = new ResourceAlias();
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
    private Resource parseIdentity(Node root){
        String localeID="", temp;
        ResourceTable table = new ResourceTable();
        Resource res = null;
        Resource current = null;
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            if(name.equals(VERSION)){
                ResourceString str = new ResourceString();
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
    private Resource parseLocaleDisplayNames(Node root, StringBuffer xpath){
        Resource first = null;
        Resource current = null;
        int savedLength = xpath.length();
        getXPath(root,xpath);
        int oldLength = xpath.length();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            Resource res = null;
            if(name.equals(LANGUAGES)|| name.equals(SCRIPTS) || name.equals(TERRITORIES) || name.equals(KEYS) || name.equals(VARIANTS)){
            	res = parseList(node, xpath);
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
    
    private Resource parseDisplayTypes(Node root){
        StringBuffer xpath = new StringBuffer();
        xpath.append("//ldml/localeDisplayNames/types/type[@key='");
        int savedLength = xpath.length();
        ResourceTable table = new ResourceTable();
        table.name = TYPES;
        ResourceTable current = null;
        for(int i=0; i<registeredKeys.length; i++){
            xpath.append(registeredKeys[i]);
            xpath.append("']");
        	NodeList list = LDMLUtilities.getNodeList(root.getOwnerDocument(), xpath.toString());
            if(list.getLength()!=0){
                ResourceTable subTable = new ResourceTable();
                subTable.name = registeredKeys[i];
                if(i==0){
                    table.first = current = subTable;
                }else{
                    current.next = subTable;
                    current = (ResourceTable)current.next;
                }
                ResourceString currentString = null;
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
                    
                    ResourceString string = new ResourceString();
                    string.name = type;
                    string.val  = value;
                    if(j==0){
                        subTable.first = currentString = string;
                    }else{
                        currentString.next = string;
                        currentString = (ResourceString)currentString.next;
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
    private Resource parseList(Node root, StringBuffer xpath){
    	ResourceTable table = new ResourceTable();
        table.name=root.getNodeName();
        Resource current = null;
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
            	current = table.first = new ResourceString();
            }else{
            	current.next = new ResourceString();
                current = current.next;
            }
            current.name = LDMLUtilities.getAttributeValue(node, TYPE);

            ((ResourceString)current).val  = LDMLUtilities.getNodeValue(node);
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        return table;
    }
    private static final String EXEMPLAR_CHARACTERS ="exemplarCharacters";
    private static final String MAPPING ="mapping";
    
    private Resource parseCharacters(Node root, StringBuffer xpath){
        Resource current = null, first=null;
        int savedLength = xpath.length();
        getXPath(root,xpath);
        int oldLength = xpath.length();
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            getXPath(node, xpath);
            String name = node.getNodeName();
            Resource res = null;
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
    private Resource parseStringResource(Node node){
        ResourceString str = new ResourceString();
        str.val = LDMLUtilities.getNodeValue(node);
        str.name = node.getNodeName();
        return str;
    }
    private static final String QS  = "quotationStart";
    private static final String QE  = "quotationEnd";
    private static final String AQS = "alternateQuotationStart";
    private static final String AQE = "alternateQuotationEnd";
    private Resource parseDelimiters(Node root, StringBuffer xpath){
        ResourceTable table = new ResourceTable();
        table.name = root.getNodeName();
        getXPath(root,xpath);
        Resource res = parseDelimiter(root, xpath);
        if(res!=null){
            table.first = res;
            return table;
        }
        return null;
    }
    private Resource parseDelimiter(Node root, StringBuffer xpath){
        Resource current = null,first=null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            getXPath(node, xpath);
            String name = node.getNodeName();
            Resource res = null;
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
    
    private Resource parsePaperSize(Node root, StringBuffer xpath){
        ResourceIntVector vector = new ResourceIntVector();
        vector.name = root.getNodeName();
        Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            Resource res = null;
            getXPath(node, xpath);
            // here we assume that the DTD enforces the correct order
            // of elements
            if(name.equals(HEIGHT)||name.equals(WIDTH)){
                ResourceInt resint = new ResourceInt();
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
                    current = vector.first = (ResourceInt) res;
                }else{
                    current.next = res;
                    current = current.next;
                }
            }
            xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(oldLength, xpath.length());
        return vector;
    }
    private Resource parseMeasurement (Node root, StringBuffer xpath){
        Resource current = null,first=null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        for(Node node = root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            getXPath(node, xpath);
            String name = node.getNodeName();
            Resource res = null;
            if(name.equals(MS)){
                String altVal = LDMLUtilities.getAttributeValue(node, ALT);
                // the alt atrribute is set .. so ignore the resource
                if(altVal!=null){
                    continue;
                }
                ResourceInt resint = new ResourceInt();
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
    
    private Resource parseDates(Node root, StringBuffer xpath){
        Resource first = null;
        Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            Resource res = null;
            if(name.equals(ALIAS)){
                //dont compute xpath
                res = parseAliasResource(node);
            }else if(name.equals(DEFAULT) ){
                ResourceString str = new ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node, TYPE);
                res = str;
            }else if(name.equals(LPC)){
                getXPath(node, xpath);
                if(xpath.indexOf(ALT)<0){
                    ResourceString str = new ResourceString();
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
    private Resource parseCalendars(Node root, StringBuffer xpath){
        ResourceTable table = new ResourceTable();
        Resource current = null;
        table.name = root.getNodeName();
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name =table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ResourceString str = new ResourceString();
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
    private Resource parseTimeZoneNames(Node root, StringBuffer xpath){
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
    
    private Resource parseCalendar(Node root, StringBuffer xpath){
        ResourceTable table = new ResourceTable();
        Resource current = null;
        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
        if(xpath.indexOf(ALT)>-1){
            return null;
        }
        boolean writtenAmPm = false;
        table.name = LDMLUtilities.getAttributeValue(root,TYPE);
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = node.getNodeName();
            Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name =table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ResourceString str = new ResourceString();
                str.name = name;
                str.val = LDMLUtilities.getAttributeValue(node,TYPE);
                res = str;
            }else if(name.equals(MONTHS)|| name.equals(DAYS)){
                res = parseMonthsAndDays(node, xpath);
            }else if(name.equals(WEEK)){
                Resource temp = parseWeek(node, xpath);
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
    private Resource parseMonthsAndDays(Node root, StringBuffer xpath){
        ResourceTable table = new ResourceTable();
        Resource current = null;
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
            Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name=table.name;
                return res;
            }else if(name.equals(DEFAULT)){
                ResourceString str = new ResourceString();
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
    private Resource parseContext(Node root, StringBuffer xpath ){
        ResourceTable table = new ResourceTable();
        Resource current = null;
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
            Resource res = null;
            if(name.equals(ALIAS)){
                res = parseAliasResource(node);
                res.name = table.name;
                return res; // an alias if for the resource
            }else if(name.equals(DEFAULT)){
                ResourceString str = new ResourceString();
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
    
    private Resource parseWidth(Node root, String resName, StringBuffer xpath){
        ResourceArray array = new ResourceArray();
        Resource current = null;
        array.name = LDMLUtilities.getAttributeValue(root,TYPE);

        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
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
               ResourceString res = new ResourceString();
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
    private Resource parseWeek(Node root, StringBuffer xpath){
        Resource dte = parseDTE(root, xpath);
        Resource wkend = parseWeekend(root, xpath);
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
    private Resource parseWeekend(Node root, StringBuffer xpath){
        Node wkendStart = LDMLUtilities.getNode(root, WENDSTART, fullyResolved, xpath.toString());
        Node wkendEnd = LDMLUtilities.getNode(root, WENDEND, fullyResolved, xpath.toString());
        ResourceIntVector wkend = null;
        
        if(wkendStart!=null && wkendEnd!=null){
            try{
                wkend =  new ResourceIntVector();
                wkend.name = WEEKEND;
                ResourceInt startday = new ResourceInt();
                startday.val = getDayIndexAsString(LDMLUtilities.getAttributeValue(wkendStart, DAY));
                ResourceInt starttime = new ResourceInt();
                starttime.val = Integer.toString(getMillis(LDMLUtilities.getAttributeValue(wkendStart, TIME)));
                ResourceInt endday = new ResourceInt();
                endday.val = getDayIndexAsString(LDMLUtilities.getAttributeValue(wkendEnd, DAY));
                ResourceInt endtime = new ResourceInt();
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
    private Resource parseDTE(Node root, StringBuffer xpath){
        Node minDays = LDMLUtilities.getNode(root, MINDAYS, fullyResolved, xpath.toString());
        Node firstDay = LDMLUtilities.getNode(root, FIRSTDAY, fullyResolved, xpath.toString());
        ResourceIntVector dte = null;
        if(minDays!=null && firstDay!=null){
            dte =  new ResourceIntVector();
            ResourceInt int1 = new ResourceInt();
            int1.val = LDMLUtilities.getAttributeValue(minDays, COUNT);
            ResourceInt int2 = new ResourceInt();
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
    private Resource parseEras(Node root, StringBuffer xpath){
        return null;
    }
    private static final String AM_PM_MARKERS = "AmPmMarkers";
    private Resource parseAmPm(Node root, StringBuffer xpath){
        Node parent =root.getParentNode();
        Node amNode = LDMLUtilities.getNode(parent, AM, fullyResolved, xpath.toString());
        Node pmNode = LDMLUtilities.getNode(parent, PM, fullyResolved, xpath.toString());
        ResourceArray arr = null;
        if(amNode!=null && pmNode!= null){
            arr = new ResourceArray();
            arr.name = AM_PM_MARKERS;
            ResourceString am = new ResourceString();
            ResourceString pm = new ResourceString();
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
    private void writeResource(Resource set, String sourceFileName){
        try {
            String outputFileName = null;
            outputFileName = destDir+File.separator+set.name+".txt";
            
            FileOutputStream file = new FileOutputStream(outputFileName);
            BufferedOutputStream writer = new BufferedOutputStream(file);

            //TODO: fix me
            writeHeader(writer,sourceFileName);
            
            //Now start writing the resource;
            Resource current = set;
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
    private class Resource{
        String[] note = new String[20];
        int noteLen = 0;
        String translate;
        String comment;
        String name;
        Resource next;
        public String escapeSyntaxChars(String val){
            // escape the embedded quotes
            char[] str = val.toCharArray();
            StringBuffer result = new StringBuffer();
            for(int i=0; i<str.length; i++){
                switch (str[i]){
                    case '\u0022':
                        result.append('\\'); //append backslash
                    default:
                        result.append(str[i]);
                }      
            }
            return result.toString();
        }
        public void write(OutputStream writer, int numIndent, boolean bare){
            while(next!=null){
                next.write(writer, numIndent+1, false);
            }
        }
        public void writeIndent(OutputStream writer, int numIndent){
            for(int i=0; i< numIndent; i++){
                write(writer,INDENT);
            }
        }
        public void write(OutputStream writer, String value){
            try {
                byte[] bytes = value.getBytes(CHARSET);
                writer.write(bytes, 0, bytes.length);
            } catch(Exception e) {
                System.err.println(e);
                System.exit(1);
            }
        }
        public void writeComments(OutputStream writer, int numIndent){
            if(comment!=null || translate != null || noteLen > 0){
                // print the start of the comment
                writeIndent(writer, numIndent);
                write(writer, COMMENTSTART+LINESEP);
                
                // print comment if any
                if(comment!=null){
                    writeIndent(writer, numIndent);
                    write(writer, COMMENTMIDDLE);
                    write(writer, comment);
                    write(writer, LINESEP);
                }
                  
                // terminate the comment
                writeIndent(writer, numIndent);
                write(writer, COMMENTEND+LINESEP);
            }          
        }
    }

    private class ResourceString extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                
                write(writer,QUOTE+escapeSyntaxChars(val)+QUOTE); 
            }else{
                write(writer, name + OPENBRACE + QUOTE + escapeSyntaxChars(val) + QUOTE+ CLOSEBRACE + LINESEP);
            }
        }
    }
    private class ResourceAlias extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+ALIAS+ OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    }
    private class ResourceInt extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+INTS+ OPENBRACE + val +CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    }
    private class ResourceBinary extends Resource{
        String internal;
        String external;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(internal==null){
                String line = ((name==null) ? EMPTY : name)+COLON+IMPORT+ OPENBRACE+QUOTE+external+QUOTE+CLOSEBRACE + ((bare==true) ?  EMPTY : LINESEP);
                write(writer, line);
            }else{
                String line = ((name==null) ? EMPTY : name)+COLON+BIN+ OPENBRACE+internal+CLOSEBRACE+ ((bare==true) ?  EMPTY : LINESEP);
                write(writer,line);
            }
            
        }
    }
    private class ResourceIntVector extends Resource{
        ResourceInt first;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            write(writer, name+COLON+INTVECTOR+OPENBRACE+LINESEP);
            numIndent++;
            ResourceInt current = (ResourceInt) first;
            while(current != null){
                //current.write(writer, numIndent, true);
                writeIndent(writer, numIndent);
                write(writer, current.val);
                write(writer, COMMA+LINESEP);
                current = (ResourceInt) current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
    }
    private class ResourceTable extends Resource{
        Resource first;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            write(writer, name+OPENBRACE+LINESEP);
            numIndent++;
            Resource current = first;
            while(current != null){
                current.write(writer, numIndent, false);
                current = current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
    }
    private class ResourceArray extends Resource{
        Resource first;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            write(writer, name+COLON+ARRAYS+OPENBRACE+LINESEP);
            numIndent++;
            Resource current = first;
            while(current != null){
                current.write(writer, numIndent, true);
                write(writer, COMMA+LINESEP);
                current = current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
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
    private static final String ALTERNATE= "alt";
    private boolean isAlternate(Node node){
        NamedNodeMap attributes = node.getAttributes();
        Node attr = attributes.getNamedItem(ALTERNATE);
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
        buffer.append("// * Tool: org.unicode.ldml.LDML2ICUConverter.java" + LINESEP);
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
