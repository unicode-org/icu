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
package com.ibm.richtext.textapps;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.awtui.TextFrame;

import java.awt.Toolkit;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class BidiDemo {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final AppCloser fgListener = new AppCloser();

    private static final String BUNDLE_NAME = "com.ibm.richtext.textapps.resources.Sample";
    
    public static void main(String[] args) {

        String docName;
        
        if (args.length == 0) {
            docName = "default";
        }
        else {
            docName = args[0];
        }
        
        openText(docName);
    }

    private static void openText(String docName) {

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME);

            Object document = bundle.getObject(docName+".sample");
            MConstText text;
            
            if (document instanceof String) {
                text = new StyledText((String)document, 
                                      AttributeMap.EMPTY_ATTRIBUTE_MAP);
            }
            else {
                URL url = (URL) document;
                ObjectInputStream in = new ObjectInputStream(url.openStream());
                text = (MConstText) in.readObject();
            }
            
            String name = bundle.getString(docName+".name");
            
            makeFrame(text, name);
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private static void makeFrame(MConstText text, String title) {

        TextFrame frame = new TextFrame(text, title, 
                Toolkit.getDefaultToolkit().getSystemClipboard());
        frame.setSize(550, 700);
        frame.show();
        fgListener.listenToFrame(frame);
    }
}
