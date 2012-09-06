/*
 ******************************************************************************
 * Copyright (C) 2003-2010, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package com.ibm.icu.dev.tool.localeconverter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Date;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.icu.dev.tool.UOption;

public final class XLIFF2ICUConverter {
    
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int SOURCEDIR = 2;
    private static final int DESTDIR = 3;
    private static final int TARGETONLY = 4;
    private static final int SOURCEONLY = 5;
    private static final int MAKE_SOURCE_ROOT = 6;
    private static final int XLIFF_1_0 = 7;
       
    private static final UOption[] options = new UOption[] {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR(),
        UOption.DESTDIR(),
        UOption.create("target-only", 't', UOption.OPTIONAL_ARG),
        UOption.create("source-only", 'c', UOption.OPTIONAL_ARG),
        UOption.create("make-source-root", 'r', UOption.NO_ARG),
        UOption.create("xliff-1.0", 'x', UOption.NO_ARG)
    };
    
    private static final int ARRAY_RESOURCE     = 0;
    private static final int ALIAS_RESOURCE     = 1;
    private static final int BINARY_RESOURCE    = 2;
    private static final int INTEGER_RESOURCE   = 3;
    private static final int INTVECTOR_RESOURCE = 4;
    private static final int TABLE_RESOURCE     = 5;
    
    private static final String NEW_RESOURCES[] = {
        "x-icu-array",
        "x-icu-alias",
        "x-icu-binary",
        "x-icu-integer",
        "x-icu-intvector",
        "x-icu-table"
    };
    
    private static final String OLD_RESOURCES[] = {
        "array",
        "alias",
        "bin",
        "int",
        "intvector",
        "table"
    };
    
    private String resources[];
    
    private static final String ROOT            = "root";
    private static final String RESTYPE         = "restype";
    private static final String RESNAME         = "resname";
    //private static final String YES             = "yes";
    //private static final String NO              = "no";
    private static final String TRANSLATE       = "translate";
    //private static final String BODY            = "body";
    private static final String GROUPS          = "group";
    private static final String FILES           = "file";
    private static final String TRANSUNIT       = "trans-unit";
    private static final String BINUNIT         = "bin-unit";
    private static final String BINSOURCE       = "bin-source";
    //private static final String TS              = "ts";
    //private static final String ORIGINAL        = "original";
    private static final String SOURCELANGUAGE  = "source-language";
    private static final String TARGETLANGUAGE  = "target-language";
    private static final String TARGET          = "target";
    private static final String SOURCE          = "source";
    private static final String NOTE            = "note";
    private static final String XMLLANG         = "xml:lang";
    private static final String FILE            = "file";
    private static final String INTVECTOR       = "intvector";
    private static final String ARRAYS          = "array";
    private static final String STRINGS         = "string";
    private static final String BIN             = "bin";
    private static final String INTS            = "int";
    private static final String TABLE           = "table";
    private static final String IMPORT          = "import";
    private static final String HREF            = "href";
    private static final String EXTERNALFILE    = "external-file";
    private static final String INTERNALFILE    = "internal-file";
    private static final String ALTTRANS        = "alt-trans";
    private static final String CRC             = "crc";
    private static final String ALIAS           = "alias";
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
    private static final String TAG             = " * @";
    private static final String COMMENTMIDDLE   = " * ";
    private static final String SPACE           = " ";
    private static final String INDENT          = "    ";
    private static final String EMPTY           = "";
    private static final String ID              = "id";
    
    public static void main(String[] args) {
        XLIFF2ICUConverter cnv = new XLIFF2ICUConverter();
        cnv.processArgs(args);
    }
    private String    sourceDir      = null;
    //private String    fileName       = null;
    private String    destDir        = null;
    private boolean   targetOnly     = false;
    private String    targetFileName = null; 
    private boolean   makeSourceRoot = false;
    private String    sourceFileName = null;
    private boolean   sourceOnly     = false;
    private boolean   xliff10        = false;
    
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
        
        if(options[TARGETONLY].doesOccur){
            targetOnly = true;
            targetFileName = options[TARGETONLY].value;
        }
        
        if(options[SOURCEONLY].doesOccur){
            sourceOnly = true;
            sourceFileName = options[SOURCEONLY].value;
        }
        
        if(options[MAKE_SOURCE_ROOT].doesOccur){
            makeSourceRoot = true;
        }
        
        if(options[XLIFF_1_0].doesOccur) {
            xliff10 = true;
        }
        
        if(destDir==null){
            destDir = ".";
        }
        
        if(sourceOnly == true && targetOnly == true){
            System.err.println("--source-only and --target-only are specified. Please check the arguments and try again.");
            usage();
        }
        
        for (int i = 0; i < remainingArgc; i++) {
            //int lastIndex = args[i].lastIndexOf(File.separator, args[i].length()) + 1; /* add 1 to skip past the separator */
            //fileName = args[i].substring(lastIndex, args[i].length());
            String xmlfileName = getFullPath(false,args[i]);
            System.out.println("Processing file: "+xmlfileName);
            createRB(xmlfileName);
        }
    }
    
    private void usage() {
        System.out.println("\nUsage: XLIFF2ICUConverter [OPTIONS] [FILES]\n\n"+
            "This program is used to convert XLIFF files to ICU ResourceBundle TXT files.\n"+
            "Please refer to the following options. Options are not case sensitive.\n"+
            "Options:\n"+
            "-s or --sourcedir          source directory for files followed by path, default is current directory.\n" +
            "-d or --destdir            destination directory, followed by the path, default is current directory.\n" +
            "-h or -? or --help         this usage text.\n"+
            "-t or --target-only        only generate the target language txt file, followed by optional output file name.\n" +
            "                           Cannot be used in conjunction with --source-only.\n"+
            "-c or --source-only        only generate the source language bundle followed by optional output file name.\n"+
            "                           Cannot be used in conjunction with --target-only.\n"+
            "-r or --make-source-root   produce root bundle from source elements.\n" +
            "-x or --xliff-1.0          source file is XLIFF 1.0" +
            "example: com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter -t <optional argument> -s xxx -d yyy myResources.xlf");
        System.exit(-1);
    }
    
    private String getFullPath(boolean fileType, String fName){
        String str;
        int lastIndex1 = fName.lastIndexOf(File.separator, fName.length()) + 1; /*add 1 to skip past the separator*/
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
                fName = fName.trim() + ".xlf";
            }else{
                if(!fName.substring(lastIndex2).equalsIgnoreCase(".xml") && fName.substring(lastIndex2).equalsIgnoreCase(".xlf")){
                    fName = fName.substring(lastIndex1,lastIndex2) + ".xlf";
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
    private static String filenameToURL(String filename){
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
    private boolean isXmlLang (String lang){

        int suffix;
        char c;
        
        if (lang.length () < 2){
            return false;
        }

        c = lang.charAt(1);
        if (c == '-') {        
            c = lang.charAt(0);
            if (!(c == 'i' || c == 'I' || c == 'x' || c == 'X')){
                return false;
            }
            suffix = 1;
        } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            c = lang.charAt(0);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))){
                return false;
            }
            suffix = 2;
        } else{
            return false;
        }
        while (suffix < lang.length ()) {
            c = lang.charAt(suffix);
            if (c != '-'){
                break;
            }
            while (++suffix < lang.length ()) {
                c = lang.charAt(suffix);
                if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))){
                    break;
                }
            }
        }
        return  ((lang.length() == suffix) && (c != '-'));
    }
    
    private void createRB(String xmlfileName) {
       
        String urls = filenameToURL(xmlfileName);
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);
        Document doc = null;
        
        if (xliff10) {
            dfactory.setValidating(true);
            resources = OLD_RESOURCES;
        } else {
            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema();
                
                dfactory.setSchema(schema);
            } catch (SAXException e) {
                System.err.println("Can't create the schema...");
                System.exit(-1);
            } catch (UnsupportedOperationException e) {
                System.err.println("ERROR:\tOne of the schema operations is not supported with this JVM.");
                System.err.println("\tIf you are using GNU Java, you should try using the latest Sun JVM.");
                System.err.println("\n*Here is the stack trace:");
                e.printStackTrace();
                System.exit(-1);
            }
            
            resources = NEW_RESOURCES;
        }
        
        ErrorHandler nullHandler = new ErrorHandler() {
            public void warning(SAXParseException e) throws SAXException {
                            
            }
            public void error(SAXParseException e) throws SAXException {
                System.err.println("The XLIFF document is invalid, please check it first: ");
                System.err.println("Line "+e.getLineNumber()+", Column "+e.getColumnNumber());
                System.err.println("Error: " + e.getMessage());
                System.exit(-1);
            }
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        };
        
        try {
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            docBuilder.setErrorHandler(nullHandler);
            doc = docBuilder.parse(new InputSource(urls));
            
            NodeList nlist = doc.getElementsByTagName(FILES);
            if(nlist.getLength()>1){
                throw new RuntimeException("Multiple <file> elements in the XLIFF file not supported.");
            }
                        
            // get the value of source-language attribute
            String sourceLang = getLanguageName(doc, SOURCELANGUAGE);
            // get the value of target-language attribute
            String targetLang = getLanguageName(doc, TARGETLANGUAGE);
            
            // get the list of <source> elements
            NodeList sourceList = doc.getElementsByTagName(SOURCE);
            // get the list of target elements
            NodeList targetList = doc.getElementsByTagName(TARGET);
            
            // check if the xliff file has source elements in multiple languages
            // the source-language value should be the same as xml:lang values
            // of all the source elements.
            String xmlSrcLang = checkLangAttribute(sourceList, sourceLang);
            
            // check if the xliff file has target elements in multiple languages
            // the target-language value should be the same as xml:lang values
            // of all the target elements.
            String xmlTargetLang = checkLangAttribute(targetList, targetLang);
            
            // Create the Resource linked list which will hold the
            // source and target bundles after parsing
            Resource[] set = new Resource[2];
            set[0] = new ResourceTable();
            set[1] = new ResourceTable();
            
            // lenient extraction of source language
            if(makeSourceRoot == true){ 
                set[0].name = ROOT;
            }else if(sourceLang!=null){
                set[0].name = sourceLang.replace('-','_');
            }else{
                if(xmlSrcLang != null){
                    set[0].name = xmlSrcLang.replace('-','_');
                }else{
                    System.err.println("ERROR: Could not figure out the source language of the file. Please check the XLIFF file.");
                    System.exit(-1);
                }
            }
            
            // lenient extraction of the target language
            if(targetLang!=null){
                set[1].name = targetLang.replace('-','_');
            }else{
                if(xmlTargetLang!=null){
                    set[1].name = xmlTargetLang.replace('-','_');
                }else{
                    System.err.println("WARNING: Could not figure out the target language of the file. Producing source bundle only.");
                }
            }
   
            
            // check if any <alt-trans> elements are present
            NodeList altTrans = doc.getElementsByTagName(ALTTRANS);
            if(altTrans.getLength()>0){
                System.err.println("WARNING: <alt-trans> elements in found. Ignoring all <alt-trans> elements.");
            }
            
            // get all the group elements
            NodeList list = doc.getElementsByTagName(GROUPS);
            
            // process the first group element. The first group element is 
            // the base table that must be parsed recursively
            parseTable(list.item(0), set);
            
            // write out the bundle
            writeResource(set, xmlfileName);
         }
        catch (Throwable se) {
            System.err.println("ERROR: " + se.toString());
            System.exit(1);
        }        
    }
    
    private void writeResource(Resource[] set, String xmlfileName){
        if(targetOnly==false){
            writeResource(set[0], xmlfileName, sourceFileName);
        }
        if(sourceOnly == false){
            if(targetOnly==true && set[1].name == null){
                throw new RuntimeException("The "+ xmlfileName +" does not contain translation\n");
            }
            if(set[1].name != null){
                writeResource(set[1], xmlfileName, targetFileName);
            }
        }
    }
    
    private void writeResource(Resource set, String sourceFilename, String targetFilename){
        try {
            String outputFileName = null;
            if(targetFilename != null){
                outputFileName = destDir+File.separator+targetFilename+".txt";
            }else{
                outputFileName = destDir+File.separator+set.name+".txt";
            }
            FileOutputStream file = new FileOutputStream(outputFileName);
            BufferedOutputStream writer = new BufferedOutputStream(file);

            writeHeader(writer,sourceFilename);
            
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
    
    private String getLanguageName(Document doc, String lang){
        if(doc!=null){
            NodeList list = doc.getElementsByTagName(FILE);
            Node node = list.item(0);
            NamedNodeMap attr = node.getAttributes();
            Node orig = attr.getNamedItem(lang);
            
            if(orig != null){
                String name = orig.getNodeValue();
                NodeList groupList = doc.getElementsByTagName(GROUPS);
                Node group = groupList.item(0);
                NamedNodeMap groupAtt = group.getAttributes();
                Node id = groupAtt.getNamedItem(ID);
                if(id!=null){
                    String idVal = id.getNodeValue();
                    
                    if(!name.equals(idVal)){
                        System.out.println("WARNING: The id value != language name. " +
                                           "Please compare the output with the orignal " +
                                           "ICU ResourceBundle before proceeding.");
                    }
                }
                if(!isXmlLang(name)){
                    System.err.println("The attribute "+ lang + "=\""+ name +
                                       "\" of <file> element does not satisfy RFC 1766 conditions.");
                    System.exit(-1);
                }
                return name;
            }
        }
        return null;
    }
    
    // check if the xliff file is translated into multiple languages
    // The XLIFF specification allows for single <target> element
    // as the child of <trans-unit> but the attributes of the 
    // <target> element may different across <trans-unit> elements
    // check for it. Similar is the case with <source> elements
    private String checkLangAttribute(NodeList list, String origName){
        String oldLangName=origName;
        for(int i = 0 ;i<list.getLength(); i++){
            Node node = list.item(i);
            NamedNodeMap attr = node.getAttributes();
            Node lang = attr.getNamedItem(XMLLANG);
            String langName = null;
            // the target element should always contain xml:lang attribute
            if(lang==null ){
                if(origName==null){
                    System.err.println("Encountered <target> element without xml:lang attribute. Please fix the below element in the XLIFF file.\n"+ node.toString());
                    System.exit(-1);
                }else{
                    langName = origName;
                }
            }else{
                langName = lang.getNodeValue();
            }

            if(oldLangName!=null && langName!=null && !langName.equals(oldLangName)){
                throw new RuntimeException("The <trans-unit> elements must be bilingual, multilingual tranlations not supported. xml:lang = " + oldLangName + 
                                           " and xml:lang = " + langName);
            }
            oldLangName = langName;
        }
        return oldLangName;
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
            boolean translateIsDefault = translate == null || translate.equals("yes");
            
            if(comment!=null || ! translateIsDefault || noteLen > 0){
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
                
                // print the translate attribute if any
                if(! translateIsDefault){
                    writeIndent(writer, numIndent);
                    write(writer, TAG+TRANSLATE+SPACE);
                    write(writer, translate);
                    write(writer, LINESEP);
                }
                
                // print note elements if any
                for(int i=0; i<noteLen; i++){
                    if(note[i]!=null){
                        writeIndent(writer, numIndent);
                        write(writer, TAG+NOTE+SPACE+note[i]);
                        write(writer, LINESEP);
                    }
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
                write(writer, name+COLON+STRINGS+ OPENBRACE + QUOTE + escapeSyntaxChars(val) + QUOTE+ CLOSEBRACE + LINESEP);
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
            write(writer, name+COLON+TABLE+OPENBRACE+LINESEP);
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
    
    private String getAttributeValue(Node sNode, String attribName){
        String value=null;
        Node node = sNode;

        NamedNodeMap attributes = node.getAttributes();
        Node attr = attributes.getNamedItem(attribName);
        if(attr!=null){
            value = attr.getNodeValue();
        }

        return value;
    }
    
    private void parseResourceString(Node node, ResourceString[] set){
        ResourceString currentSource;
        ResourceString currentTarget;
        currentSource =  set[0];
        currentTarget =  set[1];
        String resName   = getAttributeValue(node, RESNAME);
        String translate = getAttributeValue(node, TRANSLATE);
        
        // loop to pickup <source>, <note> and <target> elements
        for(Node transUnit = node.getFirstChild(); transUnit!=null; transUnit = transUnit.getNextSibling()){
            short type = transUnit.getNodeType();
            String name = transUnit.getNodeName();
            if(type == Node.COMMENT_NODE){
                // get the comment
               currentSource.comment =  currentTarget.comment = transUnit.getNodeValue();
            }else if( type == Node.ELEMENT_NODE){
                if(name.equals(SOURCE)){
                    // save the source and target values
                    currentSource.name = currentTarget.name = resName;
                    currentSource.val = currentTarget.val = transUnit.getFirstChild().getNodeValue();
                    currentSource.translate = currentTarget.translate = translate;
                }else if(name.equals(NOTE)){
                    // save the note values
                    currentSource.note[currentSource.noteLen++] = 
                        currentTarget.note[currentTarget.noteLen++] =
                        transUnit.getFirstChild().getNodeValue();
                }else if(name.equals(TARGET)){
                    // if there is a target element replace it
                    currentTarget.val = transUnit.getFirstChild().getNodeValue();
                }
            }
            
        }
    }

    private void parseResourceInt(Node node, ResourceInt[] set){
        ResourceInt currentSource;
        ResourceInt currentTarget;
        currentSource =  set[0];
        currentTarget =  set[1];
        String resName   = getAttributeValue(node, RESNAME);
        String translate = getAttributeValue(node, TRANSLATE);
        // loop to pickup <source>, <note> and <target> elements
        for(Node transUnit = node.getFirstChild(); transUnit!=null; transUnit = transUnit.getNextSibling()){
            short type = transUnit.getNodeType();
            String name = transUnit.getNodeName();
            if(type == Node.COMMENT_NODE){
                // get the comment
               currentSource.comment =  currentTarget.comment = transUnit.getNodeValue();
            }else if( type == Node.ELEMENT_NODE){
                if(name.equals(SOURCE)){
                    // save the source and target values
                    currentSource.name = currentTarget.name = resName;
                    currentSource.translate = currentTarget.translate = translate;
                    currentSource.val = currentTarget.val = transUnit.getFirstChild().getNodeValue();
                }else if(name.equals(NOTE)){
                    // save the note values
                    currentSource.note[currentSource.noteLen++] = 
                        currentTarget.note[currentTarget.noteLen++] =
                        transUnit.getFirstChild().getNodeValue();
                }else if(name.equals(TARGET)){
                    // if there is a target element replace it
                    currentTarget.val = transUnit.getFirstChild().getNodeValue();
                }
            }
            
        }
    }
    
    private void parseResourceAlias(Node node, ResourceAlias[] set){
        ResourceAlias currentSource;
        ResourceAlias currentTarget;
        currentSource =  set[0];
        currentTarget =  set[1];
        String resName   = getAttributeValue(node, RESNAME);
        String translate = getAttributeValue(node, TRANSLATE);
        // loop to pickup <source>, <note> and <target> elements
        for(Node transUnit = node.getFirstChild(); transUnit!=null; transUnit = transUnit.getNextSibling()){
            short type = transUnit.getNodeType();
            String name = transUnit.getNodeName();
            if(type == Node.COMMENT_NODE){
                // get the comment
               currentSource.comment =  currentTarget.comment = transUnit.getNodeValue();
            }else if( type == Node.ELEMENT_NODE){
                if(name.equals(SOURCE)){
                    // save the source and target values
                    currentSource.name = currentTarget.name = resName;
                    currentSource.translate = currentTarget.translate = translate;
                    currentSource.val = currentTarget.val = transUnit.getFirstChild().getNodeValue();
                }else if(name.equals(NOTE)){
                    // save the note values
                    currentSource.note[currentSource.noteLen++] = 
                        currentTarget.note[currentTarget.noteLen++] =
                        transUnit.getFirstChild().getNodeValue();
                }else if(name.equals(TARGET)){
                    // if there is a target element replace it
                    currentTarget.val = transUnit.getFirstChild().getNodeValue();
                }
            }
            
        }
    }
    private void parseResourceBinary(Node node, ResourceBinary[] set){
        ResourceBinary currentSource;
        ResourceBinary currentTarget;
        currentSource =  set[0];
        currentTarget =  set[1];

        // loop to pickup <source>, <note> and <target> elements
        for(Node transUnit = node.getFirstChild(); transUnit!=null; transUnit = transUnit.getNextSibling()){
            short type = transUnit.getNodeType();
            String name = transUnit.getNodeName();
            if(type == Node.COMMENT_NODE){
                // get the comment
               currentSource.comment =  currentTarget.comment = transUnit.getNodeValue();
            }else if( type == Node.ELEMENT_NODE){
                if(name.equals(BINSOURCE)){
                    // loop to pickup internal-file/extenal-file element
                    continue;
                }else if(name.equals(NOTE)){
                    // save the note values
                    currentSource.note[currentSource.noteLen++] = 
                        currentTarget.note[currentTarget.noteLen++] =
                        transUnit.getFirstChild().getNodeValue();
                }else if(name.equals(INTERNALFILE)){
                    // if there is a target element replace it
                    String crc = getAttributeValue(transUnit, CRC);
                    String value = transUnit.getFirstChild().getNodeValue();
                    
                    //verify that the binary value conforms to the CRC
                    if(Integer.parseInt(crc, 10) != CalculateCRC32.computeCRC32(value)) {
                        System.err.println("ERROR: CRC value incorrect! Please check.");
                        System.exit(1);
                    }
                    
                    currentTarget.internal = currentSource.internal= value;
                    
                }else if(name.equals(EXTERNALFILE)){
                    currentSource.external = getAttributeValue(transUnit, HREF);
                    currentTarget.external = currentSource.external;
                }
            }
            
        }
    }
    private void parseTransUnit(Node node, Resource[] set){

        String attrType = getAttributeValue(node, RESTYPE);
        String translate = getAttributeValue(node, TRANSLATE);
        if(attrType==null || attrType.equals(STRINGS)){
            ResourceString[] strings = new ResourceString[2];
            strings[0] = new ResourceString();
            strings[1] = new ResourceString();
            parseResourceString(node, strings);
            strings[0].translate = strings[1].translate = translate;
            set[0] = strings[0];
            set[1] = strings[1];
        }else if(attrType.equals(resources[INTEGER_RESOURCE])){
            ResourceInt[] ints = new ResourceInt[2];
            ints[0] = new ResourceInt();
            ints[1] = new ResourceInt();
            parseResourceInt(node, ints);
            ints[0].translate = ints[1].translate = translate;
            set[0] = ints[0];
            set[1] = ints[1];
        }else if(attrType.equals(resources[ALIAS_RESOURCE])){
            ResourceAlias[] ints = new ResourceAlias[2];
            ints[0] = new ResourceAlias();
            ints[1] = new ResourceAlias();
            parseResourceAlias(node, ints);
            ints[0].translate = ints[1].translate = translate;
            set[0] = ints[0];
            set[1] = ints[1];
        }
    }

    private void parseBinUnit(Node node, Resource[] set){
        if (getAttributeValue(node, RESTYPE).equals(resources[BINARY_RESOURCE])) {
            ResourceBinary[] bins = new ResourceBinary[2];
            
            bins[0] = new ResourceBinary();
            bins[1] = new ResourceBinary();
            
            Resource currentSource = bins[0];
            Resource currentTarget = bins[1];
            String resName   = getAttributeValue(node, RESNAME);
            String translate = getAttributeValue(node, TRANSLATE);
            
            currentTarget.name = currentSource.name = resName;
            currentSource.translate = currentTarget.translate = translate;
            
            for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()){
                short type = child.getNodeType();
                String name = child.getNodeName();
                
                if(type == Node.COMMENT_NODE){
                    currentSource.comment = currentTarget.comment = child.getNodeValue();
                }else if(type == Node.ELEMENT_NODE){
                    if(name.equals(BINSOURCE)){
                        parseResourceBinary(child, bins);
                    }else if(name.equals(NOTE)){
                        String note =  child.getFirstChild().getNodeValue();
                        
                        currentSource.note[currentSource.noteLen++] = currentTarget.note[currentTarget.noteLen++] = note;
                    }
                }
            }
            
            set[0] = bins[0];
            set[1] = bins[1];
        }
    }
    
    private void parseArray(Node node, Resource[] set){
        if(set[0]==null){
            set[0] = new ResourceArray();
            set[1] = new ResourceArray();
        }
        Resource currentSource = set[0];
        Resource currentTarget = set[1];
        String resName = getAttributeValue(node, RESNAME);
        currentSource.name = currentTarget.name = resName;
        boolean isFirst = true;
        
        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()){
            short type = child.getNodeType();
            String name = child.getNodeName();
            if(type == Node.COMMENT_NODE){
                currentSource.comment = currentTarget.comment = child.getNodeValue();
            }else if(type == Node.ELEMENT_NODE){
                if(name.equals(TRANSUNIT)){
                    Resource[] next = new Resource[2];
                    parseTransUnit(child, next);
                    if(isFirst==true){
                       ((ResourceArray) currentSource).first = next[0];
                       ((ResourceArray) currentTarget).first = next[1];
                       currentSource = ((ResourceArray) currentSource).first;
                       currentTarget = ((ResourceArray) currentTarget).first;
                       isFirst = false;
                    }else{
                        currentSource.next = next[0];
                        currentTarget.next = next[1];
                        // set the next pointers
                        currentSource = currentSource.next;
                        currentTarget = currentTarget.next;
                    }
                }else if(name.equals(NOTE)){
                    String note =  child.getFirstChild().getNodeValue();
                    currentSource.note[currentSource.noteLen++] = currentTarget.note[currentTarget.noteLen++] = note;
                }else if(name.equals(BINUNIT)){
                    Resource[] next = new Resource[2];
                    parseBinUnit(child, next);
                    if(isFirst==true){
                       ((ResourceArray) currentSource).first = next[0];
                       ((ResourceArray) currentTarget).first = next[1];
                       currentSource = ((ResourceArray) currentSource).first.next;
                       currentTarget = ((ResourceArray) currentTarget).first.next;
                       isFirst = false;
                    }else{
                        currentSource.next = next[0];
                        currentTarget.next = next[1];
                        // set the next pointers
                        currentSource = currentSource.next;
                        currentTarget = currentTarget.next;
                    }
                }
            }
        }
    }
    private void parseIntVector(Node node, Resource[] set){
        if(set[0]==null){
            set[0] = new ResourceIntVector();
            set[1] = new ResourceIntVector();
        }
        Resource currentSource = set[0];
        Resource currentTarget = set[1];
        String resName = getAttributeValue(node, RESNAME);
        String translate = getAttributeValue(node,TRANSLATE);
        currentSource.name = currentTarget.name = resName;
        currentSource.translate = translate;
        boolean isFirst = true;
        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()){
            short type = child.getNodeType();
            String name = child.getNodeName();
            if(type == Node.COMMENT_NODE){
                currentSource.comment = currentTarget.comment = child.getNodeValue();
            }else if(type == Node.ELEMENT_NODE){
                if(name.equals(TRANSUNIT)){
                    Resource[] next = new Resource[2];
                    parseTransUnit(child, next);
                    if(isFirst==true){
                        // the down cast should be safe .. if not something is terribly wrong!!
                       ((ResourceIntVector) currentSource).first = (ResourceInt)next[0];
                       ((ResourceIntVector) currentTarget).first = (ResourceInt) next[1];
                       currentSource = ((ResourceIntVector) currentSource).first;
                       currentTarget = ((ResourceIntVector) currentTarget).first;
                       isFirst = false;
                    }else{
                        currentSource.next = next[0];
                        currentTarget.next = next[1];
                        // set the next pointers
                        currentSource = currentSource.next;
                        currentTarget = currentTarget.next;
                    }
                }else if(name.equals(NOTE)){
                    String note =  child.getFirstChild().getNodeValue();
                    currentSource.note[currentSource.noteLen++] = currentTarget.note[currentTarget.noteLen++] = note;
                }
            }
        }
    }
    private void parseTable(Node node, Resource[] set){
        if(set[0]==null){
            set[0] = new ResourceTable();
            set[1] = new ResourceTable();
        }
        Resource currentSource = set[0];
        Resource currentTarget = set[1];
        
        String resName = getAttributeValue(node, RESNAME);
        String translate = getAttributeValue(node,TRANSLATE);
        if(resName!=null && currentSource.name==null && currentTarget.name==null){
            currentSource.name = currentTarget.name = resName;
        }
        currentTarget.translate = currentSource.translate = translate;
        
        boolean isFirst = true;
        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()){
            short type = child.getNodeType();
            String name = child.getNodeName();
            if(type == Node.COMMENT_NODE){
                currentSource.comment = currentTarget.comment = child.getNodeValue();
            }else if(type == Node.ELEMENT_NODE){
                if(name.equals(GROUPS)){
                    Resource[] next = new Resource[2];
                    parseGroup(child, next);
                    if(isFirst==true){
                        // the down cast should be safe .. if not something is terribly wrong!!
                       ((ResourceTable) currentSource).first = next[0];
                       ((ResourceTable) currentTarget).first = next[1];
                       currentSource = ((ResourceTable) currentSource).first;
                       currentTarget = ((ResourceTable) currentTarget).first;
                       isFirst = false;
                    }else{
                        currentSource.next = next[0];
                        currentTarget.next = next[1];
                        // set the next pointers
                        currentSource = currentSource.next;
                        currentTarget = currentTarget.next;
                    }
                }else if(name.equals(TRANSUNIT)){
                    Resource[] next = new Resource[2];
                    parseTransUnit(child, next);
                    if(isFirst==true){
                        // the down cast should be safe .. if not something is terribly wrong!!
                       ((ResourceTable) currentSource).first = next[0];
                       ((ResourceTable) currentTarget).first = next[1];
                       currentSource = ((ResourceTable) currentSource).first;
                       currentTarget = ((ResourceTable) currentTarget).first;
                       isFirst = false;
                    }else{
                        currentSource.next = next[0];
                        currentTarget.next = next[1];
                        // set the next pointers
                        currentSource = currentSource.next;
                        currentTarget = currentTarget.next;
                    }
                }else if(name.equals(NOTE)){
                    String note =  child.getFirstChild().getNodeValue();
                    currentSource.note[currentSource.noteLen++] = currentTarget.note[currentTarget.noteLen++] = note;
                }else if(name.equals(BINUNIT)){
                    Resource[] next = new Resource[2];
                    parseBinUnit(child, next);
                    if(isFirst==true){
                        // the down cast should be safe .. if not something is terribly wrong!!
                       ((ResourceTable) currentSource).first = next[0];
                       ((ResourceTable) currentTarget).first = next[1];
                       currentSource = ((ResourceTable) currentSource).first;
                       currentTarget = ((ResourceTable) currentTarget).first;
                       isFirst = false;
                    }else{
                        currentSource.next = next[0];
                        currentTarget.next = next[1];
                        // set the next pointers
                        currentSource = currentSource.next;
                        currentTarget = currentTarget.next;
                    }
                }
            }
        }
    }
    
    private void parseGroup(Node node, Resource[] set){

        // figure out what kind of group this is
        String resType = getAttributeValue(node, RESTYPE);
        if(resType.equals(resources[ARRAY_RESOURCE])){
            parseArray(node, set);
        }else if( resType.equals(resources[TABLE_RESOURCE])){
            parseTable(node, set);
        }else if( resType.equals(resources[INTVECTOR_RESOURCE])){
            parseIntVector(node, set);
        }
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
        final String header = 
            "// ***************************************************************************" + LINESEP +
            "// *" + LINESEP +
            "// * Tool: com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter.java" + LINESEP +
            "// * Date & Time: {0,date,MM/dd/yyyy hh:mm:ss a z}"+ LINESEP +
            "// * Source File: {1}" + LINESEP +
            "// *" + LINESEP +                    
            "// ***************************************************************************" + LINESEP;
            
        writeBOM(writer);
        MessageFormat format = new MessageFormat(header);
        Object args[] = {new Date(System.currentTimeMillis()), fileName};

        writeLine(writer, format.format(args));
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
