/*
 *****************************************************************************
 * Copyright (C) 2000-2002, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/rbm/ScanResult.java,v $ 
 * $Date: 2002/05/20 18:53:08 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************
 */
package com.ibm.rbm;


import java.util.*;

/**
 * This class represents the results found for each resource key while
 * performing the code scan done by RBReporter.
 * 
 * @author Jared Jackson - Email: <a href="mailto:jjared@almaden.ibm.com">jjared@almaden.ibm.com</a>
 * @see com.ibm.rbm.RBReporter
 */
public class ScanResult {
    BundleItem item;
    Vector occurances;
    
    ScanResult(BundleItem item) {
        this.item = item;
        occurances = new Vector();
    }
	
    BundleItem getItem() {
        return item;
    }
	
    int getNumberOccurances() {
        return occurances.size();
    }
	
    Vector getOccurances() {
        return occurances;
    }
	
    void addOccurance(Occurance o) {
        occurances.addElement(o);
    }
	
    String getName() {
        return item.getKey();
    }
	
    String getGroupName() {
        if (item.getParentGroup() != null) return item.getParentGroup().getName();
        return "Unknown";
    }
}