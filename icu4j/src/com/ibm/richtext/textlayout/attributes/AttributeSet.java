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

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;

/**
 * An AttributeSet is an immutable collection of unique Objects.
 * It has several operations
 * which return new AttributeSet instances.
 */
public final class AttributeSet implements Set {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    /**
     * An AttributeSet with no members.
     */
    public static final AttributeSet EMPTY_SET = new AttributeSet();

    private Hashtable elements;

    private static final String errString = "AttributeSet is immutable.";

    private AttributeSet(Hashtable elements) {

        this.elements = elements;
    }

    /**
     * Package only.  For AttributeMap use.
     */
    static AttributeSet createKeySet(Hashtable hashtable) {

        Hashtable newElements = new Hashtable();

        Enumeration e = hashtable.keys();
        while (e.hasMoreElements()) {
            Object next = e.nextElement();
            newElements.put(next, next);
        }

        return new AttributeSet(newElements);
    }

    /**
     * Create a new, empty AttributeSet.  The set is semantically
     * equivalent to EMPTY_SET.
     */
    public AttributeSet() {

        elements = new Hashtable();
    }

    /**
     * Create a new AttributeSet with the single element elem.
     */
    public AttributeSet(Object elem) {

        elements = new Hashtable(1, 1);
        elements.put(elem, elem);
    }

    /**
     * Create a new AttributeSet containing the items in the array elems.
     */
    public AttributeSet(Object[] elems) {

        elements = new Hashtable(elems.length, 1);
        for (int i=0; i < elems.length; i++) {
            Object next = elems[i];
            elements.put(next, next);
        }
    }

    /**
     * Return true if the number of elements in this set is 0.
     * @return true if the number of elements in this set is 0
     */
    public boolean isEmpty() {

        return elements.isEmpty();
    }

    /**
     * Return the number of elements in this set.
     * @return the number of elements in this set
     */
    public int size() {

        return elements.size();
    }

    public boolean equals(Object rhs) {

        try {
            return equals((AttributeSet) rhs);
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    public boolean equals(AttributeSet rhs) {

        if (rhs == null) {
            return false;
        }

        return elements.equals(rhs.elements);
    }

    /**
     * Return true if this set contains the given Object
     * @return true if this set contains <code>o</code>
     */
    public boolean contains(Object o) {

        Object value = elements.get(o);
        return value != null;
    }

    /**
     * Return true if this set contains all elements in the given
     * Collection
     * @param coll the collection to compare with
     * @return true if this set contains all elements in the given
     * Collection
     */
    public boolean containsAll(Collection coll) {

        return elements.keySet().containsAll(coll);
    }

    /**
     * Return an Enumeration of the elements in this set.
     * @return an Enumeration of the elements in this set
     */
    public Enumeration elements() {

        return elements.keys();
    }

    /**
     * Return an Iterator with the elements in this set.
     * @return an Iterator with the elements in this set.
     * The Iterator cannot be used to modify this AttributeSet.
     */
    public Iterator iterator() {

        return new EnumerationIterator(elements.keys());
    }

    /**
     * Fill in the given array with the elements in this set.
     * @param storage an array to fill with this set's elements.
     * The array cannot be null.
     * @return the <tt>storage</tt> array.
     */
    public Object[] toArray(Object[] storage) {

        Enumeration keys = elements.keys();
        int n=0;
        while (keys.hasMoreElements()) {
            storage[n++] = keys.nextElement();
        }
        return storage;
    }

    /**
     * Return an array with the elements in this set.
     * @return an array with the elements in this set
     */
    public Object[] toArray() {

        return toArray(new Object[size()]);
    }
    
    /**
     * Throws UnsupportedOperationException.
     * @see #addElement
     * @throws UnsupportedOperationException
     */
    public boolean add(Object o){
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @throws UnsupportedOperationException
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @see #unionWith
     * @throws UnsupportedOperationException
     */
    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @see #subtract
     * @throws UnsupportedOperationException
     */
    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @see #intersectWith
     * @throws UnsupportedOperationException
     */
    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     * @see #EMPTY_SET
     * @throws UnsupportedOperationException
     */
    public void clear() {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Return an AttributeSet containing the elements of this set
     * and the given element
     * @param element the element to add
     * @return an AttributeSet like this one, with <code>element</code>
     * added
     */
    public AttributeSet addElement(Object element) {

        Hashtable newElements = (Hashtable) elements.clone();
        newElements.put(element, element);
        return new AttributeSet(newElements);
    }

    /**
     * Return an AttributeSet which is the union of
     * this set with the given set.
     * @param s the set to union with
     * @return an AttributeSet of the elements in this set or
     * in <code>s</code>
     */
    public AttributeSet unionWith(AttributeSet s) {

        Hashtable newElements = (Hashtable) elements.clone();

        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            newElements.put(next, next);
        }

        return new AttributeSet(newElements);
    }

    /**
     * Return an AttributeSet which is the intersection of
     * this set with the given set.
     * @param s the set to intersect with
     * @return an AttributeSet of the elements in this set which
     * are in <code>s</code>
     */
    public AttributeSet intersectWith(AttributeSet s) {

        Hashtable newElements = new Hashtable();

        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (elements.get(next) != null) {
                newElements.put(next, next);
            }
        }

        return new AttributeSet(newElements);
    }

    /**
     * Return an AttributeSet with the elements in this set which
     * are not in the given set.
     * @param s the set of elements to exclude
     * @return an AttributeSet of the elements in this set which
     * are not in <code>s</code>
     */
    public AttributeSet subtract(AttributeSet s) {

        Hashtable newElements = (Hashtable) elements.clone();

        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            newElements.remove(iter.next());
        }

        return new AttributeSet(newElements);
    }

    private static final class EnumerationIterator implements Iterator {

        private Enumeration e;

        EnumerationIterator(Enumeration e) {
            this.e = e;
        }

        public boolean hasNext() {
            return e.hasMoreElements();
        }

        public Object next() {
            return e.nextElement();
        }

        public void remove() {
            throw new UnsupportedOperationException(errString);
        }
    }
}