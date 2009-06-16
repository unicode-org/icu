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

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

import com.ibm.richtext.textpanel.TextPanel;
import com.ibm.richtext.textpanel.TextPanelSettings;
import com.ibm.richtext.awtui.AwtMenuBuilder;

public class CodeEdit extends EditApplication {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    
    protected final TextPanelSettings fSettings;
    
    public static synchronized void main(String[] args) {

        if (args.length > 0 && args[0].equals("-swing")) {
            new com.ibm.richtext.swingdemo.SwingCodeEdit(args,1);
        }
        else {
            new CodeEdit(args, 0);
        }
    }

    protected CodeEdit(String[] args, int start) {

        super(Toolkit.getDefaultToolkit().getSystemClipboard(),
              TextDocument.PLAIN_TEXT);

        AttributeMap defaultStyle = new AttributeMap(TextAttribute.SIZE, new Float(12))
                                       .addAttribute(TextAttribute.FAMILY, "Monospaced");
                                       
        fSettings = TextPanel.getDefaultSettings();
        fSettings.setWraps(false);
        fSettings.addDefaultValues(defaultStyle);
        
        if (args.length == start) {
            doNewWindow();
        }
        else {
            boolean openedADocument = false;
            for (int i=start; i < args.length; i++) {

                File file = new File(args[i]);
                TextDocument document = null;
                Throwable error = null;
                try {
                    document = TextDocument.createFromFile(file, TextDocument.PLAIN_TEXT);
                }
                catch(Exception e) {
                    error = e;
                }
                
                if (error != null) {
                    error.printStackTrace();
                }
                else {
                    addDocument(document);
                    openedADocument = true;
                }
            }
            if (!openedADocument) {
                quit();
            }
        }
    }
    
    protected DocumentWindow createDocumentWindow(TextDocument document) {

        return new AwtDocumentWindow(this, 
                                  document,
                                  fSettings,
                                  false,
                                  new SyntaxColorer(),
                                  false,
                                  true,
                                  menus);
    }
    
    protected static final int[] menus = { AwtMenuBuilder.EDIT, 
                                           AwtMenuBuilder.BIDI,
                                           AwtMenuBuilder.ABOUT };

    public TextDocument openDocument(Frame dialogParent) {
    
        String title = ResourceUtils.getString(EditorResources.OPEN_TITLE);
        
        File file = AwtDocumentWindow.getFileFromDialog(null, title, dialogParent, FileDialog.LOAD);
        if (file != null) {
            try {
                return TextDocument.createFromFile(file, TextDocument.PLAIN_TEXT);
            }
            catch(Exception e) {
                System.out.print("");
            }
        }
        return null;
    }
}