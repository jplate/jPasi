/*
 * Created on 27.05.2007
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.*;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.item.Direction.CLOCKWISE;
import static jPasi.item.Direction.COUNTERCLOCKWISE;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jPasi.Canvas;
import jPasi.TransformModifier;
import jPasi.codec.Codec1;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.CubicCurve;
import jPasi.codec.Texdraw.Path;
import jPasi.codec.Texdraw.StrokedShape;
import jPasi.edit.EditorEntry;
import jPasi.item.group.Group;
import jPasi.item.group.GroupMember;
import jPasi.item.group.GroupMemberDelegator;
import jPasi.item.group.OrderedGroup;
import jPasi.item.group.StandardGroup;
import util.MathTools;

/**
 * @author Jan Plate
 * 
 * Implements GroupMember by way of delegating the relevant functions to its nodeGroup.
 * The class has to implement GroupMember for the sake of encoding/decoding. 
 */
public class Contour extends DependentItem implements GroupMemberDelegator<StandardGroup<?>> {
    
    private static class CurveInfo {
		boolean raw = true;
		private CubicCurve curve;
		double phi1, phi2; // phi2 of first node and phi1 of second node
		double cpr1, cpr2; // analogously
		private Point2D p1, p2; // the two end points
				
		CurveInfo(CubicCurve c) {
			this.curve = c;
			p2 = (Point2D)curve.p2.clone();
			raw = true;
		}
		CurveInfo(double phi1, double cpr1, double phi2, double cpr2, Point2D p2) {
			this.phi1 = phi1;
			this.cpr1 = cpr1;
			this.phi2 = phi2;
			this.cpr2 = cpr2;
			this.p2 = p2;
			raw = true;
		}
		
		public String toString() {
			return "CurveInfo "+p1+" "+phi1+" "+cpr1+" "+phi2+" "+cpr2+" "+p2;
		}
		void update(CurveInfo pre) {
			if(!raw) return;
			this.p1 = (Point2D)pre.p2.clone(); 
			if(curve!=null) {		    	
		    	phi1 = deviation(MathTools.angle(p1, p2), MathTools.angle(p1, curve.p1a));
		    	phi2 = deviation(MathTools.angle(p2, p1), MathTools.angle(p2, curve.p2a));
		    	cpr1 = p1.distance(curve.p1a);
		    	cpr2 = p2.distance(curve.p2a);
			}
			raw = false;
		}
	}

    public class NodeGroup extends StandardGroup<CNode> implements OrderedGroup<CNode>, GroupMember<StandardGroup<?>> {
        
        public NodeGroup() {
            super();        
            members = Collections.synchronizedList(new ArrayList<CNode>());
        }
        
    	@SuppressWarnings("unchecked")
		public boolean add(GroupMember gm) {
    	    return this.add(members.size(), gm);	    
    	}
    	
        /**
         * @see jPasi.item.group.OrderedGroup#add(int, pasi.item.GroupMember)
         */
        public synchronized boolean add(int index, GroupMember<?> cn) {
        	return internalAdd(index, cn, true);
        }
        
        @SuppressWarnings("unchecked")
        public Object clone(Contour enclosing, Map<Object, Object> map) throws CloneNotSupportedException {
        	NodeGroup clone;
            if(map.values().contains(this)) {
                clone = this;
            } else {
                clone = (NodeGroup)map.get(this);
            }
            if(clone==null) {
        		clone = enclosing.new NodeGroup();
        		enclosing.nodeGroup = clone;
            	clone.getMembers().addAll(members);
                if(group!=null) {
            	    /*
            	     *  Our own clone is put into a clone of our own group if we are inGroup (i.e. active member); if not, it 
            	     *  is put into our own group.
            	     */
                    if(inGroup && group!=null) {
                        clone.group = (StandardGroup<NodeGroup>)group.clone(map);
                        clone.group.add(clone);
                        clone.group.members.remove(this);
                    } else {
                        group.add(clone);
                    }
                }
                clone.inGroup = inGroup;
        		map.put(this, clone);
        	}
        	
        	return clone;
        }
        
        protected Collection<CNode> cloneMembers() {
        	Collection<CNode> c = Collections.synchronizedList(new ArrayList<CNode>());
        	c.addAll(members);
            return c;
        }

        public CNode get(int index) {
            return ((List<CNode>)members).get(index);
        }

        public Contour getContour() {
            return Contour.this;
        }

        /**
         * @see jPasi.item.group.OrderedGroup#indexOf(T)
         */
        public int indexOf(CNode gm) {
            return ((List<CNode>)members).indexOf(gm);
        }

        protected synchronized boolean internalAdd(int index, GroupMember<?> m, boolean repaint) {
        	if(!(m instanceof CNode)) {
        		throw new IllegalArgumentException("Contour node groups only accept splinegon nodes as members.");
        	}
        	int i = ((List<CNode>)members).indexOf(m);
        	CNode cn = (CNode)m;
        	if(i<0) {
	        	cn.setGroup(this);
	        	cn.addDependentItem(Contour.this);
	        	((List<CNode>)members).add(index, (CNode)cn);
    	        invalidate();
	    	    if(repaint) {
    		    	adjustCenter();
    		    	repaint();
    		    	if(lX!=null) {
    		    		canvas.relocate(lX);
    		    	}
    		    	if(lY!=null) {
    		    		canvas.relocate(lY);
    		    	}
	    	    }
	        	return true;
    	    } else {
    	    	return false;
    	    }
        }

        void internalRemove(CNode m, boolean repaint) {
            super.remove(m);
            m.removeDependentItem(Contour.this);
    	    invalidate();
    	    if(repaint) {
	    	    center = computeCenter();
	    	    repaint();
    	    }
        }

        public Set<CNode> memberSet() {
            Set<CNode> result = new LinkedHashSet<CNode>();
            result.addAll(members);
            return result;
        }
        
        @SuppressWarnings("unchecked")
		public synchronized void remove(GroupMember m) {
            Contour.this.removeInvolute((CNode)m);
            internalRemove((CNode)m, true);
        }
    }
    
    private static final long serialVersionUID = 3098097351036152214L;    
    public static final float[] DEFAULT_DASH = null;
    
    private static final char[] CODE;
	private static final HashMap<Character, Integer> DECODE = new HashMap<Character, Integer>();
    private static final int MAX_FP_DIGITS = 3;
    private static char[] FP_CODE;
	public static BasicStroke auxStroke = new BasicStroke(.3f);
    private static final HashMap<Character, Integer> FP_DECODE = new HashMap<Character, Integer>();
	
    private static final Pattern FPP;
    
    static {
    	/*
    	 * Prepare the coder and decoder.
    	 */
        CODE = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ&#".toCharArray();
        for(int i = 0; i < CODE.length; i++) {
            DECODE.put(CODE[i], i);
        }
        String s = "+-.,=/!?"; 
        FP_CODE = s.toCharArray();
        for(int i = 0; i < FP_CODE.length; i++) {
        	FP_DECODE.put(FP_CODE[i], i);
        }
        FPP = Pattern.compile("([^"+s+"]*?)(["+s+"])");
    }    
    
    private static void adjustNode(CNode cn, CurveInfo c1, CurveInfo c2) {
		float r = CNode.DEFAULT_RADIUS;		
		cn.move((float)c1.p2.getX() - r, (float)c1.p2.getY() - r);
		cn.phi1 = c1.phi2;
		cn.phi2 = c2.phi1;
		cn.cpr1 = (float)c1.cpr2;
		cn.cpr2 = (float)c2.cpr1;
	}
    
	
	private static double deviation(double a0, double a1) {
		    double d = MathTools.d(a0, a1);
		    if(d>Math.PI) d = d - 2d*Math.PI;
		    return d;
	  }

	/**
	 * Reads cnodes from the first element of the info string.
	 */
	private static List<CNode> getNodes(String s) {
	    List<CNode> l = new ArrayList<CNode>();
        int m = CODE.length/2;
	    char[] c = s.toCharArray();
	    for(int i = 0; i<c.length; i++) {
	        int k = DECODE.get(c[i]);	 
	        for(int j = m; j>=2; j /= 2) {
	            CNode cn = new CNode();
	            if(k>=j) {
	                cn.omitLine = true;
	                k -= j;
	            }
	            j /= 2;
	            if(k>=j) {
	                cn.fixedCornerAngle = true;
	                k -= j;
	            }
	            l.add(cn);
	        }
	    }	    
	    return l;
	}


	private static List<CurveInfo> readCurves(String s, Map<Character, Integer> decode, int n, 
			Map<Character, Integer> fpDecode, Pattern fpp) throws ParseException {
		boolean end = false;
		//System.err.println("s: "+s);
		int[] index = new int[] {0};
		List<CurveInfo> result = new LinkedList<CurveInfo>();
		double[] d = Codec1.read(s, decode, n, fpDecode, fpp);
		float r = CNode.DEFAULT_RADIUS;
		for(int i = 0, l = d.length; i<l; i+=6) {
			CurveInfo c = new CurveInfo(d[i+0], d[i+1], d[i+2], d[i+3], 
							new Point2D.Float((float)d[i+4], (float)d[i+5]));
			result.add(c);
			//System.err.println(c);
		}
		return result;
	}

    private Point2D.Double center;

    private volatile GeneralPath[] drawnPaths;

	private NodeGroup nodeGroup; // private because Item#subclone wouldn't respect the fact that this has an enclosing instance.
    
    protected volatile boolean fixedAngles = false; 
    
    volatile double aX = 0;
    volatile double aY = Math.PI/2;
	EditorEntry xAxisInfo;
	EditorEntry yAxisInfo;
	volatile Axis lX;
	volatile Axis lY;
	
    public Contour() {
		super();
		nodeGroup = new NodeGroup();

		xAxisInfo = new EditorEntry("XAxis", FLOAT, "X-Axis");
		xAxisInfo.setSpinnerValues(Float.NaN, -180f, 180f, 5f, true);
		xAxisInfo.requestNotifier();
		xAxisInfo.setGlobal(true);

		yAxisInfo = new EditorEntry("YAxis", FLOAT, "Y-Axis");
		yAxisInfo.setSpinnerValues(Float.NaN, -180f, 180f, 5f, true);
		yAxisInfo.requestNotifier();
		yAxisInfo.setGlobal(true);
	}

    public Contour(Canvas c, List<CNode> nodes) {
        this();
        setCanvas(c);
        for(CNode cn: nodes) {            
            add(nodeGroup.getMembers().size(), cn);
            cn.setInGroup(true);
       }
    }

    public boolean acceptsAsGroup(Group<?> g) {
	    return true;
    }

    public boolean add(int index, CNode cn) {
    	return add(index, cn, true);
    }
    
    protected synchronized boolean add(int index, CNode cn, boolean repaint) {
    	return nodeGroup.internalAdd(index, cn, repaint);
    }

    /**
	 * Updates the axis indicated by the first parameter (0 for x-axis, 1 for y-axis).
	 * The third parameter indicates whether this should happen even if no previous line exists.
	 */
	void adjustAxis(int i, double angle) {
    	Axis l = i==0? lX: lY;
    	Point2D center = getCenter();
    	boolean changed = false;
		if(i==0) {
			if(aX!=angle || lX==null || !lX.point.equals(center)) {
				aX = angle;
				changed = true;
			}
		} else {
			if(aY!=angle || lY==null || !lY.point.equals(center)) {
				aY = angle;
				changed = true;
			}
		}
		boolean visible = l.isVisible();
		
        Canvas canvas = getCanvas();
    	if(changed) {
        	l.adjust(center, angle);
	        canvas.repaint();
    	}
    }
	
	void createAxis(int i, boolean visible) {
		Canvas canvas = getCanvas();
		Axis l = new Axis(canvas, getCenter(), i==0? aX: aY);
        l.setStroke(Contour.auxStroke);
        if(i==0) {
        	lX = l;
        } else {
        	lY = l;
        }
        canvas.add(l);
        l.setVisible(visible);
	    canvas.setComponentZOrder(l, 0);
	    if(visible) canvas.repaint();
	}

    /**
     * Adjusts the center and the two axes.
     * 
     * Not to be performed as often as repaint(), but rather only when one or more nodes have been manually moved, or
     * when the Contour has been added to the Canvas by cloning or loading. That way, we make sure that the shape does 
     * not change any further when the equalizer-buttons are pressed repeatedly.
     */
    public void adjustCenter() {
        center = computeCenter();
		if(lX!=null) adjustAxis(0, aX);
		if(lY!=null) adjustAxis(1, aY);
    }

	private void adjustNodes(List<CNode> nodes, StrokedShape[] s, String info) throws ParseException {
		List<CurveInfo> curves = null;
	    if(info!=null) {
	    	curves = readCurves(info, DECODE, CODE.length, FP_DECODE, FPP);
	    }
	    
	    // if(0<s.length) System.err.println("s[0]: "+s[0]);
	    
	    int ch = canvas.getHeight();
	    int offset = 0; 
	    boolean filled = s.length>0 && s[0].shape instanceof Path && ((Path)s[0].shape).isFilled();
	    
	    if(!filled && nodes.size()>0) {
	        /*
	         * Coordinate the node list with the list of drawn shapes, i.e., move the initial segment of connected 
	         * nodes to the end of the list. 
	         */		    
		    List<CNode> initialSegment = new LinkedList<CNode>();
		    boolean gap = false;
		    CNode cn0 = nodes.get(0);
		    boolean foundGap = cn0.isLineOmitted();
		    for(int i = 1, n = nodes.size(); !foundGap && i<=n; i++) {
		        CNode cn1 = nodes.get(i==n? 0: i);
		        if(!cn1.isLineOmitted()) {
			        if(!foundGap) {
			        	offset++;
			        	initialSegment.add(cn0);
			        } 
		        } else {
		        	foundGap = true;
		        }
		        cn0 = cn1;
		    }
		    if(foundGap) {
		        nodes.subList(0, offset).clear();
		        nodes.addAll(initialSegment);
		    } else {
		    	nodes = initialSegment;
		    }
	    }
	    
	    /*
	     * Set fields of the nodes in nodes1.
	     */
	    Iterator<CNode> i = nodes.iterator();
	    Iterator<CurveInfo> j = null;
	    if(curves!=null) {
	    	j = curves.iterator();
	    }
	    if(i.hasNext()) {
	        CNode cn0 = i.next();
		    CNode cn1 = cn0;
		    CNode cn2 = null, // will be cn0 at the end; its being identical with cn0 ends the loop.
		          cn3 = null; // the successor of cn0.		    
		    int si = 0; // index into s
		    Path p = null; // current Path, if applicable
		    int pi = 0; // index into p
		    CurveInfo c0 = null, // current CurveInfos; two consecutive ones are needed to determine one node.
		    		  c1 = null, // the info for the curve between cn1 and cn2.
		    		  c2 = null, // the info for curve that connects c0 with its successor.
		    		  c3 = null; // the info for the successor of c2.
		    while(i.hasNext() || cn2!=cn0) {
		        cn2 = i.hasNext()? i.next(): cn0; // cn0 because the last curve is assumed to go back to the beginning.

		        if(filled || !cn2.omitLine) { // get curve from code
	    			Texdraw.Shape sh = null;
		    		if(p!=null && pi<p.shapes.length) {
		    			sh = p.shapes[pi++];
		    		} else if(si<s.length) {
		    			Texdraw.Shape sh0 = s[si++].shape;
		    			if(sh0 instanceof Path) {
		    				p = (Path)sh0;
			    			pi = 0;
		    				sh = p.shapes[pi++];
		    			} else {
		    				sh = sh0;
		    			}
		    		} else {
		    			throw new ParseException(TEX, "More curves expected");
		    		}
	    			if(!(sh instanceof CubicCurve)) {
	    				throw new ParseException(TEX, "Curve expected");
	    			}
		    		c1 = new CurveInfo((CubicCurve)sh.transform(ch));
				} else {
					if(j==null) {
						throw new ParseException("No information about invisible nodes found");
					}
					c1 = j.next();
				}

		        if(c0!=null) {
		        	c1.update(c0);
		        	
		        	if(!c0.raw) {
			        	adjustNode(cn1, c0, c1);
		        	} else {
		        		cn3 = cn1;
		        		c2 = c0;
		        		c3 = c1;
		        	}
		        }
				
	            cn1 = cn2;
				c0 = c1;			
		    }
		    
	        c2.update(c1);
		    adjustNode(cn2, c1, c2);
		    
		    if(cn2!=cn3) {
		    	c2.update(c1);
		    	adjustNode(cn3, c2, c3);
		    }
	    }
	}
    
    private double adjustToFlip(double a, AffineTransform t) {
		double a1;
    	if(t.getScaleX() < 0) {
    		a1 = Math.PI - MathTools.normalize(a, Math.PI/2);
		} else {
			a1 = 2*(a>-Math.PI/2 && a<=Math.PI/2? 0: Math.PI) - a;
		}
		return MathTools.normalize(a1, 0);
    }
    
    public void adjustToTransform(AffineTransform t, int modifier) {
    	//System.err.println("contour: "+this+" t: "+t);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0) {    		
    		double factor = t.getScaleX();
    		if(TransformModifier.isScalingLines(modifier)) {
    			setLineWidth((float)factor*getLineWidth());
    			setAltLineWidth((float)factor*getAltLineWidth());
    		}
    	}

    	if((t.getType() & AffineTransform.TYPE_GENERAL_ROTATION) > 0) {    		
            aX = MathTools.normalize(aX + MathTools.getRotation(t), 0);
            aY = MathTools.normalize(aY + MathTools.getRotation(t), 0);
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0) {
    		aX = adjustToFlip(aX, t);
    		aY = adjustToFlip(aY, t);
    		
			flexDirection = flexDirection==COUNTERCLOCKWISE? CLOCKWISE: COUNTERCLOCKWISE;
    	}
    	if((t.getType() & (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_FLIP)) > 0) {    		
    		if(xAxisInfo!=null && yAxisInfo!=null) {
    			for(CNode cn: nodeGroup.members) { 
    				/* 
    				 * A bit of a hack: try every SNode as a target. Result of the fact that the SNodes
    				 * are in charge of the EditorPane.
    				 */
    				xAxisInfo.notify(cn);
    				yAxisInfo.notify(cn);
    			}
    		}
    	}
		adjustCenter();
    }

    /**
	 * Performs clean-up after cloning, before adding to the Canvas. In particular, drops or replaces with clones 
	 * all nodes that don't have this as their Contour. 
	 */
	@SuppressWarnings("unchecked")
    public void cleanUp(Map<Object, Object> cloneMap) {
		Iterator i = nodeGroup.cloneMembers().iterator();
		Canvas canvas = getCanvas();
		List<CNode> toRemove = new LinkedList<CNode>();
		if(i.hasNext()) {
	        CNode[] cn0 = new CNode[2];
	        cn0[0] = (CNode)i.next();
		    CNode cn1 = cn0[0];
		    cn0[1] = i.hasNext()? (CNode)i.next(): cn0[0];
		    CNode cn2 = cn0[1];
		    CNode cn3 = null;
            boolean b1 = cn1.getContour()==this;
            boolean b2 = cn2.getContour()==this;
            int j = 0;
		    while(i.hasNext() || cn1!=cn0[0] || cn3==null) {
		        cn3 = i.hasNext()? (CNode)i.next(): cn0[j++];
	            boolean b3 = cn3.getContour()==this;
	            
	            if(!b2) {
	            	if(b1 || b3) { // clone
	            		CNode cnc = null;
	            		try {
	                        cnc = (CNode)cn2.clone(cloneMap);	                        
                        } catch (CloneNotSupportedException e) {
	                        throw new RuntimeException(e);
                        }
                        if(cnc!=null) {
		            		if(!b1) {
		            			cnc.omitLine = true;
		            		}
		            		canvas.addItem(cnc, true);
                        }
	            	} else { // drop
	            		toRemove.add(cn2);
	            	}
	            } 
	            
	            cn1 = cn2;
	            cn2 = cn3;	            
	            b1 = b2;
	            b2 = b3;
		    }
	    }
		for(CNode cn: toRemove) {
			nodeGroup.internalRemove(cn, false);
		}
		adjustCenter();
	}
    
	@SuppressWarnings("unchecked")
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
    	Contour clone = (Contour)super.clone(map);       
        NodeGroup ngc = nodeGroup;
        if(clone!=this) {
        	ngc = (NodeGroup)nodeGroup.clone(clone, map);
        	clone.nodeGroup = ngc;
        }
        
        List<CNode> members = (List<CNode>)ngc.getMembers();
        synchronized(members) {
        	List members1 = (List)ngc.cloneMembers();
	        for(int i = 0, n = members1.size(); i<n; i++) {	
	        	CNode cn = (CNode)members1.get(i);
	        	CNode cnc = (CNode)map.get(cn);
	        	if(cnc!=null) {
	        		members.remove(i);
	        		members.add(i, cnc);
	        		cnc.setGroup(ngc);
	        	}
	        }
        }
        
        return clone;
    }

    protected Point2D.Double computeCenter() {
        return computeMassCenter();
    }

    protected Point2D.Double computeMassCenter() {
        double xSum = 0;
        double ySum = 0;
        int n = nodeGroup.getMembers().size();
        for(Iterator<?> i = nodeGroup.iterator(); i.hasNext();) {
            CNode cn  = (CNode)i.next();
            Point2D.Float c = (Point2D.Float)cn.getCenter();
            xSum += c.x;
            ySum += c.y;
        }
        return new Point2D.Double(xSum/n, ySum/n);
    }

    protected Point2D.Double computeNodeBoundsCenter() {
        double xmax = Float.NEGATIVE_INFINITY;
        double xmin = Float.POSITIVE_INFINITY;
        double ymax = Float.NEGATIVE_INFINITY;
        double ymin = Float.POSITIVE_INFINITY;
        int n = nodeGroup.getMembers().size();
        for(Iterator<CNode> i = nodeGroup.iterator(); i.hasNext();) {
            CNode cn  = i.next();
            Point2D.Float c = (Point2D.Float)cn.getCenter();
            if(c.x > xmax) xmax = c.x;
            if(c.y > ymax) ymax = c.y;
            if(c.x < xmin) xmin = c.x;
            if(c.y < ymin) ymin = c.y;
        }
        return new Point2D.Double((xmin+xmax)/2, (ymin+ymax)/2);
    }

	protected Shape computeShape() {
	    for(Iterator<CNode> i = nodeGroup.iterator(); i.hasNext();) {
	        CNode cn = i.next();
	        cn.updateAngles();
	    }
	    
		GeneralPath outline = new GeneralPath();
		List<GeneralPath> list = new LinkedList<GeneralPath>();
	    Iterator<CNode> i = nodeGroup.iterator();
	    if(i.hasNext()) {
	        GeneralPath path = new GeneralPath();
	        boolean pathEmpty = true;
	        CNode cn0 = (CNode)i.next();
		    CNode cn1 = cn0;
		    CNode cn2 = null;
		    while(i.hasNext() || cn2!=cn0) {
		        cn2 = i.hasNext()? (CNode)i.next(): cn0;
	            CubicCurve2D c = getLine(cn1, cn2);
		        outline.append(c, true);

	            if(!cn2.isLineOmitted()) {
    	            path.append(c, true);
    	            pathEmpty = false;
	            } else {
	                list.add(path);
	                path = new GeneralPath();
	                pathEmpty = true;
	            }
		        
	            cn1 = cn2;
		    }
    	    if(!pathEmpty && list.size()>1) {
    	        GeneralPath initialSegment = list.get(0);
    	        path.append(initialSegment, true);
    	        list.remove(initialSegment);
    	    }
	        list.add(path);
	    }

	    drawnPaths = list.toArray(new GeneralPath[] {});
	    
	    return outline;
	}

    public boolean contains(CNode cn) {
		return nodeGroup.getMembers().contains(cn);
	}
    
    public boolean containsSome(Set<?> s) {
		boolean result = s!=null && !Collections.disjoint(nodeGroup.getMembers(), s);
		return result;
	}
           
	public void createAxes(boolean visible) {		
		createAxis(0, visible);
		createAxis(1, visible);
    }

	public void createNode(int index) {
        
        /*
         * Find the location at which the new node should be placed.
         */
        CNode cn1 = (CNode)nodeGroup.get(index>=nodeGroup.size()? 0: index);
        CNode cn0 = cn1.getPrevious();
        CubicCurve2D.Float c = (CubicCurve2D.Float)getLine(cn0, cn1);
        CubicCurve2D.Float c1 = new CubicCurve2D.Float();
        CubicCurve2D.Float c2 = new CubicCurve2D.Float();
        c.subdivide(c1, c2);        
        Point2D.Float loc = (Point2D.Float)c2.getP1();
        
        CNode cn = new CNode(getCanvas(), loc);
        
        add(index, cn);
        cn.setInGroup(true);
        cn.setValues(c1, c2, true);
        cn0.setValues(null, c1, true);
        cn1.setValues(c2, null, true);

        shapeChanged();

        canvas.addItem(cn, true);
        canvas.select(cn, -1, false);
    }

	public synchronized strictfp void equalizeAngularIntervals(CNode ref) {
	    CNode[] nodes = nodeGroup.getMembers().toArray(new CNode[0]);
	    double incr = 2*Math.PI/nodes.length;
	    
	    int refIndex = 0;
	    boolean started = false;
	    boolean beenThere = false;
	    double angle = 0;
	    for(int i = 0; i<nodes.length && !(i>=refIndex && beenThere); i++) {
	        if(!started) {
	            if(nodes[i]==ref) {
	                started = true;
	                refIndex = i;
	                angle = MathTools.angle(center, ref.getCenter());
	            }
	        } else {
	            angle += incr;
	            Point2D c = nodes[i].getCenter();
	            double d = c.distance(center);
	            double r = nodes[i].getRadius();
	            Point2D c1 = new Point2D.Double(
	                   center.x + Math.cos(angle)*d - r, 
	                   center.y - Math.sin(angle)*d - r);
	            nodes[i].setLocation2D(c1);
	    	    getCanvas().relocate(nodes[i]);
	        }
            if(started && i==nodes.length-1) {
	            i = -1;
	            beenThere = true;
	        }
	    }
	    
	    invalidate();
	    repaint();
    }

	public synchronized strictfp void equalizeCenterDistances(CNode ref) {
	    CNode[] nodes = nodeGroup.getMembers().toArray(new CNode[0]);
	    
	    int refIndex = 0;
	    boolean started = false;
	    boolean beenThere = false;
	    double d = 0;
	    for(int i = 0; i<nodes.length && !(i>=refIndex && beenThere); i++) {
	        if(!started) {
	            if(nodes[i]==ref) {
	                started = true;
	                refIndex = i;
	                d = center.distance(ref.getCenter());
	            }
	        } else {
	            Point2D c = nodes[i].getCenter();
	            double r = nodes[i].getRadius();
	            double angle = MathTools.angle(center, c);
	            Point2D c1 = new Point2D.Double(
	                    center.x + Math.cos(angle)*d - r, 
	                    center.y - Math.sin(angle)*d - r);
	            nodes[i].setLocation2D(c1);
	    	    getCanvas().relocate(nodes[i]);
	        }
            if(started && i==nodes.length-1) {
	            i = -1;
	            beenThere = true;
	        }
	    }
	    invalidate();
	    repaint();
    }
    
	/**
     * @see jPasi.item.DependentItem#getAngle(int)
     */
    public double getAngle(int k) {
        return 0;
    }
	
	
	public Point2D getCenter() {
        if(center==null) {
            center = computeCenter();
        }
        return center;
    }
	
	public StandardGroup<?> getGroup() {
	    return nodeGroup.getGroup();
    }
		
	@SuppressWarnings("unchecked")
    public String getInfoString() {
	    StringBuilder sb0 = new StringBuilder();
	    
	    sb0.append(Codec1.encodeFloat(zValue)).append(" ");
	    sb0.append(Codec1.encodeFloat((float)Math.toDegrees(aX))).append(" ");
	    sb0.append(Codec1.encodeFloat((float)Math.toDegrees(aY))).append(" ");
	    
	    sb0.append(fixedAngles? "+": "-").append(" ");
	    
	    /*
	     * Append information about the nodes' connectedness and the fixedness of their corner angles. 
	     */
	    int m = CODE.length/2;
	    int j = m;
	    int k = 0;
	    int unused = 0;
	    for(Iterator<CNode> i = nodeGroup.iterator(); i.hasNext(); j /= 2) {
	        CNode cn = i.next();
	        k += cn.omitLine? j: 0;
	        j /= 2;
	        k += cn.fixedCornerAngle? j: 0;	        
	        if(!i.hasNext() || j<4) {
		        if(!i.hasNext()) {
		        	unused = (int)(Math.log(j)/Math.log(2));
		        }
	        	sb0.append(CODE[k]);
		        k = 0;
		        j = 2*m;
	        }
	    }
	    sb0.append(unused/2); // divided by 2 because there are two bits per node.
	    
	    /*
	     * Append the geom. information for those nodes that are not connected by lines to others and so do not come up
	     * in the texdraw code, unless the Contour is 'filled'.
	     */
	    String s1 = null;
	    boolean noLines = true;
	    if(!isFilled()) {
	    	int precision = MathTools.alternatePrecision(Codec1.PRECISION, CODE.length);
		    StringBuilder sb1 = new StringBuilder();		    
		    /*
		     * Append information.
		     */
		    List<CNode> nodes = (List<CNode>)nodeGroup.members;
		    for(int i = 0, n = nodes.size(); i<n; i++) {
		    	CNode cn0 = nodes.get(i);
		    	CNode cn1 = nodes.get(i+1<n? i+1: 0);
	            CubicCurve2D c = getLine(cn0, cn1);
	            
	            if(cn1.omitLine) {
                    String[] s = new String[6];
                    Point2D p = cn1.getCenter();
                    s[0] = Codec1.encode(cn0.phi2, true, CODE, FP_CODE, precision);
                    s[1] = Codec1.encode(cn0.cpr2, true, CODE, FP_CODE, precision);
                    s[2] = Codec1.encode(cn1.phi1, true, CODE, FP_CODE, precision);
                    s[3] = Codec1.encode(cn1.cpr1, true, CODE, FP_CODE, precision);
                    s[4] = Codec1.encode(p.getX(), true, CODE, FP_CODE, precision);
                    s[5] = Codec1.encode(p.getY(), true, CODE, FP_CODE, precision);

                    for(String sx: s) {
                    	sb1.append(sx);
                    }
	            } else {
	            	noLines = false;
	            }
		    }
		    s1 = sb1.toString();
	    }	    
	    if(s1!=null && s1.length()>0) {
	    	sb0.append(" ").append(s1);
	    } else {
	    	sb0.append(" ").append("-");
	    }
	    
	    /*
	     * Append line information if there is no texdraw code to contain it.
	     */
	    String s2 = null;
	    if(noLines && shading==1) {
	    	StringBuilder sb2 = new StringBuilder();
	    	sb2.append(Codec1.encodeFloat(stroke.getLineWidth()));
	    	
	    	float[] dash = stroke.getDashArray();
	    	if(dash!=null) {
	    		for(float f: dash) {
			    	sb2.append(","+Codec1.encodeFloat(f));
		    	}
	    	}
		    s2 = sb2.toString();
	    }
	    if(s2!=null && s2.length()>0) {
	    	sb0.append(" ").append(s2);
	    }
	    
	    return sb0.toString();
	}
	/*
	private static String toString(double[] a) {
		StringBuilder sb = new StringBuilder();
		for(double d: a) {
			sb.append(d+" ");
		}
		return sb.toString();
	}
	*/
	
	public synchronized IndependentItem<?>[] getInvolutes() {	    
		return nodeGroup!=null? 
				nodeGroup.getMembers().toArray(new CNode[0]):
		    	null;
	}
	
	/**
     * @see jPasi.item.DependentItem#getItemIndex()
     */
    public int getItemIndex() {
        return -1;
    }

	/**
     * @see jPasi.item.DependentItem#getItemLocation()
     */
    public Point2D getItemLocation() {
        return null;
    }
	
	public CubicCurve2D getLine(CNode cn1, CNode cn2) {
        Point2D.Float p1 = (Point2D.Float)cn1.getCenter(), 
		  			  p2 = (Point2D.Float)cn2.getCenter();
		double a1 = MathTools.angle(p1, p2) + cn1.phi2;
		double a2 = MathTools.angle(p2, p1) + cn2.phi1;
		CubicCurve2D c = new CubicCurve2D.Float(
		  Math.round(p1.x), Math.round(p1.y),
		  (float)(p1.x + Math.cos(a1)*cn1.cpr2), (float)(p1.y - Math.sin(a1)*cn1.cpr2),
		  (float)(p2.x + Math.cos(a2)*cn2.cpr1), (float)(p2.y - Math.sin(a2)*cn2.cpr1),
		  Math.round(p2.x), Math.round(p2.y)
		);	
	    return c;
	}
	
	public NodeGroup getNodeGroup() {
		return nodeGroup;
	}
	
	/**
     * @see jPasi.item.DependentItem#getPreferredAngle(int)
     */
    public double getPreferredAngle(int k) {
        return 0;
    }
	
	public String getTexdrawCode(int canvasHeight) {
		List<CubicCurve2D> drawn = new LinkedList<CubicCurve2D>();
		List<CubicCurve2D> filled = new LinkedList<CubicCurve2D>();
	    Iterator<?> i = nodeGroup.iterator();
	    boolean foundGap = false;
	    if(i.hasNext()) {
			List<CubicCurve2D> initialSegment = new LinkedList<CubicCurve2D>();
	        CNode cn0 = (CNode)i.next();
		    CNode cn1 = cn0;
		    CNode cn2 = null;
		    foundGap = cn1.isLineOmitted();
		    while(i.hasNext() || cn2!=cn0) {
		        cn2 = i.hasNext()? (CNode)i.next(): cn0;
	            CubicCurve2D c = getLine(cn1, cn2);
	            
	            if(!cn2.isLineOmitted()) {
	               if(!foundGap) {
	                   initialSegment.add(c);
	               } else {
		               drawn.add(c);
	               }
	            } else {
	                foundGap = true;
	            }
	            filled.add(c);
		        
	            cn1 = cn2;
		    }
		    if(foundGap) {
	            drawn.addAll(initialSegment);
		    } else {
		        drawn = initialSegment;
		    }
	    }
	    Shape[] s = new Shape[0];
	    return getCode(drawn.toArray(s), filled.toArray(s), foundGap, stroke, canvasHeight); 
	}
	
   	public Axis getXAxis() {
	    return lX;
    }
	
	public Axis getYAxis() {
	    return lY;
    }    

	public GroupMember<StandardGroup<?>> groupMemberDelegate() {
	    return nodeGroup;
    }

	public boolean isFilled() {
	    return shading<1;
	}

    public Boolean isFixedAngles() {
	    return fixedAngles;
    }

	public boolean isInGroup() {
	    return nodeGroup.isInGroup();
    }

	public void moveBy(float dx, float dy) {
		super.moveBy(dx, dy);
		adjustCenter();
	}

	public strictfp void moveNodes(Set<CNode> nodes, float dx, float dy) {
		
		if(fixedAngles) {
			/*
			 * determine the unit vectors in S' (S': the system obtained by rotating the axes by aX and aY, respectively).
			 */
			double vxx = Math.cos(aX);
			double vxy = -Math.sin(aX);
			double vyx = -Math.cos(aY);
			double vyy = Math.sin(aY);
			
			/*
			 * determine the linear combination of these vectors that yields the mouse movement.
			 */
			double[] v = new double[2];
			MathTools.solve(vxx, vxy, vyx, vyy, dx, dy, v);			
			double dxx = v[0]*vxx;
			double dxy = v[0]*vxy;
			double dyx = v[1]*vyx;
			double dyy = v[1]*vyy;
			
			Set<CNode> setX = new HashSet<CNode>();
			Set<CNode> setY = new HashSet<CNode>();
			for(CNode cn: nodes) {
				cn.moveBy(v[0], v[1], dxx, dxy, dyx, dyy, this, setX, setY);
			}

			Canvas canvas = getCanvas();
			canvas.markForRelocation(setX);
			canvas.markForRelocation(setY);
		} else {		
			for(CNode cn: nodes) {
				cn.moveBy(dx, dy);
			}
		}				
		invalidate();
		adjustCenter();
		repaint();
	}

	public Set<CNode> nodeSet() {
        return nodeGroup.memberSet();
    }

	public synchronized void paint(Graphics g) {
		//System.err.println("paint: "+this+" z: "+(canvas==null? "?": ""+canvas.getComponentZOrder(this)));
		Graphics2D g2 = (Graphics2D)g;
		Rectangle r = getBounds(); // these will be the bounds in the parent's coordinate system
		
		g2.translate(-r.x, -r.y);
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
	    g2.setStroke(stroke); 
	
	    Shape s = getShape();    	    
	    if(shading<1) {
            g2.setPaint(shadePaint);
            g2.fill(s);
	    }
	    
        g2.setPaint(getForeground());    	    
        for(int i = 0; i<drawnPaths.length; i++) {
            g2.draw(drawnPaths[i]);
        }
        /*            
        if(spokes!=null) {
            g2.setPaint(Color.red);
            for(Iterator i = spokes.iterator(); i.hasNext();) {
                g2.draw((Line2D)i.next());
            }
        }
        */
    }
	
	public void parse(String code, String info) throws ParseException {
		StrokedShape[] s = Texdraw.getStrokedShapes(code, DEFAULT_LINEWIDTH);
		
		int ch = canvas.getHeight();
	    String[] sp;
	    if(info!=null) {
	    	sp = info.split("\\s+");
	    } else {
	    	throw new ParseException(HINT, "Missing or incomplete info string");
	    }
	    
	    if(sp.length<5) {
	    	throw new ParseException(HINT, "Incomplete info string");
	    } 
	    
	    zValue = Codec1.decodeFloat(sp[0]);
	    aX = Math.toRadians(Codec1.decodeFloat(sp[1]));
	    aY = Math.toRadians(Codec1.decodeFloat(sp[2]));
	    
	    fixedAngles = sp[3].equals("+");
	    if(fixedAngles) {
	    	createAxes(false);
	    }
	    
	    if(s.length>0) {
	    	stroke = newStroke(stroke, s[0].stroke.lineWidth, s[0].stroke.pattern);
	    	boolean filled = s[0].shape instanceof Path && ((Path)s[0].shape).isFilled();
	    	if(filled) {
	    		setShading(((Path)s[0].shape).fillLevel, false);
	    	}
	    } else {
	    	if(sp.length<7) {
	    		throw new ParseException(HINT, "Line information missing");
	    	}
	    	/*
	    	 * Decode line information.
	    	 */
	    	String[] spp = sp[6].split(",");	    	
	    	float lw = Codec1.decodeFloat(spp[0]);
	    	List<Float> l = new ArrayList<Float>();
	    	for(int i = 1; i<spp.length; i++) {
	    		l.add(Codec1.decodeFloat(spp[i]));
	    	}
	    	float[] dash = new float[l.size()];
	    	for(int i = 0, n = dash.length; i<n; i++) {
	    		dash[i] = l.get(i);
	    	}
	    	stroke = newStroke(stroke, lw, dash.length==0? null: dash);
	    }
	    
	    /*
	     * The last char of sp[4] is a number indicating how many 'node-slots' of the previous char have not been used.
	     */
	    int spl = sp[4].length();
	    int unused = Integer.parseInt(String.valueOf(sp[4].charAt(spl-1)));
	    List<CNode> nodes = getNodes(sp[4].substring(0, spl-1));
	    int n = nodes.size();
	    nodes.subList(n - unused, n).clear();

	    /*
	     * Set the geometry-fields of the nodes.
	     */
	    adjustNodes(nodes, s, sp.length>5 && !sp[5].equals("-")? sp[5]: null);
	    
	    /*
	     * Add the configured nodes to the nodeGroup.
	     */
	    for(CNode cn: nodes) {
            add(nodeGroup.getMembers().size(), cn, false);
	    }
	    
	    /*
	     * Initialize the nodes.
	     */
	    for(CNode cn: nodes) {
            cn.setCanvas(canvas);
            cn.setInGroup(true);
	    	cn.init();
	    }
	}

	public void removeAxes() {
		if(lX!=null || lY!=null) {
			removeAxis(lX);
			removeAxis(lY);
			lX = null;
			lY = null;
			getCanvas().repaint();
		}
	}

	private void removeAxis(Axis axis) {
    	if(axis!=null) { 
    		Container parent = axis.getParent(); // either Canvas or JLayeredPane
    		if(parent!=null && !(parent instanceof Canvas)) {
        		parent.remove(axis);
        	}
        	canvas.removeItem(axis); // this will also take care of Canvas#toRelocate.
        }
    }

	/**
     * @see jPasi.item.DependentItem#removeInvolute(pasi.item.Entity)
     */
    public void removeInvolute(Item i2) {
       nodeGroup.internalRemove((CNode)i2, true);
       if(nodeGroup.isDefunct()) {
           removeAxes();
           getCanvas().removeItem(this);
       }
    }

	/**
     * @see jPasi.item.DependentItem#setAngle(double, int)
     */
    public void setAngle(double a, int k) {
    }


	public void setDash(String s) {
        stroke = changeDashArray(stroke, s, DEFAULT_DASH);
        repaint();
    }


	public void setFixedAngles(boolean dragToResize) {
    	this.fixedAngles = dragToResize;
    }

	public void setGroup(Group<?> g) {
		nodeGroup.setGroup(g);
    }


	public void setInGroup(boolean b) {
		nodeGroup.setInGroup(b);
	}

	/**
     * @see jPasi.item.DependentItem#setItemLocation(java.awt.geom.Point2D)
     */
    public Point2D setItemLocation(Point2D p) {
        return null;
    }

	public void setShading(float s) {
        setShading(s, true);
    }

	public void setShading(float s, boolean repaint) {
        shading = s;
        Color c = new Color(shading, shading, shading);
        shadePaint = c;
        if(repaint) repaint();
    }
	
	public boolean shouldBeHidden() {
	    return false;
	}
	
	public void setAxesVisible(boolean b) {
    	if(lX!=null) {
    		lX.setVisible(b);
    		if(b)canvas.relocate(lX);
    	}
    	if(lY!=null) {
    		lY.setVisible(b);
    		if(b)canvas.relocate(lY);
    	}
	}

}
