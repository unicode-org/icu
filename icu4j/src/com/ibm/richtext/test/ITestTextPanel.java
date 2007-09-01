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
package com.ibm.richtext.test;

import java.awt.Button;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.util.Date;
import java.text.DateFormat;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.awtui.TextFrame;

public class ITestTextPanel extends Frame implements ActionListener {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 4776220202735727574L;

    //static final String COPYRIGHT =
    //            "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static long fgOpCount = 0;

    private TestTextPanel fTest;
    
    private MTextPanel fTextPanel;
    private Frame fTextFrame;
    private Clipboard fClipboard;
    private Button fExersize, fStressTest;

    public static void main(String[] args) {

        Date startDate = new Date();

        try {
            Clipboard clipboard = new Clipboard("ITextTestPanel");
            TextFrame frame = new TextFrame(null, "Interactive Test", clipboard);
            MTextPanel panel = frame.getTextPanel();

            new ITestTextPanel(panel, frame, clipboard).show();
        }
        finally {
            DateFormat df = DateFormat.getDateTimeInstance();
            System.out.println("Start time: " + df.format(startDate));
            System.out.println("End Time: " + df.format(new Date()));
            System.out.println("Op count: " + fgOpCount);
        }
    }

    public ITestTextPanel(MTextPanel panel,
                          Frame frame,
                          Clipboard clipboard) {

        fTextPanel = panel;
        fTest = new TestTextPanel(fTextPanel);
        fClipboard = clipboard;

        setLayout(new GridLayout(0, 1));

        fTextFrame = frame;
        fTextFrame.setSize(350, 500);
        fTextFrame.show();

        // initialize UI:
        fExersize = new Button("Exercise");
        fExersize.addActionListener(this);
        add(fExersize);

        fStressTest = new Button("Stress Test");
        fStressTest.addActionListener(this);
        add(fStressTest);

        pack();

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                //activateTextFrame();
            }
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        setSize(280, 150);
    }

    private void activateTextFrame() {

        fTextFrame.toFront();
    }

    public void actionPerformed(ActionEvent event) {

        Object source = event.getSource();
        activateTextFrame();
        Date startDate = new Date();
        boolean exitedNormally = false;

        try {
            if (source == fExersize) {
                fTest.incRandSeed();
                for (int i=0; i < 100; i++) {
                    selectOperation(fTextFrame, fClipboard);
                }
            }
            else if (source == fStressTest) {
                fTest.incRandSeed();
                while (true) {
                    selectOperation(fTextFrame, fClipboard);
                }
            }
            exitedNormally = true;
        }
        finally {
            if (!exitedNormally) {
                DateFormat df = DateFormat.getDateTimeInstance();
                System.out.println("Start time: " + df.format(startDate));
                System.out.println("End Time: " + df.format(new Date()));
                System.out.println("Rand seed: " + fTest.getRandSeed());
                System.out.println("Op count: " + fgOpCount);
            }
        }
    }

    /**
     * Perform a random operation on the MTextPanel.  Frame can
     * be null.
     */
    private static final int OP_COUNT = 15;

    public void selectOperation(Frame frame,
                                Clipboard clipboard) {

        int op = fTest.randInt(OP_COUNT);

        switch (op) {

            case 0:
                fTest._testSetSelection();
                break;

            case 1:
                fTest._testModifications(TestTextPanel.MOD_TEXT,
                                        true);
                break;

            case 2:
                fTest._testEditMenuOperations(clipboard);
                break;

            case 3:
                fTest._testModFlag(fTextPanel.getCommandLogSize());
                break;

            case 4:
                fTest.applyCharacterStyle();
                break;

            case 5:
                fTest.applyParagraphStyle();
                break;

            case 6:
            case 7:
            case 8:
            case 9:
                fTest.typeKeys();
                break;

            case 10:
                fTest.selectText();
                break;

            case 11:
                fTest.undoRedo();
                break;

            case 12:
                //if (frame != null) {
                //    fTest.resizeFrame(frame);
                //    break;
                //}

            case 13:
                fTest.applyKeyRemap();
                break;

            case 14:
                fTest._testCommandLogControl();
                break;

            default:
                throw new Error("OP_COUNT is incorrect");
        }
        fgOpCount++;
    }


}
