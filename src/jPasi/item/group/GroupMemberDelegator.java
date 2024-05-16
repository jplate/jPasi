/*
 * Created on 12.06.2007
 *
 */
package jPasi.item.group;

public interface GroupMemberDelegator<T extends Group<?>> extends GroupMember<T> {
	
	public GroupMember<T> groupMemberDelegate();

}
