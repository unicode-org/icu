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
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.SortedBag;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.dev.tool.cldr.ICUResourceWriter.Resource;
import com.ibm.icu.dev.tool.cldr.ICUResourceWriter.ResourceTable;

/**
 * Generated tests for CLDR. Currently, these are driven off of a version of ICU4J, and just
 * use the data from that.
 * TODO Get the data directly from the CLDR tree.
 * @author medavis
 */

public class GenerateCldrTests {
    
    static private PrintWriter log;
    PrintWriter out;
    private static final int 
        HELP1 = 0,
        HELP2 = 1,
        DESTDIR = 2,
        LOGDIR = 3,
        SOURCEDIR =4,
        MATCH = 5,
        FULLY_RESOLVED = 6;

    private static final UOption[] options = {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.DESTDIR().setDefault("C:\\DATA\\GEN\\cldr\\test\\"),
            UOption.create("log", 'l', UOption.REQUIRES_ARG).setDefault("C:\\DATA\\GEN\\cldr\\test\\"),
            UOption.SOURCEDIR().setDefault("C:\\ICU4C\\locale\\common\\"),
            UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
            UOption.create("fullyresolved", 'f', UOption.NO_ARG),
    };
    
    CldrCollations cldrCollations;   
    static String logDir = null, destDir = null;
    
	public static void main(String[] args) throws Exception {
        UOption.parseArgs(args, options);
        log = BagFormatter.openUTF8Writer(options[LOGDIR].value, "log.txt");
        try {

			//compareAvailable();
            //if (true) return;
			//System.out.println(createCaseClosure(new UnicodeSet("[a{bc}{def}{oss}]")));
			//System.out.println(createCaseClosure(new UnicodeSet("[a-zß{aa}]")));
            GenerateCldrTests t = new GenerateCldrTests();
			//t.generate(new ULocale("hu"), null);
			t.generate(options[MATCH].value);
			/*
			t.generate(new ULocale("da"));
			t.generate(new ULocale("hu"));
			t.generate(new ULocale("de"));
			t.generate(new ULocale("ar@collation=direct"));
			*/
		} finally {
            log.close();
            System.out.println("Done");
		}
    }
    
    /**
	 * 
	 */
    /*
	private static void compareAvailable() {
		ULocale[] cols = Collator.getAvailableULocales();
        Locale[] alocs = NumberFormat.getAvailableLocales();
		Set sCols = filter(cols);
        Set sLocs = filter(alocs);
        Set oldSLocs = new TreeSet(sCols);
        sLocs.removeAll(sCols);
        log.println("main - collation");
        showLocales(sLocs);
        sCols.removeAll(oldSLocs);
        log.println();
        log.println("collation - main");
        showLocales(sCols);
	}
    */

	/**
	 * @param sLocs
	 */
	private static void showLocales(Set sLocs) {
		for (Iterator it = sLocs.iterator(); it.hasNext();) {
            String s = (String) it.next();
        	log.println(s + "\t" + ULocale.getDisplayLanguage(s,"en"));
        }
	}

	/**
	 * @param cols
	 * @return
	 */
	private static Set filter(Object[] cols) {
		Set result = new TreeSet();
        for (int i = 0; i < cols.length; ++i) {
        	String s = cols[i].toString();
            if (s.indexOf('_') >= 0) continue;
            result.add(s);
        }
        return result;
	}
    
    Set addULocales(Object[] objects, Set target) {
    	for (int i = 0; i < objects.length; ++i) {
    		target.add(new ULocale(objects[i].toString()));
        }
        return target;
    }
    private void addLocale(ULocale item) {
        String lang = item.getLanguage();
        if (lang.length() == 0) return; // skip root
        ULocale parent = new ULocale(lang);
        //System.out.println(item + ", " + parent);
        parentToLocales.add(parent, item);
        /*
        RuleBasedCollator col = cldrCollations.getInstance(item);
        if (col == null) {
        	System.out.println("No collator for: " + item);
        }
        String rules = col.getRules(); // ((RuleBasedCollator)Collator.getInstance(item)).getRules();
        rulesToLocales.add(rules, item);
        localesToRules.put(item, rules);
        */
    }

    Set collationLocales; //  = addULocales(Collator.getAvailableULocales(), new TreeSet(ULocaleComparator));
    Set numberLocales = addULocales(NumberFormat.getAvailableLocales(), new TreeSet(ULocaleComparator));
    Set dateLocales = addULocales(DateFormat.getAvailableLocales(), new TreeSet(ULocaleComparator));
    Set allLocales = new TreeSet(ULocaleComparator);

    Map localesToRules = new HashMap();
    Relation.CollectionFactory cm = new Relation.CollectionMaker(ULocaleComparator);
    Relation rulesToLocales = new Relation(new TreeMap(), cm);
    Relation parentToLocales = new Relation(new TreeMap(ULocaleComparator), cm);
    
    void getLocaleList() {
        collationLocales = new TreeSet(ULocaleComparator);
        collationLocales.addAll(cldrCollations.getAvailableSet());
        /*
    	collationLocales = addULocales(new String[] { // HACK
                "ga",
                "nl",
                "pt",
                "de@collation=phonebook",
                "es@collation=traditional",
                "hi@collation=direct",
                "zh@collation=pinyin",
                "zh@collation=stroke",
                "zh@collation=traditional",
        }, collationLocales);
        */
        allLocales.addAll(collationLocales);
        allLocales.addAll(numberLocales);
        allLocales.addAll(dateLocales);
        // HACK        
        // get all collations with same rules
        
        for (Iterator it = allLocales.iterator(); it.hasNext();) {
			addLocale((ULocale) it.next());
        }
        /*
        String[] others = new String[] {
                "de@collation=phonebook",
                "es@collation=traditional",
                "hi@collation=direct",
                "zh@collation=pinyin",
                "zh@collation=stroke",
                "zh@collation=traditional",
        };
        for (int i = 0; i < others.length; ++i) {
            addLocale(new ULocale(others[i]));
        }
        */
    }
    
    CldrOthers cldrOthers;
    
	void generate(String pat) throws Exception {
        cldrOthers = new CldrOthers(options[SOURCEDIR].value + "main" + File.separator, pat);
        cldrOthers.show();
        //if (true) return;
        cldrCollations = new CldrCollations(options[SOURCEDIR].value + "collation" + File.separator, pat);
        cldrCollations.show();
        getLocaleList();

        Matcher m = Pattern.compile(pat).matcher("");
        for (Iterator it = parentToLocales.keySet().iterator(); it.hasNext();) {
            String p = it.next().toString();
            if (!m.reset(p).matches()) continue;
        	generate(new ULocale(p));
        }
    }

    private void generate(ULocale locale) throws Exception {
        out = BagFormatter.openUTF8Writer(options[DESTDIR].value, locale + ".xml");
        out.println("<?xml version='1.0' encoding='UTF-8' ?>");
        out.println("<!DOCTYPE ldml SYSTEM 'http://www.unicode.org/cldr/dtd/1.2/beta/cldrTest.dtd'>");
        out.println("<!-- For information, see readme.html -->");
        out.println(" <cldrTest version='1.2' base='" + locale + "'>");
        out.println(" <!-- " + BagFormatter.toXML.transliterate(
                locale.getDisplayName(ULocale.ENGLISH) + " ["
                + locale.getDisplayName(locale))
                + "] -->");
        //generateItems(locale, numberLocales, NumberEquator, NumberShower);
        //generateItems(locale, dateLocales, DateEquator, DateShower);
        generateItems(locale, collationLocales, CollationEquator, CollationShower);
        out.println(" </cldrTest>");
        out.close();
        GenerateSidewaysView.generateBat(options[SOURCEDIR].value + "test" + File.separator, locale + ".xml", options[DESTDIR].value, locale + ".xml");
    }

    /*
     * 
        // first pass through and get all the functional equivalents
        Map uniqueLocales = new TreeMap();
        
        String[] keywords = Collator.getKeywords();
        boolean [] isAvailable = new boolean[1];
        for (int i = 0; i < locales.length; ++i) {
            add(locales[i], uniqueLocales);
            if (true) continue; // TODO restore once Vladimir fixes
            for (int j = 0; j < keywords.length; ++j) {
                String[] values = Collator.getKeywordValues(keywords[j]);
                for (int k = 0; k < values.length; ++k) {
                    // TODO -- for a full job, would do all combinations of different keywords!
                    if (values[k].equals("standard")) continue;
                    add(new ULocale(locales[i] + "@" + keywords[j] + "=" + values[k]), uniqueLocales);
                    //ULocale other = Collator.getFunctionalEquivalent(keywords[j], locales[i], isAvailable);
                }
            }            
        }
        for (int i = 0; i < extras.length; ++i) {
            add(new ULocale(extras[i]), uniqueLocales);
        }
        // items are now sorted by rules. So resort by locale
        Map toDo = new TreeMap(ULocaleComparator);
        for (Iterator it = uniqueLocales.keySet().iterator(); it.hasNext();) {
            Object rules = it.next();
            Set s = (Set) uniqueLocales.get(rules);
            ULocale ulocale = (ULocale) s.iterator().next(); // get first one
            toDo.put(ulocale, s);
        }
        for (Iterator it = toDo.keySet().iterator(); it.hasNext();) {
            ULocale ulocale = (ULocale) it.next();
            Set s = (Set) toDo.get(ulocale);
            generate(ulocale);
        }
     */
    
    /**
     * add locale into list. Replace old if shorter
     * @param locale
     */
    void add(ULocale locale, Map uniqueLocales) {
        try {
			RuleBasedCollator col = cldrCollations.getInstance(locale); // (RuleBasedCollator) Collator.getInstance(locale);
            // for our purposes, separate locales if we are using different exemplars
			String key = col.getRules() + "\uFFFF" + getExemplarSet(locale, 0);
			Set s = (Set) uniqueLocales.get(key);
			if (s == null) {
				s = new TreeSet(ULocaleComparator);
			    uniqueLocales.put(key, s);
			}
            System.out.println("Adding " + locale);
			s.add(locale);
		} catch (Throwable e) { // skip
            System.out.println("skipped " + locale);
		}
    }
    
    /**
     * Work-around
     * @param locale
     * @param options
     * @return
     */
    public UnicodeSet getExemplarSet(ULocale locale, int options) {
        String n = locale.toString();
        int pos = n.indexOf('@');
        if (pos >= 0) locale = new ULocale(n.substring(0,pos));
        UnicodeSet result = cldrOthers.getExemplarSet(locale); // LocaleData.getExemplarSet(locale, options);
        if (options == 0) result.closeOver(UnicodeSet.CASE);
        return result;
    }
    
    public static final Comparator ULocaleComparator = new Comparator() {
		public int compare(Object o1, Object o2) {
			return o1.toString().compareTo(o2.toString());
		}	
    };
    
    public interface Equator {
        public boolean equals(Object o1, Object o2);
    }
    
    static boolean intersects(Collection a, Collection b) {
        for (Iterator it = a.iterator(); it.hasNext();) {
            if (b.contains(it.next())) return true;
        }
        return false;
    }
    
    static Collection extract(Object x, Collection a, Equator e, Collection output) {
        List itemsToRemove = new ArrayList();
        for (Iterator it = a.iterator(); it.hasNext();) {
            Object item = it.next();
            if (e.equals(x, item)) {
                itemsToRemove.add(item); // have to do this because iterator may not allow
                output.add(item);
            }
        }
        a.removeAll(itemsToRemove);
        return output;
    }

    class ResultsPrinter {
        Map settings = new TreeMap();
        Map oldSettings = new TreeMap();
        void set(String name, String value) {
            settings.put(name, value);
        }
        void print(String result) {
            out.print("   <result");
            for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                Object value = settings.get(key);
                if (!value.equals(oldSettings.get(key))) {
                    out.print(" " + key + "='" + BagFormatter.toXML.transliterate(value.toString()) + "'");
                    oldSettings.put(key, value);
                }
            }
            out.println(">" + BagFormatter.toXML.transliterate(result) + "</result>");
        }
    }

    interface DataShower {
        void show(ULocale first, Collection others) throws Exception;
    }

    private void generateItems(ULocale locale, Collection onlyLocales, Equator test, DataShower generator) throws Exception {
        Collection sublocales = parentToLocales.get(locale, new ArrayList());
        sublocales.retainAll(onlyLocales);
        // get all the things that share the same behavior
        while (sublocales.size() != 0) {
            // start with the first one
            ULocale first = (ULocale) sublocales.iterator().next();
            Collection others = extract(first, sublocales, test, new ArrayList());
            generator.show(first, others);
        }        
    }

    private void showLocales(String elementName, Collection others) {
        //System.out.println(elementName + ": " + locale);
        out.println("  <" + elementName + " ");
        StringBuffer comment = new StringBuffer();
        if (others != null && others.size() != 0) {
            out.print("locales='");
            boolean first = true;
            for (Iterator it = others.iterator(); it.hasNext();) {
                if (first)
                    first = false;
                else {
                    out.print(" ");
                    comment.append("; ");
                }
                ULocale loc = (ULocale) it.next();
                out.print(loc);
                comment.append(loc.getDisplayName(ULocale.ENGLISH) + " ["
                        + loc.getDisplayName(loc) + "]");
            }
            out.print("'");
        }
        out.println(">");
        out.println("<!-- "
                        + BagFormatter.toXML.transliterate(comment.toString())
                        + " -->");
    }

    // ========== DATES ==========
    
    static TimeZone utc = TimeZone.getTimeZone("GMT");
    static DateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    {
    	iso.setTimeZone(utc);
    }
    static int[] DateFormatValues = {-1, DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
    static String[] DateFormatNames = {"none", "short", "medium", "long", "full"};
    private static DateFormat getDateFormat(ULocale locale, int i, int k) {
        Locale olocale = locale.toLocale(); // TODO replace once fixed!!
        DateFormat df;
        assert(olocale == null);
        if (DateFormatValues[k] == -1) df = DateFormat.getDateInstance(DateFormatValues[i], olocale);
        else if (DateFormatValues[i] == -1) df = DateFormat.getTimeInstance(DateFormatValues[k], olocale);
        else df = DateFormat.getDateTimeInstance(DateFormatValues[i], DateFormatValues[k], olocale);
        return df;
    }

    static Equator DateEquator = new Equator() {
        /**
         * Must both be ULocales
         */
		public boolean equals(Object o1, Object o2) {
			ULocale loc1 = (ULocale) o1;
            ULocale loc2 = (ULocale) o2;
            for (int i = 0; i < DateFormatValues.length; ++i) {
                for (int j = 0; j < DateFormatValues.length; ++j) {
                    if (i == 0 && j == 0) continue; // skip null case
                    DateFormat df1 = getDateFormat(loc1, i, j);
                    NumberFormat nf = df1.getNumberFormat();
                    nf.setCurrency(NO_CURRENCY);
                    df1.setNumberFormat(nf);
                    DateFormat df2 = getDateFormat(loc2, i, j);
                    nf = df2.getNumberFormat();
                    nf.setCurrency(NO_CURRENCY);
                    df2.setNumberFormat(nf);
                    if (!df1.equals(df2)) {
                        df1.equals(df2);
                        return false;
                    }
                }
            }
            return true;
		}   	
    };
    
    DataShower DateShower = new DataShower() {
     public void show(ULocale locale, Collection others) throws ParseException {
        showLocales("date", others);

        String[] samples = {
                "1900-01-31T00:00:00Z",
                "1909-02-28T00:00:01Z",
                "1918-03-26T00:59:59Z",
                "1932-04-24T01:00:00Z",
                "1945-05-20T01:00:01Z",
                "1952-06-18T11:59:59Z",
                "1973-07-16T12:00:00Z",
                "1999-08-14T12:00:01Z",
                "2000-09-12T22:59:59Z",
                "2001-10-08T23:00:00Z",
                "2004-11-04T23:00:01Z",
                "2010-12-01T23:59:59Z",
        };
        
        ResultsPrinter rp = new ResultsPrinter();
        for (int j = 0; j < samples.length; ++j) {
            Date datetime = iso.parse(samples[j]);
            rp.set("input", iso.format(datetime));
            for (int i = 0; i < DateFormatValues.length; ++i) {
                rp.set("dateType", DateFormatNames[i]);
                for (int k = 0; k < DateFormatValues.length; ++k) {
                    if (DateFormatValues[i] == -1 && DateFormatValues[k] == -1) continue;
                    rp.set("timeType", DateFormatNames[k]);
                	DateFormat df = getDateFormat(locale, i, k);
                    df.setTimeZone(utc);
                	rp.print(df.format(datetime));
                }
            }
        }
        out.println("  </date>");    	
    }};

    // ========== NUMBERS ==========
    
	static String[] NumberNames = {"standard", "integer", "decimal", "percent", "scientific"};
    private static NumberFormat getNumberFormat(ULocale ulocale, int i) {
        Locale olocale = ulocale.toLocale();
        NumberFormat nf = null;
        switch(i) {
        case 0: nf = NumberFormat.getInstance(olocale); break;
        case 1: nf = NumberFormat.getIntegerInstance(olocale); break;
        case 2: nf = NumberFormat.getNumberInstance(olocale); break;
        case 3: nf = NumberFormat.getPercentInstance(olocale); break;
        case 4: nf = NumberFormat.getScientificInstance(olocale); break;
        default: throw new IllegalArgumentException("Unknown NumberFormat: " + i);
        }
        return nf;
    }

    static Currency NO_CURRENCY = Currency.getInstance("XXX");
    
    static Equator NumberEquator = new Equator() {
        /**
         * Must both be ULocales
         */      
        public boolean equals(Object o1, Object o2) {
            ULocale loc1 = (ULocale) o1;
            ULocale loc2 = (ULocale) o2;
            for (int i = 0; i < NumberNames.length; ++i) {
                NumberFormat nf1 = getNumberFormat(loc1, i);
                nf1.setCurrency(NO_CURRENCY);
                NumberFormat nf2 = getNumberFormat(loc2, i);
                nf2.setCurrency(NO_CURRENCY);
            	if (!nf1.equals(nf2)) {
                    //nf1.equals(nf2);
                    return false;
                }
            }
            return true;
        }       
    };

    DataShower NumberShower = new DataShower() {
        public void show(ULocale locale, Collection others) throws ParseException {
        showLocales("number", others);
        
        double[] samples = {
                0,
                0.01, -0.01,
                1, -1,
                123.456, -123.456,
                123456.78, -123456.78,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NaN
        };
        ResultsPrinter rp = new ResultsPrinter();
        for (int j = 0; j < samples.length; ++j) {
            double sample = samples[j];
            rp.set("input", String.valueOf(sample));
            for (int i = 0; i < NumberNames.length; ++i) {
                rp.set("numberType", NumberNames[i]);
                NumberFormat nf = getNumberFormat(locale, i);
                rp.print(nf.format(sample));
            }
        }
        out.println("  </number>");       
    }};
    

    // ========== COLLATION ==========

    Equator CollationEquator = new Equator() {
        /**
         * Must both be ULocales
         */
        public boolean equals(Object o1, Object o2) {
            ULocale loc1 = (ULocale) o1;
            ULocale loc2 = (ULocale) o2;
            return cldrCollations.getInstance(loc1).equals(cldrCollations.getInstance(loc2)); // Collator.getInstance(loc1).equals(Collator.getInstance(loc2));
        }       
    };
    
    DataShower CollationShower = new DataShower() {
    	public void show(ULocale locale, Collection others) {
        showLocales("collation", others);
        
		Collator col = cldrCollations.getInstance(locale); // Collator.getInstance(locale);
		
		UnicodeSet tailored = col.getTailoredSet();
        if (locale.getLanguage().equals("zh")) {
            tailored.addAll(new UnicodeSet("[[a-z]-[v]]"));
            log.println("HACK for Pinyin");
        }
        tailored = createCaseClosure(tailored);
        tailored = nfc(tailored);
        //System.out.println(tailored.toPattern(true));
        
		UnicodeSet exemplars = getExemplarSet(locale, UnicodeSet.CASE);
        // add all the exemplars
        if (false) for (Iterator it = others.iterator(); it.hasNext(); ) {
            exemplars.addAll(getExemplarSet((ULocale)it.next(), UnicodeSet.CASE));
        }
        
        exemplars = createCaseClosure(exemplars);
        exemplars = nfc(exemplars);
        //System.out.println(exemplars.toPattern(true));
		tailored.addAll(exemplars);
        //UnicodeSet tailoredMinusHan = new UnicodeSet(tailored).removeAll(SKIP_COLLATION_SET);
        if (!exemplars.containsAll(tailored)) {
        	//BagFormatter bf = new BagFormatter();
            log.println("In Tailored, but not Exemplar; Locale: " + locale + "\t" + locale.getDisplayName());
            log.println(new UnicodeSet(tailored).removeAll(exemplars).toPattern(false));
            //bf.(log,"tailored", tailored, "exemplars", exemplars);
            log.flush();
        }
        tailored.addAll(new UnicodeSet("[\\ .02{12}]"));
        tailored.removeAll(SKIP_COLLATION_SET);
		
        SortedBag bag = new SortedBag(col);
		doCollationResult(col, tailored, bag);
		out.println("  </collation>");
	}};
    static final UnicodeSet SKIP_COLLATION_SET = new UnicodeSet(
            "[[:script=han:][:script=hangul:]-[\u4e00-\u4eff \u9f00-\u9fff \uac00-\uacff \ud700-\ud7ff]]");

	/**
	 * @param col
	 * @param tailored
	 * @param bag
	 */
	private void doCollationResult(Collator col, UnicodeSet tailored, SortedBag bag) {
		for (UnicodeSetIterator usi = new UnicodeSetIterator(tailored); usi.next(); ) {
		    String s = usi.getString();
		    bag.add('x' + s);
		    bag.add('X' + s);
		    bag.add('x' + s + 'x');
		}
		//out.println("   <set locale='" + locale + "'/>");
        /*
        if (others != null) for (Iterator it = others.iterator(); it.hasNext(); ) {
            ULocale uloc = (ULocale) it.next();
            if (uloc.equals(locale)) continue;
        	out.println("   <other locale='" + uloc + "'/>");
        }
        */
		out.println("   <result>");
		String last = "";
		boolean needEquals = false;
		for (Iterator it = bag.iterator(); it.hasNext(); ) {
		    String s = (String) it.next();
		    if (col.compare(s, last) != 0) {
		        if (needEquals) out.println(last);
                needEquals = false;
                last = s;
		    } else {
		    	needEquals = true;
		    }
		    out.println(BagFormatter.toXML.transliterate(s));

		}
		out.println("   </result>");
	}
    
    static public Set getMatchingXMLFiles(String sourceDir, String localeRegex) {
        Matcher m = Pattern.compile(localeRegex).matcher("");
        Set s = new TreeSet();
        File[] files = new File(sourceDir).listFiles();
        for (int i = 0; i < files.length; ++i) {
            String name = files[i].getName();
            if (!name.endsWith(".xml")) continue;
            if (name.startsWith("supplementalData")) continue;
            String locale = name.substring(0,name.length()-4); // drop .xml
            if (!locale.equals("root") && !m.reset(locale).matches()) continue;
            s.add(locale);
        }
        return s;
    }

    static class CldrOthers {
        Map ulocale_exemplars = new TreeMap(ULocaleComparator);
        Map uniqueExemplars = new HashMap();
        String sourceDir;
        Set locales = new TreeSet(ULocaleComparator);
        
        UnicodeSet getExemplarSet(ULocale locale) {
        	return (UnicodeSet) ulocale_exemplars.get(locale); 
        }
        
        void show() {
            log.println("Showing Locales");
            log.println("Unique Exemplars: " + uniqueExemplars.size());
            for (Iterator it2 = ulocale_exemplars.keySet().iterator(); it2.hasNext();) {
                ULocale locale = (ULocale) it2.next();
                UnicodeSet us = getExemplarSet(locale);
                log.println("\t" + locale + ", " + us);   
            }       	
        }
        static final ULocale ROOT = new ULocale("root"); // since CLDR has different root.
        
        CldrOthers(String sourceDir, String localeRegex) {
            this.sourceDir = sourceDir;
            Set s = getMatchingXMLFiles(sourceDir, localeRegex);
            for (Iterator it = s.iterator(); it.hasNext();) {
                getInfo((String) it.next());
            }
            // now do inheritance manually
            for (Iterator it = locales.iterator(); it.hasNext();) {
                ULocale locale = (ULocale) it.next();
            	UnicodeSet ex = (UnicodeSet) ulocale_exemplars.get(locale);
                if (ex != null) continue;
                for (ULocale parent = locale.getFallback(); parent != null; parent = parent.getFallback()) {
                    ULocale fixedParent = parent.getLanguage().length() == 0 ? ROOT : parent;
                	ex = (UnicodeSet) ulocale_exemplars.get(fixedParent);
                    if (ex == null) continue;
                    ulocale_exemplars.put(locale, ex);
                    break;
                }
            }
            
        }
        void getInfo(String locale) {
            System.out.println("Getting info for: " + locale);     
            locales.add(new ULocale(locale));
            Document doc;
            if (options[FULLY_RESOLVED].doesOccur) {
				doc = LDMLUtilities.getFullyResolvedLDML(sourceDir, locale,
						false, false, false);
			} else {
				doc = LDMLUtilities.parse(sourceDir + locale + ".xml", false);
			}
            Node node = LDMLUtilities.getNode(doc, "//ldml/characters/exemplarCharacters");
            if (node == null) return;
            if (isDraft(node)) System.out.println("Skipping draft: " + locale + ", " + getXPath(node));
        	String exemplars = LDMLUtilities.getNodeValue(node);
            UnicodeSet exemplarSet = new UnicodeSet(exemplars);
            UnicodeSet fixed = (UnicodeSet) uniqueExemplars.get(exemplarSet);
            if (fixed == null) {
            	uniqueExemplars.put(exemplarSet, exemplarSet);
                fixed = exemplarSet;
            }
            ulocale_exemplars.put(new ULocale(locale), fixed);
        }
    }
    
    public static boolean isDraft(Node node) {
        for (; node.getNodeType() != Node.DOCUMENT_NODE; node = node.getParentNode()){
            NamedNodeMap attributes = node.getAttributes();
            if (attributes == null) continue;
            for (int i = 0; i < attributes.getLength(); ++i) {
                Node attribute = attributes.item(i);
                if (attribute.getNodeName().equals("draft") && attribute.getNodeValue().equals("true")) return true;
            }
        }
        return false;
    }
    
    public static String getXPath(Node node) {
        StringBuffer xpathFragment = new StringBuffer();
        StringBuffer xpath = new StringBuffer();
        for (; node.getNodeType() != Node.DOCUMENT_NODE; node = node.getParentNode()){
            xpathFragment.setLength(0);
            xpathFragment.append('/').append(node.getNodeName());
            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); ++i) {
                	Node attribute = attributes.item(i);
                	xpathFragment.append("[@").append(attribute.getNodeName()).append('=')
					    .append(attribute.getNodeValue()).append(']');
                }
            }
            xpath.insert(0, xpathFragment);
        }
        xpath.insert(0, '/');
        return xpath.toString();   
    }
    
    public static String getParent(String locale) {
        int pos = locale.lastIndexOf('_');
        if (pos >= 0) {
            return locale.substring(0,pos);
        }
        if (!locale.equals("root")) return "root";
        return null;
    }
    

    static class CldrCollations {
        Set validLocales = new TreeSet();
        Map ulocale_rules = new TreeMap(ULocaleComparator);
        Map locale_types_rules = new TreeMap();
        String sourceDir;
        Map collation_collation = new HashMap();
        RuleBasedCollator emptyCollator = (RuleBasedCollator) Collator.getInstance(new ULocale(""));
        
        public Set getAvailableSet() {
        	return ulocale_rules.keySet();
        }
        
		public RuleBasedCollator getInstance(ULocale locale) {
			return (RuleBasedCollator) ulocale_rules.get(locale);
		}

		void show() {
            log.println("Showing Locales");
            log.println("Unique Collators: " + collation_collation.size());
        	for (Iterator it2 = ulocale_rules.keySet().iterator(); it2.hasNext();) {
                ULocale locale = (ULocale) it2.next();
                RuleBasedCollator col = (RuleBasedCollator) ulocale_rules.get(locale);
                log.println("\t" + locale + ", " + col.getRules());   
            }
        }
        
        CldrCollations(String sourceDir, String localeRegex) throws Exception {
        	this.sourceDir = sourceDir;
            Set s = getMatchingXMLFiles(sourceDir, localeRegex);
            for (Iterator it = s.iterator(); it.hasNext();) {
            	getCollationRules((String) it.next());
            }

            // now fixup the validLocales, adding in what they inherit
            // TODO, add check: validSubLocales are masked by intervening locales.
            for (Iterator it = validLocales.iterator(); it.hasNext(); ) {
                String locale = (String) it.next();
            	Map types_rules = (Map) locale_types_rules.get(locale);
                if (types_rules != null) log.println("Weird: overlap in validLocales: " + locale);
                else {
                    for (String parentlocale = getParent(locale); parentlocale != null; locale = getParent(parentlocale)) {
                        types_rules = (Map) locale_types_rules.get(parentlocale);
                        if (types_rules != null) {
                        	locale_types_rules.put(locale, types_rules);
                            break;
                        }
                    }
                }
            }
            // now generate the @-style locales
            for (Iterator it = locale_types_rules.keySet().iterator(); it.hasNext(); ) {
            	String locale = (String) it.next();
                Map types_rules = (Map) locale_types_rules.get(locale);
                for (Iterator it2 = types_rules.keySet().iterator(); it2.hasNext(); ) {
                	String type = (String) it2.next();
                    RuleBasedCollator col = (RuleBasedCollator) types_rules.get(type);
                    String name = type.equals("standard") ? locale : locale + "@collation=" + type;
                    ulocale_rules.put(new ULocale(name), col);
                }
            }
        }
        
        public static String replace(String source, String pattern, String replacement) {
            // dumb code for now
            for (int pos = source.indexOf(pattern, 0); pos >= 0; pos = source.indexOf(pattern, pos + 1)) {
            	source = source.substring(0, pos) + replacement + source.substring(pos+pattern.length());
            }
            return source;
        }
        static Transliterator fromHex = Transliterator.getInstance("hex-any");
        
        private void getCollationRules(String locale) throws Exception {
            System.out.println(locale);
            Document doc = LDMLUtilities.getFullyResolvedLDML(sourceDir, locale, false, false, false);
            Node node = LDMLUtilities.getNode(doc, "//ldml/collations");
            LDML2ICUConverter cnv = new LDML2ICUConverter();
            StringBuffer stringBuffer = new StringBuffer();
            ICUResourceWriter.ResourceTable resource = (ICUResourceWriter.ResourceTable) cnv.parseCollations(node, stringBuffer);
            Map types_rules = new TreeMap();
            locale_types_rules.put(locale, types_rules);
            for (Resource current = resource.first; current != null; current = current.next) {
                //System.out.println(current.name);
                if (current instanceof ICUResourceWriter.ResourceTable) {
                    ICUResourceWriter.ResourceTable table = (ICUResourceWriter.ResourceTable) current;
                    for (Resource current2 = table.first; current2 != null; current2 = current2.next) {
                    	if (current2 instanceof ICUResourceWriter.ResourceString) {
                    		ICUResourceWriter.ResourceString foo = (ICUResourceWriter.ResourceString) current2;
                    		//System.out.println("\t" + foo.name + ", " + foo.val);
                            /* skip since the utilities have the wrong value
                            if (current.name.equals("validSubLocales")) {
                                // skip since it is wrong
                                log.println("Valid Sub Locale: " + foo.name);
                            	validLocales.add(foo.name);
                            } else 
                            */
                            if (foo.name.equals("Sequence")) {
                                // remove the \ u's, because they blow up
                                String rules = fromHex.transliterate(foo.val);
                                RuleBasedCollator fixed = generateCollator(locale, current.name, foo.name, rules);
                                if (fixed != null) {
                                    log.println("Rules for: " + locale + "," + current.name);
                                    log.println(rules);
                                    if (!rules.equals(foo.val)) {
                                        log.println("Original Rules from Ram: ");
                                        log.println(foo.val);
                                    }
                                    types_rules.put(current.name, fixed);
                                }
                            }
                        }
                    }
                }
                //current.write(System.out,0,false);            
            }
            // now get the valid sublocales
            Document doc2 = LDMLUtilities.parse(sourceDir + locale + ".xml", false);
            Node colls = LDMLUtilities.getNode(doc2,"//ldml/collations");
            String validSubLocales = LDMLUtilities.getAttributeValue(colls, "validSubLocales");
            if (validSubLocales != null) {
                String items[] = new String[100]; // allocate plenty
                Utility.split(validSubLocales, ' ', items);
                for (int i = 0; items[i].length() != 0; ++i) {
                    log.println("Valid Sub Locale: " + items[i]);
                    validLocales.add(items[i]);            	
                }
            }
        }

		/**
		 * @param locale
		 * @param current
		 * @param foo
		 * @param rules
		 */
		private RuleBasedCollator generateCollator(String locale, String current, String foo, String rules) {
            RuleBasedCollator fixed = null;
			try {
			    if (rules.equals("")) fixed = emptyCollator;
			    else {
			        rules = replace(rules, "[optimize[", "[optimize [");
			        rules = replace(rules, "[suppressContractions[", "[suppressContractions [");
			        RuleBasedCollator col = new RuleBasedCollator(rules);
			        fixed = (RuleBasedCollator) collation_collation.get(col);
			        if (fixed == null) {
			        	collation_collation.put(col, col);
			            fixed = col;
			        }
			    }
			} catch (Exception e) {
				log.println("***Cannot create collator from: " + locale + ", " + current + ", " + foo + ", " + rules);
			    e.printStackTrace(log);
			    RuleBasedCollator coll = (RuleBasedCollator)Collator.getInstance(new ULocale(locale));
			    String oldrules = coll.getRules();
			    log.println("Old ICU4J: " + oldrules);
			    log.println("Equal?: " + oldrules.equals(rules));
			}
			return fixed;
		}
    }
    // ========== UNICODESET UTILITIES ==========

    public static interface Apply {
    	String apply(String source);
    }   
    static UnicodeSet apply(UnicodeSet source, Apply apply) {
        UnicodeSet target = new UnicodeSet();
        for (UnicodeSetIterator usi = new UnicodeSetIterator(source); usi.next(); ) {
            String s = usi.getString();
            target.add(apply.apply(s));
        }
        return target;
    }
    static UnicodeSet nfc(UnicodeSet source) {
        return apply(source, new Apply() {
			public String apply(String source) {
				return Normalizer.compose(source, false);
			}
        });
    }
    
    public static interface CloseCodePoint {
    	/**
    	 * @param cp code point to get closure for
    	 * @param toAddTo Unicode set for the closure
    	 * @return toAddTo (for chaining)
    	 */
    	UnicodeSet close(int cp, UnicodeSet toAddTo);
    }
    
    public static UnicodeSet createCaseClosure(UnicodeSet source) {
        UnicodeSet target = new UnicodeSet();
        for (UnicodeSetIterator usi = new UnicodeSetIterator(source); usi.next(); ) {
            String s = usi.getString();
            UnicodeSet temp = createClosure(s, CCCP);
            if (temp == null) target.add(s);
            else target.addAll(temp);
        }
        return target;
    }
    
    public static final CloseCodePoint CCCP = new CloseCodePoint() {
        Locale locale = Locale.ENGLISH;
        UnicodeSet NONE = new UnicodeSet();
        UnicodeMap map = new UnicodeMap();
        
		public UnicodeSet close(int cp, UnicodeSet toAddTo) {
            UnicodeSet result = (UnicodeSet) map.getValue(cp);
            if (result == null) {
    			result = new UnicodeSet();
                result.add(cp);
                String s = UCharacter.toLowerCase(locale, UTF16.valueOf(cp));
                result.add(s);
                s = UCharacter.toUpperCase(locale, UTF16.valueOf(cp));
                result.add(s);
                s = UCharacter.toTitleCase(locale, UTF16.valueOf(cp), null);
                result.add(s);
                // special hack
                if (result.contains("SS")) result.add("sS").add("ss");
                if (result.size() == 1) result = NONE;
                map.put(cp, result);
            }
            if (result != NONE) toAddTo.addAll(result);
            else toAddTo.add(cp);
            return toAddTo;
		}
    };
    
    public static UnicodeSet createClosure(String source, CloseCodePoint closer) {
    	return createClosure(source, 0, closer);
    }
    public static UnicodeSet createClosure(String source, int position, CloseCodePoint closer) {
        UnicodeSet result = new UnicodeSet();
        // if at end, return empty set
        if (position >= source.length()) return result;
    	int cp = UTF16.charAt(source, position);
        // if last character, return its set
        int endPosition = position + UTF16.getCharCount(cp);
        if (endPosition >= source.length()) return closer.close(cp, result);
        // otherwise concatenate its set with the remainder
        UnicodeSet remainder = createClosure(source, endPosition, closer);
        return createAppend(closer.close(cp, result), remainder);
    }
    
    /**
     * Produce the result of appending each element of this to each element of other.
     * That is, [a{cd}] + [d{ef}] => [{ad}{aef}{cdd}{cdef}]
     * @param other
     * @return
     */
    public static UnicodeSet createAppend(UnicodeSet a, UnicodeSet b) {
        UnicodeSet target = new UnicodeSet();
        for (UnicodeSetIterator usi = new UnicodeSetIterator(a); usi.next(); ) {
            String s = usi.getString();
            for (UnicodeSetIterator usi2 = new UnicodeSetIterator(b); usi2.next(); ) {
                String s2 = usi2.getString();
                target.add(s + s2);
            }
        }
        return target;
    }
}