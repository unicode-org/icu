// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/* Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. */

package com.ibm.icu.dev.scan;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public abstract class CapElement implements CapNode {
	String com;
	CapElement(String str) {
		com = str;
	}

	public String getName() {
		return com;
	}
	
	public abstract void write(PrintWriter pw, int i);

	public int compareTo(Object arg0) {
		if(arg0 == this) return 0;
		int str = getName().compareTo(((CapNode)arg0).getName());
		if(str!=0) return str;
		
		if(arg0 instanceof CapElement) {		
			CapElement oth = (CapElement)arg0;
			return attributes.toString().compareTo(oth.attributes.toString());
		}
		return 0;
	}

	Map attributes = new HashMap();
	Set children = new TreeSet();
	public void setAttribute(String k, String v) {
		attributes.put(k, v);
		
	}

	public void appendChild(CapNode e) {
		children.add(e);
		
	}

	public String getNodeName() {
		return getName();
	}

}
