/*
 * Created on 21.06.2007
 *
 */
package jPasi.edit;

import java.util.EventObject;
import java.util.List;

public class EditorPaneEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    protected List<EditorEntry> entries;
    
	EditorPaneEvent(EditorPane source, List<EditorEntry> entries) {
		super(source);
		this.entries = entries;
	}
	
	public List<EditorEntry> getEntries() {
		return entries;
	}
	
}
