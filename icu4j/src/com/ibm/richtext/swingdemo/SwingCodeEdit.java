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
package com.ibm.richtext.swingdemo;

import com.ibm.richtext.demo.CodeEdit;
import com.ibm.richtext.demo.DocumentWindow;
import com.ibm.richtext.demo.SyntaxColorer;
import com.ibm.richtext.demo.TextDocument;

public class SwingCodeEdit extends CodeEdit {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    public static void main(String[] args) {
    
        new SwingCodeEdit(args, 0);
    }
    
    public SwingCodeEdit(String[] args, int start) {
    
        super(args, start);
    }
    
    protected DocumentWindow createDocumentWindow(TextDocument document) {

        return new SwingDocumentWindow(this, 
                                       document,
                                       fSettings,
                                       false,
                                       new SyntaxColorer(),
                                       false,
                                       true,
                                       menus);
    }
}