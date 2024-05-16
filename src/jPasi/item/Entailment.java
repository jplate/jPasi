/*
 * Created on 19.12.2006
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.HINT;
import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.Codec1;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.Path;
import jPasi.codec.Texdraw.StrokedShape;
import jPasi.edit.EditorEntry;

/**
 * @author Jan Plate
 *
 */

public class Entailment extends BinaryRelationship {

	private static final long serialVersionUID = -6208376608500496716L;
    
	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 8;
	public static final float DEFAULT_WC = 8;
     
    // for the left-hand point line:
    public static final float DEFAULT_X0 = 9;
    public static final float DEFAULT_Y0 = 3.5f;
	public static final float DEFAULT_X00 = 4;
    public static final float DEFAULT_Y00 = 3;
    public static final float DEFAULT_X01 = 8;
    public static final float DEFAULT_Y01 = 5;
    public static final boolean DEFAULT_FILLED = false;
	
	protected float x00 = DEFAULT_X00;
	protected float y00 = DEFAULT_Y00;
	protected float x01 = DEFAULT_X01;
	protected float y01 = DEFAULT_Y01;
	protected float x0 = DEFAULT_X0;
	protected float y0 = DEFAULT_Y0;
	protected CubicCurve2D pointLine0 = new CubicCurve2D.Float();
    protected CubicCurve2D pointLine1 = new CubicCurve2D.Float();

	public Entailment() {}

	public Entailment(ENode e0, ENode e1) {
        super(e0, e1);
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

    public float computeGap1() {
        return gap+getLineWidth();
    }
    
    public float computeGap() {
        return gap1-getLineWidth();
    }

    public Shape[] computeArrowhead() {
        pointLine0 = getCubicPointLine(x00, y00, x01, y01, x0, y0, false);
        pointLine1 = getCubicPointLine(x00, -y00, x01, -y01, x0, -y0, false);        
        return new Shape[]{pointLine0, pointLine1};
	}

	public Shape[] computeFilledArrowhead() {
        pointLine0 = getCubicPointLine(x00, y00, x01, y01, x0, y0, true);
        pointLine1 = getCubicPointLine(x00, -y00, x01, -y01, x0, -y0, false);        
        return new Shape[]{pointLine0, pointLine1};
	}

    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float x00a = x00*factor;
    		float y00a = y00*factor;
    		float x01a = x01*factor;
    		float y01a = y01*factor;
    		float x0a = x0*factor;
    		float y0a = y0*factor;
    		if(ok(x00a) && ok(y00a) && ok(x01a) && ok(y01a) && ok(x0a) && ok(y0a)) {
    			x00 = x00a;
    			y00 = y00a;
    			x01 = x01a;
    			y01 = y01a;
    			x0 = x0a;
    			y0 = y0a;
    		}
    	}
    }
    
    protected boolean coalesceDrawnShapes() {
    	return false;
    }

	// The following methods are intended for use with EditPanel:
    public float getGap() {
        return gap;
    }
    public void setGap(float gap) {
        this.gap = gap;
	    shapeChanged(true);
    }
    public float getX0() {
        return x0;
    }
    public void setX0(float x0) {
        this.x0 = x0;
	    shapeChanged(false);
    }
    public float getX00() {
        return x00;
    }
    public void setX00(float x00) {
        this.x00 = x00;
	    shapeChanged(false);
    }
    public float getX01() {
        return x01;
    }
    public void setX01(float x01) {
        this.x01 = x01;
	    shapeChanged(false);
    }
    public float getY0() {
        return y0;
    }
    public void setY0(float y0) {
        this.y0 = y0;
	    shapeChanged(false);
    }
    public float getY00() {
        return y00;
    }
    public void setY00(float y00) {
        this.y00 = y00;
	    shapeChanged(false);
    }
    public float getY01() {
        return y01;
    }
    public void setY01(float y01) {
        this.y01 = y01;
	    shapeChanged(false);
    }
    
	public List<EditorEntry> computeArrowheadInfo() {
	    List<EditorEntry> result = super.computeArrowheadInfo();
	    
	    EditorEntry x00Info = new EditorEntry("X00", FLOAT, "X 1");
	    x00Info.setSpinnerValues(DEFAULT_X00, -99, 99, 1f, false);
	    result.add(x00Info);
	    EditorEntry y00Info = new EditorEntry("Y00", FLOAT, "Y 1");
	    y00Info.setSpinnerValues(DEFAULT_Y00, -99, 99, 1f, false);
	    result.add(y00Info);

	    EditorEntry x01Info = new EditorEntry("X01", FLOAT, "X 2");
	    x01Info.setSpinnerValues(DEFAULT_X01, -99, 99, 1f, false);
	    result.add(x01Info);
	    EditorEntry y01Info = new EditorEntry("Y01", FLOAT, "Y 2");
	    y01Info.setSpinnerValues(DEFAULT_Y01, -99, 99, 1f, false);
	    result.add(y01Info);

	    EditorEntry x0Info = new EditorEntry("X0", FLOAT, "X 3");
	    x0Info.setSpinnerValues(DEFAULT_X0, -99, 99, 1f, false);
	    result.add(x0Info);
	    EditorEntry y0Info = new EditorEntry("Y0", FLOAT, "Y 3");
	    y0Info.setSpinnerValues(DEFAULT_Y0, -99, 99, 1f, false);
	    result.add(y0Info);

	    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
	    shadingInfo.setSpinnerValues(1-getDefaultShading(), 0f, 1f, .05f, false);
	    result.add(shadingInfo);
	    
	    for(EditorEntry ee: result) {
	    	ee.setGlobal(true);
	    }
	    return result;
	}	

	public String getInfoString() {
	    return Codec1.encodeFloat(x00)+" "+Codec1.encodeFloat(x01)+" "+Codec1.encodeFloat(x0);
	}
	
	public int parseArrowHead(StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(!(i<s.length && s[i].shape instanceof Path) &&
	       !(i+1<s.length && s[i].shape instanceof CubicCurve &&
	               s[i+1].shape instanceof CubicCurve)) {
	        StringBuilder ms = new StringBuilder();
	        if(i<s.length) {
	            String s0 = s[i].shape.genericDescription();
	            if(i+1<s.length) {
		            ms.append(s0+" and "+s[i+1].shape.genericDescription());	                
	            } else {
	                ms.append("just "+s0);
	            }
	        }
	 	    throw new ParseException(TEX, 
	 	            "Curve sequence or two separate curves expected, not "+ms.toString()+".");
	    }
		int ch = canvas.getHeight();
		int k;
		CubicCurve c0, c1;
		boolean filled = false;
		if(s[i].shape instanceof Path) {
		    Path p = (Path)s[i].shape;
			if(!(p.shapes.length==2 && 
			        p.shapes[0] instanceof CubicCurve && 
			        p.shapes[1] instanceof CubicCurve)) {
			    throw new ParseException(TEX, "Sequence of two curves expected.");
			}
			c0 = (CubicCurve)p.shapes[0].transform(ch);
			c1 = (CubicCurve)p.shapes[1].transform(ch);
			filled = true;
	        setShading(p.fillLevel);
			k = 1;
		} 
		else {
			c0 = (CubicCurve)s[i].shape.transform(ch);
			c1 = (CubicCurve)s[i+1].shape.transform(ch);
			k = 2;
		}
		float lw = s[i].stroke.lineWidth;
		if(s[i].stroke.pattern!=null && filled) { // special case: Path + 4 Shapes
		    if(!(i+2<s.length && s[i+1].shape instanceof CubicCurve &&
		            s[i+2].shape instanceof CubicCurve)) {
		        throw new ParseException(TEX, "Further curves expected");
		    }
	        lw = s[i+1].stroke.lineWidth;
		    k += 2;
		}
		altStroke = newStroke(altStroke, lw, s[i].stroke.pattern);
		
        if(info==null) {
            throw new ParseException(HINT, "Info string required");
        }
		String[] sp = info.split("\\s+");
		if(sp.length!=3) {
		    throw new ParseException(HINT, "Invalid info string: "+info);
		}
		x00 = Codec1.decodeFloat(sp[0]);
		x01 = Codec1.decodeFloat(sp[1]);
		x0 = Codec1.decodeFloat(sp[2]);
	
		y00 = (float)c1.p1a.distance(c0.p1a)/2;
		y01 = (float)c1.p2a.distance(c0.p2a)/2;
		y0 = (float)c1.p2.distance(c0.p2)/2;

	    return i+k;
	}
		
}
