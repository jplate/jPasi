/*
 * Created on 03.07.2007
 *
 */
package jPasi.edit;


/**
 * The convention for calling the methods of this Listener is that they should preferably be called by the 
 * ActionListeners, ChangeListeners, etc., of editor controls, and only from the main thread.
 * The editPerformed method should be called <strong>before</strong> the corresponding edit is performed. If the edit fails, 
 * editFailed() should be called with the same EditEvent as parameter. Unless this method is called, the EditorListener will 
 * assume that the edit has succeeded.
 * 
 * The purpose of the startNewPeriod method is to allow for implementations to collect minor edits into more significant ones.
 */
public interface EditListener {

	public void editing(EditEvent e);
	
	public void startNewPeriod(EditEvent e);
	
	/**
	 * Signals that the specified edit attempt (which should be the last one made) has failed.
	 */
	public void editFailed(EditEvent e);
	
}
