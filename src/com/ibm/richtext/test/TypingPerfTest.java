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
package com.ibm.richtext.test;

import java.awt.Button;
import java.awt.GridLayout;
import java.awt.Frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

import java.text.DateFormat;
import java.util.Date;

import com.ibm.richtext.textpanel.KeyEventForwarder;
import com.ibm.richtext.textpanel.TextPanel;
import com.ibm.richtext.awtui.TextFrame;
import com.ibm.richtext.styledtext.MConstText;

import com.ibm.richtext.demo.EditDemo;
import com.ibm.richtext.demo.TextDocument;

public class TypingPerfTest implements ActionListener {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private TextFrame fTextFrame;
    private KeyEventForwarder fKeyEventForwarder;
    private PrintWriter fOut;

    private static final String fgAtStartCommand = "Insert at start";
    private static final String fgAtEndCommand = "Insert at end";
    private static final String fgFwdDelete = "Forward delete";
    private static final String fgBackspace = "Backspace";
    private static final String fgAtCurrentPosCommand = "Insert at current position";
    private static final String fgLotsOfTextCommand = "Insert a lot of text";

    private static final String USAGE = "Usage: java com.ibm.richtext.test.TypingPerfTest [file] [-insertionText text]";
    private char[] fInsText;

    public static void main(String[] args) throws IOException {

        // not used OutputStream outStream = null;
        PrintWriter writer = new PrintWriter(System.out);

        MConstText text = Declaration.fgDeclaration;
        char[] insText = "The quick brown fox jumps over the lazy dog. The end. ".toCharArray();
        
        int index = 0;
        while (index < args.length) {
            if (args[index].equals("-insertionText")) {
                if (args.length == ++index) {
                    throw new Error(USAGE);
                }
                insText = args[index++].toCharArray();
            }
            else {
                // This will try MConstText first, then plain text.
                TextDocument doc = EditDemo.getDocumentFromFile(new File(args[index++]));
                if (doc == null) {
                    throw new Error("Couldn't open file "+args[index-1]);
                }
                text = doc.getText();
            }
        }
        
        if (index != args.length) {
            throw new Error(USAGE);
        }
        
        new TypingPerfTest(writer, text, insText);
    }

    public TypingPerfTest(PrintWriter out, MConstText text, char[] insText) throws IOException {

        fInsText = insText;
        fTextFrame = new TextFrame(text, "", null);
        TextPanel textPanel = (TextPanel) fTextFrame.getTextPanel();
        fKeyEventForwarder = new KeyEventForwarder(textPanel);
        fOut = out;

        DateFormat df = DateFormat.getDateTimeInstance();
        out.println("Test date: " + df.format(new Date()));

        fTextFrame.setSize(500, 700);
        fTextFrame.show();

        Frame f = new Frame("Typing Perf Test");
        f.setLayout(new GridLayout(0, 1));
        Button b;
/*
        b = new Button(fgAtStartCmd);
        b.addActionListener(this);
        f.add(b);

        b = new Button(fgAtEndCmd);
        b.addActionListener(this);
        f.add(b);
*/
        b = new Button(fgAtCurrentPosCommand);
        b.addActionListener(this);
        f.add(b);

        b = new Button(fgLotsOfTextCommand);
        b.addActionListener(this);
        f.add(b);

        b = new Button(fgFwdDelete);
        b.addActionListener(this);
        f.add(b);

        b = new Button(fgBackspace);
        b.addActionListener(this);
        f.add(b);

        f.doLayout();
        WindowAdapter closer = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                fOut.close();
                System.exit(0);
            }
        };
        
        f.addWindowListener(closer);
        fTextFrame.addWindowListener(closer);
        
        f.setSize(200, 80);
        f.show();
    }

    public void actionPerformed(ActionEvent evt) {

        try {
            if (evt.getActionCommand().equals(fgAtCurrentPosCommand)) {

                insertAtCurrentPos(1);
            }
            else if (evt.getActionCommand().equals(fgLotsOfTextCommand)) {

                insertAtCurrentPos(8);
            }
            else if (evt.getActionCommand().equals(fgFwdDelete)) {

                forwardDelete(1);
            }
            else if (evt.getActionCommand().equals(fgBackspace)) {

                backspace(1);
            }
        }
        catch(IOException e) {
            System.out.println("Caught exception: " + e);
        }
    }

    private void insertAtCurrentPos(final int times) throws IOException {

        fTextFrame.toFront();

        System.gc();

        long startTime = System.currentTimeMillis();

        for (int t=0; t < times; t++) {
            for (int i=0; i < fInsText.length; i++) {

                KeyEvent event = new KeyEvent(fTextFrame, KeyEvent.KEY_TYPED, 0, 0, 0, fInsText[i]);
                fKeyEventForwarder.handleKeyEvent(event);
            }
        }

        long time = System.currentTimeMillis() - startTime;

        fOut.println("Total time: " + time);
        fOut.println("Millis per character: " + (time / (fInsText.length*times)));
        fOut.flush();
    }

    private void forwardDelete(final int times) throws IOException {

        System.gc();

        long startTime = System.currentTimeMillis();

        for (int t=0; t < times; t++) {
            for (int i=0; i < fInsText.length; i++) {

                KeyEvent event = new KeyEvent(fTextFrame, 0, 0, 0, KeyEvent.VK_DELETE, '\u00FF');
                fKeyEventForwarder.handleKeyEvent(event);
            }
        }

        long time = System.currentTimeMillis() - startTime;

        fOut.println("Total time: " + time);
        fOut.println("Millis per character: " + (time / (fInsText.length*times)));
        fOut.flush();
    }

    private void backspace(final int times) throws IOException {

        System.gc();

        long startTime = System.currentTimeMillis();

        for (int t=0; t < times; t++) {
            for (int i=0; i < fInsText.length; i++) {

                KeyEvent event = new KeyEvent(fTextFrame, 0, 0, 0, KeyEvent.VK_BACK_SPACE, '\u0010');
                fKeyEventForwarder.handleKeyEvent(event);
            }
        }

        long time = System.currentTimeMillis() - startTime;

        fOut.println("Total time: " + time);
        fOut.println("Millis per character: " + (time / (fInsText.length*times)));
        fOut.flush();
    }
}
