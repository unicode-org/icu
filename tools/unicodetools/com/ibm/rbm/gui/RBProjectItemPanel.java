/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;

import com.ibm.rbm.*;

/**
 */
class RBProjectItemPanel extends JPanel implements ActionListener {
	RBManagerGUI gui;
	
	// Visual Components
	Box mainBox;
	JTextField itemFields[];
	JLabel     itemLabels[];
	JButton    commitButtons[];
	JButton    commitButton;
	JLabel     titleLabel;
	JLabel     keyLabel;
	JLabel     commentLabel;
	
	public RBProjectItemPanel(RBManagerGUI gui) {
		super();
		this.gui = gui;
		initComponents();
	}
	
	public void actionPerformed(ActionEvent ev) {
		JButton button = (JButton)ev.getSource();
		String buttonName = button.getName();
		if (buttonName == null) {
			// Save all components
			RBManager bundle = gui.getSelectedProjectBundle();
			Vector bundles = bundle.getBundles();
			for (int i=0; i < itemFields.length; i++) {
				String encoding = commitButtons[i].getName();
				String translation = itemFields[i].getText();
				String key = itemFields[i].getName();
				for (int j=0; j < bundles.size(); j++) {
					Bundle rbundle = (Bundle)bundles.elementAt(j);
					if (rbundle.encoding.equals(encoding)) {
						BundleItem item = rbundle.getBundleItem(key);
						if (item != null) item.setTranslation(translation);
						break;
					}
				}
			}
			gui.saveResources(bundle);
		} else {
			// Save a particular encoding
			String encoding = buttonName;
			RBManager bundle = gui.getSelectedProjectBundle();
			int index = -1;
			for (int i=0; i < commitButtons.length; i++) {
				if (commitButtons[i] == button) {
					index = i;
					break;
				}
			}
			String translation = itemFields[index].getText();
			String key = itemFields[index].getName();
			Vector bundles = bundle.getBundles();
			for (int i=0; i < bundles.size(); i++) {
				Bundle rbundle = (Bundle)bundles.elementAt(i);
				if (rbundle.encoding.equals(encoding)) {
					BundleItem item = rbundle.getBundleItem(key);
					if (item != null) {
						item.setTranslation(translation);
						RBManagerGUI.debugMsg("Set translation to : " + translation);
					}
					else
					    RBManagerGUI.debugMsg("Item was null");
					break;
				} 
				RBManagerGUI.debugMsg("Compared " + rbundle.encoding + " with " + encoding);
			}
			gui.saveResources(bundle, encoding);
		}
		updateComponents();
	}
	
	private void initComponents() {
		setLayout(new BorderLayout());
		JPanel topPanel = new JPanel(new GridLayout(2,1));
		titleLabel = new JLabel(Resources.getTranslation("project_panel_default_title"), SwingConstants.CENTER);
		titleLabel.setFont(new Font("serif",Font.BOLD,18));
		JPanel commentPanel = new JPanel(new GridLayout(2,1));
		JLabel commentLabel2 = new JLabel(Resources.getTranslation("project_panel_comment"), SwingConstants.LEFT);
		commentLabel = new JLabel(Resources.getTranslation("project_panel_comment_none"), SwingConstants.LEFT);
		commentPanel.add(commentLabel2);
		commentPanel.add(commentLabel);
		topPanel.add(titleLabel);
		topPanel.add(commentPanel);
		JPanel centerPanel = new JPanel(new BorderLayout());
		mainBox = new Box(BoxLayout.Y_AXIS);
		JScrollPane scrollPane = new JScrollPane(mainBox,
												ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
												ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		centerPanel.add(scrollPane, BorderLayout.NORTH);
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		commitButton = new JButton(Resources.getTranslation("project_panel_commit_button_all"));
		commitButton.addActionListener(this);
		botPanel.add(commitButton);
		add(topPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(botPanel, BorderLayout.SOUTH);
		
		updateComponents();
	}
	
	public void updateComponents() {
		BundleItem item = gui.getSelectedProjectBundleItem();
		
		if (item == null) {
			commentLabel.setText(Resources.getTranslation("project_panel_comment_none"));
			titleLabel.setText(Resources.getTranslation("project_panel_default_title"));
			itemFields = null;
			itemLabels = null;
			commitButtons = null;
			commitButton.setEnabled(false);
		} else {
			String comment = item.getComment();
			String key = item.getKey();
			commentLabel.setText(comment);
			titleLabel.setText(Resources.getTranslation("project_panel_title", key));
			
			RBManager manager = gui.getSelectedProjectBundle();
			Vector bundles = manager.getBundles();
			itemFields = new JTextField[bundles.size()];
			itemLabels = new JLabel[bundles.size()];
			commitButtons = new JButton[bundles.size()];
			for (int i=0; i < bundles.size(); i++) {
				Bundle bundle = (Bundle)bundles.elementAt(i);
				BundleItem bundleItem = bundle.getBundleItem(key);
				//boolean translated = bundleItem.isTranslated();
				JLabel encodingLabel = new JLabel(Resources.getTranslation("project_panel_bundle", bundle.toString()),
												  SwingConstants.LEFT);
				if (bundleItem == null || !bundleItem.isTranslated()) {
				    encodingLabel.setText(Resources.getTranslation("project_panel_bundle_untranslated",
																	bundle.toString()));
				}
				String fieldText = (bundleItem == null ? Resources.getTranslation("project_panel_item_inherits") :
														 bundleItem.getTranslation());
				JTextField itemField = new JTextField(fieldText);
				itemField.setMaximumSize(new Dimension(this.getSize().width-150, 200));
				itemField.setName(key);
				JButton commitItemButton = new JButton(Resources.getTranslation("project_panel_commit_button"));
				commitItemButton.addActionListener(this);
				commitItemButton.setName(bundle.encoding);
				itemFields[i] = itemField;
				itemLabels[i] = encodingLabel;
				commitButtons[i] = commitItemButton;
			}
			commitButton.setEnabled(true);
		}
		
		mainBox.removeAll();
		if (itemFields != null) {
			for (int i=0; i < itemFields.length; i++) {
				JPanel bundlePanel = new JPanel(new BorderLayout());
				bundlePanel.setBorder(BorderFactory.createLineBorder(Color.darkGray));
				bundlePanel.add(itemLabels[i], BorderLayout.NORTH);
				bundlePanel.add(itemFields[i], BorderLayout.CENTER);
				bundlePanel.add(commitButtons[i], BorderLayout.EAST);
				mainBox.add(bundlePanel);
			}
		}
		
		revalidate();
	}
}

