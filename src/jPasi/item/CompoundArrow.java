/*
 * Created on 27.01.2007
 *
 */
package jPasi.item;

import static jPasi.edit.EditorEntry.Type.ACTION;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jPasi.Canvas.CompoundArrowDissolve;
import jPasi.codec.DIInfo;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.StrokedShape;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class CompoundArrow extends BinaryRelationship {

    protected static class GeomInfo implements util.Cloneable {
        float gap0, gap1;
        float w0, w1, wc;
        
        GeomInfo(float g0, float g1, float w0, float w1, float wc) {
            this.gap0 = g0;
            this.gap1 = g1;
            this.w0 = w0;
            this.w1 = w1;
            this.wc = wc;
        }
        
        public Object clone() {
            Object oc = null;
            try {
                oc = super.clone();
            } catch(CloneNotSupportedException cnse) {
                cnse.printStackTrace(System.err);
            }
            return oc;
        }
        
        public String toString() {
            return "GeomInfo "+gap0+" "+gap1+" "+w0+" "+w1+" "+wc;
        }
    }

    private static final long serialVersionUID = 6315666023982050017L;
   
    protected boolean arrowHeadNeedsLine;
    
    private CompoundArrowDissolve end; 
    
    protected GeomInfo[] geomInfo;
    protected Connector[] elements;
    protected boolean[] co;
    
    protected boolean specialLine;
    protected boolean symmetric;

	private float defaultW0;
	private float defaultW1;
	private float defaultWC;
    
    public CompoundArrow() {}
    
    /**
     * 
     * @param e0: first ENode
     * @param e1: second Enode
     * @param c: the array of Connectors that are to be combined into this CompoundArrow
     * @param initElements: indicates whether those Connectors should be initialized. 
     *  When invoking this Constructor from an implementation of pasi.codec.Codec (i.e., while parsing), the value to be used for this parameter is Boolean.FALSE.
     * @param end: the Runnable that should be run if this CompoundArrow is to be dissolved
     */
    public CompoundArrow(ENode e0, ENode e1, Connector[] c, boolean initElements, CompoundArrowDissolve end) {
        super(e0, e1);
        this.elements = c;
        this.end = end;
        geomInfo = new GeomInfo[c.length];
        co = new boolean[c.length];
        setBasicParameters();
        if(initElements) { // when NOT parsing
    	    if(elements.length>=0) { // We take the stroke parameters of the first element with a 'special line', or (if there is no such element) of the first element, as a default.
    	    		// This should not be done while parsing, because the CompoundArrow may have its own stroke properties independently of those of its elements.
    		    Connector guide = elements[0];
    		    int i = 0;
    	        for(; i<elements.length && !elements[i].hasSpecialLine(); i++);
    	        if (i<elements.length) {
    	        	guide = elements[i];
    	        }	    
    	    	super.setLineWidth(guide.getLineWidth());
    	    	super.setDash(guide.getDash());
    	    }

        	setLineParameters(false);
        	initElements();
        }
    }
	
    /**
	 * Set those properties that can be inferred from the elements' properties without any parsing of the tex-code.
	 * This need not be recursive, since the CompoundArrow is built from the bottom up.
	 */
	private void setBasicParameters() {
		boolean sym = false;
		int dir = 0; 
		for(int i = 0; i<elements.length; i++) {
			Connector c = elements[i];
    	    ENode[] e = (ENode[])c.getInvolutes();
    	    
    	    co[i] = e[0]==this.involutes[0];
    	    
    	    if(!c.isSymmetric()) {
    	    	if(dir==0) {
    	    		dir = co[i]? 1: -1;
    	    	} else if(dir != (co[i]? 1: -1)) {
    	    		sym = true;
    	    	}
    	    }    	    
	    }
		this.symmetric = sym || dir==0; 
	    
	    for(int i = 0; i<elements.length && !specialLine; i++) {
    	    if(elements[i].hasSpecialLine()) {
    	        specialLine = true;
    	    }
	    }
	    
	}

    /**
     * Determines gaps, and the w and defaultW parameters on the basis of the elements. When parsing, this requires 
     * parseArrowhead() to have been called first (which means that all the elements' parseArrowhead() methods have also been 
     * called first).
     */
	private void setLineParameters(boolean recursive) {
		if(recursive) {
			for(Connector c: elements) {
				if(c instanceof CompoundArrow) {
					((CompoundArrow)c).setLineParameters(true);
				}
			}
		}
	    
		float g0x = Float.NEGATIVE_INFINITY;
        float g1x = Float.NEGATIVE_INFINITY;
        float g0min = Float.NaN;
        float g1min = Float.NaN;
        float w0x = 0, w1x = 0, wcx = 0;
        for(int i = 0; i<elements.length; i++) {

            float g0 = elements[i].gap0;
            float g1 = elements[i].gap1;            
            //System.err.println("r: "+elements[i]+" cd: "+co[i]);
            float w0 = elements[i].w0, w1 = elements[i].w1, wc = elements[i].wc;            
            
            if(co[i]) { 
                if(g0>g0x) g0x = g0;
                if(g1>g1x) g1x = g1;
                if(w0>w0x) w0x = w0;
                if(w1>w1x) w1x = w1;
            }
            else {
                if(g0>g1x) g1x = g0;
                if(g1>g0x) g0x = g1;
                if(w0>w1x) w1x = w0;
                if(w1>w0x) w0x = w1;
            }
            if(wc>wcx) wcx = wc;
        }	    
	    this.gap0 = Float.isNaN(g0min)? g0x: Math.min(g0x, g0min);
	    this.gap1 = Float.isNaN(g1min)? g1x: Math.min(g1x, g1min);
	    this.gap = this.gap1;
	    w0x += g0x; // the same is not necessary for w1x since it is already taken care of by the individual elements.
	    
	    this.defaultW0 = w0x;
	    this.defaultW1 = w1x;
	    this.defaultWC = wcx;
	    this.w0 = w0x;
	    this.w1 = w1x;
	    this.wc = wcx;
	    
	}	
	
	/**
     *  Adjusts the geometry of the elements, recording the elements' current geometry in GeomInfo objects.
     *  This method must therefore be called only once per element, each time it is incorporated into a CompoundArrow.
     */
	private void initElements() {
	    for(int i = 0, n = elements.length; i<n; i++) {
	        Connector r = elements[i];
	        ENode[] e = (ENode[])r.getInvolutes();	        
	        geomInfo[i] = new GeomInfo(r.gap0, r.gap1, r.w0, r.w1, r.wc);
	        
	        if(co[i]) {
	            r.w0 = w0;
	            r.w1 = w1;
                if(specialLine && elements[i].hasSpecialLine()) {
	                r.gap0 = gap0;
	                r.gap1 = gap1;
                }
            }
            else {
	            r.w0 = w1;
	            r.w1 = w0;
        	    if(specialLine && elements[i].hasSpecialLine()) {
	                r.gap0 = gap1;
	                r.gap1 = gap0;
                }
            }
            r.wc = wc;          
            
            r.manualCPR = true;
            r.manualBaseAngle = false;            
            if(r.canResetPriority()) {
                r.resetPriority();
            }
	    }
	    setRigidPoint(gap1);
    }
	


	/**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
		for(Connector c: elements) {
			c.adjustToTransform(t, modifier);
		}
    }
	
	public Object clone(Map<Object, Object> map, boolean add) throws CloneNotSupportedException {
        CompoundArrow clone = (CompoundArrow)super.clone(map, add);
        Connector[] ec = (Connector[])clone.elements;
        ENode[] inv = (ENode[])clone.getInvolutes();
        //System.err.println("clone: "+clone+" i0: "+inv[0]+" i1: "+inv[1]);
        
        Map<Object, Object> map1 = new HashMap<Object, Object>(); // use empty map to keep element-clones hidden from 
        		// canvas.copyPosition(), which would otherwise add them to the canvas
        
        for(int i = 0; i<ec.length; i++) {
            if(clone!=this) {
	            ec[i] = (Connector)ec[i].clone(map1, false);
            }            
        }
        for(int i = 0; i<ec.length; i++) {
            for(int j = 0; j<2; j++) {
  	            ec[i].setInvolute(co[i]? j: 1-j, inv[j], false);
            }
        }
        if(clone!=this) {
	        Object[] gi1 = clone.geomInfo;
	        for(int i = 0; i<gi1.length; i++) {
                gi1[i] = gi1[i]!=null? ((util.Cloneable)gi1[i]).clone(): null; // will be null after parsing
            }
            clone.end = getCanvas().new CompoundArrowDissolve(
                    Arrays.asList(clone.elements), end.setPriority, true);
        }
        return clone;
    }    
    
    public Shape[] computeArrowhead() {
        updateElements();
        ArrayList<Shape> list = new ArrayList<Shape>();
        for(int i = 0; i<elements.length; i++) {
            Shape[] s = elements[i].getArrowHead(); 
            if(s!=null) {
                for(int j = 0; j<s.length; j++) {
            	    list.add(s[j]);
                }
            }
        }
        return list.toArray(new Shape[]{});
    }
    
    protected void cprChanged() {
	    super.cprChanged();
	    updateElements();
	    for(int i = 0; i<elements.length; i++) {
	        elements[i].manualCPR = true;
	    }
	    repaint();
	    adjustItemLocation();
	}

    public void doDissolve() {
	    removeInvolute(involutes[0]);
	    removeInvolute(involutes[1]);
	    for(int i = 0; i<elements.length; i++) {
	        GeomInfo gi = geomInfo[i];
	        Connector r = elements[i];
	        if(gi!=null) { // this may happen when parsing -- the information loss seems acceptable.
		        r.gap0 = gi.gap0;
		        r.gap1 = gi.gap1;
		        r.w0 = gi.w0;
		        r.w1 = gi.w1;
		        r.wc = gi.wc;
	        } else {
	        	r.w0 = r.getDefaultW0();
	        	r.w1 = r.getDefaultW1();
	        	r.wc = r.getDefaultWC();
	        }
	        r.rigidPoint = r.hasByDefaultRigidPoint();
	        r.manualCPR = false;
	    }
	    end.run();
	}

    public void doLiftConstraints() {
	    super.doLiftConstraints();
	    for(int i = 0; i<elements.length; i++) {
	        elements[i].manualCPR = false;
	    }
	    updateElements();
	    repaint();
	}
	
	public CubicCurve2D getAdjustedLine() {
        return super.getAdjustedLine();
    }
	
	protected String getArrowHeadCode(int ch) {
	    StringBuilder sb = new StringBuilder();
	    for(int i = 0; i<elements.length; i++) {
	        sb.append(elements[i].getArrowHeadCode(ch)+" ");
	    }
	    return sb.toString();
	}
    
    public boolean[] getOrientations() {
	    return co;
    }
    
    public String getDash() {
        return getDashArrayString(stroke);
    }
    
	public float getDefaultW0() {
		return defaultW0;
	}
    
    public float getDefaultW1() {
		return defaultW1;
	}
	
    public float getDefaultWC() {
		return defaultWC;
	}
	
	public Connector[] getElements() {
	    return elements;
	}
    
    @Override
    public List<EditorEntry> computeArrowheadInfo() {
    	return null;
    }
    
	public List<EditorEntry> getInfo() {
	    if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    
		    EditorEntry dissolve = new EditorEntry("Dissolve", ACTION, "Decombine");
		    dissolve.setGlobal(true);
		    result.add(dissolve);
		    
		    result.addAll(super.getInfo());
		    
		    result.trimToSize();
		    this.info = result;
	    }
	    return info;
    }
	
	protected String getLineCode(int ch) {
	    StringBuilder sb = new StringBuilder();
	    for(int i = 0; i<elements.length; i++) {
	        if(elements[i].hasSpecialLine()) {
	            sb.append(elements[i].getLineCode(ch));
	        }
	    }
	    if(sb.length()==0) {
	        sb.append(super.getLineCode(ch));
	    }
	    return sb.toString();
    }
		
	public boolean hasSpecialLine() {
	    return specialLine;
	}
	
    public boolean isSymmetric() {
	    return symmetric;
    }	    

    public synchronized void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Rectangle r = getBounds(); // these will be the bounds in the parent's coordinate system
		g2.translate(-r.x, -r.y);
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
	    g2.setPaint(paint);        
		paintLine(g2);
		for(int i = 0; i<elements.length; i++) {
		    elements[i].paintArrowHead(g2);
		}
    }
    
	public void paintLine(Graphics2D g) {
        boolean b = false;
        for(int i = 0; i<elements.length; i++) {
	        if(elements[i].hasSpecialLine()) {
	            elements[i].paintLine(g);
	            b = true;
	        }
	    }
	    if(!b) {
	        super.paintLine(g);
	    }        
    }
       
    public void parse(String code, DIInfo dIInfo) throws ParseException {
	    StrokedShape[] s = Texdraw.getStrokedShapes(code, DEFAULT_LINEWIDTH);
        
	    final boolean debug = false;
	    if(debug) {
		    System.err.println("Shapes:");
		    for(StrokedShape sh: s) {
		        System.err.println(sh);
		    }
	    }
	    
	    int i = 0;
        for(Iterator<DIInfo> j = dIInfo.getComposition().iterator(); j.hasNext(); i++) {
            DIInfo dii = j.next();
    	    Class<?> cl = dii.getDIClass();
	        assert cl==elements[i].getClass();
            co[i] = !dii.getInverse();
        }
        int n = parseLine(s, dIInfo, 0); 
        setRigidPoint(gap1); // recursively set the rigidPoint parameter.
        for(Connector c: elements) {
        	c.setManualCPR(true); // same for the manualCPR of every element, to prevent them from recomputing cprs.
        }
        updateElements(); // recursively set baseAngle, phi, chi, psi, cpr and line for all elements.
	    parseArrowHead(s, dIInfo, n); // recursively set the other parameters, including gap0 and gap1.  
        setLineParameters(true); 
	}
    
    public int parseArrowHead(StrokedShape[] s, DIInfo dIInfo, int n) throws ParseException {
        int i = 0;
        for(Iterator<DIInfo> j = dIInfo.getComposition().iterator(); j.hasNext(); i++) {
            DIInfo si = j.next();
	        assert si.getDIClass()==elements[i].getClass();
	        if(elements[i] instanceof CompoundArrow) { 
                CompoundArrow ca = (CompoundArrow)elements[i];
                ca.gap0 = si.getGap0();
                ca.gap1 = si.getGap1();
                n = ca.parseArrowHead(s, si, n);
            }
            else {
                elements[i].gap1 = si.getGap1(); 
                n = elements[i].parseArrowHead(s, si.getInfoString(), n);
            }
            elements[i].gap = elements[i].computeGap();
	    }
        return n;
	}
	
	public int parseLine(StrokedShape[] s, DIInfo dIInfo, int n) throws ParseException {
        /*
         * Check elements for special-line property and let them parse portions of the line if they have it.
         * The first element with that property is used to determine the line-related parameters psi and cpr.
         */
        int i = 0;
        boolean b = false; // whether one of the elements has a special line
        for(Iterator<DIInfo> j = dIInfo.getComposition().iterator(); j.hasNext(); i++) {
            DIInfo dii = j.next();
	        if(elements[i].hasSpecialLine()) {
	            if(elements[i] instanceof CompoundArrow) { 
	                CompoundArrow ca = (CompoundArrow)elements[i];
	                n = ca.parseLine(s, dii, n);
	            }
	            else {
	                n = elements[i].parseLine(s, dii.getInfoString(), n);
	            }
	            if(!b) {
	                setLineByElement(i);
	            }
	            b = true;
	        }
	    }
	    if(!b) {
	        n = super.parseLine(s, null, n);
	    }
	    return n;
	}

	/*
	public void setAltLineWidth(float lw) {	    
        super.setAltLineWidth(lw);
        if(elements!=null) {
            for(int i = 0; i<elements.length; i++) {
		        elements[i].setAltLineWidth(lw);
            }
        }
	    repaint();
    }
	
	public void setAltStroke(BasicStroke s) {
        altStroke = s;
	    for(int i = 0; i<elements.length; i++) {
	        elements[i].setAltStroke(s);
	    }
    }
    */
	

    /**
     * Meant to be invoked after cloning. Therefore, no removal of this State from previous involutes.
     */
	public void setInvolute(int k, ENode e, boolean add) {
	    ENode[] inv = (ENode[])getInvolutes();
	    inv[k] = e;
	    if(add) {
	        e.addDependentItem(this);
	    }
        for(int i = 0; i<elements.length; i++) {	            
            elements[i].setInvolute(co[i]? k: 1-k, inv[k], false);
        }
	    invalidate();
	}
    
    private void setLineByElement(int i) {
        Connector r = elements[i];
		if(co[i]) {
	        psi0 = r.psi0;
	        psi1 = r.psi1;
	        cpr0 = r.cpr0;
	        cpr1 = r.cpr1;
	    } else {
	        psi0 = r.psi1;
	        psi1 = r.psi0;
	        cpr0 = r.cpr1;
	        cpr1 = r.cpr0;
	    }
	}


	public void setLineWidth(float lw) {	  
		super.setLineWidth(lw);
        for(int i = 0; i<elements.length; i++) {
	        if(elements[i].hasSpecialLine()) {
	            elements[i].setLineWidth(lw);
	        }
	    }
	    repaint();
    }
	
	public void setStroke(BasicStroke s) {
        super.setStroke(s);
        // System.err.println("CompoundArrow.setStroke: "+s);
        for(int i = 0; i<elements.length; i++) {
	        if(elements[i].hasSpecialLine()) {
	            elements[i].setStroke(s);
	        }
	    }
        repaint();
	}

	protected void setManualCPR(boolean b) {
		manualCPR = b;
    	for(Connector c: elements) {
			c.setManualCPR(b);
		}
    }
    
    /**
	 * Recursively sets the rigidPoint property of every element whose gap is smaller than the one specified.
	 */
    protected void setRigidPoint(float gap) {
		for(int i = 0, n = elements.length; i<n; i++) {
			Connector c = elements[i];
			float cg = co[i]? c.gap1: c.gap0;
			if(cg<gap) {
				if(c instanceof CompoundArrow) {
					((CompoundArrow)c).setRigidPoint(gap);
				} else {
					c.setRigidPoint(true);
				}
			}
		}
	}
	
	public String toString() {
	    StringBuilder sb = new StringBuilder(super.toString()).append("{");
	    if(elements!=null) {
	        for(int i = 0; i<elements.length; i++) {
		        sb.append(elements[i]).append(i<elements.length-1? " ": "");
		    }
	    }
	    return sb.append("}").toString();
	}

	/**
	 * Recursively updates the psi and cpr values, as well as the line, of all elements.
	 */
	private void updateElements() {
		CubicCurve2D l = getLine();
        CubicCurve2D l1 = MathTools.invert((CubicCurve2D.Float)l);
		for(int i = 0; i<elements.length; i++) {
    	    elements[i].invalidateShape(false);
        	if(co[i]) {
        		elements[i].line = l;
    	        elements[i].psi0 = psi0;
    	        elements[i].psi1 = psi1;
    	        elements[i].cpr0 = cpr0;
    	        elements[i].cpr1 = cpr1;
    	    } else {
    	    	elements[i].line = l1;
    	        elements[i].psi0 = psi1;
    	        elements[i].psi1 = psi0;
    	        elements[i].cpr0 = cpr1;
    	        elements[i].cpr1 = cpr0;
    	    }
    	    elements[i].shape = elements[i].computeShape(); // recursion
        }
	}
	
}
