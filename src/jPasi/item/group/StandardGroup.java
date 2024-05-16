/*
 * Created on 16.05.2007
 *
 */
package jPasi.item.group;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import util.MapCloneable;


/**
 * Whenever one of the members of a group is highlighted, all of them are (handled by Canvas).
 * 
 */
public class StandardGroup<T extends GroupMember<?>> 
		implements MapCloneable, GroupMember<StandardGroup<?>>, Group<T> {

	public static final int MINIMUM_SIZE = 1;
    protected StandardGroup<?> group;
    protected boolean inGroup;

    public Collection<T> members;
   
    public StandardGroup() {
    }
        
    /**
     * Creates a group that has all the members of the specified Collection as its members. The new StandardGroup then becomes
     * the 'group' of its members, and these are removed from any old groups they may have belonged to.
     */
    public StandardGroup(Collection<? extends GroupMember<?>> l) {
        members = new LinkedHashSet<T>();
        Set<T> s = new LinkedHashSet<T>(); // we need this as a temporary store, or the following won't work.
        for(GroupMember<?> gm: l) {
    		Group<? extends GroupMember<?>> hg = Groups.getHighestActiveGroup(gm);
    		if(!(hg instanceof GroupMember)) {
    			hg = Groups.getHighestActiveGroupBut(1, gm);
    		}
            @SuppressWarnings("unchecked")
			T newMember = (T)(hg==null? gm: hg);
            Group<? extends GroupMember<?>> current = newMember.getGroup();
            if(current!=null) {
                current.remove(gm);
            }
            s.add(newMember);
        }
        for(T gm: s) {
            add(gm);
            gm.setInGroup(true);
        }
    }

    public boolean accepts(GroupMember<?> gm) {
        return true;
    }
    
    public Collection<T> getMembers() {
    	return members;
    }

    /**
     * @see jp.pasi.item.group.GroupMember#acceptsAsGroup()
     */
    public boolean acceptsAsGroup(Group<?> g) {
        return true;
    }
	
    public synchronized boolean add(GroupMember<?> m) {
	    if(!accepts(m)) {
	        throw new IllegalArgumentException("Member not accepted: "+m);
	    }
	    if(m instanceof GroupMemberDelegator) {
	    	m = ((GroupMemberDelegator<?>)m).groupMemberDelegate();
	    }
	    boolean b = members.add((T)m);
		m.setGroup(this);
	    return b;
	}
    
    @Override
    @SuppressWarnings("unchecked")
    public Object clone() throws CloneNotSupportedException {
        StandardGroup<T> clone = (StandardGroup)super.clone();
        clone.members = cloneMembers();
        return clone;
    }

    /**
     * Cloning of Groups is assumed to be 'member-driven'.
     */
    @SuppressWarnings("unchecked")
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
    	StandardGroup clone;
        synchronized(map) {
	        if(map.values().contains(this)) {
	            clone = this;
	        } else {
	            clone = (StandardGroup)map.get(this);
	        }
	        if(clone==null) {
	            clone = (StandardGroup)this.clone();	            
	            map.put(this, clone);
	            if(group!=null) {
	        	    /*
	        	     *  Our own clone is put into a clone of our own group if we are inGroup (i.e. active member); if not, it 
	        	     *  is put into our own group.
	        	     */
	                if(inGroup && group!=null) {
	                    clone.group = (StandardGroup<?>)group.clone(map);
	                    clone.group.getMembers().add(clone);
	                    clone.group.getMembers().remove(this);
	                } else {
	                    group.add(clone);
	                }
	            }
	            clone.inGroup = inGroup;
	        }
	    	//System.err.println("clg: "+(clone.members==members)+" orientation: "+(clone==this));
        }
        return clone;
    }
    
    /**
     * Subclasses that support clone() but represent their members by a type of Collection different from HashSet 
     * should override this method.
     */
    protected Collection<T> cloneMembers() {
        Collection<T> c = new LinkedHashSet<T>();
        c.addAll(members);
    	return c;
    }
    
    public boolean contains(Object o) {
    	return members.contains(o);
    }
/*
	public Rectangle getBounds() {
	    if(members.size()==0) {
	        return new Rectangle(0, 0, 0, 0);
	    }
	    Rectangle leftMost = null,
	    	topMost = null,
	    	rightMost = null,
	    	bottomMost = null;
	    for (GroupMember element : members) {
	        if(element instanceof Item) {
		    	Item item = (Item)element;
		        Rectangle rx = item.getBounds();
		        if(leftMost==null || leftMost.getX()>rx.getX()) {
		            leftMost = rx;
		        }
		        if(topMost==null || topMost.getY()>rx.getY()) {
		            topMost = rx;
		        }
		        if(rightMost==null || rightMost.getX()<rx.getX()) {
		            rightMost = rx;
		        }
		        if(bottomMost==null || bottomMost.getY()<rx.getY()) {
		            bottomMost = rx;
		        }
		    }
	    }
	    if(leftMost!=null) {
		    double x = leftMost.getX();
		    double y = topMost.getY();
		    return new Rectangle((int)x, (int)y, 
		            (int)(rightMost.getX()+rightMost.getWidth()-x), 
		            (int)(bottomMost.getY()+bottomMost.getHeight()-y));
	    } else {
	    	throw new IllegalArgumentException("Members should be of type Item");
	    }
	}
*/
    public StandardGroup<?> getGroup() {
        return group;
    }

    public boolean hasNonActiveMembers() {
        boolean found = false;
        for(Iterator<?> i = members.iterator(); !found && i.hasNext();) {
            GroupMember<?> gm = (GroupMember<?>)i.next();
            found = !gm.isInGroup();
        }
        return found;
    }
    
    public boolean indirectlyContains(GroupMember<?> m) {
        boolean found = this==m;
        for(Iterator<?> i = members.iterator(); !found && i.hasNext();) {
            Object o = i.next();
            found = o==m || (o instanceof Group && ((Group<?>)o).indirectlyContains(m));
        }
        return found;
    }

    public boolean isDefunct() {
        return members.size() < MINIMUM_SIZE;
    }

    public boolean isInGroup() {
        return inGroup;
    }	
    
    public Iterator<T> iterator() {
        return members.iterator();
    }
    
    /**
     * Constructs and returns a set containing all the members of this Group.
     */
    public Set<T> memberSet() {
        Set<T> result = new LinkedHashSet<T>();
        result.addAll(members);
        return result;
    }
	
    public synchronized void remove(GroupMember<?> gm) {
	    members.remove(gm);
	    Group<?> g = gm.getGroup(); // may be different from this Group if it has been added to another one.
	    if(g==this) {
		    gm.setInGroup(false);
		    gm.setGroup(null);
	    }
	    if(isDefunct()) {
	        if(group!=null) {
	            for (T m : members) {
		            group.add(m);
		        }
		        group.remove(this);
	        } else {
	            for (T m : members) {
		            m.setInGroup(false);
		            m.setGroup(null);
		        }
	        }
	    }
	}
    
    public void setGroup(Group<?> g) {
	    if(g instanceof GroupMember && indirectlyContains((GroupMember<?>)g)) {	        
	        throw new IllegalArgumentException("Argument must not be direct or indirect member or identical to this Group.");
	    }
    	group = (StandardGroup<?>)g;
    }
    
    public void setInGroup(boolean b) {
        inGroup = b;
    }
	
	public void setMembersActive(boolean b) {
	    for(Object m : members) {
	        GroupMember<?> gm = (GroupMember<?>)m;
	        gm.setInGroup(b);
	    }
	}
    
    public int size() {
        return members.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()+" (");
        for (Iterator<T> i = members.iterator(); i.hasNext();) {
        	T t = i.next();
            sb.append(t).append(i.hasNext()? ", ": "");
        }
        return sb.append(")").toString();
    }
    
 }
