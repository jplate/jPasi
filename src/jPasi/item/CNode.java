/*
 * Created on 27.05.2007
 *
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.ACTION;
import static jPasi.edit.EditorEntry.Type.BOOLEAN;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.INTEGER;
import static jPasi.edit.EditorEntry.Type.LABEL;
import static jPasi.edit.EditorEntry.Type.RESET;
import static jPasi.edit.EditorEntry.Type.STRING;

import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jPasi.Canvas;
import jPasi.edit.EditorEntry;
import jPasi.item.group.Group;
import jPasi.item.group.GroupManager;
import jPasi.item.group.GroupMember;
import jPasi.item.group.Groups;
import util.DimensionF;
import util.MathTools;

/**
 * @author Jan Plate
 * 
 * CNodes (Contour nodes) define the shape of a Contour through their location and other properties. Since their location
 * is therefore not defined by the Contour (rather than the other way around), they are not 'governed' by it. 
 *
 */
public class CNode extends IndependentItem<Contour.NodeGroup> implements SplineGeometry {
	
    public class MyGroupManager extends GroupManager<CNode> {
		public MyGroupManager(CNode cn)  {
			super(cn);
		}
    	public boolean canCreate() {
            Group<?> hg = Groups.getHighestActiveGroup(CNode.this);
        	if(!(hg instanceof GroupMember)) {
        		hg = Groups.getHighestActiveGroupBut(1, CNode.this);
        	}
	        return hg!=null;
	    }
    }
	
    private static final long serialVersionUID = 1844361739207902810L;

	public static final int DEFAULT_CPR = 10;
	public static final float DEFAULT_RADIUS = 7;
	
	/**
	 * The tolerance (in radians) for line parallelity in scaling-by-dragging.
	 */
	public static final double EPSILON = .09;
    
	/**
     * The minimum distance (in pixels) along any axis of the respective Contour at which two successive nodes can be 
     * brought to lie from each other during scaling-by-dragging.
     */
	public static final double MINIMUM_DISTANCE = 2;
    
	protected EditorEntry angle1Info;
	protected EditorEntry angle2Info;
    private volatile double corner;    
	float cpr1 = DEFAULT_CPR;
    protected EditorEntry cpr1Info;
    float cpr2 = DEFAULT_CPR;    
    protected EditorEntry cpr2Info;
    
	protected boolean fixedCornerAngle;
	
	/**
	 * True if this CNode should have no line to its predecessor.
	 */
	protected boolean omitLine; 
	
    public GroupManager<?> myGroupManager = new MyGroupManager(this);
    
    double phi1 = 0f;  // deviation from straight line to previous node
    double phi2 = 0f; // deviation from straight line to next node

    public CNode() {
        this(null, new Point2D.Float());
    }

    public CNode(Canvas c) {
        this(c, new Point2D.Float());
    }
    
    public CNode(Canvas c, java.awt.Point p) {
        this(c, p!=null? new Point2D.Float(p.x, p.y): new Point2D.Float());        
    }
    
    public CNode(Canvas c, Point2D.Float p) {
        super(c, null, true);
        radius = DEFAULT_RADIUS;
        DimensionF size = new DimensionF(2*radius, 2*radius); 
        setSizeWhenHidden(size);      
        setBounds2D(p.x-radius, p.y-radius, 2*radius, 2*radius);
    }
    
    /**
     * @see jPasi.item.group.GroupMember#accepts(pasi.item.Group)
     */
    public boolean acceptsAsGroup(Group<?> g) {
        return g instanceof Contour.NodeGroup;
    }
    
    public void adjustToTransform(AffineTransform t, int modifier) {
    	Contour sp = getContour();
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE)>0) {
    		float factor = (float)t.getScaleX();
    		float cpr1a = cpr1*factor;
    		float cpr2a = cpr2*factor;
    		if(ok(cpr1a) && ok(cpr2a)) {
    			cpr1 = cpr1a;
    			cpr2 = cpr2a;
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP)>0) {
    		phi1 = -phi1;
    		phi2 = -phi2;
    	}
    }
    
    public void arrangeContacts() {
    }
    
    public void arrangeContacts(Set<Item> affectedItems, Set<DependentItem> immuneStates) {        
        updateAngles();
    }

	/**
	 * Invoked by the EditorPane.
	 */
    public boolean canChangeRadius() {
        return false;
    }
    
	/**
	 * Invoked by the EditorPane.
	 */
    public boolean canChangeXAxis() {
        return getContour().fixedAngles;
    }
    
	/**
	 * Invoked by the EditorPane.
	 */
    public boolean canChangeYAxis() {
        return getContour().fixedAngles;
    }
    
	/* (non-Javadoc)
     * @see util.MapCloneable#clone(java.util.Map)
     */
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
	    CNode clone = (CNode)super.clone(map);
	    
	    /*
	     * The interplay between 
	     * CNode and Contour is similar here to that between Item and State. In particular, the cloning of an CNode will
	     * ensure that it is put into a clone of its Contour, unless that Contour is already a clone according to the 
	     * cloneMap.
	     */
	    Contour sp = clone.getContour();
	    Contour spc = (Contour)map.get(sp);
	    if(spc==null) {
	    	spc = (Contour)sp.clone(map);
	    }
	    clone.setGroup(spc.getNodeGroup());
	    
	    return clone;
    }
    
	private strictfp double cornerAngle() {
        Point2D c0 = getCenter();
        Point2D c1 = getPrevious().getCenter();
        Point2D c2 = getNext().getCenter();
        double baseAngle1 = MathTools.angle(c0, c1);
        double baseAngle2 = MathTools.angle(c0, c2);
        return MathTools.d(baseAngle2, baseAngle1);
    }

	/**
	 * Invoked by the EditorPane.
	 */
    public void doAddNode() {
        int index = group.indexOf(this);        
        getContour().createNode(index+1);
    }
	
	/**
	 * Invoked by the EditorPane.
	 */
    public void setXAxis(float a) {
    	Contour cont = getContour();
    	double a1 = Math.toRadians(a);
    	if(cont.lX==null) {
    		cont.createAxis(0, true);
    	}
    	if(a1!=cont.aX) cont.adjustAxis(0, a1);
    }
	
	/**
	 * Invoked by the EditorPane.
	 */
    public float getXAxis() {
        return (float)Math.toDegrees(getContour().aX);
    }

	/**
	 * Invoked by the EditorPane.
	 */
    public void setYAxis(float a) {
        Contour cont = getContour();
    	double a1 = Math.toRadians(a);
    	if(cont.lY==null) {
    		cont.createAxis(1, true);
    	}
    	if(a1!=cont.aY) cont.adjustAxis(1, a1);
    }
	
	/**
	 * Invoked by the EditorPane.
	 */
    public float getYAxis() {
        return (float)Math.toDegrees(getContour().aY);
    }

    /**
	 * Invoked by the EditorPane.
	 */
    public void doEqualizeAngularIntervals() {
        getContour().equalizeAngularIntervals(this);
    }

	/**
	 * Invoked by the EditorPane.
	 */
    public void doEqualizeCenterDistances() {
        getContour().equalizeCenterDistances(this);
    }
    
	public float getAngleDegrees(int k) {
	    return (float)Math.toDegrees(MathTools.normalize(k==1? phi1: phi2, 0));
	}	

	/**
	 * Invoked by the EditorPane.
	 */
    public float getAngleDegrees1() {
	    return getAngleDegrees(1);
	}
	
	/**
	 * Invoked by the EditorPane.
	 */
	public float getAngleDegrees2() {
	    return getAngleDegrees(2);
	}

    /**
	 * Invoked by the EditorPane.
	 */
    public int getCPR1() {
        return Math.round(cpr1);
    }

	/**
	 * Invoked by the EditorPane.
	 */
    public int getCPR2() {
        return Math.round(cpr2);
    }

    public String getDash()  {
        return getDashArrayString(getContour().stroke);
    }

    public float[] getDefaultDash() {
        return Contour.DEFAULT_DASH;
    }

    public float getDefaultRadius() {
        return DEFAULT_RADIUS;
    }

    public float getDefaultShading() {
        return Contour.DEFAULT_SHADING;
    }
    
	public Contour.NodeGroup getGroup() {
        return group;
    }
    
    public GroupManager<?> getGroupManager() {
	    return myGroupManager;
    }
    
    public List<EditorEntry> getInfo() {
        if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();		    

		    EditorEntry shpLabel = new EditorEntry("LabelInfo", LABEL, "Contour Properties");
		    shpLabel.setTopSpace(2);
		    shpLabel.setBottomSpace(5);
		    result.add(shpLabel);

		    EditorEntry zvInfo = new EditorEntry("ContourZValue", FLOAT, "Depth Level");
		    zvInfo.setSpinnerValues(0f, -99f, Canvas.DEFAULT_CONTOUR_Z, 1f, false);
		    zvInfo.setGlobal(true);
		    result.add(zvInfo);

		    EditorEntry lwInfo = new EditorEntry("LineWidth", FLOAT, "Line Width");
		    lwInfo.setSpinnerValues(getDefaultLineWidth(), 0f, 99f, .2f, false);
		    lwInfo.setGlobal(true);
		    result.add(lwInfo);
		    
		    dashInfo = new EditorEntry("Dash", STRING, "Line Pattern");
		    dashInfo.setDefaultValue(getDefaultDash());		    
		    dashInfo.setGlobal(true);
		    dashInfo.requestNotifier();
		    result.add(dashInfo);
		    
		    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
		    shadingInfo.setSpinnerValues(1-getDefaultShading(), 0f, 1f, .05f, false);
		    shadingInfo.setGlobal(true);
		    result.add(shadingInfo);
		    
		    EditorEntry dsInfo = new EditorEntry("FixedAngles", BOOLEAN, "Fixed angles");
		    dsInfo.setDefaultValue(Boolean.valueOf(false));
		    dsInfo.setGlobal(true);
		    dsInfo.setTopSpace(3);
		    result.add(dsInfo);	    

		    result.add(getContour().xAxisInfo);
		    result.add(getContour().yAxisInfo);

		    EditorEntry addInfo = new EditorEntry("AddNode", ACTION, "Add Node");
		    addInfo.setTopSpace(5);
		    addInfo.setBottomSpace(5);
		    result.add(addInfo);

		    EditorEntry eqLabel = new EditorEntry("LabelInfo", LABEL, "Equalize:");
		    result.add(eqLabel);
		    
		    EditorEntry eqIntInfo = new EditorEntry("EqualizeAngularIntervals", ACTION, "Angular Intervals");
		    eqIntInfo.setGlobal(true);
		    result.add(eqIntInfo);

		    EditorEntry eqDistInfo = new EditorEntry("EqualizeCenterDistances", ACTION, "Distances to Center");
		    eqDistInfo.setGlobal(true);
		    result.add(eqDistInfo);

		    EditorEntry npLabel = new EditorEntry("LabelInfo", LABEL, "Node Properties");
		    npLabel.setTopSpace(12);
		    npLabel.setBottomSpace(5);
		    result.add(npLabel);
		    
		    xInfo = new EditorEntry("XPos_g", FLOAT, "X Coord.");
		    xInfo.setSpinnerValues(java.lang.Float.NaN, -999f, 9999f, 1f, false);
		    xInfo.requestNotifier();
		    result.add(xInfo);
		    
		    yInfo = new EditorEntry("YPos_g", FLOAT, "Y Coord.");
		    yInfo.setSpinnerValues(java.lang.Float.NaN, 0f, 9999f, 1f, false);
		    yInfo.requestNotifier();
		    result.add(yInfo);
		    
		    EditorEntry loInfo = new EditorEntry("LineOmitted_EP", BOOLEAN, "No line to next node");
		    loInfo.setDefaultValue(Boolean.valueOf(false));
		    loInfo.setGlobal(true);
		    result.add(loInfo);	    

		    EditorEntry fcaInfo = new EditorEntry("CornerAngleFixed", BOOLEAN, "Fixed corner angle");
		    fcaInfo.setDefaultValue(Boolean.valueOf(false));
		    fcaInfo.setGlobal(true);
		    result.add(fcaInfo);	    

		    angle1Info = new EditorEntry("AngleDegrees1", FLOAT, "Angle 1");
		    angle1Info.setSpinnerValues(0f, -180f, 180f, 10f, true);
		    angle1Info.requestNotifier();
		    angle1Info.setGlobal(true);
		    result.add(angle1Info);
		    
		    cpr1Info = new EditorEntry("CPR1", INTEGER, "Distance 1");
		    cpr1Info.setSpinnerValues(DEFAULT_CPR, -999, 999, 5, false);
		    cpr1Info.requestNotifier();
		    cpr1Info.setGlobal(true);
		    result.add(cpr1Info);
		    
		    angle2Info = new EditorEntry("AngleDegrees2", FLOAT, "Angle 2");
		    angle2Info.setSpinnerValues(0f, -180f, 180f, 10f, true);
		    angle2Info.requestNotifier();
		    angle2Info.setGlobal(true);
		    result.add(angle2Info);
		    
		    cpr2Info = new EditorEntry("CPR2", INTEGER, "Distance 2");
		    cpr2Info.setSpinnerValues(DEFAULT_CPR, -999, 999, 5, false);
		    cpr2Info.requestNotifier();
		    cpr2Info.setGlobal(true);
		    result.add(cpr2Info);
		    
		    EditorEntry resetInfo = new EditorEntry(null, RESET, "Defaults");
		    resetInfo.setTopSpace(14);
		    result.add(resetInfo);
		    
		    result.trimToSize();
		    this.info = result;        
		}
        return info;
    }
    
    /**
	 * Invoked by the EditorPane.
	 */
    public String getLabelInfo() {
        return "";
    }

    public float getLineWidth() {
        return getContour().getLineWidth();
    }
    
    public CNode getNext() {
        List<CNode> l = (List<CNode>)group.getMembers();
        int i = l.indexOf(this);
        return l.get(i+1>=l.size()? 0: i+1);        
    }
    
    public CNode getPrevious() {
        List<CNode> l = (List<CNode>)group.getMembers();
        int i = l.indexOf(this);
        return l.get(i-1<0? l.size()-1: i-1);        
    }
    
    public float getShading() {
        return getContour().getShading();
    }

	public Contour getContour() {
        return group!=null? group.getContour(): null;
    }
    
	public float getContourZValue() {
		return Canvas.DEFAULT_CONTOUR_Z - getContour().getZValue();
	}
	
	public String getTexdrawCode(int canvasHeight) {
		return null;
	}
    
	public void init() {
    	corner = cornerAngle();
    }
	
	public boolean isCornerAngleFixed() {
        return fixedCornerAngle;
    } 
	
	public boolean isFixedAngles() {
    	return getContour().fixedAngles;
    } 
	
	public boolean isLineOmitted() {
        return omitLine;
    }
	
	public boolean isLineOmitted_EP() {
        return getNext().omitLine;
    }
	
	/**
	 * With S' as the Contour's coordinate system:
	 * dx: x-coordinate of movement vector in S', translated into normal system as (dxx, dxy):
	 * dy: corresponding y-coordinate, translated as (dyx, dyy);
	 * aX: angle by which S' is rotated; 
	 * c: this CNode's center;
	 * setX, setY: the sets of nodes undergoing horizontal/vertical movement (may be modified).
	 */
	public void moveBy(double dx1, double dy1, double dxx, double dxy, double dyx, double dyy, 
			Contour cont, Set<CNode> setX, Set<CNode> setY) {

		boolean reversed = cont.flexDirection!=Item.DEFAULT_DIRECTION;
		Point2D c = getCenter();
		int q = 0; //getQuadrant();		

		moveBy0(dx1, dy1, dxx, dxy, dyx, dyy, cont, false, reversed, c, q, setX, setY);
		moveBy1(dx1, dy1, dxx, dxy, dyx, dyy, cont, true, reversed, c, q, setX, setY);
    }
/*
	private int getQuadrant() {
		Contour cont = getContour();
		double aX = cont.aX;
		double aY = cont.aY;
		Point2D c0 = cont.getCenter();
		Point2D c = getCenter();
		double c0x = c0.getX();
		double c0y = c0.getY();
		double cx = c.getX();
		double cy = c.getY();
		double x = (cx-c0x)*Math.cos(-aY) + (cy-c0y)*Math.sin(-aY);
		double y = -(cx-c0x)*Math.sin(-aX) + (cy-c0y)*Math.cos(-aX);
			
		return (x > 0? 1: 0) | (y > 0? 2: 0);
	}
*/
	private void moveBy0(double dx, double dy, double dxx, double dxy, double dyx, double dyy, Contour cont, 
			boolean forward, boolean reversed, Point2D c, int q, Set<CNode> setX, Set<CNode> setY) {
		
		if(setX.contains(this)) {
			dx = 0;	dxx = 0; dxy = 0;
		}
		if(setY.contains(this)) {
			dy = 0;	dyx = 0; dyy = 0;
		}
		
		moveBy((float)(dxx + dyx), (float)(dxy + dyy));
		
		if(dx != 0) {
			setX.add(this);
		}
		if(dy != 0) {
			setY.add(this);
		}
		
		moveBy1(dx, dy, dxx, dxy, dyx, dyy, cont, forward, reversed, c, q, setX, setY);
	}
	
    
    private void moveBy1(double dx, double dy, double dxx, double dxy, double dyx, double dyy, Contour cont, 
			boolean forward, boolean reversed, Point2D c, int q, Set<CNode> setX, Set<CNode> setY) {
		
    	double theta = cont.aX;
		
		double cx = c.getX();
		double cy = c.getY();

		CNode cn = forward==reversed? getPrevious(): getNext();
		Point2D c1 = cn.getCenter();
		double c1x = c1.getX();
		double c1y = c1.getY();
		
		double a = MathTools.angle(c, c1);
		
		/*
		 * Determine whether cn will normally be (or has already been) moved along with us.
		 */
		boolean mx = true; 
		boolean my = true; 
		int s = getStage(dx, dy, cont, a, cn, forward, reversed);
		switch(s) {
		case 1: {mx = false; break;}
		case 2: {my = false; break;}
		case 3: {mx = false; my = false; break;}
		default:
		}
		mx = mx || setX.contains(cn);
		my = my || setY.contains(cn);

		/*
		 * Determine 'bump conditions' bx and by.
		 */
		double dx1 = (c1x - cx)*Math.cos(-theta) + (c1y - cy)*Math.sin(-theta);
		double dy1 = -(c1x - cx)*Math.sin(-theta) +  (c1y - cy)*Math.cos(-theta);
		double sx = Math.signum(dx);
		double sy = Math.signum(dy);
		boolean bx = !mx && sx!=0 && sx==Math.signum(dx1) && Math.abs(dx) + MINIMUM_DISTANCE > Math.abs(dx1);
		boolean by = !my && sy!=0 && sy==Math.signum(dy1) && Math.abs(dy) + MINIMUM_DISTANCE > Math.abs(dy1);

		if(bx || by) {
			/* (Relevant only if start-quadrant information is used) 
			 * Let cn be moved as if separately, without 'letting go' of movement, and initiating its own
			 * follow-up movements.
			 */
			//q = cn.getQuadrant();	
		} 
		else {
			int s1 = getStage(dx, dy, cont, a, cn, forward, reversed);
			//System.err.println("c1: "+c1+" "+s1);
			switch(s1) {
			case 1: {
				dx = 0;	dxx = 0; dxy = 0;
				break;
			}
			case 2: {
				dy = 0;	dyx = 0; dyy = 0;
				break;
			}
			case 3: {
				dx = 0; dxx = 0; dxy = 0;
				dy = 0;	dyx = 0; dyy = 0;
				break;
			}
			default:
			}
		}
		
		if(dx!=0 || dy!=0) {
			cn.moveBy0(dx, dy, dxx, dxy, dyx, dyy, cont, forward, reversed, c1, q, setX, setY);
		}		
	}
    
    /** 
     * Returns two bits encoding which directions of movement should be abandoned.
     */
    private int getStage(double dx, double dy, Contour cont, double a, CNode cn, 
    		boolean forward, boolean reversed) {
		double aX = cont.aX;
		double aY = cont.aY;
		double aXa = aX + Math.PI;
		double aYa = aY + Math.PI;
		
		double a1 = forward? MathTools.normalize(a, aX):
					 2*aX - MathTools.normalize(a, aX);
		double a1a = forward? MathTools.normalize(a, aXa):
			 		 2*aXa - MathTools.normalize(a, aXa);
		double a2 = forward? MathTools.normalize(a, aY):
			 		 2*aY - MathTools.normalize(a, aY);
		double a2a = forward? MathTools.normalize(a, aYa):
					 2*aYa - MathTools.normalize(a, aYa);
		
		int stage = 0;
		if(a1 > aX - EPSILON && a1 < aX + EPSILON || 
		   a1a > aXa - EPSILON && a1a < aXa + EPSILON) {
			stage = stage | 1;
		}
		if(a2 > aY - EPSILON && a2 < aY + EPSILON || 
		   a2a > aYa - EPSILON && a2a < aYa + EPSILON) {
			stage = stage | 2;
		}
	
		//System.err.println(forward+" cn: "+cn.getCenter()+" aX: "+aX+" aY: "+aY+" a: "+a+" stage: "+stage);
		
    	return stage;
    }
/*	
    /** 
     * Alternative implementation, taking into account the start-quadrant. 
     * q0 is the quadrant where we started, c the center of the next node. 
     * Returns two bits encoding which directions of movement should be abandoned.
     * /
    private int getStage(double dx, double dy, Contour cont, int q0, double a, CNode cn, 
    		boolean forward, boolean reversed) {
		
		boolean bx0 = (q0&1)>0, by0 = (q0&2)>0;

    	int q = cn.getQuadrant();
		boolean bx1 = (q&1)>0, by1 = (q&2)>0;
    	
		double gamma1;
		double gamma2;
		double aX = cont.aX;
		double aY = cont.aY;
		switch(q0) {
		case 0: {
			gamma1 = aX; 
			gamma2 = aY-Math.PI/2;
			break;
		} 
		case 1: {
			gamma1 = aY-Math.PI/2;
			gamma2 = aX-Math.PI;
			break;
		} 
		case 2: {
			gamma1 = aY+Math.PI/2;
			gamma2 = aX; 
			break;
		} 
		case 3: {
			gamma1 = aX-Math.PI;
			gamma2 = aY+Math.PI/2; 
			break;
		}
		default: throw new Error();
		}
		if(forward) {
			double d = gamma1;
			gamma1 = gamma2;
			gamma2 = d;
		}
		
		double a1 = forward? MathTools.normalize(a, gamma1):
					 2*gamma1 - MathTools.normalize(a, gamma1);
		double a2 = forward? MathTools.normalize(a, gamma2):
			 		2*gamma2 - MathTools.normalize(a, gamma2);
		
		boolean v1st = forward==(bx0==by0); 
				
		int stage = 0;
		if(a1 > gamma1 - EPSILON && a1 < gamma1 + EPSILON && 
				(v1st && by0!=by1 || !v1st && bx0!=bx1)) {
			stage = v1st? 2: 1;
		}
		if(a2 > gamma2 - EPSILON && a2 < gamma2 + EPSILON && 
				bx0!=bx1 && by0!=by1 && (v1st && dy==0 || !v1st && dx==0)) {
			stage = 3;
		}
	
		//System.err.println(forward+" cn: "+cn.getCenter()+" q: "+q+" g1: "+gamma1+" g2: "+gamma2+" a: "+a+" stage: "+stage);
		
    	return stage;
    }
*/
    /**
     * @see jPasi.item.SplineGeometry#setCPR(int, int)
     */
	public void setAngleDegrees(int k, double degrees) {
        if(!(k==1 || k==2)) throw new IllegalArgumentException("index should be either 1 or 2");
	    
        double a = Math.toRadians(degrees);
	    
	    if(k==1) {
	        if(fixedCornerAngle) {
	            phi2 += a - phi1;
	    	    if(angle2Info!=null) angle2Info.notify(this);	    
	        }
	        phi1 = a;
	    }
	    else {
	        if(fixedCornerAngle) {
	            phi1 += a - phi2;
			    if(angle1Info!=null) angle1Info.notify(this);
	        }
	        phi2 = a;
	    }
	    shapeChanged();
	}

    /**
	 * Invoked by the EditorPane.
	 * @param degrees
	 */
	public void setAngleDegrees1(float degrees) {
	    setAngleDegrees(1, degrees);
	}
    
	/**
	 * Invoked by the EditorPane.
	 * @param degrees
	 */
	public void setAngleDegrees2(float degrees) {
	    setAngleDegrees(2, degrees);
	}	

    public void setCornerAngleFixed(boolean b) {
        fixedCornerAngle = b;
    }

    /**
     * @see jPasi.item.SplineGeometry#setCPR(int, int)
     */
    public void setCPR(int k, int cpr) {
	    if(k==1) {
	        cpr1 = cpr;
	    } else if(k==2) {
	        cpr2 = cpr;
	    } else throw new IllegalArgumentException("index should be either 1 or 2");
        shapeChanged();
    }

	/**
	 * Invoked by the EditorPane.
	 */
    public void setCPR1(int cpr1) {
        setCPR(1, cpr1);
    }

	/**
	 * Invoked by the EditorPane.
	 */
    public void setCPR2(int cpr2) {
        setCPR(2, cpr2);
    }
    
    public void setDash(String s) {
        getContour().setDash(s);
    }

    public void setFixedAngles(boolean b) {
    	Contour cont = getContour();
    	cont.fixedAngles = b;
    	if(b) {
    		if(cont.lX==null) cont.createAxis(0, true);
    		else cont.lX.setVisible(true);
    		if(cont.lY==null) cont.createAxis(1, true);
    		else cont.lY.setVisible(true);
    	} else {
    		cont.removeAxes();    		
    	}
    	cont.xAxisInfo.notify(this);
    	cont.yAxisInfo.notify(this);
    }
    
    public void setGroup(Group<?> g) {
        if(g!=null && !(g instanceof Contour.NodeGroup)) {
            throw new IllegalArgumentException("Contour nodes can only be members of Contours.");
        }
        group = (Contour.NodeGroup)g;
    }

    public void setLineOmitted(boolean b) {
        omitLine = b;
        shapeChanged();
    }

    public void setLineOmitted_EP(boolean b) {
        getNext().setLineOmitted(b);
    }

    public void setLineWidth(float lineWidth) {
        getContour().setLineWidth(lineWidth);
     }
    
    public void setRadius(float s) { // radius stays fixed.        
    }
    
    public void setSelected(boolean b) {
    	super.setSelected(b);
    	if(!b) {
    		getContour().setAxesVisible(false);
    	}
    }
    
    public void setShading(float s) {
        Contour cont = getContour();
    	if(cont!=null) {
    		cont.setShading(s);
    	}
    }
    
    public void setContourZValue(float z) {
    	getContour().setZValue(Canvas.DEFAULT_CONTOUR_Z - z);
    	getCanvas().relocateItems();
    }
    
    public void setValues(double a1, float cpr1, double a2, float cpr2, boolean updateAngles) {
    	phi1 = a1;
    	phi2 = a2;
    	this.cpr1 = cpr1;
    	this.cpr2 = cpr2;
    	if(updateAngles) {
    		updateAngles();
    	}
    }
    
    /**
     * The curve c1 should end in this node's center, while c2 should start there.
     */
    protected void setValues(CubicCurve2D.Float c1, CubicCurve2D.Float c2, boolean updateAngles) {
        Point2D p0 = getCenter();
        if(c1!=null) {
	        double a1 = MathTools.angle(p0, c1.getCtrlP2());
	        double b1 = MathTools.angle(p0, c1.getP1());
	        phi1 = MathTools.normalize(a1 - b1, 0);
	        cpr1 = (float)p0.distance(c1.getCtrlP2());
        }
        if(c2!=null) {
	        double a2 = MathTools.angle(p0, c2.getCtrlP1());
	        double b2 = MathTools.angle(p0, c2.getP2());
	        phi2 = MathTools.normalize(a2 - b2, 0);
	        cpr2 = (float)p0.distance(c2.getCtrlP1());
        }
    	if(updateAngles) {
    		updateAngles();
    	}
    }

	protected void shapeChanged() {
        Contour sp = getContour();
        if(sp!=null) {
		    sp.invalidate();
	        sp.repaint();
        }
    }

	public boolean shouldBeHidden() {
        return true;
    }

	public String toString() {
        //return "CNode ("+bounds2d.x+", "+bounds2d.y+") "+(omitLine?1:0)+","+(fixedCornerAngle?1:0)+", "+phi1+","+phi2;
    	//return String.valueOf(hashCode());
		return omitLine? "0": "1";
    }

	protected void updateAngles() {
        double newCorner = cornerAngle();
        
        if(fixedCornerAngle) {
            double d = (newCorner - corner)/2;
            phi1 -= d;
            phi2 += d;
    	    if(angle1Info!=null) angle1Info.notify(this);
    	    if(angle2Info!=null) angle2Info.notify(this);	    
        }
        
        corner = newCorner;
    }

	public boolean hasFixedCornerAngle() {
		return fixedCornerAngle;
	}
	
	public void setFixedCornerAngle(boolean b) {
		fixedCornerAngle = b;
	}

}
