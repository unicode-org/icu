package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public abstract class UnicodeProperty extends UnicodeLabel {
    
    private String propertyAlias;
    private int type;
    private Map mapToShortName = null;
    
    public static final int UNKNOWN = 0,
        BINARY = 2, EXTENDED_BINARY = 3,
        ENUMERATED = 4, EXTENDED_ENUMERATED = 5,
        NUMERIC = 6, EXTENDED_NUMERIC = 7,
        STRING = 8, EXTENDED_STRING = 9,
        LIMIT_TYPE = 10,
        EXTENDED_BIT = 1;
        
    private static final String[] TYPE_NAMES = {
        "Unknown",
        "Unknown",
        "Binary",
        "Extended Binary",
        "Enumerated",
        "Extended Enumerated",
        "Numeric",
        "Extended Numeric",
        "String",
        "Extended String",
    };
    
    public static String getTypeName(int propType) {
        return TYPE_NAMES[propType];
    }
    
    public final String getName() {
        return propertyAlias;
    }
    
    public final int getType() {
        return type;
    }

    protected final void setName(String string) {
        propertyAlias = string;
    }

    protected final void setType(int i) {
        type = i;
    }

    public abstract String getValue(int codepoint);
    public abstract Collection getAliases(Collection result);
    public abstract Collection getValueAliases(String valueAlias, Collection result);
    abstract public Collection getAvailableValueAliases(Collection result);
    
    static public class Factory {
        Map canonicalNames = new TreeMap();
        Map skeletonNames = new TreeMap();
        
        public final Factory add(UnicodeProperty sp) {
            canonicalNames.put(sp.getName(), sp);
            Collection c = sp.getAliases(new TreeSet());
            Iterator it = c.iterator();
            while (it.hasNext()) {
                skeletonNames.put(toSkeleton((String)it.next()), sp);               
            }
            return this;
        }
 
        public final UnicodeProperty getProperty(String propertyAlias) {
            return (UnicodeProperty) skeletonNames.get(toSkeleton(propertyAlias));
        }

        public final Collection getAvailableAliases(Collection result) {
            if (result == null) result = new ArrayList();
            Iterator it = canonicalNames.keySet().iterator();
            while (it.hasNext()) {
                addUnique(it.next(), result);
            }
            return result;
        }
        public final Collection getAvailableAliases() {
            return getAvailableAliases(null);
        }

        public final Collection getAvailablePropertyAliases(Collection result, int propertyTypeMask) {
            Iterator it = canonicalNames.keySet().iterator();
            while (it.hasNext()) {
                UnicodeProperty property = (UnicodeProperty)it.next();
                if (((1<<property.getType())& propertyTypeMask) == 0) continue;
                addUnique(property.getName(), result);
            }
            return result;
        }
        /**
         * Format is:
         *    propname ('=' | '!=') propvalue ( '|' propValue )*
         */
        public final UnicodeSet getSet(String propAndValue, Matcher matcher, UnicodeSet result) {
            int equalPos = propAndValue.indexOf('=');
            String prop = propAndValue.substring(0,equalPos);
            boolean negative = false;
            if (prop.endsWith("!")) {
                prop = prop.substring(0,prop.length()-1);
            }
            prop = prop.trim();
            String value = propAndValue.substring(equalPos+1);
            UnicodeProperty up = getProperty(prop);
            if (matcher != null) {
                return up.getSet(matcher.set(value), result);
            }
            return up.getSet(value,result);
        }
        public final UnicodeSet getSet(String propAndValue, Matcher matcher) {
            return getSet(propAndValue, matcher, null);
        }
        public final UnicodeSet getSet(String propAndValue) {
            return getSet(propAndValue, null, null);
        }
    }

    static class FilteredUnicodeProperty extends UnicodeProperty {
        UnicodeProperty property;
        protected StringFilter filter;
        protected UnicodeSetIterator matchIterator = new UnicodeSetIterator(new UnicodeSet(0,0x10FFFF));

        FilteredUnicodeProperty(UnicodeProperty property, StringFilter filter) {
            this.property = property;
            this.filter = filter;
        }
    
        public StringFilter getFilter() {
            return filter;
        }
    
        public UnicodeProperty setFilter(StringFilter filter) {
            this.filter = filter;
            return this;
        }

        public Collection getAvailableValueAliases(Collection result) {
            return property.getAvailableValueAliases(result);
        }

        public Collection getAliases(Collection result) {
            return property.getAliases(result);
        }

        public String getValue(int codepoint) {
            return filter.remap(property.getValue(codepoint));
        }

        public Collection getValueAliases(
            String valueAlias,
            Collection result) {
            return property.getValueAliases(valueAlias, result);
        }

    }

    public static class StringFilter implements Cloneable {
        public String remap(String original) {
            return original;
        }
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError("Should never happen.");
            }
        }
    }
    
    public static class MapFilter extends StringFilter {
        private Map valueMap;
        public MapFilter(Map valueMap){
            this.valueMap = valueMap;
        }
        public String remap(String original) {
            Object changed = valueMap.get(original);
            return changed == null ? original : (String) changed;
        }
        public Map getMap() {
            return valueMap;
        }
    }
    
    public interface Matcher {
        /**
         * Must be able to handle null
         * @param value
         * @return
         */
        public boolean matches(String value);
        public Matcher set(String pattern);
    }
    
    public static class SimpleMatcher implements Matcher {
        Comparator comparator;
        String pattern;
        public SimpleMatcher(String pattern, Comparator comparator) {
            this.comparator = comparator;    
            this.pattern = pattern;       
        }
        public boolean matches(String value) {
            if (comparator == null) return pattern.equals(value);
            return comparator.compare(pattern, value) == 0;
        }
        public Matcher set(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }
    
    public static abstract class SimpleProperty extends UnicodeProperty {
        private String shortAlias;
        Collection valueAliases = new ArrayList();
        Map toAlternates = new HashMap();
        
        protected void set(String alias, String shortAlias, int propertyType, String valueAlias) {
            set(alias,shortAlias,propertyType,new String[]{valueAlias},null);
        }
        
        protected void set(String alias, String shortAlias, int propertyType,
          String[] valueAliases, String[] alternateValueAliases) {
            setName(alias);
            setType(propertyType);
            this.shortAlias = shortAlias;
            this.valueAliases = Arrays.asList((Object[]) valueAliases.clone());
            
            for (int i = 0; i < valueAliases.length; ++i) {
                List a = new ArrayList();
                addUnique(valueAliases[i],a);
                if (alternateValueAliases != null) addUnique(alternateValueAliases[i],a);               
                toAlternates.put(valueAliases[i], a);
            }
        }
        
        protected void set(String alias, String shortAlias, int propertyType,
          Collection valueAliases) {
            setName(alias);
            setType(propertyType);
            this.shortAlias = shortAlias;
            this.valueAliases = new ArrayList(valueAliases);
        }
        
        public Collection getAliases(Collection result) {
            if (result == null) result = new ArrayList();
            addUnique(getName(), result);
            addUnique(shortAlias, result);
            return result;
        }

        public Collection getValueAliases(String valueAlias, Collection result) {
            if (result == null) result = new ArrayList();
            Collection a = (Collection) toAlternates.get(valueAlias);
            if (a != null) result.addAll(valueAliases);
            return result;
        }

        public Collection getAvailableValueAliases(Collection result) {
            if (result == null) result = new ArrayList();
            result.addAll(valueAliases);
            return result;
        }
    }

           
    public final String getValue(int codepoint, boolean getShortest) {
        String result = getValue(codepoint);
        if (!getShortest || result == null) return result;
        if (mapToShortName == null) getValueCache();
        return (String)mapToShortName.get(result);
    }

    private void getValueCache() {
        maxWidth = 0;
        mapToShortName = new HashMap();
        Iterator it = getAvailableValueAliases(null).iterator();
        while (it.hasNext()) {
            String value = (String)it.next();
            String shortest = value;
            Iterator it2 = getValueAliases(value, null).iterator();
            while (it2.hasNext()) {
                String other = (String)it2.next();
                if (shortest.length() > other.length()) shortest = other;
            }
            mapToShortName.put(value,shortest);
            if (shortest.length() > maxWidth) maxWidth = shortest.length();
        }
    }
    
    private int maxWidth = -1;
    
    public final int getMaxWidth(boolean getShortest) {
        if (maxWidth < 0) getValueCache();
        return maxWidth;
    }
    
    public final UnicodeSet getSet(String propertyValue, UnicodeSet result) {
        int type = getType();
        return getSet(new SimpleMatcher(propertyValue,
            type >= STRING ? null : new SkeletonComparator()),
          result);
    }
    
    private UnicodeMap cacheValueToSet = null;
    
    public final UnicodeSet getSet(Matcher matcher, UnicodeSet result) {
        if (result == null) result = new UnicodeSet();
        if (type >= STRING) {
            for (int i = 0; i <= 0x10FFFF; ++i) {
                String value = getValue(i);
                if (matcher.matches(value)) {
                    result.add(i);
                }
            }
            return result;
        }
        if (cacheValueToSet == null) {
            cacheValueToSet = new UnicodeMap();
            for (int i = 0; i <= 0x10FFFF; ++i) {
                cacheValueToSet.put(i, getValue(i));
            }
        }
        Collection temp = new HashSet(); // to avoid reallocating...
        Iterator it = cacheValueToSet.getAvailableValues(null).iterator();
        main:
        while (it.hasNext()) {
            String value = (String)it.next();
            temp.clear();
            Iterator it2 = getValueAliases(value,temp).iterator();
            while (it2.hasNext()) {
                String value2 = (String)it2.next();
                if (matcher.matches(value2) 
                  || matcher.matches(toSkeleton(value2))) {
                    cacheValueToSet.getSet(value, result);
                    continue main;    
                }
            }
        }
        return result;
    }

    /*
    public UnicodeSet getMatchSet(UnicodeSet result) {
        if (result == null) result = new UnicodeSet();
        addAll(matchIterator, result);
        return result;
    }

    public void setMatchSet(UnicodeSet set) {
        matchIterator = new UnicodeSetIterator(set);
    }
    */
    
    public static Collection addUnique(Object obj, Collection result) {
        if (obj != null && !result.contains(obj)) result.add(obj);
        return result;
    }

    public static Collection addAllUnique(Collection source, Collection result) {
        Iterator it = source.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj != null && !result.contains(obj)) result.add(obj);
        }
        return result;
    }
    
    public static class SkeletonComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            // TODO optimize
            return toSkeleton((String)o1).compareTo(toSkeleton((String)o2));
        }
    }
    
    private static String toSkeleton(String source) {
        StringBuffer skeletonBuffer = new StringBuffer();
        boolean gotOne = false;
        // remove spaces, '_', '-'
        // we can do this with char, since no surrogates are involved
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch == '_' || ch == ' ' || ch == '-') {
                gotOne = true;
            } else {
                char ch2 = Character.toLowerCase(ch);
                if (ch2 != ch) {
                    gotOne = true;
                    skeletonBuffer.append(ch2);
                } else {
                    skeletonBuffer.append(ch);
                }
            }
        }
        if (!gotOne) return source; // avoid string creation
        return skeletonBuffer.toString();
    }

    /**
     * Utility function for comparing codepoint to string without
     * generating new string.
     * @param codepoint
     * @param other
     * @return
     */
    public static final boolean equals(int codepoint, String other) {
        if (other.length() == 1) {
            return codepoint == other.charAt(0);
        }
        if (other.length() == 2) {
            return other.equals(UTF16.valueOf(codepoint));
        }
        return false;
    }
    
    /**
     * Utility that should be on UnicodeSet
     * @param source
     * @param result
     */
    static public void addAll(UnicodeSetIterator source, UnicodeSet result) {
        while (source.nextRange()) {
            if (source.codepoint == UnicodeSetIterator.IS_STRING) {
                result.add(source.string);
            } else {
                result.add(source.codepoint, source.codepointEnd);
            }
        }
    }
}
    
