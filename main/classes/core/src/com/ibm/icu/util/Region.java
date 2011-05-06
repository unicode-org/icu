/*
 *******************************************************************************
 * Copyright (C) 2011, International Business Machines Corporation             *
 * All Rights Reserved.                                                        *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.ICUResourceBundle;

/**
 * <code>Region</code> is the class representing a Unicode Region Code, also known as a 
 * Unicode Region Subtag, which is defined based upon the BCP 47 standard. We often think of
 * "regions" as "countries" when defining the characteristics of a locale.  Region codes There are different
 * types of region codes that are important to distinguish.
 * 
 *  Macroregion - A code for a "macro geographical (continental) region, geographical sub-region, or 
 *  selected economic and other grouping" as defined in 
 *  UN M.49 (http://unstats.un.org/unsd/methods/m49/m49regin.htm). 
 *  These are typically 3-digit codes, but contain some 2-letter codes, such as the LDML code QO 
 *  added for Outlying Oceania.  Not all UNM.49 codes are defined in LDML, but most of them are.
 *  Macroregions are represented in ICU by one of three region types: WORLD ( region code 001 ),
 *  CONTINENTS ( regions contained directly by WORLD ), and SUBCONTINENTS ( things contained directly
 *  by a continent ).
 *
 *  TERRITORY - A Region that is not a Macroregion. These are typically codes for countries, but also
 *  include areas that are not separate countries, such as the code "AQ" for Antarctica or the code 
 *  "HK" for Hong Kong (SAR China). Overseas dependencies of countries may or may not have separate 
 *  codes. The codes are typically 2-letter codes aligned with the ISO 3166 standard, but BCP47 allows
 *  for the use of 3-digit codes in the future.
 *
 *  UNKNOWN - The code ZZ is defined by Unicode LDML for use to indicate that the Region is unknown,
 *  or that the value supplied as a region was invalid.
 *
 *  DEPRECATED - Region codes that have been defined in the past but are no longer in modern usage,
 *  usually due to a country splitting into multiple territories or changing its name.
 *  
 *  GROUPING - A widely understood grouping of territories that has a well defined membership such
 *  that a region code has been assigned for it.  Some of these are UNM.49 codes that do't fall into 
 *  the world/continent/sub-continent hierarchy, while others are just well known groupings that have
 *  their own region code. Region "EU" (European Union) is one such region code that is a grouping.
 *  Groupings will never be returned by the getContainingRegion() API, since a different type of region
 *  ( WORLD, CONTINENT, or SUBCONTINENT ) will always be the containing region instead.
 *  
 * @author       John Emmons
 * @draft ICU 4.8
 * @provisional This API might change or be removed in a future release. 
 */

public class Region implements Comparable<Region> {

    /**
     * RegionType is an enumeration defining the different types of regions.  Current possible
     * values are WORLD, CONTINENT, SUBCONTINENT, TERRITORY, GROUPING, DEPRECATED, and UNKNOWN.
     * 
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */

    public enum RegionType {
        /**
         * Type representing the unknown region.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release. 
         */
        UNKNOWN,

        /**
         * Type representing a territory.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release. 
         */
        TERRITORY,

        /**
         * Type representing the whole world.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release. 
         */
        WORLD,
        /**
         * Type representing a continent.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release. 
         */
        CONTINENT,
        /**
         * Type representing a sub-continent.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release. 
         */
        SUBCONTINENT,
        /**
         * Type representing a grouping of territories that is not to be used in
         * the normal WORLD/CONTINENT/SUBCONTINENT/TERRITORY containment tree.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release. 
         */
        GROUPING,
        /**
         * Type representing a region whose code has been deprecated, usually
         * due to a country splitting into multiple territories or changing its name.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release. 
         */
        DEPRECATED,
    }

    private String id;
    private int code;
    private RegionType type;
    
    private static boolean hasData = false;
    private static boolean hasContainmentData = false;
    
    private static Map<String,Integer> regionIndexMap = null;   // Map from ID to position in the table
    private static Map<Integer,Integer> numericIndexMap = null; // Map from numeric code to position in the table
    private static Map<String,String> territoryAliasMap = null; // Aliases
    private static Map<String,Integer> numericCodeMap = null;   // Map of all possible IDs to numeric codes
    private static Region[] regions = null;
    private static BitSet[] subRegionData = null;
    private static Integer[] containingRegionData = null;
    private static ArrayList<Set<Region>> availableRegions = null;
    public static final int UNDEFINED_NUMERIC_CODE = -1;
    
    private static final String UNKNOWN_REGION_ID = "ZZ";
    private static final String WORLD_ID = "001";
   
    /*
     * Private default constructor.  Use factory methods only.
     */
    private Region () {}
    
    /*
     * Initializes the region data from the ICU resource bundles.  The region data
     * contains the basic relationships such as which regions are known, what the numeric
     * codes are, and any known aliases.  It does not contain the territory containment data.
     * Territory containment data only gets loaded if someone calls an API that is actually
     * going to use that data.
     * 
     * If the region data has already loaded, then this method simply returns without doing
     * anything meaningful.
     * 
     */
    private static synchronized void initRegionData() {
        
        if ( hasData ) {
            return;
        }
        
        territoryAliasMap = new HashMap<String,String>();
        numericCodeMap = new HashMap<String,Integer>();
        regionIndexMap = new HashMap<String,Integer>();
        numericIndexMap = new HashMap<Integer,Integer>();
        availableRegions = new ArrayList<Set<Region>>(RegionType.values().length);
        
        for (int i = 0 ; i < RegionType.values().length ; i++) {
            availableRegions.add(null);
        }
        UResourceBundle regionCodes = null;
        UResourceBundle territoryAlias = null;
        UResourceBundle codeMappings = null;
        UResourceBundle worldContainment = null;
        UResourceBundle territoryContainment = null;
        UResourceBundle groupingContainment = null;
        UResourceBundle rb = UResourceBundle.getBundleInstance(
                                    ICUResourceBundle.ICU_BASE_NAME,
                                    "metadata",
                                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        regionCodes = rb.get("regionCodes");
        territoryAlias = rb.get("territoryAlias");

        UResourceBundle rb2 = UResourceBundle.getBundleInstance(
                    ICUResourceBundle.ICU_BASE_NAME,
                    "supplementalData",
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        codeMappings = rb2.get("codeMappings");

        // Right now only fetch as much territory containment as we need in order to determine
        // types.  Only fetch the rest if we have to.
        //
        territoryContainment = rb2.get("territoryContainment");
        worldContainment = territoryContainment.get("001");
        groupingContainment = territoryContainment.get("grouping");
        
        String[] continentsArr = worldContainment.getStringArray();
        List<String> continents = Arrays.asList(continentsArr);
        String[] groupingArr = groupingContainment.getStringArray();
        List<String> groupings = Arrays.asList(groupingArr);

        
        // First put alias mappings for iso3 and numeric code mappings
        for ( int i = 0 ; i < codeMappings.getSize(); i++ ) {
            UResourceBundle mapping = codeMappings.get(i);
            if ( mapping.getType() == UResourceBundle.ARRAY ) {
                String [] codeStrings = mapping.getStringArray();
                if ( !territoryAliasMap.containsKey(codeStrings[1])) {
                    territoryAliasMap.put(codeStrings[1],codeStrings[0]); // Put alias from the numeric to the iso2 code
                }
                territoryAliasMap.put(codeStrings[2],codeStrings[0]); // Put alias from the iso3 to the iso2 code.
                numericCodeMap.put(codeStrings[0], Integer.valueOf(codeStrings[1])); // Create the mapping from the iso2 code to its numeric value
            }
        }

        for ( int i = 0 ; i < territoryAlias.getSize(); i++ ) {
            UResourceBundle res = territoryAlias.get(i);
            String key = res.getKey();
            String value = res.getString();
            if ( !territoryAliasMap.containsKey(key)) {
                territoryAliasMap.put(key, value);
            }
        }

        
        regions = new Region[regionCodes.getSize()];
        for ( int i = 0 ; i < regions.length ; i++ ) {
            regions[i] = new Region();
            String id = regionCodes.getString(i);
            regions[i].id = id;
            regionIndexMap.put(id, Integer.valueOf(i));

            if ( id.matches("[0-9]{3}")) {
                regions[i].code = Integer.valueOf(id).intValue();
                numericIndexMap.put(regions[i].code, Integer.valueOf(i));
            } else if (numericCodeMap.containsKey(id)) {
                regions[i].code = numericCodeMap.get(id).intValue();
                if ( !numericIndexMap.containsKey(regions[i].code)) {
                    numericIndexMap.put(regions[i].code, Integer.valueOf(i));
                }
            } else {
                regions[i].code = UNDEFINED_NUMERIC_CODE;
            }

            if ( territoryAliasMap.containsKey(id)){
                regions[i].type = RegionType.DEPRECATED;
            } else if ( id.equals(WORLD_ID) ) {
                regions[i].type = RegionType.WORLD;
            } else if ( id.equals(UNKNOWN_REGION_ID) ) {
                regions[i].type = RegionType.UNKNOWN;
            } else if ( continents.contains(id) ) {
                regions[i].type = RegionType.CONTINENT;
            } else if ( groupings.contains(id) ) {
                regions[i].type = RegionType.GROUPING;
            } else if ( id.matches("[0-9]{3}|QO") ) {
                regions[i].type = RegionType.SUBCONTINENT;
            } else {
                regions[i].type = RegionType.TERRITORY;
            }                
        }
        
        hasData = true;
    }

    /*
     * Initializes the containment data from the ICU resource bundles.  The containment data
     * defines the relationships between different regions, such as which regions are contained
     * within other regions.
     * 
     * Territory containment data only gets loaded if someone calls an API that is actually
     * going to use that data.  Since you have to have the basic region data as well, this
     * method will attempt to load the basic region data if it hasn't been loaded already.
     * 
     * If the containment data has already loaded, then this method simply returns without doing
     * anything meaningful.
     * 
     */

    private static synchronized void initContainmentData() {
        if ( hasContainmentData ) {
            return;
        }
        
        initRegionData();
        subRegionData = new BitSet[regions.length];
        containingRegionData = new Integer[regions.length];
        for ( int i = 0 ; i < regions.length ; i++ ) {
            subRegionData[i] = new BitSet(regions.length);
            containingRegionData[i] = null;
        }
        UResourceBundle territoryContainment = null;

        UResourceBundle rb = UResourceBundle.getBundleInstance(
                    ICUResourceBundle.ICU_BASE_NAME,
                    "supplementalData",
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        territoryContainment = rb.get("territoryContainment");

        
        // Get territory containment info from the supplemental data.
        for ( int i = 0 ; i < territoryContainment.getSize(); i++ ) {
            UResourceBundle mapping = territoryContainment.get(i);
            String parent = mapping.getKey();
            Integer parentRegionIndex = regionIndexMap.get(parent);
            for ( int j = 0 ; j < mapping.getSize(); j++ ) {
                String child = mapping.getString(j);
                Integer childRegionIndex = regionIndexMap.get(child);
                if ( parentRegionIndex != null && childRegionIndex != null ) {                    
                    subRegionData[parentRegionIndex.intValue()].set(childRegionIndex.intValue()); // Set the containment bit for this pair
                    // Regions of type GROUPING can't be set as the parent, since another region
                    // such as a SUBCONTINENT, CONTINENT, or WORLD must always be the parent.
                    if ( !regions[parentRegionIndex].isOfType(RegionType.GROUPING)) {
                        containingRegionData[childRegionIndex] = parentRegionIndex; 
                    }
                }
            }
        }
        hasContainmentData = true;
    }
    
    
    /** Returns a Region using the given region ID.  The region ID can be either a 2-letter ISO code,
     * 3-letter ISO code,  UNM.49 numeric code, or other valid Unicode Region Code as defined by the CLDR.
     * @param id The id of the region to be retrieved.
     * @return The corresponding region.
     * @throws NullPointerException if the supplied id is null.
     * @throws IllegalArgumentException if the supplied ID cannot be canonicalized to a Region ID that is known by ICU.
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */
    
    public static Region get(String id) {
        if ( id == null ) {
            throw new NullPointerException();
        }
        String canonicalID = canonicalize(id);
        if (canonicalID.equals(UNKNOWN_REGION_ID) && !id.equals(UNKNOWN_REGION_ID)) {
            throw new IllegalArgumentException("Unknown region id: " + id);
        }
        
        return regions[regionIndexMap.get(canonicalID)];
    }
    
    
    /** Returns a Region using the given numeric code as defined by UNM.49
     * @param code The numeric code of the region to be retrieved.
     * @return The corresponding region.
     * @throws IllegalArgumentException if the supplied numeric code is not recognized.
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */

    public static Region get(int code) {
        Integer index = numericIndexMap.get(Integer.valueOf(code));
        if ( index != null ) {
            Region r = regions[index];
            // Since a deprecated region will have the same numeric code as its new region code
            // we get by id which will make sure we get the canonicalized one.
            return Region.get(r.id);
        } else {
            throw new IllegalArgumentException("Unknown region code: " + code);
        }
    }

    /** Returns the canonicalized (preferred) form of the Region code.  For territories, it will
     * convert the string to the 2-letter ISO 3166 code if at all possible, and will convert any
     * known aliases to their modern counterparts.
     * 
     * @param id The string representing the region code to be canonicalized.
     * @return The canonicalized (preferred) form of the region code.  If the supplied region
     *         code is not recognized, the unknown region ( code "ZZ" ) is returned.
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */

    public static String canonicalize(String id) {
        initRegionData();
        String result = territoryAliasMap.get(id);
        if ( result != null && regionIndexMap.containsKey(result)) {
            return result;
        }
        
        if ( regionIndexMap.containsKey(id)) {
            return id;
        }
        return UNKNOWN_REGION_ID;
    }
    /** Returns true if the supplied region code is already in its canonical ( preferred ) form.
     * 
     * @param id The string representing the region code to be checked.
     * @return TRUE if the supplied region code is canonical, FALSE otherwise.
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */
   
    public static boolean isCanonical(String id) {
        return ( canonicalize(id).equals(id));
    }
    
    
    /** Used to retrieve all available regions of a specific type.
     * 
     * @param type The type of regions to be returned ( TERRITORY, MACROREGION, etc. )
     * @return An unmodifiable set of all known regions that match the given type.
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */

    public static Set<Region> getAvailable(RegionType type) {
        initRegionData();
        if ( availableRegions.get(type.ordinal()) == null) {
            Set<Region> result = new TreeSet<Region>();
            for ( Region r : regions ) {
                if ( r.type == type ) {
                    result.add(r);
                }
            }
            availableRegions.set(type.ordinal(), Collections.unmodifiableSet(result));
        }
        return availableRegions.get(type.ordinal());
    }

    
    /** Used to determine the macroregion that geographically contains this region.
     * 
     * @return The region that geographically contains this region.  Returns NULL if this region is
     *  code "001" (World) or "ZZ" (Unknown region).  For example, calling this method with region "IT" (Italy)
     *  returns the region "039" (Southern Europe).    
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */

    public Region getContainingRegion() {
        initContainmentData();
        Integer index = regionIndexMap.get(id);
        assert(index!=null);
        if ( containingRegionData[index] == null ) {
            return null;
        } else {
            return regions[containingRegionData[index]];
        }
    }

    /** Used to determine the sub-regions that are contained within this region.
     * 
     * @return An unmodifiable set containing all the regions that are immediate children
     * of this region in the region hierarchy.  These returned regions could be either macro
     * regions, territories, or a mixture of the two, depending on the containment data as defined
     * in CLDR.  This API may return an empty set if this region doesn't have any sub-regions.
     * For example, calling this method with region "150" (Europe) returns a set containing
     * the various sub regions of Europe - "039" (Southern Europe) - "151" (Eastern Europe) 
     * - "154" (Northern Europe) and "155" (Western Europe).
     *
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */

    public Set<Region> getSubRegions() {
        initContainmentData();
        
        Set<Region> result = new TreeSet<Region>();
        Integer index = regionIndexMap.get(id);
        BitSet contains = subRegionData[index];
        for( int i = contains.nextSetBit(0); i>=0; i=contains.nextSetBit(i+1)) {
            result.add(regions[i]);
        }
        return Collections.unmodifiableSet(result);
    }
    
    /** Used to determine all the territories that are contained within this region.
     * 
     * @return An unmodifiable set containing all the territories that are children of this
     *  region anywhere in the region hierarchy.  If this region is already a territory,
     *  the empty set is returned, since territories by definition do not contain other regions.
     *  For example, calling this method with region "150" (Europe) returns a set containing all
     *  the territories in Europe ( "FR" (France) - "IT" (Italy) - "DE" (Germany) etc. )
     *
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */
    
    public Set<Region> getContainedTerritories() {
        initContainmentData();
        Set<Region> result = new TreeSet<Region>();
        Set<Region> subRegions = getSubRegions();
        Iterator<Region> it = subRegions.iterator();
        while ( it.hasNext() ) {
            Region r = it.next();
            if ( r.isOfType(RegionType.TERRITORY) ) {
                result.add(r);
            } else if ( r.isOfType(RegionType.CONTINENT) || r.isOfType(RegionType.SUBCONTINENT)) {
                result.addAll(r.getContainedTerritories()); // Recursion!!!
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Returns the string representation of this region
     * 
     * @return The string representation of this region, which is its canonical ID.
     *
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */
 
    public String toString() {
        return id;
    }
    
    /** Returns the numeric code for this region
     * 
     * @return The numeric code for this region.   Returns UNDEFINED_NUMERIC_CODE (-1) if the
     * given region does not have a numeric code assigned to it.  This is a very rare case and
     * only occurs for a few very small territories.
     *
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */
   
    public int getNumericCode() {
        return code;
    }
    
    /** Returns this region's type.
     * 
     * @return This region's type classification, such as MACROREGION or TERRITORY.
     *
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */
  
    public RegionType getType() {
        return type;
    }
    
    /** Checks to see if this region is of a specific type.
     * 
     * @return Returns TRUE if this region matches the supplied type.
     *
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release. 
     */
  
    public boolean isOfType(RegionType type) {
        return this.type.equals(type);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Region other) {
        return id.compareTo(other.id);
    }
}
