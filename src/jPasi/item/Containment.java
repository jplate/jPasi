/*
 * Created on 14.12.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.item;

import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.StrokedShape;
import jPasi.edit.EditorEntry;

/**
 * @author Jan Plate
 *
 */
public class Containment extends BinaryRelationship {

    private static final long serialVersionUID = -5075403873565057445L;

	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 8;
	public static final float DEFAULT_WC = 8;
     
   public static final float DEFAULT_GAP = 2;
	public static final float DEFAULT_BASE = 3;
	public static final float DEFAULT_LENGTH = 6;
    public static final float DEFAULT_SHADING = 0; // 1 = white, 0 = black
	public static final float DEFAULT_LINEWIDTH = .6f;
	
	public static final float firstInvoluteSizeFactor = .8f; 
	
	protected float base = DEFAULT_BASE;
	protected float length = DEFAULT_LENGTH;
	
	public Containment() {}
	
	public Containment(ENode e0, ENode e1) {
		super(e0, e1);
		//if(e0!=e1) e0.setSize(2*(int)(ENode.DEFAULT_RADIUS*firstInvoluteSizeFactor));
		//e0.setResizeable(false);
		//e0.setNegatable(true);
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
	protected boolean hasByDefaultRigidPoint() {
	    return true;
    }
	
	public float computeGap1() {
	    return gap+length-getAltLineWidth();
	}

    protected float computeGap() {
        return gap1-length+getAltLineWidth();
    }

    /* 
	 * @see pasi.item.State#getShape()
	 * Creates a curved arrow with a cubic curve as a point.
	 */
	public Shape[] computeArrowhead() {
		Point2D.Float 
			c = (Point2D.Float)involutes[1].getCenter(),
			nearPoint = new Point2D.Float(),
			leftPoint = new Point2D.Float(),
			rightPoint = new Point2D.Float();
		
		float r = involutes[1].getRadius() + gap;
		float r1 = r + length;
		float r2 = (float)Math.hypot(r1, base/2);
		double beta = Math.acos(r1/r2);
		nearPoint.setLocation(c.x + r*(float)Math.cos(psi1), c.y - r*(float)Math.sin(psi1));
		leftPoint.setLocation(c.x + r2*(float)Math.cos(psi1-beta), c.y - r2*(float)Math.sin(psi1-beta));
		rightPoint.setLocation(c.x + r2*(float)Math.cos(psi1+beta), c.y - r2*(float)Math.sin(psi1+beta));
		
        return new Shape[]{new CubicCurve2D.Float(
        		Math.round(leftPoint.x), Math.round(leftPoint.y), nearPoint.x, nearPoint.y, 
                nearPoint.x, nearPoint.y, Math.round(rightPoint.x), Math.round(rightPoint.y))};
	}

    public float getDefaultLineWidth() {
        return DEFAULT_LINEWIDTH;
    }	

    public float getDefaultAltLineWidth() {
        return DEFAULT_LINEWIDTH;
    }	

    public float getDefaultShading() {
    	return DEFAULT_SHADING;
    }

    public float getDefaultGap() {
        return DEFAULT_GAP;
    }
	
    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float ba = base*factor;
    		float la = length*factor;
    		if(ok(ba) && ok(la)) {
    			base = ba;
    			length = la;
    		}
    	}
    }
    
	// The following methods are intended for use with EditPanel:
    public float getLength() {
        return length;
    }
    public void setLength(float length) {
        this.length = length;
	    shapeChanged(false);
    }
    public float getBase() {
        return base;
    }
    public void setBase(float base) {
        this.base = base;
	    shapeChanged(false);
    }
    
    public List<EditorEntry> computeArrowheadInfo() {
	    List<EditorEntry> result = super.computeArrowheadInfo();
	    		    
	    EditorEntry lengthInfo = new EditorEntry("Length", FLOAT, "Blade Length");
	    lengthInfo.setSpinnerValues(DEFAULT_LENGTH, -99f, 99f, 1f, false);
	    result.add(lengthInfo);
	    
	    EditorEntry baseInfo = new EditorEntry("Base", FLOAT, "Blade Width");
	    baseInfo.setSpinnerValues(DEFAULT_BASE, -99f, 99f, 1f, false);
	    result.add(baseInfo);
	    
	    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
	    shadingInfo.setSpinnerValues(1-getDefaultShading(), 0f, 1f, .05f, false);
	    result.add(shadingInfo);
	    
	    for(EditorEntry ee: result) {
	    	ee.setGlobal(true);
	    }
	    return result;
	}
	
	public int parseArrowHead(StrokedShape[] s, String info, int i) throws ParseException {	    
    	if(!(i<s.length && s[i].shape instanceof CubicCurve)) {
	        StringBuilder ms = new StringBuilder();
	        if(i<s.length) {
	            ms.append(s[i].shape.genericDescription());	            
	        }
	 	    throw new ParseException(TEX, "Curve expected, not "+ms.toString());
 	    }
		altStroke = newStroke(altStroke, s[i].stroke.lineWidth, s[i].stroke.pattern);
 		CubicCurve c = (CubicCurve)s[i].shape;
        setShading(c.filled? c.fillLevel: 1);
		base = (float)c.p1.distance(c.p2);
		double d0 = c.p1.distance(c.p1a);
		length = (float)Math.sqrt(d0*d0-(base*base)/4);

		return i+1;
	}
	
}
