/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package com.ibm.icu.dev.tool.cldr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

//import javax.xml.parsers.*;

/**
 * This is a simple class that walks through the CLDR hierarchy and does 2 things.
 * First, it determines all the places where the CLDR is not minimal: where there
 * are redundancies with inheritance. It generates new files in the target directory.
 * Second, it gathers together all the items from all the locales that share the
 * same element chain, and thus presents a "sideways" view of the data, in files called
 * by_type/X.html, where X is a type. X may be the concatenation of more than more than
 * one element, where the file would otherwise be too large.
 * @author medavis
 */
/*
Notes:
http://xml.apache.org/xerces2-j/faq-grammars.html#faq-3
http://developers.sun.com/dev/coolstuff/xml/readme.html
http://lists.xml.org/archives/xml-dev/200007/msg00284.html
http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/DTDHandler.html
 */
public class GenerateSidewaysView {
    // debug flags
    static final boolean DEBUG = false;
    static final boolean DEBUG2 = false;
    static final boolean DEBUG_SHOW_ADD = false;
    static final boolean DEBUG_ELEMENT = false;
    static final boolean DEBUG_SHOW_BAT = false;

    static final boolean FIX_ZONE_ALIASES = true;

    private static final int
        HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        MATCH = 4,
        SKIP = 5,
        TZADIR = 6,
        NONVALIDATING = 7,
        SHOW_DTD = 8,
		TRANSLIT = 9;

    private static final String NEWLINE = "\n";

    private static final UOption[] options = {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.SOURCEDIR().setDefault("C:\\ICU4C\\locale\\common\\main\\"),
            UOption.DESTDIR().setDefault("C:\\DATA\\GEN\\cldr\\main\\"),
            UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
            UOption.create("skip", 'z', UOption.REQUIRES_ARG).setDefault("zh_(C|S|HK|M).*"),
            UOption.create("tzadir", 't', UOption.REQUIRES_ARG).setDefault("C:\\ICU4J\\icu4j\\src\\com\\ibm\\icu\\dev\\tool\\cldr\\"),
            UOption.create("nonvalidating", 'n', UOption.NO_ARG),
            UOption.create("dtd", 'w', UOption.NO_ARG),
            UOption.create("transliterate", 'y', UOption.NO_ARG),
    };
    private static String timeZoneAliasDir = null;

    public static void main(String[] args) throws SAXException, IOException {
        UOption.parseArgs(args, options);

        Matcher skipper = Pattern.compile(options[SKIP].value).matcher("");
        Matcher matcher = Pattern.compile(options[MATCH].value).matcher("");
        //matcher = Pattern.compile("(root|b).*").matcher("");
        log = BagFormatter.openUTF8Writer(options[DESTDIR].value, "log.txt");
        timeZoneAliasDir = options[TZADIR].value;
        try {
            File sourceDir = new File(options[SOURCEDIR].value);
            String[] contents = sourceDir.list();
            for (int i = 0; i < contents.length; ++i) {
                if (!contents[i].endsWith(".xml")) continue;
                if (contents[i].startsWith("supplementalData")) continue;
                if (!matcher.reset(contents[i]).matches()) continue; // debug shutoff
                if (skipper.reset(contents[i]).matches()) continue; // debug shutoff
                //System.out.println("Processing " + contents[i]);
                log.println();
                log.println("Processing " + contents[i]);
                String baseName = contents[i].substring(0,contents[i].length()-4);
                GenerateSidewaysView temp = getCLDR(baseName, !options[NONVALIDATING].doesOccur);
                // if (baseName.equals("zh_TW")) baseName = "zh_Hant_TW";
                // if (baseName.equals("root")) temp.addMissing();
                if (options[SHOW_DTD].doesOccur) temp.writeDTDCheck();
                temp.writeTo(options[DESTDIR].value, baseName);
                generateBat(options[SOURCEDIR].value, baseName + ".xml", options[DESTDIR].value, baseName + ".xml");
                sidewaysView.putData(temp.data, baseName);
                log.flush();
            }
            sidewaysView.showCacheData();
        } finally {
            log.close();
            System.out.println("Done");
       }
    }

    /**
     *
     */
    private void writeDTDCheck() {
        DEFAULT_DECLHANDLER.checkData();
    }

    static Collator DEFAULT_COLLATION = null;

    static final Set IGNOREABLE =  new HashSet(Arrays.asList(new String[] {
            "draft",
            //"references",
            //"standard"
    }));

    static final Set IGNORELIST = new HashSet(Arrays.asList(new String[] {
            "draft", "standard", "references", "validSubLocales"
    }));

    static final Set LEAFNODES = new HashSet(Arrays.asList(new String[] {
                   "alias", "default", "firstDay", "mapping", "measurementSystem",
                   "minDays", "orientation", "settings", "weekendStart", "weekendEnd"
   }));

    static Collator getDefaultCollation() {
        if (DEFAULT_COLLATION != null) return DEFAULT_COLLATION;
        RuleBasedCollator temp = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
        temp.setStrength(Collator.IDENTICAL);
        temp.setNumericCollation(true);
        DEFAULT_COLLATION = temp;
        return temp;
    }

    public static class TimeZoneAliases {
        static Map map = null;
        static void init() {
            map = new HashMap();
            try {
                BufferedReader br = BagFormatter.openUTF8Reader(timeZoneAliasDir, "timezone_aliases.txt");
                String[] pieces = new String[2];
                while (true) {
                    String line = br.readLine();
                    if (line == null) break;
                    Utility.split(line,';', pieces);
                    map.put(pieces[0].trim(), pieces[1].trim());
                }
                br.close();
                map.put("","EMPTY-REMOVE");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static String get(Object id) {
            if (map == null) init();
            return (String) map.get(id);
        }
    }

    static MapComparator elementOrdering = new MapComparator();
    static MapComparator attributeOrdering = new MapComparator();
    {
        // hack the ordering of these two
        attributeOrdering.add("alt");
        attributeOrdering.add("draft");
    }
    static MapComparator valueOrdering = new MapComparator();

    OrderedMap data = new OrderedMap();
    MyContentHandler DEFAULT_HANDLER = new MyContentHandler();
    MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler();
    XMLReader xmlReader;

    /*SAXParser SAX;
    {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);

            SAX = factory.newSAXParser();
        } catch (Exception e) {
            throw new IllegalArgumentException("can't start");
        }
    }
    */

    static PrintWriter log;

    /**
     *
     */
    /*private void addMissing() {
        String[] currencies = getCodes(new ULocale("en","",""), "Currencies");
        //<ldml><numbers><currencies><currency type="AUD"><displayName>
        addCurrencies(currencies, "displayName");
        addCurrencies(currencies, "symbol");
    }
    */

    /*
    private void addCurrencies(String[] currencies, String lastElement) {
        ElementChain temp = new ElementChain();
        temp.push("ldml",null).push("numbers",null).push("currencies",null)
            .push("currency",null).push(lastElement,null,null);
        for (int i = 0; i < currencies.length; ++i) {
            temp.setAttribute("currency","type",currencies[i]);
            String value = (String) data.get(temp);
            if (value != null) continue;
            putData(temp, currencies[i]);
        }
    }
    */

    // UGLY hack
    private static String[] getCodes(ULocale locale, String tableName) {
        // TODO remove Ugly Hack
        // get stuff
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(locale);
        ICUResourceBundle table = bundle.getWithFallback(tableName);
        // copy into array
        ArrayList stuff = new ArrayList();
        for (Enumeration keys = table.getKeys(); keys.hasMoreElements();) {
            stuff.add(keys.nextElement());
        }
        String[] result = new String[stuff.size()];
        return (String[]) stuff.toArray(result);
        //return new String[] {"Latn", "Cyrl"};
    }


    static Map cache = new HashMap();

    static GenerateSidewaysView getCLDR(String s, boolean validating) throws SAXException, IOException {
        GenerateSidewaysView temp = (GenerateSidewaysView)cache.get(s);
        if (temp == null) {
            temp = new GenerateSidewaysView(s, validating);
            cache.put(s,temp);
        }
        return temp;
    }

    String filename;
    GenerateSidewaysView parent = null;

    private GenerateSidewaysView(String filename, boolean validating) throws SAXException, IOException {
        this.filename = filename;
        xmlReader = createXMLReader(validating);
        // make sure the parents are loaded into cache before we are
        String parentName = getParentFilename(filename);
        if (parentName != null) {
            parent = getCLDR(parentName, validating);
        }
        //System.out.println("Creating " + filename);

        xmlReader.setContentHandler(DEFAULT_HANDLER);
        xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler",DEFAULT_HANDLER);
        if (options[SHOW_DTD].doesOccur) xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", DEFAULT_DECLHANDLER);

        readFrom(options[SOURCEDIR].value, filename);
        // walk through the map removing anything that is inherited from a parent.
        // changed so that we stop when the parent has an element
        if (parent != null) {
            Parent parentOut = new Parent();
            Map toRemove = new TreeMap();
            for (Iterator it = data.iterator(); it.hasNext();) {
                Object key = it.next();
                if (((ElementChain)key).containsElement("identity")) continue;
                EndNode value = (EndNode) data.get(key);
                Object inheritedValue = getInheritedValue(key, parentOut);
                if (value.equals(inheritedValue)) {
                    // debugging: data.get(key);
                    //getInheritedValue(key);
                    //value.equals(inheritedValue);
                    toRemove.put(key, parentOut.parent);
                }
            }
            for (Iterator it = toRemove.keySet().iterator(); it.hasNext();) {
                ElementChain key = (ElementChain)it.next();
                EndNode value = (EndNode) data.get(key);
                GenerateSidewaysView parent = (GenerateSidewaysView) toRemove.get(key);
                EndNode parentValue = (EndNode) parent.data.get(key);
                log.println("Removing " + key.toString(true, 0, Integer.MAX_VALUE) + "\t" + value);
                ElementChain parentKey = (ElementChain) parent.data.getKeyFor(key);
                log.println("\tIn " + parent.filename + ":\t" + parentKey.toString(true, 0, Integer.MAX_VALUE) + "\t"+ parentValue);
                data.remove(key);
            }
        }
    }

    static class Parent {
        GenerateSidewaysView parent;
    }
    private Object getInheritedValue(Object key, Parent parentOut) {
        if (parent == null) {
            parentOut.parent = null;
            return null;
        }
        EndNode value = (EndNode) parent.data.get(key);
        if (value != null) {
            parentOut.parent = parent;
            return value;
        }
        return parent.getInheritedValue(key, parentOut);
    }

    /*
    Set badTimezoneIDs = null;
     private void detectAliases(String filename) {

        Set problems = new TreeSet();
        for (Iterator it = data.iterator(); it.hasNext();) {
            ElementChain key = (ElementChain) it.next();
            for (int i = 0; i < key.contexts.size(); ++i) {
                Element e = (Element) key.contexts.get(i);
                if (!e.elementName.equals("zone")) continue;
                for (Iterator q = e.attributes.contents.iterator(); q.hasNext(); ) {
                    SimpleAttribute a = (SimpleAttribute)q.next();
                    if (!a.name.equals("type")) continue;
                    String other = TimeZoneAliases.get(a.value);
                    if (other != null) {
                        problems.add(a.value);
                    }
                }
            }
        }

        for (Iterator it = badTimezoneIDs.iterator(); it.hasNext();) {
            String oldOne = (String)it.next();
            String newOne = TimeZoneAliases.get(oldOne);
            log.println("Fix Timezone Alias: " + filename + "\t" + oldOne + " => " + newOne);
        }
    } */

    /*
    private void removeAll(GenerateSidewaysView temp) {
        data.removeAll(temp.data);
    }
    */

    private static String getParentFilename(String filename) {
        if (filename.equals("zh_TW")) return "zh_Hant";
        int pos = filename.lastIndexOf('_');
        if (pos >= 0) {
            return filename.substring(0,pos);
        }
        if (filename.equals("root")) return null;
        return "root";
    }

    private void writeTo(String dir, String filename) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(dir, filename + ".xml");
        out.println(this);
        out.close();
    }

    public void readFrom(String dir, String filename) throws SAXException, IOException {
        File f = new File(dir + filename + ".xml");
        System.out.println("Parsing: " + f.getCanonicalPath());
        log.println("Parsing: " + f.getCanonicalPath());
        xmlReader.parse(new InputSource(new FileInputStream(f)));
        //SAX.parse(f, DEFAULT_HANDLER);
    }

    private Set findDuplicateZoneIDs() {
        Set result = new HashSet();
        // if a set contains both EST and America/Indianapolis, remove the former
        for (Iterator it = zoneIDs.iterator(); it.hasNext();) {
            Object possibleOmission = it.next();
            Object o = TimeZoneAliases.get(possibleOmission);
            if (o == null) continue;
            if (zoneIDs.contains(o)) result.add(possibleOmission);
        }
        return result;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+NEWLINE
        + "<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.2/beta/ldml.dtd\">"+NEWLINE);

        Set duplicateZoneIDs = findDuplicateZoneIDs();
        //badTimezoneIDs = new TreeSet();

        ElementChain empty = new ElementChain();
        ElementChain old = empty;
        for (Iterator it = data.iterator(); it.hasNext();) {
            ElementChain key = (ElementChain) it.next();
            if (FIX_ZONE_ALIASES) {
                Element zoneElement = key.getElement("zone");
                if (zoneElement != null) {
                    String zoneTypeValue = zoneElement.getValue("type");
                    if (zoneTypeValue != null) {
                        if (duplicateZoneIDs.contains(zoneTypeValue)) continue;
                    }
                }
            }
            if (true) {
                // weekendEnd draft="true" time="00:00" NEVER ok
                Element zoneElement = key.getElement("weekendEnd");
                if (zoneElement != null) {
                    String zoneTypeValue = zoneElement.getValue("time");
                    if ("00:00".equals(zoneTypeValue)) {
                        log.println("BAD WEEKENDEND TIME: " + zoneTypeValue);
                    }
                }
            }
            EndNode value = (EndNode) data.get(key);
            key.writeDifference(old, value, buffer);
            old = key;
        }
        empty.writeDifference(old, null, buffer);
        writeElementComment(buffer,finalComment,0);

        return buffer.toString();
    }

    static void generateBat(String sourceDir, String sourceFile, String targetDir, String targetFile) {
        boolean needBat = true;
        try {
            BufferedReader b1 = BagFormatter.openUTF8Reader(sourceDir, sourceFile);
            BufferedReader b2 = BagFormatter.openUTF8Reader(targetDir, targetFile);
            while (true) {
                String line1 = b1.readLine();
                String line2 = b2.readLine();
                if (line1 == null && line2 == null) {
                    needBat = false;
                    break;
                }
                if (line1 == null || line2 == null) {
                    if (DEBUG_SHOW_BAT) System.out.println("*File line counts differ: ");
                    break;
                }
                if (!equalsIgnoringWhitespace(line1, line2)) {
                    if (DEBUG_SHOW_BAT) {
                        System.out.println("*File lines differ: ");
                        System.out.println("\t1\t" + line1);
                        System.out.println("\t2\t" + line2);
                    }
                    break;
                }
            }
            b1.close();
            b2.close();
            String batDir = targetDir + File.separator + "diff" + File.separator;
            String batName = targetFile + ".bat";
            if (needBat) {
                PrintWriter bat = BagFormatter.openUTF8Writer(batDir, batName);
                bat.println("\"C:\\Program Files\\Compare It!\\wincmp3.exe\" " +
                        new File(sourceDir + sourceFile).getCanonicalPath() + " " +
                        new File(targetDir + targetFile).getCanonicalPath());
                bat.close();
            } else {
                File f = new File(batDir + batName);
                if (f.exists()) {
                    if (DEBUG_SHOW_BAT) System.out.println("*Deleting old " + f.getCanonicalPath());
                    f.delete();
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

     //

    }

    static boolean equalsIgnoringWhitespace(String a, String b) {
        int i = 0;
        int j = 0;
        char c, d;
        while (true) { // don't worry about surrogates
            do {c = i < a.length() ? a.charAt(i++) : 0xFFFF;} while (UCharacter.isUWhiteSpace(c));
            do {d = j < b.length() ? b.charAt(j++) : 0xFFFF;} while (UCharacter.isUWhiteSpace(d));
            if (c != d) return false;
            if (c == 0xFFFF) return true;
        }
    }

    static class SimpleAttribute implements Comparable {
        String name;
        String value;
        SimpleAttribute(String name, String value) {
            attributeOrdering.add(name);
            valueOrdering.add(value);
            this.name = name;
            this.value = value;
        }
        public boolean equals(Object other) {
            return compareTo(other) == 0;
        }
        public int hashCode() {
            return name.hashCode() ^ value.hashCode();
        }
        public String toString() {return toString(true);}
        public String toString(boolean path) {
            if (path) {
                return "[@" + name + "='" + BagFormatter.toHTML.transliterate(value) + "']";
            } else {
                return " " + name + "=\"" + BagFormatter.toXML.transliterate(value) + "\"";
            }
        }
        public int compareTo(Object o) {
            SimpleAttribute that = (SimpleAttribute) o;
            int result;
            if ((result = attributeOrdering.compare(name, that.name)) != 0) return result;
            return valueOrdering.compare(value, that.value);
        }
    }

    Set zoneIDs = new HashSet();

    class SimpleAttributes implements Comparable {
        Set contents = new TreeSet();

        SimpleAttributes() {}

        SimpleAttributes(SimpleAttributes other, String elementName) {
            contents.clear();
            contents.addAll(other.contents);
        }

        SimpleAttributes(Attributes attributes, String elementName) {
            boolean inZone = elementName.equals("zone");
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); ++i) {
                    String name = attributes.getQName(i);
                    String value = attributes.getValue(i);

                    if (FIX_ZONE_ALIASES && inZone) {
                        if (name.equals("type")) {
                            zoneIDs.add(value);
                        }
                    }

                    // hack to removed #IMPLIED
                    if (elementName.equals("ldml")
                        && name.equals("version")) continue; // skip version
                    if (name.equals("type")
                        && value.equals("standard")) continue;

                    contents.add(new SimpleAttribute(name, value));
                    tripleData.recordData(elementName, name, value);
                }
            }
        }

        public String toString() {return toString(true, false);}
        public String toString(boolean path, boolean isZone) {
            StringBuffer buffer = new StringBuffer();
            for (Iterator it = contents.iterator(); it.hasNext();) {
                SimpleAttribute a = (SimpleAttribute) it.next();
                //SimpleAttribute a = getSkipping(it);
                //if (a == null) continue;
                //SimpleAttribute a = (SimpleAttribute)it.next();
                //if (path && IGNOREABLE.contains(a.name)) continue;
                if (isZone && a.name.equals("type")) {
                    String replacement = TimeZoneAliases.get(a.value);
                    if (replacement != null) a = new SimpleAttribute("type", replacement);
                }
                buffer.append(a.toString(path));
            }
            return buffer.toString();
        }
        public int compareTo(Object o) {
            // IGNORE draft, source, reference
            SimpleAttributes that = (SimpleAttributes) o;
            // quick check for common case
            if (contents.size() == 0 && that.contents.size() == 0) return 0;
            // compare one at a time. Stop if one element is less than another.
            Iterator it = contents.iterator();
            Iterator it2 = that.contents.iterator();
            int result;
            while (true) {
                SimpleAttribute a = getSkipping(it);
                SimpleAttribute a2 = getSkipping(it2);
                if (a == null) {
                    if (a2 == null) return 0;
                    return -1;
                }
                if (a2 == null) {
                    return 1;
                }
                if ((result = a.compareTo(a2)) != 0) return result;
            }
        }
        private SimpleAttribute getSkipping(Iterator it) {
            while (it.hasNext()) {
                SimpleAttribute a = (SimpleAttribute)it.next();
                if (!IGNOREABLE.contains(a.name)) return a;
            }
            return null;
        }

        /**
         * @param attribute
         * @param value
         */
        public SimpleAttributes set(String element, String attribute, String value) {
            for (Iterator it = contents.iterator(); it.hasNext();) {
                SimpleAttribute sa = (SimpleAttribute) it.next();
                if (sa.name.equals(attribute)) {
                    contents.remove(sa);
                    break;
                }
            }
            contents.add(new SimpleAttribute(attribute, value));
            tripleData.recordData(element, attribute, value);
            return this;
        }

        /**
         * @param attributeName
         * @return
         */
        public String getValue(String attributeName) {
            for (Iterator it = contents.iterator(); it.hasNext();) {
                SimpleAttribute sa = (SimpleAttribute) it.next();
                if (sa.name.equals(attributeName)) {
                    return sa.value;
                }
            }
            return null;
        }

        /**
         * @param attributes
         */
        public void add(SimpleAttributes attributes) {
            contents.addAll(attributes.contents);
        }

        /**
         * @param ignorelist
         */
        public void removeAttributes(Set ignorelist) {
            for (Iterator it = contents.iterator(); it.hasNext();) {
                SimpleAttribute sa = (SimpleAttribute) it.next();
                if (ignorelist.contains(sa.name)) {
                    it.remove();
                }
            }
        }
    }

    static class TripleData {
        Map elementToAttributeToValues = new TreeMap(getDefaultCollation());
        Map bigguys = null;
        static final String ANYSTRING = "[any]";

        private void recordData(String element, String attribute, String value) {
            if (!element.equals(ANYSTRING) && IGNORELIST.contains(attribute)) return;
            if (bigguys == null) init();
            // record for posterity
            Map elementToAttribute = (Map) elementToAttributeToValues.get(element);
            if (elementToAttribute == null) {
                elementToAttribute = new TreeMap(getDefaultCollation());
                elementToAttributeToValues.put(element, elementToAttribute);
            }
            Set valueSet = (Set) elementToAttribute.get(attribute);
            if (valueSet == null) {
                valueSet = new TreeSet(getDefaultCollation());
                elementToAttribute.put(attribute, valueSet);
            }
            if (value != null) valueSet.add(value);
        }

        /**
         * @throws IOException
         *
         */
        private void writeData() throws IOException {
            String fileName = "attributeList.html";
            PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value, "by_type" + File.separator + fileName);
            out.println("<html><head>");
            out.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
            out.println("<title>Element/Attribute/Value</title>");
            out.println("<link rel='stylesheet' type='text/css' href='http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/common.css'>");
            out.println("<link rel='stylesheet' type='text/css' href='by_type.css'>");
            out.println("</head><body>");
            out.println("<table>");
            for (int i = 0; i < fixList.length; ++i) {
                Object[] row = fixList[i];
                String elementItem = (String)row[0];
                if (elementItem.equals("*")) continue;
                String attributeItem = (String)row[1];
                if (attributeItem.equals("*")) continue;
                recordData(elementItem, attributeItem, null);
            }
            for (Iterator it = elementToAttributeToValues.keySet().iterator(); it.hasNext();) {
                String element = (String) it.next();
                boolean newElement = true;
                Map elementToAttribute = (Map) elementToAttributeToValues.get(element);
                Set attributeKeys = elementToAttribute.keySet();
                for (Iterator it2 = attributeKeys.iterator(); it2.hasNext();) {
                    String attribute = (String) it2.next();
                    boolean newAttribute = true;
                    Collection valueSet = getValues(element, elementToAttribute, attribute);
                    out.println("<tr>");
                    if (newElement) {
                        out.print("<td rowSpan='"
                                + attributeKeys.size()
                                + "'>" + element + "</td>");
                        out.println("<!-- " + valueSet.size() + ", "  + attributeKeys.size() + "-->");
                        newElement = false;
                    }
                    if (newAttribute) {
                        out.print("<td>" + attribute + "</td>");
                        out.println("<!-- " + valueSet.size() + "-->");
                        newAttribute = false;
                    }
                    out.print("<td>");
                    boolean newSet = true;
                    for (Iterator it3 = valueSet.iterator(); it3.hasNext();) {
                        String value = (String) it3.next();
                        if (newSet) newSet = false;
                        else out.print(", ");
                        out.print(BagFormatter.toHTML.transliterate("\"" + value + "\""));
                    }
                    out.println("</td></tr>");
                    /*
                    for (Iterator it3 = valueSet.iterator(); it3.hasNext();) {
                        String value = (String) it3.next();
                        out.println("<tr>");
                        if (newElement) {
                            out.print("<td rowSpan='"
                                    + getValueCount(elementToAttribute)
                                    + "'>" + element + "</td>");
                            out.println("<!-- " + valueSet.size() + ", "  + attributeKeys.size() + "-->");
                            newElement = false;
                        }
                        if (newAttribute) {
                            out.print("<td rowSpan='" + valueSet.size() + "'>" + attribute + "</td>");
                            out.println("<!-- " + valueSet.size() + "-->");
                            newAttribute = false;
                        }
                        out.println("<td>" + BagFormatter.toHTML.transliterate("\"" + value + "\"") + "</td></tr>");
                    }
                    */
                }
            }
            out.println("</table>");
            writeFooterAndClose(out);
        }

        Object[][] fixList = {
                {"alias", "path", "<valid XPath within locale tree>"},
                {"alias", "source", "<valid locale ID>"},
                {"day", "type", new String[] {"sun", "mon", "tue", "wed", "thu", "fri", "sat"}},
                {"era", "type", "<non-negative number>"},
                {"*", "day", new String[] {"sun", "mon", "tue", "wed", "thu", "fri", "sat"}},
                {"generation", "date", "<yyyy-MM-dd format>"},
                {"version", "number", "<n.m format>"},
                {"*", "time", "<HH:mm (00:00..24:00)>"},
                {"orientation", "*", new String[] {"left-to-right", "right-to-left", "top-to-bottom", "bottom-to-top"}},
                {"minDays", "count", new String[] {"1", "2", "3", "4", "5", "6", "7"}},
                {"language", "type", "%%%language"},
                {"script", "type", "%%%script"},
                {"territory", "type", "%%%region"},
                {"variant", "type", "%%%variant"},
                {"zone", "type", "%%%tzid"},
                {"currency", "type", "%%%currency"},
                {"calendar", "type", new String[] {
                        "buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil", "japanese"
                        ,"arabic[alias]", "civil-arabic[alias]", "thai-buddhist[alias]"
                }},
                {"measurementSystem", "type", new String[] {"metric", "US", "UK"}},
                {"type", "type", "<any type value--with appropriate key>"},
                {"type", "key", "<any element name having 'type' attribute>"},
                {"key", "type", "<any element name having 'type' attribute>"},

                {ANYSTRING, "draft", new String[] {"true", "false*"}},
                {ANYSTRING, "alt", new String[] {"proposed", "variant"}},
                {ANYSTRING, "references", "<list of references>"},
                {ANYSTRING, "standard", "<list of standards>"},
                {ANYSTRING, "validSubLocales", "<list of sub-locales>"},

                {"collation", "type", new String[] {"phonebook", "traditional", "direct", "pinyin", "stroke", "posix", "big5han", "gb2312han"}},
                {"settings", "strength", new String[] {"primary", "secondary", "tertiary", "quaternary", "identical"}},
                {"settings", "alternate", new String[] {"non-ignorable", "shifted"}},
                {"settings", "backwards", new String[] {"on", "off"}},
                {"settings", "normalization", new String[] {"on", "off"}},
                {"settings", "caseLevel", new String[] {"on", "off"}},
                {"settings", "caseFirst", new String[] {"upper", "lower", "off"}},
                {"settings", "hiraganaQuarternary", new String[] {"on", "off"}},
                {"settings", "numeric", new String[] {"on", "off"}},
                {"reset", "before", new String[] {"primary", "secondary", "tertiary"}},

                {"default", "type", "<any type value legal for one of the peer elements>"},
                {"mapping", "registry", "<any charset registry, iana preferred>"},
                {"mapping", "type", "<any valid charset from the given registry>"},

                {"abbreviationFallback", "type", new String[] {"standard", "GMT"}},
                {"preferenceOrdering", "type", "<space-delimited list of timezone IDs>"},
                {"exemplarCharacters", "type", new String[] {"standard", "auxiliary"}},

                {"decimalFormatLength", "type", new String[] {"full", "long", "medium", "short"}},
                {"scientificFormatLength", "type", new String[] {"full", "long", "medium", "short"}},
                {"currencyFormatLength", "type", new String[] {"full", "long", "medium", "short"}},
                {"percentFormatLength", "type", new String[] {"full", "long", "medium", "short"}},

                {"field", "type", new String[] {"era", "year", "month", "week", "day", "weekday", "dayperiod", "hour", "minute", "second", "zone"}},
                {"relative", "type", "<integer>"},

                {"pattern", "type", "<valid pattern for format>"},

                {"dateFormat", "type", new String[] {"standard", "<special-key>"}},
                {"timeFormat", "type", new String[] {"standard", "<special-key>"}},
                {"dateTimeFormat", "type", new String[] {"standard", "<special-key>"}},
                {"decimalFormat", "type", new String[] {"standard", "<special-key>"}},
                {"scientificFormat", "type", new String[] {"standard", "<special-key>"}},
                {"percentFormat", "type", new String[] {"standard", "<special-key>"}},
                {"currencyFormat", "type", new String[] {"standard", "<special-key>"}},
        };

        String[] pieces = new String[50];

        private void init() {
            try {
                bigguys = new HashMap();
                BufferedReader br = BagFormatter.openUTF8Reader(timeZoneAliasDir, "idList.txt");
                while (true) {
                    String line = br.readLine();
                    if (line == null) break;
                    line = line.trim();
                    if (line.startsWith("#")) continue;
                    Utility.split(line,';', pieces);
                    String tag = pieces[0].trim();
                    String id = pieces[1].trim();
                    addTagValue(tag, id);
                }
                br.close();
                // hack some extras
                addTagValue("language", "root");
                addTagValue("script", "Qaai");
                addTagValue("variant", "POSIX");
                addTagValue("variant", "REVISED");
                addTagValue("variant", "bokmal");
                addTagValue("variant", "nynorsk");
                addTagValue("variant", "aaland");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @param tag
         * @param id
         */
        private void addTagValue(String tag, String id) {
            Collection c = (Collection) bigguys.get(tag);
            if (c == null) {
                c = new TreeSet();
                bigguys.put(tag, c);
            }
            Utility.split(id, ',', pieces);
            for (int i = 0; i < pieces.length; ++i) {
                if (pieces[i].length() == 0) continue;
                c.add(pieces[i].trim());
            }
        }

        /**
         * @param elementToAttribute
         * @param attribute
         * @return
         */
        private Collection getValues(String element, Map elementToAttribute, String attribute) {
            for (int i = 0; i < fixList.length; ++i) {
                Object[] row = fixList[i];
                String elementItem = (String)row[0];
                String attributeItem = (String)row[1];
                if ((attribute.equals(attributeItem) || "*".equals(attributeItem))
                        && (element.equals(elementItem) || "*".equals(elementItem))) {
                    if (row[2] instanceof String) {
                        String valueItem = (String)row[2];
                        if (valueItem.startsWith("%%%")) {
                            //System.out.println("Substituting Values: " + element + ", " + attribute);
                            Collection result = (Collection) bigguys.get(valueItem.substring(3));
                            Set s = new TreeSet((Collection)elementToAttribute.get(attribute));
                            s.removeAll(result);
                            if (s.size() != 0) {
                                System.out.print("Warning: Missing values for " + element + ", " + attribute + ": ");
                                for (Iterator it = s.iterator(); it.hasNext(); ) {
                                    System.out.print(it.next() + " ");
                                }
                                System.out.println();
                            }
                            return result;
                        }
                        List result = new ArrayList();
                        result.add(valueItem);
                        return result;
                    }
                    return Arrays.asList((Object[])row[2]);
                }
            }
            return (Collection) elementToAttribute.get(attribute);
        }
        int getValueCount(Map elementToAttribute) {
            int result = 0;
            for (Iterator it2 = elementToAttribute.keySet().iterator(); it2.hasNext();) {
                String attribute = (String) it2.next();
                Set valueSet = (Set) elementToAttribute.get(attribute);
                result += valueSet.size();
            }
            return result;
        }
    }

    static TripleData tripleData = new TripleData();

    class Element implements Comparable {
        String elementName;
        SimpleAttributes attributes;
        String comment;

        Element(String elementName, Attributes attributes, String comment) {
            //elementOrdering.add(elementName);
            this.elementName = elementName;
            this.attributes = new SimpleAttributes(attributes, elementName);
            this.comment = comment;
        }
        /**
         * @param string
         * @param fixed
         */
        public void setAttribute(String attribute, String value) {
            attributes.set(elementName, attribute, value);
        }
        /**
         * @param string
         * @return
         */
        public String getValue(String attributeName) {
            return attributes.getValue(attributeName);
        }

        Element(Element other) {
            //elementOrdering.add(elementName);
            this.elementName = other.elementName;
            this.attributes = new SimpleAttributes(other.attributes, elementName);
            this.comment = other.comment;
        }

        public String toString() {
            //throw new IllegalArgumentException("Don't use2");
            return toString(PATH);
        }
        /*
        public String toString(boolean path) {
            return toString(START_VALUE, path);
        }
        */
        static final int PATH = -1, NO_VALUE = 0, START_VALUE = 1, END_VALUE = 2;
        public String toString(int type) {
            String a = attributes.toString(type==PATH, elementName.equals("zone"));
            String result;
            if (type==PATH) {
                //if (type == NO_VALUE) return elementName + a + "-NOVALUE";
                //if (type == END_VALUE) return "END-" + elementName + ">";
                result = elementName + a;
            } else {
                if (type == NO_VALUE) result = "<" + elementName + a + "/>";
                else if (type == END_VALUE) result = "</" + elementName + ">";
                else result = "<" + elementName + a + ">";
            }
            return result;
        }
        public int compareTo(Object o) {
            if (o == null) return 1;
            int result;
            Element that = (Element) o;
            if ((result = elementOrdering.compare(elementName, that.elementName)) != 0) return result;
            return attributes.compareTo(that.attributes);
        }
        public boolean equals(Object o) {
            if (!(o instanceof Element)) return false;
            return compareTo(o) == 0;
        }
        /*
        public void addComment(String in_comment) {
            if (comment == null) comment = in_comment;
            else comment += NEWLINE + in_comment;
            return;
        }
        */
        /**
         * @param ignorelist
         * @return
         */
        public void removeAttributes(Set ignorelist) {
            attributes.removeAttributes(ignorelist);
        }
    }

    private void writeElementComment(StringBuffer out, String comment, int common) {
        if (comment != null) {
            indent(common, out);
            out.append("<!-- ");
            out.append(comment);
            if (comment.indexOf('\n') >= 0) {
                out.append("\r\n");
                indent(common, out);
            } else {
                out.append(" ");
            }
            out.append("-->\r\n");
        }
    }

    class ElementChain implements Comparable {
        List contexts;

        ElementChain() {
            contexts = new ArrayList();
        }

        /**
         * @param string
         * @return
         */
        public Element getElement(String string) {
            for (int i = 0; i < contexts.size(); ++i) {
                Element x = (Element)contexts.get(i);
                if (string.equals(x.elementName)) return x;
            }
            return null;
        }

        /**
         * @param string
         * @param string2
         * @param string3
         */
        public void setAttribute(String element, String attribute, String value) {
            for (int i = 0; i < contexts.size(); ++i) {
                Element context = (Element)contexts.get(i);
                if (context.elementName.equals(element)) {
                    context = new Element(context); // clone for safety
                    context.attributes.set(element, attribute, value);
                    contexts.set(i, context);
                    break;
                }
            }
        }

        ElementChain(ElementChain other) {
            contexts = new ArrayList(other.contexts);
        }

        public ElementChain push(String elementName, Attributes attributes, String comment) {
            elementOrdering.add(elementName);
            contexts.add(new Element(elementName, attributes, comment));
            return this;
        }

        public void pop(String elementName) {
            int last = contexts.size()-1;
            Element c = (Element) contexts.get(last);
            if (!c.elementName.equals(elementName)) throw new IllegalArgumentException("mismatch");
            contexts.remove(last);
        }

        public String toString() {
            //throw new IllegalArgumentException("Don't use");
            return toString(true, 0, Integer.MAX_VALUE);
        }

        public String toString(boolean path, int startLevel, int limitLevel) {
            StringBuffer buffer = new StringBuffer();
            if (startLevel < 0) startLevel = 0;
            if (limitLevel > contexts.size()) limitLevel = contexts.size();
            for (int i = startLevel; i < limitLevel; ++i) {
                //if (i != 0) buffer.append(' ');
                Element e = (Element) contexts.get(i);
                if (path) buffer.append("/" + e.toString(Element.PATH));
                else buffer.append(e.toString(Element.START_VALUE));
            }
            return buffer.toString();
        }

        public boolean equals(Object other) {
            return compareTo(other) == 0;
        }
        public int hashCode() {
            return contexts.hashCode();
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object o) {
            int result;
            ElementChain that = (ElementChain) o;
            if (DEBUG_ELEMENT && containsElement("weekendEnd") && that.containsElement(("weekendEnd"))) {
                result = 666;
            }
            int minLen = Math.min(contexts.size(), that.contexts.size());
            for (int i = 0; i < minLen; ++i) {
                if ((result = ((Element)contexts.get(i)).compareTo(that.contexts.get(i))) != 0) return result;
            }
            return compareInt(contexts.size(), that.contexts.size());
        }
        public void writeDifference(ElementChain former, EndNode value, StringBuffer out) {
            // find the identical stuff first!
            int csize = contexts.size();
            int fsize = former.contexts.size();
            int minLen = Math.min(csize, fsize);
            int result;

            // skip stuff that is in common
            int common;
            for (common = 0; common < minLen; ++common) {
                if ((result = ((Element)contexts.get(common)).compareTo(former.contexts.get(common))) != 0) break;
            }
            // finish up old elements, by writing out termination elements.
            // We don't do the very last one, however, since that was done with the value
            for (int j = fsize - 2; j >= common; --j) {
                indent(j, out);
                out.append(((Element)former.contexts.get(j)).toString(Element.END_VALUE));
                out.append(NEWLINE);
            }
            if (csize == 0) return; // we must be at the very end, bail.

            // write new elements if needed.
            for (; common < csize-1; ++common) {
                Element ee = ((Element)contexts.get(common));
                writeElementComment(out, ee.comment, common);
                indent(common, out);
                out.append(ee.toString(Element.START_VALUE));
                out.append(NEWLINE);
            }
            // now write the very current element
            Element ee = ((Element)contexts.get(csize-1));
            writeElementComment(out, ee.comment, common);
            indent(common, out);
            if (value == null || "".equals(value.string)) {
                out.append(ee.toString(Element.NO_VALUE));
            } else if (value.string == null) {
                Element temp = new Element(ee); // clone for safety
                temp.attributes.add(value.attributes);
                out.append(temp.toString(Element.NO_VALUE));
            } else {
                out.append(ee.toString(Element.START_VALUE));
                out.append(value.toString(Element.NO_VALUE));
                out.append(ee.toString(Element.END_VALUE));
            }
            out.append(NEWLINE);
        }

        public boolean containsElement(String string) {
            return getElement(string) != null;
        }

        public Element getLast() {
            return (Element) contexts.get(contexts.size()-1);
        }

        /**
         * @param ignorelist
         * @return
         */
        public ElementChain createRemovingAttributes(Set ignorelist) {
            ElementChain result = new ElementChain(this);
            for (int i = 0; i < contexts.size(); ++i) {
                Element e = (Element)contexts.get(i);
                e.removeAttributes(ignorelist);
            }
            return result;
        }

        /**
         * @param comment
         */
        /*
        public void addComment(String comment) {
            int count = contexts.size();
            if (count == 0) {
                System.out.println("Skipping start comment for now");
                //if (startComment == null) startComment = comment;
                //else startComment += NEWLINE + comment;
                return;
            }
            Element ec = (Element) contexts.get(count-1);
            ec.addComment(comment);
        }
        */
    }

    static int compareInt(int a, int b) {
        return a < b ? -1 : a > b ? 1 : 0;
    }

    static void indent(int count, StringBuffer out) {
        for (int i = 0; i < count; ++i) {
            out.append("\t");
        }
    }

    /*
    static {
        Object[][] temp = {
            {"keys", new Integer(13)},
            {"scripts", new Integer(7)},
            {"script", new Integer(8)},
            {"era", new Integer(38)},
            {"ldml", new Integer(0)},
            {"calendar", new Integer(22)},
            {"numbers", new Integer(57)},
            {"timeFormats", new Integer(44)},
            {"infinity", new Integer(69)},
            {"localizedPatternChars", new Integer(20)},
            {"dateTimeFormats", new Integer(47)},
            {"eraAbbr", new Integer(37)},
            {"exemplarCharacters", new Integer(18)},
            {"month", new Integer(26)},
            {"variants", new Integer(11)},
            {"group", new Integer(60)},
            {"dateTimeFormat", new Integer(49)},
            {"day", new Integer(30)},
            {"zone", new Integer(51)},
            {"types", new Integer(15)},
            {"timeFormat", new Integer(46)},
            {"default", new Integer(40)},
            {"dates", new Integer(19)},
            {"language", new Integer(4)},
            {"long", new Integer(52)},
            {"version", new Integer(2)},
            {"dayWidth", new Integer(29)},
            {"characters", new Integer(17)},
            {"variant", new Integer(12)},
            {"short", new Integer(55)},
            {"generation", new Integer(3)},
            {"am", new Integer(34)},
            {"pattern", new Integer(43)},
            {"minDays", new Integer(32)},
            {"displayName", new Integer(73)},
            {"perMille", new Integer(68)},
            {"monthContext", new Integer(24)},
            {"days", new Integer(27)},
            {"months", new Integer(23)},
            {"territories", new Integer(9)},
            {"identity", new Integer(1)},
            {"currency", new Integer(72)},
            {"exponential", new Integer(67)},
            {"territory", new Integer(10)},
            {"firstDay", new Integer(33)},
            {"languages", new Integer(6)},
            {"nan", new Integer(70)},
            {"week", new Integer(31)},
            {"nativeZeroDigit", new Integer(63)},
            {"decimal", new Integer(59)},
            {"symbols", new Integer(58)},
            {"daylight", new Integer(54)},
            {"calendars", new Integer(21)},
            {"eras", new Integer(36)},
            {"localeDisplayNames", new Integer(5)},
            {"dateTimeFormatLength", new Integer(48)},
            {"dateFormats", new Integer(39)},
            {"exemplarCity", new Integer(56)},
            {"currencies", new Integer(71)},
            {"minusSign", new Integer(66)},
            {"list", new Integer(61)},
            {"dateFormatLength", new Integer(41)},
            {"type", new Integer(16)},
            {"plusSign", new Integer(65)},
            {"dayContext", new Integer(28)},
            {"dateFormat", new Integer(42)},
            {"symbol", new Integer(74)},
            {"timeZoneNames", new Integer(50)},
            {"key", new Integer(14)},
            {"patternDigit", new Integer(64)},
            {"percentSign", new Integer(62)},
            {"standard", new Integer(53)},
            {"monthWidth", new Integer(25)},
            {"pm", new Integer(35)},
            {"timeFormatLength", new Integer(45)},
        };
        elementOrdering = new MapComparator(temp);
    }
    */

    public static class MapComparator {
        Map ordering = new TreeMap(); // maps from name to rank
        List rankToName = new ArrayList();

        MapComparator(){}
        MapComparator(Comparable[] data) {
            for (int i = 0; i < data.length; ++i) {
                add(data[i]);
            }
        }
        public void add(Comparable newObject) {
            Object already = ordering.get(newObject);
            if (already == null) {
                ordering.put(newObject, new Integer(rankToName.size()));
                rankToName.add(newObject);
            }
        }
        public int compare(Comparable a, Comparable b) {
            Comparable aa = (Comparable) ordering.get(a);
            Object bb = ordering.get(b);
            if (aa == null || bb == null) return a.compareTo(b);
            return aa.compareTo(bb);
        }
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            for (Iterator it = rankToName.iterator(); it.hasNext();) {
                Object key = it.next();
                buffer.append("\"").append(key).append("\","+NEWLINE);
            }
            return buffer.toString();
        }
    }

    public static class OrderedMap {
        private Map map = new TreeMap();
        private List list = new ArrayList();
        public void put(Object a, Object b) {
            map.put(a,b);
            list.add(a);
        }
        /**
         * @param key
         * @return
         */
        public Object getKeyFor(ElementChain key) {
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).equals(key)) return key;
            }
            return null;
        }
        /**
         * @param object
         */
        public void remove(Object object) {
            map.remove(object);
            list.remove(object);
        }
        /**
         * @param map
         */
        /*
        public void removeAll(OrderedMap other) {
            for (Iterator it = other.iterator(); it.hasNext();) {
                Object key = it.next();
                Object otherValue = other.map.get(key);
                Object value = map.get(key);
                if (value == null || !value.equals(otherValue)) continue;
                if (((ElementChain)key).containsElement("identity")) continue;
                value.equals(otherValue);
                log.println("Removing " + ((ElementChain)key).toString(true, 0) + "\t" + value);
                map.remove(key);
                while(list.remove(key)) {}
            }
        }
        */
        public Object get(Object a) {
            return map.get(a);
        }
        public Iterator iterator() {
            return list.iterator();
        }
        public int size() {
            return list.size();
        }
        public Object get(int index) {
            return list.get(index);
        }
    }

    ElementChain putData(ElementChain stack, String associatedData) {

        //If the associated data is "" and we have a final element in LEAFNODE,
        //then pull off the last element, and make it the associated data

        ElementChain result = new ElementChain(stack);
        EndNode value = new EndNode();
        value.string = associatedData;
        Element lastElement = result.getLast();
        if (LEAFNODES.contains(lastElement.elementName)) {
            if (associatedData.length() != 0) {
                System.err.println("Leaf Node must be empty: " + lastElement + "\tData: " + associatedData);
            }
            value.attributes = lastElement.attributes;
            lastElement.attributes = new SimpleAttributes();
            value.string = null;
        }
        if (DEBUG_SHOW_ADD) System.out.println("Adding: " + result + "\t" + value);
        EndNode alreadyThere = (EndNode) data.get(result);
        if (alreadyThere != null) {
            System.err.println("Overriding: " + result + "\tOld Value: " + alreadyThere + ",\t New Value: " + value);
        }
        data.put(result, value);
        return result;
    }

    //String startComment;

    static SidewaysView sidewaysView = new SidewaysView();

    static class EndNode {
        String string;
        SimpleAttributes attributes;
        public void set(Object associatedData) {
            if (associatedData instanceof String) {
                string = (String) associatedData;
                attributes = null;
            } else {
                attributes = (SimpleAttributes) associatedData;
                string = null;
            }
        }
        public String toString() {
            return toString(Element.PATH);
        }
        public String toString(int type) {
            if (string != null) return BagFormatter.toHTML.transliterate(string);
            return attributes.toString(); // TO FIX type==Element.PATH
        }
        /*
        public static String showValue(Object value) {
            if (value instanceof String) {
                return BagFormatter.toHTML.transliterate((String)value);
            }
            return ((Element)value).toString();
        }
        */


    }

    static class EndNodeComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            EndNode these = (EndNode) o1;
                EndNode that = (EndNode) o2;
                if (these.string != null) {
                    if (that.string != null) {
                        return getDefaultCollation().compare(these.string, that.string);
                    }
                    return -1;
                } else if (that.attributes != null) {
                    return these.attributes.compareTo(that.attributes);
                }
                return 1;
            }
    }

    static class SidewaysView {
        EndNodeComparator enc = new EndNodeComparator();
        Map contextCache = new TreeMap();
        Set fileNames = new TreeSet();
        Set allTypes = new TreeSet();

        void putData(OrderedMap data, String filename) {
            for (Iterator it = data.iterator(); it.hasNext();) {
                ElementChain original = (ElementChain) it.next();
                ElementChain copy = original.createRemovingAttributes(IGNORELIST);
                EndNode endNode = (EndNode)data.get(copy);
                Map dataToFile = (Map)contextCache.get(copy);
                if (dataToFile == null) {
                    dataToFile = new TreeMap(enc);
                    contextCache.put(copy, dataToFile);
                 }
                //System.out.println(copy + "\t\t" + endNode);
                Set files = (Set) dataToFile.get(endNode);
                if (files == null) {
                    files = new TreeSet();
                    dataToFile.put(endNode, files);
                }
                files.add(filename);
            }
            if (filename.indexOf('_') < 0
                || filename.equals("zh_Hant")) fileNames.add(filename); // add all language-only locales
        }

        int getChainDepth(ElementChain ec) {
            Element e = (Element)ec.contexts.get(1);
            String result = e.elementName;
            if (result.equals("numbers") || result.equals("localeDisplayNames") || result.equals("dates")) {
                return 3;
            }
            return 2;
        }

        String getChainName(ElementChain ec, int limit) {
            Element e = (Element)ec.contexts.get(1);
            String result = e.elementName;
            for (int i = 2; i < limit; ++i) {
                e = (Element)ec.contexts.get(i);
                result += "_" + e.elementName;
            }
            return result;
        }
        /*
        String getLastElementsType(ElementChain ec) {
            //TODO make SimpleAttributes a map instead of a set.
            Element e = (Element)ec.contexts.get(ec.contexts.size()-1);
            SimpleAttributes sa = e.attributes;
            for (Iterator it = sa.contents.iterator(); it.hasNext();) {
                SimpleAttribute sa1 = (SimpleAttribute)it.next();
                if (sa1.name.equals("type")) return sa1.value;
            }
            return "*NOT_FOUND*";
        }
        */
        void showCacheData() throws IOException {
        	UnicodeSet untransliteratedCharacters = new UnicodeSet();
        	Set translitErrors = new TreeSet();
        	GenerateCldrTests.DraftChecker dc = new GenerateCldrTests.DraftChecker(options[SOURCEDIR].value);
        	dc.isDraft("en");
            writeStyleSheet();
            PrintWriter out = null;
            String lastChainName = "";
            int lineCounter = 1;
            for (Iterator it = contextCache.keySet().iterator(); it.hasNext();) {
                ElementChain stack = (ElementChain) it.next();
                int limit = getChainDepth(stack);
                String chainName = getChainName(stack, limit);
                if (!chainName.equals(lastChainName)) {
                    if (out != null) {
                        out.println("</table>");
                        writeFooterAndClose(out);
                    }
                    allTypes.add(chainName); // add to the list
                    out = openAndDoHeader(chainName);
                    lastChainName = chainName;
                    lineCounter = 0;
                }
                String key = stack.toString(true, limit, Integer.MAX_VALUE);
                // strip    /ldml@version="1.2"/;

                lineCounter++;
                out.println("<tr><td colspan='2' class='head'>" +
                        "<a href='#" + lineCounter + "' name='" + lineCounter + "'>"
                        + lineCounter + "</a>&nbsp;" +
                        BagFormatter.toHTML.transliterate(key) + "</td></tr>");
                Map dataToFile = (Map) contextCache.get(stack);
                // walk through once, and gather all the filenames
                Set remainingFiles = new TreeSet(fileNames);
                for (Iterator it2 = dataToFile.keySet().iterator(); it2.hasNext();) {
                    Object data = it2.next();
                    remainingFiles.removeAll((Set) dataToFile.get(data));
                }
                // hack for zh_Hant
                if (!remainingFiles.contains("zh")) remainingFiles.remove("zh_Hant");
                // now display
                for (Iterator it2 = dataToFile.keySet().iterator(); it2.hasNext();) {
                    EndNode data = (EndNode) it2.next();
                    String dataStyle = "";
                    Set files = (Set) dataToFile.get(data);
                    if (files.contains("root")) {
                        files.addAll(remainingFiles);
                        dataStyle = " class='nodata'";
                    }

                    String extra = "";
                    if (data.string != null && options[TRANSLIT].doesOccur 
                    		&& GenerateCldrTests.NON_LATIN.containsSome(data.string)) {                    	
                    	try {
							extra = GenerateCldrTests.toLatin.transliterate(data.string);
	                    	untransliteratedCharacters.addAll(extra);
	                    	if (extra.equals(data.string)) extra = "";
	                  		else extra = "<br>(\"" + BagFormatter.toHTML.transliterate(extra) + "\")";                      	
						} catch (RuntimeException e) {
							translitErrors.add(e.getMessage());
						}
                    }
                    out.print("<tr><th" + dataStyle + 
                            (lineCounter == 1 ? " width='20%'" : "")
                            + ">\"" + data + "\""
							+ extra
							+ "</th><td>");
                    boolean first = true;
                    for (Iterator it3 = files.iterator(); it3.hasNext();) {
                        if (first) first = false;
                        else out.print(" ");
                        String localeID = (String)it3.next();
                        boolean emphasize = localeID.equals("root") || localeID.indexOf('_') >= 0;
                        if (dc.isDraft(localeID)) out.print("<i>");
                        if (emphasize) out.print("<b>");
                        out.print("\u00B7" + localeID + "\u00B7");
                        if (emphasize) out.print("</b>");
                        if (dc.isDraft(localeID)) out.print("</i>");
                    }
                    out.println("</td></tr>");
                }
            }
            if(out==null) {
                System.err.println("Out = null?");
            } else {
                out.println("</table>");
                writeFooterAndClose(out);
                out.close();
            }
            writeIndex();
            tripleData.writeData();
            untransliteratedCharacters.retainAll(GenerateCldrTests.NON_LATIN);
            log.println("Untranslated Characters*: " + untransliteratedCharacters.toPattern(false));
            log.println("Untranslated Characters* (hex): " + untransliteratedCharacters.toPattern(true));
            untransliteratedCharacters.closeOver(UnicodeSet.CASE);
            log.println("Untranslated Characters: " + untransliteratedCharacters.toPattern(false));
            log.println("Untranslated Characters (hex): " + untransliteratedCharacters.toPattern(true));
            for (Iterator it = translitErrors.iterator(); it.hasNext();) {
            	log.println(it.next());
            }
        }

        /**
         * @param type
         * @return
         * @throws IOException
         */
        private PrintWriter openAndDoHeader(String type) throws IOException {
            String fileName = type + ".html";
            PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value, "by_type" + File.separator + fileName);
            out.println("<html><head>");
            out.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
            out.println("<title>Comparison By Type: " + BagFormatter.toHTML.transliterate(type) + "</title>");
            out.println("<link rel='stylesheet' type='text/css' href='http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/common.css'>");
            out.println("<link rel='stylesheet' type='text/css' href='by_type.css'>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h2>By-Type Chart for " + "//ldml/" + type.replace('_', '/') + "/...</h1>");
            out.println("<p>" +
                    "<a href=\"index.html\">By-Type Chart Index</a> " +
                    "| <a href='http://www.jtcsv.com/cgibin/cldrwiki.pl?InterimVettingCharts'>Interim Vetting Charts</a>" +
                    "| <a href='http://oss.software.ibm.com/cvs/icu/~checkout~/locale/docs/tr35.html'>LDML Specification</a>" +
                    "| <a href='http://www.unicode.org/cldr/filing_bug_reports.html'>Filing Bug Reports</a>" +
                    "| <a href='http://oss.software.ibm.com/cvs/icu/~checkout~/locale/comparison_charts.html'>Cross Platform Charts</a>" +
                    "</p>");
            out.println("<p>This chart shows values across locales for different fields. " +
                    "Each value is listed under the field designator (in XML XPath format), " +
                    "followed by all the locales that use it. " +
                    "Locales are omitted if the value would be the same as the parent's. " +
					"The locales are listed in the format: \u00B7aa\u00B7 for searching. " +
                    "The value appears in red if it is the same as the root. " +
                    "Draft locales are italic-gray; territory locales are bold.</p>");             
            out.println("<table>");
            return out;
        }
        private void writeStyleSheet() throws IOException {
            PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value, "by_type" + File.separator + "by_type.css");
            out.println(".head { font-weight:bold; background-color:#DDDDFF }");
            out.println("td, th { border: 1px solid #0000FF; text-align }");
            out.println("th { width:10% }");
            out.println("i { color: gray }");            
            out.println(".nodata { background-color:#FF0000 }");
            out.println("table {margin-top: 1em}");
            out.close();
        }

        private void writeIndex() throws IOException {
            String fileName = "index" + ".html";
            PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value, "by_type" + File.separator + fileName);
            out.println("<html><head>");
            out.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
            out.println("<title>Comparison By Type: " + "index" + "</title>");
            out.println("<link rel='stylesheet' type='text/css' href='http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/common.css'>");
            out.println("<link rel='stylesheet' type='text/css' href='by_type.css'>");
            out.println("</head>");
            out.println("<body><h1>By Type Chart Index</h1>");
            out.println("<p>The following are charts for the individual datatypes, " +
                    "that show a comparison across locales for different fields. " +
                    "For example, in the orientation chart, one can see that all locales " +
                    "are left-to-right except \u00B7ar\u00B7 \u00B7fa\u00B7 \u00B7he\u00B7 \u00B7ps\u00B7 (and their children).</p>" +
                    "<p>Note: these charts do not yet include collations</p>");
            out.println("<ul>");
            for(Iterator e = allTypes.iterator();e.hasNext();) {
                String f = (String)e.next();
                out.println(" <li><a href=\"" + f + ".html" +  "\">" + f + "</a>");
            }
            out.println("</ul>");
            out.println("<a href='http://www.jtcsv.com/cgibin/cldrwiki.pl?ByType'>About this chart</a> <br/>");
            writeFooterAndClose(out);
        }
    }

    class MyDeclHandler implements DeclHandler {
        Map element_childComparator = new TreeMap();
        boolean showReason = false;
        Set SKIP_LIST = new HashSet(Arrays.asList(new String[] {
                "collation", "base", "settings", "suppress_contractions", "optimize", "rules", "reset",
                "context", "p", "pc", "s", "sc", "t", "tc", "q", "qc", "i", "ic", "extend", "x"
        }));
        Object DONE = new Object(); // marker

        public void checkData() {
            // verify that the ordering is the consistent for all child elements
            // do this by building an ordering from the lists.
            // The first item has no greater item in any set. So find an item that is only first
            showReason = false;
            List orderingList = new ArrayList();
            while (true) {
                Object first = getFirst(orderingList);
                if (first == DONE) {
                    log.println("Successful Ordering");
                    int count = 0;
                    for (Iterator it = orderingList.iterator(); it.hasNext();) log.println(++count + it.next().toString());
                    break;
                }
                if (first != null) {
                    orderingList.add(first);
                } else {
                    showReason = true;
                    getFirst(orderingList);
                    log.println();
                    log.println("Failed ordering. So far:");
                    for (Iterator it = orderingList.iterator(); it.hasNext();) log.print("\t" + it.next());
                    log.println();
                    log.println("Items:");
                    for (Iterator it =  element_childComparator.keySet().iterator(); it.hasNext();) showRow(it.next(), true);
                    log.println();
                    break;
                }
            }
        }

        /**
         * @param parent
         * @param skipEmpty TODO
         */
        private void showRow(Object parent, boolean skipEmpty) {
            List items = (List) element_childComparator.get(parent);
            if (skipEmpty && items.size() == 0) return;
            log.print(parent);
            for (Iterator it2 = items.iterator(); it2.hasNext();) log.print("\t" + it2.next());
            log.println();
        }

        /**
         * @param orderingList
         */
        private Object getFirst(List orderingList) {
            Set keys = element_childComparator.keySet();
            Set failures = new HashSet();
            boolean allZero = true;
            for (Iterator it = keys.iterator(); it.hasNext();) {
                List list = (List) element_childComparator.get(it.next());
                if (list.size() != 0) {
                    allZero = false;
                    Object possibleFirst = list.get(0);
                    if (!failures.contains(possibleFirst) && isAlwaysFirst(possibleFirst)) {
                        // we survived the guantlet. add to ordering list, remove from the mappings
                        removeEverywhere(possibleFirst);
                        return possibleFirst;
                    } else {
                        failures.add(possibleFirst);
                    }
                }
            }
            if (allZero) return DONE;
            return null;
        }
        /**
         * @param keys
         * @param it
         * @param possibleFirst
         */
        private void removeEverywhere(Object possibleFirst) {
            // and remove from all the lists
            for (Iterator it2 = element_childComparator.keySet().iterator(); it2.hasNext();) {
                List list2 = (List) element_childComparator.get(it2.next());
                list2.remove(possibleFirst);
            }
        }

        private boolean isAlwaysFirst(Object possibleFirst) {
            if (showReason) log.println("Trying: " + possibleFirst);
            for (Iterator it2 = element_childComparator.keySet().iterator(); it2.hasNext();) {
                Object key = it2.next();
                List list2 = (List) element_childComparator.get(key);
                int pos = list2.indexOf(possibleFirst);
                if (pos > 0) {
                    if (showReason) {
                        log.print("Failed at:\t");
                        showRow(key, false);
                    }
                    return false;
                }
            }
            return true;
        }

        // refine later; right now, doesn't handle multiple elements well.
        public void elementDecl(String name, String model) throws SAXException {
            if (SKIP_LIST.contains(name)) return;
            //log.println("Element\t" + name + "\t" + model);
            String[] list = model.split("[^A-Z0-9a-z]");
            List mc = new ArrayList();
            if (name.equals("currency")) {
                mc.add("alias");
                mc.add("symbol");
                mc.add("pattern");
            }
            for (int i = 0; i < list.length; ++i) {
                if (list[i].length() == 0) continue;
                //log.print("\t" + list[i]);
                if (mc.contains(list[i])) {
                    log.println("Duplicate attribute " + name + ", " + list[i]);
                } else {
                    mc.add(list[i]);
                }
            }
            element_childComparator.put(name, mc);
            //log.println();
        }
        public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
            //log.println("Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
        }
        public void internalEntityDecl(String name, String value) throws SAXException {
            //log.println("Internal Entity\t" + name + "\t" + value);
        }
        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
            //log.println("Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

    }

    class MyContentHandler implements ContentHandler, LexicalHandler {

        ElementChain contextStack = new ElementChain();
        String lastChars = "";
        boolean justPopped = false;
        int commentStack = 0;

        public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes)
            throws SAXException {
                //data.put(new ContextStack(contextStack), lastChars);
                //lastChars = "";
                try {
                    contextStack.push(qName, attributes, finalComment);
                    finalComment = null;
                    if (DEBUG) System.out.println("startElement:\t" + contextStack);
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
        }
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
                try {
                    if (DEBUG) System.out.println("endElement:\t" + contextStack);
                    if (lastChars.length() != 0 || justPopped == false) {
                        putData(contextStack, lastChars);
                        lastChars = "";
                    }
                    contextStack.pop(qName);
                    justPopped = true;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        public void characters(char[] ch, int start, int length)
            throws SAXException {
                try {
                    String value = new String(ch,start,length);
                    if (DEBUG) System.out.println("characters:\t" + value);
                    lastChars += value;
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

        // just for debugging

        public void notationDecl (String name, String publicId, String systemId)
        throws SAXException {
            if (DEBUG2) System.out.println("notationDecl: " + name
            + ", " + publicId
            + ", " + systemId
            );
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            if (DEBUG2) System.out.println("processingInstruction: " + target + ", " + data);
        }

        public void skippedEntity (String name)
        throws SAXException
        {
            if (DEBUG2) System.out.println("skippedEntity: " + name
            );
        }

        public void unparsedEntityDecl (String name, String publicId,
                        String systemId, String notationName) {
            if (DEBUG2) System.out.println("unparsedEntityDecl: " + name
            + ", " + publicId
            + ", " + systemId
            + ", " + notationName
            );
        }
        public void setDocumentLocator(Locator locator) {
            if (DEBUG2) System.out.println("setDocumentLocator Locator " + locator);
        }
        public void startDocument() throws SAXException {
            if (DEBUG2) System.out.println("startDocument");
            commentStack = 0; // initialize
        }
        public void endDocument() throws SAXException {
            if (DEBUG2) System.out.println("endDocument");
        }
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (DEBUG2) System.out.println("startPrefixMapping prefix: " + prefix +
                    ", uri: " + uri);
        }
        public void endPrefixMapping(String prefix) throws SAXException {
            if (DEBUG2) System.out.println("endPrefixMapping prefix: " + prefix);
        }
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (DEBUG2) System.out.println("ignorableWhitespace length: " + length);
        }
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            if (DEBUG2) System.out.println("startDTD name: " + name
                    + ", publicId: " + publicId
                    + ", systemId: " + systemId
            );
            commentStack++;
        }
        public void endDTD() throws SAXException {
            if (DEBUG2) System.out.println("endDTD");
            commentStack--;
        }
        public void startEntity(String name) throws SAXException {
            if (DEBUG2) System.out.println("startEntity name: " + name);
        }
        public void endEntity(String name) throws SAXException {
            if (DEBUG2) System.out.println("endEntity name: " + name);
        }
        public void startCDATA() throws SAXException {
            if (DEBUG2) System.out.println("startCDATA");
        }
        public void endCDATA() throws SAXException {
            if (DEBUG2) System.out.println("endCDATA");
        }
        public void comment(char[] ch, int start, int length) throws SAXException {
            if (commentStack != 0) return;
            String comment = new String(ch, start,length).trim();
            if (finalComment == null) finalComment = comment;
            else finalComment += NEWLINE + comment;
            if (DEBUG2) System.out.println("comment: " + comment);
        }
    };

    String finalComment = null;

    XMLReader createXMLReader(boolean validating) {
        XMLReader result = null;
        try { // Xerces
            result =  XMLReaderFactory
                    .createXMLReader("org.apache.xerces.parsers.SAXParser");
            result.setFeature("http://xml.org/sax/features/validation", validating);
        } catch (SAXException e1) {
            try { // Crimson
                result =  XMLReaderFactory
                        .createXMLReader("org.apache.crimson.parser.XMLReaderImpl");
                result.setFeature("http://xml.org/sax/features/validation", validating);
            } catch (SAXException e2) {
                try { // AElfred
                    result =  XMLReaderFactory
                            .createXMLReader("gnu.xml.aelfred2.XmlReader");
                    result.setFeature("http://xml.org/sax/features/validation", validating);
                } catch (SAXException e3) {
                    try { // Piccolo
                        result =  XMLReaderFactory
                                .createXMLReader("com.bluecast.xml.Piccolo");
                        result.setFeature("http://xml.org/sax/features/validation", validating);
                    } catch (SAXException e4) {
                        try { // Oracle
                            result =  XMLReaderFactory
                                    .createXMLReader("oracle.xml.parser.v2.SAXParser");
                            result.setFeature("http://xml.org/sax/features/validation", validating);
                        } catch (SAXException e5) {
                            try { // default
                                result =  XMLReaderFactory.createXMLReader();
                                result.setFeature("http://xml.org/sax/features/validation", validating);
                            } catch (SAXException e6) {
                                throw new NoClassDefFoundError(
                                        "No SAX parser is available, or unable to set validation correctly");
                                // or whatever exception your method is
                                // declared to throw
                            }
                        }
                    }
                }
            }
        }
        try {
            result.setEntityResolver(new CachingEntityResolver());
        } catch (Throwable e) {
            System.out
                    .println("WARNING: Can't set caching entity resolver  -  error "
                            + e.toString());
            e.printStackTrace();
        }
        return result;
    }
    private static void writeFooterAndClose(PrintWriter out)
    {
        out.println("Generated " + java.util.Calendar.getInstance().getTime());
        out.println("</body></html>");
        out.close();
    }
}
