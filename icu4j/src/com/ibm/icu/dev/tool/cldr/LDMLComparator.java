/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.dev.tool.cldr;

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
import java.util.Calendar;

// DOM imports
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.lang.UCharacter;


public class LDMLComparator {
    /*
     * This application will compare different locale data xml files
     * conforming to localeElements.dtd and produces an xml file file
     * in the format
     */
    private static final int OPT_SUN_JDK = 0x001; /*2exp0*/
    private static final int OPT_IBM_JDK = 0x002; /*2exp1*/
    private static final int OPT_WINDOWS = 0x004; /*2exp2*/
    private static final int OPT_HP_UX   = 0x008; /*2exp3*/
    private static final int OPT_SOLARIS = 0x010; /*2exp4*/
    private static final int OPT_IBM_TOR = 0x020; /*2exp5*/
    private static final int OPT_APPLE   = 0x040; /*2exp6*/
    private static final int OPT_ICU     = 0x080; /*2exp7*/
    private static final int OPT_OTHER   = 0x100; /*2exp8*/
    private static final int OPT_SOURCE  = 0x200; /*2exp9*/
    private static final int OPT_DEST    = 0x400; /*2exp10*/
    private static final int OPT_LINUX   = 0x800; /*2exp11*/
    private static final int OPT_AIX    = 0x1000; /*2exp12*/
    private static final int OPT_COMMON = 0x2000; /*2exp13*/
    private static final int OPT_DIFF   = 0x4000; /*2exp15*/   //PN
    private static final int OPT_DIFF_REF_COMMON
                                        = 0x8000; /*2exp16*/  //PN
    private static final int OPT_BULK
                                    = 0x00010000;   //PN
    private static final int OPT_CASE_SENSITIVE
                                    = 0x00020000;   //PN
    private static final int OPT_VETTING
                                    = 0x00040000;
    private static final int OPT_UNKNOWN
                                    = 0x00080000;

    private static final String COMMON      = "common";
    private static final String ICU         = "icu";
    private static final String IBM_TOR     = "ibm";
    private static final String WINDOWS     = "windows";
    private static final String SUNJDK      = "sunjdk";
    private static final String IBMJDK      = "ibmjdk";
    private static final String HPUX        = "hp";
    private static final String APPLE       = "apple";
    private static final String SOLARIS     = "solaris";
    private static final String OPEN_OFFICE = "open_office";
    private static final String AIX         = "aix";
    private static final String LINUX       = "linux";

    private static final String ALTERNATE   = "ALT";
    private static final String ALTERNATE_TITLE   = "(Original)";
    private static final String ALT_COLOR   = "#DDDDFD";
    //PN added
    private static final String DIFF        = "diff";
    private static final String DIFF_REF_COMMON = "diff_ref_common";
    private static final String BULK        = "bulk";
    private static final String CASE_SENSITIVE = "case_sensitive";
    private static final String VETTING        = "vetting";
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
        "-" + DIFF,   //PN added, indicates that only differing elements/attributes to be written to html
        "-" + DIFF_REF_COMMON,  //PN added, same as diff only common is excluded from diff but gets printed to html for reference purposes
        "-" + BULK,  //do a bulk comparison of folder contents
        "-" + CASE_SENSITIVE,  //do case sensitive matching (by default it's not case sensitive)
        "-" + VETTING // go into Vetting mode. (show draft, etc)
    };



    public static void main(String[] args){
        LDMLComparator comparator = new LDMLComparator();
        comparator.processArgs(args);
    }

    Hashtable optionTable = new Hashtable();
    private String sourceFolder = ".";
    private String destFolder = ".";
    private String localeStr;
    private String ourCvsVersion = "";
    private Calendar cal = Calendar.getInstance();
    private Hashtable colorHash = new Hashtable();
    private String goldFileName;
    private String goldKey;
    private int numPlatforms = 0;
    private int serialNumber =0;
    private TreeMap compareMap = new TreeMap();
    private Hashtable doesNotExist = new Hashtable();
    private Hashtable requested = new Hashtable();
    private Hashtable deprecatedLanguageCodes = new Hashtable();
    private Hashtable deprecatedCountryCodes = new Hashtable();
    private TreeSet vettingSet = new TreeSet();
    private String  encoding   = "UTF-8"; // default encoding

    //PN added
    private Vector m_PlatformVect = new Vector();  //holds names of platforms
    private Vector m_PlatformFolderVect = new Vector();  //holds names of folders containing locale data for each platform
    private int m_iOptions;
    // m_AccumulatedResultsMap key = element id (node+parentNode+type+index string), data = AccumulateDifferences instance
    private TreeMap m_AccumulatedResultsMap = new TreeMap();
    private int m_iTotalConflictingElements = 0;
    private int m_iTotalNonConflictingElements = 0;
    private TreeMap m_LocaleSummaryDataMap = new TreeMap ();  //key = localename, data = summary info
    private boolean m_Vetting = false;

    private int m_totalCount = 0;
    private String m_Messages = "";

    private class CompareElement
    {
        String node;
        String index;
        String parentNode;
        String type;
        Hashtable platformData = new Hashtable();
        String referenceUrl;
    }

    //PN added
    //used for bulk comparisons
    //holds the locales where the element identified by node,index and parentNode conflict
    //for at least 2 of the platforms tested
    //holds the locales where the element identified by node,index and parentNode don't
    //for at all the platforms tested
    private class AccumulatedResults
    {
        String node;
        String index;
        String parentNode;
        String type;
        Vector localeVectDiff = new Vector();  //holds loccales where a conflict in data was found
        Vector localeVectSame = new Vector();  //holds loccales where a no conflict in data was found
    }

    private class SummaryData
    {
        String m_szPlatforms ;
        int m_iNumConflictingElements;
    }


    LDMLComparator(){
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
        // TODO - use deprecatedMap instead.
        //deprecatedLanguageCodes.put("sh", "what ever the new one is");
        deprecatedLanguageCodes.put("iw", "he");
        deprecatedLanguageCodes.put("in", "id");
        deprecatedLanguageCodes.put("ji", "yi");
        deprecatedLanguageCodes.put("jw", "jv"); // this does not even exist, JDK thinks jw is javanese!!

        //country codes
        deprecatedCountryCodes.put("TP", "TL");
        deprecatedCountryCodes.put("ZR", "CD");
    }



     private void processArgs(String[] args)
    {
        m_iOptions = identifyOptions(args);
        if ((args.length < 2) || ((m_iOptions & OPT_UNKNOWN) != 0))
        {
            printUsage();
            return;
        }
        boolean warning[] = new boolean[1];
        warning[0] = false;
        Enumeration en = optionTable.keys();

        try
        {
            //check for bulk operation
            if ((m_iOptions & OPT_BULK) != 0)
            {
                doBulkComparison();
            }
            else
            {
                localeStr  = goldFileName.substring(goldFileName.lastIndexOf(File.separatorChar)+1,goldFileName.lastIndexOf('.'));

                String fileName = destFolder+File.separator+localeStr+".html";
                m_totalCount = 0;
                if((m_iOptions & OPT_VETTING) != 0)
                {
                    m_Vetting = true;
                    addVettable(goldFileName, goldKey);
                }
                else
                {
                    addToCompareMap(goldFileName, goldKey);
                }
                for(;en.hasMoreElements();)
                {
                    String key = (String)en.nextElement();
                    String compFile = (String) optionTable.get(key);
                    if((m_iOptions & OPT_VETTING) != 0) {
                        addVettable(goldFileName, goldKey);
                    } else {
                        addToCompareMap(compFile,key);
                    }
                }
                if((m_totalCount == 0) && m_Vetting) { // only  optional for vetting.
                    //System.out.println("INFO:  no file created (nothing to write..) " + fileName);
                } else {
                    ourCvsVersion = "";
                    getCVSVersion();
                    OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(fileName),encoding);
                    System.out.println("INFO: Writing: " + fileName + "\t(" + m_totalCount + " items)");
                    PrintWriter writer = new PrintWriter(os);
                    printHTML(writer, localeStr);
                    {
                        ULocale ourLocale = new ULocale(localeStr);
                        String idxFileName = destFolder+File.separator+ourLocale.getDisplayLanguage()+"_"+ourLocale.getDisplayCountry()+"_"+localeStr+".idx";
                        OutputStreamWriter is = new OutputStreamWriter(new FileOutputStream(idxFileName),"utf-8");
                        PrintWriter indexwriter = new PrintWriter(is);
                        indexwriter.println("<tr>");
                        indexwriter.println(" <td>" +
                                localeStr +
                                "</td>");
                        indexwriter.println(" <td><a href=\"" + localeStr+".html" + "\">" +
                            ourLocale.getDisplayName() + "</a></td>");
                        indexwriter.println(" <td>" + m_totalCount + "</td>");
                        indexwriter.println(" <td>" + LDMLUtilities.getCVSLink(localeStr,ourCvsVersion) +  ourCvsVersion + "</a></td>");
                        indexwriter.println("</tr>");
                        is.close();
                    }
                    // TODO: handle vettingSet;
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }
    private void printUsage() {
        System.err.println("Usage: LDMLComparator [<option>:<gold>] filename1 [option] filename2 ... \n"+
                           " LDMLComparator [-common:<gold>] filename [-icu] filename" +
                           " [-ibmjdk] filename [-windows] filename" +
                           " [-hpux]  filename [-solaris] filename"  +
                           " [-ibmtor] filename [-apple] filename"   +
                           " [-sunjdk]  filename [-open_office] filename" +
                           " [-aix] filename [-linux] filename" +
                           " [-diff / -diff_ref_common] [-bulk]" +
                           " [-case_sensitive (only active if -diff of -diff-ref-common option selected)]"
                           );
        System.err.println("\nExample 1: \n " +
        "LDMLComparator -solaris:gold foldername1 -sunjdk foldername2 -common foldername3 -diff-ref-common -bulk\n" +
        "\t\t will do a bulk comparison of the locales in folder1 and folder2 \n " +
        "\t\t and print the values of any differing elements plus the \n" +
        "\t\t corresponding element's value in folder3 to bulk.html \n" +
        "\t\t as well as a summary to bulk_summary.html \n");
        System.err.println("Example 2: \n" +
        "LDMLComparator -common:gold filename1 -sunjdk filename2 -diff \n" +
        "\t\t will do a comparison of the locales specified by filename1 and \n" +
        "\t\t filename2 and print the values of any differing elements \n" +
        "\t\t to a file called filename1.html in the current directory \n");
    }

   private int identifyOptions(String[] options)
    {
        int result = 0;
        for (int j = 0; j < options.length; j++)
        {
            String option = options[j];
            boolean isGold = false;
            if (option.startsWith("-"))
            {
                if(option.indexOf(":gold")>0)
                {
                    option = option.substring(0,option.indexOf(":"));
                    isGold = true;
                }
                boolean optionRecognized = false;
                for (int i = 0; i < USER_OPTIONS.length; i++)
                {

                    if (USER_OPTIONS[i].equals(option))
                    {
                        result |= (int)(1 << i); // calculate option bit value
                        optionRecognized = true;
                        if(USER_OPTIONS[i].equals("-s"))
                        {
                            sourceFolder = options[++j];
                        }else if(USER_OPTIONS[i].equals("-d"))
                        {
                            destFolder = options[++j];
                        }
                        else if (USER_OPTIONS[i].equals("-" + DIFF))
                        {

                        }
                        else if (USER_OPTIONS[i].equals("-" + DIFF_REF_COMMON))
                        {

                        }
                        else if (USER_OPTIONS[i].equals("-" + BULK))
                        {

                        }
                        else if (USER_OPTIONS[i].equals("-" + VETTING))
                        {
                         m_Vetting = true;
                        }
                        else if (USER_OPTIONS[i].equals("-" + CASE_SENSITIVE))
                        {}
                        else
                        {
                            if(!isGold)
                            {
                                optionTable.put(option.substring(1,option.length()),options[++j]);

                            }else
                            {
                                goldFileName = options[++j];
                                goldKey      = option.substring(1,option.length());
                            }
                            //PN added
                            m_PlatformVect.add(option.substring(1,option.length()));
                            m_PlatformFolderVect.add(options[j]);
                        }
                        break;
                    }
                }
                if (!optionRecognized)
                {
                    result |= OPT_UNKNOWN;
                }
            } else {
                if(m_Vetting == true) {
                    vettingSet.add(option);
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
                folder = name+"/main/";
                if(name.equals("icu")|| name.equals("common")|| name.indexOf("jdk")>=0){
                    int index = localeStr.indexOf("_");
                    String parent = "";
                    if(index > -1){
                        parent = localeStr.substring(0,index);
                    }
                    writer.print("                <th bgcolor=\""+
                                   (String)colorHash.get(name)+ "\">" +
                                   name.toUpperCase()+
                                   " (<a href=\"../../"+folder+localeStr+".xml\">"+localeStr+"</a>,"+
                                   " <a href=\"../../"+folder+parent+".xml\">"+parent+"</a>,"+
                                   " <a href=\"../../"+folder+"root.xml\">root</a>)"+
                                   "</th>\n");
                }else{
                    writer.print("                <th bgcolor=\""+
                                   (String)colorHash.get(name)+ "\">" +
                                   name.toUpperCase()+
                                   " (<a href=\"../../"+folder+localeStr+".xml\">"+localeStr+"</a>)"+
                                   "</th>\n");
                }

                numPlatforms++;

            }
        }
        if(m_Vetting) {
            writer.print("<th bgcolor=\"" + ALT_COLOR + "\">" + ALTERNATE_TITLE + "</th>");
        }
        writer.print("            </tr>\n");
    }

    //PN added
    private void printTableHeaderForDifferences(PrintWriter writer)
    {

        writer.print(  "            <tr>\n" +
        "                <th width=10%>N.</th>\n"+
        "                <th width=10%>ParentNode</th>\n"+
        "                <th width=10%>Name</th>\n"+
        "                <th width=10%>ID</th>\n");

        for (int i=0; i < m_PlatformVect.size(); i++)
        {
            String name = (String)m_PlatformVect.elementAt(i);
            String folder;

            //      Object obj = requested.get(name);
            //      if(obj!=null && doesNotExist.get(name)==null )
            //      {
            folder = name+"/xml/";
            writer.print("                <th bgcolor=\""+
            (String)colorHash.get(name)+ "\">" +
            name.toUpperCase()+
            " (<a href=\"../"+folder+localeStr+".xml\">xml</a>)"+
            "</th>\n");
            //not used          numPlatforms++;

            //     }
        }
        if(m_Vetting) {
            writer.print("<th>" + ALTERNATE_TITLE + "</th>");
        }
        writer.print("            </tr>\n");
    }

    //PN added
    // method to print differing elements/attributes only to HTML
    //returns false if a difference found otherwise true
    private boolean printDifferentValues(CompareElement element, PrintWriter writer)
    {
        boolean isEqual = true;
        //following don't count
        if ((element.node.compareTo((String)"generation") == 0)
        || (element.node.compareTo((String)"version")==0))
        {
            return isEqual;
        }

        String compareTo = null;
        boolean bFoundFirst = false;
        for (int i=0; i < m_PlatformVect.size(); i++)
        {
            String value = (String)element.platformData.get(m_PlatformVect.elementAt(i));
            if (value == null)
                continue;
            //loop until non null value is found, this is the reference for comparison
            if (bFoundFirst == false)
            {
                compareTo = value;
                bFoundFirst = true;
            }
            else
            {   //we have something to compare this element to
                if(Normalizer.compare(compareTo,value,0)==0)
                {
                    isEqual = true;
                }
                else if(Normalizer.compare(compareTo,value,Normalizer.COMPARE_IGNORE_CASE)==0)
                {
                    if ((m_iOptions & OPT_CASE_SENSITIVE) == 0)
                    {  //it's not a case sensitive search so this is a match
                        isEqual = true;
                    }
                    else
                    {
                        isEqual = false;
                        break;  //we have found a difference therefore break out of loop , we will print full row
                    }
                }
                else
                {
                    isEqual = false;
                    break;  //we have found a difference therefore break out of loop , we will print full row
                }
            }  //end if
        } //end while

        //if any differences found then print all non null values
        if (isEqual == false)
        {
            writer.print("            <tr>\n");
            writer.print("                <td><a NAME=\""+serialNumber+"\" href=\"#"+serialNumber+"\">"+serialNumber+"</a></td>\n");
            writer.print("                <td>"+mapToAbbr(element.parentNode)+"</td>\n");
            writer.print("                <td>"+mapToAbbr(element.node)+"</td>\n");
            writer.print("                <td>"+element.index+"</td>\n");

            for (int i=0; i < m_PlatformVect.size(); i++)
            {
                String val = (String)element.platformData.get(m_PlatformVect.elementAt(i));
                if (val != null)
                {
                    writer.print("                <td>"+val+"</td>\n");
                }
                else
                {
                    writer.print("                <td>&nbsp;</td>\n");
                }
            } //end while

            writer.print("            </tr>\n");
            serialNumber++;
        }  //endif
        return isEqual;
    }

    //PN added
    // method to print differing elements/attributes only to HTML excluding Common from diff
    //only if the other platfroms differ amongst themselves will the Common data be printed
    //returns false if a difference found otherwise true
    private boolean printDifferentValuesWithRef(CompareElement element, PrintWriter writer)
    {
        boolean isEqual = true;
        //following don't count
        if ((element.node.compareTo((String)"generation") == 0)
        || (element.node.compareTo((String)"version")==0))
        {
            return isEqual;
        }

        String compareTo = null;
        boolean bFoundFirst = false;
        for (int i=0; i < m_PlatformVect.size(); i++)
        {
            //excluding Common from diff
            String platform = (String) m_PlatformVect.elementAt(i);
            if (platform.compareTo(COMMON)==0)
                continue;

            String value = (String)element.platformData.get(platform);
            if (value == null)
                continue;

            //loop until non null value is found, this is the reference for comparison
            if (bFoundFirst == false)
            {
                compareTo = value;
                bFoundFirst = true;
            }
            else
            {   //we have something to compare this element to
                if(Normalizer.compare(compareTo,value,0)==0)
                {
                    isEqual = true;
                }
                else if(Normalizer.compare(compareTo,value,Normalizer.COMPARE_IGNORE_CASE)==0)
                {
                    //case difference on date and time format doesn't matter
                    if ((element.parentNode.compareTo("timeFormat")==0)
                    || (element.parentNode.compareTo("dateFormat")==0))
                    {
                        isEqual = true;
                    }
                    else
                    {
                        if ((m_iOptions & OPT_CASE_SENSITIVE) == 0)
                        {  //it's not a case sensitive search so this is a match
                            isEqual = true;
                        }
                        else
                        {
                            isEqual = false;
                            break;  //we have found a difference therefore break out of loop , we will print full row
                        }
                    }
                }
                else
                {
                    isEqual = false;
                    break;  //we have found a difference therefore break out of loop , we will print full row
                }
            }  //end if
        } //end while

        //if any differences found then print all non null values
        if (isEqual == false)
        {
            writer.print("            <tr>\n");
            writer.print("                <td><a NAME=\""+serialNumber+"\" href=\"#"+serialNumber+"\">"+serialNumber+"</a></td>\n");
            writer.print("                <td>"+mapToAbbr(element.parentNode)+"</td>\n");
            writer.print("                <td>"+mapToAbbr(element.node)+"</td>\n");
            writer.print("                <td>"+element.index+"</td>\n");

            for (int i=0; i < m_PlatformVect.size(); i++)
            {
                String val = (String)element.platformData.get(m_PlatformVect.elementAt(i));
                if (val != null)
                {
                    writer.print("                <td>"+val+"</td>\n");
                }
                else
                {
                    writer.print("                <td>&nbsp;</td>\n");
                }
            } //end while

            writer.print("            </tr>\n");
            serialNumber++;
        }  //endif

        return isEqual;
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
                    }else if(value.equals("")){
                        color = "#FFFFFF";
                        break;
                    }else if(element.parentNode.indexOf("decimal")>-1 || element.parentNode.indexOf("currency")>-1 ){
                        if(comparePatterns(compareTo, value)){
                            color = (String)colorHash.get(PLATFORM_PRINT_ORDER[j]);
                            isEqual = true;
                            break;
                        }
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
                if(m_Vetting) {
                    String altText = (String)element.platformData.get("ALT");
                    writer.print("<td>" + value);
                    String parName = mapToAbbr(element.parentNode);
                    if ((parName.indexOf("_dateFormat")!=-1)
                        || (parName.indexOf("_timeFormat")!=-1))
                            {
                                writer.print("<form method=\"POST\" action=\"http://oss.software.ibm.com/cgi-bin/icu/lx/\">" +
                                    "<input type=hidden name=\"_\" value=\"" + localeStr + "\"/>" +
                                    "<input type=hidden name=\"x\" value=\"" + "dat" + "\"/>" +
                                    "<input type=hidden name=\"str\" value=\"" + value + "\"/>" +
                                    "<input type=submit value=\"" + "Test" + "\"/>" +
                                    "</form>");
                            }
                    if(/*m_Vetting &&*/ element.referenceUrl != null) {
                        writer.print("<br><div align='right'><a href=\"" + element.referenceUrl + "\"><i>(Ref)</i></a></div>");
                    }
                    writer.print("</td>");
                    if(altText!=null) {
                        writer.print("        <td bgcolor="+ALT_COLOR+">"+altText);
                        writer.print("</td>\n");
                    }
                } else {
                    if(caseDiff==true){
                        writer.print("                <td bgcolor="+color+">"+value+"&#x2020;");
                    }else{
                        writer.print("                <td bgcolor="+color+">"+value);
                    }
                    writer.print("</td>\n");
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
      //  System.out.println("INFO: Creating the comparison chart ");
        ULocale locale = new ULocale(localeStr);
        String displayLang = locale.getDisplayLanguage();
        String dispCountry = locale.getDisplayCountry();
        String dispVariant = locale.getDisplayVariant();
        String displayName = localeStr+" ("+locale.getDisplayName()+") ";
          if ((m_iOptions & OPT_DIFF_REF_COMMON) != 0)
              writer.print("<p>   Common data shown for reference purposes only</p>\n");

        if(!m_Vetting) {
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
                                    "<a href=\"../../comparison_charts.html\">Cover Page</a>, "+
                                    "<a href=\"./index.html\">Index</a>, "+
                                    "<a href=\"../collation/"+localeStr+".html\">Collation</a> "+
                                    "</b></p>\n"+
                           "        <table>\n");
        } else {
        writer.print("<html>\n"+
                           "    <head>\n"+
                           "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"+
                           "        <title>Draft/Alt: "+localeStr+"</title>\n"+
                           "    </head>\n"+
                           "    <style>\n"+
                           "         <!--\n" +
                           "         table        { border-spacing: 0; border-collapse: collapse;  \n" +
                           "                        border: 1px solid black }\n" +
                           "         td, th       { border-spacing: 0; border-collapse: collapse;  color: black; \n" +
                           "                        vertical-align: top; border: 1px solid black }\n" +
                           "         -->\n" +
                           "     </style>"+
                           "     <body bgcolor=\"#FFFFFF\">\n"+
                           "        <p><b>"+displayName+
                                    "<a href=\"http://oss.software.ibm.com/cgi-bin/icu/lx/en/?_="+localeStr+"\">Demo</a>, "+
                                    "<a href=\"./index.html\">Main and About</a>, "+
                                    "</b></p>\n");
                  if((ourCvsVersion!=null) && (ourCvsVersion.length()>0)) {
                    writer.println("<h3><tt>"+ LDMLUtilities.getCVSLink(localeStr) + localeStr + ".xml</a> version " +
                        LDMLUtilities.getCVSLink(localeStr,ourCvsVersion) + ourCvsVersion + "</a></tt></h3>");
                  }
                  writer.print(         "        <table>\n");
        }

        //PN added
        if (((m_iOptions & OPT_DIFF) !=0)
        || ((m_iOptions & OPT_DIFF_REF_COMMON)!=0))
        {
            printTableHeaderForDifferences(writer);
        }
        else
        {
            printTableHeader(writer);
        }


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
                //PN added
                if ((m_iOptions & OPT_DIFF) !=0)
                {
                    printDifferentValues(element,writer);  //only print differences
                }
                else if((m_iOptions & OPT_DIFF_REF_COMMON)!=0)
                {
                    printDifferentValuesWithRef(element,writer);
                }
                else
                {
                    printValue(element,writer);
                }
            }else{
                throw new RuntimeException("No objects stored in the compare map!");
            }

        }
        writer.print( "        </table>\n");

        if(m_Vetting) {
              if(m_Messages.length()>0) {
                writer.print("<table bgcolor=\"#FFBBBB\" border=3><tr><th>Warnings (please see source LDML)</th></tr>" +
                    "<tr><td>" + m_Messages + "</td></tr></table><p/><p/>\n");
              }
            writer.print("<i>Interim page - subject to change</i> (<a href=\"./index.html\">Help</a>)<br/>");
        }

        writer.print( "        <p>Created on: " + cal.getTime() +"</p>\n"+
                      "    </body>\n"+
                      "</html>\n");
        writer.flush();

        writer.flush();
    }

     private Document getFullyResolvedLocale(String localeName,String fileName){
         // here we assume that "_" is the delimiter
         int index = fileName.lastIndexOf(File.separatorChar);
         String sourceDir = fileName.substring(0, index+1);
         String locale = fileName.substring(index+1, fileName.lastIndexOf("."));
         System.out.println("INFO: Creating fully resolved tree for : " + fileName);

         Document doc = LDMLUtilities.getFullyResolvedLDML(sourceDir, locale, true, true, true);
         /*
          * debugging code
          *
         try{
             OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destFolder+File.separator+localeName+"_debug.xml"),encoding);
             LDMLUtilities.printDOMTree(doc,new PrintWriter(writer));
             writer.flush();
         }catch( Exception e){
             //throw the exception away .. this is for debugging
         }
         */
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
         return extractMergeData(testDoc,key,false);

     }

     private Document getParsedLocale(String localeName,String fileName){
         // here we assume that "_" is the delimiter
         int index = fileName.lastIndexOf(File.separatorChar);
         String sourceDir = fileName.substring(0, index+1);
         String locale = fileName.substring(index+1, fileName.lastIndexOf("."));
         System.out.println("INFO: Parsing " + fileName);
         Document doc = LDMLUtilities.parse(fileName,true); // ?

         return doc;
     }

     private boolean addVettable(String fileName, String key)
     {
         // parse the test doc only if gold doc was parsed OK
         Document testDoc = getParsedLocale(key,fileName);
         requested.put(key,"");
         if (null == testDoc)
         {
             doesNotExist.put(key, "");
             return false;
         }
         return extractMergeData(testDoc,key,false);

     }

     private boolean comparePatterns(String pat1,String pat2){
        //TODO: just return for now .. this is useful only
        //when comparing data from toronto
        try{
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
        }catch(Exception e){
            //throw away the exception
        }
        return false;

        //return true;
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

    private final void addElement(String childNode, String parentNode, String id, String index,
                            String platformValue, String platformName){
        addElement(childNode,parentNode,id,index,platformValue,platformName,null);
    }

    private void addElement(String childNode, String parentNode, String id, String index,
                            String platformValue, String platformName, String referenceUrl){
        m_totalCount++;
        Object obj = compareMap.get(id);
        CompareElement element;
        if(obj==null){
            element = new CompareElement();
            //initialize the values
            element.index = index;
            element.parentNode = parentNode;
            element.node = childNode;
            element.referenceUrl = referenceUrl;
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
    private String getTag(String childNodeName, String index){

        //for months make index a,b,c,d etc
        if(childNodeName.indexOf("month")>-1){
            int i = Integer.parseInt(index);
            StringBuffer temp = new StringBuffer();
            temp.append((char)('a'+i));
            return temp.toString();
        }else if(childNodeName.indexOf("day")>-1){
            if (index.equals("sun")){
               return "a";
           }else if(index.equals("mon")){
               return "b";
           }else if( index.equals("tue")){
               return "c";
           }else if (index.equals("wed")){
               return "d";
           }else if (index.equals("thu")){
               return "e";
           }else if (index.equals("fri")){
               return "f";
           }else if (index.equals("sat")){
               return "g";
           }
        }else{
            return index;
        }
        return "";
    }

    private boolean extractMergeData(Node node,String key, boolean parentDraft){
        Node childOfSource;
        for(childOfSource = node.getFirstChild(); childOfSource != null; childOfSource = childOfSource.getNextSibling()) {
            if (childOfSource.getNodeType() != Node.ELEMENT_NODE) {
                 continue;
            }
            String altText = null;
//            String altReferenceUrl = null;
            Node altForChild = null;
            boolean subDraft = parentDraft;
            String childOfSourceName = childOfSource.getNodeName();
            //Ignore collation and special tags
            if(childOfSourceName.equals("collations")|| childOfSource.equals("special")
                || childOfSourceName.indexOf(":")>-1){
                 continue;
            }

            if(m_Vetting && LDMLUtilities.isNodeDraft(childOfSource)) {
                if(!subDraft) {
                    subDraft = true;
                }
            }
            String referenceUrl = null;
            if(m_Vetting) {
                referenceUrl = LDMLUtilities.getAttributeValue(childOfSource, LDMLConstants.REFERENCES);
                if((referenceUrl!=null)&&(referenceUrl.length()==0)) {
                    referenceUrl = null;
                }
            }

            if(m_Vetting) { /* Should this be always checked? */
                String alt = LDMLUtilities.getAttributeValue(childOfSource, LDMLConstants.ALT);
                if(alt!=null) {
                    if(alt.equals(LDMLConstants.PROPOSED)) {
                        if(subDraft == false) {
                            throw new IllegalArgumentException("***** ERROR Proposed but not draft? " + childOfSource.toString());
                            //NOTREACHED
                        }
                        altForChild = LDMLUtilities.getNonAltNodeLike(node, childOfSource);
                        if(altForChild == null) {
                            System.out.println("WARNING: can't find a node like this one: " + childOfSource.toString() + " - consider removing the alt=\"proposed\" attribute.");
                            alt = null;
                        }
//                        altReferenceUrl = LDMLUtilities.getAttributeValue(altForChild, LDMLConstants.REFERENCES);
//                        if((altReferenceUrl!=null)&&(altReferenceUrl.length()==0)) {
//                            altReferenceUrl = null;
//                        }
                    } else if(subDraft) { /* don't care about nondraft */
                        String type = LDMLUtilities.getAttributeValue(childOfSource, LDMLConstants.TYPE);
                        if(type==null) {
                            type = "";
                        }
                        m_Messages = m_Messages + " <br> UNKNOWN alt type '" + alt + "' for " +
                                node.getNodeName() + "/" + childOfSourceName + "/" + type;
                        System.err.println("Warning: unknown alt type '" + alt + "'  - *IGNORING*. " + childOfSource.toString());
                        continue;
                    }
                }
             }

             if(childrenAreElements(childOfSource)==false){
                 NamedNodeMap attr = childOfSource.getAttributes();
                 Node typeNode = attr.getNamedItem("type");
                 String index="";
                 if(typeNode!=null){

                     if(childOfSource.getNodeName().equals("era")&&!key.equals("common")){
                         //remap type for comparison purpose
                         // TODO remove this hack
                         int j = Integer.parseInt(typeNode.getNodeValue());
                         if(j>0){
                             j--;
                         }
                         typeNode.setNodeValue(Integer.toString(j));
                     }
                     String temp =typeNode.getNodeValue();

                     if(!temp.equals("standard")){
                         index = temp;
                     }

                 }
                 if(m_Vetting) { // TODO: all?
                     Node keyNode = attr.getNamedItem("key");
                     String keyIndex="";
                     if(keyNode!=null){
                         String temp =keyNode.getNodeValue();
                         index = index + " (" + temp + ")";
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
                 if(altForChild != null) {
                     Node valueNode2 = altForChild.getFirstChild();
                     if(valueNode2 != null){
                        String temp = trim(valueNode2.getNodeValue());
                        if(!temp.equals("standard")){
                            altText = temp;
                        } else {
                            altText = "??? alt=standard";
                        }
                    } else {
                        altText = "??? alt has no value";
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
                             if( ! parentNodeName.equals("calendar")){
                                 type = mytype;
                             }else{
                                 parentNodeName = mytype;
                             }
                         }
                     }

                 }
                 if(grandParentNodeName.equals("eras") ){
                     Node calendar = grandParentNode.getParentNode();
                     NamedNodeMap gpa = calendar.getAttributes();
                     Node gptNode = gpa.getNamedItem("type");
                     if(gptNode!=null){
                         String gptType = gptNode.getNodeValue();
                         if(!gptType.equals("standard")){
                             grandParentNodeName = gptType;
                         }
                     }
                     parentNodeName = grandParentNodeName+ "\u200b_" + parentNodeName;
                 }
                 if(grandParentNodeName.equals("calendar")){
                     NamedNodeMap gpa = grandParentNode.getAttributes();
                     Node gptNode = gpa.getNamedItem("type");
                     if(gptNode!=null){
                         String gptType = gptNode.getNodeValue();
                         if(!gptType.equals("standard")){
                             grandParentNodeName = gptType;
                         }
                     }
                     parentNodeName = grandParentNodeName+ "\u200b_" + parentNodeName;
                 }
                 if(grandParentNodeName.equals("monthContext")|| grandParentNodeName.equals("dayContext") ||
                         grandParentNodeName.equals("dateFormatLength") || grandParentNodeName.equals("timeFormatLength") ||
                         grandParentNodeName.equals("dateTimeFormatLength")){

                     Node calendar = grandParentNode.getParentNode().getParentNode();
                     NamedNodeMap ggpa = calendar.getAttributes();
                     Node ggptNode = ggpa.getNamedItem("type");
                     if(ggptNode!=null){
                         String ggptType = ggptNode.getNodeValue();
                         if(!ggptType.equals("standard")){
                             grandParentNodeName = ggptType;
                             parentNodeName = ggptType+"\u200b_"+parentNodeName;
                         }
                     }
                    NamedNodeMap gpa = grandParentNode.getAttributes();
                    Node gptNode = gpa.getNamedItem("type");
                    if(gptNode!=null){
                        String gptType = gptNode.getNodeValue();
                        if(!gptType.equals("standard")){
                            parentNodeName = parentNodeName+"\u200b_"+gptType;
                        }
                    }
                    NamedNodeMap pa = parentNode.getAttributes();
                    Node ptNode = pa.getNamedItem("type");
                    if(ptNode!=null){
                        String ptType = ptNode.getNodeValue();
                        if(!ptType.equals("standard")){
                            parentNodeName = parentNodeName+"\u200b_"+ptType;
                        }
                    }

                 }
                 if(childNodeName.equals("pattern") ||grandParentNodeName.equals("zone") ){
                     if(parentNodeName.indexOf("date")==-1 && parentNodeName.indexOf("time")==-1){
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
                 }
                 if(grandParentNodeName.equals("special") || parentNodeName.equals("special") || childNodeName.equals("special")
                    || grandParentNodeName.indexOf(":")>0){
                     continue;
                 }
                 if(!nodeValue.equals("") &&
                    !childOfSource.getNodeName().equals("version")){


                     // for country codes and language codes
                     // replace the deprecated codes with the latest ones
                     if(childNodeName.equals("language")){
                         String temp = (String)deprecatedLanguageCodes.get(index);
                         if(temp!=null){
                             index = temp;
                         }
                     }else if( childNodeName.equals("territory")){
                         String temp = (String)deprecatedCountryCodes.get(index);
                         if(temp!=null){
                             index = temp;
                         }
                     }
                     String id = "";
                     if(!type.equals("")){
                         id = parentNodeName+"_"+childNodeName+"_"+type+"_"+getTag(childNodeName, index)+"_"+grandParentNodeName;
                     }else{
                         id = parentNodeName+"_"+childNodeName+"_"+getTag(childNodeName, index)+"_"+grandParentNodeName;
                     }
                     if(!index.equals("")){
                         if(!index.equals(nodeValue) && !index.equals("Fallback")){
                            if(!m_Vetting || subDraft) {
                                addElement(childNodeName, parentNodeName, id, index, nodeValue, key,referenceUrl);
                                if(altText!=null) {
                                addElement(childNodeName, parentNodeName, id, index, altText, "ALT",null /* altReferenceUrl */);
                                }
                            }
                         }
                     }else{
                         if(!type.equals(nodeValue) && !type.equals("Fallback")){
                            if(!m_Vetting || subDraft) {
                                addElement(childNodeName, parentNodeName, id, type, nodeValue, key,referenceUrl);
                                if(altText!=null) {
                                    addElement(childNodeName, parentNodeName, id, index, altText, "ALT",null /* altReferenceUrl */);
                                }
                            }
                         }
                     }
                 }
                 if(attr.getLength()>0 && typeNode==null){ //TODO: make this a fcn
                     // add an element for each attribute different for each attribute
                     if(!m_Vetting || subDraft) {
                         for(int i=0; i<attr.getLength(); i++){
                             Node item = attr.item(i);
                             String attrName =item.getNodeName();
                             String subAltText = null;
                             if(attrName.equals("type")){
                                 continue;
                             }
                             if(attrName.equals("alt")){
                                 continue;
                             }
                             if(attrName.equals("draft")){
                                 continue;
                             }
                             if(grandParentNodeName.equals("zone") ){
                                parentNodeName = grandParentNodeName+"\u200b_"+parentNodeName;
                             }
                             String id = grandParentNodeName+"_"+parentNodeName+"_"+childNodeName+"_"+type+"_"+attrName;
                             String subNodeValue = item.getNodeValue();
                             if(altForChild!=null) {
                                subAltText="?";
                                System.err.println(parentNodeName + "/" + childNodeName + " alt?? : " + altText);
                                throw new IllegalArgumentException("UNKNOWN ALT SUBTAG + " + parentNodeName + "/" + childNodeName + " alt?? : " + altText + " not " + subNodeValue);
                             }
                             if(!index.equals("")){
                                 addElement(childNodeName, parentNodeName, id, index, subNodeValue, key);
                             }else if(!type.equals("")){
                                 addElement(childNodeName, parentNodeName, id, type, subNodeValue, key);
                             }else{
                                 if(!attrName.equals("draft")){
                                     addElement(childNodeName, parentNodeName, id, attrName, subNodeValue, key);
                                 }
                             }
                         }
                     }
                 }
             }else{
                 //the element has more children .. recurse to pick them all
                 extractMergeData(childOfSource,key, subDraft);
             }
        }
        return true;
    }

    //*****************************************************************************************************
    // method writes the differences between xml files all to one HTML file
    // added by PN
    //*****************************************************************************************************
    private void doBulkComparison()
    {
        //get the output file name
        String fileName = destFolder+ "/" + "Bulk.html";
        System.out.println("INFO: Creating file named: " + fileName);
        String fileName_summary = destFolder+ "/" + "Bulk_summary.html";
        System.out.println("INFO: Creating file named: " + fileName_summary);

        try
        {
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(fileName),encoding);
            OutputStreamWriter os_summary = new OutputStreamWriter(new FileOutputStream(fileName_summary),encoding);

            //write the beginning of HTML page
            PrintWriter writer = new PrintWriter(os);
            PrintWriter writer_summary = new PrintWriter(os_summary);
            printHTMLStart(writer);
            printInfo(writer);
            printHTMLStart(writer_summary);

            //not all platforms have files for all locales so first build a locale superset
            //loop thru locale files from each folder, each folder contains a certain number of locales
            //build a HashSet superset
            File localeDir = null;
            String [] fileList;
            TreeSet localeTreeSet = new TreeSet();  //use TreeSet for locales in alphabetical order
            for (int i = 0; i < m_PlatformFolderVect.size(); i++)
            {
                localeDir = new File((String) m_PlatformFolderVect.elementAt(i));
                fileList = localeDir.list();
                for (int j=0; j<fileList.length; j++)
                {
                    if (fileList[j].endsWith(".xml"))
                    {
                        //need to exclude root.xml and supplementalData.xml
                        if ((fileList[j].compareTo("root.xml")==0)
                        || (fileList[j].compareTo("supplementalData.xml")==0))
                            continue;

                        //exclude common if -diff_ref_common option chosen by user
                        //as common will only be shown as a reference if there are differences between locales for other platforms
                        if ((m_iOptions & OPT_DIFF_REF_COMMON) != 0)
                        {
                            String platform = (String) m_PlatformVect.elementAt(i);
                            if (platform.compareTo(COMMON)==0)
                                continue;
                        }

                        //entries are only added to TreeSets if not already there
                        localeTreeSet.add(fileList[j]);
                        //     System.out.println (j + " adding " + fileList[j] + " to super set for platform " + (String) m_PlatformFolderVect.elementAt(i) );
                    }
                }
            }

            //      System.out.println(" size of locale set = " + localeTreeSet.size());
            //      System.out.println(" number of platforms = " + m_PlatformFolderVect.size() + "(" + m_PlatformVect.size() + ")");

            //loop thru all locales
            Object[] localeArray = localeTreeSet.toArray();
            int i=0;
            for (i=0; i < localeArray.length; i++)
            {
                String platforms_with_this_locale = "";

                String localeFile = (String)localeArray[i];  //locale file name without path
                //class member localeStr used for writing to html
                localeStr  = localeFile.substring(0, localeFile.indexOf('.'));
                System.out.println("INFO: locale : " + localeStr);

                //add entry to CompareMap for any platforms having an xml file for the locale in question
                for (int j = 0; j < m_PlatformFolderVect.size(); j++)
                {
                    localeDir = new File((String) m_PlatformFolderVect.elementAt(j));
                    fileList = localeDir.list();
                    for (int k=0; k < fileList.length; k++)
                    {
                        if (fileList[k].compareTo(localeFile)==0)  //test for 2 matching xml filenames
                        {
                            String key = (String) m_PlatformVect.elementAt(j); //should use hashtable to link m_PlatformVect and m_PlatformFolderVect
                            String xmlFileName = localeDir + "/" + localeArray[i];
                            //       System.out.println(i + " " + j + " " + k + " adding " + xmlFileName + " to compareMap at key " + key);
                            addToCompareMap(xmlFileName, key);

                            if (!(((m_iOptions & OPT_DIFF_REF_COMMON) !=0)
                                && (key.compareTo(COMMON)==0)))
                            {
                                platforms_with_this_locale += key;
                                platforms_with_this_locale += ",  ";
                            }
                        }
                    }
                }
                // System.out.println("size of compareMap " + compareMap.size());

                //print locale info and table header for this locale
                printHTMLLocaleStart(writer, i, platforms_with_this_locale);
                printTableHeaderForDifferences(writer);

                //now do the comparison for a specific locale
                walkCompareMap(writer, localeStr, platforms_with_this_locale);

                //clear the compareMap before starting next locale
                compareMap.clear();

                //finish html table
                printHTMLLocaleEnd(writer);

            }  //end outer for loop on locales

            //print summary data to html summary file
            printLocaleSummaryToHTML (writer_summary);
            printAccumulatedResultsToHTML(writer_summary);

            printHTMLEnd(writer, i);
            printHTMLEnd(writer_summary, i);
        }
        catch ( Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("INFO: Finished writing file named: " + fileName);
        System.out.println("INFO: Finished writing file named: " + fileName_summary);
    }

    // added by PN
    private void printHTMLStart(PrintWriter writer)
    {
       // System.out.println("INFO: Creating the comparison chart ");

        writer.print("<html>\n"+
        "    <head>\n"+
        "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"+
        "    </head>\n"+
        "    <style>\n"+
        "         <!--\n" +
        "         table        { border-spacing: 0; border-collapse: collapse; width:100%; \n" +
        "                        border: 1px solid black }\n" +
        "         td, th       { border-spacing: 0; border-collapse: collapse; color: black; \n" +
        "                        vertical-align: top; border: 1px solid black }\n" +
        "         -->\n" +
        "     </style>"+
        "     <body bgcolor=\"#FFFFFF\"> \n" +
        "        <p><b>LOCALE DATA AUDIT</b></p>");

        writer.print("        <p>Created on: " + cal.getTime() +"</p>\n");
    }

    private void printInfo(PrintWriter writer)
    {
        if (((m_iOptions & OPT_DIFF_REF_COMMON) != 0)
        || ((m_iOptions & OPT_DIFF) != 0))
        {
            writer.print("        <p>locale elements where there is a difference between at least two platforms are shown. \n" +
            "If a locale element is the same across all platforms it is not shown </p>");
        }

        if ((m_iOptions & OPT_DIFF_REF_COMMON) != 0)
            writer.print("<p>   Common data is shown for reference purposes only and is not part of the comparison</p>\n");

    }

    // added by PN
    private void printHTMLEnd(PrintWriter writer, int iTotalNumLocales)
    {
        writer.print("<p>&nbsp;</p>");
        writer.print("<p>&nbsp;</p>");
        writer.print("          <p><b>SUMMARY : </b></p>");
        String platforms = "";
        for (int i=0; i < m_PlatformVect.size(); i++)
        {
            if (((m_iOptions & OPT_DIFF_REF_COMMON) != 0)
            &&(m_PlatformVect.elementAt(i).equals(COMMON)))
            {
                continue;
            }
            platforms += m_PlatformVect.elementAt(i);
            platforms += ", ";
        }

        writer.print("          <p><b>Platforms compared : "+ platforms + "</b></p>");
        writer.print("          <p><b>Total Number of locales audited : " +  iTotalNumLocales + "</b></p>" +
        "           <p><b>Total Number of conflicting locale data across all locales : " + serialNumber + "</b></p>" +
        "           <p><b>Number of locale elements where a conflict was found for at least one locale : " + m_iTotalConflictingElements + "</b></p>" +
        "           <p><b>Number of locale elements where no conflicts were found for any locale having this element : " + m_iTotalNonConflictingElements + "</b></p>" +
        "    </body>\n"+
        "</html>\n");
        writer.flush();

        writer.flush();
    }

    // added by PN
    private void printHTMLLocaleStart(PrintWriter writer, int iLocaleCounter, String platforms_with_this_locale)
    {
        ULocale locale = new ULocale(localeStr);
        String displayLang = locale.getDisplayLanguage();
        String dispCountry = locale.getDisplayCountry();
        String dispVariant = locale.getDisplayVariant();
        String displayName = localeStr+" ("+displayLang+"_"+dispCountry;
        if(dispVariant.length()>0)
        {
            displayName += "_"+dispVariant+") ";
        }else
        {
            displayName += ") ";
        }

        writer.print(
        "        <p><b>" + iLocaleCounter + "&nbsp;&nbsp;&nbsp;" +displayName+
        //   "<a href=\"http://oss.software.ibm.com/cgi-bin/icu/lx/en/?_="+localeStr+"\">Demo</a>, "+
        //   "<a href=\"../comparison_charts.html\">Cover Page</a>, "+
        //   "<a href=\"./index.html\">Index</a>, "+
        //   "<a href=\"../collation_diff/"+localeStr+"_collation.html\">Collation</a> "+
        "</b>" +
        "<b>&nbsp;&nbsp;&nbsp; platforms with this locale : " + platforms_with_this_locale + "</b></p>\n" +
        "        <table>\n");
    }

    // added by PN
    private void printHTMLLocaleEnd(PrintWriter writer)
    {
        writer.print( "        </table>\n");
    }

    // added by PN
    private void walkCompareMap(PrintWriter writer, String locale, String platforms)
    {
        SummaryData summData = new SummaryData ();

        // walk down the compare map and print the data
        Iterator iter = compareMap.keySet().iterator();
        while(iter.hasNext())
        {
            Object obj = iter.next();
            CompareElement element;
            if(obj != null)
            {
                Object value = compareMap.get(obj);
                if(value instanceof CompareElement)
                {
                    element = (CompareElement)value;
                }else
                {
                    throw new RuntimeException("The object stored in the compare map is not an instance of CompareElement");
                }

                boolean bIsEqual = true;
                if ((m_iOptions & OPT_DIFF) !=0)
                {
                    bIsEqual = printDifferentValues(element,writer);
                    AddToAccumulatedResultsMap((String)obj, element, localeStr, bIsEqual);
                }
                else if((m_iOptions & OPT_DIFF_REF_COMMON)!=0)
                {
                    bIsEqual = printDifferentValuesWithRef(element,writer);
                    AddToAccumulatedResultsMap((String)obj, element, localeStr, bIsEqual);
                }
                else
                {
                    printValue(element,writer);
                }

                if (bIsEqual == false)
                    summData.m_iNumConflictingElements++;

            }else
            {
                throw new RuntimeException("No objects stored in the compare map!");
            }
        }
        summData.m_szPlatforms = platforms;
        m_LocaleSummaryDataMap.put(locale, summData);

    }

    //PN added
    private void AddToAccumulatedResultsMap(String id, CompareElement element, String locale, boolean bIsEqual)
    {
        if (element == null)
            return;

        Object obj = m_AccumulatedResultsMap.get(id);
        AccumulatedResults ad;
        if(obj==null)
        {
            //  System.out.println("id = " + id);

            //add a new entry, there's none there with this key
            ad = new AccumulatedResults();
            ad.index = element.index;
            ad.node = element.node;
            ad.parentNode = element.parentNode;
            ad.type = element.type;
            if (bIsEqual == false)
                ad.localeVectDiff.add(locale);
            else
                ad.localeVectSame.add(locale);
            m_AccumulatedResultsMap.put(id, ad);
        }
        else
        {
            if(obj instanceof AccumulatedResults)
            {
                ad = (AccumulatedResults) obj;
                if((!ad.index.equals(element.index)) ||
                (!ad.node.equals(element.node)) ||
                (!ad.parentNode.equals(element.parentNode))) // ||
          //      (!ad.type.equals(element.type)))  type can be null so don't ceck its value
                {
                    throw new RuntimeException("The retrieved object is not the same as the one trying to be saved");
                }
                else
                {
                    if (bIsEqual == false)
                        ad.localeVectDiff.add(locale);
                    else
                        ad.localeVectSame.add(locale);
                }
            }
            else
            {
                throw new RuntimeException("The object stored in the compareMap is not a CompareElement object!");
            }
        }
    }

    private void printAccumulatedResultsToHTML(PrintWriter writer)
    {
        writer.print("<p>&nbsp;</p>");
        writer.print("<p>&nbsp;</p>");
        writer.print("<p><b>Table below shows the number of locales where conflicts did and didn't occur on a per locale element basis");
        writer.print("&nbsp;&nbsp; (For brevity, locale elements where no conflicts were detected for any locale are not shown) </b></p>");
        writer.print("<p></p>");
        writer.print("      <table width=\"700\">\n");
        writer.print(  "            <tr>\n" +
        "                <th width=5%>N.</th>\n"+
        "                <th width=10%>ParentNode</th>\n"+
        "                <th width=10%>Name</th>\n"+
        "                <th width=10%>ID</th>\n" +
        "                <th width=10%># of non-conflicting locales</th>" +
        "                <th width=10%># of conflicting locales</th>" +
        "                <th width=45%>Locales where conflicts were found</th>" +
        "            </tr>\n");

        // walk down the cm_AccumulateDifferenceMap and print the data
        Iterator iter = m_AccumulatedResultsMap.keySet().iterator();
        //  System.out.println ("size = " +  m_AccumulateDifferenceMap.size());

        int iCounter = 0;
        while(iter.hasNext())
        {
            Object obj = iter.next();
            AccumulatedResults ad;
            if(obj != null)
            {
                Object value = m_AccumulatedResultsMap.get(obj);
                if(value instanceof AccumulatedResults)
                {
                    ad = (AccumulatedResults)value;
                }else
                {
                    throw new RuntimeException("The object stored in the AccumulateDifferencesMap is not an instance of AccumulateDifferences");
                }

                //only print locale elements where differences occurred
                if (ad.localeVectDiff.size() > 0)
                {
                    m_iTotalConflictingElements++;
                    writer.print("            <tr>\n");
                    writer.print("                <td>"+(iCounter++)+"</td>\n");
                    writer.print("                <td>"+ad.parentNode+"</td>\n");
                    writer.print("                <td>"+ad.node+"</td>\n");
                    writer.print("                <td>"+ad.index+"</td>\n");
                    writer.print("                <td>"+ad.localeVectSame.size()+"</td>\n");
                    writer.print("                <td>"+ad.localeVectDiff.size()+"</td>\n");
                    String locales = "";
                    for (int i=0; i < ad.localeVectDiff.size(); i++)
                    {
                        locales += ad.localeVectDiff.elementAt(i);
                        locales += ", ";
                    }
                    writer.print("                <td>"+locales+"</td>\n");
                    writer.print("            </tr>\n");
                }
                else
                {
                    m_iTotalNonConflictingElements++;
                }

            }else
            {
                throw new RuntimeException("No objects stored in the AccumulateDifferencesMap!");
            }
        }

        writer.print("      </table>\n");

    }

    private void printLocaleSummaryToHTML(PrintWriter writer)
    {
        writer.print("<p>&nbsp;</p>");
        writer.print("<p>&nbsp;</p>");
        writer.print("<p><b>Table below shows the number of conflicting elements on a per locale basis\n</b></p>");
        writer.print("<p></p>");
        writer.print("      <table width=\"700\">\n");
        writer.print(  "            <tr>\n" +
        "                <th width=5%>N.</th>\n"+
        "                <th width=20%>Locale</th>\n"+
        "                <th width=40%>Platforms With This Locale</th>\n"+
        "                <th width=35%># of elements where a conflict was found</th>\n"+
        "            </tr>\n");

        // walk down the cm_AccumulateDifferenceMap and print the data
        Iterator iter = m_LocaleSummaryDataMap.keySet().iterator();
        int iCounter =0;
        while(iter.hasNext())
        {
            Object obj = iter.next();
            SummaryData summData;
            if(obj != null)
            {
                Object value = m_LocaleSummaryDataMap.get(obj);
                if(value instanceof SummaryData)
                {
                    summData = (SummaryData)value;
                }else
                {
                    throw new RuntimeException("The object stored in the AccumulateDifferencesMap is not an instance of AccumulateDifferences");
                }

                    writer.print("            <tr>\n");
                    writer.print("                <td>"+(iCounter++)+"</td>\n");
                    writer.print("                <td>"+(String)obj+"</td>\n");
                    writer.print("                <td>"+summData.m_szPlatforms+"</td>\n");
                    writer.print("                <td>"+summData.m_iNumConflictingElements+"</td>\n");
                    writer.print("            </tr>\n");
            }else
            {
                throw new RuntimeException("No objects stored in the AccumulateDifferencesMap!");
            }
        }

        writer.print("      </table>\n");
    }

    private void getCVSVersion()
    {
        ourCvsVersion = LDMLUtilities.getCVSVersion(goldFileName);
    }

} //end of class definition/declaration
