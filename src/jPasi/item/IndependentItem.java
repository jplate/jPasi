/*
 * Created on 27.05.2007
 *
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.STRING;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jPasi.Canvas;
import jPasi.edit.Editable;
import jPasi.edit.EditorEntry;
import jPasi.edit.EditorTopic;
import jPasi.item.group.Group;
import jPasi.item.group.GroupManager;
import jPasi.item.group.GroupMember;
import jPasi.item.group.GroupUIDelegator;

/**
 * @author Jan Plate
 *
 */
public abstract class IndependentItem<T extends Group<?>> extends Item implements GroupMember<T>, GroupUIDelegator {

	private static final long serialVersionUID = -266568428072984317L;
	
	public boolean inGroup;
    protected float radius;
	protected EditorEntry radiusInfo;
	protected EditorEntry xInfo;
	protected EditorEntry yInfo;

    protected T group; // not MapCloneable because the NodeGroup of an SNode should be accessible only via 
    			// its states field and Contour.
	protected GroupManager<IndependentItem<?>> groupManager = new GroupManager<IndependentItem<?>>(this);
    
    public IndependentItem() {
        super();        
    }

    public IndependentItem(Canvas c) {
        super(c);
    }

    public IndependentItem(Canvas c, Item governor, boolean hidden) {
        super(c, governor, hidden);
    }
    
    public abstract void arrangeContacts();
    
    public abstract void arrangeContacts(Set<Item> affected, Set<DependentItem> immuneStates);
    
    public void changePosition(float x, float y) {
        Rectangle2D.Float r = bounds2d;
        float dx = x-r.x;
        float dy = y-r.y;
        Canvas c = getCanvas();
        c.prepareForMovingSelection();
        c.moveSelectionBy(dx, dy);
        c.relocateItems();
    }    
    
    public abstract float getDefaultRadius();

    public Editable getEditorDelegate(EditorTopic topic) {
		switch(topic) {
		case GROUPS: {
	    	return getGroupManager();
	    }
	    default: 
	    	return super.getEditorDelegate(topic);
	    }
    } 
	
	
	public T getGroup() {
        return group;
    }

    public GroupManager<?> getGroupManager() {
	    return groupManager;
    }
	
    /*
	public void setBounds2D(float x, float y, float w, float h) {
		bounds2d.x = x;
		bounds2d.y = y;
		bounds2d.width = w;
		bounds2d.height = h;		
		int bx = (int)x;
		int by = (int)y;
		int x1 = (int)(x+w);
		if(x1<x+w) x1++;
		int y1 = (int)(y+h);
		if(y1<y+h) y1++;
		int bw = x1-(int)x;
		int bh = y1-(int)y;
		setBounds(bx, by, bw, bh);
	}
*/
    public List<EditorEntry> getInfo() {
        if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    
		    xInfo = new EditorEntry("XPos_g", FLOAT, "X Coord.");
		    xInfo.setSpinnerValues(java.lang.Float.NaN, -999, 9999, .1f, false);
		    xInfo.requestNotifier();
		    result.add(xInfo);
		    
		    yInfo = new EditorEntry("YPos_g", FLOAT, "Y Coord.");
		    yInfo.setSpinnerValues(java.lang.Float.NaN, 0, 9999, .1f, false);
		    yInfo.requestNotifier();
		    result.add(yInfo);
	
		    radiusInfo = new EditorEntry("Radius", FLOAT, "Radius");
		    radiusInfo.setSpinnerValues(getDefaultRadius(), 0f, 999f, 1f, false);
	        radiusInfo.setVisibleWhenDisabled(false);
		    radiusInfo.setGlobal(true);
	        radiusInfo.requestNotifier();
		    result.add(radiusInfo);
		    
		    EditorEntry lwInfo = new EditorEntry("LineWidth", FLOAT, "Line Width");
		    lwInfo.setSpinnerValues(getDefaultLineWidth(), 0f, 99f, .2f, false);
		    lwInfo.setGlobal(true);
		    result.add(lwInfo);
		    
		    dashInfo = new EditorEntry("Dash", STRING, "Line Pattern");
		    dashInfo.setDefaultValue(DEFAULT_DASH);		    
		    dashInfo.requestNotifier();
		    dashInfo.setGlobal(true);
		    result.add(dashInfo);
		    
		    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
		    shadingInfo.setSpinnerValues(1-getDefaultShading(), 0f, 1f, .05f, false);
		    shadingInfo.setGlobal(true);
		    result.add(shadingInfo);
		    
		    result.trimToSize();
		    info = result;
        }
        return info;
	}

	public float getRadius() {
		return radius;
	}	
    
	public EditorEntry getXInfo() {
		if(xInfo==null) {
			getInfo();
		}
		return xInfo;
	}

    /**
     * For use with EditorPane
     */
	public float getXPos_g() {
	    //return (int)Math.round(bounds2d.x+bounds2d.width/2);
	    return bounds2d.x+bounds2d.width/2;
	}
    
    public EditorEntry getYInfo() {
		if(yInfo==null) {
			getInfo();
		}
		return yInfo;
	}
	
	/**
     * For use with EditorPane
     */
	public float getYPos_g() {
	    int h = getCanvas().getHeight();
	    //return h - (int)Math.round(bounds2d.y - bounds2d.height/2);
	    return h - bounds2d.y - bounds2d.height/2;
	}
		
	public boolean isInGroup() {
        return inGroup;
    }

    public void setBounds2D(float x, float y, float w, float h) {
	    float x0 = bounds2d.x;
	    float y0 = bounds2d.y;
	    super.setBounds2D(x, y, w, h);
		if(xInfo!=null && x0!=x) xInfo.notify(this);
		if(yInfo!=null && y0!=y) yInfo.notify(this);
	}
	
	public void setInGroup(boolean b) {
        inGroup = b;
    }

	public abstract void setRadius(float s);
	
	/**
     * For use with EditorPane
     */
	public void setXPos_g(float x) {
	    changePosition(x-bounds2d.width/2, bounds2d.y);
	}

	/**
     * For use with EditorPane
     */
	public void setYPos_g(float y) {
	    int h = getCanvas().getHeight();
	    changePosition(bounds2d.x, h - (y + bounds2d.height/2));
	}
	
}
