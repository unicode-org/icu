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
        Locale req = getLocale(requestedLocale);
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

    /**
     * Construct a locale from its ID (this should be in Locale!).
     */
    static Locale getLocale(String ID) {
        String language=ID, country="", variant="";
        int i = ID.indexOf('_');
        if (i>=0) {
            language = ID.substring(0, i);
            int j = ID.indexOf('_', i+1);
            if (j<0) {
                country = ID.substring(i+1);
            } else {
                country = ID.substring(i+1, j);
                variant = ID.substring(j+1);
            }
        }
        return new Locale(language, country, variant);
    }
}
