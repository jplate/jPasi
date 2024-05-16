/*
 * Created on 11.11.2006
 *
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.*;
import static jPasi.item.Direction.CLOCKWISE;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.List;

import jPasi.Canvas;
import jPasi.TransformModifier;
import jPasi.codec.CodePortion;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.edit.EditorEntry;
import util.DimensionF;
import util.MathTools;

public abstract class Connector extends DependentItem {
 
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_CPR = 10;
    public static final float[] DEFAULT_ALT_DASH = null;
	
	/**
	 * For use in subclass-implementations of parseArrowHead().
	 */
	protected static Point2D.Float rotateBack(Point2D p, Point2D refP, double gamma) {
		double beta = MathTools.angle(p, refP);
		double d = p.distance(refP);
		double alpha = gamma - beta;
		float x = (float)(d*Math.cos(alpha));
		if(x==-0) x = 0;
		float y = (float)(d*Math.sin(alpha));
		if(y==-0) y = 0;
		return new Point2D.Float(x, y);
	}

    protected volatile CubicCurve2D adjustedLine;
    protected volatile CubicCurve2D adjustedLineD; // for display on screen
	protected EditorEntry angle0Info;
	protected EditorEntry angle1Info;
	private boolean anglesFound = false; // whether preferred angles have already been determined
    
    protected Shape[] arrowHead; 
    protected Shape[] arrowHeadD; // for display on screen
    protected Shape[] filledArrowHead; 
    protected Shape[] filledArrowHeadD; // for display on screen 

    protected double baseAngle;
	protected double chi0, chi1; // the preferred absolute angles 
	protected float cpr0, cpr1; // control point radii
	public float gap0, gap1; // gap values used for computing the 'adjusted line'; directly manipulated by CompoundArrow
	
	protected CubicCurve2D line;
	public boolean manualBaseAngle; // relevant if e[0]==e[1]: chi0 and chi1 have been selected manually 
	public boolean manualCPR; // the CPRs have been selected manually (and so should not be changed automatically)
	protected double phi0, phi1; // the user-defined angles of this Connector's contacts relative to the baseline angle
	protected double psi0, psi1; // the actual (as opposed to preferred) absolute angles
    protected boolean rigidPoint;
	
	protected float t = 0;

	protected float w0 = 0; // parameter controlling the length of the 'stiff part' at the beginning of the connector
	protected float w1 = 0; // parameter controlling the length of the 'stiff part' at the end of the connector
	protected float wc = 0;

	protected EditorEntry baseAngleInfo;
	protected EditorEntry releaseInfo;
	protected EditorEntry cpr0Info;
	protected EditorEntry cpr1Info;
	protected EditorEntry dashInfo;
    protected EditorEntry altDashInfo;
	protected EditorEntry altLinewidthInfo;

	public Connector() {
	    super();
	    w0 = getDefaultW0();
	    w1 = getDefaultW1();
	    wc = getDefaultWC();
        gap1 = computeGap1();
        rigidPoint = hasByDefaultRigidPoint();
    }
	
	private void adjustAngles() {
        if(!manualBaseAngle) {
            baseAngle = baseAngle();
        }
        psi0 = baseAngle + phi0;
        psi1 = baseAngle + phi1 - Math.PI;	    
	}
	
	protected void adjustItemLocation() {
	    if(item!=null) {
			defineBounds();
            Point2D pi = getItemLocation();
			if(pi!=null) {
				item.setBounds2D((float)pi.getX(), (float)pi.getY(), item.getWidth2D(), item.getHeight2D());		
				if(getParent()!=null) {
					((Canvas)getParent()).relocate(item);
					if(item instanceof ENode) {
						((ENode)item).arrangeContacts();
					}
				}
			}
        }
    }
	/**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    @Override
	public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
        double theta = MathTools.getRotation(t);
        if(involutes[0]==involutes[1]) {
            setBaseAngle(MathTools.normalize(baseAngle+theta, 0));
    	    if(baseAngleInfo!=null) baseAngleInfo.notify(this);
        }
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE)>0) {
    		float factor = (float)t.getScaleX();
    		
    		float w0a = w0*factor;
    		float w1a = w1*factor;
    		float wca = wc*factor;
    		float cpr0a = cpr0*factor;
    		float cpr1a = cpr1*factor;
    		if(ok(w0a) && ok(w1a) && ok(wca) && ok(cpr0a) && ok(cpr1a)) {
    			w0 = w0a;
    			w1 = w1a;
    			wc = wca;
    			cpr0 = cpr0a;
    			cpr1 = cpr1a;
	    		if(TransformModifier.isScalingNodes(modifier)) {
	        		float gapa = gap*factor;
	        		float gap0a = gap0*factor;
	        		float gap1a = gap1*factor;
	        		if(ok(gapa) && ok(gap0a) && ok(gap1a)) {
	        			gap = gapa;
	        			gap0 = gap0a;
	        			gap1 = gap1a;
	        		}
	    		}
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP)>0) {
    		phi0 = -phi0;
    		phi1 = -phi1;
    	}
    }
	
	/**
	 * Subclasses should override this if the set returned by their getArrowHead() method contains some Shapes that are not 
	 * returned by their getFilledArrowHead() method. 
	 * @return false
	 */
    protected boolean arrowHeadHasNonFillableShapes() {
        return false;
    }

    private double baseAngle() {
        Point2D c0 = involutes[0].getCenter();
        Point2D c1 = involutes[1].getCenter();
        return MathTools.angle(c0, c1);
	}
    
	public boolean canChangeBaseAngleDegrees() {
	    return involutes[0]==involutes[1];
	}
    
	public boolean canLiftConstraints() {
        return canResetPriority() || manualBaseAngle || manualCPR;
    }
	@Override
	public Object clone() throws CloneNotSupportedException {
        Connector clone = (Connector)super.clone();
        clone.arrowHead = null;   
        clone.filledArrowHead = null;
        return clone;
    }
	
	public Shape[] computeArrowhead() {
        return null;
    }
	
	/**
	 * Returns an array of Shapes for display (which involves rounding to whole pixels).
	 */
	public Shape[] computeDisplayArrowhead() {
        Shape[] head = getArrowHead();
        if(head==null) return null;
        
        Shape[] headD = new Shape[head.length];
        int i = 0;
        for(Shape s: head) {
        	headD[i++] = MathTools.round(s);
        }
        return headD;		
    }

	protected List<EditorEntry> computeArrowheadInfo() {
	    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
	    
	    altLinewidthInfo = new EditorEntry("AltLineWidth", FLOAT, "Line Width");
	    altLinewidthInfo.setSpinnerValues(getDefaultAltLineWidth(), 0, 99, .2f, false);
	    altLinewidthInfo.setGlobal(true);
	    result.add(altLinewidthInfo);

	    altDashInfo = new EditorEntry("AltDash", STRING, "Line Pattern");
	    altDashInfo.setDefaultValue(DEFAULT_ALT_DASH);		    
	    altDashInfo.requestNotifier();
	    altDashInfo.setGlobal(true);
	    result.add(altDashInfo);

	    EditorEntry gapInfo = new EditorEntry("Gap", FLOAT, "Gap");
	    gapInfo.setSpinnerValues(getDefaultGap(), -99, 99, .5f, false);
	    gapInfo.setGlobal(true);
	    result.add(gapInfo);

	    return result;
    }
	
    public Shape[] computeFilledArrowhead() {
        return computeArrowhead(); // as default
    }

	public Shape[] computeDisplayFilledArrowhead() {
        Shape[] head = getFilledArrowHead();
        if(head==null) return null;
        
        Shape[] headD = new Shape[head.length];
        int i = 0;
        for(Shape s: head) {
        	headD[i++] = MathTools.round(s);
        }
        return headD;	
	}   
        
     /**
     * Meant for invocation by CompoundArrow#parse() on the CompoundArrow's elements. Should return the appropriate 
     * gap parameter for the supplied gap1 value, or in other words, it should do the inverse of what computeGap1() does.
     */
    protected float computeGap() {
        return gap1;
    }

    protected float computeGap1() {
        return gap;
    }
  
    public List<EditorEntry> computeLineInfo() {
	    ArrayList<EditorEntry> list = new ArrayList<EditorEntry>();
	    
	    priorityInfo.requestNotifier();

	    EditorEntry w0Info = new EditorEntry("W0", FLOAT, "w0");
	    w0Info.setSpinnerValues(getDefaultW0(), 0, 999, 1f, false);
	    w0Info.setPotentiallyVisible(false);
	    list.add(w0Info);
	    
	    EditorEntry w1Info = new EditorEntry("W1", FLOAT, "w1");
	    w1Info.setSpinnerValues(getDefaultW1(), 0, 999, 1f, false);
	    w1Info.setPotentiallyVisible(false);
	    list.add(w1Info);
	    
	    EditorEntry wcInfo = new EditorEntry("WC", FLOAT, "w1");
	    wcInfo.setSpinnerValues(getDefaultWC(), 0, 999, 1f, false);
	    wcInfo.setPotentiallyVisible(false);
	    list.add(wcInfo);
	    
	    EditorEntry lwInfo = new EditorEntry("LineWidth", FLOAT, "Line Width");
	    lwInfo.setSpinnerValues(getDefaultLineWidth(), 0f, 99f, .2f, false);
	    list.add(lwInfo);

	    dashInfo = new EditorEntry("Dash", STRING, "Line Pattern");
	    dashInfo.setDefaultValue(DEFAULT_DASH);		    
	    dashInfo.requestNotifier();
	    list.add(dashInfo);
	    
	    baseAngleInfo = new EditorEntry("BaseAngleDegrees", INTEGER, "Base Angle");
	    baseAngleInfo.setSpinnerValues(0, -180, 180, 10, true);
        baseAngleInfo.requestNotifier();
        baseAngleInfo.setVisibleWhenDisabled(false);
        list.add(baseAngleInfo);

	    angle0Info = new EditorEntry("AngleDegrees0", FLOAT, "Angle 1");
	    angle0Info.setSpinnerValues(java.lang.Float.NaN, -180F, 180F, 10F, true);
	    angle0Info.requestNotifier();
	    list.add(angle0Info);
	    
	    cpr0Info = new EditorEntry("CPR0", INTEGER, "Distance 1");
	    cpr0Info.setSpinnerValues(java.lang.Float.NaN, 0, 999, 5, false);
	    cpr0Info.requestNotifier();
	    list.add(cpr0Info);
	    
	    angle1Info = new EditorEntry("AngleDegrees1", FLOAT, "Angle 2");
	    angle1Info.setSpinnerValues(java.lang.Float.NaN, -180F, 180F, 10F, true);
	    angle1Info.requestNotifier();
	    list.add(angle1Info);
	    
	    cpr1Info = new EditorEntry("CPR1", INTEGER, "Distance 2");
	    cpr1Info.setSpinnerValues(java.lang.Float.NaN, 0, 999, 5, false);
	    cpr1Info.requestNotifier();
	    list.add(cpr1Info);

	    releaseInfo = new EditorEntry("LiftConstraints", ACTION, "Lift Constraints");
	    releaseInfo.requestNotifier();
	    list.add(releaseInfo);
	    
	    for(EditorEntry ee: list) {
	    	ee.setGlobal(true);
	    }
	    return list;
	}

    protected List<EditorEntry> computeNodeInfo() {
	    return null;
    }
    
    @Override
	public Shape computeShape() {        
        GeneralPath path = new GeneralPath();
        if(involutes!=null) {
	        gap1 = computeGap1();	        
	        CubicCurve2D adjustedLine = getAdjustedLine();
	        if(adjustedLine!=null) path.append(adjustedLine, false);
	        Shape[] s = getArrowHead();
	        if(s!=null) {
	            for(int i = 0; i<s.length; i++) {
	                path.append(s[i], false);
	            }
	        }
	        item.setSizeWhenHidden(getHiddenItemSize());
        }
        return path;
    }
    
    /**
     * Called after a manual change of a control point radius.
     */
    protected void cprChanged() {
        manualCPR = true;
	    releaseInfo.getNotifier().run(this);
	    invalidateShape(false);
	    defineBounds();
	    repaint();
		adjustItemLocation();
    }
    
    /**
     * Lifts the manual constraints imposed on the shape of this Connector's arrow by the user. 
     * Invoked by the EditorPane.
     */
    public void doLiftConstraints() {
        if(canResetPriority()) {
            resetPriority();
            priorityInfo.getNotifier().run(this);
            phi0 = 0;
            phi1 = 0;
        }
        manualBaseAngle = false;
        manualCPR = false;
        releaseInfo.getNotifier().run(this);
        for(int i = 0; i<involutes.length; i++) {
            involutes[i].arrangeContacts();
        }
        baseAngleInfo.getNotifier().run(this);
        angle0Info.getNotifier().run(this);
        angle1Info.getNotifier().run(this);
        cpr0Info.getNotifier().run(this);
        cpr1Info.getNotifier().run(this);
    }

	public void findPreferredAngles() {
	    ENode e0 = (ENode)this.involutes[0];
        ENode e1 = (ENode)this.involutes[1];
        float w0 = this.w0;
        float w1 = (int)(this.w1+gap1);
        
        boolean inverse = false;
		if(e0.getRadius()>e1.getRadius()) {
        	ENode e = e0;
        	float w = w0;
        	e0 = e1;
        	e1 = e;
        	w0 = w1;
        	w1 = w;
        	inverse = true;
        }
        float r0 = e0.getRadius();
        float r1 = e1.getRadius();
        Point2D c0 = e0.getCenter();
        Point2D c1 = e1.getCenter();
        
        float w = w0 + w1 + wc;

		float d = (float)c0.distance(c1);
		// discrepancy per Entity involved:
        float discr = w+r0+r1-d;
        discr = discr<0? 0: discr/2;
        
        // distance between centers at which exit points will become less than sqrt(2)*w apart:
        double d0 = Math.sqrt(w*w/2 - r1*Math.sin(Math.acos((r1-r0)/r1)))+r1-r0;
		double d1 = Math.min(w, w+r0+r1-d0)/2;
		//System.err.println("d1: "+d1);
		double rr = 1 + Math.log(r1/r0);
        double m0 = discr<r0? discr: discr<d1? r0 + (discr - r0)/(d1 - r0)*(r0*rr*d1/(w+r0+r1)): r0 + r0*rr*discr/(w+r0+r1);
        double m1 = discr<r0? discr: discr<d1? r0 + (discr - r0)/(d1 - r0)*(r0/rr*d1/(w+r0+r1)): r0 + r0/rr*discr/(w+r0+r1);
        //System.err.println("discr: "+discr+" m0: "+m0+" m1: "+m1);
        m0 = Math.min(m0, 9f/5*r0);
        m1 = Math.min(m1, 9f/5*r1);
                
        if(!manualBaseAngle) {
            baseAngle = MathTools.angle(c0, c1);
        }
        if(inverse) {
        	baseAngle = MathTools.normalize(baseAngle + Math.PI, 0);
        	this.chi0 = baseAngle - Math.acos((r1-m1)/r1);
        	this.chi1 = baseAngle - Math.PI + Math.acos((r0-m0)/r0);
        }
        else {
        	this.chi0 = baseAngle - Math.acos((r0-m0)/r0);
        	this.chi1 = baseAngle - Math.PI + Math.acos((r1-m1)/r1);
        }
        if(flexDirection==CLOCKWISE) {
        	this.chi0 = 2*baseAngle - this.chi0;
        	this.chi1 = 2*baseAngle - this.chi1;
        }
        //System.err.println("angles: "+gamma+", "+chi0+", "+chi1);
        
        anglesFound = true;
	}
	
    protected synchronized CubicCurve2D getAdjustedLine() {
	    if(adjustedLine==null) {
	        adjustedLine = getAdjustedLine(involutes[0].getRadius() + gap0, involutes[1].getRadius() + gap1);
	    }
	    return adjustedLine;
	}
	
    protected CubicCurve2D getDisplayAdjustedLine() {
	    if(adjustedLineD==null) {
	        adjustedLineD = (CubicCurve2D)MathTools.round(getAdjustedLine());
	    }
	    return adjustedLineD;
	}

    /**
	 * @return the line connecting the two involutes with the end points adjusted to leave room for an arrowhead (etc.).
	 * The specified parameters give the distances of the new line's endpoints from the centers of the two involutes.
	 */  
	protected CubicCurve2D getAdjustedLine(float r0, float r1) {
		Point2D.Float c0 = (Point2D.Float)involutes[0].getCenter();
		Point2D.Float c1 = (Point2D.Float)involutes[1].getCenter();
        CubicCurve2D.Float link = (CubicCurve2D.Float)getLine();
		Point2D.Float 
			p0 = (Point2D.Float)link.getP1(), 
			p0a = (Point2D.Float)link.getCtrlP1(), 
			p1a = (Point2D.Float)link.getCtrlP2(), 
			p1 = (Point2D.Float)link.getP2(),
			q0 = new Point2D.Float(),
			q1 = new Point2D.Float();
		q0.setLocation(c0.x + r0*(float)Math.cos(psi0), c0.y - r0*(float)Math.sin(psi0));
		q1.setLocation(c1.x + r1*(float)Math.cos(psi1), c1.y - r1*(float)Math.sin(psi1));
		p0a.setLocation(p0a.x + (q0.x-p0.x), p0a.y + (q0.y-p0.y));
		p1a.setLocation(p1a.x + (q1.x-p1.x), p1a.y + (q1.y-p1.y));
        return new CubicCurve2D.Float(q0.x, q0.y, p0a.x, p0a.y, p1a.x, p1a.y, q1.x, q1.y);
	}


	/**
	 * Invoked by an EditorPane.
	 */
	public String getAltDash() {
        return getDashArrayString(altStroke);
    }

	@Override
	public double getAngle(int k) {
		if(k==0) {
			return psi0;
		}
		else if(k==1) {
			return psi1;
		}
		else throw new IllegalArgumentException("Illegal argument: "+k);
	}
	
	public float getAngleDegrees(int k) {
	    double angle = 0;
	    if(k==0) {
	        angle = psi0 - baseAngle();
	    }
	    else {	
	        angle = psi1 - baseAngle() + Math.PI;
	    }
	    return (float)Math.toDegrees(MathTools.normalize(angle, 0));
	}

	/**
	 * Invoked by the EditorPane.
	 */
    public float getAngleDegrees0() {
	    return getAngleDegrees(0);
	}
    
	/**
	 * Invoked by the EditorPane.
	 */
	public float getAngleDegrees1() {
	    return getAngleDegrees(1);
	}	

	public Shape[] getArrowHead() {
        if(arrowHead==null) {
            arrowHead = computeArrowhead();
        }
        return arrowHead;
    }
	
	public Shape[] getDisplayArrowHead() {
        if(arrowHeadD==null) {
            arrowHeadD = computeDisplayArrowhead();
        }
        return arrowHeadD;
    }
	
	public Shape[] getDisplayFilledArrowHead() {
        if(filledArrowHeadD==null) {
            filledArrowHeadD = computeDisplayFilledArrowhead();
        }
        return filledArrowHeadD;
    }

	protected String getArrowHeadCode(int ch) {
        return getCode(getArrowHead(), getFilledArrowHead(), arrowHeadHasNonFillableShapes(), altStroke, ch);
    }
    
	public double getBaseAngle() {
		return baseAngle;
	}

    /**
     * To be called by EditorPane if the two involutes are the same entity.
     * @param degrees
     */
	public int getBaseAngleDegrees() {
	    return (int)Math.round(Math.toDegrees(baseAngle));
	}	

	/**
	 * Invoked by the EditorPane.
	 */
    public int getCPR0() {
        return (int)cpr0;
    }
    
	/**
	 * Invoked by the EditorPane.
	 */
    public int getCPR1() {
        return (int)cpr1;
    }
	
	protected CubicCurve2D.Float getCubicPointLine(float x0, float y0, float x1, float y1, float x, float y, boolean reverse) {
	    if(!rigidPoint) {
	        return getNonRigidCubicPointLine(getAdjustedLine(), x0, y0, x1, y1, x, y, reverse);
	    }
	    else {
		    float r = involutes[1].getRadius();
		    CubicCurve2D.Float line0 = (CubicCurve2D.Float)getAdjustedLine(0, r+gap);
		    return getRigidCubicPointLine(line0, x0, y0, x1, y1, x, y, reverse);
	    }
	}
	
    @Override
	public String getDash() {
        return getDashArrayString(stroke);
    }
    
	@Override
	public float getDefaultLineWidth() {
        return DEFAULT_LINEWIDTH;
    }

    public abstract float getDefaultW0();
    
    public abstract float getDefaultW1();

    public abstract float getDefaultWC();
    
    public Shape[] getFilledArrowHead() {
        if(filledArrowHead==null) {
            filledArrowHead = computeFilledArrowhead();
        }
        return filledArrowHead;
    }
    
    protected DimensionF getHiddenItemSize() {
		int n = Math.min(45, Math.max(20, Math.max(
							 (int)((w0+w1+wc)/2), 
							 (int)(involutes[0].getCenter().distance(involutes[1].getCenter())-involutes[0].getRadius()-involutes[1].getRadius())/3)));
		return new DimensionF(n, n);
	}

    @Override
	public List<EditorEntry> getInfo() {
	    if(info==null) {
		    List<EditorEntry> result = super.getInfo();
		    
		    result.addAll(getLineInfo());
		    result.addAll(getArrowheadInfo());
		    result.addAll(getNodeInfo());
		    result.addAll(getResetInfo());		   
		    
		    this.info = result;
	    }
	    return info;
	}
    
    protected List<EditorEntry> getLineInfo() {
    	ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
    	
		EditorEntry label = new EditorEntry("LabelInfo", LABEL, "Line Properties");		   
		List<EditorEntry> lineInfo = computeLineInfo();
		if(lineInfo!=null) {
		    result.add(label);
		    result.addAll(lineInfo);
		    label.setTopSpace(9);
		    label.setBottomSpace(5);
		}
		    
		return result;
    }
    
    protected List<EditorEntry> getArrowheadInfo() {
    	ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
    	
		EditorEntry label = new EditorEntry("LabelInfo", LABEL, "Arrow Head");
		List<EditorEntry> arrowheadInfo = computeArrowheadInfo();
		if(arrowheadInfo!=null) {
		    result.add(label);
		    result.addAll(arrowheadInfo);
		    label.setTopSpace(9);
		    label.setBottomSpace(5);
		}
		    
		return result;
    }
    
    
    protected List<EditorEntry> getNodeInfo() {
    	ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
  		EditorEntry label = new EditorEntry("LabelInfo", LABEL, "Node Properties");
		List<EditorEntry> nodeInfo = computeNodeInfo();
		if(nodeInfo!=null) {
		    result.add(label);
		    result.addAll(nodeInfo);
		    label.setTopSpace(9);
			label.setBottomSpace(5);
		}
		return result;
    }
		    
    protected List<EditorEntry> getResetInfo() {
    	ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
	    EditorEntry resetInfo = new EditorEntry(null, RESET, "Defaults");
	    resetInfo.setTopSpace(14);
	    result.add(resetInfo);
	   
	    return result;
	}
    
    public String getItemDash() {
        return getDashArrayString(item.stroke);
    }

    @Override
	public int getItemIndex() {
        return 2;
    }
    /*
	 * Assumes that the Item is an Entity
	 * @see pasi.State#getItemLocation()
	 */
	@Override
	public Point2D getItemLocation() {
	    CubicCurve2D curve = getAdjustedLine();
	    if(curve!=null) {
			Point2D p = MathTools.getPoint(curve, getItemLocationParameter());
			return new Point2D.Float((float)(p.getX()-item.getWidth2D()/2), (float)(p.getY()-item.getHeight2D()/2));
	    }
	    else return null;
	}
    public float getItemLocationParameter() {
		return t;
	}
    
    protected CubicCurve2D getLine() {
		if(line==null) {
			
	        float r0 = involutes[0].getRadius();
	        float r1 = involutes[1].getRadius();
	        Point2D c0 = involutes[0].getCenter();
	        Point2D c1 = involutes[1].getCenter();
	        
	        float w1 = this.w1+gap1;
	        float w = w0 + w1 + wc;

	        if(!manualCPR) { 
				float d = (float)c0.distance(c1);

				// discrepancy per Entity involved:
		        float discr = w+r0+r1-d;
		        discr = discr<0? 0: discr/2;
		        
		        // If |xi0a|+|xi1a| <= 0.0001, the curve is straight, and the Point at t=.5 should lie in the center. 
		        // This requires that cpr0-r0==cpr1-r1:
		        cpr0 = r0;
		        cpr1 = r1;
		        
	        	double xi0a = Math.min(MathTools.d(baseAngle, psi0), MathTools.d(psi0, baseAngle));
	        	double xi1a = Math.min(MathTools.d(baseAngle + Math.PI, psi1), MathTools.d(psi1, baseAngle + Math.PI));
	        	
	        	//System.err.println("discr: "+discr+"  psi0: "+psi0+"  psi1: "+psi1);
		        if(Math.abs(xi0a) + Math.abs(xi1a) > .0001) {
		        	double xi0a2 = xi0a*xi0a*(xi0a-Math.PI/4);
		        	xi0a2 = Math.max(0, xi0a2);
		        	double xi1a2 = xi1a*xi1a*(xi1a-Math.PI/4);
		        	xi1a2 = Math.max(0, xi1a2);
		        	cpr0 = r0 + w1 + (float)(xi0a<Math.PI/2? xi0a2*w0/4: Math.PI/2*Math.PI/2*w0/4+(xi0a-Math.PI/2)*discr);
			        if(xi0a<Math.PI/2) cpr0 = (float)Math.min(r0 + (1 + xi0a2)*(d - r1)/4 + xi0a2*w0, cpr0);
		        	cpr1 = r1 + w0 + (float)(xi1a<Math.PI/2? xi1a2*w1/4: Math.PI/2*Math.PI/2*w0/4+(xi1a-Math.PI/2)*discr); 
			        if(xi1a<Math.PI/2) cpr1 = (float)Math.min(r1 + (1 + xi1a2)*(d - r0)/4 + xi1a2*w1, cpr1);

		        	// give a boost to whichever side has a (significantly) greater distance to the base angle:
			        float d1 = d - r0 - r1;
			        if(xi0a>1.05f*xi1a) {
			            cpr0 = Math.max(cpr0, d1/3);
			        } else if(xi1a>1.05f*xi0a){
			            cpr1 = Math.max(cpr1, d1/3);
			        } else {
			            cpr0 = Math.max(cpr0, d1/4);
			            cpr1 = Math.max(cpr1, d1/4);
			        }
			        
			        // make sure that reflexive relationships look half-way elegant:
			        if(involutes[0]==involutes[1]) {
			            float dr0 = 4*r0-cpr0-gap0;
			            float dr1 = 4*r1-cpr1-gap1;
			            float dr = Math.max(dr0, dr1);
			            if(dr>0) {
			                cpr0 += dr;
			                cpr1 += dr;
			            }
			        }
		        }
		        //System.err.println("this: "+this+" xi0a: "+xi0a+" xi1a: "+xi1a+" cpr: "+cpr0+" "+cpr1+" d/3: "+d/3);
	        }
	        line = getLine1();
		}
		return line;        
	}
    
    protected CubicCurve2D getLine1() {
        float r0 = involutes[0].getRadius();
        float r1 = involutes[1].getRadius();
        Point2D.Float c0 = (Point2D.Float)involutes[0].getCenter();
        Point2D.Float c1 = (Point2D.Float)involutes[1].getCenter();
        
        return new CubicCurve2D.Float(
                c0.x + r0*(float)Math.cos(psi0), c0.y - r0*(float)Math.sin(psi0), 
                c0.x + cpr0*(float)Math.cos(psi0), c0.y - cpr0*(float)Math.sin(psi0), 
                c1.x + cpr1*(float)Math.cos(psi1), c1.y - cpr1*(float)Math.sin(psi1), 
                c1.x + r1*(float)Math.cos(psi1), c1.y - r1*(float)Math.sin(psi1));
    }
    
	protected String getLineCode(int ch) {
        return getCode(new Shape[]{getAdjustedLine()}, stroke, ch, null, java.lang.Float.NaN);
    }
    
    protected CubicCurve2D.Float getNonRigidCubicPointLine(CubicCurve2D line0, float x0, float y0, float x1, float y1, 
	        float x, float y, boolean reverse) {
	   
	    float epsilon = .0001f;
	    double[] t1 = new double[]{1}, 
       			t2 = new double[]{1},
       			t3 = new double[]{1};
       
       // the points on the arrow-line:
       Point2D.Float p = (Point2D.Float)line0.getP2(),
       		      p1 = (Point2D.Float)MathTools.travel(t1, x0, line0, -epsilon, (int)(1/epsilon)),
       		      p2 = (Point2D.Float)MathTools.travel(t2, x1, line0, -epsilon, (int)(1/epsilon)),
       			  p3 = (Point2D.Float)MathTools.travel(t3, x, line0, -epsilon, (int)(1/epsilon));
       
       double gamma1 = MathTools.angle(p1, MathTools.getPoint(line0, t1[0]-10*epsilon));
       double gamma2 = MathTools.angle(p2, MathTools.getPoint(line0, t2[0]-10*epsilon));
       double gamma3 = MathTools.angle(p3, MathTools.getPoint(line0, t3[0]-10*epsilon));

       CubicCurve2D.Float result = !reverse? new CubicCurve2D.Float(p.x, p.y, 
               (float)(p1.x+y0*Math.sin(gamma1)), (float)(p1.y+y0*Math.cos(gamma1)), 
               (float)(p2.x+y1*Math.sin(gamma2)), (float)(p2.y+y1*Math.cos(gamma2)), 
               (float)(p3.x+y*Math.sin(gamma3)), (float)(p3.y+y*Math.cos(gamma3))):
          new CubicCurve2D.Float(
               (float)(p3.x+y*Math.sin(gamma3)), (float)(p3.y+y*Math.cos(gamma3)), 
               (float)(p2.x+y1*Math.sin(gamma2)), (float)(p2.y+y1*Math.cos(gamma2)),
               (float)(p1.x+y0*Math.sin(gamma1)), (float)(p1.y+y0*Math.cos(gamma1)),                
               p.x, p.y);
       return result;
	}
	
  	protected Line2D.Float getNonRigidStraightPointLine(CubicCurve2D line0, float x0, float y0, float x1, float y1) {
	   float epsilon = .0001f;
       double[] t1 = new double[]{1}, 
       			t2 = new double[]{1};
       
       // the points on the arrow-line:
       Point2D.Float p1 = (Point2D.Float)MathTools.travel(t1, x0, line0, -epsilon, (int)(1/epsilon)),
       		         p2 = (Point2D.Float)MathTools.travel(t2, x1, line0, -epsilon, (int)(1/epsilon));
       
       double gamma1 = MathTools.angle(p1, MathTools.getPoint(line0, t1[0]-10*epsilon));
       double gamma2 = MathTools.angle(p2, MathTools.getPoint(line0, t2[0]-10*epsilon));

       return new Line2D.Float(
               (float)(p1.x+y0*Math.sin(gamma1)), (float)(p1.y+y0*Math.cos(gamma1)), 
               (float)(p2.x+y1*Math.sin(gamma2)), (float)(p2.y+y1*Math.cos(gamma2)));
	}
	
	@Override
	public double getPreferredAngle(int k) {
		if(!anglesFound) {
			findPreferredAngles();
		}
		if(k==0) {
			return chi0;
		}
		else if(k==1) {
			return chi1;
		}
		else throw new IllegalArgumentException("Illegal argument #"+k);
	}
	
	public double getPreferredAngle(int k, double a) {
		if(!anglesFound) {
			findPreferredAngles();
		}
		double chi0, chi1;
		if(k==0) {
			chi0 = this.chi0;
			chi1 = this.chi1;
		}
		else if(k==1) {
			chi0 = this.chi1;
			chi1 = this.chi0;
		}
		else throw new IllegalArgumentException("Illegal argument: "+k);
		double d = MathTools.normalize(a, chi1+Math.PI)-chi1;
		return chi0-d;
	}
	
	protected CubicCurve2D.Float getRigidCubicPointLine(CubicCurve2D line0, float x0, float y0, float x1, float y1, 
	        float x, float y, boolean reverse) {
        
	    float epsilon = 0.0001f;
	    Point2D.Float 
	  		p = (Float)line0.getP2(),
	  		q = (Float)getAdjustedLine().getP2(),
	        q_ = (Float)MathTools.travel(new double[]{1}, epsilon, line0, -epsilon, (int)(1/epsilon)); // ersatz q
	    double gamma = MathTools.angle(p, q==null? q_: q);
	  	if(q==null) {
	  	    q = p;
	  	}
	    return !reverse? new CubicCurve2D.Float(
	            p.x, p.y, 
	            (float)(p.x+x0*Math.cos(gamma)+y0*Math.sin(gamma)), (float)(p.y-x0*Math.sin(gamma)+y0*Math.cos(gamma)),
	            (float)(p.x+x1*Math.cos(gamma)+y1*Math.sin(gamma)), (float)(p.y-x1*Math.sin(gamma)+y1*Math.cos(gamma)),
	            (float)(p.x+x*Math.cos(gamma)+y*Math.sin(gamma)), (float)(p.y-x*Math.sin(gamma)+y*Math.cos(gamma))):
	       new CubicCurve2D.Float(
	            (float)(p.x+x*Math.cos(gamma)+y*Math.sin(gamma)), (float)(p.y-x*Math.sin(gamma)+y*Math.cos(gamma)),
	            (float)(p.x+x1*Math.cos(gamma)+y1*Math.sin(gamma)), (float)(p.y-x1*Math.sin(gamma)+y1*Math.cos(gamma)),
	            (float)(p.x+x0*Math.cos(gamma)+y0*Math.sin(gamma)), (float)(p.y-x0*Math.sin(gamma)+y0*Math.cos(gamma)),
	            p.x, p.y);	
	}
    
    protected Line2D.Float getRigidStraightPointLine(CubicCurve2D line0, float x0, float y0, float x1, float y1) {
        float epsilon = .0001f;
	    Point2D.Float p = (Point2D.Float)line0.getP2();        
        Point2D p1p = MathTools.travel(new double[]{1}, 10*epsilon, line0, -epsilon, (int)(1/epsilon));
        
        double gamma = MathTools.angle(p, p1p==null? p: p1p);

        return new Line2D.Float(
                (float)(p.x+x0*Math.cos(gamma)+y0*Math.sin(gamma)), (float)(p.y-x0*Math.sin(gamma)+y0*Math.cos(gamma)),
	            (float)(p.x+x1*Math.cos(gamma)+y1*Math.sin(gamma)), (float)(p.y-x1*Math.sin(gamma)+y1*Math.cos(gamma)));
	}
	
	protected Line2D.Float getStraightPointLine(float x0, float y0, float x1, float y1) {
	    if(!rigidPoint) {
	        return getNonRigidStraightPointLine(getAdjustedLine(), x0, y0, x1, y1);
	    }
	    else {
		    float r = involutes[1].getRadius();
		    CubicCurve2D.Float line0 = (CubicCurve2D.Float)getAdjustedLine(0, r+gap);
		    return getRigidStraightPointLine(line0, x0, y0, x1, y1);
	    }
	}
	
	@Override
	public String getTexdrawCode(int ch) {
        return getLineCode(ch) + getArrowHeadCode(ch);
    }
	
	public float getW0() {
    	return w0;
    }

	public float getW1() {
    	return w1;
    }
	
	public float getWC() {
    	return wc;
    }
    
	/**
     * For use in subclass-implementations of parseArrowHead. Returns the corresponding y-coordinate for the specified Point and 
     * x-coordinate relative to adjustedLine as x-axis.
     */
	protected float getYCoordinate(Point2D p1, float x) {
	    float epsilon = .0001f;
	    double[] t1 = new double[]{1}; 
		Point2D p = MathTools.travel(t1, x, adjustedLine, -epsilon, (int)(1/epsilon));
	    double gamma = MathTools.angle(p, MathTools.getPoint(adjustedLine, t1[0]-10*epsilon));
	    float result = (float)((MathTools.d(MathTools.angle(p, p1), gamma)<Math.PI? -1: 1)*p.distance(p1));
	    if(result==-0)  result = 0;
	    return result;
	}

	protected boolean hasByDefaultRigidPoint() {
	    return false;
    }

	public boolean hasSpecialLine() {
        return false;
    }

    /*
	 * Called after all components have been created in the subclass constructors.
	 */
	protected void init() {
		this.t = .5f;
		item.setResizeable(false);
		setItem(item);
	}
    
    @Override
	public void invalidate() {
		invalidateShape(false);
		anglesFound = false;
	}
	
	public void invalidateShape() {
		invalidateShape(true);
	}

	/**
	 * Called when involutes change location, but for some reason (such as high priority of this Connector in the Canvas,
	 * or the fact that the involutes retain their relative positions to each other), the angles in which this Connector
	 * makes contact with its involutes should be left unchanged. Where 'angles' here means either (a) the <i>absolute</i> angles
	 * of contact, or the user-determined <i>relative</i> angles, depending on the specified boolean. 
	 */	
	public void invalidateShape(boolean relative) {
		super.invalidate();
		line = null;
		adjustedLine = null;
		adjustedLineD = null;
		arrowHead = null;
		arrowHeadD = null;
		filledArrowHead = null;
		filledArrowHeadD = null;
		if(relative) {
		    adjustAngles();
		}
	}
			
	/**
	 * If a Connector is 'symmetric', that only means that it either has arrowheads on both sides (not necessarily similar to
	 * each other), or none at all. 
	 */
	public boolean isSymmetric() {
	    return false;
	}
	
	public ENode otherInvolute(ENode e) {
		if(e==this.involutes[0]) {
			return (ENode)this.involutes[1];
		}
		else if(e==this.involutes[1]) {
			return (ENode)this.involutes[0];
		}
		else {
			throw new IllegalArgumentException("Entity "+e+" not involved");
		}
	}
	
	@Override
	public synchronized void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Rectangle r = getBounds(); // these will be the bounds in the parent's coordinate system
		g2.translate(-r.x, -r.y);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.setPaint(paint);        
		paintLine(g2);
		paintArrowHead(g2);
	}
	
	protected void paintArrowHead(Graphics2D g) {
        g.setStroke(altStroke);
	    if(shading < 1) {
	        Shape[] s1 = getDisplayFilledArrowHead();
            if(s1!=null) {
		        GeneralPath path = new GeneralPath();
	            for(int i = 0; i<s1.length; i++) {
		            path.append(s1[i], true);
	            }
	            g.setPaint(shadePaint);
	            g.fill(path);
            }
        }
        Shape[] s = getDisplayArrowHead();        
        if(s!=null) for(int i = 0; i<s.length; i++) {
            g.setColor(getForeground());
        	g.draw(s[i]);
        }
    }
	
	protected void paintLine(Graphics2D g) {
        CubicCurve2D c = getDisplayAdjustedLine();
		g.setStroke(stroke);
	    g.draw(c);
    }
	
    @Override
	public void parse(String code, String info) throws ParseException {
	    Texdraw.StrokedShape[] s = Texdraw.getStrokedShapes(code, DEFAULT_LINEWIDTH);
	    int i = parseLine(s, info, 0);
	    parseArrowHead(s, info, i);
	    gap = computeGap();
	}
	
    /**
	 * does nothing.
	 */
	public int parseArrowHead(Texdraw.StrokedShape[] s, String info, int i) throws ParseException {	    
	    return i;
	}

	/**
	 * info ignored, may be null.
	 */
	public int parseLine(Texdraw.StrokedShape[] s, String info, int i) throws ParseException {	    
	    if(!(s[i].shape instanceof Texdraw.CubicCurve)) {
	        throw new ParseException(CodePortion.TEX, "Cubic curve command expected; got: "+s[i].shape);
	    }
	    Texdraw.CubicCurve curve = (Texdraw.CubicCurve)s[i].shape.transform(canvas.getHeight());
	    adjustedLine = new CubicCurve2D.Float(curve.p1.x, curve.p1.y, 
	            curve.p1a.x, curve.p1a.y, 
	            curve.p2a.x, curve.p2a.y, 
	            curve.p2.x, curve.p2.y);
	    final boolean debug = false;
	    if(debug) {
	    	System.err.println("Connector.parseLine: "+s[i].stroke);
	    }
    	setStroke(newStroke(stroke, s[i].stroke.lineWidth, s[i].stroke.pattern));
        
	    baseAngle = MathTools.angle(curve.p1, curve.p2);

	    Point2D.Float c0 = (Point2D.Float)involutes[0].getCenter();
	    Point2D.Float c1 = (Point2D.Float)involutes[1].getCenter();
	    float r0 = involutes[0].getRadius();
	    float r1 = involutes[1].getRadius();
	    gap0 = (float)c0.distance(curve.p1) - r0;
	    gap1 = (float)c1.distance(curve.p2) - r1;
	    
	    psi0 = MathTools.angle(c0, curve.p1);
	    psi1 = MathTools.angle(c1, curve.p2);
	    chi0 = psi0;
	    chi1 = psi1;
	    phi0 = psi0 - baseAngle;
	    phi1 = psi1 - baseAngle + Math.PI;
	    cpr0 = (float)c0.distance(curve.p1a);
	    cpr1 = (float)c1.distance(curve.p2a);
	    
	    return i+1;
	}

	@Override
	public void removeInvolute(Item e) {
		Item other = 
		        e==this.involutes[0]? e==this.involutes[1]? null: this.involutes[1]: 
		            e==this.involutes[1]? this.involutes[0]: null;		
		if(other!=null) {
			other.removeDependentItem(this);
		}
		Canvas c = getCanvas();
		c.removeItem(item);
		c.removeItem(this);
	}

	public void setAltDash(String s) {
        BasicStroke stroke = changeDashArray(altStroke, s, DEFAULT_ALT_DASH);
        setAltStroke(stroke);
        repaint();
        if(altDashInfo!=null) altDashInfo.getNotifier().run(this);
    }

	public void setAltStroke(BasicStroke s) {
        altStroke = s;
    }

	@Override
	public void setAngle(double a, int k) {
		if(k==0) {
			psi0 = MathTools.normalize(a, 0);
		}
		else if(k==1) {
			psi1 = MathTools.normalize(a, 0);
		}
		else throw new IllegalArgumentException("Illegal argument: "+k);
	}

    private void setAngleDegrees(int k, float degrees) {
	    if(k==0) {
	        phi0 = Math.toRadians(degrees);
	    } else {
	        phi1 = Math.toRadians(degrees);
	    }
	    if(priority!=specialPriority()) {
	        oldPriority = priority;
		    priority = specialPriority();	    
	        priorityResettable = true;
			priorityChangeable = false;
		    
			/*
			 * Set other phi values according to the current psi values.
			 */
			if(k==0) {
		        phi1 = psi1 - baseAngle + Math.PI;
		    } else {
		        phi0 = psi0 - baseAngle;
		    }
	    }

	    invalidateShape();
	    
	    priorityInfo.getNotifier().run(this);
	    releaseInfo.getNotifier().run(this);
	    
		involutes[k].arrangeContacts();
	}
	
    /**
	 * Invoked by the EditorPane.
	 * @param degrees
	 */
	public void setAngleDegrees0(float degrees) {
	    setAngleDegrees(0, degrees);
	}
    
	/**
	 * Invoked by the EditorPane.
	 * @param degrees
	 */
	public void setAngleDegrees1(float degrees) {
	    setAngleDegrees(1, degrees);
	}
	
	public void setBaseAngle(double a) {
	    baseAngle = a;
	    manualBaseAngle = true;
	    if(releaseInfo!=null) releaseInfo.notify(this);
	}
	
	/**
     * To be called by EditorPane if the two involutes are the same entity.
     * @param degrees
     */
	public void setBaseAngleDegrees(int degrees) {
	    setBaseAngle((float)Math.toRadians(degrees));
	    involutes[0].arrangeContacts();	    
	}

	/**
	 * Invoked by the EditorPane.
	 */
    public void setCPR0(int cpr0) {
        this.cpr0 = cpr0;
        cprChanged();
    }
	
    /**
	 * Invoked by the EditorPane.
	 */
    public void setCPR1(int cpr1) {
        this.cpr1 = cpr1;
        cprChanged();
    }
	
    /**
	 * Invoked by the EditorPane.
	 */
    @Override
	public void setDash(String s) {
        super.setDash(s);
        if(dashInfo!=null) dashInfo.notify(this);
    }
    
	/**
	 * Invoked by the EditorPane.
	 * @param degrees
	 */
    @Override
	public void setGap(float gap) {
        this.gap = gap;
        shapeChanged(true);
    }	

    public void setInvolute(int k, ENode e) {
	    setInvolute(k, e, true);
	}

    /**
     * Meant to be invoked after cloning. Therefore, no removal of this State from previous involutes.
     */
	public void setInvolute(int k, ENode e, boolean add) {
	    ENode[] inv = (ENode[])getInvolutes();
	    inv[k] = e;
	    if(add) {
	        e.addDependentItem(this);
	    }
	    invalidate();
	}
	
	@Override
	public void setItem(Item item) {
		this.item = item;
		item.setDelegate(true);
		item.setGovernor(this);
		if(item instanceof ENode) {
		    involutes[2] = (ENode)item;
		}
	}
	
    /**
	 * @return the closest legal item location (for the 'item' of this State) to the specified point.
	 */
	@Override
	public Point2D setItemLocation(Point2D p) {
		CubicCurve2D c = getAdjustedLine();
		t = (float)MathTools.closestTo(p, c, .05f, .95f);
		//System.err.println("  t: "+t);
		return getItemLocation();
	}
	
	public void setItemLocationParameter(float t) {
		this.t = t;
	}
	
	protected void setManualCPR(boolean b) {
		manualCPR = b;
    }
	
	protected void setRigidPoint(boolean b) {
		rigidPoint = b;
	}
	
	public void setW0(float f) {
    	w0 = f;
    }

    public void setW1(float f) {
    	w1 = f;
    }
    
    public void setWC(float f) {
    	wc = f;
    }

	protected void shapeChanged(boolean adjustItemLocation) {
        adjustedLine = null;
        adjustedLineD = null;
        arrowHead = null;
        arrowHeadD = null;
        filledArrowHead = null;
        filledArrowHeadD = null;
        super.shapeChanged();
        gap1 = computeGap1();
        if(adjustItemLocation) adjustItemLocation();
	}
	
	@Override
	public float specialPriority() {
        return -1;
    }

	@Override
	public String toString() {
    	return super.toString()+" chi0: "+chi0+" chi1: "+chi1;
    }
    

}
