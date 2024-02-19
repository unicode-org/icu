// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/* Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. */

package com.ibm.icu.dev.scan;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CapDocument implements CapNode {

	public class TextCap extends CapElement {
		TextCap(String str) {
			super(str);
		}

		public void write(PrintWriter pw, int i) {
			writeIndent(pw,i);
			pw.write(getName()+"\n");
		}
	}

	public class CommentElement extends CapElement {
		CommentElement(String str) {
			super(str);
		}
		public void write(PrintWriter pw, int indent) {
			writeIndent(pw,indent);
			pw.write("<!-- "+getName()+" -->\n");
		}
	}

	TagElement root = new TagElement();
	
	public class TagElement extends CapElement {
		public TagElement() {
			super("");
		}
		public TagElement(String name) {
			super(name);
		}

		public void write(PrintWriter pw, int indent) {
			if(getName().length()>0) {
				writeIndent(pw,indent);
				pw.write("<"+getName());
				if(!attributes.isEmpty()) {
					for(Iterator iter = attributes.entrySet().iterator();iter.hasNext();) {
						Map.Entry o = (Map.Entry)iter.next();
						pw.write(" "+o.getKey().toString()+"=\""+o.getValue()+"\"");
					}
				}
				pw.write(">\n");
				indent++;
			}
			
			for(Iterator iter = children.iterator();iter.hasNext();) {
				CapNode ch = (CapNode)iter.next();
				ch.write(pw, indent);
			}
			
			if(getName().length()>0) {
				indent--;
				writeIndent(pw,indent);
				pw.write("</"+getName());
				pw.write(">\n");
			}
		}
	}

	private static void writeIndent(PrintWriter pw, int indent) {
		for(;indent>0;indent--) {
			pw.write(' ');
		}
	}

	public CapElement createCapElement(String name) {
		return new TagElement(name);
	}

	public void appendChild(CapElement base) {
		root.appendChild(base);
	}

	public static CapDocument newCapDocument() {
		return new CapDocument();
	}

	public static void printDOMTree(CapDocument out, PrintWriter pw,
			String string, Object object) {
		pw.write(string+"\n");
		out.root.write(pw,0);
	}
	
	public void write(PrintWriter pw, int indent) {
		root.write(pw,0);
	}

	public CapNode createTextNode(String trim) {
		return new TextCap(trim);
	}

	public CapElement createComment(String com) {
		return new CommentElement(com);
	}

	public int compareTo(Object arg0) {
		return getName().compareTo(((CapNode)arg0).getName());
	}
	
	public String getName() {
		return root.getName();
	}
}
