/*
 * Created on 12.12.2006
 *
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
public class UniversalQuantification extends BinaryRelationship {

    private static final long serialVersionUID = -1686853858245011918L;
    
	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 8;
	public static final float DEFAULT_WC = 8;
     
    public static final float DEFAULT_DEPTH = 4f;
	public static final float DEFAULT_HOOK_RADIUS = 2.5f;
	public static final float DEFAULT_HOOK_EXTENT = -180;
	
	protected float depth = DEFAULT_DEPTH;
	protected float radius = DEFAULT_HOOK_RADIUS;
	protected float extent = DEFAULT_HOOK_EXTENT;
	
	protected Arc2D.Float arc = new Arc2D.Float();
	
	public UniversalQuantification() {}
	
	public UniversalQuantification(ENode e0, ENode e1) {
		super(e0, e1);
		rigidPoint = true;
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
	    double epsilon = .005;
	    double[] t = new double[]{1};
        CubicCurve2D.Float adjustedLine = (CubicCurve2D.Float)getAdjustedLine();
		Point2D.Float p1 = (Point2D.Float)MathTools.travel(t, depth, adjustedLine, -epsilon, (int)(1/epsilon)),
					  p2 = (Point2D.Float)MathTools.getPoint(adjustedLine, t[0]-10*epsilon);
		
		double gamma = MathTools.angle(p2, p1);		
        
        arc.setArcByCenter(p1.x - radius*Math.sin(gamma), p1.y - radius*Math.cos(gamma), 
        		Math.abs(radius), Math.toDegrees(gamma)+(radius<0? 90: -90), (radius<0? -extent: extent), Arc2D.OPEN);

       return new Shape[]{arc};
	}
	
    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float ra = radius*factor;
    		float deptha = depth*factor;
    		if(ok(ra) && ok(deptha)) {
    			radius = ra;
    			depth = deptha;
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0 && 
    			TransformModifier.isFlippingArrowheads(modifier)) {
    		radius = -radius; 
    	}
    }
	
	// The following methods are intended for use with EditPanel:
    public float getDepth() {
        return depth;
    }
    public void setDepth(float gap1) {
        this.depth = gap1;
        shapeChanged(false);
    }
    public float getExtent() {
        return extent;
    }
    public void setExtent(float extent) {
        this.extent = extent;
        shapeChanged(false);
    }
    public float getRadius() {
        return radius;
    }
    public void setRadius(float r) {
        this.radius = r;
        shapeChanged(false);
    }

	public List<EditorEntry> computeArrowheadInfo() {
	    List<EditorEntry> result = super.computeArrowheadInfo();
	    
	    EditorEntry depthInfo = new EditorEntry("Depth", FLOAT, "Depth");
	    depthInfo.setSpinnerValues(DEFAULT_DEPTH, 0, 99, .5f, false);
	    result.add(depthInfo);

	    EditorEntry radiusInfo = new EditorEntry("Radius", FLOAT, "Radius");
	    radiusInfo.setSpinnerValues(DEFAULT_HOOK_RADIUS, -99, 99, .5f, false);
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
	    boolean extentInfoNeeded = radius<0!=extent<0 && altStroke.getDashArray()!=null;
	    return depth==0 && !extentInfoNeeded? null: 
	        !extentInfoNeeded? Codec1.encodeFloat(depth):
	        Codec1.encodeFloat(depth)+" "+Codec1.encodeFloat(extent);
	}
	
	public int parseArrowHead(Texdraw.StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(i>=s.length || 
	            !(s[i].shape instanceof Texdraw.Arc)) {
	        throw new ParseException(TEX, "One or two arcs expected");
	    }
	    int ch = canvas.getHeight();
	    Texdraw.Arc a0 = (Texdraw.Arc)s[i].shape;
	    Texdraw.Arc a1 = s.length>i+1 && s[i+1].shape instanceof Texdraw.Arc? 
	            (Texdraw.Arc)s[i+1].shape: null;
	    if(a1!=null && !a1.location.equals(a0.location)) a1 = null; // it must be part of a different arrow head, then.
	    
	    a0.transform(ch);
	    if(a1!=null) a1.transform(ch);
	    
	    depth = 0;
	    boolean extentSet = false;
	    if(info!=null) {
		    String[] sp = info.split("\\s+");
		    if(sp.length>=1) {
		        depth = Codec1.decodeFloat(sp[0]);
		        if(sp.length==2) {
		            extent = Codec1.decodeFloat(sp[1]);
		            extentSet = true;
		        }
		    }
	    }	    

	    double epsilon = .0001;
	    double[] t = new double[]{1};
        CubicCurve2D.Float adjustedLine = (CubicCurve2D.Float)getAdjustedLine();
		Point2D.Float p1 = (Point2D.Float)MathTools.travel(t, depth, adjustedLine, -epsilon, (int)(1/epsilon)),
		  			  p2 = (Point2D.Float)MathTools.getPoint(adjustedLine, t[0]-10*epsilon);
		double gamma = MathTools.angle(p2, p1);		

		double alpha = MathTools.angle(p1, a0.location);
		double d = MathTools.d(alpha, gamma);
		radius = (d<Math.PI? -1: 1)*a0.radius;
		
		boolean left = radius>0;
		double startTangent = gamma + (left? -Math.PI/2: Math.PI/2);
		boolean startOnLine = Math.abs(MathTools.normalize(Math.toRadians(a0.start) - startTangent, 0)) < 
							    Math.abs(MathTools.normalize(Math.toRadians(a0.end) - startTangent, 0));
	    if(!extentSet) {
	    	extent = (startOnLine==left? -1: 1) * (a0.start - a0.end);
	    }

	    /*
	     * Reconstruct pattern.
	     */
		float[] pattern = s[i+(a1!=null? 1: 0)].stroke.pattern;
		if(radius<0!=extent<0 && pattern!=null && pattern.length>1) { 
            float[] pattern1 = pattern;
            if(pattern.length%2==1) {
                pattern1 = new float[2*pattern.length];
                System.arraycopy(pattern, 0, pattern1, 0, pattern.length);
                System.arraycopy(pattern, 0, pattern1, pattern.length, pattern.length);
            }
            else {
                pattern1 = new float[pattern.length];
                System.arraycopy(pattern, 0, pattern1, 0, pattern.length);
            }
            double[] rest = new double[] {0};
            boolean[] opaque = new boolean[] {false};
            int m = pattern1.length;
            Texdraw.Arc a = a1!=null? a1: a0;
            int n = findIndex(Math.toDegrees(MathTools.d(Math.toRadians(a.start), Math.toRadians(a.end)))*a.radius, 
                    0, pattern1, rest, opaque);
            if(!opaque[0]) {
                n = n==0? m-1: n-1;
            }
            
            // new pattern
            for(int j = 0; j<pattern.length; j++) {
                pattern[j] = pattern1[(m+n-j)%m];
            }		    
		}
		/*
		for(int j = 0; j<pattern.length; j++) {
		    System.err.print(" "+pattern[j]);
		}
		System.err.println();*/
	    altStroke = newStroke(altStroke, s[i].stroke.lineWidth, pattern);
	    
	    return i+(a1!=null? 2: 1);
	}

}
