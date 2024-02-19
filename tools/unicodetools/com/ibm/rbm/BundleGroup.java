/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.*;

/**
 * A class representing a group of BundleItems and the meta data associated with that group
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class BundleGroup {
    private String name;                        // The name of the group
    private String comment;                     // A comment describing this group
    private TreeSet items;                      // The NLS items contained in this group
    private Bundle bundle;                      // The parent Bundle object of this group
		
    /**
     * Basic data constructor.
     * Creates a BundleGroup with a parent bundle and a given name.
     */
    public BundleGroup(Bundle parent, String name) {
        bundle = parent;
        this.name = name;
        comment = null;
        items = new TreeSet(new Comparator(){
            public boolean equals(Object o) { return false; }
            public int compare(Object o1, Object o2) {
                if (!(o1 instanceof BundleItem) || !(o2 instanceof BundleItem))
                	return 0;
                BundleItem i1 = (BundleItem)o1;
                BundleItem i2 = (BundleItem)o2;
                return i1.getKey().compareTo(i2.getKey());
            }
        });
    }

    /**
     * Two bundle groups are considered equal iff their names are the same.
     */
    public boolean equals(Object o) {
        return (o instanceof BundleGroup && ((BundleGroup)o).getName().equals(name));
    }
	
    // This should be changed anywhere it is used
	
    public Vector getItemsAsVector() {
        Vector v = new Vector();
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            v.addElement(iter.next());
        }
        return v;
    }
	
    /**
     * Adds a BundleItem to the group as long as that item is not currently in the group.
     * If the item.group is not equal to this group, then it is changed to be this group.
     * This method should, in most cases, only be called from the Bundle class.
     */
    public void addBundleItem(BundleItem item) {
    	if (items.contains(item)) {
    		items.remove(item);
    	}
        item.setParentGroup(this);
        items.add(item);
    }
	
    /**
     * Remove an item of the given name from the group
     */
    public void removeBundleItem(String itemName) {
        Iterator iter = items.iterator();
        while(iter.hasNext()) {
            BundleItem item = (BundleItem)iter.next();
            if (item.getKey().equals(itemName)) {
                items.remove(item);
                break;
            }
        }
    }
	
    /**
     * Returns the number of items stored in the group
     */
    public int getItemCount() {
        return items.size();
    }
	
    /**
     * Returns a BundleItem from the set of items at a particular index point.
     * If the index is greater than or equal to the number of items in the set,
     * null is returned.
     */
    public BundleItem getBundleItem(int index) {
        if (index >= items.size())
        	return null;
        Iterator iter = items.iterator();
        for (int i=0; i < index; i++)
        	iter.next();
        return (BundleItem)iter.next();
    }
    
    /**
     * Returns the bundle to which this group belongs
     */
    public Bundle getParentBundle() {
        return bundle;
    }
    
    /**
     * Returns the comment associated with this bundle
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * Returns the name of the bundle
     */
    public String getName() {
        return name;
    }
	
    protected void setParentBundle(Bundle bundle) {
        this.bundle = bundle;
    }
	
    public void setComment(String comment) {
        this.comment = comment;
    }
	
    public void setName(String name) {
        this.name = name;
    }
	
    /**
     * The translation to a string returns the name of the group
     */
    public String toString() {
        return name;
    }
	
    /**
     * Returns the output for a group heading.
     * This will be found in comment lines above the group items
     */
	public String toOutputString() {
        String retStr = "\n#\n# @group " + name + "\n#\n";
        if (comment != null)
        	retStr += "# @groupComment " + comment + "\n";
        return retStr;
    }
	
    /**
     * Writes the output contents to a particular PrintStream.
     * The output will be suitable for a properly formatted .properties file.
     */
    public void writeContents(PrintStream ps) {
        if (!name.equals("Ungrouped Items"))
        	ps.println(this.toOutputString());
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            ((BundleItem) iter.next()).writeContents(ps);	
        }
    }
	
    /**
     * Writes the output contents to a particular Writer.
     * The output will be suitable for a properly formatted .properties file.
     */
    public void writeContents(Writer w) throws IOException {
        if (!name.equals("Ungrouped Items"))
        	w.write(this.toOutputString() + "\n");
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            ((BundleItem) iter.next()).writeContents(w);
        }
    }
}