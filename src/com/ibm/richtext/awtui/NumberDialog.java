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
package com.ibm.richtext.awtui;

import java.awt.FlowLayout;
import java.awt.Dialog;
import java.awt.TextField;
import java.awt.Button;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Label;

import java.text.NumberFormat;
import java.text.ParseException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import com.ibm.richtext.uiimpl.resources.FrameResources;
import com.ibm.richtext.uiimpl.MenuItemSet;
import com.ibm.richtext.uiimpl.ResourceUtils;

import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.textpanel.MTextPanel;

/**
* Simple dialog which gets a number, and sends an appropriate command
*/
final class NumberDialog extends Dialog implements ActionListener
{
    /**
     * For serialization
     */
    private static final long serialVersionUID = -2181405364377952745L;
    //static final String COPYRIGHT =
    //            "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private MTextPanel fTextPanel;
    private TextField fInput = null;
    private Button fOKButton = null;
    private Button fCancelButton = null;
    private boolean fCharacter;
    private Object fKey;
    private float fMultiplier; 

    /**
     * @param multiplier the factor by which to multiply the user's
     *        selection before creating the attribute value.   This
     *        is useful for subscripting.
     */
    NumberDialog(Frame parent,
                 String title,
                 String message,
                 MTextPanel textPanel,
                 Object key,
                 boolean character,
                 float multiplier) {

        super(parent, title, false);
        fTextPanel = textPanel;
        fKey = key;
        fCharacter = character;
        fMultiplier = multiplier;
        setLayout(new java.awt.GridLayout(2,1));

        Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 15));
        fInput = new TextField(5);
        panel.add(new Label(message));
        panel.add(fInput);
        add("Center", panel);

        fCancelButton = new Button(ResourceUtils.getResourceString(FrameResources.CANCEL));
        fOKButton = new Button(ResourceUtils.getResourceString(FrameResources.OK));
        Panel p = new Panel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        p.add(fCancelButton);
        p.add(fOKButton);
        add("South", p);

        pack();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeWindow(false);
            }
        });
        
        fOKButton.addActionListener(this);
        fCancelButton.addActionListener(this);
    }

    private void closeWindow(boolean sendAction) {

        setVisible(false);

        int num = 0;
        if (sendAction) {
            try {
                String text = fInput.getText();
                num = NumberFormat.getInstance().parse(text).intValue();
            }
            catch (ParseException exception) {
                sendAction = false;
            }
        }

        if (sendAction) {
            sendAction(num);
        }

        dispose();
    }

    public void actionPerformed(ActionEvent e) {

        Object source = e.getSource();

        if (source == fOKButton) {
            closeWindow(true);
        }
        else if (source == fCancelButton) {
            closeWindow(false);
        }
        else {
            throw new IllegalArgumentException("Invalid ActionEvent!");
        }
    }

    /**
    * Handle the user input
    * @param the number the user typed in
    */
    private void sendAction(int number) {
        float num = number * fMultiplier;
        StyleModifier modifier = StyleModifier.createAddModifier(
                                                fKey,
                                                new Float(num));
        if (fCharacter == MenuItemSet.CHARACTER) {
            fTextPanel.modifyCharacterStyleOnSelection(modifier);
        }
        else {
            fTextPanel.modifyParagraphStyleOnSelection(modifier);
        }
    }
}
