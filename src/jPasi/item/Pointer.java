/*
 * Created on 14.12.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.item;

import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.RESET;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.Line;
import jPasi.codec.Texdraw.Path;
import jPasi.edit.EditorEntry;

/**
 * @author Jan Plate
 *
 */
public class Pointer extends Ornament {

    private static final long serialVersionUID = -901944440194198969L;
    public static final float DEFAULT_DEPTH = 2;
	public static final float DEFAULT_OUTER_BASE = 6; // distance between leftPoint and rightPoint
	public static final float DEFAULT_INNER_BASE = 4; // distance between leftPoint_ and rightPoint_ 
	public static final float DEFAULT_HEIGHT = 6f;
    public static final float DEFAULT_PREFERRED_ANGLE = (float)-Math.PI/2;
	public static final float DEFAULT_LINEWIDTH = 0;
    public static final float DEFAULT_SHADING = 0; // 1 = white, 0 = black
	
	protected float depth = DEFAULT_DEPTH;
	protected float innerBase = DEFAULT_INNER_BASE;
	protected float outerBase = DEFAULT_OUTER_BASE;
	protected float height = DEFAULT_HEIGHT;
	protected Point2D.Float nearPoint = new Point2D.Float(),
							leftPoint = new Point2D.Float(),
							leftPoint_ = new Point2D.Float(),
							rightPoint = new Point2D.Float(),
							rightPoint_ = new Point2D.Float();
	protected CubicCurve2D.Float innerLine = new CubicCurve2D.Float();
	
	public Pointer() {}
	
	public Pointer(ENode e) {
		super(e);
		item = this;
	}

    public float getDefaultPreferredAngle() {
       return DEFAULT_PREFERRED_ANGLE;
    }
    
    public float getDefaultGap() {
        return DEFAULT_GAP;
    }
    
    public float getDefaultLineWidth() {
        return DEFAULT_LINEWIDTH;
    }	
    
    public float getDefaultShading() {
    	return DEFAULT_SHADING;
    }

    public Shape computeShape() {
		Point2D.Float c = (Point2D.Float)involutes[0].getCenter(),
			p = new Point2D.Float();
		float r = involutes[0].getRadius() + gap;
		float r1 = r + height;
		float r2 = (float)Math.hypot(r1, outerBase/2);
		float r3 = (float)Math.hypot(r1, innerBase/2);
		double beta0 = Math.acos(r1/r2);
		double beta1 = Math.acos(r1/r3);
		
		nearPoint.setLocation(c.x + r*(float)Math.cos(psi), c.y - r*(float)Math.sin(psi));
		p.setLocation(c.x + (r+depth)*(float)Math.cos(psi), c.y - (r+depth)*(float)Math.sin(psi));
		leftPoint.setLocation(c.x + r2*(float)Math.cos(psi-beta0), c.y - r2*(float)Math.sin(psi-beta0));
		leftPoint_.setLocation(c.x + r3*(float)Math.cos(psi-beta1), c.y - r3*(float)Math.sin(psi-beta1));
		rightPoint.setLocation(c.x + r2*(float)Math.cos(psi+beta0), c.y - r2*(float)Math.sin(psi+beta0));
		rightPoint_.setLocation(c.x + r3*(float)Math.cos(psi+beta1), c.y - r3*(float)Math.sin(psi+beta1));
		innerLine.setCurve(leftPoint_.x, leftPoint_.y, p.x, p.y, p.x, p.y, rightPoint_.x, rightPoint_.y);
		
		GeneralPath path = new GeneralPath();
		path.append(innerLine, false);
		path.lineTo(rightPoint.x, rightPoint.y);
		path.lineTo(nearPoint.x, nearPoint.y);
		path.lineTo(leftPoint.x, leftPoint.y);
		path.lineTo(leftPoint_.x, leftPoint_.y);
		
		return path;
	}

	public synchronized void paint(Graphics g) {
	    super.paintBorder(g);
		Graphics2D g2 = (Graphics2D)g;
		Rectangle2D.Float r = (Rectangle2D.Float)getBounds2D(); // these will be the bounds in the parent's coordinate system
		
		g2.translate(-r.x+.5f, -r.y+.5f);
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
	    g2.setStroke(stroke); 
	
	    Shape s = getShape();
	    
	    // shape always non-transparent, even if white:
        g2.setPaint(shadePaint);
        g2.fill(s);
        g2.setPaint(getForeground());
	    g2.draw(s);
	}
	
	public String getTexdrawCode(int ch) {
		return super.getTexdrawCode(ch) + getCommand(innerLine, ch) + Texdraw.line(rightPoint.x, ch - rightPoint.y) +
			Texdraw.line(nearPoint.x, ch - nearPoint.y) + Texdraw.line(leftPoint.x, ch - leftPoint.y) +
			Texdraw.line(leftPoint_.x, ch - leftPoint_.y) + Texdraw.lfill(shading);
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
    		float innerBasea = innerBase*factor;
    		float outerBasea = outerBase*factor;
    		float heighta = height*factor;
    		if(ok(deptha) && ok(innerBasea) && ok(outerBasea) && ok(heighta)) {
    			depth = deptha;
    			innerBase = innerBasea;
    			outerBase = outerBasea;
    			height = heighta;    		
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0) {
    		double xf = t.getScaleX();
    		chi = (xf<0? Math.PI: 0) - chi;
    	}
    }
	
	// The following methods are intended for use with EditPanel:
    public float getDepth() {
        return depth;
    }
    public void setDepth(float d) {
        this.depth = d;
        shapeChanged();
    }
    public float getH() {
        return height;
    }
    public void setH(float height) {
        this.height = height;
        shapeChanged();
    }
    public float getInnerBase() {
        return innerBase;
    }
    public void setInnerBase(float innerBase) {
        this.innerBase = innerBase;
        shapeChanged();
    }
    public float getOuterBase() {
        return outerBase;
    }
    public void setOuterBase(float outerBase) {
        this.outerBase = outerBase;
        shapeChanged();
    }

    public List<EditorEntry> getInfo() {
	    if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    ArrayList<EditorEntry> list = new ArrayList<EditorEntry>();
		    result.addAll(super.getInfo());
		    
		    EditorEntry gapInfo = new EditorEntry("Gap", FLOAT, "Gap");
		    gapInfo.setSpinnerValues(DEFAULT_GAP, -999f, 999f, 1f, false);
		    list.add(gapInfo);

		    EditorEntry depthInfo = new EditorEntry("Depth", FLOAT, "Depth");
		    depthInfo.setSpinnerValues(DEFAULT_DEPTH, -999f, 999f, 1f, false);
		    list.add(depthInfo);
		    
		    EditorEntry innerWidthInfo = new EditorEntry("InnerBase", FLOAT, "Width 1");
		    innerWidthInfo.setSpinnerValues(DEFAULT_INNER_BASE, 0f, 999f, 1f, false);
		    list.add(innerWidthInfo);
		    
		    EditorEntry outerWidthInfo = new EditorEntry("OuterBase", FLOAT, "Width 2");
		    outerWidthInfo.setSpinnerValues(DEFAULT_OUTER_BASE, 0f, 999f, 1f, false);
		    list.add(outerWidthInfo);
		    
		    EditorEntry heightInfo = new EditorEntry("H", FLOAT, "Height");
		    heightInfo.setSpinnerValues(DEFAULT_HEIGHT, -999f, 999f, 1f, false);
		    list.add(heightInfo);
		    
		    EditorEntry lwInfo = new EditorEntry("LineWidth", FLOAT, "Line Width");
		    lwInfo.setSpinnerValues(getDefaultLineWidth(), 0f, 99f, .2f, false);
		    list.add(lwInfo);
		    
		    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
		    shadingInfo.setSpinnerValues(1-DEFAULT_SHADING, 0f, 1f, .05f, false);
		    list.add(shadingInfo);

		    for(EditorEntry ee: list) {
		    	ee.setGlobal(true);
		    }
		    result.addAll(list);
		    
		    EditorEntry resetInfo = new EditorEntry(null, RESET, "Defaults");
		    result.add(resetInfo);
		    
		    result.trimToSize();
		    this.info = result;
	    }
	    return info;
	}
	
	public void parse(String code, String info) throws ParseException {
	    Texdraw.StrokedShape[] s = Texdraw.getStrokedShapes(code, DEFAULT_LINEWIDTH);	    
	    if(!(s.length==1 && s[0].shape instanceof Path)) {
	        String s0 = "";
    	    if(0<s.length) {
    	        s0 = ", not "+s[0].shape.genericDescription();
    	    }
    	    throw new ParseException(TEX, "Line/curve sequence expected, not "+s0);
	    }
	    Path p = (Path)s[0].shape;
		if(!(p.shapes.length==5 && 
		        p.shapes[0] instanceof CubicCurve &&
		        p.shapes[1] instanceof Line &&
		        p.shapes[2] instanceof Line &&
		        p.shapes[3] instanceof Line &&
		        p.shapes[4] instanceof Line)) {
		    throw new ParseException(TEX, "Wrong sequence of shapes");
		}
    	int ch = canvas.getHeight();
		CubicCurve c = (CubicCurve)p.shapes[0].transform(ch);
	    Line l0 = (Line)p.shapes[1].transform(ch);
	    Line l1 = (Line)p.shapes[2].transform(ch);
	    Line l2 = (Line)p.shapes[3].transform(ch);
	    Point2D c0 = involutes[0].getCenter();
	    
	    outerBase = (float)l0.p2.distance(l2.p2);
	    innerBase = (float)c.p1.distance(c.p2);
	    Point2D.Float pc = new Point2D.Float((c.p1.x + c.p2.x)/2, (c.p1.y + c.p2.y)/2);
	    float sprod = (c.p1a.x - l1.p2.x)*(pc.x - l1.p2.x) + (c.p1a.y - l1.p2.y)*(pc.y - l1.p2.y); 
	    depth = Math.signum(sprod)*(float)c.p1a.distance(l1.p2);
	    gap = (float)l1.p2.distance(c0) - involutes[0].getRadius();
	    double hypot = l1.p1.distance(l1.p2);
	    height = (float)Math.sqrt(hypot*hypot - outerBase*outerBase/4); 

	    setShading(p.filled? p.fillLevel: 1);
	    stroke = newStroke(stroke, s[0].stroke.lineWidth, s[0].stroke.pattern);	    
	}


}
