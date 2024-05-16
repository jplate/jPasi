/*
 * Created on 14.12.2006
 *
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.*;

import java.awt.Container;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jPasi.TransformModifier;
import jPasi.edit.Editable;
import jPasi.edit.EditorEntry;
import jPasi.edit.EditorTopic;
import jPasi.item.group.Group;
import jPasi.item.group.GroupManager;
import jPasi.item.group.GroupMember;
import jPasi.item.group.GroupUIDelegator;
import jPasi.item.group.OrderedGroup;
import jPasi.item.group.StandardGroup;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public abstract class Ornament extends DependentItem implements GroupMember<StandardGroup<?>>, GroupUIDelegator {
    
	public static final float DEFAULT_GAP = 1f;

	public boolean inGroup;
	protected StandardGroup<?> group;
    
    double chi = 0; // preferred angle
	double psi = 0; // actual angle
    
	protected EditorEntry angleInfo;
	protected GroupManager<?> groupManager = new GroupManager<Ornament>(this);
	
	public Ornament() {}
	
	public Ornament(ENode e) {
		this.involutes = new ENode[1];
		this.involutes[0] = e;
		e.addDependentItem(this);
		setGovernor(e);
		chi = getDefaultPreferredAngle();
	}
	
    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0) {
    		float factor = (float)t.getScaleX();
    		if(TransformModifier.isScalingNodes(modifier)) {    			
        		float gapa = gap*factor;
        		if(ok(gapa)) {
        			gap = gapa;
        		}
    		}
    	}
    }
    
    /* (non-Javadoc)
     * @see util.MapCloneable#clone(java.util.Map)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
	    Ornament clone = (Ornament)super.clone(map);
	    if(clone!=this && group!=null) {
	    	clone.group = (StandardGroup)group.clone(map);
	    }
	    
	    if(group!=null) {
		    /*
		     *  clone.group is a clone of group. Our own clone is put into this group-clone if we are inGroup 
		     * (i.e. active member); if not, it is put into our own group.
		     */
	        int index = -1;
	        if(group instanceof OrderedGroup) {
	            index = ((OrderedGroup)group).indexOf(this);
	        }
		    clone.group.getMembers().remove(this);
	        if(group instanceof OrderedGroup) {
	            ((OrderedGroup)(inGroup? clone.group: group)).add(index, clone);
	        } else {
	            (inGroup? clone.group: group).add(clone);
	        }
		}
	    return clone;
    }

    /* 
	 * @see pasi.item.State#getAngle(int)
	 */
	public double getAngle(int k) {
		if(k!=0) throw new IllegalArgumentException("Illegal argument: "+k);
		else return psi;
	}

    public abstract float getDefaultPreferredAngle();

	public List<EditorEntry> getInfo() {
	    if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    result.addAll(super.getInfo());

		    angleInfo = new EditorEntry("PreferredAngleDegrees_EP", INTEGER, "Preferred Angle");
		    angleInfo.setSpinnerValues((float)Math.toDegrees(getDefaultPreferredAngle()), -180, 180, 10, true);
		    angleInfo.setGlobal(true);

		    result.add(angleInfo);		    
		    this.info = result;
	    }
	    return info;
	}
	
	/* 
	 * @see pasi.item.State#getItemLocation()
	 */
	public Point2D getItemLocation() {
		return getLocation();
	}

    
	/**
	 * @see jPasi.item.DependentItem#getPreferredAngle(int)
	 */
	public double getPreferredAngle(int k) {
		return chi;
	}

	public int getPreferredAngleDegrees_EP() {
	    return (int)Math.round(Math.toDegrees(getPreferredAngle(0)));
	}	
		
	/**
	 * @see jPasi.item.DependentItem#removeInvolute(pasi.item.Entity)
	 */
	public void removeInvolute(Item i2) {
		Container c = getParent();
		if(c!=null) {
			c.remove(this);
		}
	}

	/* 
	 * @see pasi.item.State#setAngle(double, int)
	 */
	public void setAngle(double a, int k) {
		if(k!=0) throw new IllegalArgumentException("Illegal argument: "+k);
		else psi = MathTools.normalize(a, 0);
	}

	/* 
	 * @see pasi.item.State#setItemLocation(java.awt.geom.Point2D)
	 */
	public Point2D setItemLocation(Point2D p) {
		// TODO change location
		return getItemLocation();
	}

	public void setPreferredAngle(double chi, boolean arrangeContacts) {
	    this.chi = chi;
    	invalidate();
	    if(arrangeContacts) {
	    	((ENode)getGovernor()).arrangeContacts();
	    	getCanvas().relocateItems();
	    }
	}
	
	public void setPreferredAngleDegrees_EP(int degrees) {
	    setPreferredAngle(Math.toRadians(degrees), true);
	}
    
    protected void shapeChanged() {
        super.shapeChanged();
        ((ENode)getGovernor()).arrangeContacts();
    }

	public boolean shouldBeHidden() {
	    return false;
	}
	
    public void setGovernor(Item gov) {
        super.setGovernor(gov);
        involutes[0] = (ENode)gov;
    }


    public StandardGroup<?> getGroup() {
        return group;
    }
    
	public void setGroup(Group<?> g) {
	    group = (StandardGroup<?>)g;
    }

    public boolean isInGroup() {
        return inGroup;
    }	

    public void setInGroup(boolean b) {
        inGroup = b;
    }
    
    public int getItemIndex() {
        return 2;
    }
    
    public boolean acceptsAsGroup(Group<?> g) {
    	return g instanceof StandardGroup;
    }

	public Editable getEditorDelegate(EditorTopic topic) {
		switch(topic) {
		case GROUPS: {
	    	return getGroupManager();
	    }
	    default: 
	    	return super.getEditorDelegate(topic);
	    }
    }

	public GroupManager<?> getGroupManager() {
	    return groupManager;
    }		

}
