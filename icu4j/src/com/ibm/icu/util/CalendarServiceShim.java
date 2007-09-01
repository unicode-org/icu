/*
*   Copyright (C) 2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICULocaleService.LocaleKey;
import com.ibm.icu.impl.ICULocaleService.LocaleKeyFactory;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.ICUService.Key;
import com.ibm.icu.util.Calendar.CalendarFactory;

class CalendarServiceShim extends Calendar.CalendarShim {

    Locale[] getAvailableLocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableLocales(ICUResourceBundle.ICU_BASE_NAME);
        }
        return service.getAvailableLocales();
    }

    ULocale[] getAvailableULocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableULocales(ICUResourceBundle.ICU_BASE_NAME);
        }
        return service.getAvailableULocales();
    }

    private static final class CalFactory extends LocaleKeyFactory {
        private CalendarFactory delegate;
        CalFactory(CalendarFactory delegate) {
            super(delegate.visible() ? VISIBLE : INVISIBLE);
            this.delegate = delegate;
        }

        public Object create(Key key, ICUService service) {
            if (handlesKey(key)) {
                LocaleKey lkey = (LocaleKey)key;
                ULocale loc = lkey.canonicalLocale();
                Object result = delegate.createCalendar(loc);
                if (result == null) {
                    result = service.getKey(key, null, this);
                }
                return result;
            }
            return null;
        }

        protected Set getSupportedIDs() {
            return delegate.getSupportedLocaleNames();
        }
    }

    Calendar createInstance(ULocale desiredLocale) {
        ULocale[] actualLoc = new ULocale[1];
        if (desiredLocale.equals(ULocale.ROOT)) {
            desiredLocale = ULocale.ROOT;
        }
        Calendar cal = (Calendar)service.get(desiredLocale, actualLoc);
        if (cal == null) {
            throw new MissingResourceException("Unable to construct Calendar", "", "");
        }
        cal = (Calendar)cal.clone();

        /* !!! TODO !!! actualLoc returned by service is not properly set.
         * When this Calendar object is being created, cal.setLocale is called
         * and proper actual locale is set at that time.  Revisit this later.
         * -yoshito
         */
        /*
        ULocale uloc = actualLoc[0];
        cal.setLocale(uloc, uloc); // service make no distinction between actual and valid
        */
        return cal;
    }

    Object registerFactory(CalendarFactory factory) {
        return service.registerFactory(new CalFactory(factory));
    }

    boolean unregister(Object k) {
        return service.unregisterFactory((Factory)k);
    }

    private static class CalService extends ICULocaleService {
        CalService() {
            super("Calendar");
            class RBCalendarFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(ULocale loc, int kind, ICUService sercice) {
                    return Calendar.createInstance(loc);
                }
            }
            this.registerFactory(new RBCalendarFactory());
            markDefault();
        }
    }
    
    private static ICULocaleService service = new CalService();
}
