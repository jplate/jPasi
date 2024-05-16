/*
 * Created on 24.06.2007
 *
 */
package jPasi.item;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Map;

import jPasi.Canvas;
import util.MathTools;

public class Axis extends Item {

    private static final long serialVersionUID = 1382619166373339950L;
    
    protected Point2D point;
    protected double theta;
    private Shape[] shapes;
    protected int canvasWidth;
    protected int canvasHeight;

	protected float arrowHookLength = 7;
	protected double arrowHookAngle = .2;
    
    public Axis() {
    }
    
    public Axis(Canvas c, Point2D p, double a) {
        super(c);
        this.point = p;
        this.theta = a;
        stroke = new BasicStroke(.3f);
        getShapes();
    }
    
    public void adjust(Point2D center, double angle) {
		this.point = center;
		this.theta = angle;
		invalidate();
	}
    
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
    	Axis clone = (Axis)super.clone(map);
    	clone.setVisible(isVisible());
    	return clone;
    }
    
    private Shape getArrowHead(Line2D line, double theta) {
	    /*
	     * Determine endpoint.
	     */		
		Point2D p1 = line.getP1();
	    Point2D p2 = line.getP2();
	    Point2D left;
	    Point2D right;
	    Point2D low;
	    Point2D high;
	    {
		    double x2 = p2.getX();
		    double y2 = p2.getY();
		    double x1 = p1.getX();
		    double y1 = p1.getY();
		    if(x1>x2) {
		    	right = p1;
		    	left = p2;
		    } else {
		    	right = p2;
		    	left = p1;
		    }
		    if(y1>y2) {
		    	low = p1;
		    	high = p2;
		    } else {
		    	low = p2;
		    	high = p1;
		    }
	    }
	    theta = MathTools.normalize(theta, 0);
	    Point2D p = theta==0? right: 
	    			theta>0 && theta<Math.PI? high: 
	    			theta==-Math.PI? left: low;  
	    float x = (float)p.getX();
	    float y = (float)p.getY();
	    		
	    /*
	     * Construct path.
	     */
	    float r = arrowHookLength ;
	    double a = arrowHookAngle;
	    float x1 = x - (float)Math.cos(theta+a)*r;
	    float y1 = y + (float)Math.sin(theta+a)*r;
	    float x2 = x - (float)Math.cos(theta-a)*r;
	    float y2 = y + (float)Math.sin(theta-a)*r;
	    float xc = x - (float)Math.cos(theta)*r/2;
	    float yc = y + (float)Math.sin(theta)*r/2;
	    
	    GeneralPath path = new GeneralPath();
		path.moveTo(x, y);
		path.curveTo(xc, yc, xc, yc, x1, y1);
		path.lineTo(x2, y2);
		path.curveTo(xc, yc, xc, yc, x,y);	    
	    
		return path;
    }

	public Rectangle getBounds() {
    	return new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
    }

	public Line2D getLineThrough(Point2D c, double a) {
        a = MathTools.normalize(a, 0);
        
        double x = c.getX();
        double y = c.getY();
        double w = getWidth();
        double h = getHeight();
        
        double a0 = a<0? a+Math.PI: a;
        double b00 = x==w? Math.PI/2: Math.atan(y/(w-x));
        double b01 = x==0? Math.PI/2: Math.PI - Math.atan(y/x);
        double r0 = Math.tan(a0);
        double dx0 = 0;
        double dy0 = 0;
        if(a0<b00) {
        	dx0 = w - x;
        	dy0 = -r0*dx0;
        } else if(a0<b01) {        
	        dy0 = -y;
	        dx0 = -dy0/r0;
        } else {
            dx0 = -x;
            dy0 = -r0*dx0;
        }

        double a1 = a>=0? a-Math.PI: a;
        double b10 = x==w? -Math.PI/2: -Math.atan((h-y)/(w-x));
        double b11 = x==0? -Math.PI/2: -Math.PI + Math.atan((h-y)/x);
        double r1 = Math.tan(a1);
        double dx1 = 0;
        double dy1 = 0;
        if(a1<b11) {
        	dx1 = -x;
        	dy1 = -r1*dx1;
        } else if(a1<b10) {        
	        dy1 = h - y;
	        dx1 = -dy1/r1;
        } else {
            dx1 = w - x;
            dy1 = -r1*dx1;
        }
        
        return new Line2D.Double(x+dx0, y+dy0, x+dx1, y+dy1);
    }

	public Shape[] getShapes() {	    
		if(shapes==null) {
	        this.canvasWidth = canvas.getWidth();
	        this.canvasHeight = canvas.getHeight();
	        setBounds2D(0, 0, canvasWidth, canvasHeight);
			
			Shape[] shapes = new Shape[2];
	    	shapes[0] = getLineThrough(point, theta);
	    	shapes[1] = getArrowHead((Line2D)shapes[0], theta);
	    	this.shapes = shapes;
	    }
	    return shapes;
    }
	
	public String getTexdrawCode(int canvasHeight) {
		return null;
	}

	public void invalidate() {
		super.invalidate();
		shapes = null;
	}

	public void paint(Graphics g) {
		if(canvasWidth!=canvas.getWidth() || canvasHeight!=canvas.getHeight()) {
			shapes = null;
		}
		
        Graphics2D g2 = (Graphics2D)g;
        Shape[] shapes = getShapes();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setPaint(paint);
		
		g2.setStroke(stroke);
		g2.draw(shapes[0]);
		g2.fill(shapes[1]);
        g2.dispose();
    }
	
	public String toString() {
		return "Axis "+hashCode();
	}
	
}
