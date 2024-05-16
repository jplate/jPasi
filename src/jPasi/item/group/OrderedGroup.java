/*
 * Created on 03.06.2007
 *
 */
package jPasi.item.group;

/**
 * @author Jan Plate
 *
 */
public interface OrderedGroup<T extends GroupMember<?>> extends Group<T> {

    public int indexOf(T gm);

    /**
     * Adds the specified GroupMember to this OrderedGroup at the specified index, unless (perhaps) it already contains
     * that GroupMember. The type is unparameterized so that implementations can check the type at runtime and throw
     * their own Exceptions.
     * @return true if the call resulted in a structural change of this Group.
     */
    public boolean add(int index, GroupMember<?> gm);
    
    public T get(int index);
}
