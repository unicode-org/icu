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
package com.ibm.richtext.demo;

import com.ibm.richtext.awtui.TabRuler;

import com.ibm.richtext.textpanel.TextPanel;
import com.ibm.richtext.textpanel.TextPanelListener;
import com.ibm.richtext.textpanel.TextPanelSettings;

import com.ibm.richtext.awtui.AwtMenuBuilder;

import com.ibm.richtext.print.PrintingUtils;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.Label;
import java.awt.Panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.text.MessageFormat;

/**
 * AwtDocumentWindow is a Frame containing a TextPanel, with a document
 * for storing the text in the TextPanel.
 */
public final class AwtDocumentWindow extends Frame implements DocumentWindow {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -8075495366541764458L;

    //static final String COPYRIGHT =
    //            "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private TextPanel fTextPanel;
    private EditApplication fApplication;
    private TextDocument fDocument;

    /**
     * Create a new AwtDocumentWindow.
     * @param application the application that owns this document
     * @param clipboard the clipboard to use
     * @param document the document to show in this AwtDocumentWindow
     */
    AwtDocumentWindow(EditApplication application,
                   TextDocument document,
                   TextPanelSettings textPanelSettings,
                   boolean useTabRuler,
                   TextPanelListener listener,
                   boolean supportStyledText,
                   boolean supportPlainText,
                   int[] menus) {

        fApplication = application;
        
        fTextPanel = new TextPanel(textPanelSettings, null, application.getClipboard());
        if (listener != null) {
            fTextPanel.addListener(listener);
        }
        setDocument(document);

        addMenuBar(supportStyledText, supportPlainText, menus);

        setLayout(new BorderLayout());

        if (useTabRuler) {
            TabRuler tabRuler = new TabRuler(14, 10, fTextPanel);
            add(tabRuler, "North");
        }
        
        add(fTextPanel, "Center");
        pack();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });
    }

    private void addMenuBar(boolean supportStyledText, 
                            boolean supportPlainText,
                            int[] menus) {
        
        MenuBar menuBar = new MenuBar();
        String menuTitle = ResourceUtils.getString(EditorResources.FILE);
        Menu menu = new Menu(menuTitle);
        new AwtFileMenuManager(menu, fApplication, this,
                                supportStyledText, supportPlainText);
        menuBar.add(menu);

        AwtMenuBuilder.getInstance().createMenus(menuBar, fTextPanel, this, menus);
        setMenuBar(menuBar);
    }
    
    /**
     * Return true if it is OK to set the document text and file to
     * something different.
     */
    private boolean canChangeDocuments() {

        // If the text is modified, give the user a chance to
        // save it.  Otherwise return true.

        if (fDocument.isModified()) {
            byte save = askSave(this, getTitle());
            if (save == YES) {
                return doSave();
            }
            else {
                return save == NO;
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
        File file = getFileFromDialog(fDocument.getFile(), title, this, FileDialog.SAVE);
        
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
     * Retrieve a file from a dialog.  If the user does not
     * select a file in the dialog this method returns null.
     * @param kind either FileDialog.LOAD or FileDialog.SAVE.
     */
    public static File getFileFromDialog(File origFile,
                                         String dialogTitle, 
                                         Frame owner,
                                         int kind) {
        
        FileDialog dialog = new FileDialog(owner, 
                                           dialogTitle,
                                           kind);
        if (origFile != null) {
            dialog.setDirectory(origFile.getParent());
            dialog.setFile(origFile.getName());
        }
        dialog.show();
        String fileStr = dialog.getFile();
        String dirStr = dialog.getDirectory();
        
        File file = null;
        
        if (fileStr != null) {
            file = new File(dirStr, fileStr);
        }
        
        return file;
    }
    
    private static final byte YES = 0;
    private static final byte NO = 1;
    private static final byte CANCEL = 2;

    private static final class DialogListener implements ActionListener {

        Dialog fDialog;
        Button fYes, fNo, fCancel;
        byte fState;

        DialogListener(Dialog dialog,
                       Button yes,
                       Button no,
                       Button cancel) {

            fDialog = dialog;
            fYes = yes;
            fNo = no;
            fCancel = cancel;
            fYes.addActionListener(this);
            fNo.addActionListener(this);
            fCancel.addActionListener(this);
            fState = -1;
        }

        public void actionPerformed(ActionEvent event) {

            Object source = event.getSource();
            if (source == fYes) {
                fState = YES;
            }
            else if (source == fNo) {
                fState = NO;
            }
            else if (source == fCancel) {
                fState = CANCEL;
            }
            else {
                return;
            }

            fDialog.dispose();
        }

        byte getState() {

            return fState;
        }
    }

    /**
     * Display a dialog that asks whether the user wants to
     * save a document.  Possible reponses are Yes, No, and
     * Cancel.  The returned value indicates which response
     * was chosen.
     */
    private static byte askSave(Frame parent, String fileName) {

        Dialog dialog = new Dialog(parent, true);
        dialog.setLayout(new GridLayout(0, 1));
        
        String pattern = ResourceUtils.getString(EditorResources.SAVE_MSG);
        String text = MessageFormat.format(pattern, new Object[] {fileName});
        dialog.add(new Label(text, Label.CENTER));
        
        Button yes = new Button(ResourceUtils.getString(EditorResources.YES));
        Button no = new Button(ResourceUtils.getString(EditorResources.NO));
        Button cancel = new Button(ResourceUtils.getString(EditorResources.CANCEL));

        Panel panel = new Panel();
        panel.add(yes);
        panel.add(no);
        panel.add(cancel);
        dialog.add(panel);

        DialogListener listener = new DialogListener(dialog, yes, no, cancel);

        dialog.setSize(220, 130);
        dialog.show();

        return listener.getState();
    }
}