/*
 * Created on 16.12.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.item;

import static jPasi.codec.CodePortion.*;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.INTEGER;
import static jPasi.edit.EditorEntry.Type.STRING;
import static jPasi.item.Direction.CLOCKWISE;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.Line;
import jPasi.codec.Texdraw.Path;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class Negation extends BinaryRelationship {

    private static final long serialVersionUID = -3804348512073060625L;
    
    public static final float DEFAULT_GAP = 0;
    public static final float DEFAULT_WIDTH = 14f;
    public static final float DEFAULT_LENGTH = 16f;
    public static final float DEFAULT_POSITION = .5f;
	public static final float DEFAULT_LINEWIDTH = .6f;
    public static final float DEFAULT_ITEM_LINEWIDTH = 0.6f;
    
	public static final float DEFAULT_W0 = 8;
	public static final float DEFAULT_W1 = 8;
     
    public float w = DEFAULT_WIDTH;
    public float l = DEFAULT_LENGTH;
        
    protected Line2D.Float bar = new Line2D.Float();
    protected CubicCurve2D 
    	cl0 = new CubicCurve2D.Float(),
    	cl1 = new CubicCurve2D.Float(),
    	ql0 = new CubicCurve2D.Float(),
    	ql1 = new CubicCurve2D.Float();
    
    public Negation() {}
    
    public Negation(ENode e0, ENode e1) {
        super(e0, e1);
        w0 = 8;
        w1 = 8;
        wc = (int)getL();
        setItemLocationParameter(DEFAULT_POSITION);
    }
    
    @Override
    public float getDefaultItemLineWidth() {
        return DEFAULT_ITEM_LINEWIDTH;
    }

	@Override
    public float getDefaultW0() {
		return DEFAULT_W0;
	}
	@Override
    public float getDefaultW1() {
		return DEFAULT_W1;
	}
	@Override
    public float getDefaultWC() {
		return (int)getL();
	}    

    /* 
     * @see pasi.item.State#computeShape()
     */
    @Override
    public Shape computeShape() {
       float epsilon = .001f;
       CubicCurve2D.Float 
       		link = (CubicCurve2D.Float)getAdjustedLine(),
       		left = new CubicCurve2D.Float(),
       		right = new CubicCurve2D.Float();
       adjustedLine = link; // for getItemLocation()
       CubicCurve2D.subdivide(link, left, right);
       Point2D.Float 
       		q0 = (Point2D.Float)left.getP1(),
       		q1 = (Point2D.Float)right.getP2(),
       		q0a = (Point2D.Float)left.getCtrlP1(),
       		q1a = (Point2D.Float)right.getCtrlP2(),
            c = (Point2D.Float)MathTools.getPoint(link, t),
       		p0 = (Point2D.Float)MathTools.circleIntersection(c, l/2, link, t, 0, 5),
       		p1 = (Point2D.Float)MathTools.circleIntersection(c, l/2, link, t, 1, 5);
       if(p0==null) p0 = (Point2D.Float)MathTools.getPoint(link, Math.max(0, t-.1));
       if(p1==null) p1 = (Point2D.Float)MathTools.getPoint(link, Math.min(1, t+.1));
       
       double gamma = MathTools.angle(MathTools.getPoint(link, t-epsilon), MathTools.getPoint(link, t+epsilon));
       double dx = (flexDirection==CLOCKWISE? -1d: 1d) * w/2*Math.sin(gamma);
       double dy = (flexDirection==CLOCKWISE? -1d: 1d) * w/2*Math.cos(gamma);
       bar.setLine(c.x-dx, c.y-dy, c.x+dx, c.y+dy);
       ql0.setCurve(q0.x, q0.y, q0a.x, q0a.y, p0.x, p0.y, p0.x, p0.y);
       cl0.setCurve(p0.x, p0.y, p0.x, p0.y, c.x, c.y, c.x-dx, c.y-dy);
       ql1.setCurve(q1.x, q1.y, q1a.x, q1a.y, p1.x, p1.y, p1.x, p1.y);
       cl1.setCurve(p1.x, p1.y, p1.x, p1.y, c.x, c.y, c.x+dx, c.y+dy);
       
       GeneralPath path = new GeneralPath();
       path.append(ql0, false);
       path.append(cl0, false);
       path.append(bar, false);
       path.append(cl1, true);
       path.append(ql1, false);
 
       item.setSizeWhenHidden(getHiddenItemSize());

       return path;
    }
    
	@Override
    public boolean isSymmetric() {
	    return true;
	}
	
    @Override
    public boolean hasSpecialLine() {
        return true;
    }
    
    @Override
    public float getDefaultGap() {
        return DEFAULT_GAP;
    }
    
    @Override
    public synchronized void paintLine(Graphics2D g) {
		g.setPaint(paint);        
		g.setStroke(stroke);
	    if(stroke.getLineWidth()>0) {
			g.draw(ql0);
			g.draw(cl0);
			g.draw(ql1);
			g.draw(cl1);
	    }
		g.setStroke(altStroke);
		if(altStroke.getLineWidth()>0) {
		    g.draw(bar);
		}
	}

	@Override
    protected String getLineCode(int ch) {
        float[] dash = stroke.getDashArray();
	    return Texdraw.lineWidth(getLineWidth()) + (dash!=null? Texdraw.linePattern(dash): "") + 
	    	getCommand(ql0, ch) + getContinuingCommand(cl0, ch) +  (dash!=null? Texdraw.linePattern(null): "") +
        	getCode(new Shape[]{bar}, altStroke, ch, null, Float.NaN) + 
        	Texdraw.lineWidth(getLineWidth()) + (dash!=null? Texdraw.linePattern(dash): "") + 
        	getCommand(ql1, ch) + getContinuingCommand(cl1, ch) + (dash!=null? Texdraw.linePattern(null): "");
    }

    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    @Override
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float wa = w*factor;
    		float la = l*factor;
    		if(ok(wa) && ok(la)) {
    			w = wa;
    			l = la;
    		}
    	}
    }
	
	// The following methods are intended for use with EditPanel:
    public float getL() {
        return l;
    }
    
    public void setL(float l) {
        this.l = l;
        wc = (int)l;
        invalidateShape(false);
        for (IndependentItem<?> element : involutes) {
            element.arrangeContacts();
        }
    }
    
    @Override
    public void setItemLocationParameter(float t) {
        super.setItemLocationParameter(t);
        shapeChanged(true);
    }
    
    public float getW() {
        return w;
    }
 
    public void setW(float w) {
        this.w = w;
        shapeChanged(false);
    }
    
    @Override
    public void setFlipped(boolean b) {
    	super.setFlipped(b);
    	shapeChanged(false);
    }

    public void setGap(float gap) {
        super.setGap(gap);
        this.gap0 = gap;
        this.gap1 = gap;
        shapeChanged(false);
    }

    @Override
    public List<EditorEntry> computeArrowheadInfo() {
    	return null;
    }
    
    @Override
    public List<EditorEntry> computeLineInfo() {
        ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
        
	   result.addAll(super.computeLineInfo());
	    
	    EditorEntry posInfo = new EditorEntry("ItemLocationParameter", FLOAT, "Node Position");
	    posInfo.setSpinnerValues(DEFAULT_POSITION, 0, 1, .05f, true);
	    result.add(posInfo);
	    
	    EditorEntry gapInfo = new EditorEntry("Gap", FLOAT, "Gap");
	    gapInfo.setSpinnerValues(DEFAULT_GAP, -99, 99, .5f, false);
	    gapInfo.setGlobal(true);
	    result.add(gapInfo);
	    
	    EditorEntry widthInfo = new EditorEntry("W", FLOAT, "Center Width");
	    widthInfo.setSpinnerValues(DEFAULT_WIDTH, -99, 99, 1f, false);
	    result.add(widthInfo);

	    EditorEntry lengthInfo = new EditorEntry("L", FLOAT, "Center Length");
	    lengthInfo.setSpinnerValues(DEFAULT_LENGTH, 0, 99, 5f, false);
	    result.add(lengthInfo);
	    
	    for(EditorEntry ee: result) {
	    	ee.setGlobal(true);
	    }
	    return result;
	}
	
	@Override
    public int parseLine(Texdraw.StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(!(i+2<s.length &&
	         s[i].shape instanceof Path &&
	         s[i+1].shape instanceof Line &&
	         s[i+2].shape instanceof Path)) {
	        throw new ParseException(TEX, "Wrong sequence of shapes");
	    }
	    int ch = canvas.getHeight();
	    Path p0 = (Path)s[i].shape;
	    Path p1 = (Path)s[i+2].shape;
		if(!(p0.shapes.length==2 && 
		        p0.shapes[0] instanceof CubicCurve &&
		        p0.shapes[1] instanceof CubicCurve)) {
		    throw new ParseException(TEX, "Wrong sequence of shapes");
		}
		if(!(p1.shapes.length==2 && 
		        p1.shapes[0] instanceof CubicCurve &&
		        p1.shapes[1] instanceof CubicCurve)) {
		    throw new ParseException(TEX, "Wrong sequence of shapes");
		}
		CubicCurve ql0 = (CubicCurve)p0.shapes[0].transform(ch); 
		CubicCurve cl0 = (CubicCurve)p0.shapes[1].transform(ch); 
		CubicCurve ql1 = (CubicCurve)p1.shapes[0].transform(ch); 
		CubicCurve cl1 = (CubicCurve)p1.shapes[1].transform(ch);
		Line bar = (Line)s[i+1].shape.transform(ch);
	    
		w = (float)bar.p1.distance(bar.p2);
	    l = Math.round(cl0.p1.distance(cl1.p1));		
		
	    stroke = newStroke(stroke, s[i].stroke.lineWidth, s[i].stroke.pattern);
	    Point2D c0 = involutes[0].getCenter();
	    Point2D c1 = involutes[1].getCenter();
	    float r0 = involutes[0].getRadius();
	    float r1 = involutes[1].getRadius();
	    gap1 = (float)c1.distance(ql1.p1)-r1;
	    psi0 = MathTools.angle(c0, ql0.p1);
	    psi1 = MathTools.angle(c1, ql1.p2);
	    chi0 = psi0;
	    chi1 = psi1;
	    cpr0 = (float)c0.distance(ql0.p1a);
	    cpr1 = (float)c1.distance(ql1.p2a);
	    if(manualBaseAngle) {
	        baseAngle = MathTools.angle(ql0.p1, ql1.p2);
	    }
	    return i+3;
	}
}
