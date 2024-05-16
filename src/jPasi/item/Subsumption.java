/*
 * Created on 12.12.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.item;

import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.Codec1;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class Subsumption extends BinaryRelationship {

    private static final long serialVersionUID = 6963742641901163434L;

	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 48;
	public static final float DEFAULT_WC = 88;
     
    public static final float DEFAULT_GAP = 4;
	public static final float DEFAULT_DEPTH = 0;
	public static final float DEFAULT_HOOK_RADIUS = 2.5f;
	public static final int DEFAULT_HOOK_EXTENT = 180;
	
	protected float depth = DEFAULT_DEPTH;
	protected float r = DEFAULT_HOOK_RADIUS;
	protected float extent = DEFAULT_HOOK_EXTENT;
	
	protected Arc2D.Float leftArc = new Arc2D.Float();
	protected Arc2D.Float rightArc = new Arc2D.Float();
	
	public Subsumption() {}
	
	public Subsumption(ENode e0, ENode e1) {
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
        float epsilon = .0001f;
        double[] t = new double[]{1};
        CubicCurve2D.Float adjustedLine = (CubicCurve2D.Float)getAdjustedLine();
		
        Point2D.Float p1 = (Point2D.Float)MathTools.travel(t, depth, adjustedLine, -epsilon, (int)(1/epsilon)),
					  p2 = (Point2D.Float)MathTools.getPoint(adjustedLine, t[0]-10*epsilon);
        
        double gamma = MathTools.angle(p2, p1);
        
		float r1 = r + (getLineWidth() - getAltLineWidth())/2;
		float arc1X = (float)(p1.x - r1*Math.sin(gamma));
		float arc1Y = (float)(p1.y - r1*Math.cos(gamma));
		float arc2X = (float)(p1.x + r1*Math.sin(gamma));
		float arc2Y = (float)(p1.y + r1*Math.cos(gamma));
		
        leftArc.setArcByCenter(arc1X, arc1Y, r, Math.toDegrees(gamma)-90, extent, Arc2D.OPEN);
        rightArc.setArcByCenter(arc2X, arc2Y, r, Math.toDegrees(gamma)+90, -extent, Arc2D.OPEN);

        return new Shape[]{leftArc, rightArc};
	}
	
	public Shape[] computeDisplayArrowhead() {
        float epsilon = .0001f;
        double[] tD = new double[]{1};
        CubicCurve2D.Float adjustedLineD = (CubicCurve2D.Float)MathTools.round(getAdjustedLine());

        Point2D.Float p1D = (Point2D.Float)MathTools.travel(tD, depth, adjustedLineD, -epsilon, (int)(1/epsilon)),
        			  p2D = (Point2D.Float)MathTools.getPoint(adjustedLineD, tD[0]-10*epsilon);
        
        double gamma = MathTools.angle(p2D, p1D);
        
		float r1 = r + (getLineWidth() - getAltLineWidth())/2;
		float arc1XD = Math.round(p1D.x - r1*Math.sin(gamma));
		float arc1YD = Math.round(p1D.y - r1*Math.cos(gamma));
		float arc2XD = Math.round(p1D.x + r1*Math.sin(gamma));
		float arc2YD = Math.round(p1D.y + r1*Math.cos(gamma));
		
        Arc2D.Float leftArcD = new Arc2D.Float();
        Arc2D.Float rightArcD = new Arc2D.Float();
        leftArcD.setArcByCenter(arc1XD, arc1YD,	r, (float)Math.toDegrees(gamma)-90, extent, Arc2D.OPEN);
        rightArcD.setArcByCenter(arc2XD, arc2YD, r, (float)Math.toDegrees(gamma)+90, -extent, Arc2D.OPEN);

        return new Shape[]{leftArcD, rightArcD};
	}	

    public float getDefaultGap() {
        return DEFAULT_GAP;
    }
    
    public Shape[] computeFilledArrowhead() {
        return null; // the Arcs cannot be filled.
    }
  
    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float ra = r*factor;
    		float deptha = depth*factor;
    		if(ok(ra) && ok(deptha)) {
    			r = ra;
    			depth = deptha;
    		}
    	}
    }
    
	// The following methods are intended for use with EditPanel:
    public float getDepth() {
        return depth;
    }
    public void setDepth(float d) {
        this.depth = d;
        shapeChanged(false);
    }
    public float getExtent() {
        return extent;
    }
    public void setExtent(float extent) {
        this.extent = extent;
        shapeChanged(false);
    }
    public float getR() {
        return r;
    }
    public void setR(float r) {
        this.r = r;
        shapeChanged(false);
    }

	public List<EditorEntry> computeArrowheadInfo() {
	    List<EditorEntry> result = super.computeArrowheadInfo();
	    
	    EditorEntry depthInfo = new EditorEntry("Depth", FLOAT, "Depth");
	    depthInfo.setSpinnerValues(DEFAULT_DEPTH, 0, 99, .5f, false);
	    result.add(depthInfo);
	    
	    EditorEntry radiusInfo = new EditorEntry("R", FLOAT, "Radius");
	    radiusInfo.setSpinnerValues(DEFAULT_HOOK_RADIUS, 0, 99, .5f, false);
	    result.add(radiusInfo);
	    
	    EditorEntry extentInfo = new EditorEntry("Extent", FLOAT, "Extent");
	    extentInfo.setSpinnerValues(DEFAULT_HOOK_EXTENT, -360, 360, 10f, true);
	    result.add(extentInfo);
	    
	    for(EditorEntry ee: result) {
	    	ee.setGlobal(true);
	    }
	    return result;
	}
	
	public String getInfoString() {
	    return depth==0 && extent>=0? null: Codec1.encodeFloat(depth)+(extent<0? " n": "");
	}
	
	public int parseArrowHead(Texdraw.StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(i+1>=s.length || 
	            !(s[i].shape instanceof Texdraw.Arc) ||
	            !(s[i+1].shape instanceof Texdraw.Arc)) {
	        throw new ParseException(TEX, "Two or three arcs expected");
	    }
		boolean negativeExtent = false;
	    if(info==null || info.length()==0) {
	        depth = 0;
	    }
	    else {
	        String[] sp = info.split("\\s+");
		    depth = Codec1.decodeFloat(sp[0]);
		    negativeExtent = sp.length>1;
	    }
		int ch = canvas.getHeight();
	    Texdraw.Arc a0 = (Texdraw.Arc)s[i].shape.transform(ch);
	    Texdraw.Arc a1 = (Texdraw.Arc)s[i+1].shape;
	    Texdraw.Arc a2 = s.length>i+2 && s[i+2].shape instanceof Texdraw.Arc? (Texdraw.Arc)s[i+2].shape: null;
	    if(a2!=null && !a2.location.equals(a1.location)) a2 = null; // it must be part of a different arrow head, then.
	    
	    a1.transform(ch);
	    if(a2!=null) a2.transform(ch);
	    
	    altStroke = newStroke(altStroke, s[i].stroke.lineWidth, s[i].stroke.pattern);

	    double sa = Math.toRadians(a0.start);
        double ea = Math.toRadians(a0.end);
        extent = (negativeExtent? -1: 1)*(float)Math.toDegrees(MathTools.d(sa, ea));
		
		r = (float)(a0.location.distance(a1.location) - (stroke.getLineWidth()-altStroke.getLineWidth()))/2;
		
	    return i+(a2!=null? 3: 2);
	}

}
