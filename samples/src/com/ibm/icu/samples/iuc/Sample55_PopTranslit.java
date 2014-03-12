/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.samples.iuc.PopulationData.TerritoryEntry;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * @author srl
 *
 */
public class Sample55_PopTranslit {
    public static void main(String... args) {
        // setup
        final ULocale fromLocale = new ULocale(args[0]);
        final ULocale locale = ULocale.getDefault();
        Set<PopulationData.TerritoryEntry> territoryList;
        final Collator collator = Collator.getInstance(locale);
        territoryList = PopulationData.getTerritoryEntries(fromLocale,
                    new TreeSet<TerritoryEntry>(new Comparator<TerritoryEntry>(){
                        public int compare(TerritoryEntry o1, TerritoryEntry o2) {
                            return collator.compare(o1.territoryName(), o2.territoryName());
                        }}));
        UResourceBundle resourceBundle = 
                UResourceBundle.getBundleInstance(
                        Sample40_PopMsg.class.getPackage().getName().replace('.', '/')+"/data/popmsg",
                        locale,
                        Sample40_PopMsg.class.getClassLoader());
        
        // say hello
        String welcome = resourceBundle.getString("welcome");
        Map<String, Object> welcomeArgs = new HashMap<String, Object>();
        welcomeArgs.put("territoryCount", territoryList.size());
        System.out.println( MessageFormat.format(welcome, welcomeArgs) );
        
        // Population roll call
        String info = resourceBundle.getString("info");
        Map<String, Object> infoArgs = new HashMap<String, Object>();

        Transliterator translit = Transliterator.getInstance("Any-Latin");
        
        for(PopulationData.TerritoryEntry entry : territoryList) { 
            infoArgs.put("territory", translit.transform(entry.territoryName()));
            infoArgs.put("population", entry.population());
            System.out.println(MessageFormat.format(info, infoArgs));
        }
    }
}
