/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package com.ibm.icu.dev.tool.cldr;

import java.io.File;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * This is a file that runs the CLDR tests for ICU4J, to verify that ICU4J implements them
 * correctly. It has gotten out of sync with the format, so needs to be fixed.
 * @author medavis
 */
public class TestCldr {
    static final boolean DEBUG = false;

    //ULocale uLocale = ULocale.ENGLISH;
    //Locale oLocale = Locale.ENGLISH; // TODO Drop once ICU4J has ULocale everywhere
    static PrintWriter log;
    SAXParser SAX;

    private static final int
        HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        LOGDIR = 3,
        MATCH = 4;

    private static final UOption[] options = {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.SOURCEDIR().setDefault("C:\\ICU4C\\locale\\common\\test\\"),
            UOption.create("log", 'l', UOption.REQUIRES_ARG).setDefault(""),
            UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
    };

    public static void main(String[] args) throws Exception {
        UOption.parseArgs(args, options);
        log = BagFormatter.openUTF8Writer(options[LOGDIR].value, "log.txt");
        try {
            TestCldr x = new TestCldr();
            x.test();
                 } finally {
                    log.close();
                    System.out.println("Done");
                 }
    }

    public void test() throws Exception {
        Set s = GenerateCldrTests.getMatchingXMLFiles(options[SOURCEDIR].value, options[MATCH].value);
        for (Iterator it = s.iterator(); it.hasNext();) {
            test((String) it.next());
        }
        /*
//        test("hu");
        File[] list = new File(options[SOURCEDIR].value).listFiles();
        for (int i = 0; i < list.length; ++i) {
            String name = list[i].getName();
            if (!name.endsWith(".xml")) continue;
            test(name.substring(0,name.length()-4));
        }
        */
    }

    public void test(String localeName) throws Exception {
        //uLocale = new ULocale(localeName);
        //oLocale = uLocale.toLocale();

        File f = new File(options[SOURCEDIR].value + localeName + ".xml");
        System.out.println("Testing " + f.getCanonicalPath());
        log.println("Testing " + f.getCanonicalPath());
        SAX.parse(f, DEFAULT_HANDLER);
    }

    static Transliterator toUnicode = Transliterator.getInstance("any-hex");
    static public String showString(String in) {
        return "\u00AB" + in + "\u00BB (" + toUnicode.transliterate(in) + ")";
    }
    // ============ SAX Handler Infrastructure ============

    abstract public class Handler {
        Map settings = new TreeMap();
        String name;
        List currentLocales = new ArrayList();
        int failures = 0;

        void setName(String name) {
            this.name = name;
        }
        void set(String attributeName, String attributeValue) {
            //if (DEBUG) System.out.println(attributeName + " => " + attributeValue);
            settings.put(attributeName, attributeValue);
        }
        void checkResult(String value) {
            try {
                for (int i = 0; i < currentLocales.size(); ++i) {
                    ULocale ul = (ULocale)currentLocales.get(i);
                    log.println("  Checking " + ul + "(" + ul.getDisplayName(ULocale.ENGLISH) + ")" + " for " + name);
                    handleResult(ul, value);
                    if (failures != 0) {
                        System.out.println("\tTotal Failures: " + failures + "\t" + ul + "(" + ul.getDisplayName(ULocale.ENGLISH) + ")");
                        failures = 0;
                    }
                }
            } catch (Exception e) {
                logln("Exception with result: <" + value + ">");
                e.printStackTrace(log);
            }
        }
        public void logln(String message) {
            String temp = message + "\t[" + name;
            for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
                String attributeName = (String) it.next();
                String attributeValue = (String) settings.get(attributeName);
                temp += " " + attributeName + "=<" + attributeValue + ">";
            }
            log.println(temp + "]");
        }
        int lookupValue(Object x, Object[] list) {
            for (int i = 0; i < list.length; ++i) {
                if (x.equals(list[i])) return i;
            }
            logln("Unknown String: " + x);
            return -1;
        }
        abstract void handleResult(ULocale currentLocale, String value) throws Exception;
        /**
         * @param attributes
         */
        public void setAttributes(Attributes attributes) {
            String localeList = attributes.getValue("locales");
            String[] currentLocaleString = new String[50];
            Utility.split(localeList, ' ', currentLocaleString);
            currentLocales.clear();
            for (int i = 0; i < currentLocaleString.length; ++i) {
                if (currentLocaleString[i].length() == 0) continue;
                currentLocales.add(new ULocale(currentLocaleString[i]));

            }
        }
    }

    public Handler getHandler(String name, Attributes attributes) {
        if (DEBUG) System.out.println("Creating Handler: " + name);
        Handler result = (Handler) RegisteredHandlers.get(name);
        if (result == null) System.out.println("Unexpected test type: " + name);
        else {
            result.setAttributes(attributes);
        }
        return result;
    }

    public void addHandler(String name, Handler handler) {
        handler.setName(name);
        RegisteredHandlers.put(name, handler);
    }
    Map RegisteredHandlers = new HashMap();

    // ============ Statics for Date/Number Support ============

    static TimeZone utc = TimeZone.getTimeZone("GMT");
    static DateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    {
        iso.setTimeZone(utc);
    }
    static int[] DateFormatValues = {-1, DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
    static String[] DateFormatNames = {"none", "short", "medium", "long", "full"};

    static String[] NumberNames = {"standard", "integer", "decimal", "percent", "scientific"};


    // ============ Handler for Collation ============ 
    static UnicodeSet controlsAndSpace = new UnicodeSet("[:cc:]");
    
    static String remove(String in, UnicodeSet toRemove) {
    	int cp;
    	StringBuffer result = new StringBuffer();
    	for (int i = 0; i < in.length(); i += UTF16.getCharCount(cp)) {
    		cp = UTF16.charAt(in, i);
    		if (!toRemove.contains(cp)) UTF16.append(result, cp);
    	}
    	return result.toString();
    }

    {
		addHandler("collation", new Handler() {
			public void handleResult(ULocale currentLocale, String value) {
				Collator col = Collator.getInstance(currentLocale);
				String lastLine = "";
				int count = 0;
				for (int pos = 0; pos < value.length();) {
					int nextPos = value.indexOf('\n', pos);
					if (nextPos < 0)
						nextPos = value.length();
					String line = value.substring(pos, nextPos);
					line = remove(line, controlsAndSpace); // HACK for SAX
					if (line.trim().length() != 0) { // HACK for SAX
						int comp = col.compare(lastLine, line);
						if (comp > 0) {
							failures++;
							logln("\tLine " + (count + 1) + "\tFailure: "
									+ showString(lastLine) + " should be leq "
									+ showString(line));
						} else if (DEBUG) {
							System.out.println("OK: " + line);
						}
						lastLine = line;
					}
					pos = nextPos + 1;
					count++;
				}
			}
		});
        
        // ============ Handler for Numbers ============ 
		addHandler("number", new Handler() {
			public void handleResult(ULocale locale, String result) {
                NumberFormat nf = null;
                double v = Double.NaN;
                for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
                    String attributeName = (String) it.next();
                    String attributeValue = (String) settings
                            .get(attributeName);
                    if (attributeName.equals("input")) {
                        v = Double.parseDouble(attributeValue);
                        continue;
                    }
                    // must be either numberType at this point
                    int index = lookupValue(attributeValue, NumberNames);
                    switch(index) {
                    case 0: nf = NumberFormat.getInstance(locale); break;
                    case 1: nf = NumberFormat.getIntegerInstance(locale); break;
                    case 2: nf = NumberFormat.getNumberInstance(locale); break;
                    case 3: nf = NumberFormat.getPercentInstance(locale); break;
                    case 4: nf = NumberFormat.getScientificInstance(locale); break;
                    }
                    String temp = nf.format(v).trim();
                    result = result.trim(); // HACK because of SAX
                    if (!temp.equals(result)) {
                        logln("Mismatched Number: CLDR: <" + result + ">, Host: <" + temp + ">");
                    }

                }
            }
        });

        // ============ Handler for Dates ============
        addHandler("date", new Handler() {
            public void handleResult(ULocale locale, String result) throws ParseException {
                int dateFormat = DateFormat.DEFAULT;
                int timeFormat = DateFormat.DEFAULT;
                Date date = new Date();
                for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
                    String attributeName = (String) it.next();
                    String attributeValue = (String) settings
                            .get(attributeName);
                    if (attributeName.equals("input")) {
                        date = iso.parse(attributeValue);
                        continue;
                    }
                    // must be either dateType or timeType at this point
                    int index = lookupValue(attributeValue, DateFormatNames);
                    int value = DateFormatValues[index];
                    if (attributeName.equals("dateType"))
                        dateFormat = value;
                    else
                        timeFormat = value;

                }
                DateFormat dt = dateFormat == -1 ? DateFormat.getTimeInstance(timeFormat, locale)
                        : timeFormat == -1 ? DateFormat.getDateInstance(dateFormat, locale)
                        : DateFormat.getDateTimeInstance(dateFormat, timeFormat, locale);
                dt.setTimeZone(utc);
                String temp = dt.format(date).trim();
                result = result.trim(); // HACK because of SAX
                if (!temp.equals(result)) {
                    logln("Mismatched DateTime: CLDR: <" + result + ">, Host: <" + temp + ">");
                }
            }
        });

    }

    // ============ Gorp for SAX ============

    {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            SAX = factory.newSAXParser();
        } catch (Exception e) {
            throw new IllegalArgumentException("can't start");
        }
    }

    DefaultHandler DEFAULT_HANDLER = new DefaultHandler() {
        static final boolean DEBUG = false;
        StringBuffer lastChars = new StringBuffer();
        boolean justPopped = false;
        Handler handler;

        public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes)
            throws SAXException {
                //data.put(new ContextStack(contextStack), lastChars);
                //lastChars = "";
                try {
                    if (qName.equals("cldrTest")) {
                     // skip
                    } else if (qName.equals("result")) {
                        for (int i = 0; i < attributes.getLength(); ++i) {
                            handler.set(attributes.getQName(i), attributes.getValue(i));
                        }
                    } else {
                        handler = getHandler(qName, attributes);
                        //handler.set("locale", uLocale.toString());
                    }
                    //if (DEBUG) System.out.println("startElement:\t" + contextStack);
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
        }
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
                try {
                    //if (DEBUG) System.out.println("endElement:\t" + contextStack);
                    if (qName.equals("result")) handler.checkResult(lastChars.toString());
                    else if (qName.length() != 0) {
                        //logln("Unexpected contents of: " + qName + ", <" + lastChars + ">");
                    }
                    lastChars.setLength(0);
                    justPopped = true;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        // Have to hack around the fact that the character data might be in pieces
        public void characters(char[] ch, int start, int length)
            throws SAXException {
                try {
                    String value = new String(ch,start,length);
                    if (DEBUG) System.out.println("characters:\t" + value);
                    lastChars.append(value);
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

        // just for debugging

        public void notationDecl (String name, String publicId, String systemId)
        throws SAXException {
            System.out.println("notationDecl: " + name
            + ", " + publicId
            + ", " + systemId
            );
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            System.out.println("processingInstruction: " + target + ", " + data);
        }

        public void skippedEntity (String name)
        throws SAXException
        {
            System.out.println("skippedEntity: " + name
            );
        }

        public void unparsedEntityDecl (String name, String publicId,
                        String systemId, String notationName)
        throws SAXException {
            System.out.println("unparsedEntityDecl: " + name
            + ", " + publicId
            + ", " + systemId
            + ", " + notationName
            );
        }

    };
}