/*
 * Created on 15.12.2006
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.STRING;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import jPasi.TransformModifier;
import jPasi.codec.Codec1;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.Line;
import jPasi.codec.Texdraw.Path;
import jPasi.edit.EditorEntry;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class Obtainment extends Ornament implements Rectangular {

    private static final long serialVersionUID = 840455907224399920L;

    public static final float DEFAULT_WIDTH = 2;
	public static final float DEFAULT_HEIGHT = 2;
	public static final float DEFAULT_GAP = 2;
	public static final float DEFAULT_PREFERRED_ANGLE = (float)-Math.PI/2;
	public static final float DEFAULT_LINEWIDTH = 0;
    public static final float DEFAULT_SHADING = 0; // 1 = white, 0 = black
    public static final float[] DEFAULT_DASH = null;
	
	protected float w = DEFAULT_WIDTH;
	protected float h = DEFAULT_HEIGHT;
	protected Point2D.Float p = new Point2D.Float();
    protected EditorEntry dashInfo;
	
    public Obtainment() {}
    
	public Obtainment(ENode e) {
		super(e);
		item = this;
		Rectangle r = getBounds();
		setBounds2D(r.x, r.y, r.width, r.height);
	}

    public float getDefaultLineWidth() {
        return DEFAULT_LINEWIDTH;
    }	
	
    public float getDefaultShading() {
    	return DEFAULT_SHADING;
    }

    public float getDefaultPreferredAngle() {
        return DEFAULT_PREFERRED_ANGLE;
     }

    public float getDefaultGap() {
        return DEFAULT_GAP;
    }

    public Shape computeShape() {
		p = (Point2D.Float)involutes[0].getCenter();
		float r = involutes[0].getRadius() + gap;
		float a = (float)MathTools.normalize(psi, 0);
		float aw = (float)Math.atan(w/2/(r+h/2));
		float ah = (float)Math.atan(h/2/(r+w/2));
		if(a>=-ah && a<=ah) { // right center
	        p.x = p.x + r + w/2;
	        p.y = p.y - (r + w/2)*(float)Math.tan(a);
	    }
	    else if(a>=Math.PI/2-aw && a<=Math.PI/2+aw) {
	        p.x = p.x - (r + h/2)*(float)Math.tan(a-Math.PI/2);
	        p.y = p.y - r - h/2;
	    }
	    else if(a>=Math.PI-ah || a<=-Math.PI+ah) {
	        p.x = p.x - r - w/2;
	        p.y = p.y + (r + w/2)*(float)Math.tan(a>0? a-Math.PI: a+Math.PI);
	    }
	    else if(a>=-Math.PI/2-aw && a<-Math.PI/2+aw) {
	        p.x = p.x + (r + h/2)*(float)Math.tan(a+Math.PI/2);
	        p.y = p.y + r + h/2;
	    }
	    else {
	        int tx = 1;
	        int ty = 1;		        
	        if(a>0 && a<Math.PI/2){
	            ty = -1;
	        } else if(a>Math.PI/2 && a<Math.PI) {
	            tx = -1;
	            ty = -1;
	        } else if(a>-Math.PI && a<-Math.PI/2) {
	            tx = -1;
	        }		        
	        p.x = (float)(p.x + r*Math.cos(a) + tx*w/2);
	        p.y = (float)(p.y - r*Math.sin(a) + ty*h/2);
	    }

		GeneralPath path = new GeneralPath();
		path.moveTo(p.x-w/2, p.y-h/2);
	    path.lineTo(p.x+w/2, p.y-h/2);
	    path.lineTo(p.x+w/2, p.y+h/2);
	    path.lineTo(p.x-w/2, p.y+h/2);
	    path.lineTo(p.x-w/2, p.y-h/2);
		
	    return path;
	}

	public synchronized void paint(Graphics g) {
	    super.paint(g);
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
	    float[] dash = stroke.getDashArray();
		return super.getTexdrawCode(ch) + 
			(dash!=null? Texdraw.linePattern(dash): "") +
		    Texdraw.move(p.x-w/2, ch-p.y+h/2) + 
			Texdraw.line(p.x+w/2, ch-p.y+h/2) + 
			Texdraw.line(p.x+w/2, ch-p.y-h/2) + 
			Texdraw.line(p.x-w/2, ch-p.y-h/2) + 
			Texdraw.line(p.x-w/2, ch-p.y+h/2) + 
			Texdraw.lfill(shading) +
			(dash!=null? Texdraw.linePattern(null): "");
	}
	
    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0 && 
    			TransformModifier.isScalingArrowheads(modifier)) {
    		float factor = (float)t.getScaleX();
    		float wa = w*factor;
    		float ha = h*factor;
    		if(ok(wa) && ok(ha)) {
    			w = wa;
    			h = ha;
    		}
    	}
    }
    
	// The following methods are intended for use with EditPanel:
    public float getGap() {
        return gap;
    }
    public float getRectHeight() {
        return h;
    }
    public void setRectHeight(float h) {
        this.h = h;
		Item gov = getGovernor();
       ((ENode)gov).arrangeContacts();
    }
    public float getRectWidth() {
        return w;
    }
    public void setRectWidth(float w) {
        this.w = w;
		Item gov = getGovernor();
        ((ENode)gov).arrangeContacts();
    }
    public String getDash() {
        return getDashArrayString(stroke);
    }
    public void setDash(String s) {
        stroke = changeDashArray(stroke, s, DEFAULT_DASH);
        repaint();
        if(dashInfo!=null) dashInfo.getNotifier().run(this);
    }

    public List<EditorEntry> getInfo() {
	    if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    ArrayList<EditorEntry> list = new ArrayList<EditorEntry>();
		    result.addAll(super.getInfo());
		    
		    EditorEntry gapInfo = new EditorEntry("Gap", FLOAT, "Gap");
		    gapInfo.setSpinnerValues(DEFAULT_GAP, -999, 999, 1f, false);
		    list.add(gapInfo);

		    EditorEntry widthInfo = new EditorEntry("RectWidth", FLOAT, "Width");
		    widthInfo.setSpinnerValues(DEFAULT_WIDTH, 0, 999, 1f, false);
		    list.add(widthInfo);
		    
		    EditorEntry heightInfo = new EditorEntry("RectHeight", FLOAT, "Height");
		    heightInfo.setSpinnerValues(DEFAULT_HEIGHT, 0, 999, 1f, false);
		    list.add(heightInfo);
		    
		    EditorEntry lwInfo = new EditorEntry("LineWidth", FLOAT, "Line Width");
		    lwInfo.setSpinnerValues(getDefaultLineWidth(), 0f, 99f, .2f, false);
		    list.add(lwInfo);
		    
		    dashInfo = new EditorEntry("Dash", STRING, "Line Pattern");
		    dashInfo.setDefaultValue(DEFAULT_DASH);		    
		    dashInfo.requestNotifier();
		    list.add(dashInfo);
		    
		    EditorEntry shadingInfo = new EditorEntry("Shading_EP", FLOAT, "Shading");
		    shadingInfo.setSpinnerValues(1-DEFAULT_SHADING, 0f, 1f, .05f, false);
		    list.add(shadingInfo);

		    for(EditorEntry ee: list) {
		    	ee.setGlobal(true);
		    }
		    result.addAll(list);
		    
		    result.trimToSize();
		    this.info = result;
	    }
	    return info;
	}
	
    public String getInfoString() {
	    return Codec1.encodeFloat(gap);
	}

	public void parse(String code, String info) throws ParseException {
	    Texdraw.StrokedShape[] s = Texdraw.getStrokedShapes(code, DEFAULT_LINEWIDTH);	    
	    if(!(s.length==1 && s[0].shape instanceof Path)) {
	        throw new ParseException(TEX, "Line sequence expected, not "+s[0].shape.genericDescription());
	    }
	    Path p = (Path)s[0].shape;
		if(!(p.shapes.length==4 && 
		        p.shapes[0] instanceof Line &&
		        p.shapes[1] instanceof Line &&
		        p.shapes[2] instanceof Line &&
		        p.shapes[3] instanceof Line)) {
		    throw new ParseException(TEX, "Wrong sequence of shapes: "+p);
		}
	    Line l0 = (Line)p.shapes[0];
	    Line l1 = (Line)p.shapes[1];
	    w = (float)l0.p1.distance(l0.p2);
	    h = (float)l1.p1.distance(l1.p2);

	    setShading(p.fillLevel);
	    stroke = newStroke(stroke, s[0].stroke.lineWidth, s[0].stroke.pattern);
	    
		gap = Codec1.decodeFloat(info);
	}


}
