/*
 *******************************************************************************
 * Copyright (C) 2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/NumberFormatServiceShim.java,v $ 
 * $Date: 2003/02/25 23:39:44 $ 
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.util.Locale;
import java.util.Set;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.ICUService.Key;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.ICUResourceBundleFactory;
import com.ibm.icu.impl.ICULocaleService.LocaleKey;
import com.ibm.icu.impl.ICULocaleService.LocaleKeyFactory;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberFormat.NumberFormatFactory;

class NumberFormatServiceShim extends NumberFormat.NumberFormatShim {
    
    Locale[] getAvailableLocales() {
        if (service == null) {
            return ICULocaleData.getAvailableLocales();
        } else {
            return service.getAvailableLocales();
        }
    }

    private static final class NFFactory extends LocaleKeyFactory {
        private NumberFormatFactory delegate;

        NFFactory(NumberFormatFactory delegate) {
            super(delegate.visible() ? VISIBLE : INVISIBLE);

            this.delegate = delegate;
        }

        public Object create(Key key, ICUService service) {
            if (handlesKey(key)) {
                LocaleKey lkey = (LocaleKey)key;
                Locale loc = lkey.canonicalLocale();
                int kind = lkey.kind();

                Object result = delegate.createFormat(loc, kind);
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

    Object registerFactory(NumberFormatFactory factory) {
        return getService().registerFactory(new NFFactory(factory));
    }

    boolean unregister(Object registryKey) {
        if (service == null) {
            return false;
        } else {
            return service.unregisterFactory((Factory)registryKey);
        }
    }

    NumberFormat createInstance(Locale desiredLocale, int choice) {
        if (service == null) {
            return NumberFormat.createInstance(desiredLocale, choice);
        }
        NumberFormat result = (NumberFormat)service.get(desiredLocale, choice);
        return (NumberFormat)result.clone();
    }

    private ICULocaleService service = null;
    private ICULocaleService getService() {
        if (service == null) {
            class RBNumberFormatFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(Locale loc, int kind, ICUService service) {
                    return NumberFormat.createInstance(loc, kind);
                }
            }
                
            ICULocaleService newService = new ICULocaleService("NumberFormat");
            newService.registerFactory(new RBNumberFormatFactory());
            
            synchronized (NumberFormatServiceShim.class) {
                if (service == null) {
                    service = newService;
                }
            }
        }
        return service;
    }
}
