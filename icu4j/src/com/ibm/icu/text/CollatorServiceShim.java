/**
*******************************************************************************
* Copyright (C) 2003-2004, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.text;

import java.util.Locale;
import java.util.Set;

import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICULocaleService.LocaleKeyFactory;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.Collator.CollatorFactory;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

final class CollatorServiceShim extends Collator.ServiceShim {

    Collator getInstance(ULocale locale) {
        if (service.isDefault()) {
            return new RuleBasedCollator(locale);
        }

        try {
            ULocale[] actualLoc = new ULocale[1];
            Collator coll = (Collator)service.get(locale, actualLoc);
            coll = (Collator) coll.clone();
            coll.setLocale(actualLoc[0], actualLoc[0]); // services make no distinction between actual & valid
            return coll;
        }
        catch (CloneNotSupportedException e) {
	    ///CLOVER:OFF
            throw new InternalError(e.getMessage());
	    ///CLOVER:ON
        }
    }

    Object registerInstance(Collator collator, Locale locale) {
        return service.registerObject(collator, locale);
    }

    Object registerFactory(CollatorFactory f) {
        class CFactory extends LocaleKeyFactory {
            CollatorFactory delegate;

            CFactory(CollatorFactory f) {
                super(f.visible() 
                      ? LocaleKeyFactory.VISIBLE 
                      : LocaleKeyFactory.INVISIBLE, 
                      "CFactory");
                this.delegate = f;
            }

            public Object handleCreate(Locale loc, int kind, ICUService service) {
                Object coll = delegate.createCollator(loc);
                return coll;
            }
                
            public String getDisplayName(String id, Locale displayLocale) {
                Locale objectLocale = LocaleUtility.getLocaleFromName(id);
                return delegate.getDisplayName(objectLocale, displayLocale);
            }

            public Set getSupportedIDs() {
                return delegate.getSupportedLocaleIDs();
            }
        }

        return service.registerFactory(new CFactory(f));
    }

    boolean unregister(Object registryKey) {
        return service.unregisterFactory((Factory)registryKey);
    }

    Locale[] getAvailableLocales() {
        // TODO rewrite this to just wrap getAvailableULocales later
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableLocales(UResourceBundle.ICU_COLLATION_BASE_NAME);
        }
        return service.getAvailableLocales();
    }

    ULocale[] getAvailableULocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableULocales(UResourceBundle.ICU_COLLATION_BASE_NAME);
        }
        return service.getAvailableULocales();
    }

    String getDisplayName(Locale objectLocale, Locale displayLocale) {
        String id = LocaleUtility.canonicalLocaleString(objectLocale);
        return service.getDisplayName(id, displayLocale);
    }

    private static class CService extends ICULocaleService {
        CService() {
            super("Collator");

            class CollatorFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(ULocale uloc, int kind, ICUService service) {
                    return new RuleBasedCollator(uloc);
                }

                protected boolean supportsULocale() {
                    return true;   
                }
                
                protected Set getSupportedIDs() {
                    return ICUResourceBundle.getAvailableLocaleNameSet();
                }
            }

            this.registerFactory(new CollatorFactory());
            markDefault();
        }

        protected Object handleDefault(Key key, String[] actualIDReturn) {
            if (actualIDReturn != null) {
                actualIDReturn[0] = "root";
            }
            return new RuleBasedCollator(new ULocale("", "", ""));
        }
    }
    private static ICULocaleService service = new CService();
}
