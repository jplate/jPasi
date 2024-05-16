/*
 * Created on 19.11.2006
 *
 */
package jPasi.item;

import static jPasi.item.Direction.CLOCKWISE;
import static jPasi.item.Direction.COUNTERCLOCKWISE;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.border.Border;

import jPasi.Canvas;
import jPasi.TransformModifier;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.edit.Editable;
import jPasi.edit.EditorEntry;
import jPasi.edit.EditorTopic;
import jPasi.laf.MarkBorder;
import util.DimensionF;
import util.MapCloneable;
import util.Reflect;
import util.swing.FloatComponent;

/**
 * @author Jan Plate
 *
 */
public class Item extends JComponent implements MapCloneable, Editable, FloatComponent {
	
    private static final long serialVersionUID = 8838779362619443306L;
    
    /**
     * The highest value that item parameters should be allowed to take (to prevent crashes).
     */
    public static final long MAX_VALUE = 16384;     			
    /**
     * The lowest value that item parameters should be allowed to take (to prevent crashes).
     */
    public static final long MIN_VALUE = -MAX_VALUE; 
	private static final int DASH_STRING_LIMIT = 128;
    
	public static final float DEFAULT_ALT_LINEWIDTH = .7f;
    public static final float[] DEFAULT_DASH = null;

	public static Color DEFAULT_FOREGROUND = Color.decode("#323232");

	public static Comparator<Item> zOrderComparator = new Comparator<Item>() {
		/**
		 * Compares in such a way that items with higher z-order are ranked lower. A list sorted with this Comparator 
		 * will therefore contain its elements in the same order in which they would be painted by a Container.
		 */
		public int compare(Item i1, Item i2) {
	        Canvas c = i1.getCanvas();
	        int z1 = c.getComponentZOrder(i1);
	        int z2 = c.getComponentZOrder(i2);
			return z1<z2? 1: z1==z2? 0: -1;
        }		
	};
	
	public static final float DEFAULT_LINEWIDTH = 1f;
    public static final float DEFAULT_SHADING = 1; // 1 = white, 0 = black
    public static final Direction DEFAULT_DIRECTION = COUNTERCLOCKWISE;

	private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    
	/**
	 * Finds the index in the specified pattern at which the specified length is reached, assuming that we start with
	 * an index of 0. The last two info in the parameter list are return arguments, containing the remainder of the last
	 * pattern segment, and whether it is opaque or not. (When the method is called, the boolean array should contain 
	 * <code>false</code> iff the first segment of the pattern is opaque.)
	 */
	public static int findIndex(double arcLength, float correction, float[] pattern, double[] rest, boolean[] opaque) {
        boolean found = false;
        int n = 0; // segment #
        int m = pattern.length;
        double patternLength = 0;
        double r = arcLength;
        while(!found) {
            for(int i = 0; i<m && !found; i++) {
                //System.err.println("d: "+dash1[i]);
                opaque[0] = !opaque[0];
                r -= pattern[i]-(opaque[0]? correction: 0);
                if(r<-.0001) {
                   found = true;
                   n = i;
                }
            }
            //System.err.println("rest: "+rest);
            if(!found) {
                patternLength = arcLength - r;
                r = r%patternLength;
            }
        }
        rest[0] = r;
	    return n;
	}  

	public static String getCommand(Arc2D arc, BasicStroke stroke, int ch) {
        
	    StringBuilder result = new StringBuilder(
	            Texdraw.move((float)arc.getCenterX(), (float)(ch - arc.getCenterY())));
	    
        double ex = arc.getAngleExtent();
	    double st = arc.getAngleStart();
	    double r = arc.getWidth()/2;
	    double ed = st + ex;
	    
	    if(ex<0) {
	        ed = st;
	        st = st + ex;
	    }
        
	    float[] pattern = stroke.getDashArray();
        if(ex<0 && pattern!=null) {
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
            int n = findIndex(Math.PI*(Math.abs(ex)/180)*r, .182f, pattern1, rest, opaque);
            
            // cut-off segment
            double st1 = st + (pattern1[n]+rest[0])*180/Math.PI/r;
            if(opaque[0]) {
                result.append(Texdraw.linePattern(null)).
                	append(Texdraw.arc((float)r, (float)st, (float)st1));
            } 
            st = st1;
            n = n==0? m-1: n-1;
            if(opaque[0]) {
	            st += pattern1[n]*180/Math.PI/r;
	            n = n==0? m-1: n-1;
            }
            
            // new pattern
            for(int i = 0; i<pattern.length; i++) {
                pattern[i] = pattern1[(m+n-i)%m];
            }
            
            if(opaque[0] || !Arrays.equals(pattern, stroke.getDashArray())) 
                	result.append(Texdraw.linePattern(pattern));
            result.append(Texdraw.arc((float)r, (float)st, (float)ed));            
        } 
        else {
	        result.append(Texdraw.arc((float)r, (float)st, (float)ed));
	    }
        
        return result.toString();
	}
	
	public static String getCommand(CubicCurve2D c, int ch) {		
		Point2D.Float 
			p0 = (Point2D.Float)c.getP1(), 
			p0a = (Point2D.Float)c.getCtrlP1(), 
			p1a = (Point2D.Float)c.getCtrlP2(), 
			p1 = (Point2D.Float)c.getP2();		
		return Texdraw.move(p0.x, ch - p0.y)+
				Texdraw.curve(p0a.x, ch - p0a.y, p1a.x, ch - p1a.y, p1.x, ch - p1.y);
	}
	
	public static String getCommand(Line2D l, int ch) {
	    Point2D.Float p0 = (Point2D.Float)l.getP1();
	    Point2D.Float p1 = (Point2D.Float)l.getP2();
	    return Texdraw.move(p0.x, ch - p0.y)+
	    		Texdraw.line(p1.x, ch - p1.y);
	}

	public static String getCommand(Point2D p, int ch) {
	    return Texdraw.move((float)p.getX(), (float)(ch - p.getY()));	    
	}

	public static String getContinuingCommand(CubicCurve2D c, int ch) {		
		Point2D.Float 
			p0a = (Point2D.Float)c.getCtrlP1(), 
			p1a = (Point2D.Float)c.getCtrlP2(), 
			p1 = (Point2D.Float)c.getP2();		
		return Texdraw.curve(p0a.x, ch - p0a.y, p1a.x, ch - p1a.y, p1.x, ch - p1.y);
	}
	
	public static String getContinuingCommand(Line2D l, int ch) {
	    Point2D.Float p1 = (Point2D.Float)l.getP2();
		return Texdraw.line(p1.x, ch - p1.y);
	}

	private static void getSuperclasses(Class<?> c, List<Class<?>> l) {
	    l.add(c);
	    Class<?> sc = c.getSuperclass();
	    if(sc!=null) {
	        getSuperclasses(sc, l);
	    }
	}
        
	protected static BasicStroke newStroke(BasicStroke stroke, float lw, float[] dash) {
        return new BasicStroke(
	               lw, 
	               stroke.getEndCap(), 
	               stroke.getLineJoin(), 
	               stroke.getMiterLimit(),
			       dash, 0);
    }

    public static boolean ok(double v) {
		return v>MIN_VALUE && v<MAX_VALUE;
	}

	public static BasicStroke scaleStroke(BasicStroke s, float factor) {
		float[] dash = s.getDashArray();
		if(dash!=null) {
			for(int i = 0, n = dash.length; i<n; i++) {
				float f = dash[i] * factor;
				if(ok(f)) {
					dash[i] = f;
				}
			}
		}
		return new BasicStroke(
	               factor*s.getLineWidth(), 
	               s.getEndCap(), 
	               s.getLineJoin(), 
	               s.getMiterLimit(),
			       dash,
			       s.getDashPhase());
	}
	
	/**
	 * The direction in which lines are bent and angles adjusted.
	 */
	public Direction flexDirection = DEFAULT_DIRECTION;
	
	protected BasicStroke altStroke;
	private Border border; // keep private to hide from cloning
	protected Rectangle2D.Float bounds2d = new Rectangle2D.Float();
	
	protected Rectangle bounds = new Rectangle();
	protected boolean boundsValid = false;
    protected Canvas canvas;
    protected boolean delegate; // keep protected so it will be copied by clone(Map)
	protected DecimalFormat format;
	private Item governor;  // keep private to avoid circular cloning  
    protected boolean hidden;
	protected boolean hiddenWhenSelected;
	protected EditorEntry dashInfo; 
	protected List<EditorEntry> info;
	protected Paint paint;
    protected boolean resizeable;
    private boolean selected; // keep private to hide from cloning
    protected float shading;
	protected Paint shadePaint;
	protected DimensionF sizeWhenHidden = new DimensionF(0f, 0f);
	protected DimensionF sizeWhenNotHidden = new DimensionF(0f, 0f);
	
	protected HashSet<DependentItem> dependentItems = new HashSet<DependentItem>(); // declared as HashSet for cloning.
	protected BasicStroke stroke;

	protected int xMargin = 4;
	
	protected int yMargin = 4;
	
	protected float zValue = -1; // -1 means not yet set.
	
	protected Item() {
	    setName("");
		setForeground(DEFAULT_FOREGROUND);
		paint = DEFAULT_FOREGROUND;
		BasicStroke stroke = DEFAULT_STROKE;
		this.stroke = newStroke(stroke, getDefaultLineWidth(), null);
		this.altStroke = newStroke(stroke, getDefaultAltLineWidth(), null);
	    setShading(getDefaultShading());
		format = new DecimalFormat();
	    bounds2d = new Rectangle2D.Float(0,0,0,0);
	}
    
	public Item(Canvas c) {
	    this();
		this.canvas = c;
	}

	public Item(Canvas c, Item governor, boolean hidden) {
		this(c);
		setGovernor(governor);
		setHidden(hidden);
	}
	
    public void addDependentItem(DependentItem s) {
		dependentItems.add(s);
	}
    
    public void adjustToTransform(AffineTransform t, int modifier) {
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0) {
    		float factor = (float)t.getScaleX();
    		if(TransformModifier.isScalingLines(modifier)) {
    			stroke = scaleStroke(stroke, factor);
    			altStroke = scaleStroke(altStroke, factor);    			
    		}
    	}
    	if((t.getType() & AffineTransform.TYPE_FLIP)>0) {
			flexDirection = flexDirection==COUNTERCLOCKWISE? CLOCKWISE: COUNTERCLOCKWISE;
    	}
    }
	
    public BasicStroke changeDashArray(BasicStroke stroke, String s, float[] defaultDash) {
        float[] dash;
        BasicStroke result;
        if(s!=null && s.length()>0) {
 	       String[] tokens = s.split("[ ;\t]+");
 	       dash = new float[tokens.length];
 	       for(int i = 0; i<dash.length; i++) {
 	           Number n = null;
 	           try {
 	               n = format.parse(tokens[i]); 
 	           } 
 	           catch(java.text.ParseException pe) {}
 	           
 	           dash[i] = 0;
 	           if(n!=null) {
 	               float f = n.floatValue();
 	               dash[i] = f<0 || !ok(f)? 0: f;
 	           }
 	       }
        }
        else {
            dash = null;
        }
        try {
 	       result = newStroke(stroke, stroke.getLineWidth(), dash);
        }
        catch(IllegalArgumentException iae) {
            result = newStroke(stroke, stroke.getLineWidth(), defaultDash);
        }
        return result;
    }	
  
    @Override
    public Object clone() throws CloneNotSupportedException {
	    return clone(new HashMap<Object, Object>());
	}
    
    /**
     * See interface <code>MapCloneable</code>.
     */
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
        Item clone;
        if(map.values().contains(this)) {
            clone = this;
        } else {
            clone = (Item)map.get(this);
        }
        if(clone==null) {
            /*
             * clone all the fields declared by the subclasses and Item.class, but don't clone the leaves of info, because
             * the involutes of DependentItems are stored in Entity info, and these should not be cloned right away. Instead, the 
             * clone(Map) method of DependentItem should take care, if the State in question is a clone, to replace the latter's 
             * involutes by clones according to the clone-map, or leave them untouched if that map does not contain a clone for them.
             */
		    boolean selected = isSelected();
		    setSelected(false); // to make the clone hidden if and only if it should be, since it won't necessarily be selected.
		    clone = (Item)subclone(Item.class, map, false);
		    setSelected(selected);
		    
		    clone.setBounds2D(clone.bounds2d); // to initialize the dimension fields declared by the superclasses
		    clone.setShading(clone.getShading());  // necessary due to reliance on superclass field in setShading()

		    Item cgov = (Item)map.get(governor);
		    if(cgov!=null) {
		        clone.setGovernor(cgov);
		    }
		    
		    Set<DependentItem> cs = clone.getDependentItems();
		    cs.clear();
		    Set<?> di1 = (Set<?>)((HashSet<DependentItem>)getDependentItems()).clone();
		    for(Iterator<?> i = di1.iterator(); i.hasNext();) {
		        DependentItem s = (DependentItem)i.next();	            
		        DependentItem sc = (DependentItem)map.get(s);
		        if(sc==null) { 
		        	/* sc either needs to be cloned or is a clone already. In the latter case, the following will simply retrieve
		        	 * sc again:
		        	 */ 
		            sc = (DependentItem)s.clone(map);
		        }
		        cs.add(sc);
		    }
        }
	    return clone;
	}
    
     /**
     * Indicates whether the texdraw-code generation process may coalesce drawn shapes into paths by omitting unnecessary 
     * move-commands.
     */
    protected boolean coalesceDrawnShapes() {
    	return true;
    }

	/**
	 * Divides the set of all DependentItems of this Item into two Sets: (1) those that don't involve any Entities other than 
	 * (a) those contained in the specified Set and (orientation) the items of States based on that Set (in the sense that all 
	 * their involutes are contained in it), and (2) those that do. Those items are accumulated in the third parameter.
	 * The method is recursive and non-monotonic with respect to the accumulater of the external DependentItems, so it can also 
	 * be used for iterating over a Set of Items, as it is done in the static method of the same name in class Canvas. 
	 */
	public void divideDIs(Set<Item> items, Set<DependentItem> internal, Set<DependentItem> external, Set<Item> thirds) {
        Set<DependentItem> dis = dependentItems;
        for (DependentItem di : dis) {
            IndependentItem<?>[] inv = di.getInvolutes();
            if(inv.length==1) {
                internal.add(di);
            } else {                
                boolean provedInternal = true;
                for(int i = 0; provedInternal && i<inv.length; i++) {                    
                    if(i!=di.getItemIndex()) {
                        provedInternal = items.contains(inv[i]) || thirds.contains(inv[i]);
                    }
                }
	            if(provedInternal) {
	                external.remove(di);
	                internal.add(di);
	                Item item = di.getItem();
	                if(item!=null) {
		                thirds.add(item);                    
		                item.divideDIs(items, internal, external, thirds);
		            }
	            } else {
	                external.add(di);
	            }
            }
        }
	}

	public float getAltLineWidth() {
        return altStroke.getLineWidth();
    }
	
	public float getAltLineWidth_g() {
        return getAltLineWidth();
    }
	
	public Border getBorder() {
	    return border;
	}
	
	public Rectangle2D getBounds2D() {
		return bounds2d; 
	}

    public Canvas getCanvas()  {
        Component parent = getParent();
        if(canvas!=null) {
            return canvas;
        }
        else if(parent instanceof Canvas) {
            return (Canvas)parent;
        }
        else if(getGovernor()!=null) {
            return getGovernor().getCanvas();
        }
        else {
            System.err.println(parent);
            throw new IllegalStateException("no accessible Canvas");
        }
    }
    
	public Point2D getCenter() {
		return new Point2D.Float(bounds2d.x+bounds2d.width/2, bounds2d.y+bounds2d.height/2);
	}

    /**
	 * dash0: the line pattern that has already been set
	 * lw0: the line width that has already been set
	 */
	protected String getCode(Shape[] shapes, BasicStroke stroke, int ch, float[] dash0, float lw0) {
	    StringBuilder result = new StringBuilder();        
	    float lw = stroke.getLineWidth();
	    float[] dash = stroke.getDashArray();
	    
        if(!(lw==lw0)) result.append(Texdraw.lineWidth(lw));
        Point2D.Float lastPoint = null;
        boolean coalesce = coalesceDrawnShapes();
        for(int i = 0; i<shapes.length; i++) {
            String str = getCommand(shapes[i], ch);
            try {
	            if(coalesce && lastPoint!=null && lastPoint.equals(Texdraw.movePoint(str))) {
	                str = getContinuingCommand(shapes[i], ch);
	            }
	            lastPoint = Texdraw.lastPoint(str);
            } catch(ParseException pe) {
            	throw new Error("Failure to read back", pe);
            }
            boolean dfp = Texdraw.hasOwnLinePattern(str); 
            if(!dfp && i==0 && dash!=null && !Arrays.equals(dash0, dash)) {
                result.append(Texdraw.linePattern(dash));
            }
            result.append(str);
            if(dfp && dash!=null && i<shapes.length-1) { // append code of original pattern for remaining Shapes
                result.append(Texdraw.linePattern(dash));
            }
        }
        result.append(dash!=null? Texdraw.linePattern(null): "");

        return result.toString();
	}

    protected String getCode(Shape[] drawnShapes, Shape[] filledShapes, boolean hasNonFillableShapes, BasicStroke stroke, int ch) {
        StringBuilder result = new StringBuilder();        
        float lw = stroke.getLineWidth();
        float[] dash = stroke.getDashArray();
        boolean filled = shading < 1;
        if(filled) {
            if(filledShapes!=null) {
		        if(dash!=null) result.append(Texdraw.linePattern(dash)); // the line pattern is set already here to indicate 
		        		// to the decoder that the code contains further shapes (viz., those used for drawing)
		        result.append(Texdraw.lineWidth(lw)); 
		        result.append(getCommand(filledShapes[0], ch));
		        for(int i = 1; i<filledShapes.length; i++) {
		            result.append(getContinuingCommand(filledShapes[i], ch));
		        }
		        result.append(!hasNonFillableShapes && (dash==null || filledShapes==drawnShapes)? 
		                Texdraw.lfill(shading): Texdraw.ifill(shading));
            }
        }
        if(!filled || !(dash==null || filledShapes==drawnShapes) || hasNonFillableShapes) {
            if(drawnShapes!=null) result.append(
                    getCode(drawnShapes, stroke, ch, (filled? dash: null), (filled? lw: java.lang.Float.NaN)));
        }
        return result.toString();
    }

	/**
	 * Assumes that the arc is going to be drawn using altStroke.
	 */
	public String getCommand(Arc2D arc, int ch) {
	    return getCommand(arc, altStroke, ch);
	}

	public String getCommand(Shape s, int ch) {
	    if(s instanceof Line2D) {
	        return getCommand((Line2D)s, ch);
	    } else if(s instanceof CubicCurve2D) {
	        return getCommand((CubicCurve2D)s, ch);
	    } else if(s instanceof Arc2D) {
	        return getCommand((Arc2D)s, ch);
	    } else {
	        throw new IllegalArgumentException("Shape must be either a Line, Arc, or CubicCurve, not:"+s);
	    }
	}

	public String getContinuingCommand(Shape s, int ch) {
	    if(s instanceof Line2D) {
	        return getContinuingCommand((Line2D)s, ch);
	    } else if(s instanceof CubicCurve2D) {
	        return getContinuingCommand((CubicCurve2D)s, ch);
	    } else {
	        throw new IllegalArgumentException("Shape must be either a Line or CubicCurve, not: "+s);
	    }
	}
	
    /**
     * For use with EditorPane
     */
    public String getDash() {
        return getDashArrayString(stroke);
    }
    
    public String getDashArrayString(BasicStroke stroke) {
	    float[] dash = stroke.getDashArray();
	    StringBuilder sb = new StringBuilder();
        if(dash!=null) {
	       for(int i = 0; i<dash.length; i++) {
	           sb.append(format.format(dash[i]) + (i<dash.length-1? " ": ""));
	       }
        }
        return sb.toString();
	}	
	
	public float getDefaultAltLineWidth() {
        return DEFAULT_ALT_LINEWIDTH;
    }
	
    public float getDefaultLineWidth() {
        return DEFAULT_LINEWIDTH;
    }

	public float getDefaultShading() {
    	return DEFAULT_SHADING;
    }
	
    public Set<DependentItem> getDependentItems() {
		return dependentItems;
	}

    public Editable getEditorDelegate(EditorTopic topic) {
	    switch(topic) {
	    case ITEM: {
	    	return isDelegate()? getGovernor(): this;
	    }
	    default: 
	    	return null;
	    }
    }
	
	public Set<Editable> getGlobalSet(EditorTopic topic) {
	    HashSet<Editable> result = new HashSet<Editable>();
	    Canvas c = getCanvas();
	    for(Item item: c.getSelectedSet()) {
    		result.add(item.isDelegate()? item.getGovernor(): item);
	    }
		return result;
    }
	
	public Item getGovernor() {
		return governor;
	}
	
	public float getHeight2D() {
		return bounds2d.height;
	}
     
    public List<EditorEntry> getInfo() {
	    if(info==null) {
	    	ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    
	    	/*
	    	 * Invisible info to set the flexDirection back to normal if the Defaults button is pressed.
	    	 */
	    	EditorEntry fInfo = new EditorEntry("Flipped", EditorEntry.Type.BOOLEAN, "Orientation");
		    fInfo.setDefaultValue(false);
		    fInfo.setPotentiallyVisible(false);
		    result.add(fInfo);
		    
		    result.trimToSize();
		    info = result;
	    }
	    
	    return info;
	}
    
    public List<EditorEntry> getInfo(EditorTopic topic) {
		switch(topic) {
		case ITEM: return getInfo();
		default: return null;
		}
	}
    
	/**
	 * @return a String containing information that cannot easily be recovered from the generated texdraw output.
	 */
	public String getInfoString() {
	    return null;
	}
	
	public float getLineWidth() {
        return stroke.getLineWidth();
    }
	
	public Point2D.Float getLocation2D() {
        return new Point2D.Float(bounds2d.x, bounds2d.y);
    }	

	public float getShading() {
        return shading;
    }
	
	public final float getShading_EP() {
        return 1f-getShading();
    }
	
	public DimensionF getSizeWhenHidden() {
		return sizeWhenHidden;
	}
	
	public DimensionF getSizeWhenNotHidden() {
		return sizeWhenNotHidden;
	}

	public String getTexdrawCode(int canvasHeight) {
		return Texdraw.lineWidth(getLineWidth());
				/*"\\PSsetlinejoin "+(lj==BasicStroke.JOIN_MITER? 0: lj==BasicStroke.JOIN_ROUND? 1: 2)+" "+
				"\\PSsetlinecap "+(ec==BasicStroke.CAP_BUTT? 0: ec==BasicStroke.CAP_ROUND? 1: 2)+" ";*/
	}

	public float getWidth2D() {
		return bounds2d.width;
	}
    
    public float getZValue() {
    	return zValue;
    }
    
    public boolean isDelegate() {
	    return delegate;
	}
    
    public boolean isFlipped() {
    	return flexDirection!=DEFAULT_DIRECTION;
    }
    
    public boolean isHidden() {
		return hidden;
	}
    
    public boolean isHiddenWhenSelected() {
		return hiddenWhenSelected;		
	}
    
    public boolean isResizeable() {
		return resizeable;
	}
	
    public boolean isSelected() {
        return selected;
    }

    public void move(float x, float y) {
    	if(ok(x) && ok(y)) {
    		setBounds2D(x, y, bounds2d.width, bounds2d.height);
    	}
    }
	
	/**
     * This is the method that should be called for dragging the Item.
     */
	public void moveBy(float dx, float dy) {
		if(dx!=0 || dy!=0) {
			float x = bounds2d.x+dx;
			float y = bounds2d.y+dy;
			if(ok(x) && ok(y)) {
				setBounds2D(x, y, bounds2d.width, bounds2d.height);
			}
		}
    }
	
	@Override
    public void paint(Graphics g) {
		//System.err.println("paint: "+this+" z: "+(canvas==null? "?": ""+canvas.getComponentZOrder(this)));
        preparePaint(g);
        paintShape(g);
        paintBorder(g);
	}
	
	protected void paintBorder(Graphics g) {
		Border b = getBorder();
		Rectangle2D.Float r = (Rectangle2D.Float)getBounds2D(); // these will be the bounds in the parent's coordinate system
		if(b!=null) {
			if(b instanceof MarkBorder) {
				((MarkBorder)b).paintBorder2D(this, g, 0, 0, (float)r.getWidth(), (float)r.getHeight());
			}
		}
    }
	
	protected void paintShape(Graphics g) {}
	
	/**
	 * Invoked by Codec#load(); should be overridden by subclasses. Overriding implementations can rely on the following 
	 * fields having been set by Codec#load() before this method is called: all the fields set by the constructor, which, in 
	 * the case of a DependentItem, will be the constructor that takes the future involutes as arguments, as well as 
	 * Item#canvas. In addition, unless this Item is an element of a CompoundArrow, the following fields 
	 * will be set both before and afterwards this method is called: 
	 * DependentItem#priority, DependentItem#oldPriority, DependentItem#priorityResettable, Connector#manualCPR, and 
	 * Connector#manualBaseAngle.<br/> 
	 * Moreover, implementations of TernaryRelationships can assume that the 'item' field will also have been set by the time 
	 * this method is called. They should take care, however, not to add any Items to the Canvas.
	 * @param code
	 * @param info
	 * @throws ParseException
	 */
	public void parse(String code, String info) throws ParseException {}
	
    protected void preparePaint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Rectangle2D.Float r = (Rectangle2D.Float)getBounds2D(); // these will be the bounds in the parent's coordinate system
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(stroke);
	    g2.setPaint(paint);        
    }
	
	public void removeDependentItem(DependentItem s) {
		dependentItems.remove(s);
	}
	
	public void removeOrnament(Ornament o) {
		dependentItems.remove(o);
	}
	
	public boolean requiresResetButton() {
	    return false;
	}
	
	public boolean requiresResetButton(EditorTopic infoCode) {
	    return infoCode==EditorTopic.ITEM? requiresResetButton(): false;
	}
	
	public void setAltLineWidth(float altLineWidth) {
		if(altLineWidth>=0 && altLineWidth < MAX_VALUE) {
	        altStroke = new BasicStroke(
		               altLineWidth, 
		               altStroke.getEndCap(), 
		               altStroke.getLineJoin(), 
		               altStroke.getMiterLimit(),
				       altStroke.getDashArray(),
				       altStroke.getDashPhase());
	        repaint();
		}
    }
	
	public void setBorder(Border b) {
	    border = b;
	    repaint();
	}
	
	public void setBounds2D(float x, float y, float w, float h) {
		bounds2d.x = x;
		bounds2d.y = y;
		bounds2d.width = w;
		bounds2d.height = h;
		//if(this instanceof ENode) System.err.println("Is2: "+y+" "+Math.round(y));
		int x1 = Math.round(x);
		int y1 = Math.round(y);
		int w1 = Math.round(w);
		int h1 = Math.round(h);
		int x2 = getX();
		int y2 = getY();
		int w2 = getWidth();
		int h2 = getHeight();
		if(x1!=x2 || y1!=y2 || w1!=w2 || h1!=h2) {
			setBounds(x1, y1, w1, h1);
		}
	}	
	
	public void setBounds2D(Rectangle2D.Float r) {
        Rectangle r1 = super.getBounds();
        if(!bounds2d.equals(r) || r1.x!=r.x || r1.y!=r.y || r1.width!=r.width || r1.height!=r.height) {
            setBounds2D(r.x, r.y, r.width, r.height);
        }
	}
    
    public void setCanvas(Canvas canvas) {
	    this.canvas = canvas;
	}
	
    /**
     * For use with EditorPane
     */
    public void setDash(String s) {
    	if(s.length()>DASH_STRING_LIMIT) {
    		s = s.substring(0, DASH_STRING_LIMIT);
    	}
        setStroke(changeDashArray(stroke, s, DEFAULT_DASH));
        repaint();
    }
    
    public void setDelegate(boolean b) {
	    delegate = b;
	}
    
    public void setFlipped(boolean b) {
    	flexDirection = b? DEFAULT_DIRECTION==CLOCKWISE? COUNTERCLOCKWISE: CLOCKWISE: DEFAULT_DIRECTION;
    }
    
    public void setGovernor(Item governor) {
		this.governor = governor;
	}
 
    public void setHidden(boolean hidden) {
		boolean wasHidden = this.hidden;
		this.hidden = hidden;
		if(wasHidden!=hidden) {			
			setSize(hidden? sizeWhenHidden: sizeWhenNotHidden);
		}
		if(isVisible()) repaint();
	}
    
    public void setHiddenWhenSelected(boolean b) {
		hiddenWhenSelected = b;	
        setHidden(shouldBeHidden());
	}
    
    public void setLineWidth(float lineWidth) {
        if(lineWidth>=0 && lineWidth<MAX_VALUE) {
	    	stroke = new BasicStroke(
	               lineWidth, 
	               stroke.getEndCap(), 
	               stroke.getLineJoin(), 
	               stroke.getMiterLimit(),
			       stroke.getDashArray(),
			       stroke.getDashPhase());
	
	        repaint();
        }
     }

    public void setLocation2D(Point2D p) {
        setBounds2D((float)p.getX(), (float)p.getY(), bounds2d.width, bounds2d.height);
    }

    public void setResizeable(boolean resizeable) {
		this.resizeable = resizeable;
	}

    public void setSelected(boolean b) {
        selected = b;
        setHidden(shouldBeHidden());
    }
	
    public void setShading(float s) {
        shading = s;
        /*
         * We rely on our own field here (instead of setBackground() or some such) so as not to trigger invalidate() and 
         * repaint(), because this method is called from parse methods.
         */
        shadePaint = new Color(shading, shading, shading); 
    }
    
	/*
     * To be invoked by an EditorPane. Mainly exists because SNode overrides setShading(), so that Item is no longer 
     * the declaring class of that method. But in the end, we do need more reflection.
     */
    public final void setShading_EP(float s) {
        setShading(1f-s);
        repaint();
    }
	
	public void setSize(DimensionF d) {	    
		setBounds2D(
		        bounds2d.x + (bounds2d.width-d.width)/2, 
		        bounds2d.y + (bounds2d.height - d.height)/2, 
		        d.width, d.height);
		if(hidden) {
			sizeWhenHidden = d;
		}
		else {
			sizeWhenNotHidden = d;
		}
	}

	public void setSizeWhenHidden(DimensionF sizeWhenHidden) {
		if(hidden) {
			setSize(sizeWhenHidden);
		}
		else {
			this.sizeWhenHidden = sizeWhenHidden;
		}
	}

	public void setSizeWhenNotHidden(DimensionF sizeWhenNotHidden) {
		if(!hidden) {
			setSize(sizeWhenNotHidden);
		}
		else {
			this.sizeWhenNotHidden = sizeWhenNotHidden;
		}
	}
	
	public void setStroke(BasicStroke s) {
		this.stroke = s;
	}

	public void setZValue(float value) {
    	zValue = value;
    }

	public boolean shouldBeHidden() {
	    return governor!=null && (!selected || hiddenWhenSelected) && (dependentItems==null || dependentItems.size()==0);
	}

	/**
	 * Returns a 'partial but deep' map-clone of this Item: for every field declared by the non-interface classes instantiated 
	 * by this Item up to and including the specified class, if that field holds a Cloneable object with a public clone
	 * method, the corresponding field in the copy will be set to a clone of that object, and analogously if the field holds
	 * a MapCloneable object. (Otherwise, the field will simply be set to the object itself.)
	 * 
	 * As for the danger of circularity, implementations of this class and its subclasses have to take care that their instances
	 * do not form or contain any circular structures that the cloning process might traverse. The single exception is direct 
	 * self-reference.
	 */
    protected Object subclone(Class<?> max, Map<Object, Object> map, boolean reachThroughArrays) {
        Class<?> c0 = getClass();
	    List<Class<?>> cl = new LinkedList<Class<?>>();
	    getSuperclasses(c0, cl);
	    boolean end = false;
	    Item clone = null;
	    try {
		    Constructor<?> cstr = cl.get(0).getConstructor((Class[])null);
	        clone = (Item)cstr.newInstance((Object[])null);
		    clone.setName(getName()+"'");
	    } catch(Exception e) {
	        throw new RuntimeException(e);
	    }
	    map.put(this, clone);
	    
	    if(clone!=null) {
		    for(Iterator<Class<?>> j = cl.iterator(); !end && j.hasNext();) {
		        Class<?> c = j.next();
		        if(c.isAssignableFrom(max)) {
		            end = true;
		        }
		        
		        /*
		         * Copy all the non-private and non-static field values.
		         */
		        Field[] f = c.getDeclaredFields();
		        for (Field field : f) {
		            int fm = field.getModifiers();
                    Class<?> ft = field.getType(); 
		            if(!Modifier.isStatic(fm) && !Modifier.isPrivate(fm)) {
		                //System.err.println("Copying field "+f[i].getName()+" in "+c);
			            try {
			                Object o = field.get(this);
			                Object oc = o;
			                if(o!=null && Cloneable.class.isAssignableFrom(ft)) {
			                    if(o==this) {
			                        oc = clone;
			                    } else if(ft.isArray()) {
		                            oc = Reflect.arrayClone(o, Reflect.getLeafType(ft), map, reachThroughArrays); 
		                        } else if(MapCloneable.class.isAssignableFrom(ft)) {
			                        Method m = o.getClass().getMethod("clone", new Class[] {Map.class});
			                        oc = m.invoke(o, new Object[] {map});
		                        } else {
			                        Method m = o.getClass().getMethod("clone", (Class[])null);
			                        int mo = m.getModifiers();
			                        if(Modifier.isPublic(mo)) oc = m.invoke(o, (Object[])null);
			                    }
			                }
			                field.set(clone, oc);
			            } catch(Exception e) {
			                e.printStackTrace(System.err);
			            	throw new RuntimeException(e);
			            }
		            }
		        }
		    }
	    }
	    return clone;
    }
    
}
