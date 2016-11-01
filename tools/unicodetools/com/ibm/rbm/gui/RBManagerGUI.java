/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.ibm.rbm.*;

/**
 * The Graphical User Interface for working with and through a Resource Bundle Manager. The GUI has no public main
 * method. It is instead instantiated from running the main method in RBManager. For help with using this interface,
 * consult the documentation included in the project.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class RBManagerGUI extends JFrame implements ActionListener, MouseListener, ChangeListener, TreeSelectionListener
{
	// CONSTANTS
	private static final int buffer = 20;
	private static final Dimension dimMain = new Dimension(750,550);
	private static final Dimension dimMainMax = new Dimension(2000,1500);
	private static final Dimension dimMainMin = new Dimension(550,350);
	private static final Dimension dimTop = new Dimension(dimMain.width - buffer,50);
	private static final Dimension dimTopMax = new Dimension(dimMainMax.width - buffer,50);
	private static final Dimension dimTopMin = new Dimension(dimMainMin.width - buffer,50);
	private static final Dimension dimBottom = new Dimension(dimMain.width - buffer,dimMain.height-dimTop.height - buffer);
	private static final Dimension dimBottomMax = new Dimension(dimMainMax.width - buffer,dimMainMax.height-dimTopMin.height - buffer);
	private static final Dimension dimBottomMin = new Dimension(dimMainMin.width - buffer,dimMainMin.height-dimTopMax.height - buffer);
	private static final Dimension dimLeft = new Dimension(175,dimBottom.height - buffer);
	private static final Dimension dimRight = new Dimension(dimMain.width-dimLeft.width - buffer,dimBottom.height - buffer);
	
	/**
	 * Used for toggling the debug mode
	 */
	public static final boolean debug = false;
	/**
	 * Used to count debug messages
	 */
	public static int debugcount = 0;
	
	// member declarations
	
	// ** DATA **
	RBManager rbm = null;
	String userName = Resources.getTranslation("unknown_user");
	
	DefaultMutableTreeNode activeNode = null;
	
	// ** MAIN MENU **
	RBManagerMenuBar         jMenuBarMain = null;

	// ** CONTENT PANES **
	Box          boxMain = new Box(BoxLayout.Y_AXIS);
	//JPanel       jPanelTop = new JPanel();
	JPanel       jPanelBottom = new JPanel();
	JSplitPane   jSplitPaneMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	// ** SPLIT PANE COMPONENTS **
	JTree        jTreeDisplay = new JTree();
	JTabbedPane  jTabbedPaneMain = new JTabbedPane();
	RBStatisticsPanel        jPanelStats = new RBStatisticsPanel();
	RBUntranslatedPanel      jPanelUntrans = new RBUntranslatedPanel(this);
	RBGroupPanel             jPanelGroups = new RBGroupPanel(this);
	RBSearchPanel            jPanelSearch = new RBSearchPanel(this);
	JScrollPane  jScrollPaneTree = new JScrollPane(jTreeDisplay,
													ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
													ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	// ** PROJECT VIEW SPLIT PANE COMPONENTS
	JTabbedPane              treeTabbedPane = new JTabbedPane();
	JTree                    projectTree = new JTree();
	JScrollPane              projectScrollPane = new JScrollPane(projectTree, 
																ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
																ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	RBProjectItemPanel       projectPanel = new RBProjectItemPanel(this);
	RBProject                project = null;
	
	// ** File Chooser **
	JFileChooser openFileChooser = new JFileChooser();
	JFileChooser saveFileChooser = new JFileChooser();
	JFileChooser projectFileChooser = new JFileChooser();
	
	/**
	 * Creation of the GUI should be immediately followed by the method calls to initComponents() and setVisible(true).
	 * These methods were not called by default for programming discretion
	 */
	
	public RBManagerGUI()
	{
	}
	
	/**
	 * Inherits from JFrame.validate(), with some component updates
	 */
	
	public void validate() {
		super.validate();
		updateDisplayPanels();
	}
	
	/**
	 * Initial construction of all of the GUI components. This method should be called immediately following the
	 * construction of the GUI object.
	 */
	
	public void initComponents() throws Exception
	{
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// the following code sets the frame's initial state
		
		openFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				if (!(name.toLowerCase().endsWith(".properties"))) return false;
				if (name.indexOf("_") > 0) return false;
				return true;
			}
	
			public String getDescription() {
				return Resources.getTranslation("dialog_file_filter_description");
			}
		});
		
		saveFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				if (!(name.toLowerCase().endsWith(".properties"))) return false;
				if (name.indexOf("_") > 0) return false;
				return true;
			}
	
			public String getDescription() {
				return Resources.getTranslation("dialog_file_filter_description");
			}
		});
		
		projectFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				if (!(name.toLowerCase().endsWith(".rbproject"))) return false;
				return true;
			}
			
			public String getDescription() {
				return Resources.getTranslation("dialog_project_filter_description");	
			}
		});

        // ** The Main program icon **
        setIconImage((new ImageIcon(getClass().getResource("images/tree_icon_bundle.gif"))).getImage());
		
		// ** MAIN MENU BAR ITEMS **
		jMenuBarMain = new RBManagerMenuBar(this);
		
		// ** DISPLAY TREE **
		//jScrollPaneTree.setSize(dimLeft);
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		
		jPanelStats.setSize(dimRight);
		jPanelUntrans.setSize(dimRight);
		jPanelGroups.setSize(dimRight);
		jPanelSearch.setSize(dimRight);
		
		// ** MAIN TABBED PANE **
		jTabbedPaneMain.setSize(dimRight);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_statistics"), jPanelStats);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_untranslated"), jPanelUntrans);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_groups"), jPanelGroups);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_search"), jPanelSearch);
		
		// ** LEFT TABBED PANE **
		treeTabbedPane.setSize(dimLeft);
		treeTabbedPane.setPreferredSize(dimLeft);
		treeTabbedPane.addTab(Resources.getTranslation("tab_bundle"), jScrollPaneTree);
		treeTabbedPane.addTab(Resources.getTranslation("tab_project"), projectScrollPane);
		treeTabbedPane.addChangeListener(this);
		
		// ** MAIN SPLIT PANE **
		//jSplitPaneMain.setSize(dimBottom);
		//jSplitPaneMain.setLeftComponent(jScrollPaneTree);
		jSplitPaneMain.setLeftComponent(treeTabbedPane);
		jSplitPaneMain.setRightComponent(jTabbedPaneMain);
		jSplitPaneMain.setContinuousLayout(true);
		
		// ** BOTTOM PANEL **
		//jPanelBottom.setPreferredSize(dimBottom);
		jPanelBottom.setMaximumSize(dimBottomMax);
		jPanelBottom.setMinimumSize(dimBottomMin);
		jPanelBottom.setBorder(BorderFactory.createLineBorder(Color.black));
		jPanelBottom.setLayout(new BorderLayout(1,1));
		jPanelBottom.removeAll();
		jPanelBottom.add(jSplitPaneMain, BorderLayout.CENTER);
		
		// ** MAIN FRAME SETUP **
		dimMain.height += jMenuBarMain.getPreferredSize().height;
		setSize(dimMain);
		((JComponent)getContentPane()).setMaximumSize(dimMainMax);
		((JComponent)getContentPane()).setMinimumSize(dimMainMin);
		setJMenuBar(jMenuBarMain);
		getContentPane().removeAll();
		getContentPane().add(jPanelBottom, BorderLayout.CENTER);
		setTitle(Resources.getTranslation("resource_bundle_manager"));
		validateTree();
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
			}
		});
	}
  
  	private boolean mShown = false;
	
	/**
	 * Reveals the private method of JFrame.validateTree()
	 */
	
	public void validateMyTree() {
		validateTree();
	}
	
	/**
	 * Creates a new Resource File (i.e. English, English Canada, Finnish, etc.)
	 */
	
	public void createResourceFile() {
		new ResourceCreationDialog(rbm, this, Resources.getTranslation("dialog_title_new_file"), true);
	}
	
	/**
	 * Creates a new group for grouping BundleItems
	 */
	
	public void createBundleGroup() {
		new BundleGroupCreationDialog(rbm, this, Resources.getTranslation("dialog_title_new_group"), true);
		updateDisplayPanels();
	}
	
	/**
	 * Creates a new BundleItem
	 */
	
	public void createBundleItem() {
		new BundleItemCreationDialog(rbm, this, Resources.getTranslation("dialog_title_new_item"), true);
		updateDisplayPanels();
		updateProjectTree();
	}
	
	/**
	 * Handles events generated
	 */
	
	public void valueChanged(TreeSelectionEvent ev) {
		if (ev.getSource() == projectTree) updateProjectPanels();
		else if (ev.getSource() == jTreeDisplay) {
			TreePath selPath = jTreeDisplay.getSelectionPath();
			activeNode = (DefaultMutableTreeNode)selPath.getLastPathComponent();
			updateDisplayPanels();
			/*
			int selRow = jTreeDisplay.getRowForLocation(ev.getX(), ev.getY());
			TreePath selPath = jTreeDisplay.getPathForLocation(ev.getX(), ev.getY());
			if (selRow != -1) {
            if (ev.getClickCount() == 1 && ev.getSource() == jTreeDisplay) {
				
				System.out.println("Other tree");
			} else if (ev.getClickCount() == 1 && ev.getSource() == projectTree) {
				System.out.println("Mouse pressed");
				updateProjectPanels();	
			} else System.out.println(String.valueOf(ev.getClickCount()) + " " + ev.getSource().toString());
			
			*/
		}
	}
	
	public void stateChanged(ChangeEvent ev) {
		if (ev.getSource() == treeTabbedPane) {
			int index = treeTabbedPane.getSelectedIndex();	
			String title = treeTabbedPane.getTitleAt(index);
			if (title.equals(Resources.getTranslation("tab_bundle"))) {
				jSplitPaneMain.setRightComponent(jTabbedPaneMain);
				updateDisplayPanels();
			} else if (title.equals(Resources.getTranslation("tab_project"))) {
				jSplitPaneMain.setRightComponent(projectPanel);
				updateProjectPanels();
			}
		}
	}
	
	public void actionPerformed(ActionEvent ev) {
		if (ev.getID() == ActionEvent.ACTION_PERFORMED) {
			if (ev.getSource() instanceof JMenuItem && ((JMenuItem)ev.getSource()).getName() != null &&
				((JMenuItem)ev.getSource()).getName().startsWith("__")) {               // Menu -> File -> __Recent File
				// This item is a recent file selection. We need to open that file
				String fileLocation = ((JMenuItem)ev.getSource()).getName();
				fileLocation = fileLocation.substring(2,fileLocation.length());
				try {
					rbm = new RBManager(new File(fileLocation));
					updateDisplayTree();
					updateProjectTree();
					updateProjectPanels();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this,Resources.getTranslation("error_opening_file", ev.getActionCommand()),
												  Resources.getTranslation("dialog_title_error_opening_file"),
												  JOptionPane.ERROR_MESSAGE);
					rbm = null;
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_save")) &&
					   ((JMenuItem)ev.getSource()).getName() != null) {                // Popup Tree Menu -> Save
				String selectedEncoding = ((JMenuItem)ev.getSource()).getName();
				saveResources(selectedEncoding);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_hide")) &&
					   ((JMenuItem)ev.getSource()).getName() != null) {                // Popup Tree Menu -> Hide
				String selectedEncoding = ((JMenuItem)ev.getSource()).getName();
				// Should I prompt for this?
				hideResources(selectedEncoding);
			}  else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_delete")) &&
					   ((JMenuItem)ev.getSource()).getName() != null) {                // Popup Tree Menu -> Delete
				String selectedEncoding = ((JMenuItem)ev.getSource()).getName();
				int response = JOptionPane.showConfirmDialog(this,
					Resources.getTranslation("dialog_delete_warning"),
					Resources.getTranslation("dialog_title_quit"), JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
				if (response == JOptionPane.YES_OPTION) {
					deleteResources(selectedEncoding);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_new_project"))) {
				String response = JOptionPane.showInputDialog(this,                   // Popup Project Menu -> New Project
					Resources.getTranslation("dialog_new_project"), Resources.getTranslation("dialog_title_new_project"),
					JOptionPane.QUESTION_MESSAGE);
				if (response == null || response.trim().equals("")) return;
				project = new RBProject(response);
				updateProjectTree();
				updateProjectPanels();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_open_project"))) {
				int result = projectFileChooser.showOpenDialog(this);                // Popup Project Menu -> Open Project
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = projectFileChooser.getSelectedFile();
					try {
						project = new RBProject(f);
						updateProjectTree();
						updateProjectPanels();
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_creating_project"),
							Resources.getTranslation("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_save_project"))) {
				int result = projectFileChooser.showSaveDialog(this);                // Popup Project Menu -> Save Project
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = projectFileChooser.getSelectedFile();
					try {
						project.write(f);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_saving_project"),
							Resources.getTranslation("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_add_project_bundle"))) {
				int result = openFileChooser.showOpenDialog(this);                   // Popup Project Menu -> Add Bundle
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = openFileChooser.getSelectedFile();
					try {
						project.addBundle(f.getAbsolutePath());
						updateProjectTree();
						updateProjectPanels();
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_adding_project_bundle"),
							Resources.getTranslation("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_remove_project_bundle"))) {
				String bundleName = ((JMenuItem)ev.getSource()).getName();           // Popup Project Menu -> Remove Bundle
				project.removeBundle(bundleName);
				updateProjectTree();
				updateProjectPanels();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_select_project_bundle"))) {
				String bundleName = ((JMenuItem)ev.getSource()).getName();           // Popup Project Menu -> Select Bundle
				RBManager bundle = project.getBundle(bundleName);
				rbm = bundle;
				updateDisplayTree();
				updateDisplayPanels();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_quit"))) {
																					   // Menu -> File -> Quit
				thisWindowClosing(null);
				return;
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_new"))) {
																					   // Menu -> File -> New Resource Bundle
				promptForSave(null);
				String oldUser = getUser();
				if (rbm != null && rbm.getUser() != null && !(rbm.getUser().equals(Resources.getTranslation("unknown_user"))))
					oldUser = rbm.getUser();
				String response = JOptionPane.showInputDialog(this,
					Resources.getTranslation("dialog_new_baseclass"), Resources.getTranslation("dialog_title_new_bundle"),
					JOptionPane.QUESTION_MESSAGE);
				if (response != null) {
					// Test the response for white space
					if (response.indexOf(" ") > 0 || response.indexOf("\t") > 0 || response.indexOf("\n") > 0) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_baseclass_whitespace") + "\n" + Resources.getTranslation("error_bundle_not_created"),
							Resources.getTranslation("dialog_title_error_creating_bundle"), JOptionPane.ERROR_MESSAGE);
					} else {
						rbm = new RBManager(response);
						updateDisplayTree();
						updateProjectTree();
						updateProjectPanels();
						updateDisplayPanels();
					}
				}
				// Update the user information
				if (oldUser.equals(Resources.getTranslation("unknown_user"))) {
					String user = JOptionPane.showInputDialog(this,
						Resources.getTranslation("dialog_user_name"), Resources.getTranslation("dialog_title_user_name"),
						JOptionPane.QUESTION_MESSAGE);
					if (user != null && !(user.equals(""))) setUser(user);
				} else rbm.setUser(oldUser);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_open"))) {
																					   // Menu -> File -> Open Resource Bundle
				promptForSave(null);
				String oldUser = getUser();
				if (rbm != null && rbm.getUser() != null && !(rbm.getUser().equals(Resources.getTranslation("unknown_user"))))
					oldUser = rbm.getUser();
				openFileChooser.setSelectedFile(new File("Resources" + File.separator + "RBManager.properties"));
				int status = openFileChooser.showOpenDialog(this);
				if (status == JFileChooser.CANCEL_OPTION) {
					// File opening canceled
				} else if (status == JFileChooser.ERROR_OPTION) {
					// Error in file open
				} else {
					// A file has been selected
					try {
						rbm = new RBManager(openFileChooser.getSelectedFile());
						updateDisplayTree();
						updateProjectTree();
						updateProjectPanels();
					} catch (IOException ioe) {
						// Should provide some alert here
						System.err.println("Could not open the file " + openFileChooser.getSelectedFile().getAbsolutePath() +
										   ": " + ioe.getMessage());
						rbm = null;
					}
				}
				if (rbm == null) return;
				// Update the user information
				if (oldUser.equals(Resources.getTranslation("unknown_user"))) {
					String user = JOptionPane.showInputDialog(this,
						Resources.getTranslation("dialog_user_name"), Resources.getTranslation("dialog_title_user_name"),
						JOptionPane.QUESTION_MESSAGE);
					if (user != null && !(user.equals("")))
						setUser(user);
				} else
					rbm.setUser(oldUser);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_save"))) {
																					   // Menu -> File -> Save Resource Bundle
				saveResources();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_saveas"))) {
																					   // Menu -> File -> Save Resource Bundle As
				saveResourcesAs();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_import_properties"))) {
																					   // Menu -> File -> Import -> Properties
				if (rbm == null || rbm.getBundles() == null) return;
				RBPropertiesImporter importer = new RBPropertiesImporter(Resources.getTranslation("import_properties_title"), rbm, this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_import_java"))) {
																					   // Menu -> File -> Import -> Java
				if (rbm == null || rbm.getBundles() == null) return;
				RBJavaImporter importer = new RBJavaImporter(Resources.getTranslation("import_java_title"), rbm, this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_import_TMX"))) {
																					   // Menu -> File -> Import -> TMX
				if (rbm == null || rbm.getBundles() == null)
					return;
				RBTMXImporter importer = new RBTMXImporter(Resources.getTranslation("import_TMX_title"), rbm, this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_import_XLF"))) {
				   // Menu -> File -> Import -> XLIFF
				if (rbm == null || rbm.getBundles() == null)
				    return;
				RBxliffImporter importer = new RBxliffImporter(Resources.getTranslation("import_XLF_title"), rbm, this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_properties"))) {
																					   // Menu -> File -> Export -> Properties
				RBPropertiesExporter exp = new RBPropertiesExporter();
				try {
					if (rbm != null && rbm.getBundles() != null)
						exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_java"))) {
																					   // Menu -> File -> Export -> Java
				RBJavaExporter exp = new RBJavaExporter();
				try {
					if (rbm != null && rbm.getBundles() != null) exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_ICU"))) {
																					   // Menu -> File -> Export -> Java
				RBICUExporter exp = new RBICUExporter();
				try {
					if (rbm != null && rbm.getBundles() != null) exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_TMX"))) {
																					   // Menu -> File -> Export -> TMX
				RBTMXExporter exp = new RBTMXExporter();
				try {
					if (rbm != null && rbm.getBundles() != null) exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_XLF"))) {
				   // Menu -> File -> Export -> XLIFF
				RBxliffExporter exp = new RBxliffExporter();
				try {
					if (rbm != null && rbm.getBundles() != null)
					    exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
					Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_addfile"))) {
																					   // Menu -> Options -> Add New Resource
				createResourceFile();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_addgroup")) ||
					   ev.getActionCommand().equals(Resources.getTranslation("button_create_group"))) {
																					   // Menu -> Options -> Add New Group
				createBundleGroup();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_addentry"))) {
																					   // Menu -> Options -> Add New Entry
				createBundleItem();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_preferences"))) {
																					   // Menu -> Options -> Preferences
				PreferencesDialog pd = new PreferencesDialog(this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_help_about"))) { 
																					   // Menu -> Help -> About RBManager
				AboutDialog.showDialog(this);
			} else RBManagerGUI.debugMsg("Missed Action Command: " + ev.getActionCommand());
			
		}
		
	}
	
	/**
	 * Handles events generated
	 */
	
	public void mousePopup(MouseEvent ev) {
		if (ev.getSource() == jTreeDisplay) {
			int selRow = jTreeDisplay.getRowForLocation(ev.getX(), ev.getY());
			TreePath selPath = jTreeDisplay.getPathForLocation(ev.getX(), ev.getY());
			if (selRow != -1) {
			    if (ev.getClickCount() == 1) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
					Object obj = node.getUserObject();
					if (obj == null || !(obj instanceof Bundle)) return;
					Bundle bundle = (Bundle)obj;
					String encoding = bundle.encoding;
					if (encoding == null) encoding = new String();
							
					// Create the menu to display
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem saveItem = new JMenuItem(Resources.getTranslation("menu_tree_save"));
					JMenuItem hideItem = new JMenuItem(Resources.getTranslation("menu_tree_hide"));
					JMenuItem deleteItem = new JMenuItem(Resources.getTranslation("menu_tree_delete"));
							
					saveItem.setName(encoding); saveItem.addActionListener(this);
					hideItem.setName(encoding); hideItem.addActionListener(this);
					deleteItem.setName(encoding); deleteItem.addActionListener(this);
							
					popupMenu.add(saveItem);
					if (node.getLevel() != 1) {
						popupMenu.add(hideItem);
						popupMenu.add(deleteItem);
					}
							
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
			    }
			}
		} else if (ev.getSource() == projectTree) {
			int selRow = projectTree.getRowForLocation(ev.getX(), ev.getY());
			TreePath selPath = projectTree.getPathForLocation(ev.getX(), ev.getY());
			if (selRow != -1 && ev.getClickCount() == 1) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
				Object obj = node.getUserObject();
				if (obj == null) return;
				else if (obj instanceof String) {
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem newItem = new JMenuItem(Resources.getTranslation("menu_tree_new_project"));
					JMenuItem openItem = new JMenuItem(Resources.getTranslation("menu_tree_open_project"));
					JMenuItem saveItem = new JMenuItem(Resources.getTranslation("menu_tree_save_project"));
					newItem.addActionListener(this);
					openItem.addActionListener(this);
					saveItem.addActionListener(this);
					popupMenu.add(newItem);
					popupMenu.add(openItem);
					popupMenu.add(saveItem);
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
				} else if (obj instanceof RBProject) {
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem newItem = new JMenuItem(Resources.getTranslation("menu_tree_new_project"));
					JMenuItem openItem = new JMenuItem(Resources.getTranslation("menu_tree_open_project"));
					JMenuItem saveItem = new JMenuItem(Resources.getTranslation("menu_tree_save_project"));
					JMenuItem addItem = new JMenuItem(Resources.getTranslation("menu_tree_add_project_bundle"));
					newItem.addActionListener(this);
					openItem.addActionListener(this);
					saveItem.addActionListener(this);
					addItem.addActionListener(this);
					popupMenu.add(newItem);
					popupMenu.add(openItem);
					popupMenu.add(saveItem);
					popupMenu.add(addItem);
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
				} else if (obj instanceof RBManager) {
					RBManager rbm = (RBManager)obj;
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem selectItem = new JMenuItem(Resources.getTranslation("menu_tree_select_project_bundle"));
					JMenuItem removeItem = new JMenuItem(Resources.getTranslation("menu_tree_remove_project_bundle"));
					selectItem.setName(rbm.getBaseClass());
					removeItem.setName(rbm.getBaseClass());
					selectItem.addActionListener(this);
					removeItem.addActionListener(this);
					popupMenu.add(selectItem);
					popupMenu.add(removeItem);
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
				}
			}
		}
	}
	
	public void mousePressed(MouseEvent ev)  {
		if (ev.isPopupTrigger()) {
			mousePopup(ev);
		}
	}
	
	public void mouseReleased(MouseEvent ev) {
		if (ev.isPopupTrigger()) {
			mousePopup(ev);
			return;
		}
		// Not the popup trigger
	}
	
	public void mouseEntered(MouseEvent ev)  { }
	
	public void mouseExited(MouseEvent ev)   { }
	
	public void mouseClicked(MouseEvent ev) {
		if (ev.getClickCount() == 2 && ev.getSource() instanceof JTable) {
			// We are going to display the edit frame for the item selected
			BundleItem item = null;
			JTable table = (JTable) ev.getSource();
			if (table.getModel() instanceof UntranslatedItemsTableModel) {
				int row = table.getSelectedRow();
				UntranslatedItemsTableModel model = (UntranslatedItemsTableModel)table.getModel();
				item = model.getBundleItem(row);
				BundleItemDialog biDialog = new BundleItemDialog(rbm, item, (rbm == null ? "" : rbm.getUser()),
															 this, Resources.getTranslation("dialog_title_edit_item"), true);
				model.update();
			} else if (table.getModel() instanceof SearchItemsTableModel) {
				int row = table.getSelectedRow();
				SearchItemsTableModel model = (SearchItemsTableModel)table.getModel();
				item = model.getBundleItem(row);
				BundleItemDialog biDialog = new BundleItemDialog(rbm, item, (rbm == null ? "" : rbm.getUser()),
															 this, Resources.getTranslation("dialog_title_edit_item"), true);
				model.update();
			} else if (table.getModel() instanceof GroupItemsTableModel) {
				int row = table.getSelectedRow();
				GroupItemsTableModel model = (GroupItemsTableModel)table.getModel();
				item = model.getBundleItem(row);
				BundleItemDialog biDialog = new BundleItemDialog(rbm, item, (rbm == null ? "" : rbm.getUser()),
															 this, Resources.getTranslation("dialog_title_edit_item"), true);
				model.update();
			}
			updateDisplayPanels();
		}
	}
	
	protected void updateProjectPanels() {
		projectPanel.updateComponents();
	}
	
	// Update the display of the main panels (stats, untrans, groups). Should be called after a new tree selection
	protected void updateDisplayPanels() {
		debugMsg("Updating Display Panels");
		
		Bundle bundle = null;
		if (activeNode == null) return;
		Object o = activeNode.getUserObject();
		if (o == null)
			return;
		if (o instanceof String) {
			// A node that is not a root was selected.... I need to do something here
			String str = (String)o;
			if (rbm == null) return;
			if (str.equals(rbm.getBaseClass())) {
				// The base class node was selected
				jPanelStats.setManager(rbm);
				jPanelUntrans.setManager(rbm);
				jPanelGroups.setManager(rbm);
				jPanelSearch.setManager(rbm);
			} else {
				jPanelStats.removeElements();
				jPanelUntrans.removeElements();
				jPanelGroups.removeElements();
				jPanelSearch.removeElements();
			}
			//return;
		}
		else if (o instanceof Bundle) {
			bundle = (Bundle) activeNode.getUserObject();
			jPanelStats.setBundle(bundle);
			jPanelUntrans.setBundle(bundle);
			jPanelGroups.setBundle(bundle);
			jPanelSearch.setBundle(bundle);
		}
		else
			RBManagerGUI.debugMsg(o.toString());
		
		jPanelStats.updateComponents();
		jPanelUntrans.updateComponents();
		jPanelGroups.updateComponents();
		jPanelSearch.updateComponents();
		
		validateTree();
	}
	
	public void updateProjectTree() {
		debugMsg("Updating Project Trees");
		
		DefaultMutableTreeNode root = null;
		
		if (project != null) {
			root = new DefaultMutableTreeNode(project);
			for (int i=0; i < project.getSize(); i++) {
				RBManager rbm = project.getBundle(i);
				DefaultMutableTreeNode bundleNode = new DefaultMutableTreeNode(rbm);
				root.add(bundleNode);
				Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
				Vector groups = mainBundle.getGroupsAsVector();
				for (int j=0; j < groups.size(); j++) {
					BundleGroup group = (BundleGroup)groups.elementAt(j);
					DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
					bundleNode.add(groupNode);
					Vector items = group.getItemsAsVector();
					for (int k=0; k < items.size(); k++) {
						BundleItem item = (BundleItem)items.elementAt(k);
						DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(item);
						groupNode.add(itemNode);
					}
				}
			}
		} else if (rbm != null) {
			// There is a resource bundle open, but no project
			root = new DefaultMutableTreeNode(Resources.getTranslation("no_project"));
			Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
			DefaultMutableTreeNode bundleNode = new DefaultMutableTreeNode(rbm);//(rbm.getBaseClass());
			root.add(bundleNode);
			Vector groups = mainBundle.getGroupsAsVector();
			for (int i=0; i < groups.size(); i++) {
				BundleGroup group = (BundleGroup)groups.elementAt(i);
				DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
				bundleNode.add(groupNode);
				Vector items = group.getItemsAsVector();
				for (int j=0; j < items.size(); j++) {
					BundleItem item = (BundleItem)items.elementAt(j);
					DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(item);
					groupNode.add(itemNode);
				}
			}
		} else {
			root = new DefaultMutableTreeNode(Resources.getTranslation("no_project_bundle"));
		}
		
		// Create the tree from the roots
		projectTree = new JTree(root);
		projectTree.addMouseListener(this);
		projectTree.addTreeSelectionListener(this);
		projectTree.setCellRenderer(RBTreeCellRenderer.getInstance());
		projectScrollPane.getViewport().removeAll();
		projectScrollPane.getViewport().add(projectTree);
		repaint();
		validateTree();
		return;
	}
	
	// Update the display of the tree file map. Should be called when the tree is changed/updated
	public void updateDisplayTree() {
		debugMsg("Updating Display Trees");
		
		DefaultMutableTreeNode root = null;
		
		if (rbm == null || rbm.getBundles() == null) {
			root = new DefaultMutableTreeNode(Resources.getTranslation("no_resource_bundle"));
		} else {
			// From here on out, there is a defined resource bundle manager
			Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
			root = new DefaultMutableTreeNode(rbm.getBaseClass());
			// Add the base class
			root.add(new DefaultMutableTreeNode(mainBundle));
			
			//DefaultMutableTreeNode currNode = root;
			for (int i = 1; i < rbm.getBundles().size(); i++) {
				Bundle currBundle = (Bundle)rbm.getBundles().elementAt(i);
				String variant = currBundle.getVariantEncoding();
				String country = currBundle.getCountryEncoding();
				String language = currBundle.getLanguageEncoding();
				//DefaultMutableTreeNode languageNode = null;
				// Look for a node representing this language
				if (language == null || language.equals("")) continue;
				boolean languageNodeFound = false;
				for (int j=0; j < root.getChildCount(); j++) {
					DefaultMutableTreeNode langNode = (DefaultMutableTreeNode)root.getChildAt(j);
					Object o = langNode.getUserObject();
					if (o == null || !(o instanceof String)) continue;
					String str = (String)o;
					if (str.equals(Resources.getTranslation("tree_language_node", language))) {
						// There is a non-leaf node with this language
						languageNodeFound = true;
						if (country == null || country.equals(""))
							 langNode.add(new DefaultMutableTreeNode(currBundle));
						else {
							// We need to look at country, variant
							boolean countryNodeFound = false;
							for (int k=0; k < langNode.getChildCount(); k++) {
								DefaultMutableTreeNode countryNode = (DefaultMutableTreeNode)langNode.getChildAt(k);
								Object o2 = countryNode.getUserObject();
								if (o2 == null || !(o2 instanceof String)) continue;
								String str2 = (String)o2;
								if (str2.equals(Resources.getTranslation("tree_country_node", country))) {
									// There is a non-leaf node for this country
									countryNodeFound = true;
									if (variant == null || variant.equals("")) {
										countryNode.add(new DefaultMutableTreeNode(currBundle));
									} else {
										// We need to look at variant
										boolean variantNodeFound = false;
										for (int l=0; l < countryNode.getChildCount(); l++) {
											DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
											Object o3 = variantNode.getUserObject();
											if (o3 == null || !(o3 instanceof String)) continue;
											String str3 = (String)o3;
											if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
												variantNodeFound = true;
												variantNode.add(new DefaultMutableTreeNode(currBundle));
											} 
										} // end for - country node loop
										if (!variantNodeFound) {
											DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node"));
											countryNode.add(variantNode);
											variantNode.add(new DefaultMutableTreeNode(currBundle));
										}
									}
								}
							} // end for - language node loop
							if (!countryNodeFound) {
								DefaultMutableTreeNode countryNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_country_node", country));
								langNode.add(countryNode);
								if (variant == null || variant.equals("")) {
									countryNode.add(new DefaultMutableTreeNode(currBundle));
								} else {
									// We need to look at the variant	
									boolean variantNodeFound = false;
									for (int l=0; l < countryNode.getChildCount(); l++) {
										DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
										Object o3 = variantNode.getUserObject();
										if (o3 == null || !(o3 instanceof String)) continue;
										String str3 = (String)o3;
										if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
											variantNodeFound = true;
											variantNode.add(new DefaultMutableTreeNode(currBundle));
										} 
									} // end for - country node loop
									if (!variantNodeFound) {
										DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node"));
										countryNode.add(variantNode);
										variantNode.add(new DefaultMutableTreeNode(currBundle));
									}
								}
							}
						}
					}
				}
				if (!languageNodeFound) {
					// We need to create a node for this country
					DefaultMutableTreeNode langNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_language_node", language));
					root.add(langNode);
					if (country == null || country.equals("")) {
						langNode.add(new DefaultMutableTreeNode(currBundle));
					} else {
						// We need to look at the country, variant
						boolean countryNodeFound = false;
						for (int k=0; k < langNode.getChildCount(); k++) {
							DefaultMutableTreeNode countryNode = (DefaultMutableTreeNode)langNode.getChildAt(k);
							Object o2 = countryNode.getUserObject();
							if (o2 == null || !(o2 instanceof String)) continue;
							String str2 = (String)o2;
							if (str2.equals(Resources.getTranslation("tree_country_node", country))) {
								// There is a non-leaf node for this country
								countryNodeFound = true;
								if (variant == null || variant.equals("")) {
									countryNode.add(new DefaultMutableTreeNode(currBundle));
								} else {
									// We need to look at variant
									boolean variantNodeFound = false;
									for (int l=0; l < countryNode.getChildCount(); l++) {
										DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
										Object o3 = variantNode.getUserObject();
										if (o3 == null || !(o3 instanceof String)) continue;
										String str3 = (String)o3;
										if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
											variantNodeFound = true;
											variantNode.add(new DefaultMutableTreeNode(currBundle));
										} 
									} // end for - country node loop
									if (!variantNodeFound) {
										DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node"));
										countryNode.add(variantNode);
										variantNode.add(new DefaultMutableTreeNode(currBundle));
									}
								}
							}
						} // end for - language node loop
						if (!countryNodeFound) {
							DefaultMutableTreeNode countryNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_country_node", country));
							langNode.add(countryNode);
							if (variant == null || variant.equals("")) {
								countryNode.add(new DefaultMutableTreeNode(currBundle));
							} else {
								// We need to look at the variant	
								boolean variantNodeFound = false;
								for (int l=0; l < countryNode.getChildCount(); l++) {
									DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
									Object o3 = variantNode.getUserObject();
									if (o3 == null || !(o3 instanceof String)) continue;
									String str3 = (String)o3;
									if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
										variantNodeFound = true;
										variantNode.add(new DefaultMutableTreeNode(currBundle));
									} 
								} // end for - country node loop
								if (!variantNodeFound) {
									DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node", variant));
									countryNode.add(variantNode);
									variantNode.add(new DefaultMutableTreeNode(currBundle));
								}
							}
						}
					}
				}
			}
		}
		
		// Create the tree from the roots
		jTreeDisplay = new JTree(root);
		jTreeDisplay.addMouseListener(this);
		jTreeDisplay.addTreeSelectionListener(this);
		jTreeDisplay.setCellRenderer(RBTreeCellRenderer.getInstance());
		jScrollPaneTree.getViewport().removeAll();
		jScrollPaneTree.getViewport().add(jTreeDisplay);
		repaint();
		validateTree();
		return;
	}
	
	/**
	 * Inherits from JFrame.addNotify(), but also inserts the menu bar
	 */
	
	public void addNotify() 
	{
		super.addNotify();
		
		if (mShown)
			return;
			
		// resize frame to account for menubar
		JMenuBar jMenuBar = getJMenuBar();
		if (jMenuBar != null) {
			int jMenuBarHeight = jMenuBar.getPreferredSize().height;
			Dimension dimension = getSize();
			dimension.height += jMenuBarHeight;
			setSize(dimension);
		}

		mShown = true;
	}

	/**
	 * Called when it may be appropriate to check with the user if they want to save the file
	 */
	
	boolean promptForSave(String message) {
		if (rbm != null) {
			int response = JOptionPane.showConfirmDialog(this,
				(message == null ? Resources.getTranslation("dialog_save") : message),
				Resources.getTranslation("dialog_title_quit"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.CANCEL_OPTION) return false;
			if (response == JOptionPane.YES_OPTION) {
				return saveResources();
			}
		}
		return true;
	}
	
	public boolean deleteResources(String encoding) {
		if (rbm == null) return false;  // This should never happen
		try {
			rbm.eraseFile(encoding);	
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, Resources.getTranslation("error_deleting", ioe.getMessage()),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			if (RBManagerGUI.debug) System.err.println(ioe);
			return false;
		}
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		updateDisplayPanels();
		return true;
	}
	
	public void hideResources(String encoding) {
		rbm.hideResource(encoding);
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		updateDisplayPanels();
	}
	
	/**
	 * Save a particular resources file within the bundle.
	 */
	
	public boolean saveResources(String encoding) {
		if (rbm == null) return false;  // This should never happen
		return saveResources(rbm, encoding);
	}
	
	public boolean saveResources(RBManager bundle, String encoding) {
		try {
			bundle.writeToFile(encoding);	
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, Resources.getTranslation("error_saving", ioe.getMessage()),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			if (RBManagerGUI.debug) System.err.println(ioe);
			return false;
		}
		return true;
	}
	
	/**
	 * Called when the resources are to be saved
	 */
	
	public boolean saveResources() {
		if (rbm == null) return true;
		return saveResources(rbm);
	}
	
	public boolean saveResources(RBManager bundle) {
		try {
			bundle.writeToFile();
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, Resources.getTranslation("error_saving", ioe.getMessage()),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			if (RBManagerGUI.debug) System.err.println(ioe);
			return false;
		}
		return true;
	}
	
	/**
	 * Called when the resource bundle is to be saved, but displays a window to the user allowing them
	 * to selecte the file destination of the folder in which to save the bundle as well as the base
	 * class name for the bundle.
	 */
	
	public boolean saveResourcesAs() {
		if (rbm == null) return true;
		int result = saveFileChooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			try {
				File newFile = saveFileChooser.getSelectedFile();
				String fileName = newFile.getName();
				String baseName = fileName;
				if (fileName.toLowerCase().endsWith(".properties"))
					baseName = baseName.substring(0,baseName.length()-11);
				rbm.setBaseClass(baseName);
				rbm.setFileDirectory(newFile.getParentFile());
				rbm.writeToFile();
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(this, Resources.getTranslation("error_saving", ioe.getMessage()),
											  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				if (RBManagerGUI.debug) System.err.println(ioe);
				return false;
			}
		}
		return true;
	}
	
	void updateLocale(Locale l) {
		// Update the menubars
		jMenuBarMain.updateLocale();
		
		updateLocale(getContentPane(), l);
		updateLocale(openFileChooser, l);
		updateLocale(saveFileChooser, l);
		// Redraw the panes
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		updateDisplayPanels();
		// update the tab titles
		jTabbedPaneMain.setTitleAt(0,Resources.getTranslation("tab_statistics"));
		jTabbedPaneMain.setTitleAt(1,Resources.getTranslation("tab_untranslated"));
		jTabbedPaneMain.setTitleAt(2,Resources.getTranslation("tab_groups"));
		setTitle(Resources.getTranslation("resource_bundle_manager"));
	}
	
	static void updateLocale(Container c, Locale l) {
		Component comp[] = c.getComponents();
		for (int i=0; i < comp.length; i++) {
			if (comp[i] instanceof JComponent) {
				((JComponent)comp[i]).setLocale(l);
			}
			if (comp[i] instanceof Container) {
				updateLocale((Container)comp[i],l);
			}
		}
		if (c instanceof JMenu) {
			comp = ((JMenu)c).getMenuComponents();
			for (int i=0; i < comp.length; i++) {
				if (comp[i] instanceof JComponent) {
					((JComponent)comp[i]).setLocale(l);
				}
				if (comp[i] instanceof Container) {
					updateLocale((Container)comp[i],l);
				}
			}
		}
	}
			
	void updateUI() {
		updateUI(getContentPane());
		jMenuBarMain.updateUI();
		updateUI(jMenuBarMain);
		updateUI(openFileChooser);
		updateUI(saveFileChooser);
	}
	
	static void updateUI(Container c) {
		Component comp[] = c.getComponents();
		for (int i=0; i < comp.length; i++) {
			if (comp[i] instanceof JComponent) {
				((JComponent)comp[i]).updateUI();
			}
			if (comp[i] instanceof Container) {
				updateUI((Container)comp[i]);
			}
		}
		if (c instanceof JMenu) {
			comp = ((JMenu)c).getMenuComponents();
			for (int i=0; i < comp.length; i++) {
				if (comp[i] instanceof JComponent) {
					((JComponent)comp[i]).updateUI();
				}
				if (comp[i] instanceof Container) {
					updateUI((Container)comp[i]);
				}
			}
		}
	}
	
	// Close the window when the close box is clicked
	void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		if (promptForSave(Resources.getTranslation("dialog_quit_save"))) {
			setVisible(false);
			dispose();
			System.exit(0);
		}
	}
	
	public void setUser(String userName) {
		this.userName = userName;
		if (rbm != null) rbm.setUser(userName);
	}
	
	public String getUser() {
		return userName;
	}
	
	public BundleItem getSelectedProjectBundleItem() {
		TreePath path = projectTree.getSelectionPath();
		if (path == null) return null;
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		Object obj = node.getUserObject();
		if (obj == null || !(obj instanceof BundleItem)) return null;
		return (BundleItem)obj;
	}
	
	public RBManager getSelectedProjectBundle() {
		TreePath path = projectTree.getSelectionPath();
		if (path == null) return null;
		for (int i=0; i < path.getPathCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getPathComponent(i);
			Object obj = node.getUserObject();
			if (obj != null && obj instanceof RBManager) return (RBManager)obj;
		}
		return null;
	}
	
	public static void debugMsg(String msg) {
		if (debug) System.out.println("Debug Message [" + debugcount++ + "]: " + msg);
	}
}

class RBTreeCellRenderer extends DefaultTreeCellRenderer {
	private static RBTreeCellRenderer cellRend = null;
	private static ImageIcon bundleIcon   = null;
	private static ImageIcon languageIcon = null;
	private static ImageIcon countryIcon  = null;
	private static ImageIcon variantIcon  = null;
	private static ImageIcon fileIcon     = null;
	private static ImageIcon groupIcon    = null;
	private static ImageIcon itemIcon     = null;
	private static ImageIcon projectIcon  = null;
	
	private RBTreeCellRenderer() {
		
	}
	
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
												  boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
		int level = node.getLevel();
		Object obj = node.getUserObject();
		
		if (obj instanceof BundleGroup) {
			setIcon(groupIcon);
		} else if (obj instanceof BundleItem) {
			setIcon(itemIcon);
		} else if (obj instanceof RBManager) {
			setIcon(bundleIcon);
		} else if (obj instanceof RBProject) {
			setIcon(projectIcon);
		} else if (leaf) {
			if (level != 0) setIcon(fileIcon);	
		} else {	
			if (level == 0) {
				if (obj instanceof String && ((String)obj).equals(Resources.getTranslation("no_project"))) 
					setIcon(projectIcon);
				else setIcon(bundleIcon);
			}
			else if (level == 1) setIcon(languageIcon);
			else if (level == 2) setIcon(countryIcon);
			else if (level == 3) setIcon(variantIcon);
		}
		
		return this;
	}
	
	public static RBTreeCellRenderer getInstance() {
		if (cellRend == null) {
            try {
                Class thisClass = Class.forName("com.ibm.rbm.gui.RBManagerGUI");
			    // Create instances of the icons
                Image scaledImage = (new ImageIcon(thisClass.getResource("images/tree_icon_bundle.gif"))).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT);
			    bundleIcon   = new ImageIcon(scaledImage);
			    languageIcon = new ImageIcon(thisClass.getResource("images/tree_icon_language.gif"));
			    countryIcon  = new ImageIcon(thisClass.getResource("images/tree_icon_country.gif"));
			    variantIcon  = new ImageIcon(thisClass.getResource("images/tree_icon_variant.gif"));
			    fileIcon     = new ImageIcon(thisClass.getResource("images/tree_icon_file.gif"));
			    groupIcon    = new ImageIcon(thisClass.getResource("images/tree_icon_group.gif"));
			    itemIcon     = new ImageIcon(thisClass.getResource("images/tree_icon_item.gif"));
			    projectIcon  = new ImageIcon(thisClass.getResource("images/tree_icon_project.gif"));
            } catch (ClassNotFoundException e) {
                RBManagerGUI.debugMsg(e.toString());
            }
			// Create the instance of the renderer
			cellRend = new RBTreeCellRenderer();
		}
		return cellRend;
	}
}

/**
 * Table model for resource bundle projects
 */
class RBProject {
	java.util.List bundleNames;
	java.util.List bundleFileNames;
	java.util.List bundles;
	String projectName;
	
	public RBProject(String projectName) {
		this.projectName = projectName;
		bundleNames = new java.util.LinkedList();
		bundleFileNames = new java.util.LinkedList();
		bundles = new java.util.LinkedList();
	}
	
	public RBProject(File inputFile) throws IOException {
		this(inputFile.getName());
		
		if (projectName.indexOf(".") > 0) {
			projectName = projectName.substring(0,projectName.lastIndexOf("."));
		}
		
		FileReader fr = new FileReader(inputFile);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		int linecount = 0;
		while ((line = br.readLine()) != null) {
			if (linecount % 2 == 0) {
				bundleNames.add(line.trim());
			} else {
				bundleFileNames.add(line.trim());
			}
			linecount++;
		}
		fr.close();
		try {
			for (int i=0; i < bundleFileNames.size(); i++) {
				RBManager rbm = new RBManager(new File((String)bundleFileNames.get(i)));
				bundles.add(rbm);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(new JFrame(), Resources.getTranslation("error_load_project"),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
			bundleNames.clear();
			bundleFileNames.clear();
		}
	}
	
	public String toString() { return projectName; }
	
	public int getSize() { return bundleNames.size(); }
	
	public String getBundleName(int index) {
		return (String)bundleNames.get(index);	
	}
	
	public String getFileName(int index) {
		return (String)bundleFileNames.get(index);	
	}
	
	public RBManager getBundle(int index) {
		return (RBManager)bundles.get(index);
	}
	
	public RBManager getBundle(String bundleName) {
		int index = bundleNames.indexOf(bundleName);
		if (index >= 0) return getBundle(index);
		return null;
	}
	
	public void write(File outputFile) throws IOException {
		FileWriter fw = new FileWriter(outputFile);
		for (int i=0; i < bundleNames.size(); i++) {
			fw.write((String)bundleNames.get(i));
			fw.write("\n");
			fw.write((String)bundleFileNames.get(i));
			if (i != bundleNames.size()-1) fw.write("\n");
		}
		fw.flush();
		fw.close();
	}
	
	public void addBundle(String bundleFileName) throws IOException {
		RBManager bundle = new RBManager(new File(bundleFileName));
		bundles.add(bundle);
		bundleNames.add(bundle.getBaseClass());
		bundleFileNames.add(bundleFileName);
	}
	
	public void removeBundle(String bundleName) {
		int index = bundleNames.indexOf(bundleName);
		if (index >= 0) {
			bundleNames.remove(index);
			bundleFileNames.remove(index);
			bundles.remove(index);
		}
	}
}

class RBManagerMenuBar extends JMenuBar {
	RBManagerGUI             listener;
	
	JMenu        jMenuFile = new JMenu();                                     // Menu -> File
	JMenuItem    jMenuFileNewResourceBundle = new JMenuItem();                
	JMenuItem    jMenuFileOpenResourceBundle = new JMenuItem();
	JMenuItem    jMenuFileSaveResourceBundle = new JMenuItem();
	JMenuItem    jMenuFileSaveResourceBundleAs = new JMenuItem();
	JMenu        jMenuFileImportResourceBundle = new JMenu();                 // Menu -> File -> Import
	JMenuItem    jMenuFileImportJava = new JMenuItem();
	JMenuItem    jMenuFileImportProperties = new JMenuItem();
	JMenuItem    jMenuFileImportTMX = new JMenuItem();
	JMenuItem    jMenuFileImportXLF = new JMenuItem();
	JMenu        jMenuFileExportResourceBundle = new JMenu();                 // Menu -> File -> Export
	JMenuItem    jMenuFileExportJava = new JMenuItem();
	JMenuItem    jMenuFileExportICU = new JMenuItem();
	JMenuItem    jMenuFileExportProperties = new JMenuItem();
	JMenuItem    jMenuFileExportTMX = new JMenuItem();
	JMenuItem    jMenuFileExportXLF = new JMenuItem();
	JMenuItem    jMenuFileExit = new JMenuItem();
	JMenu        jMenuEdit = new JMenu();                                     // Menu -> Edit
	JMenuItem    jMenuEditCut = new JMenuItem();
	JMenuItem    jMenuEditCopy = new JMenuItem();
	JMenuItem    jMenuEditPaste = new JMenuItem();
	JMenuItem    jMenuEditDelete = new JMenuItem();
	JMenu        jMenuOptions = new JMenu();                                  // Menu -> Options
	JMenuItem    jMenuOptionsAddNewEntry = new JMenuItem();
	JMenuItem    jMenuOptionsAddNewGroup = new JMenuItem();
	JMenuItem    jMenuOptionsAddNewResourceFile = new JMenuItem();
	//JMenuItem    jMenuOptionsProjectViewer = new JMenuItem();
	JMenuItem    jMenuOptionsPreferences = new JMenuItem();
	JMenu        jMenuView = new JMenu();                                     // Menu -> View
	JMenuItem    jMenuViewViewStatistics = new JMenuItem();
	JMenu        jMenuHelp = new JMenu();                                     // Menu -> Help
	JMenuItem    jMenuHelpAboutResourceBundleManager = new JMenuItem();
	
	void updateLocale() {
		//FILE
		jMenuFile.setText(Resources.getTranslation("menu_file"));
		jMenuFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_trigger")));
		jMenuFileNewResourceBundle.setText(Resources.getTranslation("menu_file_new"));
		jMenuFileNewResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_new_trigger")));
		jMenuFileOpenResourceBundle.setText(Resources.getTranslation("menu_file_open"));
		jMenuFileOpenResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_open_trigger")));
		jMenuFileSaveResourceBundle.setText(Resources.getTranslation("menu_file_save"));
		jMenuFileSaveResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_save_trigger")));
		jMenuFileSaveResourceBundleAs.setText(Resources.getTranslation("menu_file_saveas"));
		jMenuFileSaveResourceBundleAs.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_saveas_trigger")));
		jMenuFileImportResourceBundle.setText(Resources.getTranslation("menu_file_import"));
		jMenuFileImportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_trigger")));
		jMenuFileImportJava.setText(Resources.getTranslation("menu_file_import_java"));
		jMenuFileImportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_java_trigger")));
		jMenuFileImportProperties.setText(Resources.getTranslation("menu_file_import_properties"));
		jMenuFileImportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_properties_trigger")));
		jMenuFileImportTMX.setText(Resources.getTranslation("menu_file_import_TMX"));
		jMenuFileImportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_TMX_trigger")));
		jMenuFileImportXLF.setText(Resources.getTranslation("menu_file_import_XLF"));
		jMenuFileImportXLF.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_XLF_trigger")));
		jMenuFileExportResourceBundle.setText(Resources.getTranslation("menu_file_export"));
		jMenuFileExportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_trigger")));
		jMenuFileExportJava.setText(Resources.getTranslation("menu_file_export_java"));
		jMenuFileExportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_java_trigger")));
		jMenuFileExportICU.setText(Resources.getTranslation("menu_file_export_ICU"));
		jMenuFileExportICU.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_ICU_trigger")));
		jMenuFileExportProperties.setText(Resources.getTranslation("menu_file_export_properties"));
		jMenuFileExportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_properties_trigger")));
		jMenuFileExportTMX.setText(Resources.getTranslation("menu_file_export_TMX"));
		jMenuFileExportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_TMX_trigger")));
		jMenuFileExportXLF.setText(Resources.getTranslation("menu_file_export_XLF"));
		jMenuFileExportXLF.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_XLF_trigger")));
		jMenuFileExit.setText(Resources.getTranslation("menu_file_quit"));
		jMenuFileExit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_quit_trigger")));
		//EDIT
		jMenuEdit.setText(Resources.getTranslation("menu_edit"));
		jMenuEdit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_trigger")));
		jMenuEditCut.setText(Resources.getTranslation("menu_edit_cut"));
		jMenuEditCut.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_cut_trigger")));
		jMenuEditCopy.setText(Resources.getTranslation("menu_edit_copy"));
		jMenuEditCopy.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_copy_trigger")));
		jMenuEditPaste.setText(Resources.getTranslation("menu_edit_paste"));
		jMenuEditPaste.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_paste_trigger")));
		jMenuEditDelete.setText(Resources.getTranslation("menu_edit_delete"));
		jMenuEditDelete.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_delete_trigger")));
		//OPTIONS
		jMenuOptions.setText(Resources.getTranslation("menu_options"));
		jMenuOptions.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_trigger")));
		jMenuOptionsAddNewEntry.setText(Resources.getTranslation("menu_options_addentry"));
		jMenuOptionsAddNewEntry.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addentry_trigger")));
		jMenuOptionsAddNewGroup.setText(Resources.getTranslation("menu_options_addgroup"));
		jMenuOptionsAddNewGroup.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addgroup_trigger")));
		jMenuOptionsAddNewResourceFile.setText(Resources.getTranslation("menu_options_addfile"));
		jMenuOptionsAddNewResourceFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addfile_trigger")));
		//jMenuOptionsProjectViewer.setText(Resources.getTranslation("menu_options_project_viewer"));
		//jMenuOptionsProjectViewer.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_project_viewer_trigger")));
		jMenuOptionsPreferences.setText(Resources.getTranslation("menu_options_preferences"));
		jMenuOptionsPreferences.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_preferences_trigger")));
		//VIEW
		jMenuView.setText(Resources.getTranslation("menu_view"));
		jMenuView.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_trigger")));
		jMenuViewViewStatistics.setText(Resources.getTranslation("menu_view_statistics"));
		jMenuViewViewStatistics.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_statistics_trigger")));
		//HELP
		jMenuHelp.setText(Resources.getTranslation("menu_help"));
		jMenuHelp.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_trigger")));
		jMenuHelpAboutResourceBundleManager.setText(Resources.getTranslation("menu_help_about"));
		jMenuHelpAboutResourceBundleManager.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_help_about_trigger")));
	}
	
	public RBManagerMenuBar(RBManagerGUI gui) {
		super();	
		
        boolean xmlAvailable;
		try {
            Class.forName("org.apache.xerces.parsers.DOMParser");
            Class.forName("javax.xml.parsers.DocumentBuilder");
            xmlAvailable = true;
        } catch (ClassNotFoundException e) {
            xmlAvailable = false;
        }
		listener = gui;
		
		// Add the menus to the menu bar 
		setVisible(true);
		add(jMenuFile);
		//add(jMenuEdit);
		add(jMenuOptions);
		//add(jMenuView);
		add(jMenuHelp);

		// Add File Menu Items to the File Menu
		jMenuFile.setVisible(true);
		jMenuFile.setText(Resources.getTranslation("menu_file"));
		jMenuFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_trigger")));
		jMenuFile.add(jMenuFileNewResourceBundle);
		jMenuFile.add(jMenuFileOpenResourceBundle);
		jMenuFile.add(jMenuFileSaveResourceBundle);
		jMenuFile.add(jMenuFileSaveResourceBundleAs);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileImportResourceBundle);
		jMenuFile.add(jMenuFileExportResourceBundle);
		jMenuFile.addSeparator();
		// Add the recent files to the file menu
		Vector recentFiles = Preferences.getRecentFilesPreferences();
		if (recentFiles.size() > 0) {
			for (int i=0; i < recentFiles.size(); i+=2) {
				String name = (String)recentFiles.elementAt(i);
				String location = (String)recentFiles.elementAt(i+1);
				JMenuItem recentMenuItem = new JMenuItem();
				recentMenuItem.setVisible(true);
				recentMenuItem.setText(name);
				recentMenuItem.setName("__" + location.trim());
				recentMenuItem.addActionListener(listener);
				jMenuFile.add(recentMenuItem);
			}
			jMenuFile.addSeparator();
		}
		jMenuFile.add(jMenuFileExit);
		
		//jMenuFileImportResourceBundle.add(jMenuFileImportJava);
		jMenuFileImportResourceBundle.add(jMenuFileImportProperties);
        jMenuFileImportTMX.setEnabled(xmlAvailable);
		jMenuFileImportResourceBundle.add(jMenuFileImportTMX);
        jMenuFileImportXLF.setEnabled(xmlAvailable);
		jMenuFileImportResourceBundle.add(jMenuFileImportXLF);
		jMenuFileExportResourceBundle.add(jMenuFileExportJava);
		jMenuFileExportResourceBundle.add(jMenuFileExportICU);
		jMenuFileExportResourceBundle.add(jMenuFileExportProperties);
        jMenuFileExportTMX.setEnabled(xmlAvailable);
		jMenuFileExportResourceBundle.add(jMenuFileExportTMX);
        jMenuFileExportXLF.setEnabled(xmlAvailable);
		jMenuFileExportResourceBundle.add(jMenuFileExportXLF);

		jMenuFileNewResourceBundle.setVisible(true);
		jMenuFileNewResourceBundle.setText(Resources.getTranslation("menu_file_new"));
		jMenuFileNewResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_new_trigger")));
		jMenuFileNewResourceBundle.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		jMenuFileNewResourceBundle.addActionListener(listener);
		
		jMenuFileOpenResourceBundle.setVisible(true);
		jMenuFileOpenResourceBundle.setText(Resources.getTranslation("menu_file_open"));
		jMenuFileOpenResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_open_trigger")));
		jMenuFileOpenResourceBundle.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		jMenuFileOpenResourceBundle.addActionListener(listener);

		jMenuFileSaveResourceBundle.setVisible(true);
		jMenuFileSaveResourceBundle.setText(Resources.getTranslation("menu_file_save"));
		jMenuFileSaveResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_save_trigger")));
		jMenuFileSaveResourceBundle.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		jMenuFileSaveResourceBundle.addActionListener(listener);

		jMenuFileSaveResourceBundleAs.setVisible(true);
		jMenuFileSaveResourceBundleAs.setText(Resources.getTranslation("menu_file_saveas"));
		jMenuFileSaveResourceBundleAs.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_saveas_trigger")));
		jMenuFileSaveResourceBundleAs.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		jMenuFileSaveResourceBundleAs.addActionListener(listener);
		
		jMenuFileImportResourceBundle.setVisible(true);
		jMenuFileImportResourceBundle.setText(Resources.getTranslation("menu_file_import"));
		jMenuFileImportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_trigger")));
		jMenuFileImportResourceBundle.addActionListener(listener);
		
		jMenuFileImportJava.setVisible(true);
		jMenuFileImportJava.setText(Resources.getTranslation("menu_file_import_java"));
		jMenuFileImportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_java_trigger")));
		jMenuFileImportJava.addActionListener(listener);
		
		jMenuFileImportProperties.setVisible(true);
		jMenuFileImportProperties.setText(Resources.getTranslation("menu_file_import_properties"));
		jMenuFileImportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_properties_trigger")));
		jMenuFileImportProperties.addActionListener(listener);
		
		jMenuFileImportTMX.setVisible(true);
		jMenuFileImportTMX.setText(Resources.getTranslation("menu_file_import_TMX"));
		jMenuFileImportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_TMX_trigger")));
		jMenuFileImportTMX.addActionListener(listener);
		
		jMenuFileImportXLF.setVisible(true);
		jMenuFileImportXLF.setText(Resources.getTranslation("menu_file_import_XLF"));
		jMenuFileImportXLF.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_XLF_trigger")));
		jMenuFileImportXLF.addActionListener(listener);
		
		jMenuFileExportResourceBundle.setVisible(true);
		jMenuFileExportResourceBundle.setText(Resources.getTranslation("menu_file_export"));
		jMenuFileExportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_trigger")));
		jMenuFileExportResourceBundle.addActionListener(listener);
		
		jMenuFileExportJava.setVisible(true);
		jMenuFileExportJava.setText(Resources.getTranslation("menu_file_export_java"));
		jMenuFileExportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_java_trigger")));
		jMenuFileExportJava.addActionListener(listener);
		
		jMenuFileExportICU.setVisible(true);
		jMenuFileExportICU.setText(Resources.getTranslation("menu_file_export_ICU"));
		jMenuFileExportICU.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_ICU_trigger")));
		jMenuFileExportICU.addActionListener(listener);

		jMenuFileExportProperties.setVisible(true);
		jMenuFileExportProperties.setText(Resources.getTranslation("menu_file_export_properties"));
		jMenuFileExportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_properties_trigger")));
		jMenuFileExportProperties.addActionListener(listener);
		
		jMenuFileExportTMX.setVisible(true);
		jMenuFileExportTMX.setText(Resources.getTranslation("menu_file_export_TMX"));
		jMenuFileExportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_TMX_trigger")));
		jMenuFileExportTMX.addActionListener(listener);
		
		jMenuFileExportXLF.setVisible(true);
		jMenuFileExportXLF.setText(Resources.getTranslation("menu_file_export_XLF"));
		jMenuFileExportXLF.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_XLF_trigger")));
		jMenuFileExportXLF.addActionListener(listener);

		jMenuFileExit.setVisible(true);
		jMenuFileExit.setText(Resources.getTranslation("menu_file_quit"));
		jMenuFileExit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_quit_trigger")));
		jMenuFileExit.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		jMenuFileExit.addActionListener(listener);

		// Add Edit Menu Items to the Edit Menu
		jMenuEdit.setVisible(true);
		jMenuEdit.setText(Resources.getTranslation("menu_edit"));
		jMenuEdit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_trigger")));
		jMenuEdit.add(jMenuEditCut);
		jMenuEdit.add(jMenuEditCopy);
		jMenuEdit.add(jMenuEditPaste);
		jMenuEdit.add(jMenuEditDelete);

		jMenuEditCut.setVisible(true);
		jMenuEditCut.setText(Resources.getTranslation("menu_edit_cut"));
		jMenuEditCut.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_cut_trigger")));
		jMenuEditCut.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_X, ActionEvent.CTRL_MASK));

		jMenuEditCopy.setVisible(true);
		jMenuEditCopy.setText(Resources.getTranslation("menu_edit_copy"));
		jMenuEditCopy.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_copy_trigger")));
		jMenuEditCopy.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_C, ActionEvent.CTRL_MASK));

		jMenuEditPaste.setVisible(true);
		jMenuEditPaste.setText(Resources.getTranslation("menu_edit_paste"));
		jMenuEditPaste.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_paste_trigger")));
		jMenuEditPaste.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		
		jMenuEditDelete.setVisible(true);
		jMenuEditDelete.setText(Resources.getTranslation("menu_edit_delete"));
		jMenuEditDelete.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_delete_trigger")));

		// Add Options Menu Items to the Options Menu
		jMenuOptions.setVisible(true);
		jMenuOptions.setText(Resources.getTranslation("menu_options"));
		jMenuOptions.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_trigger")));
		jMenuOptions.add(jMenuOptionsAddNewEntry);
		jMenuOptions.add(jMenuOptionsAddNewGroup);
		jMenuOptions.add(jMenuOptionsAddNewResourceFile);
		//jMenuOptions.addSeparator();
		//jMenuOptions.add(jMenuOptionsProjectViewer);
		jMenuOptions.addSeparator();
		jMenuOptions.add(jMenuOptionsPreferences);

		jMenuOptionsAddNewEntry.setVisible(true);
		jMenuOptionsAddNewEntry.setText(Resources.getTranslation("menu_options_addentry"));
		jMenuOptionsAddNewEntry.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addentry_trigger")));
		jMenuOptionsAddNewEntry.addActionListener(listener);
		
		jMenuOptionsAddNewGroup.setVisible(true);
		jMenuOptionsAddNewGroup.setText(Resources.getTranslation("menu_options_addgroup"));
		jMenuOptionsAddNewGroup.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addgroup_trigger")));
		jMenuOptionsAddNewGroup.addActionListener(listener);

		jMenuOptionsAddNewResourceFile.setVisible(true);
		jMenuOptionsAddNewResourceFile.setText(Resources.getTranslation("menu_options_addfile"));
		jMenuOptionsAddNewResourceFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addfile_trigger")));
		jMenuOptionsAddNewResourceFile.addActionListener(listener);
		
		/*
		jMenuOptionsProjectViewer.setVisible(true);
		jMenuOptionsProjectViewer.setText(Resources.getTranslation("menu_options_project_viewer"));
		jMenuOptionsProjectViewer.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_project_viewer_trigger")));
		jMenuOptionsProjectViewer.addActionListener(listener);
		*/
		
		jMenuOptionsPreferences.setVisible(true);
		jMenuOptionsPreferences.setText(Resources.getTranslation("menu_options_preferences"));
		jMenuOptionsPreferences.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_preferences_trigger")));
		jMenuOptionsPreferences.addActionListener(listener);

		// Add View Menu Items to the View Menu
		jMenuView.setVisible(true);
		jMenuView.setText(Resources.getTranslation("menu_view"));
		jMenuView.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_trigger")));
		jMenuView.add(jMenuViewViewStatistics);

		jMenuViewViewStatistics.setVisible(true);
		jMenuViewViewStatistics.setText(Resources.getTranslation("menu_view_statistics"));
		jMenuViewViewStatistics.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_statistics_trigger")));

		// Add Help Menu Items to the Help Menu
		jMenuHelp.setVisible(true);
		jMenuHelp.setText(Resources.getTranslation("menu_help"));
		jMenuHelp.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_help_trigger")));
		jMenuHelp.add(jMenuHelpAboutResourceBundleManager);

		jMenuHelpAboutResourceBundleManager.setVisible(true);
		jMenuHelpAboutResourceBundleManager.setText(Resources.getTranslation("menu_help_about"));
		jMenuHelpAboutResourceBundleManager.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_help_about_trigger")));
		jMenuHelpAboutResourceBundleManager.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_H, ActionEvent.CTRL_MASK));
		jMenuHelpAboutResourceBundleManager.addActionListener(listener);
	}
	
	public static int getKeyEventKey(String character) {
		if (character == null) return KeyEvent.VK_A;
		character = character.toUpperCase();
	
		if (character.startsWith("A")) return KeyEvent.VK_A;
		else if (character.startsWith("B")) return KeyEvent.VK_B;
		else if (character.startsWith("C")) return KeyEvent.VK_C;
		else if (character.startsWith("D")) return KeyEvent.VK_D;
		else if (character.startsWith("E")) return KeyEvent.VK_E;
		else if (character.startsWith("F")) return KeyEvent.VK_F;
		else if (character.startsWith("G")) return KeyEvent.VK_G;
		else if (character.startsWith("H")) return KeyEvent.VK_H;
		else if (character.startsWith("I")) return KeyEvent.VK_I;
		else if (character.startsWith("J")) return KeyEvent.VK_J;
		else if (character.startsWith("K")) return KeyEvent.VK_K;
		else if (character.startsWith("L")) return KeyEvent.VK_L;
		else if (character.startsWith("M")) return KeyEvent.VK_M;
		else if (character.startsWith("N")) return KeyEvent.VK_N;
		else if (character.startsWith("O")) return KeyEvent.VK_O;
		else if (character.startsWith("P")) return KeyEvent.VK_P;
		else if (character.startsWith("Q")) return KeyEvent.VK_Q;
		else if (character.startsWith("R")) return KeyEvent.VK_R;
		else if (character.startsWith("S")) return KeyEvent.VK_S;
		else if (character.startsWith("T")) return KeyEvent.VK_T;
		else if (character.startsWith("U")) return KeyEvent.VK_U;
		else if (character.startsWith("V")) return KeyEvent.VK_V;
		else if (character.startsWith("W")) return KeyEvent.VK_W;
		else if (character.startsWith("X")) return KeyEvent.VK_X;
		else if (character.startsWith("Y")) return KeyEvent.VK_Y;
		else if (character.startsWith("Z")) return KeyEvent.VK_Z;
		else if (character.startsWith("0")) return KeyEvent.VK_0;
		else if (character.startsWith("1")) return KeyEvent.VK_1;
		else if (character.startsWith("2")) return KeyEvent.VK_2;
		else if (character.startsWith("3")) return KeyEvent.VK_3;
		else if (character.startsWith("4")) return KeyEvent.VK_4;
		else if (character.startsWith("5")) return KeyEvent.VK_5;
		else if (character.startsWith("6")) return KeyEvent.VK_6;
		else if (character.startsWith("7")) return KeyEvent.VK_7;
		else if (character.startsWith("8")) return KeyEvent.VK_8;
		else if (character.startsWith("9")) return KeyEvent.VK_9;

		return KeyEvent.VK_A;
	}
	
}
