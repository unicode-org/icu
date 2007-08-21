/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;


import java.io.*;

import com.ibm.rbm.gui.RBManagerGUI;

import java.util.*;
import java.net.*;

/**
 * This is the super class for all importer plug-in classes. As of yet, there
 * is little contained in this class.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class RBJavaImporter extends RBImporter {
	
    public RBJavaImporter(String title, RBManager rbm, RBManagerGUI gui) {
        super(title, rbm, gui);
    }
	
    protected void setupFileChooser() {
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                if (f.getName().endsWith(".class") && f.getName().indexOf("_") < 0) return true;
                return false;
            }
			
            public String getDescription() {
                return Resources.getTranslation("import_java_file_description");
            }
        });
    }
	
    protected void beginImport() throws IOException {
        super.beginImport();
        ListResourceBundle base_lrb = null;
        URLClassLoader urlLoader = null;
        try {
            File baseFile = getChosenFile();
            URL baseURL = baseFile.toURL();
            URL urls[] = new URL[1];
            urls[0] = baseURL;
            urlLoader = new URLClassLoader(urls);
            String baseName = baseFile.getName();
            baseName = baseName.substring(0, baseName.indexOf(".class"));
            
            Class baseClass = urlLoader.loadClass(baseName);
            base_lrb = (ListResourceBundle)baseClass.newInstance();
        } catch (Exception e) {
            RBManagerGUI.debugMsg(e.toString());
            RBManagerGUI.debugMsg(e.getMessage());
            e.printStackTrace(System.err);
        }
        if (base_lrb != null) {
            Enumeration keys = base_lrb.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                RBManagerGUI.debugMsg("Resource -> " + key + " = " + base_lrb.getString(key));
            }
        }
    }
}

/*
class myClassLoader extends ClassLoader {
    public myClassLoader() {
        super();
    }
	
    public Class myDefineClass(String name, byte array[], int off, int len) {
        return super.defineClass(name, array, off, len);
    }
}
*/
