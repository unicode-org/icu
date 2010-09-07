/*
 *******************************************************************************
 * Copyright (C) 2003-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

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
import com.ibm.icu.text.NumberFormat.NumberFormatFactory;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

class NumberFormatServiceShim extends NumberFormat.NumberFormatShim {

    Locale[] getAvailableLocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableLocales();
        }
        return service.getAvailableLocales();
    }

    ULocale[] getAvailableULocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableULocales();
        }
        return service.getAvailableULocales();
    }

    private static final class NFFactory extends LocaleKeyFactory {
        private NumberFormatFactory delegate;

        NFFactory(NumberFormatFactory delegate) {
            super(delegate.visible() ? VISIBLE : INVISIBLE);

            this.delegate = delegate;
        }

        public Object create(Key key, ICUService srvc) {
            if (handlesKey(key)) {
                LocaleKey lkey = (LocaleKey)key;
                ULocale loc = lkey.canonicalLocale();
                int kind = lkey.kind();

                Object result = delegate.createFormat(loc, kind);
                if (result == null) {
                    result = srvc.getKey(key, null, this);
                }
                return result;
            }
            return null;
        }

        protected Set<String> getSupportedIDs() {
            return delegate.getSupportedLocaleNames();
        }
    }

    Object registerFactory(NumberFormatFactory factory) {
        return service.registerFactory(new NFFactory(factory));
    }

    boolean unregister(Object registryKey) {
        return service.unregisterFactory((Factory)registryKey);
    }

    NumberFormat createInstance(ULocale desiredLocale, int choice) {

    // use service cache
//          if (service.isDefault()) {
//              return NumberFormat.createInstance(desiredLocale, choice);
//          }

        ULocale[] actualLoc = new ULocale[1];
        NumberFormat fmt = (NumberFormat)service.get(desiredLocale, choice,
                                                     actualLoc);
        if (fmt == null) {
            throw new MissingResourceException("Unable to construct NumberFormat", "", "");
        }
        fmt = (NumberFormat)fmt.clone();

        // If we are creating a currency type formatter, then we may have to set the currency
        // explicitly, since the actualLoc may be different than the desiredLocale        
        if ( choice == NumberFormat.CURRENCYSTYLE ||
             choice == NumberFormat.ISOCURRENCYSTYLE || 
             choice == NumberFormat.PLURALCURRENCYSTYLE) {
            fmt.setCurrency(Currency.getInstance(desiredLocale));
        }

        ULocale uloc = actualLoc[0];
        fmt.setLocale(uloc, uloc); // services make no distinction between actual & valid
        return fmt;
    }

    private static class NFService extends ICULocaleService {
        NFService() {
            super("NumberFormat");

            class RBNumberFormatFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return NumberFormat.createInstance(loc, kind);
                }
            }
                
            this.registerFactory(new RBNumberFormatFactory());
            markDefault();
        }
    }
    private static ICULocaleService service = new NFService();
}
