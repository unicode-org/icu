/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

/**
 * This is a class used by the RBReporter to track occurances of a resource
 * key found while scanning a text code file. It is used mainly to produce error
 * messages with helpful context information.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBReporter
 */
public class Occurance {
    private String file_name;
    private String file_path;
    private int line_number;
    
    /**
     * Basic data constructor.
     */
    
    Occurance (String file_name, String file_path, int line_number) {
        this.file_name = file_name;
        this.file_path = file_path;
        this.line_number = line_number;
    }
    
    /**
     * Returns the associated file name of the occurance
     */
    
    public String getFileName() {
        return file_name;
    }
    
    /**
     * Returns the associated file path of the occurance
     */
    
    public String getFilePath() {
        return file_path;
    }
    
    /**
     * Returns the line number of the occurance.
     */
    
    public int getLineNumber() {
        return line_number;
    }
    
    /**
     * A representation of the occurance of the form 'Occurance: _file_path_ (_line_number_)'
     */
    
    public String toString() {
        return "Occurance: " + file_path + " (" + line_number + ")";
    }
}