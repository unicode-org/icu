/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/VersionInfo.java,v $ 
 * $Date: 2002/03/04 04:34:31 $ 
 * $Revision: 1.1 $
 *
 * jitterbug 1741
 *****************************************************************************************
 */

package com.ibm.icu.util;

import java.util.HashMap;

/**
 * Class to store version numbers of the form major.minor.milli.micro.
 * @author synwee
 * @since March 1 2002
 * @draft 2.1
 */
public final class VersionInfo
{
    /**
     * Returns an instance of VersionInfo with the argument version.
     * @param version version String in the format of "major.minor.milli.micro", 
     *                where major, minor, milli, micro are non-negative numbers
     * @return an instance of VersionInfo with the argument version.
     * @exception throws an IllegalArgumentException when the argument version 
     *                is not in the right format
     * @draft 2.1
     */
    public static VersionInfo getInstance(String version)
    {
    	int length  = version.length();
    	int array[] = {0, 0, 0, 0};
    	int count   = 0;
    	int index   = 0;
    	
    	while (count < 4 && index < length) {
    		char c = version.charAt(index);
    		if (c == '.') {
    			count ++;
    		}
    		else {
    			c -= '0';
    			if (c < 0 || c > 9) {
    				throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
    			}
    			array[count] *= 10;
    			array[count] += c;
    		}
    		index ++;
    	}
    	if (index != length || count != 3) {
    		throw new IllegalArgumentException(
    		    "Invalid version number: Insufficient data or string exceeds version format");
    	}
    	for (int i = 0; i < 4; i ++) {
    		if (array[i] < 0 || array[i] > 255) {
  	    		throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
    	    }
    	}
    	
    	int     tempversion = getInt(array[0], array[1], array[2], array[3]);
    	Integer key         = new Integer(tempversion);
    	Object  result      = MAP_.get(key);
    	// checks if it is in the hashmap
    	if (result == null) {
    		result = new VersionInfo(tempversion);
    		MAP_.put(key, result);
    	}
    	return (VersionInfo)result;
    }
 
    /** 
     * Returns an instance of VersionInfo with the argument version.
     * @param major major version, non-negative number.
     * @param minor minor version, non-negative number.
     * @param milli milli version, non-negative number.
     * @param micro micro version, non-negative number.
     * @exception throws an IllegalArgumentException when either arguments are
     *                                     negative 
     * @draft 2.1
     */
    public static VersionInfo getInstance(int major, int minor, int milli, 
                                          int micro)
    {
   	 	// checks if it is in the hashmap
   	 	// else
   	 	if (major < 0 || major > 255 || minor < 0 || minor > 255 || 
    	    milli < 0 || milli > 255 || micro < 0 || micro > 255) {
    	    throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
    	}
    	int     version = getInt(major, minor, milli, micro);
    	Integer key     = new Integer(version);
    	Object  result  = MAP_.get(key);
    	if (result == null) {
	   	 	result = new VersionInfo(version);
	   	 	MAP_.put(key, result);
    	}
      	return (VersionInfo)result;
    }
 
    /** 
     * Returns the String representative of VersionInfo in the format of 
     * "major.minor.milli.micro"   
     * @return String representative of VersionInfo
     * @draft 2.1
     */
    public String toString()
    {
   	    StringBuffer result = new StringBuffer(7);
    	result.append(getMajor());
        result.append('.');
        result.append(getMinor());
        result.append('.');
        result.append(getMilli());
        result.append('.');
        result.append(getMicro());
        return result.toString();
    }
    
    /** 
     * Returns the major version number
     * @return the major version number    
     * @draft 2.1
     */
    public int getMajor()
    {
    	return (m_version_ >> 24) & LAST_BYTE_MASK_ ;
    }
 
    /** 
     * Returns the minor version number
     * @return the minor version number    
     * @draft 2.1
     */
    public int getMinor()
    {
    	return (m_version_ >> 16) & LAST_BYTE_MASK_ ;
    }
 
    /** 
     * Returns the milli version number
     * @return the milli version number    
     * @draft 2.1
     */
    public int getMilli()
    {
    	return (m_version_ >> 8) & LAST_BYTE_MASK_ ;
    }
 
    /** 
     * Returns the micro version number
     * @return the micro version number
     * @draft 2.1    
     */
    public int getMicro()
    {
    	return m_version_ & LAST_BYTE_MASK_ ;
    }
 
    /**
     * Checks if this version information is equals to the argument version
     * @param other object to be compared
     * @return true if other is equals to this object's version information, 
     *         false otherwise
     * @draft 2.1
     */
    public boolean equals(Object other)
	{
		return other == this;
	}
 
    /**
     * Compares other with this VersionInfo. 
     * @param other VersionInfo to be compared
     * @return 0 if the argument is a VersionInfo object that has version 
     *           information equals to this object. 
     *           Less than 0 if the argument is a VersionInfo object that has 
     *           version information greater than this object. 
     *           Greater than 0 if the argument is a VersionInfo object that 
     *           has version information less than this object.
     * @draft 2.1
     */
    public int compareTo(VersionInfo other)
    {
    	return m_version_ - other.m_version_;
    }
   
    // private data members ----------------------------------------------
    
    /**
     * Version number stored as a byte for each of the major, minor, milli and
     * micro numbers in the 32 bit int.
     * Most significant for the major and the least significant contains the 
     * micro numbers.
     */
    private int m_version_;
    /**
     * Map of singletons
     */
    private static HashMap MAP_ = new HashMap();
    /**
     * Last byte mask
     */
    private static final int LAST_BYTE_MASK_ = 0xFF;
    /**
     * Error statement string
     */
    private static final String INVALID_VERSION_NUMBER_ = 
        "Invalid version number: Version number may be negative or greater than 255";
    
    // private constructor -----------------------------------------------
    
    /**
     * Constructor with int 
     * @param compactversion a 32 bit int with each byte representing a number
     */
    private VersionInfo(int compactversion) 
    {
    	m_version_ = compactversion;   	
    }
    
    /**
     * Gets the int from the version numbers
     * @param major non-negative version number
     * @param minor non-negativeversion number
     * @param milli non-negativeversion number
     * @param micro non-negativeversion number
     */
    private static int getInt(int major, int minor, int milli, int micro) 
    {
    	return (major << 24) | (minor << 16) | (milli << 8) | micro;
    }
}