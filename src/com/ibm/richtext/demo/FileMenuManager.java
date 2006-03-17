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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class creates a File menu and manages user interactions
 * with the menu.
 */
public abstract class FileMenuManager implements ActionListener {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private EditApplication fApplication;
    private DocumentWindow fDocumentWindow;
    private Object fNew, fNewWindow, fOpen, fSave;
    private Object fSaveAsStyled, fSaveAsText, fClose, fPrint, fExit;

    protected FileMenuManager(EditApplication application, 
                              DocumentWindow document) {

        fApplication = application;
        fDocumentWindow = document;
    }
    
    protected final void createItems(boolean supportStyledFormat,
                                     boolean supportPlainFormat) {
        
        if (!supportStyledFormat && !supportPlainFormat) {
            throw new IllegalArgumentException("Must support at least one format.");
        }
        
        fNew = addMenuItem(EditorResources.NEW);
        fNewWindow = addMenuItem(EditorResources.NEW_WINDOW);
        
        addSeparator();

        fOpen = addMenuItem(EditorResources.OPEN);

        fSave = addMenuItem(EditorResources.SAVE);
        
        if (supportStyledFormat) {
            if (supportPlainFormat) {
                fSaveAsStyled = addMenuItem(EditorResources.SAVE_AS_STYLED);
                fSaveAsText = addMenuItem(EditorResources.SAVE_AS_TEXT);
            }
            else {
                fSaveAsStyled = addMenuItem(EditorResources.SAVE_AS);
            }
        }
        else {
            fSaveAsText = addMenuItem(EditorResources.SAVE_AS);
        }
        
        addSeparator();
        fClose = addMenuItem(EditorResources.CLOSE);
        addSeparator();
        fPrint = addMenuItem(EditorResources.PRINT);
        addSeparator();
        fExit = addMenuItem(EditorResources.EXIT);
    }
    
    protected abstract Object addMenuItem(String key);

    protected abstract void addSeparator();
    
    public final void actionPerformed(ActionEvent event) {

        Object source = event.getSource();

        if (source == fNew) {
            fDocumentWindow.doNew();
        }
        else if (source == fNewWindow) {
            fApplication.doNewWindow();
        }
        else if (source == fOpen) {
            fDocumentWindow.doOpen();
        }
        else if (source == fClose) {
            fDocumentWindow.doClose();
        }
        else if (source == fSave) {
            fDocumentWindow.doSave();
        }
        else if (source == fSaveAsStyled) {
            fDocumentWindow.doSaveAs(TextDocument.STYLED_TEXT);
        }
        else if (source == fSaveAsText) {
            fDocumentWindow.doSaveAs(TextDocument.PLAIN_TEXT);
        }
        else if (source == fPrint) {
            fDocumentWindow.doPrint();
        }
        else if (source == fExit) {
            fApplication.doExit();
        }
        else {
            throw new Error("Unknown event source: " + source);
        }
    }
}