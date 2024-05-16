/*
 * Created on 15.01.2007
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.Line;
import jPasi.codec.Texdraw.Path;
import jPasi.codec.Texdraw.StrokedShape;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class Transition extends BinaryRelationship {

    private static final long serialVersionUID = 7127752792649454155L;

	public static final float DEFAULT_W0 = 7;
	public static final float DEFAULT_W1 = 7;
	public static final float DEFAULT_WC = 8;
     
    public static final float DEFAULT_DEPTH = 8f;

	// for the point line:
    public static final float DEFAULT_X = 10;
    public static final float DEFAULT_Y = 4;
    public static final float DEFAULT_X1 = 10;
    public static final float DEFAULT_Y1 = 4;
	public static final float DEFAULT_X0 = 0;
    public static final float DEFAULT_Y0 = 0;
	public static final boolean DEFAULT_FILLED = false;	
    
	protected float depth = DEFAULT_DEPTH;
	protected float x_ = DEFAULT_X;
	protected float y_ = DEFAULT_Y;
	protected float x0 = DEFAULT_X0;
	protected float y0 = DEFAULT_Y0;
	protected float x1 = DEFAULT_X1;
	protected float y1 = DEFAULT_Y1;
	protected CubicCurve2D.Float pointLine0; 
	protected CubicCurve2D.Float pointLine1;
	protected Point2D.Float point;
	
	public Transition() {}
	
	public Transition(ENode e0, ENode e1) {
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
	protected boolean hasByDefaultRigidPoint() {
	    return true;
    }
	
	public float computeGap1() {
	    return gap+depth+getLineWidth()/2;
	}	

    protected float computeGap() {
        return gap1-depth-getLineWidth()/2;
    }

	public Shape[] computeArrowhead() {
        point = (Float)getAdjustedLine().getP2();
	    pointLine0 = getCubicPointLine(x0, y0, x1, y1, x_, y_, false);
	    pointLine1 = getCubicPointLine(x0, -y0, x1, -y1, x_, -y_, false);
	    Point2D.Float p0 = (Float)pointLine0.getP2();
	    Point2D.Float p1 = (Float)pointLine1.getP2();
	    
	    return new Shape[]{pointLine0, pointLine1, 
                new Line2D.Float(p0.x, p0.y, point.x, point.y),
                new Line2D.Float(p1.x, p1.y, point.x, point.y)}; 
	}

	public Shape[] computeFilledArrowhead() {
        point = (Float)adjustedLine.getP2();
	    pointLine0 = getCubicPointLine(x0, y0, x1, y1, x_, y_, true);
	    pointLine1 = getCubicPointLine(x0, -y0, x1, -y1, x_, -y_, false);
	    Point2D.Float p0 = (Float)pointLine0.getP1();
	    Point2D.Float p1 = (Float)pointLine1.getP2();
	    
	    return new Shape[]{pointLine0, pointLine1, 
                new Line2D.Float(p1.x, p1.y, point.x, point.y), 
                new Line2D.Float(point.x, point.y, p0.x, p0.y)};
	}	

    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float deptha = depth*factor;
    		float x_a = x_*factor;
    		float y_a = y_*factor;
    		float x0a = x0*factor;
    		float y0a = y0*factor;
    		float x1a = x1*factor;
    		float y1a = y1*factor;
    		if(ok(deptha) && ok(x_a) && ok(y_a) && ok(x0a) && ok(y0a) && ok(x1a) && ok(y1a)) {
    			depth = deptha;
    			x_ = x_a;
    			y_ = y_a;
    			x0 = x0a;
    			y0 = y0a;
    			x1 = x1a;
    			y1 = y1a;
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0 && 
    			TransformModifier.isFlippingArrowheads(modifier)) {
    		y_ = -y_;
    		y0 = -y0;
    		y1 = -y1;
    	}
    }
	
    protected boolean coalesceDrawnShapes() {
    	return false;
    }

    // The following methods are intended for use with EditPanel:
     public float getDepth() {
        return depth;
    }
    public void setDepth(float depth) {
        this.depth = depth;
	    shapeChanged(true);
    }
   public float getX0() {
        return x0;
    }
    public void setX0(float x0) {
        this.x0 = x0;
	    shapeChanged(false);
    }
    public float getX_() {
        return x_;
    }
    public void setX_(float x) {
        this.x_ = x;
	    shapeChanged(false);
    }
    public float getX1() {
        return x1;
    }
    public void setX1(float x) {
        this.x1 = x;
	    shapeChanged(false);
    }
    public float getY_() {
        return y_;
    }
    public void setY_(float y) {
        this.y_ = y;
	    shapeChanged(false);
    }
    public float getY0() {
        return y0;
    }
    public void setY0(float y) {
        this.y0 = y;
	    shapeChanged(false);
    }
    public float getY1() {
        return y1;
    }
    public void setY1(float y) {
        this.y1 = y;
	    shapeChanged(false);
    }
    public List<EditorEntry> computeArrowheadInfo() {
	    List<EditorEntry> result = super.computeArrowheadInfo();
	    
	    EditorEntry depthInfo = new EditorEntry("Depth", FLOAT, "Depth");
	    depthInfo.setSpinnerValues(DEFAULT_DEPTH, 0, 99, 1f, false);
	    result.add(depthInfo);

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
		
	public int parseArrowHead(StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(!(i<s.length && s[i].shape instanceof Path) &&
	       !(i+3<s.length && s[i].shape instanceof CubicCurve &&
	               s[i+1].shape instanceof CubicCurve &&
	               s[i+2].shape instanceof Line &&
	               s[i+3].shape instanceof Line)) {
	        throw new ParseException(TEX, "Line/curve sequence and/or 4 shapes expected");
	    }
		int ch = canvas.getHeight();
		int k;
		CubicCurve c1;
		Line l0;
		boolean filled = false;
		if(s[i].shape instanceof Path) {
		    Path p = (Path)s[i].shape;
			if(!(p.shapes.length==4 && 
			        p.shapes[0] instanceof CubicCurve && 
			        p.shapes[1] instanceof CubicCurve && 
			        p.shapes[2] instanceof Line &&
			        p.shapes[3] instanceof Line)) {
			    throw new ParseException(TEX, "Wrong sequence of shapes");
			}
			c1 = (CubicCurve)p.shapes[1].transform(ch);
			l0 = (Line)p.shapes[2].transform(ch);
			filled = true;
			setShading(p.fillLevel);
			k = 1;
		} 
		else {
			c1 = (CubicCurve)s[i+1].shape.transform(ch);
			l0 = (Line)s[i+2].shape.transform(ch);
			k = 4;
		}
		float lw = s[i].stroke.lineWidth;
		if(s[i].stroke.pattern!=null && filled) { // special case: Path + 4 Shapes
		    if(!(i+4<s.length && s[i+1].shape instanceof CubicCurve &&
		            s[i+2].shape instanceof CubicCurve &&
		            s[i+3].shape instanceof Line &&
		            s[i+4].shape instanceof Line)) {
		        throw new ParseException(TEX, "Further shapes expected.");
		    }
	        lw = s[i+1].stroke.lineWidth;
		    k += 4;
		}
		altStroke = newStroke(altStroke, lw, s[i].stroke.pattern);
		
		depth = (float)c1.p1.distance(l0.p2) - getLineWidth()/2;
		
		double gamma = MathTools.angle(l0.p2, c1.p1);
		
        Point2D.Float p0 = rotateBack(c1.p1a, c1.p1, gamma);
		x0 = p0.x;
		y0 = p0.y==0? 0: -p0.y;
		Point2D.Float p1 = rotateBack(c1.p2a, c1.p1, gamma);
		x1 = p1.x;
		y1 = p1.y==0? 0: -p1.y;
		Point2D.Float p_ = rotateBack(c1.p2, c1.p1, gamma);
		x_ = p_.x;
		y_ = p_.y==0? 0: -p_.y;		

	    return i+k;
	}
	
}