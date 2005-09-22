/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.tool.serializable;

import com.ibm.icu.impl.URLHandler;
import java.lang.Class;
import java.lang.reflect.Field;
import java.net.URL;

/**
 * This class examines all the classes in a Jar file or a directory
 * and lists all those classes that implement <code>Serializable</code>. It also checks
 * to make sure that those classes have the <code>serialVersionUID</code>
 * field define.
 * 
 */
public class SerializableChecker implements URLHandler.URLVisitor
{
    private static Class serializable;
    private static Class throwable;
    
    static {
        try {    
            serializable = Class.forName("java.io.Serializable");
            throwable    = Class.forName("java.lang.Throwable");
        } catch (Exception e) {
            // we're in deep trouble...
            System.out.println("Woops! Can't get class info for Serializable and Throwable.");
        }
    }
    
    public void visit(String str)
    {
        int ix = str.lastIndexOf(".class");
        
        if (ix >= 0) {
            String className = "com.ibm.icu" + str.substring(0, ix).replace('/', '.');
            
            // Skip things in com.ibm.icu.dev; they're not relevant.
            if (className.startsWith("com.ibm.icu.dev.")) {
                return;
            }
            
            try {
                Class c = Class.forName(className);
                
                if (serializable.isAssignableFrom(c) && ! throwable.isAssignableFrom(c)) {
                    Field uid;
                    
                    System.out.print(className);
                    
                    try {
                        uid = c.getDeclaredField("serialVersionUID");
                    } catch (Exception e) {
                        System.out.print(" - no serialVersionUID!");
                    }
                    
                    System.out.println();
                }
           } catch (Exception e) {
                System.out.println("Error processing " + className);
            }
        }
    }

    public static void main(String[] args) throws java.net.MalformedURLException
    {
        try {
            //URL jarURL  = new URL("jar:file:/dev/eclipse/workspace/icu4j/icu4j.jar!/com/ibm/icu");
            //URL fileURL = new URL("file:/dev/eclipse/workspace/icu4j/classes/com/ibm/icu");
            URL url = new URL(args[0]);
            URLHandler handler  = URLHandler.get(url);
            SerializableChecker checker = new SerializableChecker();
            
            System.out.println("Checking classes from " + args[0] + ":");
            handler.guide(checker, true, false);
        } catch (Exception e) {
            System.out.println("Error processing URL \"" + args[0] + "\" - " + e.getMessage());
        }
    }
}
