/*
 * Created on 12.06.2007
 *
 */
package jPasi.item.group;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public interface Group<T extends GroupMember<?>> {

	public Collection<T> getMembers();

	public boolean accepts(GroupMember<?> gm);
	
	public boolean add(GroupMember<?> m); // type is GroupMember so that implementations can throw their own exceptions.

	public boolean hasNonActiveMembers();

	public boolean indirectlyContains(GroupMember<?> m);

	public boolean isDefunct();

	public Iterator<?> iterator();

	/**
	 * Constructs and returns a set containing all the members of this Group.
	 */
	public Set<T> memberSet();

	public void remove(GroupMember<?> gm);

	public void setMembersActive(boolean b);

}