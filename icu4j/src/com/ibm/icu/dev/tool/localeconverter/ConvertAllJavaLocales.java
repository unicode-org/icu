/**
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

/**

The ConvertJavaLocale application converts java locales to 
Java and ICU Locale files.  It's usage is as follows

    ConvertJavaLocale [-11] [-12] [-icu] locale...

Usage

-11
    If this option is specified, data is output in 
    Java 1.1.x locale format.
    
-12
    If this option is specified, data is output in
    Java 1.2.x locale format.  If an output format
    is not specified, -12 is the default.
    
-icu
    If this option is specified, data is output in
    ICU locale format.

locale
    The locale to convert


*/
/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
public class ConvertAllJavaLocales {
    public static void main(String args[]) {
        try {
            new ConvertAllJavaLocales(args);
        } catch (Throwable t) {
            System.err.println("Unknown error: "+t);
        }
    }
    
    public ConvertAllJavaLocales(String argsIn[]) {
        try {
            String packageName = argsIn[0];
            System.out.println("This is the packagename : "+packageName);
            String classname = packageName+".Locale";
            //classname.concat();
            System.out.println("This is the classname : "+classname);
          /*  Class cl = Class.forName(classname);
            Class[] paramList=null;
            Method gvl = cl.getMethod("getAvailableLocales", paramList);
            Object[] params = new Object[]{""};
            gvl.invoke(null,params);*/
            final Locale[] locales = java.util.Locale.getAvailableLocales();//(Locale[])gvl.invoke(null,params);;
       
            for (int i = 0; i < locales.length; i++) {
                final String localeName = locales[i].toString();
                final String[] args = {"-package",packageName,"-icu", localeName};
                
                System.out.println("Converting "+localeName);
                
                final FileOutputStream outFile = new FileOutputStream(localeName + ".txt");
                final PrintStream out = new PrintStream(outFile, true);
                
                new ConvertJavaLocale(args, out);
                
                out.close();
            }
            System.out.println("Converting root locale");
            final String[] args = {"-package",packageName,"-icu","root"};
            final FileOutputStream outFile = new FileOutputStream("root.txt");
            final PrintStream out = new PrintStream(outFile, true);
            new ConvertJavaLocale(args, out);
            out.close();
                
        } catch (IOException e) {
            System.err.println("Unexpected IO error");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
