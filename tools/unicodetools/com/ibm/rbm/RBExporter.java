/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.io.IOException;

import javax.swing.*;

/**
 * This is the super class for all exporter plug-in classes. As of yet, there
 * is little contained in this class.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public abstract class RBExporter {
    protected static JFileChooser chooser;
	
    public abstract void export(RBManager rbm) throws IOException;
}