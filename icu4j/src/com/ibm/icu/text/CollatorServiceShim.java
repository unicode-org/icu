/**
*******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/CollatorServiceShim.java,v $
* $Date: 2003/04/19 00:01:53 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu.text;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.ICUResourceBundleFactory;
import com.ibm.icu.impl.ICULocaleService.LocaleKeyFactory;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.Collator.CollatorFactory;

final class CollatorServiceShim extends Collator.ServiceShim {

    Collator getInstance(Locale locale) {
        if (service == null) {
            return new RuleBasedCollator(locale);
        }

        try {
            return (Collator)((Collator)service.get(locale)).clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    Object registerInstance(Collator collator, Locale locale) {
        return getService().registerObject(collator, locale);
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

        return getService().registerFactory(new CFactory(f));
    }

    boolean unregister(Object registryKey) {
        if (service == null) {
            return false;
        }
        return service.unregisterFactory((Factory)registryKey);
    }

    Locale[] getAvailableLocales() {
        if (service == null) {
            return ICULocaleData.getAvailableLocales();
        }
        return service.getAvailableLocales();
    }

    Map getDisplayNames(Locale locale) {
        Collator col = Collator.getInstance(locale);
        return getService().getDisplayNames(locale, col, null);
    }

    String getDisplayName(Locale objectLocale, Locale displayLocale) {
        String id = LocaleUtility.canonicalLocaleString(objectLocale);
        return getService().getDisplayName(id, displayLocale);
    }

    private static ICULocaleService service;
    private static ICULocaleService getService() {
        if (service == null) {
            ICULocaleService newService = new ICULocaleService("Collator") {
                    protected Object handleDefault(Key key, String[] actualIDReturn) {
                        if (actualIDReturn != null) {
                            actualIDReturn[0] = "root";
                        }
                        return new RuleBasedCollator(new Locale("", "", ""));
                    }
                };

            class CollatorFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(Locale loc, int kind, ICUService service) {
                    return new RuleBasedCollator(loc);
                }

                protected Set getSupportedIDs() {
                    return ICULocaleData.getAvailableLocaleNameSet();
                }
            }
            newService.registerFactory(new CollatorFactory());

            synchronized (Collator.class) {
                if (service == null) {
                    service = newService;
                }
            }
        }
        return service;
    }
}
