/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.richtext.demo;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Toolkit;

import java.io.File;
import java.io.StreamCorruptedException;
import com.ibm.richtext.textpanel.TextPanel;

/**
 * EditDemo is the main class for a simple, multiple-document
 * styled text editor, built with the classes in the textpanel
 * and textframe packages.
 * <p>
 * To run EditDemo, type:
 * <blockquote><pre>
 * java com.ibm.richtext.demo.EditDemo [file1] [file2] [...]
 * </pre></blockquote>
 * where the filenames are files saved with this demo.
 */
public class EditDemo extends EditApplication {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    
    public static synchronized void main(String[] args) {

        if (args.length > 0 && args[0].equals("-swing")) {
            new com.ibm.richtext.swingdemo.SwingEditDemo(args,1);
        }
        else {
            new EditDemo(args, 0);
        }
    }

    protected EditDemo(String[] args, int start) {

        super(Toolkit.getDefaultToolkit().getSystemClipboard(), 
              TextDocument.STYLED_TEXT);

        if (args.length == start) {
            doNewWindow();
        }
        else {
            boolean openedADocument = false;
            for (int i=start; i < args.length; i++) {

                File file = new File(args[i]);
                TextDocument document = getDocumentFromFile(file);
                
                if (document != null) {
                    addDocument(document);
                    openedADocument = true;
                }
            }
            if (!openedADocument) {
                quit();
            }
        }
    }
    
    public static TextDocument getDocumentFromFile(File file) {
    
        Exception exception = null;
        
        try {
            return TextDocument.createFromFile(file, TextDocument.STYLED_TEXT);
        }
        catch(StreamCorruptedException e) {
            try {
                return TextDocument.createFromFile(file, TextDocument.PLAIN_TEXT);
            }
            catch(Exception e2) {
                exception = e2;
            }
        }
        catch(Exception e) {
            exception = e;
        }
        
        System.err.println("Exception opening file.");
        exception.printStackTrace();
        
        return null;
    }
    
    protected DocumentWindow createDocumentWindow(TextDocument document) {
        
        return new AwtDocumentWindow(this, 
                                     document,
                                     TextPanel.getDefaultSettings(),
                                     true,
                                     null,
                                     true,
                                     true,
                                     null);
    }
    
    public TextDocument openDocument(Frame dialogParent) {
    
        String title = ResourceUtils.getString(EditorResources.OPEN_TITLE);
        File file = AwtDocumentWindow.getFileFromDialog(null,
                                                     title,
                                                     dialogParent,
                                                     FileDialog.LOAD);
        if (file != null) {
            return getDocumentFromFile(file);
        }
        else {
            return null;
        }
    }
}