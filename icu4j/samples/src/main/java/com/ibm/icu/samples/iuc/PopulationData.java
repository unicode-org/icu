// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

/**
 * @author srl
 *
 */
public class PopulationData {
    /**
     * Entry in the population list
     */
    public static class TerritoryEntry implements Comparable<TerritoryEntry> {
        private String territoryName;
        private double population;
        public TerritoryEntry(String displayCountry, double population) {
            this.territoryName = displayCountry;
            this.population = population;
        }
        public String territoryName() {
            return territoryName;
        }
        public double population() {
            return population;
        }
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(TerritoryEntry o) {
            int rc = 0;
            if (rc==0) rc = territoryName.compareTo(o.territoryName());
            if (rc==0) rc = ((Double)population).compareTo(o.population());
            return rc;
        }
    };
    public static Set<TerritoryEntry> getTerritoryEntries(Locale loc, Set<TerritoryEntry> entries) {
        // Note: format of supplementalData is NOT STATIC and may change. It is internal to ICU!
        UResourceBundle suppData = SupplementalUtilities.getICUSupplementalData();
        UResourceBundle territoryInfo = suppData.get("territoryInfo");
//        int nTerr = territoryInfo.getSize();
        for(UResourceBundleIterator iter =  territoryInfo.getIterator();iter.hasNext();) {
            UResourceBundle rawEntry= iter.next();
            UResourceBundle territoryF = rawEntry.get("territoryF");
            int vec[] = territoryF.getIntVector();
            
            // now we have the entry
            // territoryF = { gdp, literacy, population }
            String terrID = rawEntry.getKey();
            ULocale territory = new ULocale("und", terrID);
            entries.add(new TerritoryEntry(territory.getDisplayCountry(ULocale.forLocale(loc)), SupplementalUtilities.ldml2d(vec[2])));
        }
        return entries;
  }
    
  public static void main(String... args) {
      NumberFormat nf = NumberFormat.getInstance();
      System.out.println("Loading population/territory data from CLDR");
      Set<TerritoryEntry> territoryEntries = getTerritoryEntries(Locale.getDefault(), new HashSet<TerritoryEntry>());
      System.out.println(".. count="+ nf.format(territoryEntries.size()));
      for(TerritoryEntry te : territoryEntries) {
          System.out.println(" "+ te.territoryName() + " = " + nf.format(te.population()));
      }
  }
}
