/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.List;
import java.util.TreeSet;

/**
 * A list with unique items. It does not permit multiple items to be added, and does not support (at
 * least for now) adding elements at a position. (Support may be added later). Also should add support
 * for Equator.
 * @author davis
 */
public class ListSet implements Set, List {
	List list = new ArrayList();
	Set set;
	Comparator comparator;
	
	ListSet(Comparator comparator) {
		this.comparator = comparator;
		set = new TreeSet(comparator);
	}
	/**
	 * @param index
	 * @param element
	 */
	public void add(int index, Object element) {
		throw new UnsupportedOperationException();
	}
	/**
	 * @param o
	 * @return
	 */
	public boolean add(Object o) {
		boolean result = set.add(o);
		if (result) list.add(o);
		return result;
	}
	/**
	 * @param index
	 * @param c
	 * @return
	 */
	public boolean addAll(int index, Collection c) {
		throw new UnsupportedOperationException();
	}
	/**
	 * @param c
	 * @return
	 */
	public boolean addAll(Collection c) {
		// TODO optimize
		boolean result = false;
		for (Iterator it = c.iterator(); it.hasNext();) {
			result = result || add(it.next());
		}
		return result;
	}
	/**
	 * 
	 */
	public void clear() {
		list.clear();
	}
	/**
	 * @param o
	 * @return
	 */
	public boolean contains(Object o) {
		return set.contains(o);
	}
	/**
	 * @param c
	 * @return
	 */
	public boolean containsAll(Collection c) {
		return set.containsAll(c);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		try {
			ListSet other = (ListSet) obj;
			return list.equals(other.list) && set.equals(other.set);
		} catch (ClassCastException e) {
			return false;
		}
	}
	/**
	 * @param index
	 * @return
	 */
	public Object get(int index) {
		return list.get(index);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return list.hashCode();
	}
	/**
	 * @param o
	 * @return
	 */
	public int indexOf(Object o) {
		for (int i = 0; i < list.size(); ++i) {
			if (0 == comparator.compare(list.get(i), o)) return i;
		}
		return -1;
	}
	/**
	 * @return
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}
	/**
	 * @return
	 */
	public Iterator iterator() {
		return list.iterator();
	}
	/**
	 * @param o
	 * @return
	 */
	public int lastIndexOf(Object o) {
		for (int i = list.size()-1; i >= 0 ; --i) {
			if (0 == comparator.compare(list.get(i), o)) return i;
		}
		return -1;
	}
	/**
	 * @return
	 */
	public ListIterator listIterator() {
		return list.listIterator();
	}
	/**
	 * @param index
	 * @return
	 */
	public ListIterator listIterator(int index) {
		return list.listIterator(index);
	}
	/**
	 * @param index
	 * @return
	 */
	public Object remove(int index) {
		throw new UnsupportedOperationException();
	}
	/**
	 * @param o
	 * @return
	 */
	public boolean remove(Object o) {
		boolean result = set.remove(o);
		if (!result) return false;
		return matchListToSet();
	}
	/**
	 * @param c
	 * @return
	 */
	public boolean removeAll(Collection c) {
		boolean result = set.removeAll(c);
		if (!result) return false;
		return matchListToSet();

	}
	/**
	 * @param c
	 * @return
	 */
	public boolean retainAll(Collection c) {
		boolean result = set.retainAll(c);
		if (!result) return false;
		return matchListToSet();
	}
	/**
	 * @return
	 */
	private boolean matchListToSet() {
		for (Iterator it = list.iterator(); it.hasNext();) {
			Object o = it.next();
			if (!set.contains(o)) it.remove();
		}
		return true;
	}
	/**
	 * @param index
	 * @param element
	 * @return
	 */
	public Object set(int index, Object element) {
		throw new UnsupportedOperationException();
	}
	/**
	 * @return
	 */
	public int size() {
		return list.size();
	}
	/**
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	public List subList(int fromIndex, int toIndex) {
		ListSet result = new ListSet(comparator);
		result.add(list.subList(fromIndex, toIndex));
		return result;
	}
	/**
	 * @return
	 */
	public Object[] toArray() {
		return list.toArray();
	}
	/**
	 * @param a
	 * @return
	 */
	public Object[] toArray(Object[] a) {
		return list.toArray(a);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return list.toString();
	}
}