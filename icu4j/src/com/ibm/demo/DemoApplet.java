/*
 * $RCSfile: DemoApplet.java,v $ $Revision: 1.2 $ $Date: 2000/02/24 19:53:00 $
 *
 * (C) Copyright Taligent, Inc. 1996 - 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * Portions copyright (c) 1996 Sun Microsystems, Inc. All Rights Reserved.
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

package com.ibm.demo;

import java.applet.Applet;
import java.util.Locale;
import java.awt.*;
import java.awt.event.*;

public abstract class DemoApplet extends java.applet.Applet {
    private Button   demoButton;
    private Frame    demoFrame;
	private static int demoFrameCount = 0;

    protected abstract Frame createDemoFrame(DemoApplet applet);
    protected Dimension getDefaultFrameSize(DemoApplet applet, Frame f) {
    	return new Dimension(700, 550);
    }

    //Create a button that will display the demo
    public void init()
    {
        setBackground(Color.white);
        demoButton = new Button("Demo");
        demoButton.setBackground(Color.yellow);
        add( demoButton );

        demoButton.addActionListener( new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                if (e.getID() == ActionEvent.ACTION_PERFORMED) {
                    demoButton.setLabel("loading");

                    if (demoFrame == null) {
                       demoFrame = createDemoFrame(DemoApplet.this);
                       showDemo();
                    }

                    demoButton.setLabel("Demo");
                }
             }
        } );
    }

    public void showDemo()
    {
    	demoFrame = createDemoFrame(this);
        demoFrame.layout();
        Dimension d = getDefaultFrameSize(this, demoFrame);
        demoFrame.resize(d.width, d.height);
        demoFrame.show();
		demoFrameOpened();
    }

    public void demoClosed()
    {
        demoFrame = null;
		demoFrameClosed();
    }

	protected static void demoFrameOpened() {
		demoFrameCount++;
    }
	protected static void demoFrameClosed() {
		if (--demoFrameCount == 0) {
			System.exit(0);
		}
    }
}

