package com.ibm.icu.dev.test.util;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    
    public static boolean DEBUG = false;
    
    private String propertyAlias;
    private String shortestPropertyAlias = null;
    private int type;
    private Map valueToShortValue = null;
    
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

    public String getVersion() {
        return _getVersion();
    }
    public String getValue(int codepoint) {
        return _getValue(codepoint);
    }
    public Collection getAliases(Collection result) {
        return _getAliases(result);
    }
    public Collection getValueAliases(String valueAlias, Collection result) {
        result = _getValueAliases(valueAlias, result);
        if (!result.contains(valueAlias) && type < NUMERIC) {
            throw new IllegalArgumentException(
                "Internal error: result doesn't contain " + valueAlias);
        }
        return result;
    }
    public Collection getAvailableValueAliases(Collection result) {
        return _getAvailableValueAliases(result);
    }

    protected abstract String _getVersion();
    protected abstract String _getValue(int codepoint);
    protected abstract Collection _getAliases(Collection result);
    protected abstract Collection _getValueAliases(String valueAlias, Collection result);
    protected abstract Collection _getAvailableValueAliases(Collection result);
    
    // conveniences
    public final Collection getAliases() {
        return _getAliases(null);
    }
    public final Collection getValueAliases(String valueAlias) {
        return _getValueAliases(valueAlias, null);
    }
    public final Collection getAvailableValueAliases() {
        return _getAvailableValueAliases(null);
    }
    
    static public class Factory {
        Map canonicalNames = new TreeMap();
        Map skeletonNames = new TreeMap();
        Map propertyCache = new HashMap();
        
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
        InverseMatcher inverseMatcher = new InverseMatcher();
        /**
         * Format is:
         *    propname ('=' | '!=') propvalue ( '|' propValue )*
         */
        public final UnicodeSet getSet(String propAndValue, Matcher matcher, UnicodeSet result) {
            int equalPos = propAndValue.indexOf('=');
            String prop = propAndValue.substring(0,equalPos);
            String value = propAndValue.substring(equalPos+1);
            boolean negative = false;
            if (prop.endsWith("!")) {
                prop = prop.substring(0,prop.length()-1);
                negative = true;
            }
            prop = prop.trim();
            UnicodeProperty up = getProperty(prop);
            if (matcher == null) {
                matcher = new SimpleMatcher(value,
                up.getType() >= STRING ? null : new SkeletonComparator());
            }
            if (negative) {
                inverseMatcher.set(matcher); 
                matcher = inverseMatcher;
            }
            return up.getSet(matcher.set(value), result);
        }
        
        public final UnicodeSet getSet(String propAndValue, Matcher matcher) {
            return getSet(propAndValue, matcher, null);
        }
        public final UnicodeSet getSet(String propAndValue) {
            return getSet(propAndValue, null, null);
        }
    }

    public static class FilteredProperty extends UnicodeProperty {
        private UnicodeProperty property;
        protected StringFilter filter;
        protected UnicodeSetIterator matchIterator = new UnicodeSetIterator(new UnicodeSet(0,0x10FFFF));
        protected HashMap backmap;
        
        public FilteredProperty(UnicodeProperty property, StringFilter filter) {
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

        Collection temp = new ArrayList();
        
        public Collection _getAvailableValueAliases(Collection result) {
            temp.clear();
            return filter.addUnique(property.getAvailableValueAliases(temp), result);
        }

        public Collection _getAliases(Collection result) {
            temp.clear();
            return filter.addUnique(
                property.getAliases(temp), result);
        }

        public String _getValue(int codepoint) {
            return filter.remap(property.getValue(codepoint));
        }

        public Collection _getValueAliases(String valueAlias, Collection result) {
            temp.clear();
            if (backmap == null) {
                backmap = new HashMap();
                temp.clear();
                Iterator it = property.getAvailableValueAliases(temp).iterator();
                while (it.hasNext()) {
                    String item = (String) it.next();
                    String mappedItem = filter.remap(item);
                    if (backmap.get(mappedItem) != null) {
                        throw new IllegalArgumentException("Filter makes values collide!");
                    }
                    backmap.put(mappedItem, item);
                }
            }
            return filter.addUnique(
              property.getValueAliases((String) backmap.get(valueAlias), temp), result);
        }

        public String _getVersion() {
            return property.getVersion();
        }

    }

    public static abstract class StringFilter implements Cloneable {
        public abstract String remap(String original);
        public final Collection addUnique(Collection source, Collection result) {
            if (result == null) result = new ArrayList();
            Iterator it = source.iterator();
            while (it.hasNext()) {
                UnicodeProperty.addUnique(
                    remap((String) it.next()), result);                
            }
            return result;
        }
        /*
         public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError("Should never happen.");
            }
        }
        */
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
    
    public static class InverseMatcher implements Matcher {
        Matcher other;
        public Matcher set(Matcher toInverse) {
            other = toInverse;
            return this;
        }
        public boolean matches(String value) {
            return !other.matches(value);
        }
        public Matcher set(String pattern) {
            other.set(pattern);
            return this;
        }
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
        String version;
        
        public SimpleProperty setMain(String alias, String shortAlias, int propertyType,
          String version) {
            setName(alias);
            setType(propertyType);
            this.shortAlias = shortAlias;
            this.version = version;
            return this;
        }
        
        public SimpleProperty setValues(String valueAlias) {
            setValues(new String[]{valueAlias}, null);
            return this;
        }
        
        public SimpleProperty setValues(String[] valueAliases, String[] alternateValueAliases) {
            this.valueAliases = Arrays.asList((Object[]) valueAliases.clone());
            
            for (int i = 0; i < valueAliases.length; ++i) {
                List a = new ArrayList();
                addUnique(valueAliases[i],a);
                if (alternateValueAliases != null) addUnique(alternateValueAliases[i],a);               
                toAlternates.put(valueAliases[i], a);
            }
            return this;
        }
        
        public SimpleProperty setValues(Collection valueAliases) {
            this.valueAliases = new ArrayList(valueAliases);           
            for (Iterator it = this.valueAliases.iterator(); it.hasNext(); ) {
                Object item = it.next();
                List list = new ArrayList();
                list.add(item);
                toAlternates.put(item, list);
            }
            return this;
        }
        
        public Collection _getAliases(Collection result) {
            if (result == null) result = new ArrayList();
            addUnique(getName(), result);
            addUnique(shortAlias, result);
            return result;
        }

        public Collection _getValueAliases(String valueAlias, Collection result) {
            if (result == null) result = new ArrayList();
            Collection a = (Collection) toAlternates.get(valueAlias);
            if (a != null) addAllUnique(a, result);
            return result;
        }

        public Collection _getAvailableValueAliases(Collection result) {
            if (result == null) result = new ArrayList();
            result.addAll(valueAliases);
            return result;
        }
        
        public String _getVersion() {
            return version;
        }
    }

           
    public final String getValue(int codepoint, boolean getShortest) {
        String result = getValue(codepoint);
        if (!getShortest || result == null) return result;
        return getShortestValueAlias(result);
    }
    
    public final String getShortestValueAlias(String value) {
        if (valueToShortValue == null) getValueCache();
        return (String)valueToShortValue.get(value);       
    }

    public final String getShortestAlias() {
        if (shortestPropertyAlias == null) {
            shortestPropertyAlias = propertyAlias;
            for (Iterator it = _getAliases(null).iterator(); it.hasNext();) {
                String item = (String) it.next();
                if (item.length() < shortestPropertyAlias.length()) {
                    shortestPropertyAlias = item;
                }
            }
        }
        return shortestPropertyAlias;       
    }

    private void getValueCache() {
        maxValueWidth = 0;
        maxShortestValueWidth = 0;
        valueToShortValue = new HashMap();
        Iterator it = getAvailableValueAliases(null).iterator();
        while (it.hasNext()) {
            String value = (String)it.next();
            String shortest = value;
            Iterator it2 = getValueAliases(value, null).iterator();
            while (it2.hasNext()) {
                String other = (String)it2.next();
                if (shortest.length() > other.length()) shortest = other;
            }
            valueToShortValue.put(value,shortest);
            if (value.length() > maxValueWidth) maxValueWidth = value.length();
            if (shortest.length() > maxShortestValueWidth) maxShortestValueWidth = shortest.length();
        }
    }
    
    private int maxValueWidth = -1;
    private int maxShortestValueWidth = -1;
    
    public final int getMaxWidth(boolean getShortest) {
        if (maxValueWidth < 0) getValueCache();
        if (getShortest) return maxShortestValueWidth;
        return maxValueWidth;
    }
    
    public final UnicodeSet getSet(String propertyValue) {
        return getSet(propertyValue,null);
    }
    public final UnicodeSet getSet(Matcher matcher) {
        return getSet(matcher,null);
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
        if (cacheValueToSet == null) cacheValueToSet = _getUnicodeMap();
        Collection temp = new HashSet(); // to avoid reallocating...
        Iterator it = cacheValueToSet.getAvailableValues(null).iterator();
        main:
        while (it.hasNext()) {
            String value = (String)it.next();
            temp.clear();
            Iterator it2 = getValueAliases(value,temp).iterator();
            while (it2.hasNext()) {
                String value2 = (String)it2.next();
                System.out.println("Values:" + value2);
                if (matcher.matches(value2) 
                  || matcher.matches(toSkeleton(value2))) {
                    cacheValueToSet.getSet(value, result);
                    continue main;    
                }
            }
        }
        return result;
    }
    
    protected UnicodeMap _getUnicodeMap() {
        UnicodeMap result = new UnicodeMap();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (DEBUG && i == 0x41) System.out.println(i + "\t" + getValue(i));
            result.put(i, getValue(i));
        }
        if (DEBUG) {
            System.out.println(getName() + ":\t" + getClass().getName()
                 + "\t" + getVersion());
            System.out.println(getStack());
            System.out.println(result);
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
    
    public static String getStack() {
        Exception e = new Exception();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return "Showing Stack with fake " + sw.getBuffer().toString();
    }
    

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
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
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
    
