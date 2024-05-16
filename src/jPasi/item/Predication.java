/*
 * Created on 16.12.2006
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.HINT;
import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.Codec1;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.StrokedShape;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class Predication extends BinaryRelationship {

    private static final long serialVersionUID = 6845438763323903115L;
    
    // for the left-hand point line:
	public static final float DEFAULT_W0 = 5;
	public static final float DEFAULT_W1 = 15;
	public static final float DEFAULT_WC = 10;
     
    public static final float DEFAULT_X = 9;
    public static final float DEFAULT_Y = 0;
	public static final float DEFAULT_X0 = 7;
    public static final float DEFAULT_Y0 = 4;
    public static final float DEFAULT_X1 = 7;
    public static final float DEFAULT_Y1 = 4;
	public static final boolean DEFAULT_FILLED = false;	

	protected float x0 = DEFAULT_X0;
	protected float y0 = DEFAULT_Y0;
	protected float x1 = DEFAULT_X1;
	protected float y1 = DEFAULT_Y1;
	protected float x = DEFAULT_X;
	protected float y = DEFAULT_Y;
	protected CubicCurve2D.Float pointLine = new CubicCurve2D.Float();

	public Predication() {}
	
    public Predication(ENode e0, ENode e1) {
        super(e0, e1);
        w0 = 5;
        w1 = 15;
        wc = 10;
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
        return gap+getLineWidth()/2;
    }
    
    public float computeGap() {
        return gap1-getLineWidth()/2;
    }
    
    public Shape[] computeArrowhead() {
       pointLine = getCubicPointLine(x0, -y0, x1, -y1, x, -y, false);
       return new Shape[]{pointLine};
    }

    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float x0a = x0*factor;
    		float y0a = y0*factor;
    		float x1a = x1*factor;
    		float y1a = y1*factor;
    		float xa = x*factor;
    		float ya = y*factor;
    		if(ok(x0a) && ok(y0a) && ok(x1a) && ok(y1a) && ok(xa) && ok(ya)) {
    			x0 = x0a;
    			y0 = y0a;
    			x1 = x1a;
    			y1 = y1a;
    			x = xa;
    			y = ya;
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0 && 
    			TransformModifier.isFlippingArrowheads(modifier)) {
    		y0 = -y0;
    		y1 = -y1;
    		y = -y;
    	}
    }
	
 	public float getX_() {
        return x;
    }
    public void setX_(float x) {
        this.x = x;
	    shapeChanged(false);
    }
    public float getX0() {
        return x0;
    }
    public void setX0(float x0) {
        this.x0 = x0;
	    shapeChanged(false);
    }
    public float getX1() {
        return x1;
    }
    public void setX1(float x1) {
        this.x1 = x1;
	    shapeChanged(false);
    }
    public float getY_() {
        return y;
    }
    public void setY_(float y) {
        this.y = y;
	    shapeChanged(false);
    }
    public float getY0() {
        return y0;
    }
    public void setY0(float y0) {
        this.y0 = y0;
	    shapeChanged(false);
    }
    public float getY1() {
        return y1;
    }
    public void setY1(float y1) {
        this.y1 = y1;
	    shapeChanged(false);
    }
    
	public List<EditorEntry> computeArrowheadInfo() {
	    List<EditorEntry> result = super.computeArrowheadInfo();
	    
	    EditorEntry x0Info = new EditorEntry("X0", FLOAT, "X 1");
	    x0Info.setSpinnerValues(DEFAULT_X0, -99, 99, 1f, false);
	    result.add(x0Info);
	    EditorEntry y0Info = new EditorEntry("Y0", FLOAT, "Y 1");
	    y0Info.setSpinnerValues(DEFAULT_Y0, -99, 99, 1f, false);
	    result.add(y0Info);
	    
	    EditorEntry x1Info = new EditorEntry("X1", FLOAT, "X 2");
	    x1Info.setSpinnerValues(DEFAULT_X1, -99, 99, 1f, false);
	    result.add(x1Info);
	    EditorEntry y1Info = new EditorEntry("Y1", FLOAT, "Y 2");
	    y1Info.setSpinnerValues(DEFAULT_Y1, -99, 99, 1f, false);
	    result.add(y1Info);

	    EditorEntry xInfo = new EditorEntry("X_", FLOAT, "X 3");
	    xInfo.setSpinnerValues(DEFAULT_X, -99, 99, 1f, false);
	    result.add(xInfo);
	    EditorEntry yInfo = new EditorEntry("Y_", FLOAT, "Y 3");
	    yInfo.setSpinnerValues(DEFAULT_Y, -99, 99, 1f, false);
	    result.add(yInfo);

	    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
	    shadingInfo.setSpinnerValues(1-getDefaultShading(), 0f, 1f, .05f, false);
	    result.add(shadingInfo);
	    		    
	    for(EditorEntry ee: result) {
	    	ee.setGlobal(true);
	    }
	    return result;
	}
	
	public String getInfoString() {
	    if(rigidPoint) {
		    float r = involutes[1].getRadius();
		    CubicCurve2D.Float line0 = (CubicCurve2D.Float)getAdjustedLine(0, r+gap);
		    float epsilon = 0.0001f;
		    Point2D.Float 
		  		p = (Float)line0.getP2(),
		  		q = (Float)adjustedLine.getP2(),
		        q_ = (Float)MathTools.travel(new double[]{1}, epsilon, line0, -epsilon, (int)(1/epsilon)); // ersatz q
		    double gamma = MathTools.angle(q==null? q_: q, p);
		    return Codec1.encodeFloat((float)gamma);
	    }
	    else {
		    return Codec1.encodeFloat(x0)+" "+Codec1.encodeFloat(x1)+" "+Codec1.encodeFloat(x);
	    }
	}
	
	public int parseArrowHead(StrokedShape[] s, String info, int i) throws ParseException {	    
    	if(!(i<s.length && s[i].shape instanceof CubicCurve)) {
    	    String s0 = "";
    	    if(i<s.length) {
    	        s0 = ", not "+s[i].shape.genericDescription();
    	    }
 	        throw new ParseException(TEX, "Curve expected"+s0);
 	    }
    	int ch = canvas.getHeight();
		altStroke = newStroke(altStroke, s[i].stroke.lineWidth, s[i].stroke.pattern);
 		CubicCurve c = (CubicCurve)s[i].shape.transform(ch);
 		setShading(c.filled? c.fillLevel: 1);
	    
		if(!rigidPoint) {
	        if(info==null) {
	            throw new ParseException(HINT, "Info string required");
	        }
			String[] sp = info.split("\\s+");
			if(sp.length!=3) {
			    throw new ParseException(HINT, "Invalid info string: "+info);
			}
			x0 = Codec1.decodeFloat(sp[0]);
    	    y0 = getYCoordinate(c.p1a, x0);
			x1 = Codec1.decodeFloat(sp[1]);
    	    y1 = getYCoordinate(c.p2a, x1);
			x = Codec1.decodeFloat(sp[2]);
    	    y = getYCoordinate(c.p2, x);
		} 
		else { 
			double gamma = Codec1.decodeFloat(info);
			
	        Point2D.Float p0 = rotateBack(c.p1a, c.p1, gamma);
			x0 = p0.x;
			y0 = p0.y==0? 0: -p0.y;
			Point2D.Float p1 = rotateBack(c.p2a, c.p1, gamma);
			x1 = p1.x;
			y1 = p1.y==0? 0: -p1.y;
			Point2D.Float p_ = rotateBack(c.p2, c.p1, gamma);
			x = p_.x;
			y = p_.y==0? 0: -p_.y;		
		}

		return i+1;
	}
	
}
