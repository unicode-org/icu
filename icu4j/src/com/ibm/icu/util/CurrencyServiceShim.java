/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/CurrencyServiceShim.java,v $
 * $Date: 2003/04/21 21:02:42 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.Locale;
import com.ibm.icu.impl.ICULocaleData;

import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Key;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.ICUResourceBundleFactory;

/**
 * This is a package-access implementation of registration for
 * currency.  The shim is instantiated by reflection in Currency, all
 * dependencies on ICUService are located in this file. This structure
 * is to allow ICU4J to be built without service registration support.  
 */
final class CurrencyServiceShim extends Currency.ServiceShim {
    
    Locale[] getAvailableLocales() {
        if (service == null) {
            return ICULocaleData.getAvailableLocales();
        } else {
            return service.getAvailableLocales();
        }
    }

    Currency createInstance(Locale loc) {
        if (service == null) {
            return Currency.createCurrency(loc);
        } else {
            return (Currency)service.get(loc);
        }
    }

    Object registerInstance(Currency currency, Locale locale) {
        return getService().registerObject(currency, locale);
    }
    
    boolean unregister(Object registryKey) {
        if (service == null) {
            return false;
        }
        return service.unregisterFactory((Factory)registryKey);
    }

    private static ICULocaleService service;
    private static ICULocaleService getService() {
        if (service == null) {
            ICULocaleService newService = new ICULocaleService("Currency");

            class CurrencyFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(Locale loc, int kind, ICUService service) {
                    return Currency.createCurrency(loc);
                }
            }
            
            newService.registerFactory(new CurrencyFactory());
            synchronized (CurrencyServiceShim.class) {
                if (service == null) {
                    service = newService;
                }
            }
        }
        return service;
    }
}
