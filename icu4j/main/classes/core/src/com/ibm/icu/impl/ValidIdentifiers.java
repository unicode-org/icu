/*
 *******************************************************************************
 * Copyright (C) 2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

/**
 * @author markdavis
 *
 */
public class ValidIdentifiers {

    public enum Datatype {
        currency,
        language,
        region,
        script,
        subdivision,
        unit,
        variant,
    }

    public enum Datasubtype {
        deprecated,
        private_use,
        regular,
        special,
        unknown,
        macroregion,
    }

    static class ValiditySet {
        public final Set<String> regularData;
        public final Map<String,Set<String>> subdivisionData;
        public ValiditySet(Set<String> plainData, boolean makeMap) {
            if (makeMap) {
                HashMap<String,Set<String>> _subdivisionData = new HashMap<String,Set<String>>();
                for (String s : plainData) {
                    int pos = s.indexOf('-');
                    String key = s.substring(0,pos);
                    Set<String> oldSet = _subdivisionData.get(key);
                    if (oldSet == null) {
                        _subdivisionData.put(key, oldSet = new HashSet<String>());
                    }
                    oldSet.add(s.substring(pos+1));
                }
                this.regularData = null;
                HashMap<String,Set<String>> _subdivisionData2 = new HashMap<String,Set<String>>();
                // protect the sets
                for (Entry<String, Set<String>> e : _subdivisionData.entrySet()) {
                    Set<String> value = e.getValue();
                    // optimize a bit by using singleton
                    Set<String> set = value.size() == 1 ? Collections.singleton(value.iterator().next()) 
                            : Collections.unmodifiableSet(value);
                    _subdivisionData2.put(e.getKey(), set);
                }

                this.subdivisionData = Collections.unmodifiableMap(_subdivisionData2);
            } else {
                this.regularData = Collections.unmodifiableSet(plainData);
                this.subdivisionData = null;
            }
        }
        /**
         * @param code
         * @return
         */
        public boolean contains(String code) {
            if (regularData != null) {
                return regularData.contains(code);
            } else {
                int pos = code.indexOf('-');
                String key = code.substring(0,pos);
                final String value = code.substring(pos+1);
                return contains(key, value);
            }
        }
        
        public boolean contains(String key, String value) {
            Set<String> oldSet = subdivisionData.get(key);
            return oldSet != null && oldSet.contains(value);
        }
        
        @Override
        public String toString() {
            if (regularData != null) {
                return regularData.toString();
            } else {
                return subdivisionData.toString();
            }
        }
    }

    static class ValidityData {
        static final Map<Datatype,Map<Datasubtype,ValiditySet>> data;
        static {
            Map<Datatype, Map<Datasubtype, ValiditySet>> _data = new EnumMap<Datatype,Map<Datasubtype,ValiditySet>>(Datatype.class);
            UResourceBundle suppData = UResourceBundle.getBundleInstance(
                    ICUResourceBundle.ICU_BASE_NAME,
                    "supplementalData",
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle validityInfo = suppData.get("idValidity");
            for(UResourceBundleIterator datatypeIterator = validityInfo.getIterator(); 
                    datatypeIterator.hasNext();) {
                UResourceBundle datatype = datatypeIterator.next();
                String rawKey = datatype.getKey();
                Datatype key = Datatype.valueOf(rawKey);
                Map<Datasubtype,ValiditySet> values = new EnumMap<Datasubtype,ValiditySet>(Datasubtype.class);
                for(UResourceBundleIterator datasubtypeIterator = datatype.getIterator(); 
                        datasubtypeIterator.hasNext();) {
                    UResourceBundle datasubtype = datasubtypeIterator.next();
                    String rawsubkey = datasubtype.getKey();
                    Datasubtype subkey = Datasubtype.valueOf(rawsubkey);
                    // handle single value specially
                    Set<String> subvalues = new HashSet<String>();
                    if (datasubtype.getType() == UResourceBundle.STRING) {
                        addRange(datasubtype.getString(), subvalues);
                    } else {
                        for (String string : datasubtype.getStringArray()) {
                            addRange(string, subvalues);
                        }
                    }
                    values.put(subkey, new ValiditySet(subvalues, key == Datatype.subdivision));
                }
                _data.put(key, Collections.unmodifiableMap(values));
            }
            data = Collections.unmodifiableMap(_data);
        }
        private static void addRange(String string, Set<String> subvalues) {
            int pos = string.indexOf('~');
            if (pos < 0) {
                subvalues.add(string);
            } else {
                StringRange.expand(string.substring(0,pos), string.substring(pos+1), false, subvalues);
            }
        }
        static Map<Datatype, Map<Datasubtype, ValiditySet>> getData() {
            return data;
        }

        /**
         * Returns the Datasubtype containing the code, or null if there is none.
         * @param datatype
         * @param datasubtypes
         * @param code
         * @return
         */
        static Datasubtype isValid(Datatype datatype, Set<Datasubtype> datasubtypes, String code) {
            Map<Datasubtype, ValiditySet> subtable = data.get(datatype);
            if (subtable != null) {
                for (Datasubtype datasubtype : datasubtypes) {
                    ValiditySet validitySet = subtable.get(datasubtype);
                    if (validitySet != null) {
                        if (validitySet.contains(code)) {
                            return datasubtype;
                        }
                    }
                }
            }
            return null;
        }
        
        static Datasubtype isValid(Datatype datatype, Set<Datasubtype> datasubtypes, String code, String value) {
            Map<Datasubtype, ValiditySet> subtable = data.get(datatype);
            if (subtable != null) {
                for (Datasubtype datasubtype : datasubtypes) {
                    ValiditySet validitySet = subtable.get(datasubtype);
                    if (validitySet != null) {
                        if (validitySet.contains(code, value)) {
                            return datasubtype;
                        }
                    }
                }
            }
            return null;
        }

    }

    // Quick testing for now
    
    public static void main(String[] args) {
        showValid(Datatype.script, EnumSet.of(Datasubtype.regular, Datasubtype.unknown), "Zzzz");
        showValid(Datatype.script, EnumSet.of(Datasubtype.regular), "Zzzz");
        showValid(Datatype.subdivision, EnumSet.of(Datasubtype.regular), "US-CA");
        showValid(Datatype.subdivision, EnumSet.of(Datasubtype.regular), "US", "CA");
        showValid(Datatype.subdivision, EnumSet.of(Datasubtype.regular), "US-?");
        showValid(Datatype.subdivision, EnumSet.of(Datasubtype.regular), "US", "?");
        showAll();
    }

    private static void showAll() {
        Map<Datatype, Map<Datasubtype, ValiditySet>> data = ValidityData.getData();
        for (Entry<Datatype, Map<Datasubtype, ValiditySet>> e1 : data.entrySet()) {
            System.out.println(e1.getKey());
            for (Entry<Datasubtype, ValiditySet> e2 : e1.getValue().entrySet()) {
                System.out.println("\t" + e2.getKey());
                System.out.println("\t\t" + e2.getValue());
            }
        }
    }

    /**
     * @param script
     * @param of
     * @param string
     */
    private static void showValid(Datatype datatype, Set<Datasubtype> datasubtypes, String code) {
        Datasubtype value = ValidityData.isValid(datatype, datasubtypes, code);   
        System.out.println(datatype + ", " + datasubtypes + ", " + code + " => " + value);
    }
    private static void showValid(Datatype datatype, Set<Datasubtype> datasubtypes, String code, String value2) {
        Datasubtype value = ValidityData.isValid(datatype, datasubtypes, code, value2);   
        System.out.println(datatype + ", " + datasubtypes + ", " + code + ", " + value + " => " + value);
    }

}
