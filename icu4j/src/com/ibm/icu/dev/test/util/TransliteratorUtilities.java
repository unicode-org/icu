//##header
/*
 *******************************************************************************
 * Copyright (C) 2002-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.io.IOException;

import com.ibm.icu.text.Transliterator;
//#if defined(FOUNDATION10) || defined(J2SE13)
//##import com.ibm.icu.dev.test.TestUtil;
//#endif

public class TransliteratorUtilities {
    public static boolean DEBUG = false;

    public static void registerTransliteratorFromFile(String dir, String id) {
        try {
            String filename = id.replace('-', '_') +  ".txt";
            String rules = getFileContents(dir, filename);
            Transliterator t;
            int pos = id.indexOf('-');
            String rid;
            if (pos < 0) {
                rid = id + "-Any";
                id = "Any-" + id;
            } else {
                rid = id.substring(pos+1) + "-" + id.substring(0, pos);
            }
            t = Transliterator.createFromRules(id, rules, Transliterator.FORWARD);
            Transliterator.unregister(id);
            Transliterator.registerInstance(t);

            /*String test = "\u049A\u0430\u0437\u0430\u049B";
            System.out.println(t.transliterate(test));
            t = Transliterator.getInstance(id);
            System.out.println(t.transliterate(test));
            */

            t = Transliterator.createFromRules(rid, rules, Transliterator.REVERSE);
            Transliterator.unregister(rid);
            Transliterator.registerInstance(t);
            if (DEBUG) System.out.println("Registered new Transliterator: " + id + ", " + rid);
        } catch (IOException e) {
//#if defined(FOUNDATION10) || defined(J2SE13)
//##        throw (IllegalArgumentException) new IllegalArgumentException("Can't open " + dir + ", " + id+" "+ e.getMessage());
//#else
            throw (IllegalArgumentException) new IllegalArgumentException("Can't open " + dir + ", " + id).initCause(e);
//#endif
        }
    }

    /**
     * 
     */
    public static String getFileContents(String dir, String filename) throws IOException {
//#if defined(FOUNDATION10) || defined(J2SE13)
//##        BufferedReader br = TestUtil.openUTF8Reader(dir, filename);
//#else
        BufferedReader br = BagFormatter.openUTF8Reader(dir, filename);
//#endif 
        StringBuffer buffer = new StringBuffer();
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (line.length() > 0 && line.charAt(0) == '\uFEFF') line = line.substring(1);
            buffer.append(line).append("\r\n");
        }
        br.close();
        return buffer.toString();
         
    }

    private static final String BASE_RULES =
        ":: (hex-any/xml);" +
        ":: (hex-any/xml10);" + 
        "'<' > '&lt;' ;" +
        "'<' < '&'[lL][Tt]';' ;" +
        "'&' > '&amp;' ;" +
        "'&' < '&'[aA][mM][pP]';' ;" +
        "'>' < '&'[gG][tT]';' ;" +
        "'\"' < '&'[qQ][uU][oO][tT]';' ; " +
        "'' < '&'[aA][pP][oO][sS]';' ; ";

    private static final String CONTENT_RULES =
        "'>' > '&gt;' ;";

    private static final String HTML_RULES = BASE_RULES + CONTENT_RULES + 
        "'\"' > '&quot;' ; ";

    private static final String HTML_RULES_CONTROLS = HTML_RULES + 
        ":: [[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]] hex/unicode ; ";

    private static final String HTML_RULES_ASCII = HTML_RULES + 
        ":: [[:C:][:^ASCII:]] any-hex/xml ; ";

    private static final String XML_RULES = HTML_RULES +
        "'' > '&apos;' ; "
;
    
    /*
The ampersand character (&) and the left angle bracket (<) MUST NOT appear 

in their literal form, except when used as markup delimiters, or within a 

comment, a processing instruction, or a CDATA section. If they are needed 

elsewhere, they MUST be escaped using either numeric character references or 

the strings "&amp;" and "&lt;" respectively. The right angle bracket (>) MAY 

be represented using the string "&gt;", and MUST, for compatibility, be 

escaped using either "&gt;" or a character reference when it appears in the string 

"]]>" in content, when that string is not marking the end of a CDATA section.

In the content of elements, character data is any string of characters which does 

not contain the start-delimiter of any markup and does not include the 

CDATA-section-close delimiter, "]]>". In a CDATA section, character data is 

any string of characters not including the CDATA-section-close delimiter, 

"]]>".

To allow attribute values to contain both single and double quotes, the 

apostrophe or single-quote character (') MAY be represented as "&apos;", and 

the double-quote character (") as "&quot;".


     */
    
    public static final Transliterator toXML = Transliterator.createFromRules(
            "any-xml", XML_RULES, Transliterator.FORWARD);
    public static final Transliterator fromXML = Transliterator.createFromRules(
            "xml-any", XML_RULES, Transliterator.REVERSE);
    public static final Transliterator toHTML = Transliterator.createFromRules(
            "any-html", HTML_RULES, Transliterator.FORWARD);
    public static final Transliterator toHTMLControl = Transliterator.createFromRules(
            "any-html", HTML_RULES_CONTROLS, Transliterator.FORWARD);
    public static final Transliterator toHTMLAscii = Transliterator.createFromRules(
            "any-html", HTML_RULES_ASCII, Transliterator.FORWARD);
    public static final Transliterator fromHTML = Transliterator.createFromRules(
            "html-any", HTML_RULES, Transliterator.REVERSE);
}
