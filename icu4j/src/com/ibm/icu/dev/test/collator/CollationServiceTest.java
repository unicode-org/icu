/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.collator;

import java.util.Collections;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Collator.CollatorFactory;
import com.ibm.icu.util.ULocale;

public class CollationServiceTest extends TestFmwk {
    public static void main(String[] args) {
        new CollationServiceTest().run(args);
    }

    public void TestRegister() {
        // register a singleton
        Collator frcol = Collator.getInstance(Locale.FRANCE);
        Collator uscol = Collator.getInstance(Locale.US);
        	
        { // try override en_US collator
            Object key = Collator.registerInstance(frcol, Locale.US);
            Collator ncol = Collator.getInstance(Locale.US);
            if (!frcol.equals(ncol)) {
                errln("register of french collator for en_US failed");
            }

    	    // coverage
    	    Collator test = Collator.getInstance(Locale.GERMANY); // CollatorFactory.handleCreate

            if (!Collator.unregister(key)) {
                errln("failed to unregister french collator");
            }
            ncol = Collator.getInstance(Locale.US);
            if (!uscol.equals(ncol)) {
                errln("collator after unregister does not match original");
            }
        }

        Locale fu_FU = new  Locale("fu", "FU", "FOO");

        { // try create collator for new locale
            Collator fucol = Collator.getInstance(fu_FU);
            Object key = Collator.registerInstance(frcol, fu_FU);
            Collator ncol = Collator.getInstance(fu_FU);
            if (!frcol.equals(ncol)) {
                errln("register of fr collator for fu_FU failed");
            }
            
            Locale[] locales = Collator.getAvailableLocales();
            boolean found = false;
            for (int i = 0; i < locales.length; ++i) {
                if (locales[i].equals(fu_FU)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errln("new locale fu_FU not reported as supported locale");
            }
            
            String name = Collator.getDisplayName(fu_FU);
            if (!"fu (FU,FOO)".equals(name)) {
                errln("found " + name + " for fu_FU");
            }

            name = Collator.getDisplayName(fu_FU, fu_FU);
            if (!"fu (FU,FOO)".equals(name)) {
                errln("found " + name + " for fu_FU");
            }

            if (!Collator.unregister(key)) {
                errln("failed to unregister french collator");
            }
            ncol = Collator.getInstance(fu_FU);
            if (!fucol.equals(ncol)) {
                errln("collator after unregister does not match original fu_FU");
            }
        }

    	{
    	    // coverage after return to default 
    	    Locale[] locales = Collator.getAvailableLocales();
    
    	    Collator ncol = Collator.getInstance(Locale.US);
    	}
    }

    public void TestRegisterFactory() {

        class CollatorInfo {
            Locale locale;
            Collator collator;
            Map displayNames; // locale -> string

            CollatorInfo(Locale locale, Collator collator, Map displayNames) {
                this.locale = locale;
                this.collator = collator;
                this.displayNames = displayNames;
            }

            String getDisplayName(Locale displayLocale) {
                String name = null;
                if (displayNames != null) {
                    name = (String)displayNames.get(displayLocale);
                }
                if (name == null) {
                    name = locale.getDisplayName(displayLocale);
                }
                return name;
            }
        }

        class TestFactory extends CollatorFactory {
            private Map map;
            private Set ids;
            
            TestFactory(CollatorInfo[] info) {
                map = new HashMap();
                for (int i = 0; i < info.length; ++i) {
                    CollatorInfo ci = info[i];
                    map.put(ci.locale, ci);
                }
            }

            public Collator createCollator(Locale loc) {
                CollatorInfo ci = (CollatorInfo)map.get(loc);
                if (ci != null) {
                    return ci.collator;
                }
                return null;
            }

            public String getDisplayName(Locale objectLocale, Locale displayLocale) {
                CollatorInfo ci = (CollatorInfo)map.get(objectLocale);
                if (ci != null) {
                    return ci.getDisplayName(displayLocale);
                }
                return null;
            }

            public Set getSupportedLocaleIDs() {
                if (ids == null) {
                    HashSet set = new HashSet();
                    Iterator iter = map.keySet().iterator();
                    while (iter.hasNext()) {
                        Locale locale = (Locale)iter.next();
                        String id = LocaleUtility.canonicalLocaleString(locale.toString());
                        set.add(id);
                    }
                    ids = Collections.unmodifiableSet(set);
                }
                return ids;
            }
        }
	
    	class TestFactoryWrapper extends CollatorFactory {
    	    CollatorFactory delegate;
    
    	    TestFactoryWrapper(CollatorFactory delegate) {
    		this.delegate = delegate;
    	    }
    
                public Collator createCollator(Locale loc) {
    		return delegate.createCollator(loc);
    	    }
    
    	    // use CollatorFactory getDisplayName(Locale, Locale) for coverage
    
                public Set getSupportedLocaleIDs() {
    		return delegate.getSupportedLocaleIDs();
    	    }
    	}

        Locale fu_FU = new Locale("fu", "FU", "");
        Locale fu_FU_FOO = new Locale("fu", "FU", "FOO");

        Map fuFUNames = new HashMap();
        fuFUNames.put(fu_FU, "ze leetle bunny Fu-Fu");
        fuFUNames.put(fu_FU_FOO, "zee leetel bunny Foo-Foo");
        fuFUNames.put(Locale.US, "little bunny Foo Foo");

        Collator frcol = Collator.getInstance(Locale.FRANCE);
       /* Collator uscol = */Collator.getInstance(Locale.US);
        Collator gecol = Collator.getInstance(Locale.GERMANY);
        Collator jpcol = Collator.getInstance(Locale.JAPAN);
        Collator fucol = Collator.getInstance(fu_FU);
        
        CollatorInfo[] info = {
            new CollatorInfo(Locale.US, frcol, null),
            new CollatorInfo(Locale.FRANCE, gecol, null),
            new CollatorInfo(fu_FU, jpcol, fuFUNames),
        };

        TestFactory factory = new TestFactory(info);

    	// coverage
    	{
    	    TestFactoryWrapper wrapper = new TestFactoryWrapper(factory); // in java, gc lets us easily multiply reference!
    	    Object key = Collator.registerFactory(wrapper);
    	    String name = Collator.getDisplayName(fu_FU, fu_FU_FOO);
    	    logln("*** default name: " + name);
    	    Collator.unregister(key);
    
    	    Collator col = Collator.getInstance(new Locale("bar", "BAR"));
    	}

        int n1 = checkAvailable("before registerFactory");
        
        {
            Object key = Collator.registerFactory(factory);
            
            int n2 = checkAvailable("after registerFactory");
            
            Collator ncol = Collator.getInstance(Locale.US);
            if (!frcol.equals(ncol)) {
                errln("frcoll for en_US failed");
            }

            ncol = Collator.getInstance(fu_FU_FOO);
            if (!jpcol.equals(ncol)) {
                errln("jpcol for fu_FU_FOO failed, got: " + ncol);
            }
            
            Locale[] locales = Collator.getAvailableLocales();
            boolean found = false;
            for (int i = 0; i < locales.length; ++i) {
                if (locales[i].equals(fu_FU)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errln("new locale fu_FU not reported as supported locale");
            }
            
            String name = Collator.getDisplayName(fu_FU);
            if (!"little bunny Foo Foo".equals(name)) {
                errln("found " + name + " for fu_FU");
            }

            name = Collator.getDisplayName(fu_FU, fu_FU_FOO);
            if (!"zee leetel bunny Foo-Foo".equals(name)) {
                errln("found " + name + " for fu_FU in fu_FU_FOO");
            }

            if (!Collator.unregister(key)) {
                errln("failed to unregister factory");
            }

            int n3 = checkAvailable("after unregister");
            assertTrue("register increases count", n2>n1);
            assertTrue("unregister restores count", n3==n1);
            
            ncol = Collator.getInstance(fu_FU);
            if (!fucol.equals(ncol)) {
                errln("collator after unregister does not match original fu_FU");
            }
        }
    }

    /**
     * Check the integrity of the results of Collator.getAvailableLocales().
     * Return the number of items returned.
     */
    int checkAvailable(String msg) {
        Locale locs[] = Collator.getAvailableLocales();
        if (!assertTrue("getAvailableLocales != null", locs!=null)) return -1;
        checkArray(msg, locs, null);
        ULocale ulocs[] = Collator.getAvailableULocales();
        if (!assertTrue("getAvailableULocales != null", ulocs!=null)) return -1;
        checkArray(msg, ulocs, null);
        assertTrue("getAvailableLocales().length == getAvailableULocales().length",
                   locs.length == ulocs.length);
        return locs.length;
    }
    
    private static final String KW[] = {
        "collation"
    };

    private static final String KWVAL[] = {
        "phonebook",
        "stroke"
    };

    public void TestSeparateTrees() {
        String kw[] = Collator.getKeywords();
        if (!assertTrue("getKeywords != null", kw!=null)) return;
        checkArray("getKeywords", kw, KW);
        
        String kwval[] = Collator.getKeywordValues(KW[0]);
        if (!assertTrue("getKeywordValues != null", kwval!=null)) return;
        checkArray("getKeywordValues", kwval, KWVAL);

        boolean isAvailable[] = new boolean[1];
        ULocale equiv = Collator.getFunctionalEquivalent(KW[0],
                                                         new ULocale("fr"),
                                                         isAvailable);
        if (assertTrue("getFunctionalEquivalent(fr)!=null", equiv!=null)) {
            assertEquals("getFunctionalEquivalent(fr)", "fr", equiv.toString());
        }
        assertTrue("getFunctionalEquivalent(fr).isAvailable==true",
                   isAvailable[0] == true);
        
        equiv = Collator.getFunctionalEquivalent(KW[0],
                                                 new ULocale("fr_FR"),
                                                 isAvailable);
        if (assertTrue("getFunctionalEquivalent(fr_FR)!=null", equiv!=null)) {
            assertEquals("getFunctionalEquivalent(fr_FR)", "fr", equiv.toString());
        }
        assertTrue("getFunctionalEquivalent(fr_FR).isAvailable==false",
                   isAvailable[0] == false);
    }
}
