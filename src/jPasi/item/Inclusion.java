/*
 * Created on 15.01.2007
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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.Path;
import jPasi.codec.Texdraw.StrokedShape;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class Inclusion extends BinaryRelationship {

    private static final long serialVersionUID = -7613485335275038343L;

	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 8;
	public static final float DEFAULT_WC = 8;
     
    public static final float DEFAULT_ALT_LINEWIDTH = .5f;
	public static final float DEFAULT_DEPTH = 8f;
    public static final float DEFAULT_SHADING = 0; // 1 = white, 0 = black

	// for the point line:
    public static final float DEFAULT_X = 8.5f;
    public static final float DEFAULT_Y = 3.5f;
	public static final float DEFAULT_X0 = 4.5f;
    public static final float DEFAULT_Y0 = .5f;
    public static final float DEFAULT_X1 = 8.5f;
    public static final float DEFAULT_Y1 = 3.5f;
    
	protected float depth = DEFAULT_DEPTH;
	protected float x_ = DEFAULT_X;
	protected float y_ = DEFAULT_Y;
	protected float x0 = DEFAULT_X0;
	protected float y0 = DEFAULT_Y0;
	protected float x1 = DEFAULT_X1;
	protected float y1 = DEFAULT_Y1;
	protected CubicCurve2D pointLine0 = new CubicCurve2D.Float();
    protected Point2D.Float point = new Point2D.Float();
    	
    public Inclusion() {}
    
    public Inclusion(ENode e0, ENode e1) {
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
        return gap+depth;
    }
 
    protected float computeGap() {
        return gap1-depth;
    }

	/* 
	 * @see pasi.item.State#getShape()
	 */
	public Shape[] computeArrowhead() {
	    float r1 = involutes[1].getRadius();
	    CubicCurve2D.Float line0 = (CubicCurve2D.Float)getAdjustedLine(0, r1+gap);
        CubicCurve2D.Float adjustedLine = (CubicCurve2D.Float)getAdjustedLine();
        Point2D.Float 
      		p = (Float)line0.getP2(),
      		q = (Float)adjustedLine.getP2();
        double gamma = MathTools.angle(p, q);   
        float yCorrection = (y0>0? -1f: 1f)*(getLineWidth()-getAltLineWidth())/2;
        float dx = (float)(yCorrection*Math.sin(gamma));
        float dy = (float)(yCorrection*Math.cos(gamma));
        p.setLocation(p.x+dx, p.y+dy);        
        point.setLocation(q.x+dx, q.y+dy);    
        pointLine0 = getCubicPointLine(x0, y0, x1, y1, x_, y_, false);
        Point2D.Float  
        	p1a = (Float)pointLine0.getCtrlP1(),
        	p2a = (Float)pointLine0.getCtrlP2(),
        	p2 = (Float)pointLine0.getP2();       
        pointLine0.setCurve(p.x, p.y, p1a.x+dx, p1a.y+dy, p2a.x, p2a.y, p2.x, p2.y);
        
		return new Shape[]{pointLine0, 
		        new Line2D.Float(p2.x, p2.y, point.x, point.y), 
		        new Line2D.Float(point.x, point.y, p.x, p.y)};
	}

    public float getDefaultAltLineWidth() {
        return DEFAULT_ALT_LINEWIDTH;
    }	

    public float getDefaultShading() {
    	return DEFAULT_SHADING;
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
    		if(ok(deptha) && ok(x_a) && ok(y_a) && ok(x0a) && ok(x1a) && ok(y1a)) {
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
	
	// The following methods are intended for use with EditPanel:
    public void setLineWidth(float lineWidth) {
        super.setLineWidth(lineWidth);
        shapeChanged(false);
    }
    public void setAltLineWidth(float altLineWidth) {
        super.setAltLineWidth(altLineWidth);
        shapeChanged(false);
    }
    
    public float getDepth() {
        return depth;
    }
    public void setDepth(float d) {
        this.depth = d;
	    shapeChanged(true);
    }
    public float getX_() {
        return x_;
    }
    public void setX_(float x_) {
        this.x_ = x_;
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
        return y_;
    }
    public void setY_(float y_) {
        this.y_ = y_;
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

	    EditorEntry x_Info = new EditorEntry("X_", FLOAT, "X 3");
	    x_Info.setSpinnerValues(DEFAULT_X, -99, 99, 1f, false);
	    result.add(x_Info);
	    
	    EditorEntry y_Info = new EditorEntry("Y_", FLOAT, "Y 3");
	    y_Info.setSpinnerValues(DEFAULT_Y, -99, 99, 1f, false);
	    result.add(y_Info);

	    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
	    shadingInfo.setSpinnerValues(1-getDefaultShading(), 0f, 1f, .05f, false);
	    result.add(shadingInfo);
	    
	    for(EditorEntry ee: result) {
	    	ee.setGlobal(true);
	    }
	    return result;
	}
	
	public int parseArrowHead(StrokedShape[] s, String info, int i) throws ParseException {	    
    	if(!(i<s.length && s[i].shape instanceof Path)) {
 	        throw new ParseException(TEX, "Line/curve sequence expected");
 	    }
 		int ch = canvas.getHeight();
 		int k;
 		CubicCurve c;
 		Texdraw.Line l0, l1;
	    Path p = (Path)s[i].shape;
		if(!(p.shapes.length==3 && 
		        p.shapes[0] instanceof Texdraw.CubicCurve && 
		        p.shapes[1] instanceof Texdraw.Line &&
		        p.shapes[2] instanceof Texdraw.Line)) {
		    throw new ParseException(TEX, "Wrong sequence of shapes");
		}
		c = (CubicCurve)p.shapes[0].transform(ch);
		l0 = (Texdraw.Line)p.shapes[1].transform(ch);
		l1 = (Texdraw.Line)p.shapes[2].transform(ch);
		setShading(p.filled? p.fillLevel: 1);
		k = 1;
 		    
		altStroke = newStroke(altStroke, s[i].stroke.lineWidth, s[i].stroke.pattern);
		depth = (float)l1.p1.distance(l1.p2);
		
		double gamma = MathTools.angle(l1.p1, c.p1);
		
		int cDir = MathTools.d(MathTools.angle(l1.p1, involutes[1].getCenter()), gamma)>Math.PI? -1: 1;
		float yCorrection = cDir*(getLineWidth()-getAltLineWidth())/2;
        
        Point2D.Float p0 = rotateBack(c.p1a, c.p1, gamma);
		x0 = p0.x;
		y0 = p0.y;
		Point2D.Float p1 = rotateBack(c.p2a, c.p1, gamma);
		x1 = p1.x;
		y1 = p1.y + yCorrection;
		Point2D.Float p_ = rotateBack(c.p2, c.p1, gamma);
		x_ = p_.x;
		y_ = p_.y + yCorrection;
		
	    return i+k;
	}

}
