/*
 * Created on 11.11.2006
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.INTEGER;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.Line;
import jPasi.edit.EditorEntry;
import util.MathTools;

public class Order extends BinaryRelationship {
	
	private static final long serialVersionUID = -8369201233416668401L;

	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 8;
	public static final float DEFAULT_WC = 8;
     
	public final static double DEFAULT_HOOK_ANGLE = .39;
	public final static float DEFAULT_HOOK_LENGTH = 10f;
	
	protected double angle = DEFAULT_HOOK_ANGLE; // the angle of the 'harpoonhead's' hook
	protected float length = DEFAULT_HOOK_LENGTH; // the length of that hook
	protected Line2D.Float hook = new Line2D.Float(); // the end point of the hook, needed for getTexdrawCode
	
	public Order() {}
	
	public Order(ENode e0, ENode e1) {
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
        float epsilon = .001f;
	    Point2D.Float p1 = (Point2D.Float)getAdjustedLine().getP2();        
        Point2D p1p = MathTools.travel(new double[]{1}, length, 
        		adjustedLine, -epsilon, (int)(1/epsilon));
        double gamma = rigidPoint? psi1: MathTools.angle(p1, p1p);
        
        float bx = (float)(length*Math.cos(gamma+angle));
        float by = (float)(length*Math.sin(gamma+angle));
        hook.setLine(p1.x, p1.y, p1.x+bx, p1.y-by); 
        
        return new Shape[]{hook};
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
    		float lengtha = length*factor;
    		if(ok(lengtha)) {
    			length = lengtha;
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0 && 
    			TransformModifier.isFlippingArrowheads(modifier)) {
    		angle = -angle;
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
	    result.add(angleInfo);

	    EditorEntry lengthInfo = new EditorEntry("HookLength", FLOAT, "Hook Length");
	    lengthInfo.setSpinnerValues(DEFAULT_HOOK_LENGTH, -99, 99, 1f, false);
	    result.add(lengthInfo);
	    
	    for(EditorEntry ee: result) {
	    	ee.setGlobal(true);
	    }
	    return result;
	}
	
	public int parseArrowHead(Texdraw.StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(!(i<s.length && s[i].shape instanceof Line)) {
	        String s0 = "";
    	    if(i<s.length) {
    	        s0 = ", not "+s[i].shape.genericDescription();
    	    }
    	    throw new ParseException(TEX, "Line expected, not "+s0);
 	    }
 		int ch = canvas.getHeight();
 		Line l0 = (Line)s[i].shape.transform(ch);
 		float lw = s[i].stroke.lineWidth;
		altStroke = newStroke(altStroke, lw, s[i].stroke.pattern);
		
 		length = (float)l0.p1.distance(l0.p2);
	    Point2D.Float p2 = (Point2D.Float)adjustedLine.getP2();
        double a0 = MathTools.angle(p2, l0.p2);

        float epsilon = .0001f;
        Point2D p2p = MathTools.travel(new double[]{1}, rigidPoint? 10*epsilon: length, 
                	adjustedLine, -epsilon, (int)(1/epsilon));
        double gamma = MathTools.angle(p2, p2p==null? p2: p2p);

        double a1 = MathTools.d(gamma, a0);
        double a2 = MathTools.d(a0, gamma);
        angle = a1>a2? -a2: a1;
 		
	    return i+1;
	}

}
