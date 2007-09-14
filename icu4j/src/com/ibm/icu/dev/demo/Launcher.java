/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.icu.dev.demo.impl.DemoApplet;
import com.ibm.icu.dev.demo.impl.DemoUtility;
import com.ibm.icu.util.VersionInfo;


/**
 * @author srl
 * Application to provide a panel of demos to launch
 */
public class Launcher extends DemoApplet {
    private static final long serialVersionUID = -8054963875776183877L;
    
    /**
     * base package of all demos
     */
    public static final String demoBase = "com.ibm.icu.dev.demo";
    /**
     * list of classes, relative to the demoBase. all must have a static void main(String[])
     */
    public static final String demoList[] = { 
        "calendar.CalendarApp",
        "charsetdet.DetectingViewer",
        "holiday.HolidayCalendarDemo",
//        "number.CurrencyDemo", -- console
//        "rbbi.DBBIDemo",
//        "rbbi.RBBIDemo",
//        "rbbi.TextBoundDemo",
        "rbnf.RbnfDemo",
//        "timescale.PivotDemo",  -- console
        "translit.Demo",
    };

    public class LauncherFrame extends Frame implements ActionListener {
        private static final long serialVersionUID = -8054963875776183878L;
        
        public Button buttonList[] = new Button[demoList.length]; // one button for each demo
        public Label statusLabel;
        private DemoApplet applet;
        
        LauncherFrame(DemoApplet applet) {
            init();
            this.applet = applet;
        }
        
        public void init() {
            // close down when close is clicked.
            // TODO: this should be factored..
            addWindowListener(
                    new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                            setVisible(false);
                            dispose();

                            if (applet != null) {
                                applet.demoClosed();
                            } else System.exit(0);
                        }
                    } );

            setBackground(DemoUtility.bgColor);
            setLayout(new BorderLayout());

            Panel topPanel = new Panel();
            topPanel.setLayout(new GridLayout(5,3));

            for(int i=0;i<buttonList.length;i++) {
                String demo = demoList[i];
                Button b = new Button(demo);
                b.addActionListener(this);
                buttonList[i]=b;
                topPanel.add(b);
            }
            add(BorderLayout.CENTER,topPanel);
            statusLabel = new Label("");
            statusLabel.setAlignment(Label.LEFT);
            String javaVersion = "";
            try { 
                javaVersion = "* Java: "+System.getProperty("java.version");
            } catch (Throwable t) {
                javaVersion = "";
            }
            add(BorderLayout.NORTH, new Label(
                   "ICU Demos * ICU version "+VersionInfo.ICU_VERSION +
                   " * http://icu-project.org "+javaVersion));
            add(BorderLayout.SOUTH,statusLabel);
            // set up an initial status.
            showStatus(buttonList.length+" demos ready. ");
        }
        
        /**
         * Change the 'status' field, and set it to black
         * @param status
         */
        void showStatus(String status) {
            statusLabel.setText(status);
            statusLabel.setForeground(Color.BLACK);
            statusLabel.setBackground(Color.WHITE);
//            statusLabel.setFont(Font.PLAIN);
            doLayout();
        }
        void showStatus(String demo, String status) {
            showStatus(demo+": "+status);
        }
        void showFailure(String status) {
            statusLabel.setText(status);
            statusLabel.setBackground(Color.GRAY);
            statusLabel.setForeground(Color.RED);
//            statusLabel.setFont(Font.BOLD);
            doLayout();
        }
        void showFailure(String demo, String status) {
            showFailure(demo+": "+status);
        }

        
        public void actionPerformed(ActionEvent e) {
            // find button
            for(int i=0;i<buttonList.length;i++) {
                if(e.getSource() == buttonList[i]) {
                    String demoShort = demoList[i];
                    String demo = demoBase+'.'+demoShort;
                    showStatus(demoShort, "launching");
                    try {
                        Class c = Class.forName(demo);
                        String args[] = new String[0];
                        Class params[] = new Class[1];
                        params[0] = args.getClass();
                        Method m = c.getMethod("main", params );
                        Object[] argList = { args };
                        m.invoke(null, argList);
                        showStatus(demoShort, "launched.");
                    } catch (ClassNotFoundException e1) {
                        showFailure(demoShort,e1.toString());
                        e1.printStackTrace();
                    } catch (SecurityException se) {
                        showFailure(demoShort,se.toString());
                        se.printStackTrace();
                    } catch (NoSuchMethodException nsme) {
                        showFailure(demoShort,nsme.toString());
                        nsme.printStackTrace();
                    } catch (IllegalArgumentException iae) {
                        showFailure(demoShort,iae.toString());
                        iae.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        showFailure(demoShort,iae.toString());
                        iae.printStackTrace();
                    } catch (InvocationTargetException ite) {
                        showFailure(demoShort,ite.toString());
                        ite.printStackTrace();
                    }
                    repaint();
                }
            }
        }

    }
    
    /* This creates a Frame for the demo applet. */
    protected Frame createDemoFrame(DemoApplet applet) {
        return new LauncherFrame(applet);
    }

    /**
     * The main function which defines the behavior of the Demo
     * applet when an applet is started.
     */
    public static void main(String[] args) {
        new Launcher().showDemo();
    }
}
