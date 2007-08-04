/**
*******************************************************************************
* Copyright (C) 2003-2007, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.text;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICULocaleService.LocaleKeyFactory;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.text.Collator.CollatorFactory;
import com.ibm.icu.util.ULocale;

final class CollatorServiceShim extends Collator.ServiceShim {

    Collator getInstance(ULocale locale) {
    // use service cache, it's faster than instantiation
//          if (service.isDefault()) {
//              return new RuleBasedCollator(locale);
//          }

        try {
            ULocale[] actualLoc = new ULocale[1];
            Collator coll = (Collator)service.get(locale, actualLoc);
            if (coll == null) {
                throw new MissingResourceException("Could not locate Collator data", "", "");
            }
            coll = (Collator) coll.clone();
            coll.setLocale(actualLoc[0], actualLoc[0]); // services make no distinction between actual & valid
            return coll;
        }
        catch (CloneNotSupportedException e) {
        ///CLOVER:OFF
            throw new IllegalStateException(e.getMessage());
        ///CLOVER:ON
        }
    }

    Object registerInstance(Collator collator, ULocale locale) {
        return service.registerObject(collator, locale);
    }

    Object registerFactory(CollatorFactory f) {
        class CFactory extends LocaleKeyFactory {
            CollatorFactory delegate;

            CFactory(CollatorFactory f) {
                super(f.visible()); 
                this.delegate = f;
            }

            public Object handleCreate(ULocale loc, int kind, ICUService service) {
                Object coll = delegate.createCollator(loc);
                return coll;
            }
                
            public String getDisplayName(String id, ULocale displayLocale) {
                ULocale objectLocale = new ULocale(id);
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
            return ICUResourceBundle.getAvailableLocales(ICUResourceBundle.ICU_COLLATION_BASE_NAME);
        }
        return service.getAvailableLocales();
    }

    ULocale[] getAvailableULocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableULocales(ICUResourceBundle.ICU_COLLATION_BASE_NAME);
        }
        return service.getAvailableULocales();
    }

    String getDisplayName(ULocale objectLocale, ULocale displayLocale) {
        String id = objectLocale.getName();
        return service.getDisplayName(id, displayLocale);
    }

    private static class CService extends ICULocaleService {
        CService() {
            super("Collator");

            class CollatorFactory extends ICUResourceBundleFactory {
                CollatorFactory() {
                    super(ICUResourceBundle.ICU_COLLATION_BASE_NAME);
                }

                protected Object handleCreate(ULocale uloc, int kind, ICUService service) {
                    return new RuleBasedCollator(uloc);
                }
            }

            this.registerFactory(new CollatorFactory());
            markDefault();
        }

        protected Object handleDefault(Key key, String[] actualIDReturn) {
            if (actualIDReturn != null) {
                actualIDReturn[0] = "root";
            }
            try {
                return new RuleBasedCollator(ULocale.ROOT);
            }
            catch (MissingResourceException e) {
                return null;
            }
        }
    }
    private static ICULocaleService service = new CService();
}
