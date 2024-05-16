/*
 * Created on 11.11.2006
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.FLOAT;

import java.util.List;

import jPasi.codec.ParseException;
import jPasi.edit.EditorEntry;


/**
 * @author Jan Plate
 *
 */
public class Identity extends BinaryRelationship {

    private static final long serialVersionUID = -3520529984071453612L;

	public static final float DEFAULT_W0 = 7;
	public static final float DEFAULT_W1 = 7;
	public static final float DEFAULT_WC = 8;
     
    public static final float DEFAULT_GAP = 0;
    public static final float[] DEFAULT_DASH = null;

    protected EditorEntry dashInfo;
    
    public Identity() {}
    
	public Identity(ENode e0, ENode e1) {
		super(e0, e1);
	}	
	
	public boolean isSymmetric() {
	    return true;
	}
	
	public float getDefaultW0() {
		return DEFAULT_W0;
	}
	public float getDefaultW1() {
		return DEFAULT_W1;
	}
	public float getDefaultWC() {
		return DEFAULT_WC;
	}    

	public float getDefaultGap() {
        return DEFAULT_GAP;
    }

    public void setGap(float gap) {
        super.setGap(gap);
        this.gap0 = gap;
        this.gap1 = gap;
        shapeChanged(false);
    }

    public String getDash() {
       return getDashArrayString(stroke);
    }
   
    public void setDash(String s) {
       stroke = changeDashArray(stroke, s, DEFAULT_DASH);
       repaint();
       if(dashInfo!=null) dashInfo.getNotifier().run(this);
    }
    
    @Override
    public List<EditorEntry> computeArrowheadInfo() {
    	return null;
    }
    
    @Override
    public List<EditorEntry> computeLineInfo() {
	    List<EditorEntry> result = super.computeLineInfo();
	    
	    EditorEntry gapInfo = new EditorEntry("Gap", FLOAT, "Gap");
	    gapInfo.setSpinnerValues(DEFAULT_GAP, -99, 99, .5f, false);
	    gapInfo.setGlobal(true);
	    result.add(gapInfo);
	    
	    return result;
	}

	public void parse(String code, String info) throws ParseException {
	    super.parse(code, info);
	    gap0 = gap;
	}

}
