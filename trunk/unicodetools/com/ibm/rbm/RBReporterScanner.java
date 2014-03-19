/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;


import java.io.*;
import java.util.*;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * RBReporterScaner is a utility class for RBReporter. It creates a report from an xml settings
 * file that scans code for resources and compares them against a resource bundle.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBReporter
 */
public class RBReporterScanner {
	private Bundle       bundle;
	private Document     config;
	private Hashtable    fileRules;
	private Hashtable    parseRules;
	private Hashtable    results;
	private Hashtable    missing;
	private boolean      resultsFound;
	
	protected RBReporterScanner(Bundle bundle, File configFile) throws IOException {
		resultsFound = false;
		this.bundle = bundle;
		
		try {
			InputSource is = new InputSource(new FileInputStream(configFile));
			DOMParser parser = new DOMParser();
			parser.parse(is);
			config = parser.getDocument();
		} catch (SAXException saxe) {
			throw new IOException("Illegal XML Document: " + saxe.getMessage());
		}
		
		Element root = config.getDocumentElement();
		fileRules = getFileRules(root);
		parseRules = getParseRules(root);
		
		results = new Hashtable();
		Enumeration keys = bundle.allItems.keys();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			BundleItem item = (BundleItem)bundle.allItems.get(key);
			results.put(key, new ScanResult(item));
		}
		
		missing = new Hashtable();
	}
	
	protected int getNumberResourcesFound() {
		return results.size();
	}
	
	protected int getNumberMissingResources() {
		return missing.size();
	}
	
	protected int getNumberUnusedResources() {
		int count = 0;
		Enumeration elems = results.elements();
		while (elems.hasMoreElements()) {
			ScanResult result = (ScanResult)elems.nextElement();
			if (result.getOccurances().size() < 1) count++;
		}
		return count;
	}
	
	protected Vector getMissingResources() {
		Enumeration elems = missing.elements();
		Vector v = new Vector();
		while (elems.hasMoreElements())
			v.addElement(elems.nextElement());
		return v;
	}
	
	protected Vector getUnusedResources() {
		Enumeration elems = results.elements();
		Vector v = new Vector();
		while (elems.hasMoreElements()) {
			ScanResult result = (ScanResult)elems.nextElement();
			if (result.getOccurances().size() < 1) {
				v.addElement(result);
			}
		}
		return v;
	}
	
	protected boolean performScan() throws IOException {
		resultsFound = false;
		
		Element root = config.getDocumentElement();
		NodeList nl = root.getElementsByTagName("Scan");
		if (nl.getLength() < 1) return resultsFound;
		Element scan_elem = (Element)nl.item(0);
		nl = scan_elem.getElementsByTagName("Directory");
		for (int i=0; i < nl.getLength(); i++) {
			Element dir_elem = (Element)nl.item(i);
			File directory = new File(dir_elem.getAttribute("location"));
			boolean recurse = dir_elem.getAttribute("recurse_directories").equalsIgnoreCase("true");
			NodeList rules_list = dir_elem.getElementsByTagName("Rules");
			if (rules_list.getLength() < 1) continue;
			Element rules_elem = (Element)rules_list.item(0);
			NodeList frules_list = rules_elem.getElementsByTagName("ApplyFileRule");
			// For each file rule
			for (int j=0; j < frules_list.getLength(); j++) {
				Element frule_elem = (Element)frules_list.item(j);
				FileRule frule = (FileRule)fileRules.get(frule_elem.getAttribute("name"));
				if (frule == null) continue;
				NodeList prules_list = frule_elem.getElementsByTagName("ApplyParseRule");
				Vector prules_v = new Vector();
				// For each parse rule
				for (int k=0; k < prules_list.getLength(); k++) {
					Element prule_elem = (Element)prules_list.item(k);
					ParseRule prule = (ParseRule)parseRules.get(prule_elem.getAttribute("name"));
					if (prule == null) continue;
					prules_v.addElement(prule);
				}
				if (prules_v.size() < 1) continue;
				scanDirectory(directory, frule, prules_v, recurse);
			}
		}
		
		return resultsFound;
	}
	
	private void scanDirectory(File directory, FileRule frule, Vector prules, boolean recurse) throws IOException {
		
		// Recursion step
		if (recurse) {
			File children[] = directory.listFiles(new java.io.FileFilter(){
				public boolean accept(File f) {
					return f.isDirectory();
				}
				
				public String getDescription() {
					return "";
				}
			});
			for (int i=0; i < children.length; i++) {
				File new_directory = children[i];
				scanDirectory(new_directory, frule, prules, recurse);
			}
		}
		// Go through each acceptable file
		File children[] = directory.listFiles();
		for (int i=0; i < children.length; i++) {
			File f = children[i];
			if (f.isDirectory() || !(frule.applyRule(f.getName()))) continue;
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			int line_count = 0;
			// Read the file line by line
			while ((line = br.readLine()) != null) {
				line_count++;
				Vector findings = new Vector();
				// Apply all parse rules to each line
				for (int j=0; j < prules.size(); j++) {
					ParseRule prule = (ParseRule)prules.elementAt(j);
					Vector temp_results = prule.applyRule(line);
					for (int k=0; k < temp_results.size(); k++) {
						findings.addElement(temp_results.elementAt(k));
					}
				}
				for (int j=0; j < findings.size(); j++) {
					String name = (String)findings.elementAt(j);
					Occurance occ = new Occurance(f.getName(), f.getAbsolutePath(), line_count);
					// If the name is found in the resource bundles derived hashtable
					if (results.containsKey(name)) {
						ScanResult scan_res = (ScanResult)results.get(name);
						scan_res.addOccurance(occ);
					} else {
						// Add it to the missing results
						ScanResult scan_res = new ScanResult(new BundleItem(null, name, "*unknown*"));
						scan_res.addOccurance(occ);
						missing.put(name, scan_res);
						results.put(name, scan_res);
					}
				}
			}
		}
	}
	
	private Hashtable getFileRules(Element root) {
		Hashtable result = new Hashtable();
		NodeList frules_list = root.getElementsByTagName("FileRules");
		Element frules_elem = null;
		if (frules_list.getLength() > 0) frules_elem = (Element)frules_list.item(0);
		if (frules_elem == null) return result;
		frules_list = frules_elem.getElementsByTagName("FileRule");
		for (int i=0; i < frules_list.getLength(); i++) {
			Element elem = (Element)frules_list.item(i);
			FileRule frule = new FileRule(elem.getAttribute("name"), elem.getAttribute("starts_with"),
										  elem.getAttribute("ends_with"), elem.getAttribute("contains"));
			result.put(elem.getAttribute("name"), frule);
		}
		return result;
	}
	
	private Hashtable getParseRules(Element root) {
		Hashtable result = new Hashtable();
		NodeList prules_list = root.getElementsByTagName("ParseRules");
		Element prules_elem = null;
		if (prules_list.getLength() > 0)
			prules_elem = (Element)prules_list.item(0);
		if (prules_elem == null)
			return result;
		prules_list = prules_elem.getElementsByTagName("ParseRule");
		for (int i=0; i < prules_list.getLength(); i++) {
			Element elem = (Element)prules_list.item(i);
			ParseRule prule = new ParseRule(elem.getAttribute("name"), elem.getAttribute("follows"),
											elem.getAttribute("precedes"));
			result.put(elem.getAttribute("name"), prule);
		}
		return result;
	}
}

class FileRule {
	String name;
	String starts_with;
	String ends_with;
	String contains;
	
	FileRule(String name, String starts_with, String ends_with, String contains) {
		this.name = name;
		this.starts_with = starts_with;
		this.ends_with = ends_with;
		this.contains = contains;
	}
	
	boolean applyRule(String source) {
		boolean accept = true;
		if (starts_with != null && starts_with.length() > 0 && !(source.startsWith(starts_with))) accept = false;
		if (ends_with != null && ends_with.length() > 0 && !(source.endsWith(ends_with))) accept = false;
		if (contains != null && contains.length() > 0 && source.indexOf(contains) < 0) accept = false;
		return accept;
	}
}

class ParseRule {
	String name;
	String before;
	String after;
	
	ParseRule(String name, String before, String after) {
		this.name = name;
		this.before = before;
		this.after = after;
	}
	
	// returns the vector of strings found after before and before after
	
	Vector applyRule(String source) {
		Vector v = new Vector();
		if (before != null && before.length() > 0) {
			if (after != null && after.length() > 0) {
				// Both before and after non-empty
				int before_index = -1;
				int after_index = -1;
				while ((before_index = source.indexOf(before, ++before_index)) >= 0) {
					//before_index = source.indexOf(before, before_index);
					after_index = -1;
					after_index = source.indexOf(after, before_index + before.length()+1);
					if (after_index < 0 || before_index < 0 || before.length() < 0) {
					    break;
					}
					v.addElement(source.substring(before_index + before.length(), after_index));
					before_index = after_index;
				}
			} else {
				// Before non-empty, after empty
				int index = -1;
				while (source.indexOf(before, ++index) >= 0) {
					index = source.indexOf(before, index);
					String result = source.substring(index + before.length(), source.length());
					if (result != null && result.length() > 0) v.addElement(result);
				}
			}
		} else if (after != null && after.length() > 0) {
			// Before empty, after not
			int index = -1;
			while (source.indexOf(after, ++index) >= 0) {
				index = source.indexOf(before, index);
				String result = source.substring(0, index);
				if (result != null && result.length() > 0) v.addElement(result);
			}
		} else {
			// Before and after empty
			v.addElement(source);
		}
		return v;
	}
}