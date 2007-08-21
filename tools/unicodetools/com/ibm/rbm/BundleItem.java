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
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * A class representing a single translation item and all of the meta-data associated with that translation
 * 
 * @author Jared Jackson - Email: <a href="mailto:jjared@almaden.ibm.com">jjared@almaden.ibm.com</a>
 * @see com.ibm.rbm.RBManager
 */
public class BundleItem {
    private String name;                                   // The name of the NLS item key
    private String value;                                  // The translation of the key item
    private String comment;                                // A comment about this item
    private boolean translated;                            // Has this item been translated?
    private Date created;                                  // The date of creation of the item
    private Date modified;                                 // The last modification date of the item
    private String creator;                                // The name of the person who created the item
    private String modifier;                               // The name of the person who last modified the item
    private Hashtable lookups;                             // A hastable of lookups for the item (i.e. ({#}, Meaning) pairs)
    private BundleGroup group;                             // The parent group of the item
		
    /**
     * Basic data constructor for a resource bundle item.
     * @param parent The BundleGroup to which the item belongs. This group will have its own Bundle parent.
     * @param name The NLS lookup key common across all bundle files in the resource bundle
     * @param value The translated value of the item appropriate for the encoding of the bundle file to which the item belongs
     */
     
    public BundleItem(BundleGroup parent, String name, String value) {
        this.name = name;
        this.value = value;
        this.group = parent;
        comment = null;
        translated = false;
        created = new Date();                               // Defaults to the system's current date
        modified = new Date();                              // Defaults to the system's current date
        creator = null;
        modifier = null;
        lookups = new Hashtable();
    }

    /**
     * Returns the BundleGroup to which this item belongs
     */
	
    public BundleGroup getParentGroup() {
        return group;
    }

    /**
     * Returns the date this item was last modified.
     */
	
    public Date getModifiedDate() {
        return modified;
    }
    
    /**
     * Returns the date the item was first created.
     */
	
    public Date getCreatedDate() {
        return created;
    }
    
    /**
     * Returns the login name of the user that created the item.
     */
	
    public String getCreator() {
        return creator;
    }
    
    /**
     * Returns the login name of the user that last modified the item.
     */
    
    public String getModifier() {
        return modifier;
    }
    
    /**
     * Returns the NLS lookup key for the item.
     */
	
    public String getKey() {
        return name;
    }

    /**
     * Returns the translation value for the item.
     */
	
    public String getTranslation() {
        return value;
    }
    
    /**
     * Returns a comment associated with the item.
     */
    
    public String getComment() {
        return comment;
    }
    
    /**
     * Has the item yet been translated, or was it merely derived from a previous
     * bundle file?
     */
	
    public boolean isTranslated() {
        return translated;
    }
    
    /**
     * Returns a hashtable of the various lookups associated with the item. Lookups are
     * context sensitive information stored within the resource item and have their own
     * meta-data associated with themselves.
     */
	
    public Hashtable getLookups() {
        return lookups;
    }

    /**
     * Sets the translated value of the item. A true mark indicates that the item has
     * been examined or modified and is ready for use in the encoding specified by the
     * parent Bundle.
     */
	
    public void setTranslated(boolean isTranslated) {
        if (translated == isTranslated) return;
        translated = isTranslated;
        if (this.getParentGroup() != null && this.getParentGroup().getParentBundle() != null) {
            Bundle bundle = this.getParentGroup().getParentBundle();
            if (isTranslated) bundle.removeUntranslatedItem(this.name);
            else bundle.addUntranslatedItem(this);
        }
    }
    
    /**
     * Sets the comment associated with this item.
     */
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    /**
     * Given a hashtable of lookups, associates those lookups with this item.
     */
	
    public void setLookups(Hashtable lookups) {
        this.lookups = lookups;
    }
    
    /**
     * Sets the NLS key associated with this item. Be careful using this method, as
     * it does not change the lookup value of any other items in the resource bundle.
     * This must be done at a higher level.
     */
    
    public void setKey(String keyName) {
        name = keyName;
    }
    
    /**
     * Sets the translation value of the item.
     */
    
    public void setTranslation(String translationValue) {
        value = translationValue;
    }
    
    /**
     * Sets the parent BundleGroup of the item.
     */
    
    public void setParentGroup(BundleGroup group) {
        this.group = group;
    }
    
    /**
     * Associates a login name of the creator of the item with the item.
     */
	
    public void setCreator(String name) {
        creator = name;
    }

    /**
     * Associates a login name of the last modifier of the item with the item.
     */
	
    public void setModifier(String name) {
        modifier = name;
    }

    /**
     * Sets the created date of the item given a date formatted string.
     * The format can be either 'YYYY-MM-DD' (e.g. 20002-02-05) or
     * the format can be 'YYYMMDDTHHMMSSZ' (e.g. 20020205T103000Z)
     */
    
    public void setCreatedDate(String dateStr) {
        if (dateStr != null) created = parseDateFromString(dateStr);
    }
    
    /**
     * Sets the created date of the item.
     */
    
    public void setCreatedDate(Date date) {
        created = date;
    }
    
    /**
     * Sets the last modififcation date of the item given a date formatted string.
     * The format can be either 'YYYY-MM-DD' (e.g. 2002-02-05) or
     * the format can be 'YYYMMDDTHHMMSSZ' (e.g. 20020205T103000Z)
     */
    
    public void setModifiedDate(String dateStr) {
        if (dateStr != null)
        	modified = parseDateFromString(dateStr);
    }
    
    /**
     * Sets the last modification date of the item.
     */
    
    public void setModifiedDate(Date date) {
        modified = date;
    }
    
    /**
     * Simply returns the lookup name of the item.
     */
    
    public String toString() {
        return name;
    }
    
    /**
     * Returns the formatted output of this bundle item as it would be included in a .properties
     * formatted resource bundle file. This format also contains the meta-data used by RBManager in
     * the form of parseable comments.
     */

    public String toOutputString() {
        String retStr = (translated ? "# @translated true" : "# @translated false");
        if (created != null) {
            GregorianCalendar createdCal = new GregorianCalendar();
            createdCal.setTime(created);
            int year = createdCal.get(Calendar.YEAR);
            int month = createdCal.get(Calendar.MONTH)+1;
            int day = createdCal.get(Calendar.DAY_OF_MONTH);
            retStr += " @created " + String.valueOf(year) + "-"
                + (month > 9 ? String.valueOf(month) : "0" + String.valueOf(month)) + "-"
                + (day > 9 ? String.valueOf(day) : "0" + String.valueOf(day));
        }
        if (modified != null) {
            GregorianCalendar modifiedCal = new GregorianCalendar();
            modifiedCal.setTime(modified);
            int year = modifiedCal.get(Calendar.YEAR);
            int month = modifiedCal.get(Calendar.MONTH)+1;
            int day = modifiedCal.get(Calendar.DAY_OF_MONTH);
            retStr += " @modified " + String.valueOf(year) + "-"
                + (month > 9 ? String.valueOf(month) : "0" + String.valueOf(month)) + "-"
                + (day > 9 ? String.valueOf(day) : "0" + String.valueOf(day));
        }
        if (creator != null) retStr += " @creator " + creator;
        if (modifier != null) retStr += " @modifier " + modifier;
        Enumeration elems = lookups.keys();
        while (elems.hasMoreElements()) {
            String str = (String)elems.nextElement();
            retStr += "\n# @{" + str + "} " + (String)lookups.get(str);
        }
        if (comment != null) retStr += "\n# @comment " + comment;
		
        retStr += "\n" + name + "=" + saveConvert(value);
        return retStr;
    }
    
    /**
     * Writes the formatted contents to a PrintStream.
     */
    
    public void writeContents(PrintStream ps) {
        ps.println(this.toOutputString());
    }
	
    /**
     * Writes the formatted contents to a writer such as a FileWriter.
     */

    public void writeContents(Writer w) throws IOException {
        w.write(this.toOutputString() + "\n");
    }
		
    /*
     * Converts unicodes to encoded \\uxxxx
     * and writes out any of the characters in specialSaveChars
     * with a preceding slash
     */
    // Taken from java.util.Properties
    private String saveConvert(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len*2);

        for(int x=0; x<len; ) {
            aChar = theString.charAt(x++);
            switch(aChar) {
                case '\\':outBuffer.append('\\'); outBuffer.append('\\');
                          continue;
                case '\t':outBuffer.append('\\'); outBuffer.append('t');
                          continue;
                case '\n':outBuffer.append('\\'); outBuffer.append('n');
                          continue;
                case '\r':outBuffer.append('\\'); outBuffer.append('r');
                          continue;
                case '\f':outBuffer.append('\\'); outBuffer.append('f');
                          continue;
                default:
                    if ((aChar < 20) || (aChar > 127)) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >> 8) & 0xF));
                        outBuffer.append(toHex((aChar >> 4) & 0xF));
                        outBuffer.append(toHex((aChar >> 0) & 0xF));
                    }
                    else {
                        if (specialSaveChars.indexOf(aChar) != -1)
                            outBuffer.append('\\');
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }
	
    /**
     * Convert a nibble to a hex character
     * @param	nibble	the nibble to convert.
     */
	// Taken from java.util.Properties
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }
	
    /** A table of hex digits */
    // Taken from java.util.Properties
    private static final char[] hexDigit = {
	'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
	
    // Taken from java.util.Properties
    private static final String specialSaveChars = "=: \t\r\n\f#!";
	
    private Date parseDateFromString(String dateStr) {
        SimpleDateFormat format = null;
        if (dateStr.length() == 10)
        	format = new SimpleDateFormat("yyyy-MM-dd"); // Simple format
        else
        	format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");              // TMX ISO format
        try {
            return format.parse(dateStr);
        } catch (ParseException pe) {
            return new Date();
        }
    }
}