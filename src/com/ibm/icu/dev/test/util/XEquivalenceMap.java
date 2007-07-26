/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Everything that maps to the same value is part of the same equivalence class
 * @author davis
 *
 */
public class XEquivalenceMap {
	HashMap source_target = new HashMap();
	HashMap target_sourceSet = new HashMap();
	HashMap source_Set = new HashMap();
	public XEquivalenceMap clear() {
		source_target.clear();
		target_sourceSet.clear();
		source_Set.clear();
		return this;
	}
	public XEquivalenceMap add(Object source, Object target) {
		Object otherTarget = source_target.get(source);
		if (otherTarget != null) {
			if (otherTarget.equals(target)) return this;
			throw new IllegalArgumentException("Same source mapping to different targets: "
					+ source + " => " + otherTarget + " & " + target);
		}
		source_target.put(source, target);
		Set s = (Set) target_sourceSet.get(target);
		if (s == null) target_sourceSet.put(target, s = new HashSet());
		s.add(source);
		source_Set.put(source, s);
		return this;
	}
	public Set getEquivalences (Object source) {
		Set s = (Set) source_Set.get(source);
		if (s == null) return null;
		return Collections.unmodifiableSet(s);
	}
	public boolean areEquivalent (Object source1, Object source2) {
		Set s = (Set) source_Set.get(source1);
		if (s == null) return false;
		return s.contains(source2);
	}
	public Object getTarget(Object source) {
		return source_target.get(source);
	}
	public Set getSources(Object target) {
		Set s = (Set) target_sourceSet.get(target);
		return Collections.unmodifiableSet(s);
	}
	public Iterator iterator() {
		MyIterator result = new MyIterator();
		result.target_sourceSet_iterator = target_sourceSet.keySet().iterator();
		return result;
	}
	public int size() {
		return target_sourceSet.size();
	}
	private class MyIterator implements Iterator {
		private Iterator target_sourceSet_iterator;
		public void remove() {
			throw new UnsupportedOperationException();
		}
		public boolean hasNext() {
			return target_sourceSet_iterator.hasNext();
		}
		public Object next() {
			return getSources(target_sourceSet_iterator.next());
		}		
	}
}