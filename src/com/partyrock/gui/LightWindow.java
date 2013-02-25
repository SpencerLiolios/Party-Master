package com.partyrock.gui;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.partyrock.LightMaster;
import com.partyrock.anim.ElementAnimation;
import com.partyrock.element.ElementController;
import com.partyrock.gui.elements.ElementDisplay;
import com.partyrock.gui.elements.ElementTableRenderer;
import com.partyrock.gui.elements.ElementUpdater;
import com.partyrock.gui.elements.ElementsEditor;
import com.partyrock.gui.uc.UCEditor;
import com.partyrock.tools.PartyToolkit;

/**
 * The main GUI window
 * @author Matthew
 * 
 */
public class LightWindow implements ElementTableRenderer, ElementDisplay {
	private LightMaster master;
	private LightWindowManager windowManager;
	private Shell shell;
	private ToolBar toolbar;
	private ScrolledComposite tableScroll;
	private Table table;
	private ElementsEditor editor;
	private LightWindowElementsTableRenderer tableRenderer;

	public LightWindow(LightMaster master, LightWindowManager manager) {
		this.master = master;
		this.windowManager = manager;

		tableRenderer = new LightWindowElementsTableRenderer(master, this);

		this.windowManager.addElementDisplay(this);

		// Construct the GUI shell
		this.shell = new Shell(manager.getDisplay());
		shell.setText("Light Master");

		// Layout with 1 column, and no equal width columns
		shell.setLayout(new GridLayout(1, false));

		// Create Toolbar
		toolbar = new ToolBar(shell, SWT.HORIZONTAL);

		// Generate Toolbar items
		ToolItem elementsEditorButton = new ToolItem(toolbar, SWT.PUSH);
		elementsEditorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				showElementsEditor();
			}
		});
		elementsEditorButton.setText("Elements Editor");

		ToolItem ucEditorButton = new ToolItem(toolbar, SWT.PUSH);
		ucEditorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				showUCEditor();
			}
		});
		ucEditorButton.setText("�C Editor");

		// Finish Toolbar init
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolbar.pack();

		// Main table scroll container
		tableScroll = new ScrolledComposite(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tableScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		tableScroll.setExpandHorizontal(true);
		tableScroll.setExpandVertical(true);

		// Actually make the table
		table = new Table(tableScroll, SWT.MULTI | SWT.FULL_SELECTION);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				tableClick(e);
			}
		});
		table.setHeaderVisible(false);
		table.setLinesVisible(false);
		table.setToolTipText("");

		// Do the custom rendering stuff for the table
		tableRenderer.addCustomListeners(table);

		// Set the scroll contianer's content
		tableScroll.setContent(table);

		// Construct the table's columns (label + content)
		int columnCount = 2;
		for (int a = 0; a < columnCount; a++) {
			TableColumn column = new TableColumn(table, SWT.NONE);

			// This is hidden anyways, only here to help debugging if necessary
			column.setText("Column " + a);
		}

		// Load in TableItems
		ElementUpdater.updateElements(this, table);

		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText("File");

		Menu menu_1 = new Menu(mntmFile);
		mntmFile.setMenu(menu_1);

		MenuItem mntmSaveLocation = new MenuItem(menu_1, SWT.NONE);
		mntmSaveLocation.setAccelerator(SWT.MOD1 + 'S');
		mntmSaveLocation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				saveLocationFile();
			}
		});
		mntmSaveLocation.setText("Save Elements");

		MenuItem mntmSaveLocationAs = new MenuItem(menu_1, SWT.NONE);
		mntmSaveLocationAs.setAccelerator(SWT.MOD1 + SWT.SHIFT + 'S');
		mntmSaveLocationAs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				saveLocationFileAs();
			}
		});
		mntmSaveLocationAs.setText("Save Elements As");

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmLoadLocation = new MenuItem(menu_1, SWT.NONE);
		mntmLoadLocation.setAccelerator(SWT.MOD1 + 'O');
		mntmLoadLocation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				loadLocation();
			}
		});
		mntmLoadLocation.setText("Load Location");

		shell.open();

	}

	/**
	 * Performs the GUI loop. This method will not return until the GUI closes.
	 */
	public void loop() {
		while (!shell.isDisposed()) {
			if (!windowManager.getDisplay().readAndDispatch()) {
				windowManager.getDisplay().sleep();
			}
		}

		windowManager.getDisplay().dispose();
	}

	/**
	 * Adds a specific element to the end of the table, and sets the element as
	 * the item's data.
	 * @param element the element to add
	 */
	public void addElementAsRow(ElementController element) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setData(element);

		// This is awkwardly necessary here
		ElementUpdater.packTable(table);
	}

	/**
	 * Saves the location file, or pops open the dialog if it doesn't exist
	 */
	public void saveLocationFile() {
		if (master.getLocationManager().getLocation() == null) {
			// We don't have a location right now, so make a new one
			this.saveLocationFileAs();
		} else {
			// The file exists, just save it
			master.getLocationManager().saveLocationFile();
		}
	}

	/**
	 * Saves a new location file (pops up the file prompt)
	 */
	public void saveLocationFileAs() {
		File f = getLocationFileFromDialog(SWT.SAVE);

		if (f.exists()) {
			boolean override = PartyToolkit.openConfirm(shell, "Are you sure you wish to override the existing location file "
					+ f.getName() + "?", "Overwrite?");
			if (override) {
				// If we don't delete, PersistentSettings automatically merges
				// the files which would be very bad (some of the old elements
				// might hang around, but only some, and it would depend on the
				// number of new elements. Basically, it would be very bad.)

				f.delete();
			} else {
				return;
			}
		}
		if (f != null) {
			master.getLocationManager().saveLocationToFile(f);
		}
	}

	public void loadLocation() {
		File f = getLocationFileFromDialog(SWT.OPEN);
		if (f != null) {
			master.getLocationManager().loadLocation(f);
		}
	}

	/**
	 * Gets a new location file from a new FileDialog
	 * @param style
	 * @return
	 */
	public File getLocationFileFromDialog(int style) {
		FileDialog dialog = new FileDialog(shell, style);
		dialog.setFilterNames(new String[] { "Location Files", "All Files (*.*)" });
		dialog.setFilterExtensions(new String[] { "*.loc", "*.*" });
		dialog.setFilterPath(".");
		dialog.setFileName("location.loc");
		String fileName = dialog.open();
		if (fileName != null && !fileName.trim().equals("")) {
			try {
				File file = new File(fileName);
				return file;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.err.println("Error saving location file");
		return null;
	}

	public LightMaster getMaster() {
		return master;
	}

	public Shell getShell() {
		return shell;
	}

	/**
	 * Shows an ElementsEditor
	 */
	public void showElementsEditor() {
		if (editor != null && !editor.isDisposed()) {
			editor.getShell().forceActive();
		} else {
			editor = new ElementsEditor(this);
			windowManager.addElementDisplay(editor);
			editor.open();
		}
	}

	/**
	 * Updates the elements in the table
	 */
	public void updateElements() {
		ElementUpdater.updateElements(this, table);
	}

	public void showUCEditor() {
		UCEditor editor = new UCEditor(this);
		editor.open();

	}

	@Override
	public boolean isDisposed() {
		return shell.isDisposed();
	}

	public LightWindowManager getWindowManager() {
		return windowManager;
	}

	/**
	 * Returns the width of the window
	 */
	public int getWidth() {
		return shell.getSize().x;
	}

	/**
	 * Returns the height of the window
	 */
	public int getHeight() {
		return shell.getSize().y;
	}

	/**
	 * Returns the size of the window
	 * @return
	 */
	public Point getSize() {
		return shell.getSize();
	}

	public Display getDisplay() {
		return windowManager.getDisplay();
	}

	public ArrayList<ElementController> getSelectedElements() {
		ArrayList<ElementController> ret = new ArrayList<ElementController>();
		for (TableItem item : table.getSelection()) {
			ElementController element = (ElementController) item.getData();
			ret.add(element);
		}
		return ret;
	}

	public void tableClick(MouseEvent event) {
		// 3rd button, right click
		if (event.button == 3) {
			// Dynamically construct the popup menu based on what's selected
			Menu menu = new Menu(table.getShell(), SWT.POP_UP);

			MenuItem previewAnimation = new MenuItem(menu, SWT.CASCADE);
			previewAnimation.setText("Preview Animation");

			Menu previewAnimationMenu = new Menu(previewAnimation);
			final ArrayList<ElementController> selectedElements = getSelectedElements();
			ArrayList<Class<? extends ElementAnimation>> animationList = master.getAnimationManager().getAnimationListForElements(selectedElements);

			// Add each available animation to the menu
			for (final Class<? extends ElementAnimation> c : animationList) {
				MenuItem menuItem = new MenuItem(previewAnimationMenu, SWT.PUSH);
				menuItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						ElementAnimation animation = null;
						try {
							animation = c.getConstructor(LightMaster.class, int.class, ArrayList.class).newInstance(master, -1, selectedElements);
							previewAnimation(animation);
						} catch (Exception ex) {
							System.out.println("There was an error previewing the animation " + animation
									+ " - Check that it has the correct constructor");
							ex.printStackTrace();
						}
					}
				});
				menuItem.setText(c.getSimpleName());
				menuItem.setData(c);
			}

			previewAnimation.setMenu(previewAnimationMenu);
			Point eventPoint = new Point(event.x, event.y);
			eventPoint = table.toDisplay(eventPoint);
			menu.setLocation(eventPoint);
			menu.setVisible(true);

		}
	}

	/**
	 * Executes an animation immediately
	 * @param animation
	 */
	public void previewAnimation(ElementAnimation animation) {
		animation.trigger();
	}
}
