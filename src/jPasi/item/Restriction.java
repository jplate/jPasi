/*
 * Created on 17.12.2006
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.HINT;
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
import jPasi.codec.Codec1;
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
 * 
 */
public class Restriction extends BinaryRelationship {
    
    private static final long serialVersionUID = -841144750774606225L;
    
	public static final float DEFAULT_W0 = 10;
	public static final float DEFAULT_W1 = 15;
	public static final float DEFAULT_WC = 10;
     
    // for the left-hand point line:
    public static final float DEFAULT_X = 9;
    public static final float DEFAULT_Y = 0;
	public static final float DEFAULT_X0 = 7;
    public static final float DEFAULT_Y0 = 4;
    public static final float DEFAULT_X1 = 7;
    public static final float DEFAULT_Y1 = 4;
	public static final boolean DEFAULT_FILLED = false;	

    public static final float DEFAULT_X2 = 11;
    public static final float DEFAULT_Y2 = 4;

    protected float x0 = DEFAULT_X0;
	protected float y0 = DEFAULT_Y0;
	protected float x1 = DEFAULT_X1;
	protected float y1 = DEFAULT_Y1;
	protected float x = DEFAULT_X;
	protected float y = DEFAULT_Y;
	protected CubicCurve2D.Float pointLine = new CubicCurve2D.Float();
    
    public float x2 = DEFAULT_X2;
    public float y2 = DEFAULT_Y2;
    
    protected Line2D.Float rStroke = new Line2D.Float();
    
    public Restriction() {}
    
    public Restriction(ENode e0, ENode e1) {
        super(e0, e1);
        w0 = 10;
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
        Line2D.Float l = getStraightPointLine(x, -y, x2, -y2);
        /*
         * The first point might deviate somewhat from the last point of the cubic curve, so we make the line ourselves, 
         * using only the second point, so that the two shapes form a path. 
         */        
        rStroke.setLine(pointLine.x2, pointLine.y2, l.x2, l.y2);        
        
	    return new Shape[]{pointLine, rStroke};
    }
    
    public Shape[] computeFilledArrowhead() {
        pointLine = getCubicPointLine(x0, -y0, x1, -y1, x, -y, false);
        
	    return new Shape[]{pointLine};
    }

    protected boolean arrowHeadHasNonFillableShapes() {
        return true;
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
    		float x2a = x2*factor;
    		float y2a = y2*factor;
    		if(ok(x0a) && ok(y0a) && ok(x1a) && ok(y1a) && ok(xa) && ok(ya) && ok(x2a) && ok(y2a)) {
    			x0 = x0a;
    			y0 = y0a;
    			x1 = x1a;
    			y1 = y1a;
    			x = xa;
    			y = ya;
    			x2 = x2a;
    			y2 = y2a;
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0 && 
    			TransformModifier.isFlippingArrowheads(modifier)) {
    		y0 = -y0;
    		y1 = -y1;
    		y = -y;
    		y2 = -y2;
    	}
    }
	
	// The following methods are intended for use with EditPanel:
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
    public float getX2() {
        return x2;
    }
    public void setX2(float x2) {
        this.x2 = x2;
	    shapeChanged(false);
    }
    public float getY2() {
        return y2;
    }
    public void setY2(float y2) {
        this.y2 = y2;
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
	    
	    EditorEntry x2Info = new EditorEntry("X2", FLOAT, "X 4");
	    x2Info.setSpinnerValues(DEFAULT_X2, -99, 99, 1f, false);
	    result.add(x2Info);

	    EditorEntry y2Info = new EditorEntry("Y2", FLOAT, "Y 4");
	    y2Info.setSpinnerValues(DEFAULT_Y2, -99, 99, 1f, false);
	    result.add(y2Info);
	    
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
		    return Codec1.encodeFloat(x0)+" "+Codec1.encodeFloat(x1)+" "+Codec1.encodeFloat(x)+" "+Codec1.encodeFloat(x2);
	    }
	}
	
	public int parseArrowHead(StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(!(i<s.length && s[i].shape instanceof Path) &&
	       !(i+1<s.length && s[i].shape instanceof CubicCurve &&
	               s[i+1].shape instanceof Path)) {
	        throw new ParseException(TEX, 
	                "Line/curve sequence, or curve followed by line/curve sequence, expected");
	    }
		int ch = canvas.getHeight();
		int k = 0;
		CubicCurve c;
		Texdraw.Line l;		
		if(s[i].shape instanceof CubicCurve) {
			c = (CubicCurve)s[i].shape.transform(ch);
			setShading(c.filled? c.fillLevel: 1);
			k = 1;
		}
	    Path p = (Path)s[i+k].shape;
		if(!(p.shapes.length==2 && 
		        p.shapes[0] instanceof CubicCurve && 
		        p.shapes[1] instanceof Texdraw.Line)) {
		    throw new ParseException(TEX, "Wrong sequence of shapes");
		}
		c = (CubicCurve)p.shapes[0].transform(ch);
		l = (Texdraw.Line)p.shapes[1].transform(ch);
		k += 1;

		float lw = s[i].stroke.lineWidth;		
		altStroke = newStroke(altStroke, lw, s[i].stroke.pattern);
		
		if(!rigidPoint) {
	        if(info==null) {
	            throw new ParseException(HINT, "Info string required");
	        }
			String[] sp = info.split("\\s+");
			if(sp.length!=4) {
			    throw new ParseException(HINT, "Invalid info string: "+info);
			}
			x0 = Codec1.decodeFloat(sp[0]);
    	    y0 = getYCoordinate(c.p1a, x0);
			x1 = Codec1.decodeFloat(sp[1]);
    	    y1 = getYCoordinate(c.p2a, x1);
			x = Codec1.decodeFloat(sp[2]);
    	    y = getYCoordinate(c.p2, x);
			x2 = Codec1.decodeFloat(sp[3]);
    	    y2 = getYCoordinate(l.p2, x2); 		
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
			Point2D.Float p2 = rotateBack(l.p2, c.p1, gamma);
			x2 = p2.x;
			y2 = p2.y==0? 0: -p2.y;		
		}

	    return i+k;
	}

}
