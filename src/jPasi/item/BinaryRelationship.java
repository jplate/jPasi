/*
 * Created on 14.12.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.*;

import java.util.ArrayList;
import java.util.List;

import jPasi.edit.Editable;
import jPasi.edit.EditorEntry;

/**
 * @author Jan Plate
 * 
 * Note: This class used to be called 'TernaryRelationship' because the connector-node was treated as a representation of a
 * third relatum (and in some cases, this would indeed be appropriate).

 */
public abstract class BinaryRelationship extends Connector {

    public static final int ITEM_SIZE = 10;

    protected EditorEntry itemDashInfo;

    protected BinaryRelationship() {}
    
    public BinaryRelationship(ENode e0, ENode e1) {
		super();
        
		involutes = new ENode[3]; 
		involutes[0] = e0;
		involutes[1] = e1;
		involutes[0].addDependentItem(this);
		if(involutes[0]!=involutes[1]) {
			involutes[1].addDependentItem(this);
		}		
	
		installItem();		
	}	
   
	public void installItem() {
	    installItem(new ENode(null, this, true));
	    setItemLineWidth(getDefaultItemLineWidth());
	}
	
    public float getDefaultItemLineWidth() {
        return Item.DEFAULT_LINEWIDTH;
    }

    public void installItem(ENode e) {
		item = e;
		e.setSizeWhenNotHidden(ITEM_SIZE);
		e.setDelegate(true);	
		init();
	}

	public float getItemLineWidth() {
	    return ((ENode)item).getLineWidth();
	}
	
    public void setItemLineWidth(float lw) {
	    ((ENode)item).setLineWidth(lw);
	}

    /**
	 * Invoked by the EditorPane.
	 */
    public String getLabelInfo() {
        return "";
    }

    /**
	 * Invoked by the EditorPane.
	 */
    public boolean canChangeXPos_g() {
    	return false;
    }
    
    /**
	 * Invoked by the EditorPane.
	 */
    public boolean canChangeYPos_g() {
    	return false;
    }
    
	/**
     * For use with EditorPane
     */
	public float getXPos_g() {
	    return ((ENode)item).getXPos_g();
	}

	/**
     * For use with EditorPane
     */
	public float getYPos_g() {	    
	    return ((ENode)item).getYPos_g();
	}

	/**
     * For use with EditorPane
     */
	public void setXPos_g(float x) {
		((ENode)item).setXPos_g(x);
	}

	/**
     * For use with EditorPane
     */
	public void setYPos_g(float y) {
		((ENode)item).setYPos_g(y);
	}
	
    public void setItemDash(String s) {
        item.stroke = changeDashArray(item.stroke, s, ENode.DEFAULT_DASH);
        repaint();
        if(itemDashInfo!=null) itemDashInfo.notify(this);
    }
    
    public int getNodeRadius() {
        return (int)((ENode)item).getRadius();
    }

    public void setNodeRadius(int r) {
        ((ENode)item).setSizeWhenNotHidden(2*r);
    }
    
    public float getNodeShading_EP() {
        return ((ENode)item).getShading_EP();
    }

    public void setNodeShading_EP(float s) {
        ((ENode)item).setShading_EP(s);
    }

    public List<EditorEntry> computeNodeInfo() {
        ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
	    /*
	    EditorEntry negationInfo = new EditorEntry("Negated", TOGGLE, "Negated");
	    negationInfo.setGlobal(true);
	    result.add(negationInfo);
	    */
	    EditorEntry nxInfo = ((ENode)item).getXInfo(); 
	    nxInfo.requestNotifier();
	    result.add(nxInfo);

	    EditorEntry nyInfo = ((ENode)item).getYInfo(); 
	    nyInfo.requestNotifier();
	    result.add(nyInfo);
	    
	    EditorEntry radiusInfo = new EditorEntry("NodeRadius", INTEGER, "Radius");
	    radiusInfo.setSpinnerValues(ITEM_SIZE/2, 0, 999, 2, false);
	    radiusInfo.setGlobal(true);
	    result.add(radiusInfo);
	    
	    EditorEntry lwInfo = new EditorEntry("ItemLineWidth", FLOAT, "Line Width");
	    lwInfo.setSpinnerValues(getDefaultAltLineWidth(), 0, 99, .2f, false);
	    lwInfo.setGlobal(true);
	    result.add(lwInfo);
	    
	    itemDashInfo = new EditorEntry("ItemDash", STRING, "Line Pattern");
	    itemDashInfo.setDefaultValue(ENode.DEFAULT_DASH);		    
	    itemDashInfo.requestNotifier();
	    itemDashInfo.setGlobal(true);
	    result.add(itemDashInfo);

	    EditorEntry shadingInfo = new EditorEntry("NodeShading_EP", FLOAT, "Shading");
	    shadingInfo.setSpinnerValues(1-ENode.DEFAULT_SHADING, 0f, 1f, .05f, false);
	    result.add(shadingInfo);
	    
	    return result;
	}
	
	public boolean isEditorDelegateOf(Editable e) {
		return e==item;
	}
    
}
