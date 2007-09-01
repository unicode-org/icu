/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

/**
 * The path list GUI component.
 */
public class PathComponent extends JComponent {
    /**
     * The serializable UID.
     */
    public static final long serialVersionUID = 1340;

    /**
     * A menu item for <code>pathPopup</code> to add all drives to the path
     * model.
     */
    private JMenuItem pathAddAllDrivesItem = new JMenuItem(
            "Add All Drives to List");

    /**
     * The browse button where the user can browse for a particular path.
     */
    private JButton pathBrowseButton = new JButton("Browse...");

    /**
     * A menu item that copies the selected filenames to the clipboard.
     */
    private JMenuItem pathCopyItem = new JMenuItem("Copy selected");

    /**
     * The browse dialog that pops up when the browse button is clicked.
     */
    private JFileChooser pathChooser = new JFileChooser();

    /**
     * The field where the user can enter a path.
     */
    private JTextField pathField = new JTextField(30);

    /**
     * The label for path input field.
     */
    private JLabel pathInputLabel = new JLabel(
            "Include/exclude a directory or a file:");

    /**
     * The label for the path list.
     */
    private JLabel pathListLabel = new JLabel(
            "Directories to search and ICU4J jar files to check:");

    /**
     * The panel to hold the input components.
     */
    private JPanel pathInputPanel = new JPanel();

    /**
     * The JList that holds the path model.
     */
    private JList pathList = new JList();

    /**
     * The path model that stores all the paths.
     */
    private PathModel pathModel;

    /**
     * The panel to hold the output components.
     */
    private JPanel pathOptionPanel = new JPanel();

    /**
     * The context menu for extra options.
     */
    private JPopupMenu pathPopup = new JPopupMenu();

    /**
     * A menu item for <code>pathPopup</code> to remove all paths from the
     * path model.
     */
    private JMenuItem pathRemoveAllItem = new JMenuItem("Remove All");

    /**
     * A menu item for <code>pathPopup</code> to remove the selected paths
     * from the path model.
     */
    private JMenuItem pathRemoveSelectedItem = new JMenuItem(
            "Remove Selected Items");

    /**
     * A menu item for <code>pathPopup</code> to begin a search on the
     * selected paths in the path model.
     */
    private JMenuItem pathSearchAllItem = new JMenuItem("Search All");

    /**
     * The search button that starts the search on the selected paths (or all
     * the paths if none are selected).
     */
    private JButton pathSearchAllButton = new JButton("Search All");

    /**
     * The panel to hold the search components.
     */
    private JPanel pathSearchPanel = new JPanel();

    /**
     * A menu item for <code>pathPopup</code> to begin a search on all paths
     * in the path model.
     */
    private JMenuItem pathSearchSelectedItem = new JMenuItem(
            "Search Selected Items");

    /**
     * The combobox where a user specifies whether to include or to exclude an
     * entered path.
     */
    private JComboBox pathSignBox = new JComboBox(new Object[] { "Include",
            "Exclude" });

    /**
     * The checkbox where the user can specify whether or not to search
     * subdirectories. Set to true by default.
     */
    private JCheckBox pathSubdirOption = new JCheckBox("Search Subdirectories",
            true);

    /**
     * Constructs the path list GUI component.
     * 
     * @param owner
     *            The GUILoader object that ownes this component.
     */
    public PathComponent(final GUILoader owner) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(pathInputPanel);
        add(pathListLabel);
        add(new JScrollPane(pathList));
        add(pathOptionPanel);
        add(pathSearchPanel);

        JPanel pathInputSubPanel = new JPanel();
        pathInputPanel
                .setLayout(new BoxLayout(pathInputPanel, BoxLayout.Y_AXIS));
        pathInputPanel.add(pathInputLabel);
        pathInputPanel.add(pathInputSubPanel);
        pathInputSubPanel.add(pathSignBox);
        pathInputSubPanel.add(pathField);
        pathInputSubPanel.add(pathBrowseButton);

        pathOptionPanel.add(pathSubdirOption);
        pathSearchPanel.add(pathSearchAllButton);

        pathChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        pathPopup.add(pathCopyItem);
        pathPopup.add(new JSeparator());
        pathPopup.add(pathAddAllDrivesItem);
        pathPopup.add(pathRemoveSelectedItem);
        pathPopup.add(pathRemoveAllItem);
        pathPopup.add(new JSeparator());
        pathPopup.add(pathSearchSelectedItem);
        pathPopup.add(pathSearchAllItem);

        pathField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addFile(new File(pathField.getText().trim()));
                pathField.selectAll();
            }
        });

        pathList.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent event) {
                checkPopup(event);
            }

            public void mouseEntered(MouseEvent event) {
                checkPopup(event);
            }

            public void mouseExited(MouseEvent event) {
                checkPopup(event);
            }

            public void mousePressed(MouseEvent event) {
                checkPopup(event);
            }

            public void mouseReleased(MouseEvent event) {
                checkPopup(event);
            }

            private void checkPopup(MouseEvent event) {
                if (event.isPopupTrigger())
                    pathPopup.show((Component) event.getSource(), event.getX(),
                            event.getY());
            }
        });

        pathList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                int code = event.getKeyCode();
                if (code == KeyEvent.VK_DELETE
                        || code == KeyEvent.VK_BACK_SPACE)
                    pathModel.remove(pathList.getSelectedIndices());
            }
        });

        pathCopyItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String selection = "";
                int[] rows = pathList.getSelectedIndices();
                for (int i = 0; i < rows.length; i++) {
                    String includePathString = pathModel.getElementAt(rows[i])
                            .toString();
                    // get rid of a + or - at the begining of includePathString
                    // if one exists
                    if (includePathString.length() > 0
                            && (includePathString.charAt(0) == '+' || includePathString
                                    .charAt(0) == '-'))
                        includePathString = includePathString.substring(1);
                    selection += includePathString + "\n";
                }
                getToolkit().getSystemClipboard().setContents(
                        new StringSelection(selection), null);
            }
        });

        pathRemoveSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                pathModel.remove(pathList.getSelectedIndices());
            }
        });

        pathRemoveAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                pathModel.removeAll();
            }
        });

        pathSearchSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.search(pathList.getSelectedIndices(), pathSubdirOption
                        .isSelected());
            }
        });

        pathSearchAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.searchAll(pathSubdirOption.isSelected());
            }
        });

        pathSearchAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.searchAll(pathSubdirOption.isSelected());
            }
        });

        pathAddAllDrivesItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                pathModel.addAllDrives();
            }
        });

        pathBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // set the chooser's intial path to be whatever is in the text
                // field
                File path = new File(pathField.getText().trim());
                if (path.exists())
                    pathChooser.setSelectedFile(path);

                // run the chooser dialog
                int returnVal = pathChooser.showOpenDialog(PathComponent.this);

                // on an accept, add the path to the model and set the text
                // field to it
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    path = pathChooser.getSelectedFile();
                    addFile(path);
                    pathField.setText(path.getPath());
                }
            }
        });
    }

    /**
     * Sets the path model.
     * 
     * @param pathModel
     *            The path model.
     */
    public void setPathModel(PathModel pathModel) {
        this.pathModel = pathModel;
        pathList.setModel(pathModel);
    }

    /**
     * Sets whether the search button should be enabled.
     * 
     * @param value
     *            Whether the search button should be enabled.
     */
    public void setSearchEnabled(boolean value) {
        pathSearchAllButton.setEnabled(value);
    }

    /**
     * Attempts to add a path to the path model.
     * 
     * @param file
     *            The path to add.
     */
    private void addFile(File file) {
        if (!pathModel.add(new IncludePath(file, isIncluded())))
            JOptionPane.showMessageDialog(PathComponent.this, "\""
                    + file.getPath() + "\" is not a valid file or path.",
                    "Cannot add path/file", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns whether the user has specified to include or to exclude the
     * entered path.
     * 
     * @return Whether the user has specified to include or to exclude the
     *         entered path.
     */
    private boolean isIncluded() {
        return ((String) pathSignBox.getSelectedItem()).equals("Include");
    }
}
