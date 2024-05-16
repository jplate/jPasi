/*
 * Created on 15.12.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.item;

import static jPasi.codec.CodePortion.*;
import static jPasi.edit.EditorEntry.Type.BOOLEAN;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.TEXT;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import jPasi.Canvas;
import jPasi.TransformModifier;
import jPasi.codec.Codec1;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.codec.Texdraw.Text;
import jPasi.edit.EditorEntry;
import util.MathTools;
import util.swing.TextTools;

/**
 * @author Jan Plate
 *
 */
public class Label extends Ornament {

    private static final long serialVersionUID = -935898244892120945L;

    public static final int MIN_WIDTH = 18;
	public static final int MAX_WIDTH = 180;
	public static final int HEIGHT = 10;
	public static final float DEFAULT_PREFERRED_ANGLE = 0.175f;
	public static final float DEFAULT_GAP = 2;
	public static final boolean DEFAULT_CENTERED = false;
	
	protected String text;
	protected boolean centered = DEFAULT_CENTERED;
		
	private String displayText;
	private int descent;
	private EditorEntry gapInfo;
	private boolean angleChangeable = !DEFAULT_CENTERED;
	
	public Label() {}
	
	public Label(ENode e) {
	    this(e, "");
	}
	
	public Label(ENode e, String s) {
		super(e);
		item = this;
		this.text = s;
		Rectangle r = getBounds();
		setBounds2D(r.x, r.y, r.width, r.height);
		gapChangeable = !DEFAULT_CENTERED;
		priorityChangeable = !DEFAULT_CENTERED;
	}


    public float getDefaultPreferredAngle() {
       return DEFAULT_PREFERRED_ANGLE;
    }
    
    public float getDefaultGap() {
        return DEFAULT_GAP;
     }

    public Shape computeShape() {
		Point2D.Float c = (Point2D.Float)involutes[0].getCenter();
		float r = centered? 0: involutes[0].getRadius() + gap;
		
		Rectangle2D.Float rect = new Rectangle2D.Float(0, 0, MIN_WIDTH, HEIGHT);
		Component parent = getParent();
		if(parent!=null) {
			Graphics2D g2 = (Graphics2D)parent.getGraphics();	
			displayText = TextTools.trim(text, g2, MAX_WIDTH);
			float sw = (float)TextTools.stringWidth(displayText, g2.getFont(), g2.getFontRenderContext());			
			rect.width = Math.max(MIN_WIDTH, sw);
		}

		int top = psi>0 && psi<Math.PI? 1: psi==0 || (int)Math.round(Math.abs(Math.toDegrees(psi)))==180? 0: -1;
		int left = psi<-Math.PI/2 || psi>Math.PI/2? 1: psi==-Math.PI/2 || psi==Math.PI/2? 0: -1;			
		rect.x = c.x + r*(float)Math.cos(psi) - (centered || left==0? rect.width/2: left>0? rect.width: 0);
		rect.y = c.y - r*(float)Math.sin(psi) - (centered || top==0? rect.height/2: top>0? rect.height: 0);
		
		GeneralPath path = new GeneralPath();
		path.moveTo(rect.x, rect.y);
		path.lineTo(rect.x, rect.y + rect.height);
		path.lineTo(rect.x + rect.width, rect.y + rect.height);			
		
		return path;
	}

	public synchronized void paint(Graphics g) {
	    super.paint(g);
		Graphics2D g2 = (Graphics2D)g;
		Rectangle2D.Float r = (Rectangle.Float)getBounds2D(); // these will be the bounds in the parent's coordinate system
		float ascent = g2.getFontMetrics().getAscent();
		
	    descent = g2.getFont().getSize()/3;
		g2.drawString(displayText, 0, ascent);
	}
	
	public String getTexdrawCode(int ch) {	    
		int top = psi>0 && psi<Math.PI? 1: equal(psi, 0) || equal(psi, -Math.PI) || equal(psi, Math.PI)? 0: -1;
		int left = psi<-Math.PI/2 || psi>Math.PI/2? 1: equal(psi, -Math.PI/2) || equal(psi, Math.PI/2)? 0: -1;
		float x = bounds2d.x + (centered || left==0? bounds2d.width/2: left>0? bounds2d.width - xMargin: xMargin);
		float y = bounds2d.y + (centered || top==0? bounds2d.height/2: top>0? bounds2d.height - descent: descent);
		return Texdraw.textref(
		        centered || left==0? Texdraw.CENTER:
		        	left>0? Texdraw.RIGHT:  Texdraw.LEFT,
		        centered || top==0? Texdraw.CENTER:
		        	top>0? Texdraw.BOTTOM: Texdraw.TOP) +
		        Texdraw.htext(x, ch - y, text);
	}
	
	private boolean equal(double a, double b) {
	    double epsilon = 1e-12;
	    return Math.abs(a - b)<epsilon;
	}
    
	public void adjustToTransform(AffineTransform t, int m) {
	    boolean rotate = TransformModifier.isRotatingLabels(m);
    	if((t.getType() & AffineTransform.TYPE_FLIP) > 0) {
    		double xf = t.getScaleX();    		
    		chi = MathTools.normalize((xf<0? Math.PI: 0) - chi, 0);
    	}
	}

	public void setGap(float gap) {
        this.gap = gap;
        Canvas canvas = getCanvas();
        canvas.relocate(this);
        canvas.repaint();
    }

    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
        Canvas canvas = getCanvas();
        if(canvas!=null) {
            canvas.relocate(this);
            canvas.repaint();
        }
    }

    /**
     * Called by an EditorPane.
     */
    public boolean isCentered_EP() {
        return centered;
    }
    
    public boolean isCentered() {
        return centered;
    }
    
    /**
     * Called by an EditorPane.
     */
    public void setCentered_EP(boolean b) {
        
    	setCentered(b);
    	priorityChangeable = !b;
    	
        if(priorityInfo!=null) priorityInfo.getNotifier().run(this);
        if(angleInfo!=null) angleInfo.getNotifier().run(this);
        if(gapInfo!=null) gapInfo.getNotifier().run(this);

        ((ENode)getGovernor()).arrangeContacts();        
    }

    public void setCentered(boolean centered) {
        
        if(!this.centered && centered) {
	        oldPriority = priority;
	        priority = specialPriority();
        }
        else if(!centered && this.centered){
            priority = oldPriority;
        }
        priorityResettable = centered; // necessary only for codec
        this.centered = centered;
    }

    public boolean canChangePreferredAngleDegrees() {
        return angleChangeable;
    }
    
    public float specialPriority() {
        return Float.POSITIVE_INFINITY;
    }

    public List<EditorEntry> getInfo() {
	    if(info==null) {
	        ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
	        ArrayList<EditorEntry> list = new ArrayList<EditorEntry>();
		    result.addAll(super.getInfo());
		    
		    priorityInfo.requestNotifier();
		    angleInfo.requestNotifier();
		    
		    gapInfo = new EditorEntry("Gap", FLOAT, "Gap");
		    gapInfo.setSpinnerValues(DEFAULT_GAP, -999, 999, 1f, false);		    
		    gapInfo.requestNotifier();
		    list.add(gapInfo);
		    
		    EditorEntry centerInfo = new EditorEntry("Centered_EP", BOOLEAN, "Centered");
		    list.add(centerInfo);
		    
		    EditorEntry textInfo = new EditorEntry("Text", TEXT, null);
		    list.add(textInfo);
		    
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
	    Text[] t = Texdraw.getTexts(code);
	    if(t.length<1) {
	        throw new ParseException(TEX, "Missing text element");
	    }
	    if(t[0].href.equals(Texdraw.CENTER) && t[0].vref.equals(Texdraw.CENTER)) {
	        setCentered(true);
	    }
	    this.text = t[0].text;
	    this.gap = Codec1.decodeFloat(info);
	}

}
