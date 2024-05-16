/*
 * Created on 12.06.2007
 *
 */
package jPasi.item.group;

import java.util.List;
import java.util.Stack;

public class Groups {

	@SuppressWarnings("unchecked")
    private static Stack<Group<?>> collectActiveGroups(GroupMember<?> gm, Stack<Group<?>> st) {
        Group group = gm.getGroup();
        if(group!=null && gm.isInGroup()) {
        	st.push(group);
            if(group instanceof GroupMember) {
                return collectActiveGroups((GroupMember)group, st);
            }
            else {
            	return st;
            }
        } else {
        	return st;
        }
    }
    
    public static Group<?> getHighestActiveGroup(GroupMember<?> gm) {
    	Group<?> group = gm.getGroup();
    	if(gm.isInGroup()) {
	        if(group instanceof GroupMember<?>) {
	        	return getHighestActiveGroup((GroupMember<?>)group);
	        } else {
	        	return group;
	        }
    	} else if(gm instanceof Group) {
    		return (Group<?>)gm;
    	} else {
    		return null;
    	}
    }

    public static Group<?> getHighestActiveGroupBut(int n, GroupMember<?> gm) {
        List<Group<?>> l = collectActiveGroups(gm, new Stack<Group<?>>());
        int m = l.size();
        if(m>n) {
            return l.get(m-n-1);
        }
        else {
            return null;
        }
    }
    
    /*
     * result should be initialized to [0, 0]. Then the method will return with result [k, n], where k is the number of 
     * group levels upwards from gm such that at every level, the corresponding group is an 'active' member (isInGroup) of the
     * next higher group, and n is the total number of 'available' group levels.
     */
    public static void getGroupLevels(GroupMember<?> gm, int[] result, boolean b) {
        Group<?> g = gm.getGroup();
        if(g instanceof GroupMember) {
            b = b || !gm.isInGroup();
            if(!b) result[0]++;
            result[1]++;
            getGroupLevels((GroupMember<?>) g, result, b);
        }
    }

}
