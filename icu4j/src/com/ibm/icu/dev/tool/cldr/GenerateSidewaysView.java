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
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
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
 */public class GenerateSidewaysView {
    
    static boolean VALIDATING = false;

    private static final int 
        HELP1 = 0,
		HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        MATCH = 4,
        SKIP = 5,
        TZADIR = 6;
        
    private static final String NEWLINE = "\n";

    private static final UOption[] options = {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.SOURCEDIR().setDefault("C:\\ICU4C\\locale\\common\\main\\"),
            UOption.DESTDIR().setDefault("C:\\DATA\\GEN\\cldr\\main\\"),
            UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
            UOption.create("skip", 'z', UOption.REQUIRES_ARG).setDefault("zh_(C|S|HK|M).*"),
            UOption.create("tzadir", 't', UOption.REQUIRES_ARG).setDefault("C:\\ICU4J\\icu4j\\src\\com\\ibm\\icu\\dev\\tool\\cldr\\"),

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
                GenerateSidewaysView temp = getCLDR(baseName);
                // if (baseName.equals("zh_TW")) baseName = "zh_Hant_TW";
                // if (baseName.equals("root")) temp.addMissing();

                temp.writeTo(options[DESTDIR].value, baseName);
                sidewaysView.putData(temp.data, baseName);
                log.flush();          
            }
            sidewaysView.showCacheData();
        } finally {
            log.close();
       }
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
    static MapComparator valueOrdering = new MapComparator();
        
    OrderedMap data = new OrderedMap();
    MyContentHandler DEFAULT_HANDLER = new MyContentHandler();
    XMLReader xmlReader = createXMLReader(VALIDATING);

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
    
    static GenerateSidewaysView getCLDR(String s) throws SAXException, IOException {
        GenerateSidewaysView temp = (GenerateSidewaysView)cache.get(s);
        if (temp == null) {
            temp = new GenerateSidewaysView(s);
            cache.put(s,temp);
        }
        return temp;
    }
    
    String filename;
        
    private GenerateSidewaysView(String filename) throws SAXException, IOException {
        //System.out.println("Creating " + filename);
        this.filename = filename;

        xmlReader.setContentHandler(DEFAULT_HANDLER);
        xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler",DEFAULT_HANDLER);

        readFrom(options[SOURCEDIR].value, filename);
        String current = filename;
        while (true) {
            current = getParent(current);
            if (current == null) break;
            GenerateSidewaysView temp = getCLDR(current);
            this.removeAll(temp);
       }
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
    
    private void removeAll(GenerateSidewaysView temp) {
        data.removeAll(temp.data);
    }

    private static String getParent(String filename) {
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
            String value = (String) data.get(key);
            key.getDifference(old, value, buffer);
            old = key;           
        }
        empty.getDifference(old, "", buffer);
        writeElementComment(buffer,finalComment,0);

        return buffer.toString();
    }
    

    public static final Set IGNOREABLE = new HashSet(Arrays.asList(new String[] {
            "draft", 
            "references",
            "standard"}));

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
            	return "@" + name + "=\"" + BagFormatter.toHTML.transliterate(value) + "\"";
            } else {
                return " " + name + "=\"" + BagFormatter.toHTML.transliterate(value) + "\"";                
            }
        }
        public int compareTo(Object o) {
            SimpleAttribute that = (SimpleAttribute) o;
            int result;
            if ((result = attributeOrdering.compare(name, that.name)) != 0) return result;
            return valueOrdering.compare(value, that.value);
        }
    }
    
    static final boolean FIX_ZONE_ALIASES = true;
    Set zoneIDs = new HashSet();
    
    class SimpleAttributes implements Comparable {
        Set contents = new TreeSet();
        
        SimpleAttributes(SimpleAttributes other, String elementName) {
            contents.clear();
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
                        && name.equals("version") 
                        && value.equals("1.1")) continue;
                    if (name.equals("type") 
                        && value.equals("standard")) continue;

                    contents.add(new SimpleAttribute(name, value));
                }
            }
        }
        
        public String toString() {return toString(true, false);}
        public String toString(boolean path, boolean isZone) {
            StringBuffer buffer = new StringBuffer();
            for (Iterator it = contents.iterator(); it.hasNext();) {
                SimpleAttribute a = (SimpleAttribute)it.next();
                if (path && IGNOREABLE.contains(a.name)) continue;
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
            int result;
            SimpleAttributes that = (SimpleAttributes) o;
            // compare one at a time. Stop if one element is less than another.
            Iterator it = contents.iterator();
            Iterator it2 = that.contents.iterator();
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
        public SimpleAttributes set(String attribute, String value) {
            for (Iterator it = contents.iterator(); it.hasNext();) {
                SimpleAttribute sa = (SimpleAttribute) it.next();
                if (sa.name.equals(attribute)) {
                    contents.remove(sa);
                    break;
                }
            }
            contents.add(new SimpleAttribute(attribute, value));
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
    }
    
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
			attributes.set(attribute, value);			
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
        public String toString() {return toString(true);}
        public String toString(boolean path) {
            return toString(START_VALUE, path);
        }
        static final int NO_VALUE = 0, START_VALUE = 1, END_VALUE = 2;
        public String toString(int type, boolean path) {
            String a = attributes.toString(path, elementName.equals("zone"));
            String result;
            if (path) {
                if (type == NO_VALUE) return elementName + a + "-NOVALUE";
                if (type == END_VALUE) return "END-" + elementName + ">";
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
        	return compareTo(o) == 0;
        }
        /*
        public void addComment(String in_comment) {
            if (comment == null) comment = in_comment;
            else comment += NEWLINE + in_comment;
            return;
        }
        */
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
                    context.attributes.set(attribute, value);
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
        
        public String toString() {return toString(true);}
        public String toString(boolean path) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < contexts.size(); ++i) {
                //if (i != 0) buffer.append(' ');
                Element e = (Element) contexts.get(i);
                if (path) buffer.append("/" + e.toString(path));
                else buffer.append(e.toString(path));
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
            int minLen = Math.min(contexts.size(), that.contexts.size());
            for (int i = 0; i < minLen; ++i) {
                if ((result = ((Element)contexts.get(i)).compareTo(that.contexts.get(i))) != 0) return result;
            }
            return compareInt(contexts.size(), that.contexts.size());
        }
        public void getDifference(ElementChain former, String value, StringBuffer out) {
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
                out.append(((Element)former.contexts.get(j)).toString(Element.END_VALUE, false));
                out.append(NEWLINE);
            }
            if (csize == 0) return; // we must be at the very end, bail.
            
            // write new elements if needed.
            for (; common < csize-1; ++common) {
                Element ee = ((Element)contexts.get(common));
                writeElementComment(out, ee.comment, common);
                indent(common, out);
                out.append(ee.toString(Element.START_VALUE, false));
                out.append(NEWLINE);
            }
            // now write the very current element
            Element ee = ((Element)contexts.get(csize-1));
            writeElementComment(out, ee.comment, common);
            indent(common, out);
            if (value.length() == 0) {                           
                out.append(ee.toString(Element.NO_VALUE, false));
            } else {
                out.append(ee.toString(Element.START_VALUE, false));
                out.append(BagFormatter.toHTML.transliterate(value));
                out.append(ee.toString(Element.END_VALUE, false));
            }
            out.append(NEWLINE);
        }

		/**
         * @param string
         * @return
         */
        public boolean containsElement(String string) {
            return getElement(string) != null;
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
         * @param map
         */
        public void removeAll(OrderedMap other) {
            for (Iterator it = other.iterator(); it.hasNext();) {
                Object key = it.next();
                Object otherValue = other.map.get(key);
                Object value = map.get(key);
                if (value == null || !value.equals(otherValue)) continue;
                if (((ElementChain)key).containsElement("identity")) continue;
                log.println("Removing " + ((ElementChain)key).toString(true) + "\t" + value);
                map.remove(key);
                while(list.remove(key)) {}
            }
        }
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
        ElementChain result = new ElementChain(stack);
        data.put(result, associatedData);
        return result;
    }
    
    //String startComment;
    
    static SidewaysView sidewaysView = new SidewaysView();
    
    static class SidewaysView {
        Collator col = Collator.getInstance(ULocale.ENGLISH);
        { col.setStrength(Collator.IDENTICAL); }
        Map contextCache = new TreeMap();
        Set fileNames = new TreeSet();
        Set allTypes = new TreeSet();
        void putData(OrderedMap data, String filename) {
            for (Iterator it = data.iterator(); it.hasNext();) {
                ElementChain copy = (ElementChain) it.next();
                String associatedData = (String) data.get(copy);
                Map dataToFile = (Map)contextCache.get(copy);
                if (dataToFile == null) {
                    dataToFile = new TreeMap(col);
                    contextCache.put(copy, dataToFile);
                 }
                Set files = (Set) dataToFile.get(associatedData);
                if (files == null) {
                    files = new TreeSet();
                    dataToFile.put(associatedData, files);
                }
                files.add(filename);
            }
            if (filename.indexOf('_') < 0
                || filename.equals("zh_Hant")) fileNames.add(filename); // add all language-only locales
        }
        
        String getChainName(ElementChain ec) {
        	Element e = (Element)ec.contexts.get(1);
            String result = e.elementName;
            if (result.equals("numbers") || result.equals("localeDisplayNames") || result.equals("dates")) {
            	e = (Element)ec.contexts.get(2);
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
            writeStyleSheet();
            PrintWriter out = null;
            String lastChainName = "";
            for (Iterator it = contextCache.keySet().iterator(); it.hasNext();) {
                ElementChain stack = (ElementChain) it.next();
                String chainName = getChainName(stack);
                if (!chainName.equals(lastChainName)) {
                    if (out != null) {
                        out.println("</table></body></html>");
                        out.close();
                    }
                    allTypes.add(chainName); // add to the list
                	out = openAndDoHeader(chainName);
                    lastChainName = chainName;
                }
                out.println("<tr><td colspan='2' class='head'>" + BagFormatter.toHTML.transliterate(stack.toString(true)) + "</td></tr>");
                Map dataToFile = (Map) contextCache.get(stack);
                // walk through once, and gather all the filenames
                Set remainingFiles = new TreeSet(fileNames);
                for (Iterator it2 = dataToFile.keySet().iterator(); it2.hasNext();) {
                    String data = (String) it2.next();
                    remainingFiles.removeAll((Set) dataToFile.get(data));
                }
                // hack for zh_Hant
                if (!remainingFiles.contains("zh")) remainingFiles.remove("zh_Hant");
                // now display
                for (Iterator it2 = dataToFile.keySet().iterator(); it2.hasNext();) {
                    String data = (String) it2.next();
                    String dataStyle = "";
                    Set files = (Set) dataToFile.get(data);
                    if (files.contains("root")) {
                        files.addAll(remainingFiles);
                        dataStyle = " class='nodata'";
                    }
                    out.print("<tr><th" + dataStyle + ">\"" + BagFormatter.toHTML.transliterate(data) + "\"</th><td>");
                    boolean first = true;
                    for (Iterator it3 = files.iterator(); it3.hasNext();) {
                        if (first) first = false;
                        else out.print(" ");
                        out.print("\u0387" + it3.next() + "\u0387");
                    }
                    out.println("</td></tr>");
                }
            }
            if(out==null) {
                System.err.println("Out = null?");
            } else {
                out.println("</table></body></html>");
                out.close();
            }
            writeIndex();
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
			out.println("<title>Comparison By Type: " + type + "</title>");
            out.println("<link rel='stylesheet' type='text/css' href='http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/common.css'>");
            out.println("<link rel='stylesheet' type='text/css' href='by_type.css'>");
			out.println("</head>");
            out.println("<body>");
            out.println("<ul><li><a href=\"index.html\">index</a></li></ul>");
            out.println("<table>");
			return out;
		}
        private void writeStyleSheet() throws IOException {
            PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value, "by_type" + File.separator + "by_type.css");
            out.println(".head { font-weight:bold; background-color:#DDDDFF }");
            out.println("td, th { border: 1px solid #0000FF; text-align }");
            out.println("th { width:10% }");
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
            out.println("<body><ul>");
            for(Iterator e = allTypes.iterator();e.hasNext();) {
                String f = (String)e.next();
                out.println(" <li><a href=\"" + f + ".html" +  "\">" + f + "</a>");
            }
            out.println("</ul></body></html>");
            out.close();
        }
    }
    
    class MyContentHandler implements ContentHandler, LexicalHandler {
        static final boolean DEBUG = false;
        static final boolean DEBUG2 = false;
        
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
		} catch (SAXException e1) {
			try { // Crimson
				result =  XMLReaderFactory
						.createXMLReader("org.apache.crimson.parser.XMLReaderImpl");
			} catch (SAXException e2) {
				try { // Ælfred
					result =  XMLReaderFactory
							.createXMLReader("gnu.xml.aelfred2.XmlReader");
				} catch (SAXException e3) {
					try { // Piccolo
						result =  XMLReaderFactory
								.createXMLReader("com.bluecast.xml.Piccolo");
					} catch (SAXException e4) {
						try { // Oracle
							result =  XMLReaderFactory
									.createXMLReader("oracle.xml.parser.v2.SAXParser");
						} catch (SAXException e5) {
							try { // default
								result =  XMLReaderFactory.createXMLReader();
							} catch (SAXException e6) {
								throw new NoClassDefFoundError(
										"No SAX parser is available");
								// or whatever exception your method is  
								// declared to throw
							}
						}
					}
				}
			}
		}
        try {
			result.setFeature("http://xml.org/sax/features/validation", validating);
		} catch (SAXException e) {
			System.out.println("WARNING: Can't set parser validation to: " + validating);
		}
        return result;
	}
}
 