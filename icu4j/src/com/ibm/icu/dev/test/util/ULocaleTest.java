/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: January 14 2004
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberFormat.*;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import java.lang.reflect.*;
import java.util.Locale;

public class ULocaleTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new ULocaleTest().run(args);
    }
    
    public void TestCalendar() {
        // TODO The CalendarFactory mechanism is not public,
        // so we can't test it yet.  If it becomes public,
        // enable this code.

        // class CFactory implements CalendarFactory {
        //     Locale loc;
        //     Calendar proto;
        //     public CFactory(Locale locale, Calendar prototype) {
        //         loc = locale;
        //         proto = prototype;
        //     }
        //     public Calendar create(TimeZone tz, Locale locale) {
        //         // ignore tz -- not relevant to this test
        //         return locale.equals(loc) ?
        //             (Calendar) proto.clone() : null;
        //     }
        //     public String factoryName() {
        //         return "CFactory";
        //     }
        // };

        checkService("en_US_BROOKLYN", new ServiceFacade() {
            public Object create(Locale req) {
                return Calendar.getInstance(req);
            }
        // }, null, new Registrar() {
        //     public Object register(Locale loc, Object prototype) {
        //         CFactory f = new CFactory(loc, (Calendar) prototype);
        //         return Calendar.register(f, loc);
        //     }
        //     public boolean unregister(Object key) {
        //         return Calendar.unregister(key);
        //     }
        });
    }

    public void TestCurrency() {
        checkService("zh_TW_TAIPEI", new ServiceFacade() {
            public Object create(Locale req) {
                return Currency.getInstance(req);
            }
        }, null, new Registrar() {
            public Object register(Locale loc, Object prototype) {
                return Currency.registerInstance((Currency) prototype, loc);
            }
            public boolean unregister(Object key) {
                return Currency.unregister(key);
            }
        });
    }

    public void TestBreakIterator() {
        checkService("hi_IN_BHOPAL", new ServiceFacade() {
            public Object create(Locale req) {
                return BreakIterator.getWordInstance(req);
            }
        }, null, new Registrar() {
            public Object register(Locale loc, Object prototype) {
                return BreakIterator.registerInstance(
                            (BreakIterator) prototype,
                            loc, BreakIterator.KIND_WORD);
            }
            public boolean unregister(Object key) {
                return BreakIterator.unregister(key);
            }            
        });
    }

    public void TestCollator() {
        checkService("ja_JP_YOKOHAMA", new ServiceFacade() {
            public Object create(Locale req) {
                return Collator.getInstance(req);
            }
        }, null, new Registrar() {
            public Object register(Locale loc, Object prototype) {
                return Collator.registerInstance((Collator) prototype, loc);
            }
            public boolean unregister(Object key) {
                return Collator.unregister(key);
            }            
        });
    }

    public void TestDateFormat() {
        checkService("de_CH_ZURICH", new ServiceFacade() {
            public Object create(Locale req) {
                return DateFormat.getDateInstance(DateFormat.DEFAULT, req);
            }
        }, new Subobject() {
            public Object get(Object parent) {
                return ((SimpleDateFormat) parent).getDateFormatSymbols();
            }
        }, null);
    }

    public void TestNumberFormat() {
        class NFactory extends SimpleNumberFormatFactory {
            NumberFormat proto;
            Locale locale;
            public NFactory(Locale loc, NumberFormat fmt) {
                super(loc);
                this.locale = loc;
                this.proto = fmt;
            }
            public NumberFormat createFormat(Locale loc, int formatType) {
                return (NumberFormat) (locale.equals(loc) ?
                                       proto.clone() : null);
            }
        };

        checkService("fr_FR_NICE", new ServiceFacade() {
            public Object create(Locale req) {
                return NumberFormat.getInstance(req);
            }
        }, new Subobject() {
            public Object get(Object parent) {
                return ((DecimalFormat) parent).getDecimalFormatSymbols();
            }
        }, new Registrar() {
            public Object register(Locale loc, Object prototype) {
                NFactory f = new NFactory(loc, (NumberFormat) prototype);
                return NumberFormat.registerFactory(f);
            }
            public boolean unregister(Object key) {
                return NumberFormat.unregister(key);
            }
        });
    }

    // ================= Infrastructure =================

    /**
     * Compare two locale IDs.  If they are equal, return 0.  If `string'
     * starts with `prefix' plus an additional element, that is, string ==
     * prefix + '_' + x, then return 1.  Otherwise return a value < 0.
     */
    static int loccmp(String string, String prefix) {
        int slen = string.length(),
                plen = prefix.length();
        /* 'root' is "less than" everything */
        if (prefix.equals("root")) {
            return string.equals("root") ? 0 : 1;
        }
        // ON JAVA (only -- not on C -- someone correct me if I'm wrong)
        // consider "" to be an alternate name for "root".
        if (plen == 0) {
            return slen == 0 ? 0 : 1;
        }
        if (!string.startsWith(prefix)) return -1; /* mismatch */
        if (slen == plen) return 0;
        if (string.charAt(plen) == '_') return 1;
        return -2; /* false match, e.g. "en_USX" cmp "en_US" */
    }

    /**
     * Check the relationship between requested locales, and report problems.
     * The caller specifies the expected relationships between requested
     * and valid (expReqValid) and between valid and actual (expValidActual).
     * Possible values are:
     * "gt" strictly greater than, e.g., en_US > en
     * "ge" greater or equal,      e.g., en >= en
     * "eq" equal,                 e.g., en == en
     */
    void checklocs(String label,
                   String req,
                   Locale validLoc,
                   Locale actualLoc,
                   String expReqValid,
                   String expValidActual) {
        String valid = validLoc.toString();
        String actual = actualLoc.toString();
        int reqValid = loccmp(req, valid);
        int validActual = loccmp(valid, actual);
        if (((expReqValid.equals("gt") && reqValid > 0) ||
             (expReqValid.equals("ge") && reqValid >= 0) ||
             (expReqValid.equals("eq") && reqValid == 0)) &&
            ((expValidActual.equals("gt") && validActual > 0) ||
             (expValidActual.equals("ge") && validActual >= 0) ||
             (expValidActual.equals("eq") && validActual == 0))) {
            logln("Ok: " + label + "; req=" + req + ", valid=" + valid +
                  ", actual=" + actual);
        } else {
            errln("FAIL: " + label + "; req=" + req + ", valid=" + valid +
                  ", actual=" + actual);
        }
    }

    /**
     * Interface used by checkService defining a protocol to create an
     * object, given a requested locale.
     */
    interface ServiceFacade {
        Object create(Locale requestedLocale);
    }

    /**
     * Interface used by checkService defining a protocol to get a
     * contained subobject, given its parent object.
     */
    interface Subobject {
        Object get(Object parent);
    }

    /**
     * Interface used by checkService defining a protocol to register
     * and unregister a service object prototype.
     */
    interface Registrar {
        Object register(Locale loc, Object prototype);
        boolean unregister(Object key);
    }

    /**
     * Use reflection to call getLocale() on the given object to
     * determine both the valid and the actual locale.  Verify these
     * for correctness.
     */
    void checkObject(String requestedLocale, Object obj,
                     String expReqValid, String expValidActual) {
        Class[] params = new Class[] { ULocale.Type.class };
        try {
            Class cls = obj.getClass();
            Method getLocale = cls.getMethod("getLocale", params);
            ULocale valid = (ULocale) getLocale.invoke(obj, new Object[] {
                ULocale.VALID_LOCALE });
            ULocale actual = (ULocale) getLocale.invoke(obj, new Object[] {
                ULocale.ACTUAL_LOCALE });
            checklocs(cls.getName(), requestedLocale,
                      valid.toLocale(), actual.toLocale(),
                      expReqValid, expValidActual);
        }
        
        // Make the following exceptions _specific_ -- do not
        // catch(Exception), since that will catch the exception
        // that errln throws.
        catch(NoSuchMethodException e1) {
            errln("FAIL: reflection failed: " + e1);
        } catch(SecurityException e2) {
            errln("FAIL: reflection failed: " + e2);
        } catch(IllegalAccessException e3) {
            errln("FAIL: reflection failed: " + e3);
        } catch(IllegalArgumentException e4) {
            errln("FAIL: reflection failed: " + e4);
        } catch(InvocationTargetException e5) {
            errln("FAIL: reflection failed: " + e5);
        }
    }

    /**
     * Verify the correct getLocale() behavior for the given service.
     * @param requestedLocale the locale to request.  This MUST BE
     * FAKE.  In other words, it should be something like
     * en_US_FAKEVARIANT so this method can verify correct fallback
     * behavior.
     * @param svc a factory object that can create the object to be
     * tested.  This isn't necessary here (one could just pass in the
     * object) but is required for the overload of this method that
     * takes a Registrar.
     */
    void checkService(String requestedLocale, ServiceFacade svc) {
        checkService(requestedLocale, svc, null, null);
    }

    /**
     * Verify the correct getLocale() behavior for the given service.
     * @param requestedLocale the locale to request.  This MUST BE
     * FAKE.  In other words, it should be something like
     * en_US_FAKEVARIANT so this method can verify correct fallback
     * behavior.
     * @param svc a factory object that can create the object to be
     * tested.
     * @param sub an object that can be used to retrieve a subobject
     * which should also be tested.  May be null.
     * @param reg an object that supplies the registration and
     * unregistration functionality to be tested.  May be null.
     */
    void checkService(String requestedLocale, ServiceFacade svc,
                      Subobject sub, Registrar reg) {
        Locale req = LocaleUtility.getLocaleFromName(requestedLocale);
        Object obj = svc.create(req);
        checkObject(requestedLocale, obj, "gt", "ge");
        if (sub != null) {
            Object subobj = sub.get(obj);
            checkObject(requestedLocale, subobj, "gt", "ge");
        }
        if (reg != null) {
            logln("Info: Registering service");
            Object key = reg.register(req, obj);
            Object objReg = svc.create(req);
            checkObject(requestedLocale, objReg, "eq", "eq");
            if (sub != null) {
                Object subobj = sub.get(obj);
                // Assume subobjects don't come from services, so
                // their metadata should be structured normally.
                checkObject(requestedLocale, subobj, "gt", "ge");
            }
            logln("Info: Unregistering service");
            if (!reg.unregister(key)) {
                errln("FAIL: unregister failed");
            }
            Object objUnreg = svc.create(req);
            checkObject(requestedLocale, objUnreg, "gt", "ge");
        }
    }
    private static final int LOCALE_SIZE = 9;
    private static final String[][] rawData2 = new String[][]{
            /* language code */
            {   "en",   "fr",   "ca",   "el",   "no",   "zh",   "de",   "es",  "ja"    },
            /* script code */
            {   "",     "",     "",     "",     "",     "Hans", "", "", ""  },
            /* country code */
            {   "US",   "FR",   "ES",   "GR",   "NO",   "CN", "DE", "", "JP"    },
            /* variant code */
            {   "",     "",     "",     "",     "NY",   "", "", "", ""      },
            /* full name */
            {   "en_US",    "fr_FR",    "ca_ES",    
                "el_GR",    "no_NO_NY", "zh_Hans_CN", 
                "de_DE@collation=phonebook", "es@collation=traditional",  "ja_JP@calendar=japanese" },
            /* ISO-3 language */
            {   "eng",  "fra",  "cat",  "ell",  "nor",  "zho", "deu", "spa", "jpn"   },
            /* ISO-3 country */
            {   "USA",  "FRA",  "ESP",  "GRC",  "NOR",  "CHN", "DEU", "", "JPN"   },
            /* LCID */
            {   "409", "40c", "403", "408", "814",  "804", "407", "a", "411"     },

            /* display language (English) */
            {   "English",  "French",   "Catalan", "Greek",    "Norwegian", "Chinese", "German", "Spanish", "Japanese"    },
            /* display script code (English) */
            {   "",     "",     "",     "",     "",     "Simplified Han", "", "", ""       },
            /* display country (English) */
            {   "United States",    "France",   "Spain",  "Greece",   "Norway", "China", "Germany", "", "Japan"       },
            /* display variant (English) */
            {   "",     "",     "",     "",     "NY",  "", "", "", ""       },
            /* display name (English) */
            {   "English (United States)", "French (France)", "Catalan (Spain)", 
                "Greek (Greece)", "Norwegian (Norway, NY)", "Chinese (Simplified Han, China)", 
                "German (Germany, Collation=Phonebook Order)", "Spanish (Collation=Traditional)", "Japanese (Japan, Calendar=Japanese Calendar)" },

            /* display language (French) */
            {   "anglais",  "fran\\u00E7ais",   "catalan", "grec",    "norv\\u00E9gien",    "chinois", "allemand", "espagnol", "japonais"     },
            /* display script code (French) */
            {   "",     "",     "",     "",     "",     "Hans", "", "", ""         },
            /* display country (French) */
            {   "\\u00C9tats-Unis",    "France",   "Espagne",  "Gr\\u00E8ce",   "Norv\\u00E8ge",    "Chine", "Allemagne", "", "Japon"       },
            /* display variant (French) */
            {   "",     "",     "",     "",     "NY",   "", "", "", ""       },
            /* display name (French) */
            {   "anglais (\\u00C9tats-Unis)", "fran\\u00E7ais (France)", "catalan (Espagne)", 
                "grec (Gr\\u00E8ce)", "norv\\u00E9gien (Norv\\u00E8ge, NY)",  "chinois (Hans, Chine)", 
                "allemand (Allemagne, Ordonnancement=Ordre de l'annuaire)", "espagnol (Ordonnancement=Ordre traditionnel)", "japonais (Japon, Calendrier=Calendrier japonais)" },

            /* display language (Catalan) */
            {   "angl\\u00E8s", "franc\\u00E8s", "catal\\u00E0", "grec",  "noruec", "xin\\u00E9s", "alemany", "espanyol", "japon\\u00E8s"    },
            /* display script code (Catalan) */
            {   "",     "",     "",     "",     "",     "Hans", "", "", ""         },
            /* display country (Catalan) */
            {   "Estats Units", "Fran\\u00E7a", "Espanya",  "Gr\\u00E8cia", "Noruega",  "Xina", "Alemanya", "", "Jap\\u00F3"    },
            /* display variant (Catalan) */
            {   "", "", "",                    "", "NY",    "", "", "", ""    },
            /* display name (Catalan) */
            {   "angl\\u00E8s (Estats Units)", "franc\\u00E8s (Fran\\u00E7a)", "catal\\u00E0 (Espanya)", 
            "grec (Gr\\u00E8cia)", "noruec (Noruega, NY)", "xin\\u00E9s (Hans, Xina)", 
            "alemany (Alemanya, COLLATION=PHONEBOOK)", "espanyol (COLLATION=TRADITIONAL)", "japon\\u00E8s (Jap\\u00F3, CALENDAR=JAPANESE)" },

            /* display language (Greek) */
            {
                "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac",
                "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac",
                "\\u039a\\u03b1\\u03c4\\u03b1\\u03bb\\u03b1\\u03bd\\u03b9\\u03ba\\u03ac",
                "\\u0395\\u03bb\\u03bb\\u03b7\\u03bd\\u03b9\\u03ba\\u03ac",
                "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac",
                "\\u039A\\u03B9\\u03BD\\u03B5\\u03B6\\u03B9\\u03BA\\u03AC", 
                "\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC", 
                "\\u0399\\u03C3\\u03C0\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC", 
                "\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03B9\\u03BA\\u03AC"   
            },
            /* display script code (Greek) */
            {   "",     "",     "",     "",     "",     "Hans", "", "", ""         },
            /* display country (Greek) */
            {
                "\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2",
                "\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1",
                "\\u0399\\u03c3\\u03c0\\u03b1\\u03bd\\u03af\\u03b1",
                "\\u0395\\u03bb\\u03bb\\u03ac\\u03b4\\u03b1",
                "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1",
                "\\u039A\\u03AF\\u03BD\\u03B1", 
                "\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03AF\\u03B1", 
                "", 
                "\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03AF\\u03B1"   
            },
            /* display variant (Greek) */
            {   "", "", "", "", "NY", "", "", "", ""    }, /* TODO: currently there is no translation for NY in Greek fix this test when we have it */
            /* display name (Greek) */
            {
                "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac (\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2)",
                "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac (\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1)",
                "\\u039a\\u03b1\\u03c4\\u03b1\\u03bb\\u03b1\\u03bd\\u03b9\\u03ba\\u03ac (\\u0399\\u03c3\\u03c0\\u03b1\\u03bd\\u03af\\u03b1)",
                "\\u0395\\u03bb\\u03bb\\u03b7\\u03bd\\u03b9\\u03ba\\u03ac (\\u0395\\u03bb\\u03bb\\u03ac\\u03b4\\u03b1)",
                "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac (\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1, NY)",
                "\\u039A\\u03B9\\u03BD\\u03B5\\u03B6\\u03B9\\u03BA\\u03AC (Hans, \\u039A\\u03AF\\u03BD\\u03B1)", 
                "\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC (\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03AF\\u03B1, COLLATION=PHONEBOOK)", 
                "\\u0399\\u03C3\\u03C0\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC (COLLATION=TRADITIONAL)", 
                "\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03B9\\u03BA\\u03AC (\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03AF\\u03B1, CALENDAR=JAPANESE)"
            }
        };
    private static final int ENGLISH = 0;
    private static final int FRENCH = 1;
    private static final int CATALAN = 2;
    private static final int GREEK = 3;
    private static final int NORWEGIAN = 4;
    private static final int LANG = 0;
    private static final int SCRIPT = 1;
    private static final int CTRY = 2;
    private static final int VAR = 3;
    private static final int NAME = 4;
    private static final int LANG3 = 5;
    private static final int CTRY3 = 6;
    private static final int LCID = 7;
    private static final int DLANG_EN = 8;
    private static final int DSCRIPT_EN = 9;
    private static final int DCTRY_EN = 10;
    private static final int DVAR_EN = 11;
    private static final int DNAME_EN = 12;
    private static final int DLANG_FR = 13;
    private static final int DSCRIPT_FR = 14;
    private static final int DCTRY_FR = 15;
    private static final int DVAR_FR = 16;
    private static final int DNAME_FR = 17;
    private static final int DLANG_CA = 18;
    private static final int DSCRIPT_CA = 19;
    private static final int DCTRY_CA = 20;
    private static final int DVAR_CA = 21;
    private static final int DNAME_CA = 22;
    private static final int DLANG_EL = 23;
    private static final int DSCRIPT_EL = 24;
    private static final int DCTRY_EL = 25;
    private static final int DVAR_EL = 26;
    private static final int DNAME_EL = 27;

    public void TestBasicGetters() {
        int i;
        logln("Testing Basic Getters\n");
        for (i = 0; i < LOCALE_SIZE; i++) {
            String testLocale=(rawData2[NAME][i]);
            logln("Testing "+ testLocale+".....\n");
            
            String lang =ULocale.getLanguage(testLocale);
            if (0 !=lang.compareTo(rawData2[LANG][i]))    {
                errln("  Language code mismatch: "+lang+" versus "+  rawData2[LANG][i]);
            }

            String ctry=ULocale.getCountry(testLocale);
            if (0 !=ctry.compareTo(rawData2[CTRY][i]))    {
                errln("  Country code mismatch: "+ctry+" versus "+  rawData2[CTRY][i]);
            }

            String var=ULocale.getVariant(testLocale);
            if (0 !=var.compareTo(rawData2[VAR][i]))    {
                errln("  Variant code mismatch: "+var+" versus "+  rawData2[VAR][i]);
            }

            String name = ULocale.getName(testLocale);
            if (0 !=name.compareTo(rawData2[NAME][i]))    {
                errln("  Name mismatch: "+name+" versus "+  rawData2[NAME][i]);
            }

        }
    }
    String [][] testData = new String[][]{
        {"sv", "", "FI", "AL", "sv-fi-al", "sv_FI_AL" },
        {"en", "", "GB", "", "en-gb", "en_GB" },
        {"i-hakka", "", "MT", "XEMXIJA", "i-hakka_MT_XEMXIJA", "i-hakka_MT_XEMXIJA"},
        {"i-hakka", "", "CN", "", "i-hakka_CN", "i-hakka_CN"},
        {"i-hakka", "", "MX", "", "I-hakka_MX", "i-hakka_MX"},
        {"x-klingon", "", "US", "SANJOSE", "X-KLINGON_us_SANJOSE", "x-klingon_US_SANJOSE"},
        
        {"mr", "", "", "", "mr.utf8", "mr"},
        {"de", "", "TV", "", "de-tv.koi8r", "de_TV"},
        {"x-piglatin", "", "ML", "", "x-piglatin_ML.MBE", "x-piglatin_ML"},  /* Multibyte English */
        {"i-cherokee", "","US", "", "i-Cherokee_US.utf7", "i-cherokee_US"},
        {"x-filfli", "", "MT", "FILFLA", "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA"},
        {"no", "", "NO", "NY", "no-no-ny.utf32@B", "no_NO_NY"}, /* @ ignored unless variant is empty */
        {"no", "", "NO", "",  "no-no.utf32@B", "no_NO_B" },
        {"no", "", "",   "NY", "no__ny", "no__NY" },
        {"no", "", "",   "", "no@ny", "no__NY" },
        {"el", "Latn", "", "", "el-latn", "el_Latn" },
        {"en", "Cyrl", "RU", "", "en-cyrl-ru", "en_Cyrl_RU" },
        {"zh", "Hant", "TW", "STROKE", "zh-hant_TW_STROKE", "zh_Hant_TW_STROKE" },
        {"qq", "Qqqq", "QQ", "QQ", "qq_Qqqq_QQ_QQ", "qq_Qqqq_QQ_QQ" },
        {"qq", "Qqqq", "", "QQ", "qq_Qqqq__QQ", "qq_Qqqq__QQ" },
        {"12", "3456", "78", "90", "12_3456_78_90", "12_3456_78_90" }, /* total garbage */
        
        { "","","","",""}
    };

    public void TestPrefixes() {
        
        String loc, buf,buf1;
        String [] testTitles = new String[] { "ULocale.getLanguage()", "ULocale.getScript()", "ULocale.getCountry()", "ULocale.getVariant()", "name", "ULocale.getName()", "country3" };
        ULocale uloc;
        
        for(int row=0;testData[row][0].length()!= 0;row++) {
            loc = testData[row][NAME];
            logln("Test #"+row+": "+loc);
            
            uloc = new ULocale(loc);    
            
            for(int n=0;n<=(NAME+1);n++) {
                if(n==NAME) continue;

                switch(n) {
                case LANG:
                    buf  = ULocale.getLanguage(loc);
                    buf1 = uloc.getLanguage();
                    break;
                    
                case SCRIPT:
                    buf  = ULocale.getScript(loc);
                    buf1 = uloc.getScript();
                    break;
                    
                case CTRY:
                    buf  = ULocale.getCountry(loc);
                    buf1 = uloc.getCountry();
                    break;
                    
                case VAR:
                    buf  = ULocale.getVariant(loc);
                    buf1 = buf;
                    break;
                    
                case NAME+1:
                    buf  = ULocale.getName(loc);
                    buf1 = uloc.getName();
                    break;
                    
                default:
                    buf = "**??";
                    buf1 = buf;
                }
                
                logln("#"+row+": "+testTitles[n]+" on "+loc+": -> ["+buf+"]");
                
                if(buf.compareTo(testData[row][n])!=0) {
                    errln("#"+row+": "+testTitles[n]+" on "+loc+": -> ["+buf+"] (expected '"+testData[row][n]+"'!)");
                }
                if(buf1.compareTo(testData[row][n])!=0) {
                    errln("#"+row+": "+testTitles[n]+" on ULocale object "+loc+": -> ["+buf1+"] (expected '"+testData[row][n]+"'!)");
                }
            }
        }
    }
    private static final String[][] tests = new String[][]{
          /* locale, language3, language2, Country3, country2 */  
        { "eng_USA", "eng", "en", "USA", "US" },
        { "kok",  "kok", "kok", "", "" },
        { "in",  "ind", "in", "", "" },
        { "id",  "ind", "id", "", "" }, /* NO aliasing */
        { "sh",  "srp", "sh", "", "" },
        { "zz_FX",  "", "zz", "FXX", "FX" },
        { "zz_RO",  "", "zz", "ROU", "RO" },
        { "zz_TP",  "", "zz", "TMP", "TP" },
        { "zz_TL",  "", "zz", "TLS", "TL" },
        { "zz_ZR",  "", "zz", "ZAR", "ZR" },
        { "zz_FXX",  "", "zz", "FXX", "FX" }, /* no aliasing. Doesn't go to PS(PSE). */
        { "zz_ROM",  "", "zz", "ROU", "RO" },
        { "zz_ROU",  "", "zz", "ROU", "RO" },
        { "zz_ZAR",  "", "zz", "ZAR", "ZR" },
        { "zz_TMP",  "", "zz", "TMP", "TP" },
        { "zz_TLS",  "", "zz", "TLS", "TL" },
        { "mlt_PSE", "mlt", "mt", "PSE", "PS" },
        { "iw", "heb", "iw", "", "" },
        { "ji", "yid", "ji", "", "" },
        { "jw", "jaw", "jw", "", "" },
        { "sh", "srp", "sh", "", "" },
        { "", "", "", "", "" }
    };

    public void TestObsoleteNames(){
        
        for(int i=0;i<tests.length;i++){
            String locale = tests[i][0];
            logln("** Testing : "+ locale);
            String buff, buff1;
            ULocale uloc  = new ULocale(locale);
            
            buff = ULocale.getISO3Language(locale);
            if(buff.compareTo(tests[i][1])!=0){
                errln("FAIL: ULocale.getISO3Language("+locale+")=="+
                        buff+",\t expected "+tests[i][1]);
            }else{
                logln("   ULocale.getISO3Language("+locale+")=="+buff);
            }
            
            buff1 = uloc.getISO3Language();
            if(buff1.compareTo(tests[i][1])!=0){
                errln("FAIL: ULocale.getISO3Language("+locale+")=="+
                        buff+",\t expected "+tests[i][1]);
            }else{
                logln("   ULocale.getISO3Language("+locale+")=="+buff);
            }
            
            buff = ULocale.getLanguage(locale);
            if(buff.compareTo(tests[i][2])!=0){
                   errln("FAIL: ULocale.getLanguage("+locale+")=="+
                        buff+",\t expected "+tests[i][2]);
            }else{
                logln("   ULocale.getLanguage("+locale+")=="+buff);
            }
            
            buff = ULocale.getISO3Country(locale);
            if(buff.compareTo(tests[i][3])!=0){
                errln("FAIL: ULocale.getISO3Country("+locale+")=="+
                        buff+",\t expected "+tests[i][3]);
            }else{
                logln("   ULocale.getISO3Country("+locale+")=="+buff);
            }

            buff1 = uloc.getISO3Country();
            if(buff1.compareTo(tests[i][3])!=0){
                errln("FAIL: ULocale.getISO3Country("+locale+")=="+
                        buff+",\t expected "+tests[i][3]);
            }else{
                logln("   ULocale.getISO3Country("+locale+")=="+buff);
            }

            buff = ULocale.getCountry(locale);
            if(buff.compareTo(tests[i][4])!=0){
                errln("FAIL: ULocale.getCountry("+locale+")=="+
                        buff+",\t expected "+tests[i][4]);
            }else{
                logln("   ULocale.getCountry("+locale+")=="+buff);
            }
        }

        if (ULocale.getLanguage("iw_IL").compareTo( ULocale.getLanguage("he_IL"))==0) {
            errln("he,iw ULocale.getLanguage mismatch");
        }

        String buff = ULocale.getLanguage("kok_IN");
        if(buff.compareTo("kok")!=0){
            errln("ULocale.getLanguage(\"kok\") failed. Expected: kok Got: "+buff);   
        }
    }
    private static final String[][]testCases = new String[][]{
        { "ca_ES_PREEURO-with-extra-stuff-that really doesn't make any sense-unless-you're trying to increase code coverage",
            "ca_ES_PREEURO_WITH_EXTRA_STUFF_THAT REALLY DOESN'T MAKE ANY SENSE_UNLESS_YOU'RE TRYING TO INCREASE CODE COVERAGE"},
        { "ca_ES_PREEURO", "ca_ES@currency=ESP" },
        { "de_AT_PREEURO", "de_AT@currency=ATS" },
        { "de_DE_PREEURO", "de_DE@currency=DEM" },
        { "de_LU_PREEURO", "de_LU@currency=EUR" },
        { "el_GR_PREEURO", "el_GR@currency=GRD" },
        { "en_BE_PREEURO", "en_BE@currency=BEF" },
        { "en_IE_PREEURO", "en_IE@currency=IEP" },
        { "es_ES_PREEURO", "es_ES@currency=ESP" },
        { "eu_ES_PREEURO", "eu_ES@currency=ESP" },
        { "fi_FI_PREEURO", "fi_FI@currency=FIM" },
        { "fr_BE_PREEURO", "fr_BE@currency=BEF" },
        { "fr_FR_PREEURO", "fr_FR@currency=FRF" },
        { "fr_LU_PREEURO", "fr_LU@currency=LUF" },
        { "ga_IE_PREEURO", "ga_IE@currency=IEP" },
        { "gl_ES_PREEURO", "gl_ES@currency=ESP" },
        { "it_IT_PREEURO", "it_IT@currency=ITL" },
        { "nl_BE_PREEURO", "nl_BE@currency=BEF" },
        { "nl_NL_PREEURO", "nl_NL@currency=NLG" },
        { "pt_PT_PREEURO", "pt_PT@currency=PTE" },
        { "de__PHONEBOOK", "de@collation=phonebook" },
        { "en_GB_EURO",    "en_GB@currency=EUR" },
        { "en_GB@EURO",    "en_GB@currency=EUR" }, //POSIX ID
        { "es__TRADITIONAL", "es@collation=traditional" },
        { "hi__DIRECT", "hi@collation=direct" },
        { "ja_JP_TRADITIONAL", "ja_JP@calendar=japanese" },
        { "th_TH_TRADITIONAL", "th_TH@calendar=buddhist" },
        { "zh_TW_STROKE", "zh_TW@collation=stroke" },
        { "zh__PINYIN", "zh@collation=pinyin" },
        { "zh@collation=pinyin", "zh@collation=pinyin" },
        { "zh_CN@collation=pinyin", "zh_CN@collation=pinyin" },
        { "zh_CN_CA@collation=pinyin", "zh_CN_CA@collation=pinyin" },
        { "en_US_POSIX", "en_US_POSIX" }, 
        { "hy_AM_REVISED", "hy_AM_REVISED" }, 
        { "no_NO_NY",   "no_NO_NY" },
        { "no@ny",      "no__NY" }, //POSIX ID
        { "no-no.utf32@B", "no_NO_B" }, //POSIX ID
        { "qz-qz@Euro", "qz_QZ@currency=EUR" }, /* qz-qz uses private use iso codes */
        { "en-BOONT",   "en__BOONT" }, /* registered name */
        { "de-1901",    "de__1901" }, /* registered name */
        { "de-1906",    "de__1906" }, /* registered name */
        { "sr-SP-Cyrl",     "sr_Cyrl_SP" }, /* .NET name */
        { "sr-SP-Latn",     "sr_Latn_SP" }, /* .NET name */
        { "uz-UZ-Cyrl",     "uz_Cyrl_UZ" }, /* .NET name */
        { "uz-UZ-Latn",     "uz_Latn_UZ" }, /* .NET name */
        { "zh-CHS",         "zh_Hans" }, /* .NET name */
        { "zh-CHT",         "zh_TW" }, /* .NET name This may change back to zh_Hant */
    };
    public void TestCanonicalization(){      
        for(int i = 0; i< testCases.length;i++){
            String canonical = ULocale.canonicalize(testCases[i][0]);
            if(!canonical.equals(testCases[i][1])){
                errln("ULocale.canonicalize did not return the expected ID for: "+ testCases[i][0]+
                        " Expected: "+ testCases[i][1]+
                        " Got: "+ canonical);   
            }
        }
           
    }
    public void TestGetAvailable(){
        ULocale[] locales = ULocale.getAvailableLocales();
        if(locales.length<10){
            errln("Did not get the correct result from getAvailableLocales");
        }
        if(!locales[locales.length-1].equals("zh_TW")){
            errln("Did not get the expected result");   
        }
    }
}
