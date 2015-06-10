/*
 *******************************************************************************
 * Copyright (C) 2005-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;

import com.ibm.icu.impl.URLHandler;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CoverageTest extends CompatibilityTest implements URLHandler.URLVisitor
{

    private static Class serializable;

    public void init() {
        try {    
            serializable = Class.forName("java.io.Serializable");
        } catch (Exception e) {
            // we're in deep trouble...
            warnln("Woops! Can't get class info for Serializable.");
        }
    }

    private Target head = new Target(null);
    private Target tail = head;
    
    private String path;
    
    public CoverageTest()
    {
        this(null);
    }
    
    public CoverageTest(String path)
    {
        this.path = path;
        
        if (path != null) {
            File dir = new File(path);
            
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }
    
    private void writeFile(String className, byte bytes[])
    {
        File file = new File(path + File.separator + className + ".dat");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            stream.write(bytes);
        } catch (Exception e) {
            System.out.print(" - can't write file!");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void add(String className, int classModifiers, byte bytes[])
    {
        CoverageTarget newTarget = new CoverageTarget(className, classModifiers, bytes);
        
        tail.setNext(newTarget);
        tail = newTarget;
    }
    
    public class CoverageTarget extends HandlerTarget
    {
        private byte bytes[];
        private int modifiers;
        
        public CoverageTarget(String className, int classModifiers, byte bytes[])
        {
            super(className, bytes == null? null : new ByteArrayInputStream(bytes));
            
            this.bytes = bytes;
            modifiers = classModifiers;
        }
        
        public boolean validate()
        {
            return super.validate() || Modifier.isAbstract(modifiers);
        }
        
        public void execute() throws Exception
        {
            Class c = Class.forName(name);
            try {
                /*Field uid = */c.getDeclaredField("serialVersionUID");
            } catch (Exception e) {
                errln("No serialVersionUID for " + name);
            }
            
            if (inputStream == null) {
                params.testCount += 1;
            } else {
               if (path != null) {
                    writeFile(name, bytes);
                }
                
                super.execute();
            }
        }
    }
    
    public void visit(String str)
    {
        if(serializable==null){
            return;
        }
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
                
                if (!c.isEnum() && serializable.isAssignableFrom(c)) {
                    if (Modifier.isPublic(m) && !Modifier.isInterface(m)) { 
                        SerializableTest.Handler handler = SerializableTest.getHandler(className);
                        
                        if (handler != null) {
                            Object objectsOut[] = handler.getTestObjects();
                            
                            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                            ObjectOutputStream out = new ObjectOutputStream(byteOut);
                            
                            try {
                                out.writeObject(objectsOut);
                                out.close();
                                byteOut.close();
                            } catch (IOException e) {
                                if (str.equals("/text/PluralRules$FixedDecimal.class")
                                        && logKnownIssue("10268", "Serializable interface is not implemented in PluralRules$FixedDecimal")) {
                                    return;
                                }
                                warnln("Error writing test objects: " + e.toString());
                                return;
                            }
                            
                            add(className, m, byteOut.toByteArray());
                        } else {
                            if (!Modifier.isAbstract(m)) {
                                errln("Missing test handler. Update the list of tests in SerializableTest.java to include a test case for " + className);
                            } else {
                                add(className, m, null);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                warnln("coverage of " + className + ": " + e.toString());
            } catch (Throwable e) {
                e.printStackTrace();
                warnln("coverage of " + className + ": " + e.toString());
            }
        }
    }
    
    protected Target getTargets(String targetName)
    {
        
        if (System.getSecurityManager() != null) {
            // This test won't run under a security manager

            // TODO: Is the above statement really true?
            // We probably need to set up the security policy properly
            // for writing/reading serialized data.

            return null;
        }
        
        if(serializable==null){
            init();
        }

        try {
            Enumeration<URL> urlEnum = getClass().getClassLoader().getResources("com/ibm/icu");
            while (urlEnum.hasMoreElements()) {
                URL url = urlEnum.nextElement();
                URLHandler handler  = URLHandler.get(url);
                if (handler == null) {
                    errln("Unsupported URL: " + url);
                    continue;
                }
                handler.guide(this, true, false);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return head.getNext();
    }
    
    public static void main(String[] args)
    {
        CoverageTest test = new CoverageTest();
        
        test.run(args);
    }
}
