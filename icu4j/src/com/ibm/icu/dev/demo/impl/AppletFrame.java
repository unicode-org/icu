/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.impl;
import java.applet.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>A Frame that runs an Applet within itself, making it possible
 * for an applet to run as an application.  Usage:
 *
 * <pre>
 * public class MyApplet extends Applet {
 *     public static void main(String args[]) {
 *         MyApplet applet = new MyApplet();
 *         new AppletFrame("My Applet Running As An App", applet, 640, 480);
 *     }
 *     ...
 * }
 * <pre>
 *
 * @author Alan Liu
 */
public class AppletFrame extends Frame implements AppletStub, AppletContext {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 818828281190757725L;
    Applet applet;

    /**
     * Construct a Frame running the given Applet with the default size
     * of 640 by 480.
     * When the Frame is closed, the applet's stop() method is called,
     * the Frame is dispose()d of, and System.exit(0) is called.
     *
     * @param name the Frame title
     * @param applet the applet to be run
     */
    public AppletFrame(String name, Applet applet) {
        this(name, applet, 640, 480);
    }

    /**
     * Construct a Frame running the given Applet with the given size.
     * When the Frame is closed, the applet's stop() method is called,
     * the Frame is dispose()d of, and System.exit(0) is called.
     *
     * @param name the Frame title
     * @param applet the applet to be run
     * @param width width of the Frame
     * @param height height of the Frame
     */
    public AppletFrame(String name, Applet applet, int width, int height) {
        super(name);
        this.applet = applet;
        applet.setStub(this);

        setSize(width, height);
        add("Center", applet);
        show();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                AppletFrame.this.applet.stop();
                dispose();
                System.exit(0);
            }
        });

        applet.init();
        applet.start();
    }

    // AppletStub API
    public void appletResize(int width, int height) {
        setSize(width, height);
    }

    public AppletContext getAppletContext() {
        return this;
    }

    public URL getCodeBase() {
        return null;
    }

    public URL getDocumentBase() {
        return null;
    }
    
    public String getParameter(String name) {
        return "PARAMETER";
    }

    public boolean isActive() {
        return true;
    }
    
    
    // AppletContext API
    public Applet getApplet(String name) {
        return applet;
    }

    public Enumeration getApplets() {
        return null;
    }

    public AudioClip getAudioClip(URL url) {
        return null;
    }

    public Image getImage(URL url) {
        return null;
    }

    public void showDocument(URL url) {}
    public void showDocument(URL url, String target) {}

    public void showStatus(String status) {
        System.out.println(status);
    }
    
    public void setStream(String key, InputStream stream) throws IOException {
    }
    
    public InputStream getStream(String key) {
        return null;
    }
    
    public Iterator getStreamKeys() {
        return null;
    }
}
