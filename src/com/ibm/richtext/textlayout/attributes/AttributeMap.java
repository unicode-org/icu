/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
// Requires Java2
package com.ibm.richtext.textlayout.attributes;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;


/**
 * AttributeMap is an immutable Map.  Additionally, there are
 * several methods for common operations (union,
 * remove, intersect);  these methods return new AttributeMap
 * instances.
 * <p>
 * Although any non-null Object can be a key or value in an
 * AttributeMap, typically the keys are fields of TextAttribute.
 * @see TextAttribute
 */
public final class AttributeMap implements java.util.Map,
                                com.ibm.richtext.textlayout.attributes.Map,
                                Externalizable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int CURRENT_VERSION = 1;

    private static final long serialVersionUID = 9510803;

    private static final String errString = "StyleSets are immutable.";

    // This is passed to the Hashtable constructor as the
    // load factor argument.  It is chosen to avoid resizing
    // the Hashtable whenever possible.  I think that 1
    // does this.
    private static final int LOAD_FACTOR = 1;

    private Hashtable styleTable;
    private transient AttributeSet cachedKeySet = null;
    private transient Collection cachedValueCollection = null;
    private transient Set cachedEntrySet = null;

    /**
     * An empty AttributeMap.
     */
    public static final AttributeMap EMPTY_ATTRIBUTE_MAP = new AttributeMap();

// ==============
// Constructors
// ==============

    /**
     * Create a new, empty AttributeMap.  EMPTY_STYLE_SET can be used
     * in place of an AttributeMap produced by this constructor.
     */
    public AttributeMap() {

        styleTable = new Hashtable(1, LOAD_FACTOR);
    }

    /**
     * Create an AttributeMap with the same key-value
     * entries as the given Map.
     * @param map a Map whose key-value entries will
     *      become the entries for this AttributeMap. <code>map</code>
     *      is not modified, and must not contain null keys or values.
     */
    public AttributeMap(java.util.Map map) {

        styleTable = new Hashtable(map.size(), LOAD_FACTOR);
        styleTable.putAll(map);
    }

    /**
     * Create an AttributeMap with the same key-value
     * entries as the given Hashtable.
     * @param hashtable a Hashtable whose key-value entries will
     *      become the entries for this AttributeMap. <code>table</code>
     *      is not modified.
     */
    public AttributeMap(Hashtable hashtable) {

        this((java.util.Map) hashtable);
    }

    /**
     * Create an AttributeMap with a single entry of
     * <code>{attribute, value}</code>.
     * @param key the key in this AttributeMap's single entry
     * @param value the value in this AttributeMap's single entry
     */
    public AttributeMap(Object key, Object value) {

        styleTable = new Hashtable(1, LOAD_FACTOR);

        // hashtable checks value for null
        styleTable.put(key, value);
    }

    // For internal use only.
    private AttributeMap(Hashtable table, boolean clone) {

        if (clone) {
            styleTable = (Hashtable) table.clone();
        }
        else {
            this.styleTable = table;
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeInt(CURRENT_VERSION);
        out.writeInt(styleTable.size());
        Enumeration e = styleTable.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            out.writeObject(AttributeKey.mapAttributeToKey(key));
            out.writeObject(styleTable.get(key));
        }
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {

        if (in.readInt() != CURRENT_VERSION) {
            throw new IOException("Invalid version of StyleBuffer");
        }

        int count = in.readInt();
        for (int i=0; i < count; i += 1) {
            Object key = AttributeKey.mapKeyToAttribute(in.readObject());
            Object value = in.readObject();
            styleTable.put(key, value);
        }
    }

// ==============
// Map interface
// ==============

// queries
    /**
     * Return the number of entries in the AttributeMap.
     * @return the number of entries in the AttributeMap
     */
    public int size() {

        return styleTable.size();
    }

    /**
     * Return true if the number of entries in the AttributeMap
     * is 0.
     * @return true if the number of entries in the AttributeMap
     * is 0
     */
    public boolean isEmpty() {

        return styleTable.isEmpty();
    }

    /**
     * Return true if the given key is in this AttributeMap.
     * @param key the key to test
     * @return true if <code>key</code> is in this AttributeMap
     */
    public boolean containsKey(Object key) {

        return styleTable.containsKey(key);
    }

    /**
     * Return true if the given value is in this AttributeMap.
     * @param value the value to test
     * @return true if <code>value</code> is in this AttributeMap
     */
    public boolean containsValue(Object value) {

        return styleTable.containsValue(value);
    }

    /**
     * Return the value associated with the given key.  If the
     * key is not in this AttributeMap null is returned.
     * @param key the key to look up
     * @return the value associated with <code>key</code>, or
     *     null if <code>key</code> is not in this AttributeMap
     */
    public Object get(Object key) {

        return styleTable.get(key);
    }

// modifiers - all throw exceptions

    /**
     * Throws UnsupportedOperationException.
     * @see #addAttribute
     * @throws UnsupportedOperationException
     */
    public Object put(Object key, Object value) {

        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @see #removeAttributes
     * @throws UnsupportedOperationException
     */
    public Object remove(Object key) {

        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @see #addAttributes
     * @throws UnsupportedOperationException
     */
    public void putAll(java.util.Map t) {

        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @see #EMPTY_ATTRIBUTE_MAP
     * @throws UnsupportedOperationException
     */
    public void clear() {

        throw new UnsupportedOperationException(errString);
    }

// views

    /**
     * Return an AttributeSet containing every key in this AttributeMap.
     * @return an AttributeSet containing every key in this AttributeMap
     */
    public Set keySet() {

        return getKeySet();
    }

    /**
     * Return an AttributeSet containing every key in this AttributeMap.
     * @return an AttributeSet containing every key in this AttributeMap
     */
    public AttributeSet getKeySet() {

        AttributeSet result = cachedKeySet;

        if (result == null) {
            result = AttributeSet.createKeySet(styleTable);
            cachedKeySet = result;
        }

        return result;
    }

    /**
     * Return a Collection containing every value in this AttributeMap.
     * @return a Collection containing every value in this AttributeMap
     */
    public Collection values() {

        Collection result = cachedValueCollection;

        if (result == null) {
            result = Collections.unmodifiableCollection(styleTable.values());
            cachedValueCollection = result;
        }

        return result;
    }

    /**
     * Return a Set containing all entries in this AttributeMap.
     */
    public Set entrySet() {

        Set result = cachedEntrySet;

        if (result == null) {
            result = Collections.unmodifiableSet(styleTable.entrySet());
            cachedEntrySet = result;
        }

        return result;
    }

    public boolean equals(Object rhs) {

        if (rhs == this) {
            return true;
        }

        if (rhs == null) {
            return false;
        }

        AttributeMap rhsStyleSet = null;

        try {
            rhsStyleSet = (AttributeMap) rhs;
        }
        catch(ClassCastException e) {
            return false;
        }

        return styleTable.equals(rhsStyleSet.styleTable);
    }

    public int hashCode() {

        return styleTable.hashCode();
    }

    public String toString() {

        return styleTable.toString();
    }

// ==============
// Operations
// ==============

    /**
     * Return a AttributeMap which contains entries in this AttributeMap,
     * along with an entry for <attribute, value>.  If attribute
     * is already present in this AttributeMap its value becomes value.
     */
    public AttributeMap addAttribute(Object key, Object value) {

        // try to optimize for case where <key, value> is already there?
        Hashtable newTable = new Hashtable(styleTable.size() + 1, LOAD_FACTOR);
        newTable.putAll(styleTable);
        newTable.put(key, value);
        return new AttributeMap(newTable, false);
    }

    /**
     * Return a AttributeMap which contains entries in this AttributeMap
     * and in rhs.  If an attribute appears in both StyleSets the
     * value from rhs is used.
     */
    public AttributeMap addAttributes(AttributeMap rhs) {

        int thisSize = size();

        if (thisSize == 0) {
            return rhs;
        }

        int otherSize = rhs.size();

        if (otherSize == 0) {
            return this;
        }

        Hashtable newTable = new Hashtable(thisSize + otherSize, LOAD_FACTOR);

        newTable.putAll(styleTable);
        newTable.putAll(rhs);

        return new AttributeMap(newTable, false);
    }

    /**
     * Return a AttributeMap which contains entries in this AttributeMap
     * and in rhs.  If an attribute appears in both StyleSets the
     * value from rhs is used.
     * The Map's keys and values must be non-null.
     */
    public AttributeMap addAttributes(java.util.Map rhs) {

        if (rhs instanceof AttributeMap) {
            return addAttributes((AttributeMap)rhs);
        }

        Hashtable newTable = new Hashtable(size() + rhs.size(), LOAD_FACTOR);

        newTable.putAll(styleTable);
        newTable.putAll(rhs);

        return new AttributeMap(newTable, false);
    }

    /**
     * Return a AttributeMap with the entries in this AttributeMap, but
     * without attribute as a key.
     */
    public AttributeMap removeAttribute(Object attribute) {

        if (!containsKey(attribute)) {
            return this;
        }

        Hashtable newTable = new Hashtable(styleTable.size(), LOAD_FACTOR);
        newTable.putAll(styleTable);
        newTable.remove(attribute);

        return new AttributeMap(newTable, false);
    }

    /**
     * Return a AttributeMap with the entries of this AttributeMap whose
     * attributes are <b>not</b> in the Set.
     */
    public AttributeMap removeAttributes(AttributeSet attributes) {

        Set set = attributes;
        return removeAttributes(set);
    }

    /**
     * Return a AttributeMap with the entries of this AttributeMap whose
     * attributes are <b>not</b> in the Set.
     */
    public AttributeMap removeAttributes(Set attributes) {

        // Create newTable on demand;  if null at
        // end of iteration then return this set.
        // Should we intersect styleTable.keySet with
        // attributes instead?

        Hashtable newTable = null;
        Iterator attrIter = attributes.iterator();
        while (attrIter.hasNext()) {
            Object current = attrIter.next();
            if (current != null && styleTable.containsKey(current)) {
                if (newTable == null) {
                    newTable = new Hashtable(styleTable.size(), LOAD_FACTOR);
                    newTable.putAll(styleTable);
                }
                newTable.remove(current);
            }
        }

        if (newTable != null) {
            return new AttributeMap(newTable, false);
        }
        else {
            return this;
        }
    }

    /**
     * Return a AttributeMap with the keys of this AttributeMap which
     * are also in the Set.  The set must not contain null.
     */
    public AttributeMap intersectWith(AttributeSet attributes) {

        Set set = attributes;
        return intersectWith(set);
    }

    /**
     * Return a AttributeMap with the keys of this AttributeMap which
     * are also in the Set.  The set must not contain null.
     */
    public AttributeMap intersectWith(Set attributes) {

        // For now, forget about optimizing for the case when
        // the return value is equivalent to this set.

        int attrSize = attributes.size();
        int styleTableSize = styleTable.size();
        int size = Math.min(attrSize, styleTableSize);
        Hashtable newTable = new Hashtable(size, LOAD_FACTOR);

        if (attrSize < styleTableSize) {
            Iterator attrIter = attributes.iterator();
            while (attrIter.hasNext()) {
                Object current = attrIter.next();
                if (current != null) {
                    Object value = styleTable.get(current);
                    if (value != null) {
                        newTable.put(current, value);
                    }
                }
            }
        }
        else {
            Iterator attrIter = keySet().iterator();
            while (attrIter.hasNext()) {
                Object current = attrIter.next();
                if (attributes.contains(current)) {
                    newTable.put(current, styleTable.get(current));
                }
            }
        }

        return new AttributeMap(newTable, false);
    }

    /**
     * Put all entries in this AttributeMap into the given Map.
     * @param rhs the Map into which entries are placed
     */
    public void putAllInto(java.util.Map rhs) {

        rhs.putAll(this);
    }
}
