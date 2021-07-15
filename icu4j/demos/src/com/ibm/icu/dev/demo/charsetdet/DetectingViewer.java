// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 **************************************************************************
 * Copyright (C) 2005-2010, International Business Machines Corporation   *
 * and others. All Rights Reserved.                                       *
 **************************************************************************
 *
 */

package com.ibm.icu.dev.demo.charsetdet;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.AccessControlException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

import com.ibm.icu.charset.CharsetICU;
import com.ibm.icu.dev.demo.impl.DemoApplet;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * This simple application demonstrates how to use the CharsetDetector API. It
 * opens a file or web page, detects the encoding, and then displays it using that
 * encoding.
 */
public class DetectingViewer extends JFrame implements ActionListener
{
    
    /**
     * For serialization
     */
    private static final long serialVersionUID = -2307065724464747775L;
    private JTextPane text;
    private JFileChooser fileChooser;
    
    /**
     * @throws java.awt.HeadlessException
     */
    public DetectingViewer()
    {
        super();
        DemoApplet.demoFrameOpened();
        
        try {
            fileChooser = new JFileChooser();
        } catch (AccessControlException ace) {
            System.err.println("no file chooser - access control exception. Continuing without file browsing. "+ace.toString());
            fileChooser = null; //
        }
        
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 800);

        setJMenuBar(makeMenus());
        text = new JTextPane();
        text.setContentType("text/plain");
        text.setText("");
        text.setSize(800, 800);
        
        Font font = new Font("Arial Unicode MS", Font.PLAIN, 24);
        text.setFont(font);
        
        JScrollPane scrollPane = new JScrollPane(text);
        
        getContentPane().add(scrollPane);
        setVisible(true);

        addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
//                        setVisible(false);
//                        dispose();

                          doQuit();
                    }
                } );

    
    }

    public void actionPerformed(ActionEvent event)
    {
        String cmd = event.getActionCommand();
        
        if (cmd.equals("New...")) {
           doNew();
        } else if (cmd.equals("Open File...")) {
           doOpenFile();
        } else if (cmd.equals("Open URL...")) {
            doOpenURL();
        } else if (cmd.equals("Quit")) {
           doQuit();
        }
    }

    public static void main(String[] args)
    {
        new DetectingViewer();
    }
    
    private void errorDialog(String title, String msg)
    {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }
    
    private BufferedInputStream openFile(File file)
    {
        FileInputStream fileStream = null;
        
        try {
            fileStream = new FileInputStream(file);
        } catch (Exception e) {
            errorDialog("Error Opening File", e.getMessage());
            return null;
        }
        
        return new BufferedInputStream(fileStream);
    }
    
//    private void openFile(String directory, String filename)
//    {
//        openFile(new File(directory, filename));
//    }
    
    
    private BufferedInputStream openURL(String url)
    {
        InputStream s = null;

        try {
            URL aURL = new URL(url);
            s = aURL.openStream();
        } catch (Exception e) {
            errorDialog("Error Opening URL", e.getMessage());
            return null;
        }
        
        return new BufferedInputStream(s);
    }
    
    private String encodingName(CharsetMatch match)
    {
        return match.getName() + " (" + match.getLanguage() + ")";
    }
    
    private void setMatchMenu(CharsetMatch[] matches)
    {
        JMenu menu = getJMenuBar().getMenu(1);
        JMenuItem menuItem;
        
        menu.removeAll();
        
        for (int i = 0; i < matches.length; i += 1) {
            CharsetMatch match = matches[i];
            
            menuItem = new JMenuItem(encodingName(match) + " " + match.getConfidence());
            
            menu.add(menuItem);
        }
    }
    
    private byte[] scriptTag = {(byte) 's', (byte) 'c', (byte) 'r', (byte) 'i', (byte) 'p', (byte) 't'};
    private byte[] styleTag  = {(byte) 's', (byte) 't', (byte) 'y', (byte) 'l', (byte) 'e'};
    private static int BUFFER_SIZE = 100000;
    
    private boolean openTag(byte[] buffer, int offset, int length, byte[] tag)
    {
        int tagLen = tag.length;
        int bufRem = length - offset;
        int b;
        
        for (b = 0; b < tagLen && b < bufRem; b += 1) {
            if (buffer[b + offset] != tag[b]) {
                return false;
            }
        }
        
        return b == tagLen;
    }
    
    private boolean closedTag(byte[] buffer, int offset, int length, byte[] tag)
    {
        if (buffer[offset] != (byte) '/') {
            return false;
        }
        
        return openTag(buffer, offset + 1, length, tag);
    }
    
    private byte[] filter(InputStream in)
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRemaining = BUFFER_SIZE;
        int bufLen = 0;
        
        in.mark(BUFFER_SIZE);
        
        try {
            while (bytesRemaining > 0) {
                int bytesRead = in.read(buffer, bufLen, bytesRemaining);
                
                if (bytesRead <= 0) {
                    break;
                }
                
                bufLen += bytesRead;
                bytesRemaining -= bytesRead;
            }
        } catch (Exception e) {
            // TODO: error handling?
            return null;
        }
        
        boolean inTag = false;
        boolean skip  = false;
        int out = 0;
        
        for (int i = 0; i < bufLen; i += 1) {
            byte b = buffer[i];
            
            if (b == (byte) '<') {
                inTag = true;
                
                if (openTag(buffer, i + 1, bufLen, scriptTag) ||
                    openTag(buffer, i + 1, bufLen, styleTag)) {
                    skip = true;
                } else if (closedTag(buffer, i + 1, bufLen, scriptTag) ||
                           closedTag(buffer, i + 1, bufLen, styleTag)) {
                    skip = false;
                }
            } else if (b == (byte) '>') {
                inTag = false;
            } else if (! (inTag || skip)) {
                buffer[out++] = b;
            }
        }

        byte[] filtered = new byte[out];
        
        System.arraycopy(buffer, 0, filtered, 0, out);
        return filtered;
    }
    
    private CharsetMatch[] detect(byte[] bytes)
    {
        CharsetDetector det = new CharsetDetector();
        
        det.setText(bytes);
        
        return det.detectAll();
    }
    
    private CharsetMatch[] detect(BufferedInputStream inputStream)
    {
        CharsetDetector det    = new CharsetDetector();
        
        try {
            det.setText(inputStream);
            
            return det.detectAll();
        } catch (Exception e) {
            // TODO: error message?
            return null;
        }
    }
    
    private void show(InputStream inputStream, CharsetMatch[] matches, String title)
    {
        InputStreamReader isr;
        char[] buffer = new char[1024];
        int bytesRead = 0;
        
        if (matches == null || matches.length == 0) {
            errorDialog("Match Error", "No matches!");
            return;
        }
        
        try {
            StringBuffer sb = new StringBuffer();
            String encoding = matches[0].getName();
            
            inputStream.reset();
            
            if (encoding.startsWith("UTF-32")) {
                byte[] bytes = new byte[1024];
                int offset = 0;
                int chBytes = 0;
                Charset utf32 = CharsetICU.forNameICU(encoding);
                
                while ((bytesRead = inputStream.read(bytes, offset, 1024)) >= 0) {
                    offset  = bytesRead % 4;
                    chBytes = bytesRead - offset;
                    
                    sb.append(utf32.decode(ByteBuffer.wrap(bytes)).toString());
                    
                    if (offset != 0) {
                        for (int i = 0; i < offset; i += 1) {
                            bytes[i] = bytes[chBytes + i];
                        }
                    }
                }
            } else {
                isr = new InputStreamReader(inputStream, encoding);
                
                while ((bytesRead = isr.read(buffer, 0, 1024)) >= 0) {
                    sb.append(buffer, 0, bytesRead);
                }
                
                isr.close();
            }
            
            this.setTitle(title + " - " + encodingName(matches[0]));
            
            setMatchMenu(matches);
            text.setText(sb.toString());
        } catch (IOException e) {
            errorDialog("IO Error", e.getMessage());
        } catch (Exception e) {
            errorDialog("Internal Error", e.getMessage());
        }
    }
    
    private void doNew()
    {
        // open a new window...
    }
    
    private void doOpenFile()
    {
        int retVal = fileChooser.showOpenDialog(this);
        
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            BufferedInputStream inputStream = openFile(file);
            
            if (inputStream != null) {
                CharsetMatch[] matches = detect(inputStream);
                
                show(inputStream, matches, file.getName());                
            }
        }
    }
    
    private void doOpenURL()
    {
        String url = (String) JOptionPane.showInputDialog(this, "URL to open:", "Open URL", JOptionPane.PLAIN_MESSAGE,
                null, null, null);
        
        if (url != null && url.length() > 0) {
            BufferedInputStream inputStream = openURL(url);
            
            if (inputStream != null) {
                byte[] filtered = filter(inputStream);
                CharsetMatch[] matches = detect(filtered);
                
                show(inputStream, matches, url);                
            }
        }
}
    
    private void doQuit()
    {
        DemoApplet.demoFrameClosed();
        this.setVisible(false);
        this.dispose();
    }
    
    private JMenuBar makeMenus()
    {
        JMenu menu = new JMenu("File");
        JMenuItem mi;
        
        mi = new JMenuItem("Open File...");
        mi.setAccelerator((KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK)));
        mi.addActionListener(this);
        menu.add(mi);
        if(fileChooser == null) {
            mi.setEnabled(false); // no file chooser.
        }
        
        mi = new JMenuItem("Open URL...");
        mi.setAccelerator((KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK)));
        mi.addActionListener(this);
        menu.add(mi);
        
        mi = new JMenuItem("Quit");
        mi.setAccelerator((KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK)));
        mi.addActionListener(this);
        menu.add(mi);
        
        JMenuBar mbar = new JMenuBar();
        mbar.add(menu);
        
        menu = new JMenu("Detected Encodings");
        mbar.add(menu);
        
        return mbar;
    }
}
