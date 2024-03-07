// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.samples.iuc.PopulationData.TerritoryEntry;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * @author srl
 *
 */
public class Sample50_PopSort {
    public static void main(String... args) {
        // setup
        // setup
        Locale defaultLocaleID = Locale.getDefault();
        LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(ULocale.forLocale(defaultLocaleID),
                DialectHandling.DIALECT_NAMES);
        String defaultLocaleName = ldn.localeDisplayName(defaultLocaleID);

        Set<PopulationData.TerritoryEntry> territoryList;
        territoryList = PopulationData.getTerritoryEntries(defaultLocaleID,
                    new HashSet<TerritoryEntry>());
        int territoryCount = territoryList.size();

        // sort it        
        final Collator collator = Collator.getInstance(defaultLocaleID);
        territoryList = PopulationData.getTerritoryEntries(defaultLocaleID,
                    new TreeSet<TerritoryEntry>(new Comparator<TerritoryEntry>(){
                        public int compare(TerritoryEntry o1, TerritoryEntry o2) {
                            return collator.compare(o1.territoryName(), o2.territoryName());
                        }}));
        UResourceBundle resourceBundle = 
                UResourceBundle.getBundleInstance(
                        Sample40_PopMsg.class.getPackage().getName().replace('.', '/')+"/data/popmsg",
                        defaultLocaleID,
                        Sample40_PopMsg.class.getClassLoader());
        
        // say hello
        String pattern = resourceBundle.getString("welcome");
        MessageFormat fmt = new MessageFormat(pattern,defaultLocaleID);
        Map<String, Object> msgargs = new HashMap<String, Object>();
        msgargs.put("territoryCount", territoryCount);
        msgargs.put("myLanguage", defaultLocaleName);
        msgargs.put("today", System.currentTimeMillis());
        System.out.println(fmt.format(msgargs, new StringBuffer(), null));
        
        // Population roll call
        String info = resourceBundle.getString("info");
        Map<String, Object> infoArgs = new HashMap<String, Object>();
        for(PopulationData.TerritoryEntry entry : territoryList) { 
            infoArgs.put("territory", entry.territoryName());
            infoArgs.put("population", entry.population());
            System.out.println(MessageFormat.format(info, infoArgs));
        }
    }
}
