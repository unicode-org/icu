/*
**********************************************************************
* Copyright (c) 2004-2007, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: January 14 2004
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberFormat.*;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;
import java.lang.reflect.*;
import java.util.Locale;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.TreeMap;

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
                public Object create(ULocale req) {
                    return Calendar.getInstance(req);
                }
                // }, null, new Registrar() {
                //     public Object register(ULocale loc, Object prototype) {
                //         CFactory f = new CFactory(loc, (Calendar) prototype);
                //         return Calendar.register(f, loc);
                //     }
                //     public boolean unregister(Object key) {
                //         return Calendar.unregister(key);
                //     }
            });
    }

    // Currency getLocale API is obsolete in 3.2.  Since it now returns ULocale.ROOT,
    // and this is not equal to the requested locale zh_TW_TAIPEI, the
    // checkService call would always fail.  So we now omit the test.
    /*
    public void TestCurrency() {
        checkService("zh_TW_TAIPEI", new ServiceFacade() {
                public Object create(ULocale req) {
                    return Currency.getInstance(req);
                }
            }, null, new Registrar() {
                    public Object register(ULocale loc, Object prototype) {
                        return Currency.registerInstance((Currency) prototype, loc);
                    }
                    public boolean unregister(Object key) {
                        return Currency.unregister(key);
                    }
                });
    }
    */

    public void TestBreakIterator() {
        checkService("ja_JP_OSAKA", new ServiceFacade() {
                public Object create(ULocale req) {
                    return BreakIterator.getWordInstance(req);
                }
            }, null, new Registrar() {
                    public Object register(ULocale loc, Object prototype) {
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
                public Object create(ULocale req) {
                    return Collator.getInstance(req);
                }
            }, null, new Registrar() {
                    public Object register(ULocale loc, Object prototype) {
                        return Collator.registerInstance((Collator) prototype, loc);
                    }
                    public boolean unregister(Object key) {
                        return Collator.unregister(key);
                    }
                });
    }

    public void TestDateFormat() {
        checkService("de_CH_ZURICH", new ServiceFacade() {
                public Object create(ULocale req) {
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
            ULocale locale;
            public NFactory(ULocale loc, NumberFormat fmt) {
                super(loc);
                this.locale = loc;
                this.proto = fmt;
            }
            public NumberFormat createFormat(ULocale loc, int formatType) {
                return (NumberFormat) (locale.equals(loc) ?
                                       proto.clone() : null);
            }
        }

        checkService("fr_FR_NICE", new ServiceFacade() {
                public Object create(ULocale req) {
                    return NumberFormat.getInstance(req);
                }
            }, new Subobject() {
                    public Object get(Object parent) {
                        return ((DecimalFormat) parent).getDecimalFormatSymbols();
                    }
                }, new Registrar() {
                        public Object register(ULocale loc, Object prototype) {
                            NFactory f = new NFactory(loc, (NumberFormat) prototype);
                            return NumberFormat.registerFactory(f);
                        }
                        public boolean unregister(Object key) {
                            return NumberFormat.unregister(key);
                        }
                    });
    }

    public void TestSetULocaleKeywords() {
        ULocale uloc = new ULocale("en_Latn_US");
        uloc = uloc.setKeywordValue("Foo", "FooValue");
        if (!"en_Latn_US@foo=FooValue".equals(uloc.getName())) {
            errln("failed to add foo keyword, got: " + uloc.getName());
        }
        uloc = uloc.setKeywordValue("Bar", "BarValue");
        if (!"en_Latn_US@bar=BarValue;foo=FooValue".equals(uloc.getName())) {
            errln("failed to add bar keyword, got: " + uloc.getName());
        }
        uloc = uloc.setKeywordValue("BAR", "NewBarValue");
        if (!"en_Latn_US@bar=NewBarValue;foo=FooValue".equals(uloc.getName())) {
            errln("failed to change bar keyword, got: " + uloc.getName());
        }
        uloc = uloc.setKeywordValue("BaR", null);
        if (!"en_Latn_US@foo=FooValue".equals(uloc.getName())) {
            errln("failed to delete bar keyword, got: " + uloc.getName());
        }
        uloc = uloc.setKeywordValue(null, null);
        if (!"en_Latn_US".equals(uloc.getName())) {
            errln("failed to delete all keywords, got: " + uloc.getName());
        }
    }

    /*
     * ticket#5060
     */
    public void TestJavaLocaleCompatibility() {
        Locale backupDefault = Locale.getDefault();
        
        // Java Locale for ja_JP with Japanese calendar
        Locale jaJPJP = new Locale("ja", "JP", "JP");
        Locale jaJP = new Locale("ja", "JP");
 
        Calendar cal = Calendar.getInstance(jaJPJP);
        String caltype = cal.getType();
        if (!caltype.equals("japanese")) {
            errln("FAIL: Invalid calendar type: " + caltype + " /expected: japanese");
        }

        cal = Calendar.getInstance(jaJP);
        caltype = cal.getType();
        if (!caltype.equals("gregorian")) {
            errln("FAIL: Invalid calendar type: " + caltype + " /expected: gregorian");
        }

        // Default locale
        Locale.setDefault(jaJPJP);
        ULocale defUloc = ULocale.getDefault();
        if (!defUloc.toString().equals("ja_JP@calendar=japanese")) {
            errln("FAIL: Invalid default ULocale: " + defUloc + " /expected: ja_JP@calendar=japanese");
        }
        // Check calendar type
        cal = Calendar.getInstance();
        caltype = cal.getType();
        if (!caltype.equals("japanese")) {
            errln("FAIL: Invalid calendar type: " + caltype + " /expected: japanese");
        }
        Locale.setDefault(backupDefault);

        // Set default via ULocale
        ULocale.setDefault(new ULocale("ja_JP@calendar=japanese"));
        if (!Locale.getDefault().equals(jaJPJP)) {
            errln("FAIL: ULocale#setDefault failed to set Java Locale ja_JP_JP /actual: " + Locale.getDefault());
        }
        Locale.setDefault(backupDefault);

        // We also want to map ICU locale ja@calendar=japanese to Java ja_JP_JP
        ULocale.setDefault(new ULocale("ja@calendar=japanese"));
        if (!Locale.getDefault().equals(jaJPJP)) {
            errln("FAIL: ULocale#setDefault failed to set Java Locale ja_JP_JP /actual: " + Locale.getDefault());
        }
        Locale.setDefault(backupDefault);

        // Java no_NO_NY
        Locale noNONY = new Locale("no", "NO", "NY");
        Locale.setDefault(noNONY);
        defUloc = ULocale.getDefault();
        if (defUloc.toString().equals("nn_NY")) {
            errln("FAIL: Invalid default ULocale: " + defUloc + " /expected: nn_NY");
        }
        Locale.setDefault(backupDefault);

        // Set default via ULocale
        ULocale.setDefault(new ULocale("nn_NO"));
        if (!Locale.getDefault().equals(noNONY)) {
            errln("FAIL: ULocale#setDefault failed to set Java Locale no_NO_NY /actual: " + Locale.getDefault());
        }
        Locale.setDefault(backupDefault);        

        // We also want to map ICU locale nn to Java no_NO_NY
        ULocale.setDefault(new ULocale("nn"));
        if (!Locale.getDefault().equals(noNONY)) {
            errln("FAIL: ULocale#setDefault failed to set Java Locale no_NO_NY /actual: " + Locale.getDefault());
        }
        Locale.setDefault(backupDefault);
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
    boolean reqOK = (expReqValid.equals("gt") && reqValid > 0) ||
        (expReqValid.equals("ge") && reqValid >= 0) ||
        (expReqValid.equals("eq") && reqValid == 0);
    boolean valOK = (expValidActual.equals("gt") && validActual > 0) ||
        (expValidActual.equals("ge") && validActual >= 0) ||
        (expValidActual.equals("eq") && validActual == 0);
        if (reqOK && valOK) {
            logln("Ok: " + label + "; req=" + req + ", valid=" + valid +
                  ", actual=" + actual);
        } else {
            errln("FAIL: " + label + "; req=" + req + ", valid=" + valid +
                  ", actual=" + actual +
          (reqOK ? "" : "\n  req !" + expReqValid + " valid") +
          (valOK ? "" : "\n  val !" + expValidActual + " actual"));
        }
    }

    /**
     * Interface used by checkService defining a protocol to create an
     * object, given a requested locale.
     */
    interface ServiceFacade {
        Object create(ULocale requestedLocale);
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
        Object register(ULocale loc, Object prototype);
        boolean unregister(Object key);
    }

    /**
     * Use reflection to call getLocale() on the given object to
     * determine both the valid and the actual locale.  Verify these
     * for correctness.
     */
    void checkObject(String requestedLocale, Object obj,
                     String expReqValid, String expValidActual) {
        Class[] getLocaleParams = new Class[] { ULocale.Type.class };
        try {
            Class cls = obj.getClass();
            Method getLocale = cls.getMethod("getLocale", getLocaleParams);
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
        // no longer an error, Currency has no getLocale
            // errln("FAIL: reflection failed: " + e1);
        } catch(SecurityException e2) {
            errln("FAIL: reflection failed: " + e2);
        } catch(IllegalAccessException e3) {
            errln("FAIL: reflection failed: " + e3);
        } catch(IllegalArgumentException e4) {
            errln("FAIL: reflection failed: " + e4);
        } catch(InvocationTargetException e5) {
        // no longer an error, Currency has no getLocale
            // errln("FAIL: reflection failed: " + e5);
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
        ULocale req = new ULocale(requestedLocale);
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
//    private static final int ENGLISH = 0;
//    private static final int FRENCH = 1;
//    private static final int CATALAN = 2;
//    private static final int GREEK = 3;
//    private static final int NORWEGIAN = 4;
    private static final int LANG = 0;
    private static final int SCRIPT = 1;
    private static final int CTRY = 2;
    private static final int VAR = 3;
    private static final int NAME = 4;
//    private static final int LANG3 = 5;
//    private static final int CTRY3 = 6;
//    private static final int LCID = 7;
//    private static final int DLANG_EN = 8;
//    private static final int DSCRIPT_EN = 9;
//    private static final int DCTRY_EN = 10;
//    private static final int DVAR_EN = 11;
//    private static final int DNAME_EN = 12;
//    private static final int DLANG_FR = 13;
//    private static final int DSCRIPT_FR = 14;
//    private static final int DCTRY_FR = 15;
//    private static final int DVAR_FR = 16;
//    private static final int DNAME_FR = 17;
//    private static final int DLANG_CA = 18;
//    private static final int DSCRIPT_CA = 19;
//    private static final int DCTRY_CA = 20;
//    private static final int DVAR_CA = 21;
//    private static final int DNAME_CA = 22;
//    private static final int DLANG_EL = 23;
//    private static final int DSCRIPT_EL = 24;
//    private static final int DCTRY_EL = 25;
//    private static final int DVAR_EL = 26;
//    private static final int DNAME_EL = 27;

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

    public void TestPrefixes() {
        // POSIX ids are no longer handled by getName, so POSIX failures are ignored
        final String [][] testData = new String[][]{
            /* null canonicalize() column means "expect same as getName()" */
            {"sv", "", "FI", "AL", "sv-fi-al", "sv_FI_AL", null},
            {"en", "", "GB", "", "en-gb", "en_GB", null},
            {"i-hakka", "", "MT", "XEMXIJA", "i-hakka_MT_XEMXIJA", "i-hakka_MT_XEMXIJA", null},
            {"i-hakka", "", "CN", "", "i-hakka_CN", "i-hakka_CN", null},
            {"i-hakka", "", "MX", "", "I-hakka_MX", "i-hakka_MX", null},
            {"x-klingon", "", "US", "SANJOSE", "X-KLINGON_us_SANJOSE", "x-klingon_US_SANJOSE", null},

            {"mr", "", "", "", "mr.utf8", "mr.utf8", "mr"},
            {"de", "", "TV", "", "de-tv.koi8r", "de_TV.koi8r", "de_TV"},
            {"x-piglatin", "", "ML", "", "x-piglatin_ML.MBE", "x-piglatin_ML.MBE", "x-piglatin_ML"},  /* Multibyte English */
            {"i-cherokee", "","US", "", "i-Cherokee_US.utf7", "i-cherokee_US.utf7", "i-cherokee_US"},
            {"x-filfli", "", "MT", "FILFLA", "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA"},
            {"no", "", "NO", "NY_B", "no-no-ny.utf32@B", "no_NO_NY.utf32@B", "no_NO_NY_B"},
            {"no", "", "NO", "B",  "no-no.utf32@B", "no_NO.utf32@B", "no_NO_B"},
            {"no", "", "",   "NY", "no__ny", "no__NY", null},
            {"no", "", "",   "NY", "no@ny", "no@ny", "no__NY"},
            {"el", "Latn", "", "", "el-latn", "el_Latn", null},
            {"en", "Cyrl", "RU", "", "en-cyrl-ru", "en_Cyrl_RU", null},
            {"zh", "Hant", "TW", "STROKE", "zh-hant_TW_STROKE", "zh_Hant_TW_STROKE", null},
            {"qq", "Qqqq", "QQ", "QQ", "qq_Qqqq_QQ_QQ", "qq_Qqqq_QQ_QQ", null},
            {"qq", "Qqqq", "", "QQ", "qq_Qqqq__QQ", "qq_Qqqq__QQ", null},
            {"12", "3456", "78", "90", "12_3456_78_90", "12_3456_78_90", null}, /* total garbage */

            // odd cases
            {"", "", "", "", "@FOO=bar", "@foo=bar", null},
            {"", "", "", "", "_@FOO=bar", "@foo=bar", null},
            {"", "", "", "", "__@FOO=bar", "@foo=bar", null},
            {"", "", "", "FOO", "__foo@FOO=bar", "__FOO@foo=bar", null}, // we have some of these prefixes
        };

        String loc, buf,buf1;
        final String [] testTitles = {
            "ULocale.getLanguage()",
            "ULocale.getScript()",
            "ULocale.getCountry()",
            "ULocale.getVariant()",
            "name",
            "ULocale.getName()",
            "canonicalize()",
        };
        ULocale uloc;

        for(int row=0;row<testData.length;row++) {
            loc = testData[row][NAME];
            logln("Test #"+row+": "+loc);

            uloc = new ULocale(loc);

            for(int n=0;n<=(NAME+2);n++) {
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

                case NAME+2:
                    buf = ULocale.canonicalize(loc);
                    buf1 = ULocale.createCanonical(loc).getName();
                    break;

                default:
                    buf = "**??";
                    buf1 = buf;
                }

                logln("#"+row+": "+testTitles[n]+" on "+loc+": -> ["+buf+"]");

                String expected = testData[row][n];
                if (expected == null && n == (NAME+2)) {
                    expected = testData[row][NAME+1];
                }

                // ignore POSIX failures in getName, we don't spec behavior in this case
                if (n == NAME+1 &&
                    (expected.indexOf('.') != -1 ||
                     expected.indexOf('@') != -1)) {
                    continue;
                }

                if(buf.compareTo(expected)!=0) {
                    errln("#"+row+": "+testTitles[n]+" on "+loc+": -> ["+buf+"] (expected '"+expected+"'!)");
                }
                if(buf1.compareTo(expected)!=0) {
                    errln("#"+row+": "+testTitles[n]+" on ULocale object "+loc+": -> ["+buf1+"] (expected '"+expected+"'!)");
                }
            }
        }
    }

    public void TestObsoleteNames(){
        final String[][] tests = new String[][]{
            /* locale, language3, language2, Country3, country2 */
            { "eng_USA", "eng", "en", "USA", "US" },
            { "kok",  "kok", "kok", "", "" },
            { "in",  "ind", "in", "", "" },
            { "id",  "ind", "id", "", "" }, /* NO aliasing */
            { "sh",  "srp", "sh", "", "" },
            { "zz_CS",  "", "zz", "SCG", "CS" },
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
            { "zz_YUG",  "", "zz", "YUG", "YU" },
            { "mlt_PSE", "mlt", "mt", "PSE", "PS" },
            { "iw", "heb", "iw", "", "" },
            { "ji", "yid", "ji", "", "" },
            { "jw", "jaw", "jw", "", "" },
            { "sh", "srp", "sh", "", "" },
            { "", "", "", "", "" }
        };

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
    public void TestCanonicalization(){
        final String[][]testCases = new String[][]{
            { "ca_ES_PREEURO", "ca_ES_PREEURO", "ca_ES@currency=ESP" },
            { "de_AT_PREEURO", "de_AT_PREEURO", "de_AT@currency=ATS" },
            { "de_DE_PREEURO", "de_DE_PREEURO", "de_DE@currency=DEM" },
            { "de_LU_PREEURO", "de_LU_PREEURO", "de_LU@currency=EUR" },
            { "el_GR_PREEURO", "el_GR_PREEURO", "el_GR@currency=GRD" },
            { "en_BE_PREEURO", "en_BE_PREEURO", "en_BE@currency=BEF" },
            { "en_IE_PREEURO", "en_IE_PREEURO", "en_IE@currency=IEP" },
            { "es_ES_PREEURO", "es_ES_PREEURO", "es_ES@currency=ESP" },
            { "eu_ES_PREEURO", "eu_ES_PREEURO", "eu_ES@currency=ESP" },
            { "fi_FI_PREEURO", "fi_FI_PREEURO", "fi_FI@currency=FIM" },
            { "fr_BE_PREEURO", "fr_BE_PREEURO", "fr_BE@currency=BEF" },
            { "fr_FR_PREEURO", "fr_FR_PREEURO", "fr_FR@currency=FRF" },
            { "fr_LU_PREEURO", "fr_LU_PREEURO", "fr_LU@currency=LUF" },
            { "ga_IE_PREEURO", "ga_IE_PREEURO", "ga_IE@currency=IEP" },
            { "gl_ES_PREEURO", "gl_ES_PREEURO", "gl_ES@currency=ESP" },
            { "it_IT_PREEURO", "it_IT_PREEURO", "it_IT@currency=ITL" },
            { "nl_BE_PREEURO", "nl_BE_PREEURO", "nl_BE@currency=BEF" },
            { "nl_NL_PREEURO", "nl_NL_PREEURO", "nl_NL@currency=NLG" },
            { "pt_PT_PREEURO", "pt_PT_PREEURO", "pt_PT@currency=PTE" },
            { "de__PHONEBOOK", "de__PHONEBOOK", "de@collation=phonebook" },
            { "en_GB_EURO", "en_GB_EURO", "en_GB@currency=EUR" },
            { "en_GB@EURO", null, "en_GB@currency=EUR" }, /* POSIX ID */
            { "es__TRADITIONAL", "es__TRADITIONAL", "es@collation=traditional" },
            { "hi__DIRECT", "hi__DIRECT", "hi@collation=direct" },
            { "ja_JP_TRADITIONAL", "ja_JP_TRADITIONAL", "ja_JP@calendar=japanese" },
            { "th_TH_TRADITIONAL", "th_TH_TRADITIONAL", "th_TH@calendar=buddhist" },
            { "zh_TW_STROKE", "zh_TW_STROKE", "zh_TW@collation=stroke" },
            { "zh__PINYIN", "zh__PINYIN", "zh@collation=pinyin" },
            { "zh@collation=pinyin", "zh@collation=pinyin", "zh@collation=pinyin" },
            { "zh_CN@collation=pinyin", "zh_CN@collation=pinyin", "zh_CN@collation=pinyin" },
            { "zh_CN_CA@collation=pinyin", "zh_CN_CA@collation=pinyin", "zh_CN_CA@collation=pinyin" },
            { "en_US_POSIX", "en_US_POSIX", "en_US_POSIX" },
            { "hy_AM_REVISED", "hy_AM_REVISED", "hy_AM_REVISED" },
            { "no_NO_NY", "no_NO_NY", "no_NO_NY" /* not: "nn_NO" [alan ICU3.0] */ },
            { "no@ny", null, "no__NY" /* not: "nn" [alan ICU3.0] */ }, /* POSIX ID */
            { "no-no.utf32@B", null, "no_NO_B" /* not: "nb_NO_B" [alan ICU3.0] */ }, /* POSIX ID */
            { "qz-qz@Euro", null, "qz_QZ@currency=EUR" }, /* qz-qz uses private use iso codes */
            { "en-BOONT", "en_BOONT", "en__BOONT" }, /* registered name */
            { "de-1901", "de_1901", "de__1901" }, /* registered name */
            { "de-1906", "de_1906", "de__1906" }, /* registered name */
            { "sr-SP-Cyrl", "sr_SP_CYRL", "sr_Cyrl_CS" }, /* .NET name */
            { "sr-SP-Latn", "sr_SP_LATN", "sr_Latn_CS" }, /* .NET name */
            { "sr_YU_CYRILLIC", "sr_YU_CYRILLIC", "sr_Cyrl_CS" }, /* Linux name */
            { "uz-UZ-Cyrl", "uz_UZ_CYRL", "uz_Cyrl_UZ" }, /* .NET name */
            { "uz-UZ-Latn", "uz_UZ_LATN", "uz_Latn_UZ" }, /* .NET name */
            { "zh-CHS", "zh_CHS", "zh_Hans" }, /* .NET name */
            { "zh-CHT", "zh_CHT", "zh_Hant" }, /* .NET name This may change back to zh_Hant */

            /* posix behavior that used to be performed by getName */
            { "mr.utf8", null, "mr" },
            { "de-tv.koi8r", null, "de_TV" },
            { "x-piglatin_ML.MBE", null, "x-piglatin_ML" },
            { "i-cherokee_US.utf7", null, "i-cherokee_US" },
            { "x-filfli_MT_FILFLA.gb-18030", null, "x-filfli_MT_FILFLA" },
            { "no-no-ny.utf8@B", null, "no_NO_NY_B" /* not: "nn_NO" [alan ICU3.0] */ }, /* @ ignored unless variant is empty */

            /* fleshing out canonicalization */
            /* sort keywords, ';' is separator so not present at end in canonical form */
            { "en_Hant_IL_VALLEY_GIRL@currency=EUR;calendar=Japanese;", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR" },
            /* already-canonical ids are not changed */
            { "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR" },
            /* PRE_EURO and EURO conversions don't affect other keywords */
            /* not in spec
               { "es_ES_PREEURO@CALendar=Japanese", "es_ES_PREEURO@calendar=Japanese", "es_ES@calendar=Japanese;currency=ESP" },
               { "es_ES_EURO@SHOUT=zipeedeedoodah", "es_ES_EURO@shout=zipeedeedoodah", "es_ES@currency=EUR;shout=zipeedeedoodah" },
            */
            /* currency keyword overrides PRE_EURO and EURO currency */
            /* not in spec
               { "es_ES_PREEURO@currency=EUR", "es_ES_PREEURO@currency=EUR", "es_ES@currency=EUR" },
               { "es_ES_EURO@currency=ESP", "es_ES_EURO@currency=ESP", "es_ES@currency=ESP" },
            */
            /* norwegian is just too weird, if we handle things in their full generality */
            /* this is a negative test to show that we DO NOT handle 'lang=no,var=NY' specially. */
            { "no-Hant-GB_NY@currency=$$$", "no_Hant_GB_NY@currency=$$$", "no_Hant_GB_NY@currency=$$$" /* not: "nn_Hant_GB@currency=$$$" [alan ICU3.0] */ },

            /* test cases reflecting internal resource bundle usage */
            /* root is just a language */
            { "root@kw=foo", "root@kw=foo", "root@kw=foo" },
            /* level 2 canonicalization should not touch basename when there are keywords and it is null */
            { "@calendar=gregorian", "@calendar=gregorian", "@calendar=gregorian" },
        };

        for(int i = 0; i< testCases.length;i++){
            String[] testCase = testCases[i];
            String source = testCase[0];
            String level1Expected = testCase[1];
            String level2Expected = testCase[2];

            if (level1Expected != null) { // null means we have no expectations for how this case is handled
                String level1 = ULocale.getName(source);
                if (!level1.equals(level1Expected)) {
                    errln("ULocale.getName error for: '" + source +
                          "' expected: '" + level1Expected + "' but got: '" + level1 + "'");
                } else {
                    logln("Ulocale.getName for: '" + source + "' returned: '" + level1 + "'");
                }
            } else {
                logln("ULocale.getName skipped: '" + source + "'");
            }

            if (level2Expected != null) {
                String level2 = ULocale.canonicalize(source);
                if(!level2.equals(level2Expected)){
                    errln("ULocale.getName error for: '" + source +
                          "' expected: '" + level2Expected + "' but got: '" + level2 + "'");
                } else {
                    logln("Ulocale.canonicalize for: '" + source + "' returned: '" + level2 + "'");
                }
            } else {
                logln("ULocale.canonicalize skipped: '" + source + "'");
            }
        }
    }

    public void TestGetAvailable(){
        ULocale[] locales = ULocale.getAvailableLocales();
        if(locales.length<10){
            errln("Did not get the correct result from getAvailableLocales");
        }
        if(!locales[locales.length-1].equals("zh_Hant_TW")){
            errln("Did not get the expected result");
        }
    }

    public void TestDisplayNames() {
        // consistency check, also check that all data is available
        {
            ULocale[] locales = ULocale.getAvailableLocales();
            for (int i = 0; i < locales.length; ++i) {
                ULocale l = locales[i];
                String name = l.getDisplayName();

                logln(l + " --> " + name +
                      ", " + l.getDisplayName(ULocale.GERMAN) +
                      ", " + l.getDisplayName(ULocale.FRANCE));

                String language = l.getDisplayLanguage();
                String script = l.getDisplayScript();
                String country = l.getDisplayCountry();
                String variant = l.getDisplayVariant();

                checkName(name, language, script, country, variant);

                for (int j = 0; j < locales.length; ++j) {
                    ULocale dl = locales[j];

                    name = l.getDisplayName(dl);
                    language = l.getDisplayLanguage(dl);
                    script = l.getDisplayScript(dl);
                    country = l.getDisplayCountry(dl);
                    variant = l.getDisplayVariant(dl);

                    if (!checkName(name, language, script, country, variant)) {
                        break;
                    }
                }
            }
        }
        // spot check
        {
            ULocale[] locales = {
                ULocale.US, ULocale.GERMANY, ULocale.FRANCE
            };
            String[] names = {
                "Chinese (China)", "Chinesisch (China)", "chinois (Chine)"
            };
            ULocale locale = new ULocale("zh_CN");
            for (int i = 0; i < locales.length; ++i) {
                String name = locale.getDisplayName(locales[i]);
                if (!names[i].equals(name)) {
                    errln("expected '" + names[i] + "' but got '" + name + "'");
                }
            }
        }
    }

    private boolean checkName(String name, String language, String script, String country, String variant) {
        if (language.length() > 0 && name.indexOf(language) == -1) {
            errln("name '" + name + "' does not contain language '" + language + "'");
            return false;
        }
        if (script.length() > 0 && name.indexOf(script) == -1) {
            errln("name '" + name + "' does not contain script '" + script + "'");
            return false;
        }
        if (country.length() > 0 && name.indexOf(country) == -1) {
            errln("name '" + name + "' does not contain country '" + country + "'");
            return false;
        }
        if (variant.length() > 0 && name.indexOf(variant) == -1) {
            errln("name '" + name + "' does not contain variant '" + variant + "'");
            return false;
        }
        return true;
    }

    public void TestCoverage() {
        {
            //Cover displayXXX
            int i, j;
            String localeID="zh_CN";
            String name, language, script, country, variant;
            logln("Covering APIs with signature displayXXX(String, String)");
            for (i = 0; i < LOCALE_SIZE; i++) {
                //localeID String
                String testLocale=(rawData2[NAME][i]);

                logln("Testing "+ testLocale+".....");
                name = ULocale.getDisplayName(localeID, testLocale);
                language = ULocale.getDisplayLanguage(localeID, testLocale);
                script = ULocale.getDisplayScript(localeID, testLocale);
                country = ULocale.getDisplayCountry(localeID, testLocale);
                variant = ULocale.getDisplayVariant(localeID, testLocale);

                if (!checkName(name, language, script, country, variant)) {
                    break;
                }
            }

            logln("Covering APIs with signature displayXXX(String, ULocale)\n");
            for (j = 0; j < LOCALE_SIZE; j++) {
                String testLocale=(rawData2[NAME][j]);
                ULocale loc = new ULocale(testLocale);

                logln("Testing "+ testLocale+".....");
                name = ULocale.getDisplayName(localeID, loc);
                language = ULocale.getDisplayLanguage(localeID, loc);
                script = ULocale.getDisplayScript(localeID, loc);
                country = ULocale.getDisplayCountry(localeID, loc);
                variant = ULocale.getDisplayVariant(localeID, loc);

                if (!checkName(name, language, script, country, variant)) {
                    break;
                }
            }
        }
        ULocale loc1 = new ULocale("en_US_BROOKLYN");
        ULocale loc2 = new ULocale("en","US","BROOKLYN");
        if (!loc2.equals(loc1)){
            errln("ULocale.ULocale(String a, String b, String c)");
        }
        
        ULocale loc3 = new ULocale("en_US");
        ULocale loc4 = new ULocale("en","US");
        if (!loc4.equals(loc3)){
            errln("ULocale.ULocale(String a, String b)");
        }
        
        ULocale loc5 = (ULocale) loc4.clone();
        if (!loc5.equals(loc4)){
            errln("ULocale.clone should get the same ULocale");
        }
        ULocale.getISOCountries();	// To check the result ?!
    }

    public void TestBamBm() {
        // "bam" shouldn't be there since the official code is 'bm'
        String[] isoLanguages = ULocale.getISOLanguages();
        for (int i = 0; i < isoLanguages.length; ++i) {
            if ("bam".equals(isoLanguages[i])) {
                errln("found bam");
            }
            if (i > 0 && isoLanguages[i].compareTo(isoLanguages[i-1]) <= 0) {
                errln("language list out of order: '" + isoLanguages[i] + " <= " + isoLanguages[i-1]);
            }
        }
    }

    public void TestDisplayKeyword() {
        //prepare testing data
        initHashtable();
        String[] data = {"en_US@collation=direct;calendar=islamic-civil",
             "zh_Hans@collation=pinyin;calendar=chinese",
             "foo_Bar_BAZ@collation=traditional;calendar=buddhist"};

        for (int i = 0; i < data.length; i++) {
            String localeID = data[i];
            logln("");
            logln("Testing locale " + localeID + " ...");
            ULocale loc = new ULocale(localeID);

            Iterator it = loc.getKeywords();
            Iterator it2 = ULocale.getKeywords(localeID);
            //it and it2 are not equal here. No way to verify their equivalence yet.
            while(it.hasNext()) {
                String key = (String)it.next();
                String key2 = (String)it2.next();
                if (!key.equals(key2)) {
                    errln("FAIL: static and non-static getKeywords returned different results.");
                }

                //To verify display of Keyword
                // display the above key in English
                String s0 = ULocale.getDisplayKeyword(key); //display in default locale
                String s1 = ULocale.getDisplayKeyword(key, ULocale.US);
                String s2 = ULocale.getDisplayKeyword(key, "en_US");
                if (!s1.equals(s2)) {
                    errln ("FAIL: one of the getDisplayKeyword methods failed.");
                }
                if (ULocale.getDefault().equals(ULocale.US) && !s1.equals(s0)) {
                    errln ("FAIL: getDisplayKeyword methods failed for the default locale.");
                }
                if (!s1.equals(h[0].get(key))) {
                    warnln("Locale " + localeID + " getDisplayKeyword for key: " + key +
                          " in English expected \"" + h[0].get(key) + "\" saw \"" + s1 + "\" instead");
                } else {
                    logln("OK: getDisplayKeyword for key: " + key + " in English got " + s1);
                }

                // display the key in S-Chinese
                s1 = ULocale.getDisplayKeyword(key, ULocale.CHINA);
                s2 = ULocale.getDisplayKeyword(key, "zh_Hans");
                if (!s1.equals(s2)) {
                    errln ("one of the getDisplayKeyword methods failed.");
                }
                if (!s1.equals(h[1].get(key))) {
                    warnln("Locale " + localeID + " getDisplayKeyword for key: " + key +
                          " in Chinese expected \"" + h[1].get(key) + "\" saw \"" + s1 + "\" instead");
                } else {
                    logln("OK: getDisplayKeyword for key: " + key + " in Chinese got " + s1);
                }

                //To verify display of Keyword values
                String type = loc.getKeywordValue(key);
                // display type in English
                String ss0 = loc.getDisplayKeywordValue(key);
                String ss1 = loc.getDisplayKeywordValue(key, ULocale.US);
                String ss2 = ULocale.getDisplayKeywordValue(localeID, key, "en_US");
                String ss3 = ULocale.getDisplayKeywordValue(localeID, key, ULocale.US);
                if (!ss1.equals(ss2) || !ss1.equals(ss3)) {
                    errln ("FAIL: one of the getDisplayKeywordValue methods failed.");
                }
                if (ULocale.getDefault().equals(ULocale.US) && !ss1.equals(ss0)) {
                    errln ("FAIL: getDisplayKeyword methods failed for the default locale.");
                }
                if (!ss1.equals(h[0].get(type))) {
                    warnln(" Locale " + localeID + " getDisplayKeywordValue for key: " + key +
                          " in English expected \"" + h[0].get(type) + "\" saw \"" + ss1 + "\" instead");
                } else {
                    logln("OK: getDisplayKeywordValue for key: " + key + " in English got " + ss1);
                }

                // display type in Chinese
                ss0 = loc.getDisplayKeywordValue(key);
                ss1 = loc.getDisplayKeywordValue(key, ULocale.CHINA);
                ss2 = ULocale.getDisplayKeywordValue(localeID, key, "zh_Hans");
                ss3 = ULocale.getDisplayKeywordValue(localeID, key, ULocale.CHINA);
                if (!ss1.equals(ss2) || !ss1.equals(ss3)) {
                    warnln ("one of the getDisplayKeywordValue methods failed.");
                }
                if (!ss1.equals(h[1].get(type))) {
                    warnln("Locale " + localeID + " getDisplayKeywordValue for key: " + key +
                          " in Chinese expected \"" + h[1].get(type) + "\" saw \"" + ss1 + "\" instead");
                } else {
                    logln("OK: getDisplayKeywordValue for key: " + key + " in Chinese got " + ss1);
                }
            }
        }
    }
    private void initHashtable() {
        h[0] = new Hashtable();
        h[1] = new Hashtable();

        //display in English
        h[0].put("collation", "collation");
        h[0].put("calendar", "calendar");
        h[0].put("currency", "Currency");
        h[0].put("phonebook", "Phonebook Order");
        h[0].put("pinyin", "Pinyin Sort Order");
        h[0].put("traditional", "Traditional Sort Order");
        h[0].put("stroke", "Stroke Order");
        h[0].put("direct", "Direct Sort Order");
        h[0].put("japanese", "Japanese Calendar");
        h[0].put("buddhist", "Buddhist Calendar");
        h[0].put("islamic", "Islamic Calendar");
        h[0].put("islamic-civil", "Islamic-Civil Calendar" );
        h[0].put("hebrew", "Hebrew Calendar");
        h[0].put("chinese", "Chinese Calendar");
        h[0].put("gregorian", "Gregorian Calendar" );

        //display in S-Chinese
        h[1].put("collation", "\u5BF9\u7167");
        h[1].put("calendar", "\u65E5\u5386");
        h[1].put("currency", "\u8D27\u5E01");
        h[1].put("direct", "\u987A\u5E8F");
        h[1].put("phonebook", "\u7535\u8BDD\u7C3F\u987A\u5E8F");
        h[1].put("pinyin", "\u62FC\u97F3\u987a\u5e8f");
        h[1].put("stroke", "\u7B14\u5212\u987A\u5E8F");
        h[1].put("traditional", "\u4F20\u7EDF\u5386\u6CD5");
        h[1].put("japanese", "\u65E5\u672C\u65E5\u5386");
        h[1].put("buddhist", "\u4F5B\u6559\u65E5\u5386");
        h[1].put("islamic", "\u4F0A\u65AF\u5170\u65E5\u5386");
        h[1].put("islamic-civil", "\u4F0A\u65AF\u5170\u5E0C\u5409\u6765\u5386");
        h[1].put("hebrew", "\u5E0C\u4F2F\u6765\u65E5\u5386");
        h[1].put("chinese", "\u519C\u5386");
        h[1].put("gregorian", "\u516C\u5386");
    }

    //Hashtables for storing expected display of keys/types of locale in English and Chinese
    private static Hashtable[] h = new Hashtable[2];
    
    private static final String ACCEPT_LANGUAGE_TESTS[][]  =  {
    /*#      result  fallback? */
    /*0*/ { "mt_MT", "false" },
    /*1*/ { "en", "false" },
    /*2*/ { "en", "true" }, // fell back from en-zzz to en
    /*3*/ {  null, "true" },
    /*4*/ {  "es", "false" }, 
    /*5*/ { "de", "false" }};
    
    private static final String ACCEPT_LANGUAGE_HTTP[] = { 
                    /*0*/ "mt-mt, ja;q=0.76, en-us;q=0.95, en;q=0.92, en-gb;q=0.89, fr;q=0.87, iu-ca;q=0.84, iu;q=0.82, ja-jp;q=0.79, mt;q=0.97, de-de;q=0.74, de;q=0.71, es;q=0.68, it-it;q=0.66, it;q=0.63, vi-vn;q=0.61, vi;q=0.58, nl-nl;q=0.55, nl;q=0.53, th-th-traditional;q=.01",
                    /*1*/ "ja;q=0.5, en;q=0.8, tlh",
                    /*2*/ "en-zzz, de-lx;q=0.8",
                    /*3*/ "mga-ie;q=0.9, tlh",
                    /*4*/ "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                          "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, xxx-yyy;q=.01, "+
                               "es",
                        /*5*/ "de;q=.9, fr;q=.9, xxx-yyy, sr;q=.8"};
    
    
    public void TestAcceptLanguage() {
        for(int i = 0 ; i < (ACCEPT_LANGUAGE_HTTP.length); i++) {
            Boolean expectBoolean = new Boolean(ACCEPT_LANGUAGE_TESTS[i][1]);
            String expectLocale=ACCEPT_LANGUAGE_TESTS[i][0];
            
           logln("#" + i + ": expecting: " + expectLocale + " (" + expectBoolean + ")");
            
            boolean r[] = { false };
            ULocale n = ULocale.acceptLanguage(ACCEPT_LANGUAGE_HTTP[i], r);
            if((n==null)&&(expectLocale!=null)) {
                errln("result was null! line #" + i);
                continue;
            }
            if(((n==null)&&(expectLocale==null)) || (n.toString().equals(expectLocale))) {
                logln(" locale: OK." );
            } else {
                errln("expected " + expectLocale + " but got " + n.toString());
            }
            if(expectBoolean.equals(new Boolean(r[0]))) {
                logln(" bool: OK.");
            } else {
                errln("bool: not OK, was " + new Boolean(r[0]).toString() + " expected " + expectBoolean.toString());
            }
        }
    }
    
    private ULocale[] StringToULocaleArray(String acceptLanguageList){
        //following code is copied from 
        //ULocale.acceptLanguage(String acceptLanguageList, ULocale[] availableLocales, boolean[] fallback)
        class ULocaleAcceptLanguageQ implements Comparable {
            private double q;
            private double serial;
            public ULocaleAcceptLanguageQ(double theq, int theserial) {
                q = theq;
                serial = theserial;
            }
            public int compareTo(Object o) {
                ULocaleAcceptLanguageQ other = (ULocaleAcceptLanguageQ) o;
                if(q > other.q) { // reverse - to sort in descending order
                    return -1;
                } else if(q < other.q) {
                    return 1;
                }
                if(serial < other.serial) {
                    return -1;
                } else if(serial > other.serial) {
                    return 1;
                } else {
                    return 0; // same object
                }
            }
        }

        // 1st: parse out the acceptLanguageList into an array
        
        TreeMap map = new TreeMap();
        
        final int l = acceptLanguageList.length();
        int n;
        for(n=0;n<l;n++) {
            int itemEnd = acceptLanguageList.indexOf(',',n);
            if(itemEnd == -1) {
                itemEnd = l;
            }
            int paramEnd = acceptLanguageList.indexOf(';',n);
            double q = 1.0;
 
            if((paramEnd != -1) && (paramEnd < itemEnd)) {
                /* semicolon (;) is closer than end (,) */
                int t = paramEnd + 1;
                while(Character.isSpace(acceptLanguageList.charAt(t))) {
                    t++;
                }
                if(acceptLanguageList.charAt(t)=='q') {
                    t++;
                }
                while(Character.isSpace(acceptLanguageList.charAt(t))) {
                    t++;
                }
                if(acceptLanguageList.charAt(t)=='=') {
                    t++;
                }
                while(Character.isSpace(acceptLanguageList.charAt(t))) {
                    t++;
                }
                try {
                    String val = acceptLanguageList.substring(t,itemEnd).trim();
                    q = Double.parseDouble(val);
                } catch (NumberFormatException nfe) {
                    q = 1.0;
                }
            } else {
                q = 1.0; //default
                paramEnd = itemEnd;
            }

            String loc = acceptLanguageList.substring(n,paramEnd).trim();
            int serial = map.size();
            ULocaleAcceptLanguageQ entry = new ULocaleAcceptLanguageQ(q,serial);
            map.put(entry, new ULocale(ULocale.canonicalize(loc))); // sort in reverse order..   1.0, 0.9, 0.8 .. etc
            n = itemEnd; // get next item. (n++ will skip over delimiter)
        }
        
        // 2. pull out the map 
        ULocale acceptList[] = (ULocale[])map.values().toArray(new ULocale[map.size()]);
        return acceptList;
    }
    
    public void TestAcceptLanguage2() {
        for(int i = 0 ; i < (ACCEPT_LANGUAGE_HTTP.length); i++) {
            Boolean expectBoolean = new Boolean(ACCEPT_LANGUAGE_TESTS[i][1]);
            String expectLocale=ACCEPT_LANGUAGE_TESTS[i][0];
            
           logln("#" + i + ": expecting: " + expectLocale + " (" + expectBoolean + ")");
            
            boolean r[] = { false };
            ULocale n = ULocale.acceptLanguage(StringToULocaleArray(ACCEPT_LANGUAGE_HTTP[i]), r);
            if((n==null)&&(expectLocale!=null)) {
                errln("result was null! line #" + i);
                continue;
            }
            if(((n==null)&&(expectLocale==null)) || (n.toString().equals(expectLocale))) {
                logln(" locale: OK." );
            } else {
                errln("expected " + expectLocale + " but got " + n.toString());
            }
            if(expectBoolean.equals(new Boolean(r[0]))) {
                logln(" bool: OK.");
            } else {
                errln("bool: not OK, was " + new Boolean(r[0]).toString() + " expected " + expectBoolean.toString());
            }
        }
    }
    public void TestJB3962(){
        ULocale loc = new ULocale("de_CH");
        String disp = loc.getDisplayName(ULocale.GERMAN);
        if(!disp.equals("Deutsch (Schweiz)")){
            errln("Did not get the expected display name for de_CH locale. Got: "+ prettify(disp));
        }
    }

    public void TestAddLikelySubtags() {
        String[][] data = {
            {"en", "en_Latn_US"},
            {"en_US_BOSTON", "en_Latn_US_BOSTON"},
            {"th@calendar=buddhist", "th_Thai_TH@calendar=buddhist"},
            {"ar_ZZ", "ar_Arab_EG"},
            {"cch", "cch_Latn_NG"},
            {"zh", "zh_Hans_CN"},
            {"zh_TW", "zh_Hant_TW"},
            {"zh_HK", "zh_Hant_HK"},
            {"zh_Hant", "zh_Hant_TW"},
            {"zh_Zzzz_CN", "zh_Hans_CN"},
            {"und_US", "en_Latn_US"},
            {"und_HK", "zh_Hant_HK"},
            /* Not yet implemented
            {"art_lojban", "arg_lojban"},
            {"zh_cmn_Hans", "zh_cmn_Hans"},
            */
        };
        for (int i = 0; i < data.length; i++) {
            ULocale org = new ULocale(data[i][0]);
            ULocale res = ULocale.addLikelySubtag(org);
            if (!res.toString().equals(data[i][1])) {
                errln("Original: " + data[i][0] + " Expected: " + data[i][1] + " - but got " + res.toString());
            }
        }
    }
}
