/*
 * Created on 05.07.2007
 *
 */
package jPasi.codec;

import java.util.LinkedList;
import java.util.List;

import jPasi.item.DependentItem;

public abstract class DIInfo {

    public Class<? extends DependentItem> cl;
    public LinkedList<DIInfo> composition;
    public float gap0, gap1;
    public String info;
    public boolean inverse;
    public float priority;

    public String toString() {
        return "["+cl.getSimpleName()+" "+priority+" "+(info==null? "null": "\""+info+"\"")+" "+
        		composition+" "+inverse+"]";
    }

	public List<DIInfo> getComposition() {
        return composition;
    }

	public Class<? extends DependentItem> getDIClass() {
        return cl;
    }

	public boolean getInverse() {
        return inverse;
    }

	public float getPriority() {
        return priority;
    }

	public String getInfoString() {
        return info;
    }

	public float getGap0() {
        return gap0;
    }

	public float getGap1() {
        return gap1;
    }

}
