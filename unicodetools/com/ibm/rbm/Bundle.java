/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;


import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.*;

import com.ibm.rbm.gui.RBManagerGUI;

/**
 * A class representing the entire Bundle of Resources for a particular language, country, variant.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class Bundle {
    
    /**
     * The following public class variables reflect the various properties that can be included as
     * meta-data in a resource bundle formatted by RBManager
     */
    public String name;
    /**
     * The encoding of the bundle (e.g. 'en', 'en_US', 'de', etc.)
     */
    public String encoding;
    /**
     * A descriptor of the language in the encoding (e.g. English, German, etc.)
     */
    public String language;
    /**
     * A descriptor of the country in the encoding (e.g. US, Canada, Great Britain)
     */
    public String country;
    /**
     * The descriptor of the variant in the encoding (e.g. Euro, Irish, etc.)
     */
    public String variant;
    /**
     * A comment concerning the bundle
     */
    public String comment;
    /**
     * The name of the person responsible for the managerment of this bundle
     */
    public String manager;
    
    private TreeSet groups;                                 // A vector of groups of NLS items, the key is the group name
    
    /**
     * A hashtable of all of the items in the bundle, hashed according to their
     * NLS key.
     */
    
    public Hashtable allItems;                              // A hashtable of all items in the file, the key is the NLS key
    
    private TreeSet untranslatedItems;                      // A vector of all items which are untranslated
    
    /**
     * A vector containing all of the items which are duplicates (based on the NLS keys)
     * of items previously declared in the bundle.
     */
    
    public Vector duplicates;                               // A vector of items which are duplicates (NLS Keys) of previous items
		
    /**
     * Constructor for creating an empty bundle with a given encoding
     */
    
    public Bundle(String encoding) {
        this.encoding = encoding;
        language = null;
        country  = null;
        variant  = null;
        comment  = null;
        manager  = null;
        groups = new TreeSet(new Comparator() {
            public boolean equals(Object o) { return false; }
            
            public int compare(Object o1, Object o2) {
                if (!(o1 instanceof BundleGroup) || !(o2 instanceof BundleGroup))
                	return 0;
                BundleGroup g1 = (BundleGroup)o1;
                BundleGroup g2 = (BundleGroup)o2;
                return g1.getName().compareTo(g2.getName());
            }
        });
            
        untranslatedItems = new TreeSet(new Comparator() {
            public boolean equals(Object o) { return false; }
            
            public int compare(Object o1, Object o2) {
                if (!(o1 instanceof BundleItem) || !(o2 instanceof BundleItem)) return 0;
                BundleItem i1 = (BundleItem)o1;
                BundleItem i2 = (BundleItem)o2;
                return i1.getKey().compareTo(i2.getKey());
            }
        });
        
        duplicates = new Vector();
        allItems = new Hashtable();
    }
		
    /**
     * Encodings are of the form -> language_country_variant <- (for example: "en_us_southern").
     * This method returns the language encoding string, or null if it is not specified
     */
		
    public String getLanguageEncoding() {
        if (encoding == null)
            return null;
        if (encoding.indexOf("_") >= 0)
            return encoding.substring(0,encoding.indexOf("_"));
        return encoding.trim();
    }
		
    /**
     * Encodings are of the form -> language_country_variant <- (for example: "en_us_southern").
     * This method returns the country encoding string, or null if it is not specified
     */
		
    public String getCountryEncoding() {
        if (encoding == null || encoding.indexOf("_") < 0)
            return null;
        // Strip off the language
        String workStr = encoding.substring(encoding.indexOf("_")+1,encoding.length());
        if (workStr.indexOf("_") >= 0)
            return workStr.substring(0,encoding.indexOf("_"));
        return workStr.trim();
    }
		
    /**
     * Encodings are of the form -> language_country_variant <- (for example: "en_us_southern").
     * This method returns the variant encoding string, or null if it is not specified
     */
		
    public String getVariantEncoding() {
        if (encoding == null || encoding.indexOf("_") < 0)
        	return null;
        // Strip off the language
        String workStr = encoding.substring(encoding.indexOf("_")+1,encoding.length());
        if (workStr == null || workStr.length() < 1 || workStr.indexOf("_") < 0)
        	return null;
        // Strip off the country
        workStr = workStr.substring(encoding.indexOf("_")+1, workStr.length());
        return workStr.trim();
    }
    
    /**
     * Returns the UntranslatedItems as a vector. I should find where this happens and stop it.
     */
		
    public Vector getUntranslatedItemsAsVector() {
        Iterator iter = untranslatedItems.iterator();
        Vector v = new Vector();
        while (iter.hasNext())
        	v.addElement(iter.next());
        return v;
    }
		
    /**
     * Checks all items in the untranslated items set. If they belong to a group whose name
     * matches the passed in name, then they are removed.
     */
		
    public void removeUntranslatedItemsByGroup(String groupName) {
        Iterator iter = untranslatedItems.iterator();
        try {
            while(iter.hasNext()) {
                BundleItem item = null;
                item = (BundleItem)iter.next();
                if (item != null && item.getParentGroup().getName().equals(groupName)) {
                    removeUntranslatedItem(item.getKey());
                }
            }
        } catch (Exception e) {
            RBManagerGUI.debugMsg(e.getMessage());
        }
    }
		
    /**
     * Checks to see if an item of the given key name exists in the set of untranslated items. If
     * it does exist, then it is removed.
     */
		
    public void removeUntranslatedItem(String name) {
        Iterator iter = untranslatedItems.iterator();
        while (iter.hasNext()) {
            BundleItem item = (BundleItem)iter.next();
            if (item.getKey().equals(name)) {
                untranslatedItems.remove(item);
                break;
            }
        }
    }
		
    /**
     * Returns the boolean of wether a group of a given name exists in the bundle
     */
		
    public boolean hasGroup(String groupName) {
        Iterator iter = groups.iterator();
        while (iter.hasNext()) {
            BundleGroup group = (BundleGroup)iter.next();
            if (group.getName().equals(groupName))
            	return true;
        }
        return false;
    }
    
    /**
     * Creates a group of the given name and optionally associates a comment with
     * that group.
     */
    
    public void addBundleGroup(String groupName, String groupComment) {
        BundleGroup bg = new BundleGroup(this, groupName);
        bg.setComment(groupComment);
        addBundleGroup(bg);
    }
		
    /**
     * Removes the group of the given name if it exists in the bundle
     */
		
    public void removeGroup(String groupName) {
        Iterator iter = groups.iterator();
        while (iter.hasNext()) {
            BundleGroup tempGroup = (BundleGroup)iter.next();
            if (tempGroup.getName().equals(groupName)) {
                groups.remove(tempGroup);
                break;
            }
        }
        // Remove the items from the untanslated items
        removeUntranslatedItemsByGroup(groupName);
			
        // Loop through all Items
        Enumeration elems = allItems.elements();
        while(elems.hasMoreElements()) {
            BundleItem item = (BundleItem)elems.nextElement();
            if (item.getParentGroup().getName().equals(groupName)) {
                allItems.remove(item);
            }
        }
    }
		
    /**
     * Removes a single resource item from the bundle
     */
		
    public void removeItem(String key) {
        Object o = allItems.get(key);
        if (o != null) {
            BundleItem item = (BundleItem)o;
            // Remove from allItems Hashtable
            allItems.remove(key);
            // Remove from item's group
            if (item.getParentGroup() != null) {
                BundleGroup group = item.getParentGroup();
                group.removeBundleItem(key);
            }
            // Remove from untranslatedItems Hashtable
            removeUntranslatedItem(key);
        }
    }
		
    /**
     * Attempts to add a BundleItem to the untranslatedItems. The addition will fail in two cases: One, if
     * the item does not all ready belong to this Bundle, and Two, if the item is all ready in the set of
     * untranslated items.
     */
		
    public void addUntranslatedItem(BundleItem item) {
        if (item.getParentGroup().getParentBundle() != this)
        	return;
        // Remove it if it exists.
        if (untranslatedItems.contains(item)) {
        	untranslatedItems.remove(item);
        }
    	untranslatedItems.add(item);
    }
		
    /**
     * Returns the number of items currently marked as untranslated
     */
		
    public int getUntranslatedItemsSize() {
        return untranslatedItems.size();
    }
		
    /**
     * Returns the indexth untranslated item
     */
		
    public BundleItem getUntranslatedItem(int index) {
        if (index >= untranslatedItems.size())
        	return null;
        Iterator iter = untranslatedItems.iterator();
        for (int i=0; i < index; i++)
        	iter.next();
        return (BundleItem)iter.next();
    }
    
    /**
     * Return the various resource bundle groups stored in a Vector collection.
     */
    
    public Vector getGroupsAsVector() {
        Vector v = new Vector();
        Iterator iter = groups.iterator();
        while (iter.hasNext()) {
            BundleGroup group = (BundleGroup)iter.next();
            v.addElement(group);
        }
        return v;
    }
    
    /**
     * Returns the number of groups in the bundle.
     */
    
    public int getGroupCount() {
        return groups.size();
    }
    
    /**
     * Returns a bundle group given a certain index.
     */
    
    public BundleGroup getBundleGroup(int index) {
        if (index >= getGroupCount())
        	return null;
        Iterator iter = groups.iterator();
        for (int i=0; i < index; i++)
        	iter.next();
        return (BundleGroup)iter.next();
    }

    /**
     * Looks for a bundle group of a given name within a bundle and
     * returns it if found.
     */
		
    public BundleGroup getBundleGroup(String groupName) {
        Iterator iter = groups.iterator();
        while(iter.hasNext()) {
            BundleGroup group = (BundleGroup)iter.next();
            if (group.getName().equals(groupName))
            	return group;
        }
        return null;
    }
    
    /**
     * Looks up and returns a bundle item stored in the bundle based on its
     * NLS lookup key.
     */
    
    public BundleItem getBundleItem(String key) {
        return (BundleItem)allItems.get(key);
    }
                    
    /**
     * One group is created for all bundles called 'Ungrouped Items'. This is the bundle
     * group in which bundle items are placed that are not specifically grouped in the
     * resource bundle file. This method returns that bundle group.
     */
    
    public BundleGroup getUngroupedGroup() {
        return getBundleGroup("Ungrouped Items");
    }
		
    /**
     * Add a bundle group to the bundle
     */
     
    public void addBundleGroup(BundleGroup bg) {
        groups.add(bg);
    }

    /**
     * Add a bundle item to the bundle. This bundle item should all ready have its
     * bundle group assigned.
     */
		
    public void addBundleItem(BundleItem item) {
        if (allItems.containsKey(item.getKey())) {
            duplicates.addElement(item);
        } else {
            if (!(groups.contains(item.getParentGroup())))
            	addBundleGroup(item.getParentGroup());
            item.getParentGroup().addBundleItem(item);
            allItems.put(item.getKey(), item);
            removeUntranslatedItem(item.getKey());
            if (!item.isTranslated())
            	addUntranslatedItem(item);
        }
    }
    
    /**
     * A method useful in debugging. The string returned displays the encoding
     * information about the bundle and wether or not it is the base class of
     * a resource bundle.
     */
    
    public String toString() {
        String retStr = new String();
        if (language != null && !language.equals("")) retStr = language;
        if (country != null && !country.equals("")) retStr += ", " + country;
        if (variant != null && !variant.equals("")) retStr += ", " + variant;
			
        retStr += " (" + (encoding == null || encoding.equals("") ? "Base Class" : encoding) + ")";
        return retStr;
    }
    
    /**
     * This method produces a String which is suitable for inclusion in a .properties
     * style resource bundle. It attaches (in comments) the meta data that RBManager
     * reads to manage the resource bundle file. This portion of the output should
     * be included at the beginning of the resource bundle file.
     */
    
    public String toOutputString() {
        String                retStr  = "# @file          " + name     + "\n";
        if (encoding != null) retStr += "# @fileEncoding  " + encoding + "\n";
        if (language != null) retStr += "# @fileLanguage  " + language + "\n";
        if (country  != null) retStr += "# @fileCountry   " + country  + "\n";
        if (variant  != null) retStr += "# @fileVariant   " + variant  + "\n";
        if (manager  != null) retStr += "# @fileManager   " + manager  + "\n";
        if (comment  != null) retStr += "# @fileComment   " + comment  + "\n";
        return retStr;
    }

    /**
     * A helping method for outputting the formatted contents of the bundle to a
     * print stream. The method first outputs the header information and then outputs
     * each bundle group's formatted data which includes each bundle item.
     */
		
    public void writeContents(PrintStream ps) {
        ps.println(this.toOutputString());
        Iterator iter = groups.iterator();
        while (iter.hasNext()) {
            ((BundleGroup)iter.next()).writeContents(ps);
        }
    }

    /**
     * A helping method for outputting the formatted contents of the bundle to a
     * ouput Writer (such as a FileWriter). The method first outputs the header
     * information and then outputs each bundle group's formatted data which includes
     * each bundle item.
     */
		
    public void writeContents(Writer w) throws IOException {
        w.write(this.toOutputString() + "\n");
        Iterator iter = groups.iterator();
        while (iter.hasNext()) {
            ((BundleGroup)iter.next()).writeContents(w);
        }
    }
}