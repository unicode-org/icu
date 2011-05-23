/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import com.ibm.rbm.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/** A dialog displaying information about this application */
class AboutDialog {
	private static JDialog dialog = null;
	
	public static void showDialog(Frame parent) {
		if (dialog == null) {
			dialog = new JDialog(parent, Resources.getTranslation("dialog_title_about_rbmanager"), false);
			initComponents();
		}
		dialog.setVisible(true);
	}
	
	private static void initComponents() {
		dialog.getContentPane().setLayout(new BorderLayout());
        JLabel logoLabel = null;
		JLabel titleLabel = new JLabel(Resources.getTranslation("rbmanager"));
		JLabel versionLabel = new JLabel(Resources.getTranslation("version", Package.getPackage("com.ibm.rbm").getImplementationVersion()));
		JLabel copyrightLabel = new JLabel(Resources.getTranslation("copyright"));
		JLabel contactLabel = new JLabel(Resources.getTranslation("rbmanager_contact"));
		JPanel panel = new JPanel();
		Box box = new Box(BoxLayout.Y_AXIS);
		
        try {
            Class thisClass = Class.forName("com.ibm.rbm.gui.AboutDialog");
		    logoLabel = new JLabel(new ImageIcon(thisClass.getResource("images/" +
									      Resources.getTranslation("logo_filename"))));
        } catch (ClassNotFoundException e) {
            RBManagerGUI.debugMsg(e.toString());
        }

		box.add(titleLabel);
		box.add(versionLabel);
		box.add(Box.createVerticalStrut(10));
		box.add(copyrightLabel);
		box.add(Box.createVerticalStrut(5));
		box.add(contactLabel);
		
		panel.add(box);
		dialog.getContentPane().add(logoLabel, BorderLayout.WEST);
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		
		dialog.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent ev) {
				hideDialog();
			}
		});
		
		//dialog.validate();
		dialog.pack();
		Point parentLoc = dialog.getParent().getLocation();
		dialog.setLocation(new Point(parentLoc.x + 50, parentLoc.y + 50));
		dialog.setResizable(false);
	}
	
	private static void hideDialog() {
		dialog.setVisible(false);
		dialog.dispose();
		dialog = null;
	}
}
