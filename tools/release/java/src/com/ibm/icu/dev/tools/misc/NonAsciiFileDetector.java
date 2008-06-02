/*
 **********************************************************************
 * Copyright (c) 2008, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package com.ibm.icu.dev.tools.misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;

public class NonAsciiFileDetector
{
    public static class ICUSourceFileFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name) 
        {
            return name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h") || name.endsWith(".java");
        }
    }

    public static int isNonAscii(File file) throws IOException
    {
        BufferedReader in = new BufferedReader(new FileReader(file));
        int line = 0;
        while (true) {
            String str = in.readLine();
            if (str == null) {
                in.close();
                return -1;
            }
            for (int i = 0; i < str.length(); i ++) {
                if (str.charAt(i) > 0x7f) {
                    System.out.println("Ascii test failed in " 
                                       + file.getAbsolutePath() + " line "
                                       + line + " string\n" + str);
                    // non-latin1
                    in.close();
                    return line;
                }
            }
            line ++;
        }
    }
    
    public static void listFiles(File file, 
                                 FilenameFilter filter,
                                 Vector list) throws IOException 
    {
        File files[] = file.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i ++) {
                if (files[i].isDirectory()) {
                    listFiles(files[i], filter, list);          
                }
                else {
                    if (filter.accept(file, files[i].getName())) {
                        list.add(files[i]);
                    }
                }
            }
        }
    }

    public static Vector getNonAsciiFiles(String directory, 
                                            FilenameFilter filter)
                                            throws IOException
    {
        Vector files = new Vector();
        Vector result = new Vector();
        listFiles(new File(directory), filter, files);
        int filecount = files.size();
        if (filecount == 0) {
            return null;
        }
        for (int i = 0; i < filecount; i ++) {
             int isnonascii = isNonAscii((File)files.elementAt(i));
             if (isnonascii != -1) {
                 result.add(((File)files.elementAt(i)).getAbsolutePath());
                 result.add(new Integer(isnonascii));
             }
        }
        return result;
    }

    public static void main(String arg[])
    {
        try {
            Vector nonascii = getNonAsciiFiles(arg[0], new ICUSourceFileFilter());
            System.out.println();
            if (nonascii != null && nonascii.size() > 0) {
                for (int i = 0; i < nonascii.size(); i += 2) {
                     System.out.println("Non ascii files " 
                            + (String)nonascii.elementAt(i) + " " 
                            + ((Integer)nonascii.elementAt(i + 1)).intValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
             
