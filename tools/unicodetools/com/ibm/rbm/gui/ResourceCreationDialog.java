/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.ibm.rbm.*;

/**
 * A dialog for creating new Resource Files
 */
class ResourceCreationDialog extends JDialog {
	RBManager rbm;
	RBManagerGUI gui;
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box infoBox = new Box(BoxLayout.Y_AXIS);
	JPanel infoPanel = new JPanel();
	JPanel infoTitlePanel = new JPanel();
	JPanel infoCommentPanel = new JPanel();
	JPanel infoManagerPanel = new JPanel();
	JPanel langPanel = new JPanel();
	JPanel counPanel = new JPanel();
	JPanel variPanel = new JPanel();
	JPanel buttPanel = new JPanel();
	
	JLabel instructionsLabel = new JLabel("");
	JLabel titleLabel = new JLabel(Resources.getTranslation("dialog_file_title"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_file_comment"));
	JLabel managerLabel = new JLabel(Resources.getTranslation("dialog_file_manager"));
	JLabel enc1Label = new JLabel(Resources.getTranslation("dialog_encoding"));
	JLabel enc2Label = new JLabel(Resources.getTranslation("dialog_encoding"));
	JLabel enc3Label = new JLabel(Resources.getTranslation("dialog_encoding"));
	JLabel nam1Label = new JLabel(Resources.getTranslation("dialog_name"));
	JLabel nam2Label = new JLabel(Resources.getTranslation("dialog_name"));
	JLabel nam3Label = new JLabel(Resources.getTranslation("dialog_name"));
	
	JTextField titleField = new JTextField("");
	JTextField commentField = new JTextField("");
	JTextField managerField = new JTextField("");
	JTextField enc1Field = new JTextField("");
	JTextField enc2Field = new JTextField("");
	JTextField enc3Field = new JTextField("");
	JTextField nam1Field = new JTextField("");
	JTextField nam2Field = new JTextField("");
	JTextField nam3Field = new JTextField("");
	
	JCheckBox copyCheckBox = new JCheckBox(Resources.getTranslation("dialog_checkbox_copy_elements"), true);
	
	JButton createButton = new JButton(Resources.getTranslation("button_create"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	public ResourceCreationDialog(RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.gui = (RBManagerGUI)frame;
		this.rbm = rbm;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			boolean success = createResource();
			if (!success) {
				String alert = Resources.getTranslation("error_create_file") + " " +
							   Resources.getTranslation("error_try_again_file");
				JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			} else {
				setVisible(false);
				dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	boolean createResource() {
		if (rbm == null) return false;
		String encoding = enc1Field.getText().trim();
		String enc2 = enc2Field.getText().trim();
		if (enc2 != null && !enc2.equals("")) encoding += "_" + enc2;
		String enc3 = enc3Field.getText().trim();
		if (enc3 != null && !enc3.equals("")) encoding += "_" + enc3;
		boolean ret = rbm.createResource(titleField.getText().trim(), commentField.getText().trim(), managerField.getText().trim(),
							encoding, nam1Field.getText().trim(), nam2Field.getText().trim(),
							nam3Field.getText().trim(), copyCheckBox.isSelected());
		if (ret) { 
			gui.updateDisplayTree();
			gui.updateProjectTree();
			gui.updateProjectPanels();
		}
		return ret;
	}
	
	public void initComponents(){
		// Error check
		if (rbm == null) {
			String alert = Resources.getTranslation("error_no_bundle_for_file");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		int tempWidth = 175;
		Dimension labelDim = new Dimension(tempWidth, 30);
		titleLabel.setPreferredSize(labelDim);
		commentLabel.setPreferredSize(labelDim);
		managerLabel.setPreferredSize(labelDim);
		
		infoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_file_info")));
		langPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_language")));
		counPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_country")));
		variPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_variant")));
		
		titleField.setColumns(30);
		commentField.setColumns(30);
		managerField.setColumns(30);
		
		enc1Field.setColumns(3);
		enc2Field.setColumns(3);
		enc3Field.setColumns(3);
		nam1Field.setColumns(20);
		nam2Field.setColumns(20);
		nam3Field.setColumns(20);
		
		// Set up the components
		infoTitlePanel.add(titleLabel); infoTitlePanel.add(titleField);
		infoCommentPanel.add(commentLabel); infoCommentPanel.add(commentField);
		infoManagerPanel.add(managerLabel); infoManagerPanel.add(managerField);
		infoBox.add(infoTitlePanel);
		infoBox.add(infoCommentPanel);
		infoBox.add(infoManagerPanel);
		infoPanel.add(infoBox);
		
		langPanel.add(enc1Label); langPanel.add(enc1Field); langPanel.add(nam1Label); langPanel.add(nam1Field);
		counPanel.add(enc2Label); counPanel.add(enc2Field); counPanel.add(nam2Label); counPanel.add(nam2Field);
		variPanel.add(enc3Label); variPanel.add(enc3Field); variPanel.add(nam3Label); variPanel.add(nam3Field);
		
		buttPanel.add(createButton); buttPanel.add(cancelButton);
		
		// Add the appropriate listeners
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				ResourceCreationDialog dialog =
					(ResourceCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
				boolean success = dialog.createResource();
				if (!success) {
					String alert = Resources.getTranslation("error_create_file") + " " +
								   Resources.getTranslation("error_try_again_file");
					JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				} else {
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		});
		getRootPane().setDefaultButton(createButton);
		
		// Complete the component layout
		mainBox.removeAll();
		//mainBox.add(instructionsLabel);
		mainBox.add(infoPanel);
		mainBox.add(langPanel);
		mainBox.add(counPanel);
		mainBox.add(variPanel);
		mainBox.add(copyCheckBox);
		mainBox.add(buttPanel);
		
		setLocation(new java.awt.Point(50, 50));
		getContentPane().add(mainBox, BorderLayout.CENTER);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
}

