/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/testParser.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

/** Simple Test program for XMLParse
 */
import java.io.*;
import java.util.*;

public class testParser implements XMLParseTypes {
    public static final String BASE_DIR = "C:\\Documents and Settings\\Davis\\My Documents\\UnicodeData\\UNIDATA 3.0.1\\";
    public static final boolean VERBOSE = false;

    private static final String testFile = BASE_DIR + "UCD-Main.xml"; // "test.xml"; // BASE_DIR + "UCD-Main.xml";

    public static void main (String[] args) throws Exception {
        //test1();
        //test2();
        test3();
    }

    public static void test1() throws Exception {
        XMLParse xml = new XMLParse(testFile, new char[1000]);
        for (int i = 0; i < 100000; ++i) {
            byte kind = xml.next();
            if (kind == DONE) break;
            String value = xml.getValue();
            int quoteFlags = QUOTE_IEBUG | QUOTE_NON_ASCII | (kind != TEXT ? QUOTE_TABCRLF : 0);
            String qValue = XMLParse.quote(value, quoteFlags);
            if (VERBOSE) System.out.println(kindNames[kind] + ", \"" + value + "\", \"" + qValue + "\"");
            else {
                switch (kind) {
                    case ELEMENT_TAG: System.out.print('<' + qValue); break;
                    case ELEMENT_TAG_SLASH: System.out.print("</" + qValue); break;
                    case ELEMENT_TAG_COMMENT: System.out.print("<" + qValue); break;
                    case ELEMENT_TAG_QUESTION: System.out.print("<?" + qValue); break;

                    case END_ELEMENT: System.out.print(">"); break;
                    case END_ELEMENT_COMMENT: System.out.print(">"); break;
                    case END_ELEMENT_SLASH: System.out.print("/>"); break;
                    case END_ELEMENT_QUESTION: System.out.print("?>"); break;

                    case ATTRIBUTE_TAG: System.out.print(" " + qValue + "="); break;
                    case ATTRIBUTE_VALUE: System.out.print("\"" + qValue + "\""); break;

                    case TEXT: System.out.print(qValue); break;

                    default: throw new Exception("Unknown KIND");
                }
            }
        }
    }

    static final int NORMAL_QUOTE = QUOTE_NON_ASCII | QUOTE_IEBUG | QUOTE_TABCRLF;

    static void test2() throws Exception {

        PrintWriter log = Utility.openPrintWriter("UCD-Extract.html");

        //int fieldCount = 4;
        //int width = 100/fieldCount;
        //int first = width + 100 - width*fieldCount;
        try {
            log.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
            log.println("<style><!--");
            log.println("th { background-color: #99FFFF; text-align: Left; font-style: italic; font-weight: bold }");
            log.println("table { page-break-after: always }");
            log.println("--></style>");

            log.println("<title>Extract from UCD</title>");
            log.println("</head><body>");

            String tableHead = "<table border='1' width='100%' cellpadding='4'><tr>"
                + "<th width='20'>Code</th>"
                + "<th width='20'>Char</th>"
                + "<th width='20'>GC</th>"
                + "<th width='50%'>Props</th>"
                + "<th width='50%'>Name</th></tr></tr>";
            log.println(tableHead);

            XMLParse xml = new XMLParse(BASE_DIR + "UCD-Main.xml", new char[1000]);
            boolean recordingChar = false;
            int topByte = 0;
            int printByte = 0;
            Map data = new TreeMap();
            String lastTag = "";

            for (int line = 0; ; ++line) {
                byte kind = xml.next();
                if (kind == DONE) break;
                String value = xml.getValue();
                switch (kind) {
                    case ELEMENT_TAG:
                        recordingChar = value.equals("e");
                        break;

                    case ATTRIBUTE_TAG:
                        if (!recordingChar) break;
                        lastTag = value;
                        break;

                    case ATTRIBUTE_VALUE:
                        if (!recordingChar) break;
                        data.put(lastTag, value);
                        break;

                    case END_ELEMENT:
                    case END_ELEMENT_SLASH:
                        if (!recordingChar) break;
                        recordingChar = false;

                        // get data

                        String ch = (String)data.get("c");
                        ch = fixHack(ch);
                        String name = (String)data.get("n");
                        if (name == null) name = "<computed>";
                        String props = (String)data.get("xs");
                        if (props == null) props = "\u00A0";
                        String gc = (String)data.get("gc");
                        if (gc == null) gc = "Lo";

                        // split tables
                        int code = UTF32.char32At(ch, 0);
                        if ((topByte & ~0x1F) != (code & ~0x1F)) {
                            log.println("</table><br>");
                            log.println(tableHead);
                            topByte = code;
                            if ((printByte & ~0xFF) != (code & ~0xFF)) {
                                System.out.println("Printing table for " + XMLParse.hex(topByte,2));
                                printByte = code;
                            }
                        }

                        // draw line

                        log.println("<tr><td>" + XMLParse.hex(code,4) +
                            "</td><td>" + XMLParse.quote(ch,NORMAL_QUOTE) +
                            "</td><td>" + XMLParse.quote(gc,NORMAL_QUOTE) +
                            "</td><td>" + XMLParse.quote(props,NORMAL_QUOTE) +
                            "</td><td>" + XMLParse.quote(name,NORMAL_QUOTE) + "</td></tr>");

                        // clear storage
                        data.clear();
                        break;

                }
            }
            log.println("</table></body></html>");
        } finally {
            log.close();
        }
    }

    static void test3() throws Exception {
        PrintWriter log = new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(BASE_DIR + "CaseFoldingDraft3.txt"),
                "UTF8"),
            32*1024));

        try {
            collect(log, "Other_Math");
            collect (log, "Other_Alphabetic");
            collect (log, "Other_Composite");
            //int fieldCount = 4;
            //int width = 100/fieldCount;
            //int first = width + 100 - width*fieldCount;
        } finally {
            log.close();
        }
    }

        static final void collect(PrintWriter log, String prop)  throws Exception {
            XMLParse xml = new XMLParse(BASE_DIR + "UCD-Main.xml", new char[1000]);
            //boolean recordingChar = false;
            //int topByte = 0;
            //int printByte = 0;
            //Map data = new TreeMap();
            String lastTag = "";
            String lastChar = "";
            String lastName = "";
            String lastCat = "";
            int startChar = -1;
            int endChar = -2;
            String startName = "";
            String startCat = "";

            for (int line = 0; ; ++line) {
                if ((line % 10000) == 0) System.err.println("Item " + line);
                byte kind = xml.next();
                if (kind == DONE) break;
                String value = xml.getValue();
                switch (kind) {
                    case ATTRIBUTE_TAG:
                        lastTag = value;
                        break;

                    case ATTRIBUTE_VALUE:
                        if (lastTag.equals("c")) lastChar = value;
                        else if (lastTag.equals("n")) lastName = value;
                        else if (lastTag.equals("gc")) lastCat = value;
                        else if (lastTag.equals("xs") && value.indexOf(prop) >= 0) {
                            lastChar = fixHack(lastChar);
                            int ch = UTF32.char32At(lastChar,0);
                            if (ch == endChar + 1) endChar = ch;
                            else {
                                //FDD0; FDEF; Noncharacter_Code_Point; # XX;    32;
                                if (endChar >= 0) log.println(Utility.hex(startChar, 4) + "; "
                                    + (endChar == startChar ? "    " : Utility.hex(endChar, 4))
                                    + "; " + prop
                                    + "; # " + startCat
                                    + "; " + (endChar-startChar+1)
                                    + "; " + startName
                                    + (endChar == startChar ? "" : "..."));
                                startChar = endChar = ch;
                                startName = lastName;
                                startCat = lastCat;
                            }
                        }
                        break;
                }
            }
            if (endChar >= 0) log.println(Utility.hex(startChar, 4) + "; "
                                    + (endChar == startChar ? "    " : Utility.hex(endChar, 4))
                                    + "; " + prop
                                    + "; # " + startCat
                                    + "; " + (endChar-startChar+1)
                                    + "; " + startName
                                    + (endChar == startChar ? "" : "..."));
        }

    static void test4() throws Exception {
        PrintWriter log = new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(BASE_DIR + "CaseFoldingDraft3.txt"),
                "UTF8"),
            32*1024));

        //int fieldCount = 4;
        //int width = 100/fieldCount;
        //int first = width + 100 - width*fieldCount;
        try {
            XMLParse xml = new XMLParse(BASE_DIR + "UCD-Main.xml", new char[1000]);
            boolean recordingChar = false;
            //int topByte = 0;
            //int printByte = 0;
            Map data = new TreeMap();
            String lastTag = "";

            for (int line = 0; ; ++line) {
                if ((line % 10000) == 0) System.err.println("Item " + line);
                byte kind = xml.next();
                if (kind == DONE) break;
                String value = xml.getValue();
                switch (kind) {
                    case ELEMENT_TAG:
                        recordingChar = value.equals("e");
                        break;

                    case ATTRIBUTE_TAG:
                        if (!recordingChar) break;
                        lastTag = value;
                        break;

                    case ATTRIBUTE_VALUE:
                        if (!recordingChar) break;
                        data.put(lastTag, value);
                        break;

                    case END_ELEMENT:
                    case END_ELEMENT_SLASH:
                        if (!recordingChar) break;
                        recordingChar = false;

                        // get data

                        String ch = (String)data.get("c");
                        ch = fixHack(ch);

                        String name = (String)data.get("n");
                        if (name == null) name = "<computed>";

                        String lc = (String)data.get("lc");
                        if (lc == null) lc = ch;

                        String fc = (String)data.get("fc");
                        if (fc == null) fc = (String)data.get("sl");
                        if (fc == null) fc = lc;

                        if (fc.equals(ch)) continue;

                        if (fc.length() == 1) {
                            log.println(Utility.hex(ch, " ") + "; C; " + Utility.hex(fc, " ") + "; # " + name);
                        } else {
                            log.println(Utility.hex(ch, " ") + "; F; " + Utility.hex(fc, " ") + "; # " + name);
                            if (!lc.equals(ch)) {
                                log.println(Utility.hex(ch, " ") + "; S; " + Utility.hex(lc, " ") + "; # " + name);
                            }
                        }

                        // clear storage
                        data.clear();
                        break;

                }
            }
        } finally {
            log.close();
        }
    }

    static final String fixHack(String s) {
        StringBuffer result = new StringBuffer();
        char last = '\u0000';
        int position = -1;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (position > 0) {
                if (c == ';') {
                    int x = Integer.parseInt(s.substring(position,i),16);
                    result.append(UTF32.valueOf32(x));
                    position = -1;
                }
            } else {
                if (last == '#' && c == 'x') {
                    result.setLength(result.length()-1); // remove '#'
                    position = i+1;
                } else {
                    result.append(c);
                }
            }
            last = c;
        }
        if (result != null) return result.toString();
        return s;
    }
}