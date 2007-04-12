/*
 * (C) Copyright IBM Corp. 1998-2007.  All Rights Reserved.
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
package com.ibm.richtext.swingdemo;

import com.ibm.richtext.textpanel.JTextPanel;
import com.ibm.richtext.textpanel.TextPanelListener;
import com.ibm.richtext.textpanel.TextPanelSettings;

import com.ibm.richtext.swingui.JTabRuler;
import com.ibm.richtext.swingui.SwingMenuBuilder;

import com.ibm.richtext.print.PrintingUtils;

import com.ibm.richtext.demo.AwtDocumentWindow;
import com.ibm.richtext.demo.DocumentWindow;
import com.ibm.richtext.demo.EditApplication;
import com.ibm.richtext.demo.EditorResources;
import com.ibm.richtext.demo.ResourceUtils;
import com.ibm.richtext.demo.TextDocument;

import java.awt.BorderLayout;
import java.awt.FileDialog;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.text.MessageFormat;

/**
 * AwtDocumentWindow is a Frame containing a TextPanel, with a document
 * for storing the text in the TextPanel.
 */
final class SwingDocumentWindow extends JFrame implements DocumentWindow {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -1514405707157485775L;

    //static final String COPYRIGHT =
    //            "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private JTextPanel fTextPanel;
    private EditApplication fApplication;
    private TextDocument fDocument;

    /**
     * Create a new AwtDocumentWindow.
     * @param application the application that owns this document
     * @param document the document to show in this AwtDocumentWindow
     */
    SwingDocumentWindow(EditApplication application,
                        TextDocument document,
                        TextPanelSettings textPanelSettings,
                        boolean useTabRuler,
                        TextPanelListener listener,
                        boolean supportStyledText,
                        boolean supportPlainText,
                        int[] menus) {

        fApplication = application;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        fTextPanel = new JTextPanel(textPanelSettings, null, application.getClipboard());
        if (listener != null) {
            fTextPanel.addListener(listener);
        }
        setDocument(document);

        addMenuBar(supportStyledText, supportPlainText, menus);

        getContentPane().setLayout(new BorderLayout());

        if (useTabRuler) {
            JTabRuler tabRuler = new JTabRuler(14, 10, fTextPanel);
            getContentPane().add(tabRuler, "North");
        }
        
        getContentPane().add(fTextPanel, "Center");

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });
    }

    private void addMenuBar(boolean supportStyledText, 
                            boolean supportPlainText,
                            int[] menus) {
        
        JMenuBar menuBar = new JMenuBar();
        String menuTitle = ResourceUtils.getString(EditorResources.FILE);
        JMenu menu = new JMenu(menuTitle);
        new SwingFileMenuManager(menu, fApplication, this,
                                 supportStyledText, supportPlainText);
        menuBar.add(menu);

        SwingMenuBuilder.getInstance().createMenus(menuBar, fTextPanel, this, menus);
        setJMenuBar(menuBar);
    }
    
    /**
     * Return true if it is OK to set the document text and file to
     * something different.
     */
    private boolean canChangeDocuments() {

        // If the text is modified, give the user a chance to
        // save it.  Otherwise return true.

        if (fDocument.isModified()) {
            int save = askSave();
            if (save == JOptionPane.YES_OPTION) {
                return doSave();
            }
            else {
                return save == JOptionPane.NO_OPTION;
            }
        }
        else {
            return true;
        }
    }

    private void setDocument(TextDocument document) {

        fDocument = document;
        fDocument.setTextPanel(fTextPanel);
        setTitle(fDocument.getTitle());
    }

    /**
     * Set the document to empty text with no associated file.  If
     * the document text is not saved, prompt the user to save the
     * the text first.  If this operation is canceled, the document
     * is unchanged.
     */
    public void doNew() {

        if (!canChangeDocuments()) {
            return;
        }
        
        setDocument(fApplication.createNewDocument());
    }

    /**
     * Prompt the user for a file from which to load a text document.
     * If the current text is not saved, first prompt the user to
     * save.  If either operation is canceled or fails, the document
     * is unchanged.
     */
    public void doOpen() {

        if (!canChangeDocuments()) {
            return;
        }

        TextDocument document = fApplication.openDocument(this);

        if (document != null) {
            setDocument(document);
        }
    }

    /**
     * Prompt the user for a file in which to save the document text.
     * If this operation is not canceled, save the text in the file.
     * The file becomes this document's file.
     */
    public boolean doSaveAs(int format) {

        String title = ResourceUtils.getString(EditorResources.SAVE_TITLE);
        File file = AwtDocumentWindow.getFileFromDialog(fDocument.getFile(), title, this, FileDialog.SAVE);
        
        if (file == null) {
            return false;
        }
        
        fDocument.setFile(file);
        setTitle(fDocument.getTitle());

        fDocument.setFormat(format);
        
        return fDocument.save();
    }

    /**
     * Save the text in this document.  If there is no file associated
     * with the text, this is equivalent to <code>doSaveAs</code>.
     * This method returns true if the document was successfully saved.
     */
    public boolean doSave() {

        if (fDocument.getFile() == null) {
            return doSaveAs(fDocument.getFormat());
        }
        
        return fDocument.save();
    }

    /**
     * Print the contents of this window.
     */
    public void doPrint() {

        PrintingUtils.userPrintText(fDocument.getText(),
                                    fTextPanel.getDefaultValues(),
                                    this,
                                    this.getTitle());
    }

    /**
     * Attempt to close this window.  If the text has not been saved,
     * give the user a chance to save the text before closing the
     * window.  If the user cancels this operation, this method returns
     * false and the window is not closed;  otherwise this method
     * returns true and the window is closed.
     */
    public boolean doClose() {

        if (canChangeDocuments()) {
            setVisible(false);
            dispose();
            fApplication.removeDocumentWindow(this);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Display a dialog that asks whether the user wants to
     * save a document.  The returned value will be YES_OPTION,
     * NO_OPTION, or CANCEL_OPTION from JOptionPane.
     */
    private int askSave() {

        String pattern = ResourceUtils.getString(EditorResources.SAVE_MSG);
        String message = MessageFormat.format(pattern, new Object[] {getTitle()});
        
        String yes = ResourceUtils.getString(EditorResources.YES);
        String no = ResourceUtils.getString(EditorResources.NO);
        String cancel = ResourceUtils.getString(EditorResources.CANCEL);

        return JOptionPane.showOptionDialog(this,
                                            message,
                                            "",
                                            JOptionPane.YES_NO_CANCEL_OPTION,
                                            JOptionPane.QUESTION_MESSAGE,
                                            null,
                                            new Object[]{yes,no,cancel},
                                            yes);
    }
}