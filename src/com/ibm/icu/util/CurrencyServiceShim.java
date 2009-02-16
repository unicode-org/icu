/**
 *******************************************************************************
 * Copyright (C) 2001-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.Locale;

import com.ibm.icu.impl.ICUResourceBundle;
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

    Currency createInstance(ULocale loc) {
        // TODO: convert to ULocale when service switches over

        if (service.isDefault()) {
            return Currency.createCurrency(loc);
        }
        ULocale[] actualLoc = new ULocale[1];
    Currency curr = (Currency)service.get(loc, actualLoc);
        ULocale uloc = actualLoc[0];
        curr.setLocale(uloc, uloc); // services make no distinction between actual & valid
        return curr;
    }

    Object registerInstance(Currency currency, ULocale locale) {
        return service.registerObject(currency, locale);
    }
    
    boolean unregister(Object registryKey) {
        return service.unregisterFactory((Factory)registryKey);
    }

    private static class CFService extends ICULocaleService {
        CFService() {
            super("Currency");

            class CurrencyFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return Currency.createCurrency(loc);
                }
            }
            
            registerFactory(new CurrencyFactory());
            markDefault();
        }
    }
    static final ICULocaleService service = new CFService();
}
