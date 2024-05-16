/*
 * Created on 15.01.2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.item;

import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.INTEGER;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.Line;
import jPasi.codec.Texdraw.Path;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Adjunction extends BinaryRelationship {

    private static final long serialVersionUID = -6932509612121814328L;

	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 8;
	public static final float DEFAULT_WC = 8;
     
    public final static double DEFAULT_HOOK_ANGLE = .39;
	public final static float DEFAULT_HOOK_LENGTH = 10f;
	
	protected double angle = DEFAULT_HOOK_ANGLE; // the angle of the 'harpoonhead's' hook
	protected float length = DEFAULT_HOOK_LENGTH; // the length of that hook
	
	protected Point2D.Float point = new Point2D.Float();
	protected Point2D.Float hookPoint0 = new Point2D.Float(); // the end point of the hook, needed for getTexdrawCode
	protected Point2D.Float hookPoint1 = new Point2D.Float(); // the end point of the hook, needed for getTexdrawCode

	public Adjunction() {}

	public Adjunction(ENode e0, ENode e1) {
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

	public Shape[] computeArrowhead() {
	    float r1 = involutes[1].getRadius();
        
        float epsilon = .0001f;
        CubicCurve2D.Float adjustedLine = (CubicCurve2D.Float)getAdjustedLine();
	    Point2D.Float p1 = (Point2D.Float)adjustedLine.getP2();        
        Point2D p1p = MathTools.travel(new double[]{1}, length, adjustedLine, -epsilon, (int)(1/epsilon));
        
        double gamma = rigidPoint? psi1: MathTools.angle(p1, p1p);
        float bx0 = (float)(length*Math.cos(gamma-angle));
        float by0 = (float)(length*Math.sin(gamma-angle));
        float bx1 = (float)(length*Math.cos(gamma+angle));
        float by1 = (float)(length*Math.sin(gamma+angle));
        point.setLocation(p1);
        hookPoint0.setLocation(p1.x+bx0, p1.y-by0); 
        hookPoint1.setLocation(p1.x+bx1, p1.y-by1); 
        
        GeneralPath path = new GeneralPath();
        
        return new Shape[]{new Line2D.Float(p1.x, p1.y, p1.x+bx0, p1.y-by0), 
                		   new Line2D.Float(p1.x, p1.y, p1.x+bx1, p1.y-by1)};
	}	

    public float getDefaultAltLineWidth() {
        return DEFAULT_LINEWIDTH;
    }	

    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float la = length*factor;
    		if(ok(la)) {
    			length = la;
    		}
    	}
    }
    
	// The following methods are intended for use with EditPanel:
	public int getHookAngle() {
	    return (int)Math.round(Math.toDegrees(angle));
	}	
	public void setHookAngle(int degrees) {
	    angle = Math.toRadians(degrees);
	    shapeChanged(false);
	}
	public float getHookLength() {
	    return length;
	}
	public void setHookLength(float l) {
	    length = l;
	    shapeChanged(false);
	}
	
	public List<EditorEntry> computeArrowheadInfo() {
	    List<EditorEntry> result = super.computeArrowheadInfo();
	    
	    EditorEntry angleInfo = new EditorEntry("HookAngle", INTEGER, "Hook Angle");
	    angleInfo.setSpinnerValues((int)Math.toDegrees(DEFAULT_HOOK_ANGLE), -180, 180, 5, true);
	    angleInfo.setGlobal(true);
	    result.add(angleInfo);

	    EditorEntry lengthInfo = new EditorEntry("HookLength", FLOAT, "Hook Length");
	    lengthInfo.setSpinnerValues(DEFAULT_HOOK_LENGTH, -99, 99, 1f, false);
	    lengthInfo.setGlobal(true);
	    result.add(lengthInfo);
	    
	    return result;
	}

	public int parseArrowHead(Texdraw.StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(!(i<s.length && s[i].shape instanceof Path) &&
	 	       !(i+1<s.length && s[i].shape instanceof Line &&
	 	               s[i+1].shape instanceof Line)) {
	        StringBuilder ms = new StringBuilder();
	        if(i<s.length) {
	            String s0 = s[i].shape.genericDescription();
	            if(i+1<s.length) {
		            ms.append(s0+" and "+s[i+1].shape.genericDescription());	                
	            } else {
	                ms.append("just "+s0);
	            }	            
	        }
	 	    throw new ParseException(TEX, "Line sequence or two separate lines expected, not "+ms.toString());
 	    }
 		int ch = canvas.getHeight();
 		int k;
 		Line l0, l1;
 		if(s[i].shape instanceof Path) { // may happen if hook length == 0
 		    Path p = (Path)s[i].shape;
 			if(!(p.shapes.length==2 && 
 			        p.shapes[0] instanceof Line && 
 			        p.shapes[1] instanceof Line)) {
 			    throw new ParseException(TEX, "Wrong sequence of shapes");
 			}
 			l0 = (Line)p.shapes[0].transform(ch);
 			l1 = (Line)p.shapes[1].transform(ch);
 			k = 1;
 		} 
 		else {
 			l0 = (Line)s[i].shape.transform(ch);
 			l1 = (Line)s[i+1].shape.transform(ch);
 			k = 2;
 		}
 		float lw = s[i].stroke.lineWidth;
		altStroke = newStroke(altStroke, lw, s[i].stroke.pattern);
		
 		length = (float)l0.p1.distance(l0.p2);
 		double a0 = MathTools.angle(l0.p1, l0.p2);
 		double a1 = MathTools.angle(l1.p1, l1.p2);
 		angle = Math.min(MathTools.d(a0, a1), MathTools.d(a1, a0))/2;
	 		
	    return i+k;
	}
	
}
