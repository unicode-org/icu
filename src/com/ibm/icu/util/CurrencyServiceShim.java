/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/CurrencyServiceShim.java,v $
 * $Date: 2003/06/03 18:49:35 $
 * $Revision: 1.4 $
 *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.Locale;
import com.ibm.icu.impl.ICULocaleData;

import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.ICULocaleService;

/**
 * This is a package-access implementation of registration for
 * currency.  The shim is instantiated by reflection in Currency, all
 * dependencies on ICUService are located in this file. This structure
 * is to allow ICU4J to be built without service registration support.  
 */
final class CurrencyServiceShim extends Currency.ServiceShim {
    
    Locale[] getAvailableLocales() {
        if (service.isDefault()) {
            return ICULocaleData.getAvailableLocales();
        }
        return service.getAvailableLocales();
    }

    Currency createInstance(Locale loc) {
        if (service.isDefault()) {
            return Currency.createCurrency(loc);
        }
        return (Currency)service.get(loc);
    }

    Object registerInstance(Currency currency, Locale locale) {
        return service.registerObject(currency, locale);
    }
    
    boolean unregister(Object registryKey) {
        return service.unregisterFactory((Factory)registryKey);
    }

    private static class CFService extends ICULocaleService {
        CFService() {
            super("Currency");

            class CurrencyFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(Locale loc, int kind, ICUService service) {
                    return Currency.createCurrency(loc);
                }
            }
            
            registerFactory(new CurrencyFactory());
            markDefault();
        }
    }
    static final ICULocaleService service = new CFService();
}
