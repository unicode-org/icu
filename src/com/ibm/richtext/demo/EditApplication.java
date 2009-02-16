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

import java.awt.Frame;
import java.awt.datatransfer.Clipboard;

import java.text.MessageFormat;

import java.util.Vector;

public abstract class EditApplication {

    private Clipboard fClipboard;
    private int fDefaultFormat;
    private Vector fWindows = new Vector();
    private int fUntitledCount = 0;
    
    protected EditApplication(Clipboard clipboard, int defaultFormat) {
        
        fClipboard = clipboard;
        fDefaultFormat = defaultFormat;
    }
    
    /**
     * New documents are named "Untitled 1", "Untitled 2", etc.  This
     * method returns the appropriate name for the next new document.
     * @return the next new document name
     */
    private String getNextNewName() {

        fUntitledCount += 1;
        String pattern = ResourceUtils.getString(EditorResources.UNTITLED_MSG);
        return MessageFormat.format(pattern,
                                    new Object[]{new Integer(fUntitledCount)});
    }
    
    public final Clipboard getClipboard() {
        
        return fClipboard;
    }

    protected abstract DocumentWindow createDocumentWindow(TextDocument document);
    
    public abstract TextDocument openDocument(Frame dialogParent);
    
    public final TextDocument createNewDocument() {
    
        String name = getNextNewName();
        int format = fDefaultFormat;
        return TextDocument.createEmpty(name, format);
    }
    
    public final void doNewWindow() {
        
        addDocument(TextDocument.createEmpty(getNextNewName(), fDefaultFormat));
    }
    
    public final void addDocument(TextDocument document) {
        
        final DocumentWindow window = createDocumentWindow(document);

        window.setSize(500, 400);
        window.show();
        fWindows.addElement(window);
    }
    
    /**
     * Remove document from list of documents.  Quit application if list
     * length falls to zero.
     * @param window window of the document to remove
     */
    public final void removeDocumentWindow(DocumentWindow window) {

        fWindows.removeElement(window);
        if (fWindows.isEmpty()) {
            quit();
        }
    }

    /**
     * Go through list of documents and attempt to close each document.
     * If all documents close successfully, then exit.
     */
    public final void doExit() {

        // Clone fWindows since it can get modified while being traversed.
        Vector windows = (Vector) fWindows.clone();

        int size = windows.size();
        for (int i=0; i < size; i++) {
            DocumentWindow window = (DocumentWindow) windows.elementAt(i);
            if (!window.doClose()) {
                return;
            }
        }

        // quit will be called when last document removes itself
    }

    /**
     * Called when last document window closes.  Default implementation
     * calls System.exit.
     */
    protected void quit() {

        System.exit(0);
    }
}
