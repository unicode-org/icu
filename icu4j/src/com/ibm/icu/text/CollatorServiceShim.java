/**
*******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/CollatorServiceShim.java,v $
* $Date: 2004/01/12 22:50:16 $
* $Revision: 1.7 $
*
*******************************************************************************
*/

package com.ibm.icu.text;

import java.util.Locale;
import java.util.Set;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.LocaleKeyFactory;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.Collator.CollatorFactory;
import com.ibm.icu.util.ULocale;

final class CollatorServiceShim extends Collator.ServiceShim {

    Collator getInstance(Locale locale) {
        if (service.isDefault()) {
            return new RuleBasedCollator(locale);
        }

        try {
            Locale[] actualLoc = new Locale[1];
            Collator coll = (Collator)service.get(locale, actualLoc);
            ULocale uloc = new ULocale(actualLoc[0]);
            coll = (Collator) coll.clone();
            coll.setLocale(uloc, uloc); // services make no distinction between actual & valid
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
        if (service.isDefault()) {
            return ICULocaleData.getAvailableLocales();
        }
        return service.getAvailableLocales();
    }

    String getDisplayName(Locale objectLocale, Locale displayLocale) {
        String id = LocaleUtility.canonicalLocaleString(objectLocale);
        return service.getDisplayName(id, displayLocale);
    }

    private static class CService extends ICULocaleService {
        CService() {
            super("Collator");

            class CollatorFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(Locale loc, int kind, ICUService service) {
                    return new RuleBasedCollator(loc);
                }

                protected Set getSupportedIDs() {
                    return ICULocaleData.getAvailableLocaleNameSet();
                }
            }

            this.registerFactory(new CollatorFactory());
            markDefault();
        }

        protected Object handleDefault(Key key, String[] actualIDReturn) {
            if (actualIDReturn != null) {
                actualIDReturn[0] = "root";
            }
            return new RuleBasedCollator(new Locale("", "", ""));
        }
    }
    private static ICULocaleService service = new CService();
}
