/*
 * Created on 13.06.2007
 *
 */
package jPasi.item.group;

import static jPasi.edit.EditorEntry.Type.ACTION;
import static jPasi.edit.EditorEntry.Type.BOOLEAN;
import static jPasi.edit.EditorEntry.Type.LABEL;
import static jPasi.edit.EditorEntry.Type.TOGGLE;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jPasi.Canvas;
import jPasi.edit.Editable;
import jPasi.edit.EditorEntry;
import jPasi.edit.EditorTopic;
import jPasi.item.Item;
import util.MapCloneable;
import util.MapCloningException;

public class GroupManager<T extends Item & GroupMember<?>> implements Editable, MapCloneable {

	public static boolean canDissolveGroup(GroupMember<?> client) {
        return client.isInGroup();
    } 

    public static boolean canLeaveGroup(GroupMember<?> client) {
        return client.isInGroup();
    }
    
    public static boolean canRejoinGroup(GroupMember<?> client) {
        Group<?> hg = Groups.getHighestActiveGroup(client);        
        if(hg!=null && !(hg instanceof GroupMember<?>)) {        	
        	hg = Groups.getHighestActiveGroupBut(1, client);
        }
    	GroupMember<?> gm1 = (GroupMember<?>)(hg!=null? hg: client);
        Group<?> gg = gm1.getGroup();
        return !gm1.isInGroup() && gg!=null;
    }
    
    public static boolean canRestoreGroup(GroupMember<?> client) {
        Group<?> g = Groups.getHighestActiveGroup(client);
        return g!=null && g.hasNonActiveMembers();
    }
    
    public static Group<?> createGroup(GroupMember<?> client) {
    	Canvas canvas = ((Item)client).getCanvas();
    	Set<?> selection = canvas.getSelectedSet();
        LinkedList<GroupMember<?>> l = new LinkedList<GroupMember<?>>();
        for (Item item : canvas.getSelectedSet()) {
            if(item instanceof GroupMember<?>) {
                GroupMember<?> gm = (GroupMember<?>)item;
                Group<?> hg = Groups.getHighestActiveGroup(gm);
            	if(!(hg instanceof GroupMember<?>)) {
            		hg = Groups.getHighestActiveGroupBut(1, gm);
            	}
                if(hg!=null) {
               		l.add((GroupMember<?>)hg);
                } else {
                	l.add(gm);
                }
            }
        }
        Group<?> result = new StandardGroup<GroupMember<?>>(l);
        return result;
    }
    
    public static void dissolveGroup(GroupMember<?> client) {
    	Canvas canvas = ((Item)client).getCanvas();
        Group<?> g = Groups.getHighestActiveGroup(client);
        if(g!=null) {
            g.setMembersActive(false);
        }
        canvas.setAdding(false);
        canvas.clearSelection((Item)client);
    }

    public static void leaveGroup(GroupMember<?> client) {
    	Canvas canvas = ((Item)client).getCanvas();
    	GroupMember<?> gm = (GroupMember<?>)Groups.getHighestActiveGroupBut(1, client);
        if(gm==null) { // then group will be the Group to leave, and this Entity the GroupMember<?> to leave it.
            gm = client;
        }
        gm.setInGroup(false);
        canvas.setAdding(false);
        canvas.clearSelection((Item)client);
    }

    public static void rejoinGroup(GroupMember<?> client) {
    	Canvas canvas = ((Item)client).getCanvas();
        Group<?> g = Groups.getHighestActiveGroup(client);
        GroupMember<?> gm;
        if(g instanceof GroupMember<?>) {
        	gm = (GroupMember<?>)g;
        } else {
        	gm = (GroupMember<?>)Groups.getHighestActiveGroupBut(1, client);
        }
        gm = gm!=null? gm: (GroupMember<?>)client;
        if(gm.getGroup()!=null) {
            gm.setInGroup(true);
        }
        canvas.selectGroup(client);
	    canvas.setItemToBeEdited((Item)client);
    }
    
    public static void restoreGroup(GroupMember<?> client) {
    	Canvas canvas = ((Item)client).getCanvas();
        Group<?> g = Groups.getHighestActiveGroup(client);
        if(g!=null) {
            g.setMembersActive(true);
        }
        canvas.selectGroup(client);
	    canvas.setItemToBeEdited((Item)client);
    }
    
    public T client;
    
    protected List<EditorEntry> groupInfo;

    protected EditorEntry addMembersInfo;    
    protected EditorEntry addInfo;    
    protected EditorEntry groupLevelInfo;

    public GroupManager() {
    }
    public GroupManager(T gm) {
    	this.client = gm;
    }
    
    public boolean canChangeAdding() {
    	return client.isInGroup();
    }
    
    public boolean canChangeAddingMembers() {
    	Canvas canvas = ((Item)client).getCanvas();
    	return client.isInGroup() && canvas.isAdding();
    }
    
    public boolean canCreate() {
    	return true;
    }
    
    public boolean canDissolve() {
    	return canDissolveGroup(client);
    }
    
    public boolean canLeave() {
        return canLeaveGroup(client);
    }
	 
    public boolean canRejoin() {
    	return canRejoinGroup(client);
    }
    
    public boolean canRestore() {
    	return canRestoreGroup(client);
    }
    
    @SuppressWarnings("unchecked")
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
	    GroupManager<T> clone; 
        if(map.values().contains(this)) {
            clone = this;
        } else {
            clone = (GroupManager)map.get(this);
        }
        if(clone==null) {
        	T cc = (T)map.get(client);
        	if(cc==null) {
        		throw new MapCloningException("Client clone not found");
        	}
        	clone = new GroupManager<T>(cc);
        }
        return clone;
    }
    
    public void doCreate() {
    	createGroup(client);
    }
    
    public void doDissolve() {
    	dissolveGroup(client);
    }
    
    public void doLeave() {
    	GroupManager.leaveGroup(client);
    }
    
    public void doRejoin() {
    	rejoinGroup(client);
    }
    
    public void doRestore() {
    	restoreGroup(client);
    }

    public void externalSetAdding(boolean b) {
		if(canChangeAdding()) {
			setAdding(b);			
			if(addInfo!=null) addInfo.notify(this);
		}
	}
    
    public void externalSetAddingMembers(boolean b) {
		if(canChangeAdding()) {
			setAddingMembers(b);
			if(addMembersInfo!=null) addMembersInfo.notify(this);
		}
	}
    
	public Editable getEditorDelegate(EditorTopic topic) {
	    return null;
    }

	public Set<Editable> getGlobalSet(EditorTopic topic) {
		return null; // we won't need it.
	}
    
    public List<EditorEntry> getGroupInfo() {
	    if(groupInfo==null) {
	        ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    
	        EditorEntry createInfo = new EditorEntry("Create", ACTION, "Create Group");
		    createInfo.setTopSpace(2);
		    createInfo.setReconfigurationAction();
		    result.add(createInfo);
		    
		    groupLevelInfo = new EditorEntry("GroupLevelInfo", LABEL, "Group Level:");
		    groupLevelInfo.setTopSpace(3);
		    result.add(groupLevelInfo);
	
		    EditorEntry leaveInfo = new EditorEntry("Leave", ACTION, "Leave Group");
		    leaveInfo.setTopSpace(3);
		    result.add(leaveInfo);
		    
		    EditorEntry rejoinInfo = new EditorEntry("Rejoin", ACTION, "Rejoin");
		    result.add(rejoinInfo);
		    
		    EditorEntry dissolveInfo = new EditorEntry("Dissolve", ACTION, "Dissolve");
		    result.add(dissolveInfo);
	
		    EditorEntry restoreInfo = new EditorEntry("Restore", ACTION, "Restore");
		    result.add(restoreInfo);
		    
		    addInfo = new EditorEntry("Adding", TOGGLE, "Add...");
		    addInfo.requestNotifier();
		    result.add(addInfo);
		    
		    addMembersInfo = new EditorEntry("AddingMembers", BOOLEAN, "All members");
		    addMembersInfo.requestNotifier();
		    result.add(addMembersInfo);
		    
		    result.trimToSize();
		    groupInfo = result;
	    }

	    return groupInfo;
	}
    
	public String getGroupLevelInfo() {
        int[] result = new int[2];
        Groups.getGroupLevels(client, result, false);
        return ""+result[0]+"/"+result[1];
    }   
    
    public List<EditorEntry> getInfo(EditorTopic topic) {
	    switch(topic) {
	    case GROUPS: return getGroupInfo();
	    default: return null;	    
	    }
    }

	public boolean isAdding() {
        return client.getCanvas().isAdding();
    }

	public boolean isAddingMembers() {
        return client.getCanvas().isAddingMembers() ;
    }
	
	public boolean requiresResetButton(EditorTopic topic) {
	    return false;
    }

	public void setAdding(boolean b) {
        client.getCanvas().setAdding(b);
        if(addMembersInfo!=null) addMembersInfo.notify(this);
    }

	public void setAddingMembers(boolean b) {
        client.getCanvas().setAddMembers(b);
    }

	public void setClient(T client) {
    	this.client = client;
    }

}
