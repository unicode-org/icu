// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.locale;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * Stub class to make migration easier until we get either Guava or a higher level of Java.
 */
public class XCldrStub {

    public static class Multimap<K, V> {
        private final Map<K,Set<V>> map;
        private final Class<Set<V>> setClass;

        @SuppressWarnings("unchecked")
        private Multimap(Map<K,Set<V>> map, Class<?> setClass) {
            this.map = map;
            this.setClass = (Class<Set<V>>) (setClass != null
                    ? setClass
                            : HashSet.class);
        }
        @SafeVarargs
        @SuppressWarnings("varargs")    // Not supported by Eclipse, but we need this for javac
        public final Multimap<K, V> putAll(K key, V... values) {
            if (values.length != 0) {
                createSetIfMissing(key).addAll(Arrays.asList(values));
            }
            return this;
        }
        public void putAll(K key, Collection<V> values) {
            if (!values.isEmpty()) {
                createSetIfMissing(key).addAll(values);
            }
        }
        public void putAll(Collection<K> keys, V value) {
            for (K key : keys) {
                put(key, value);
            }
        }
        public void putAll(Multimap<K, V> source) {
            for (Entry<K, Set<V>> entry : source.map.entrySet()) {
                putAll(entry.getKey(), entry.getValue());
            }
        }
        public void put(K key, V value) {
            createSetIfMissing(key).add(value);
        }
        private Set<V> createSetIfMissing(K key) {
            Set<V> old = map.get(key);
            if (old == null) {
                map.put(key, old = getInstance());
            }
            return old;
        }
        private Set<V> getInstance() {
            try {
                return setClass.newInstance();
            } catch (Exception e) {
                throw new ICUException(e);
            }
        }
        public Set<V> get(K key) {
            Set<V> result = map.get(key);
            return result; //  == null ? Collections.<V>emptySet() : result;
        }
        public Set<K> keySet() {
            return map.keySet();
        }
        public Map<K, Set<V>> asMap() {
            return map;
        }
        public Set<V> values() {
            Collection<Set<V>> values = map.values();
            if (values.size() == 0) {
                return Collections.<V>emptySet();
            }
            Set<V> result = getInstance();
            for ( Set<V> valueSet : values) {
                result.addAll(valueSet);
            }
            return result;
        }
        public int size() {
            return map.size();
        }
        public Iterable<Entry<K, V>> entries() {
            return new MultimapIterator<>(map);
        }
        @Override
        public boolean equals(Object obj) {
            return this == obj ||
                    (obj != null
                    && obj.getClass() == this.getClass()
                    && map.equals(((Multimap<?,?>) obj).map));
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }
    }

    public static class Multimaps {
        public static <K, V, R extends Multimap<K, V>> R invertFrom(Multimap<V, K> source, R target) {
            for (Entry<V, Set<K>> entry : source.asMap().entrySet()) {
                target.putAll(entry.getValue(), entry.getKey());
            }
            return target;
        }
        public static <K, V, R extends Multimap<K, V>> R invertFrom(Map<V, K> source, R target) {
            for (Entry<V, K> entry : source.entrySet()) {
                target.put(entry.getValue(), entry.getKey());
            }
            return target;
        }
        /**
         * Warning, not functionally the same as Guava; only for use in invertFrom.
         */
        public static <K, V> Map<K,V> forMap(Map<K,V> map) {
            return map;
        }
    }

    private static class MultimapIterator<K,V> implements Iterator<Entry<K,V>>, Iterable<Entry<K,V>> {
        private final Iterator<Entry<K, Set<V>>> it1;
        private Iterator<V> it2 = null;
        private final ReusableEntry<K,V> entry = new ReusableEntry<>();

        private MultimapIterator(Map<K,Set<V>> map) {
            it1 = map.entrySet().iterator();
        }
        @Override
        public boolean hasNext() {
            return it1.hasNext() || it2 != null && it2.hasNext();
        }
        @Override
        public Entry<K, V> next() {
            if (it2 != null && it2.hasNext()) {
                entry.value = it2.next();
            } else {
                Entry<K, Set<V>> e = it1.next();
                entry.key = e.getKey();
                it2 = e.getValue().iterator();
            }
            return entry;
        }
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return this;
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ReusableEntry<K,V> implements Entry<K,V> {
        K key;
        V value;
        @Override
        public K getKey() {
            return key;
        }
        @Override
        public V getValue() {
            return value;
        }
        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    public static class HashMultimap<K, V> extends Multimap<K, V> {
        private HashMultimap() {
            super(new HashMap<K, Set<V>>(), HashSet.class);
        }
        public static <K, V> HashMultimap<K, V> create() {
            return new HashMultimap<>();
        }
    }

    public static class TreeMultimap<K, V> extends Multimap<K, V> {
        private TreeMultimap() {
            super(new TreeMap<K, Set<V>>(), TreeSet.class);
        }
        public static <K, V> TreeMultimap<K, V> create() {
            return new TreeMultimap<>();
        }
    }

    public static class LinkedHashMultimap<K, V> extends Multimap<K, V> {
        private LinkedHashMultimap() {
            super(new LinkedHashMap<K, Set<V>>(), LinkedHashSet.class);
        }
        public static <K, V> LinkedHashMultimap<K, V> create() {
            return new LinkedHashMultimap<>();
        }
    }


    //    public static class Counter<T> implements Iterable<T>{
    //        private Map<T,Long> data;
    //        @Override
    //        public Iterator<T> iterator() {
    //            return data.keySet().iterator();
    //        }
    //        public long get(T s) {
    //            Long result = data.get(s);
    //            return result != null ? result : 0L;
    //        }
    //        public void add(T item, int count) {
    //            Long result = data.get(item);
    //            data.put(item, result == null ? count : result + count);
    //        }
    //    }

    public static <T> String join(T[] source, String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < source.length; ++i) {
            if (i != 0) result.append(separator);
            result.append(source[i]);
        }
        return result.toString();
    }

    public static <T> String join(Iterable<T> source, String separator) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (T item : source) {
            if (!first) result.append(separator);
            else first = false;
            result.append(item.toString());
        }
        return result.toString();
    }

    public static class CollectionUtilities {
        public static <T, U extends Iterable<T>> String join(U source, String separator) {
            return XCldrStub.join(source, separator);
        }
    }

    public static class Joiner {
        private final String separator;
        private Joiner(String separator) {
            this.separator = separator;
        }
        public static final Joiner on(String separator) {
            return new Joiner(separator);
        }
        public <T> String join(T[] source) {
            return XCldrStub.join(source, separator);
        }
        public <T> String join(Iterable<T> source) {
            return XCldrStub.join(source, separator);
        }
    }

    public static class Splitter {
        Pattern pattern;
        boolean trimResults = false;
        public Splitter(char c) {
            this(Pattern.compile("\\Q" + c + "\\E"));
        }
        public Splitter(Pattern p) {
            pattern = p;
        }
        public static Splitter on(char c) {
            return new Splitter(c);
        }
        public static Splitter on(Pattern p) {
            return new Splitter(p);
        }
        public List<String> splitToList(String input) {
            String[] items = pattern.split(input);
            if (trimResults) {
                for (int i = 0; i < items.length; ++i) {
                    items[i] = items[i].trim();
                }
            }
            return Arrays.asList(items);
        }
        public Splitter trimResults() {
            trimResults = true;
            return this;
        }
        public Iterable<String> split(String input) {
            return splitToList(input);
        }
    }

    public static class ImmutableSet {
        public static <T> Set<T> copyOf(Set<T> values) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(values)); // copy set for safety, preserve order
        }
    }
    public static class ImmutableMap {
        public static <K,V> Map<K,V> copyOf(Map<K,V> values) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(values)); // copy set for safety, preserve order
        }
    }
    public static class ImmutableMultimap {
        public static <K,V> Multimap<K,V> copyOf(Multimap<K,V> values) {
            LinkedHashMap<K, Set<V>> temp = new LinkedHashMap<>(); // semi-deep copy, preserve order
            for (Entry<K, Set<V>> entry : values.asMap().entrySet()) {
                Set<V> value = entry.getValue();
                temp.put(entry.getKey(), value.size() == 1
                        ? Collections.singleton(value.iterator().next())
                                : Collections.unmodifiableSet(new LinkedHashSet<>(value)));
            }
            return new Multimap<>(Collections.unmodifiableMap(temp), null);
        }
    }

    public static class FileUtilities {
        public static final Charset UTF8 = Charset.forName("utf-8");

        public static BufferedReader openFile(Class<?> class1, String file) {
            return openFile(class1, file, UTF8);
        }

        public static BufferedReader openFile(Class<?> class1, String file, Charset charset) {
            // URL path = null;
            // String externalForm = null;
            try {
                final InputStream resourceAsStream = class1.getResourceAsStream(file);
                if (charset == null) {
                    charset = UTF8;
                }
                InputStreamReader reader = new InputStreamReader(resourceAsStream, charset);
                BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
                return bufferedReader;
            } catch (Exception e) {
                String className = class1 == null ? null : class1.getCanonicalName();
                String canonicalName = null;
                try {
                    String relativeFileName = getRelativeFileName(class1, "../util/");
                    canonicalName = new File(relativeFileName).getCanonicalPath();
                } catch (Exception e1) {
                    throw new ICUUncheckedIOException("Couldn't open file: " + file + "; relative to class: "
                            + className, e);
                }
                throw new ICUUncheckedIOException("Couldn't open file " + file + "; in path " + canonicalName + "; relative to class: "
                        + className, e);
            }
        }
        public static String getRelativeFileName(Class<?> class1, String filename) {
            URL resource = class1 == null ?
                    FileUtilities.class.getResource(filename) : class1.getResource(filename);
                    String resourceString = resource.toString();
                    if (resourceString.startsWith("file:")) {
                        return resourceString.substring(5);
                    } else if (resourceString.startsWith("jar:file:")) {
                        return resourceString.substring(9);
                    } else {
                        throw new ICUUncheckedIOException("File not found: " + resourceString);
                    }
        }
    }

    static public class RegexUtilities {
        public static int findMismatch(Matcher m, CharSequence s) {
            int i;
            for (i = 1; i < s.length(); ++i) {
                boolean matches = m.reset(s.subSequence(0, i)).matches();
                if (!matches && !m.hitEnd()) {
                    break;
                }
            }
            return i - 1;
        }
        public static String showMismatch(Matcher m, CharSequence s) {
            int failPoint = findMismatch(m, s);
            String show = s.subSequence(0, failPoint) + "☹" + s.subSequence(failPoint, s.length());
            return show;
        }
    }

    public interface Predicate<T> {
        /**
         * Evaluates this predicate on the given argument.
         *
         * @param t the input argument
         * @return {@code true} if the input argument matches the predicate,
         * otherwise {@code false}
         */
        boolean test(T t);
    }
}
