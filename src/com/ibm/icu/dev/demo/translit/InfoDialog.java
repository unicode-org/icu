/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.translit;
import java.awt.event.*;
import java.awt.*;
public class InfoDialog extends Dialog {
    protected Button button;
    protected TextArea area;
    protected Dialog me;
    protected Panel bottom;
        
    public TextArea getArea() {
        return area;
    }
        
    public Panel getBottom() {
        return bottom;
    }
        
    InfoDialog(Frame parent, String title, String label, String message) {
        super(parent, title, false);
        me = this;
        this.setLayout(new BorderLayout());
        if (label.length() != 0) {
            this.add("North", new Label(label));
        }
            
        area = new TextArea(message, 8, 80, TextArea.SCROLLBARS_VERTICAL_ONLY);
        this.add("Center", area);
            
        button = new Button("Hide");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                me.hide();
            }
        });
        bottom = new Panel();
        bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.add(button);
        this.add("South", bottom);
        this.pack();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                me.hide();
            }
        });
    }
}
