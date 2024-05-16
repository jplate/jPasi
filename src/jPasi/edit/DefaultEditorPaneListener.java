/*
 * Created on 02.07.2007
 *
 */
package jPasi.edit;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DefaultEditorPaneListener implements EditorPaneListener {
	
	private class TabbedPaneListener implements ChangeListener {
		boolean activated;
		
		public void stateChanged(ChangeEvent ce) {
			if(activated) {
    			showWhenPossible = false;
    			activated = false;
			}
		}
	}
	
	private boolean showWhenPossible;
	private JTabbedPane tabbedPane;
	private TabbedPaneListener tpListener = new TabbedPaneListener();
	
	public DefaultEditorPaneListener(JTabbedPane tabbedPane) {
		this.tabbedPane = tabbedPane;
		tabbedPane.addChangeListener(tpListener);
	}
	
	public void panelChanged(EditorPaneEvent epe) {
		List<EditorEntry> entries = epe.getEntries();
		EditorPane pane = (EditorPane)epe.getSource();
		int i = tabbedPane.indexOfComponent(pane);
		
		boolean enable = entries!=null && entries.size()>0;
		tabbedPane.setEnabledAt(i, enable);
		int j = tabbedPane.getSelectedIndex();
		int n = -1;
		if(!enable && i==j) {
			changeTab(0);
			showWhenPossible = true;
			tpListener.activated = true;
		} else if(enable && showWhenPossible) {
			changeTab(i);
		}
    }
	
	private void changeTab(int n) {
		if(n>=0) {
			KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager(); 
			Component c = tabbedPane.getComponentAt(n);
			tabbedPane.setSelectedIndex(n);
			kfm.focusNextComponent(c);
		}
	}
}