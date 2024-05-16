/*
 * Created on 21.05.2007
 *
 */
package jPasi.edit;

import java.util.List;
import java.util.Set;

/**
 * @author Jan Plate
 *
 */
public interface Editable {

    /**
     * Returns a List of EditorEntries. The supplied argument serves to make it possible to have different such Lists for
     * each Editable that can then be handled separately (e.g., by different EditorPanes).
     */
    public List<EditorEntry> getInfo(EditorTopic topic);
    
    /**
     * The returned delegate will not only determine (via getInfo(EditorTopic)) the editor components, but also the class
     * under which these components are registered in an EditorPane.
     */
	public Editable getEditorDelegate(EditorTopic topic);
	
	/**
	 * Returns the set of Editables that an EditorPane is meant to operate on when an EditorEntry is declared as 'global'. 
	 * For every such EditorEntry in the list returned by getInfo(), the EditorPane will apply the corresponding method
	 * to all members of that set that are instances of that method's declaring class.
	 */
	public Set<Editable> getGlobalSet(EditorTopic topic);

	
}
