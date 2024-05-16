/*
 * Created on 17.05.2007
 *
 */
package jPasi.item.group;

/**
 * @author Jan Plate
 *
 */
public interface GroupMember<T extends Group<?>> {

    public boolean acceptsAsGroup(Group<?> g);
    
    public T getGroup();
    
    public boolean isInGroup();
    
    public void setGroup(Group<?> g); // type is Group so that implementations can throw their own Exceptions.
    
    public void setInGroup(boolean b);

}
