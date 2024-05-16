/*
 * Created on 11.11.2006
 *
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.STRING;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jPasi.Canvas;
import jPasi.edit.Editable;
import jPasi.edit.EditorEntry;
import jPasi.edit.EditorTopic;

public abstract class DependentItem extends Item {

	
	private static final long serialVersionUID = -8156061175000300434L;

	public static final float DEFAULT_GAP = .4f;

	public static final Comparator<DependentItem> priorityComparator = new Comparator<DependentItem>() { 
		public int compare(DependentItem s0, DependentItem s1) {
		    float p0 = s0.getTruePriority();
		    float p1 = s1.getTruePriority();
			if(!Float.isNaN(p0) && !Float.isNaN(p1)) { 
				return p0<p1? -1: p0==p1? 0: 1; 
			}
			else if(!Float.isNaN(p0)) {
				return -1;
			}
			else if(!Float.isNaN(p1)) {
				return 1;
			}
			else return 0;
		}
	};

    /**
     * The gap between a DependentItem and its Involute(s)
     */
    public float gap = DEFAULT_GAP;

    protected boolean gapChangeable = true;

	@SuppressWarnings("unchecked")
	protected IndependentItem[] involutes = null; 
	protected Item item = null; // an Item that will be added to the canvas to provide for marking capability
	public float oldPriority;
	public float priority = 0f;
    protected EditorEntry priorityInfo;
    public boolean priorityResettable;
    public boolean priorityChangeable = true;
	
	protected volatile Shape shape = null;
	
    public DependentItem() {
        super(null);
        gap = getDefaultGap();
    }

    public boolean canChangeGap() {
        return gapChangeable;
    }

    /*
	public boolean canChangeNegated() {
	    return item instanceof ENode;
	}	
    */
    
    public boolean canChangePriority_EP() {
        return priorityChangeable;
    }

    public boolean canResetPriority() {
        return priorityResettable;
    }

    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
        return clone(map, true);
    }
    
    /**
     * The boolean parameter has been added for the sake of CompoundArrow's element-cloning.
     */
    @SuppressWarnings("unchecked")
	public Object clone(Map<Object, Object> map, boolean add) throws CloneNotSupportedException {
        DependentItem clone = (DependentItem)super.clone(map);
        clone.setItem(clone.item);
        
        IndependentItem[] inv = clone.getInvolutes();
        for(int i = 0; i<inv.length; i++) {
            IndependentItem ic = (IndependentItem)map.get(inv[i]);
            
            if(i!=getItemIndex()) {
	            if(ic!=null) {
	                inv[i].removeDependentItem(clone);
	                inv[i] = ic;
	            }
	            if(add) {
	                inv[i].addDependentItem(clone);
	            }
            }
        }
        return clone;
    }
	
	protected Shape computeShape() {
	    return null;
	}

	public void defineBounds() {
	    if(!boundsValid || shape==null) {
	        Shape shape = getShape();
	        if(shape!=null) {
		        Rectangle2D.Float r = (Rectangle2D.Float)shape.getBounds2D();
			    r.x -= xMargin;
			    r.y -= yMargin; 
			    r.width += 2*xMargin;
			    r.height += 2*yMargin;
			    setBounds2D(r);
			    boundsValid = true;
	        }
	    }
	}
	
	public abstract double getAngle(int k);
	
	public Rectangle2D getBounds2D() {
	    defineBounds();
	    return super.getBounds2D();
	}

    public float getDefaultGap() {
        return DEFAULT_GAP;
    }
    
    public float getGap() {
        return gap;
    }
	public Set<Editable> getGlobalSet(EditorTopic topic) {
	    HashSet<Editable> result = new HashSet<Editable>();
	    Canvas c = getCanvas();
	    for(Item item: c.getSelectedSet()) {
	    	result.add(item.isDelegate()? item.getGovernor(): item);
	    }
		return result;
    }
	
	public List<EditorEntry> getInfo() {
	    if(info==null) {
		    info = getPriorityInfo();
	    }
	    return info;
	}
		
	protected synchronized List<EditorEntry> getPriorityInfo() {
	    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
	    priorityInfo = new EditorEntry("Priority_EP", FLOAT, "Priority");
	    priorityInfo.setSpinnerValues(Float.NaN, 0f, Float.POSITIVE_INFINITY, 1f, false);
	    priorityInfo.setGlobal(true);		
		    
	    result.add(priorityInfo);
	    result.trimToSize();
	    return result;
	}

	/**
	 * @return an array containing the Entities that are ontologically involved in this DependentItem. 
	 * If one of these is this DependentItem's 'item', it should be the <strong>last</strong> element in 
	 * the array. The other involutes should occur in the same order in which they would appear as arguments 
	 * of the corresponding predicate.
	 */
	public IndependentItem<?>[] getInvolutes() {
		return involutes;
	}
    
	public Item getItem() {
		return item;
	}
  	
	/**
	 * @return the index in the array of involutes at which the DependentItem's item is found, if it is 
	 * among them. If not, the method should return some number that lies outside of the bounds of that 
	 * array. 
	 */
	public abstract int getItemIndex();
	
	/*
	 * @return top-left corner of this DependentItem's item
	 */
	public abstract Point2D getItemLocation();

	public abstract double getPreferredAngle(int k);
	
	public float getPriority() {
		return priority;
	}
	
	public float getPriority_EP() {
		return priority;
	}

    /**
	 * Returns the Shape of this DependentItem. May be called at construction time.
	 * @return
	 */
	public Shape getShape() {
	    if(shape==null && getInvolutes()!=null) { // second clause necessary because computeShape normally 
	        		// depends on involutes being non-null
	        shape = computeShape();
	    }
	    return shape;
	}
	
	public float getTruePriority() {
        return canResetPriority()? oldPriority: priority;
    }

    public void invalidate() {
		super.invalidate();
		shape = null;
		boundsValid = false;
	}
/*
	public boolean isNegated() {
	    return item instanceof ENode? ((ENode)item).isNegated(): false;
	}
    
	public boolean isNegated_g() {
	    return isNegated();
	}
*/
	/**
	 * Remove an Entity involved in this DependentItem. Implementations should take all necessary 
	 * measures, including self-destruction and self-removal from other Entities.
	 *
	 */
	public abstract void removeInvolute(Item i2);

    public void repaint() {
	    defineBounds();
	    super.repaint();
	}

	public void resetPriority() {
        priority = oldPriority;
        priorityResettable = false;
        priorityChangeable = true;
    }

    public abstract void setAngle(double a, int k);

	public void setGap(float gap) {
        this.gap = gap;
        shapeChanged();
    }
	
	public void setItem(Item item) {
		this.item = item;
	}
	
	/**
	 * Sets the top-left corner of the bounding rectangle of this DependentItem's item so that its center 
	 * is a close to p as possible while satisfying the constraints imposed by the DependentItem.
	 */
	public abstract Point2D setItemLocation(Point2D p);	
/*	
	public void setNegated(boolean b) {
	    if(item instanceof ENode) {
	        ((ENode)item).setNegated(b);
	    }
	}	
	
	/**
	 * Invoked by the EditorPane.
	 */
/*	public void setNegated_g(boolean b) {
        Canvas c = getCanvas();
        ENode.setNegated_g(c.getSelectedSet(), b);
	}
*/
	public void setPriority(float priority) {
		this.priority = priority;
	}
	
    public void setPriority_EP(float priority) {
		Canvas c = getCanvas();
		float d = priority - getPriority();
		for(Item item: c.getSelectedSet()) {
			DependentItem s = item instanceof DependentItem? (DependentItem)item:
					item.getGovernor() instanceof DependentItem? (DependentItem)item.getGovernor(): null;
			if(s!=null) {
				s.setTruePriority(s.getTruePriority() + d);
			}
		}
	}

	public void setTruePriority(float p) {
        if(canResetPriority()) {
            oldPriority = p;
        } else {
            priority = p;
        }
    }

    protected void shapeChanged() {
        shape = null;
        boundsValid = false;
        repaint();
	}
    
    /**
     * The priority that a DependentItem of this class typically assumes under 'special' circumstances 
     * (in which the priority becomes resettable) 
     */
    public float specialPriority() {
        return 0;
    }
    
	public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()+"[");
        IndependentItem<?>[] inv = getInvolutes();
        for(int i = 0; i<inv.length; i++) {
            sb.append(inv[i] + (i<inv.length-1? " ": ""));
        }        
        return sb.append("]").toString();
    }

}

