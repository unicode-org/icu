/*
 *****************************************************************************
 * Copyright (C) 2000-2002, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/rbm/RBExporter.java,v $ 
 * $Date: 2002/05/20 18:53:10 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.io.*;
import javax.swing.*;
import java.util.*;

/**
 * This is the super class for all exporter plug-in classes. As of yet, there
 * is little contained in this class.
 * 
 * @author Jared Jackson - Email: <a href="mailto:jjared@almaden.ibm.com">jjared@almaden.ibm.com</a>
 * @see com.ibm.rbm.RBManager
 */
public class RBExporter {
    protected static JFileChooser chooser;
	
    /**
     * Basic empty constructor.
     */
    public RBExporter() {
        
    }
}