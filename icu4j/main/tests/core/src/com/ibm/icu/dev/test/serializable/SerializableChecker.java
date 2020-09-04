// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.ibm.icu.impl.URLHandler;

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
    //private static Class throwable;

    private String path = null;

    //private boolean write;

    public SerializableChecker(String path)
    {
        this.path = path;

        if (path != null) {
            File dir = new File(path);

            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    static {
        try {
            serializable = Class.forName("java.io.Serializable");
            //throwable    = Class.forName("java.lang.Throwable");
        } catch (Exception e) {
            // we're in deep trouble...
            System.out.println("Woops! Can't get class info for Serializable and Throwable.");
        }
    }

    private void writeFile(String className, byte bytes[])
    {
        File file = new File(path + File.separator + className + ".dat");

        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(bytes);
            stream.close();
        } catch (Exception e) {
            System.out.print(" - can't write file!");
        }
    }

    @Override
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
                int   m = c.getModifiers();

                if (serializable.isAssignableFrom(c) /*&&
                    (! throwable.isAssignableFrom(c) || c.getDeclaredFields().length > 0)*/) {
                    //Field uid;

                    System.out.print(className + " (" + Modifier.toString(m) + ") - ");

                    if(!Modifier.isInterface(m)){
                        try {
                            /* uid = */
                            c.getDeclaredField("serialVersionUID");
                        } catch (Exception e) {
                            System.out.print("no serialVersionUID - ");
                        }
                    }

                    if (Modifier.isPublic(m)) {
                        SerializableTestUtility.Handler handler = SerializableTestUtility.getHandler(className);

                        if (!Modifier.isInterface(m) && handler != null) {
                            Object objectsOut[] = handler.getTestObjects();
                            Object objectsIn[];
                            boolean passed = true;

                            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                            ObjectOutputStream out = new ObjectOutputStream(byteOut);

                            try {
                                out.writeObject(objectsOut);
                                out.close();
                                byteOut.close();
                            } catch (IOException e) {
                                System.out.println("Eror writing test objects:" + e.toString());
                                return;
                            }

                            if (path != null) {
                                writeFile(className, byteOut.toByteArray());
                            }

                            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
                            ObjectInputStream in = new ObjectInputStream(byteIn);

                            try {
                                objectsIn = (Object[]) in.readObject();
                                in.close();
                                byteIn.close();
                            } catch (Exception e) {
                                System.out.println("Error reading test objects:" + e.toString());
                                return;
                            }

                            for(int i = 0; i < objectsIn.length; i += 1) {
                                if (! handler.hasSameBehavior(objectsIn[i], objectsOut[i])) {
                                    passed = false;
                                    System.out.println("Object " + i + " failed behavior test.");
                                }
                            }

                            if (passed) {
                                System.out.print("test passed.");
                            }
                        } else {
                            // it's OK to not have tests for abstract classes...
                            if (! Modifier.isAbstract(m)) {
                                System.out.print("no test.");
                            }
                        }
                    }

                    System.out.println();
                }
           } catch (Exception e) {
                System.out.println("Error processing " + className + ": " + e.toString());
            }
        }
    }

    public static void main(String[] args)
    {
        List argList = Arrays.asList(args);
        String path = null;

        for (Iterator it = argList.iterator(); it.hasNext(); /*anything?*/) {
            String arg = (String) it.next();

            if (arg.equals("-w")) {
                if (it.hasNext()) {
                    path = (String) it.next();
                } else {
                    System.out.println("Missing directory name on -w command.");
                }
            } else {


                try {
                    //URL jarURL  = new URL("jar:file:/dev/eclipse/workspace/icu4j/icu4j.jar!/com/ibm/icu");
                    //URL fileURL = new URL("file:/dev/eclipse/workspace/icu4j/classes/com/ibm/icu");
                    URL url = new URL(arg);
                    URLHandler handler  = URLHandler.get(url);
                    SerializableChecker checker = new SerializableChecker(path);

                    System.out.println("Checking classes from " + arg + ":");
                    handler.guide(checker, true, false);
                } catch (Exception e) {
                    System.out.println("Error processing URL \"" + arg + "\" - " + e.getMessage());
                }
            }
        }
    }
}
