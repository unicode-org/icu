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
package com.ibm.richtext.awtui;

import java.util.Hashtable;

import com.ibm.richtext.textlayout.attributes.AttributeSet;
import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.uiimpl.MenuItemSet;

import com.ibm.richtext.uiimpl.resources.FrameResources;
import com.ibm.richtext.uiimpl.ResourceUtils;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Label;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

/**
* Simple dialog which gets a color
*/
final class ObjectDialog extends Dialog implements ActionListener
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private final MTextPanel fTextPanel;
    private final Object fKey;
    private boolean fCharacter;

    private final Button fOKButton;
    private final Button fCancelButton;
    private final Choice fItems;
    private final Hashtable fNameToValueMap;
    /**
    * Construct a new ColorDialog.
    * @param parent the dialog's parent frame
    * @param title the dialogs title
    * @param message the message displayed next to the input box
    */
    ObjectDialog(Frame parent, 
                 String title, 
                 String message, 
                 MTextPanel textPanel,
                 Object key,
                 boolean character,
                 String[] names,
                 Object[] values) {
                    
        super(parent, title, false);
        fTextPanel = textPanel;
        fKey = key;
        fCharacter = character;
        
        setLayout(new GridLayout(2, 1));

        Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        fItems = new Choice();

        if (names.length != values.length) {
            throw new IllegalArgumentException("Must have same number of names and values.");
        }

        fNameToValueMap = new Hashtable(names.length);
        
        for (int i=0; i < names.length; i++) {
            fItems.add(names[i]);
            if (values[i] != null) {
                fNameToValueMap.put(names[i], values[i]);
            }
        }

        panel.add(new Label(message));        
        panel.add(fItems);
        
        add("North", panel);

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
        if (sendAction) {
            Object value = fNameToValueMap.get(fItems.getSelectedItem());
            sendAction(value);
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
    * @param obj the value object
    */
    private void sendAction(Object value) {

        StyleModifier modifier;
        if (value != null) {
            modifier = StyleModifier.createAddModifier(fKey, value);
        }
        else {
            AttributeSet set = new AttributeSet(fKey);
            modifier = StyleModifier.createRemoveModifier(set);
        }
        
        if (fCharacter == MenuItemSet.CHARACTER) {
            fTextPanel.modifyCharacterStyleOnSelection(modifier);
        }
        else {
            fTextPanel.modifyParagraphStyleOnSelection(modifier);
        }
    }
}
