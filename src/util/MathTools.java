package util;

import java.awt.Shape;
import java.awt.geom.*;
import java.awt.geom.Point2D.Float;


public class MathTools {

 
  public static Point2D getPoint(CubicCurve2D c, double t) {
  	return getPoint(c.getP1(), c.getCtrlP1(), c.getP2(), c.getCtrlP2(), t);
  }
  
  
  /*
   * @return the point of the cubic curve defined by the specified points that corresponds to the parameter t.  
   */
  public static Point2D getPoint(Point2D p0, Point2D p0a, Point2D p1, Point2D p1a, double t) {
  	double b0 = bernsteinCoeff(3,0,t);
  	double b0a = bernsteinCoeff(3,1,t);
  	double b1 = bernsteinCoeff(3,3,t);
  	double b1a = bernsteinCoeff(3,2,t);
  	
  	return new Point2D.Float(
  			(float)(b0*p0.getX() + b0a*p0a.getX() + b1*p1.getX() + b1a*p1a.getX()), 
  			(float)(b0*p0.getY() + b0a*p0a.getY() + b1*p1.getY() + b1a*p1a.getY()));
  }
  
  public static double bernsteinCoeff(int n, int m, double t) {
  	return over(n, m) * Math.pow(t, m) * Math.pow(1-t, n-m);
  }
  
  public static double over(int n, int m) {
  	return factorial(n)/(factorial(n-m)*factorial(m));
  }
  
  public static double factorial(int n) {
  	return n==0? 1: n*factorial(n-1);
  }

  public static int alternatePrecision(int precision, int radix) {
	    int m = smallestPrimeIn(radix);
	    double d = 1;
	    while(d<precision) {
	    	d *= radix;
	    }
	    while(d>precision) {
	    	d /= m;
	    }	    
	    return (int)(d*m);
  }      
  

  public static int smallestPrimeIn(int n) {
	    int len = (int)Math.sqrt(n);
	    boolean[] sieve = new boolean[len];
	    boolean found = false;
	    int k = 2;
	    for(; k<len && !found; k++) {
	    	if(!sieve[k]) {
		    	found = n%k==0;
		    	if(!found) {
		    		for(int j = k; j<len; j+=k) {
		    			sieve[j] = true;
		    		}
		    	}
	    	}
	    }	    
	    return found? k-1: n;
  }

  /*
   * @return the approximately closest point to p on the curve c between the t-values t0 and t1 (both should be between 0 and 1) 
   */
  public static double closestTo(Point2D p, CubicCurve2D c, double t0, double t1) {
	return closestTo(p, c, t0, getPoint(c, t0).distance(p), t1, getPoint(c, t1).distance(p), 4);
  }
	
   /* simple search algorithm
    * 
    */
	private static double closestTo(Point2D p, CubicCurve2D c, double t0, double d0, double t1, double d1, int j) {
		if(t0>t1) {
			double t = t0;
			t0 = t1;
			t1 = t;
			double d = d0;
			d0 = d1;
			d1 = d;
		}
		int div = 20;
		float dmin = .5f;
		double incr = (t1 - t0)/div;
		double[] ds = new double[div+1];
		ds[0] = d0;
		ds[div] = d1;
		int first = 0;
		int second = div;
		if(d1<d0) {
			first = div;
			second = 0;			
		}
		for(int i = 1; i<div && ds[first]>dmin; i++) {
			ds[i] = getPoint(c, t0 + i*incr).distance(p);
			if(ds[i]<ds[first]) {
				second = first;
				first = i;
			}
		}

		if(ds[first]<=dmin || j<=0 || (first==0 && second!=1) || (first==div && second!=div-1)) {
			//System.err.println("t: "+(t0+first*incr)+" "+(ds[first]<=dmin)+" "+(j<=0)+" "+(first==0 && second!=1)+" "+(first==div && second!=div-1));
		    return t0 + first*incr;
		}		
		return closestTo(p, c, t0 + first*incr, ds[first], t0 + second*incr, ds[second], j-1);
	}

	  /** Simple search algorithm, with n as a precision parameter: the difference between the t-values of the desired and the returned Point on the curve c
	   * will be at most 2^(-n)/abs(t1-t0). Returns null if the search strays outside the interval [t0..t1]. The search will start at a t-value of t0 and progress in
	   * the direction of t1.
	   * 
	   */
	public static Point2D circleIntersection(Point2D p, double r, CubicCurve2D c, double t0, double t1, int n) {
		return circleIntersection(p, r, c, t0, new double[]{t1}, n);
	}
	
	  /** Like <code>circleIntersection(Point2D, double, c, t0, t1, n)</code>, but returns the corresponding t-value as the 
	   * first element of the supplied array.
	   * 
	   */
	public static Point2D circleIntersection(Point2D p, double r, CubicCurve2D c, double t0, double[] u, int n) {
	    double t = t0, t1 = u[0];
	    //boolean _b = n==3;
		boolean a = t0>t1;
		Point2D p1 = null;
		double d = 0;
		for(int k = 1; k<=n; k++) {
			if(p1==null || a==t>t0 || a==t<t1) {
				if(a==t>t0) {
					t = t0;
				}
				else if(a==t<t1) {
					t = t1;
				}
				p1 = getPoint(c, t);
				d = p.distance(p1);
			}
			double dt = (a? -1: 1)*(t0==t1? 1: Math.abs(t0-t1))/Math.pow(4, k);
			boolean b = d>r;
			while((!a? t>=t0 && t<=t1: t<=t0 && t>=t1) && d!=r && d>r==b) {
				t = t + (b? -dt: dt);
				p1 = getPoint(c, t);
				d = p.distance(p1);
				//if(_b) System.err.println("limits "+t0+" "+t1+"  t: "+t);
			} 
			if(d==r) {
				break;
			}
			else if(d>r==b && (!a? t<t0 || t>t1: t>t0 || t<t1)) {
				p1 = null; // no result
			    break;
			}
		}
		//if(_b) System.err.println(" result: "+t);
		u[0] = t;
		return p1;
	}
	
	/**
	 * 
	 * @param u initially contains the starting position, will contain the end position
	 * @param distance approximate path-length from start to end position 
	 * @param c the curve to be travelled
	 * @param dt the step size 
	 * @param cutOff the maximum number of iterations allowed
	 * @return the point at the end position
	 */
	public static Point2D travel(double[] u, double distance, CubicCurve2D c, double dt, int cutOff) {
	    return travel(u, distance, c, dt, cutOff, Integer.MAX_VALUE);
	}
	
	/**
	 * 
	 * @param u initially contains the starting position, will contain the end position
	 * @param distance approximate path-length from start to end position 
	 * @param c the curve to be travelled
	 * @param dt the step size 
	 * @param cutOff the maximum number of iterations allowed
	 * @param maxK the maximum number of steps that can be compounded into one
	 * @return the point at the end position
	 */
	public static Point2D travel(double[] u, double distance, CubicCurve2D c, double dt, int cutOff, int maxK) {
	    double t0 = u[0];
		double t = t0;
		Point2D p = getPoint(c, t0);
		Point2D p1 = p;
		double dist = 0;
		boolean done = false;
		int i = 0;
		float safetyFactor = 1.2f;
		float sfAdd = 0f;
		int n = 0;
		while(!done) {
			Point2D p_ = null;
			double t_ = t;
		    double d = 0;
			int k = 1;
			int k_ = k;
			while(dist<distance && i<cutOff) {
			    t_ = t;
			    t += k*dt;

			    p1 = getPoint(c, t);
				d = p.distance(p1);
				p_ = p;
				p = p1;
			    
				dist += d;
				
			    i += k;
			    k_ = k;
			    k = (int)Math.min(maxK, Math.max(1, (distance-dist)*k/d/(safetyFactor+sfAdd)));
			    sfAdd = 0;
			    
				//System.err.println("k: "+k+"  dist: "+dist);
			}
			if(k_>1) {
			    dist -= d;
			    i -= k_;
			    t = t_;
			    p = p_;
			    n++;
			    sfAdd = n*.1f;
			    //System.err.println("sf: "+(safetyFactor+sfAdd));
			}
			else done = true;
		}
		//System.err.println("result: "+t);
		u[0] = t;
		return p1;
	}
	
	
	public static CubicCurve2D.Float invert(CubicCurve2D.Float c) {
        Point2D.Float p0 = (Float)c.getP1();
        Point2D.Float p0a = (Float)c.getCtrlP1();
        Point2D.Float p1a = (Float)c.getCtrlP2();
        Point2D.Float p1 = (Float)c.getP2();
        
        return new CubicCurve2D.Float(p1.x, p1.y, p1a.x, p1a.y, p0a.x, p0a.y, p0.x, p0.y);
	}
  
	public static CubicCurve2D.Double invert(CubicCurve2D.Double c) {
        Point2D.Double p0 = (Point2D.Double)c.getP1();
        Point2D.Double p0a = (Point2D.Double)c.getCtrlP1();
        Point2D.Double p1a = (Point2D.Double)c.getCtrlP2();
        Point2D.Double p1 = (Point2D.Double)c.getP2();
        
        return new CubicCurve2D.Double(p1.x, p1.y, p1a.x, p1a.y, p0a.x, p0a.y, p0.x, p0.y);
	}
		
	/**
	 * Normalize the specified angle with respect to a reference angle alpha. The returned angle will lie between alpha-PI 
	 * and alpha+PI.
	 * @param angle
	 * @param alpha
	 * @return
	 */
	public static double normalize(double angle, double alpha) {
		return angle>=alpha+Math.PI? angle - 2*Math.PI*(1+(int)((angle-alpha-Math.PI)/2/Math.PI)): 
			   angle<alpha-Math.PI? angle + 2*Math.PI*(1+(int)((alpha-Math.PI-angle)/2/Math.PI)): 
			   angle;
	}
	
	/*
	 * difference between a and orientation, in positive direction. Ranges over [0..2*Math.PI[
	 */
	public static double d(double a, double b) {
	    double epsilon = 1e-12;
		double d = MathTools.normalize(b, a+Math.PI) - a;
		if(Math.abs(d - 2d*Math.PI)<epsilon) {
			d = 0;
		}
		return d;
	}
	
	/**
	 * Returns the counterclockwise-measured angle of the vector connecting p0 and p1.
	 */
	public static double angle(Point2D p0, Point2D p1) {
		double dx = p1.getX() - p0.getX();
        double dy = p1.getY() - p0.getY();
        return dy==0? dx>0? 0: Math.PI: -Math.signum(dy)*Math.PI/2 + Math.atan(dx/dy);
  	}
	
    public static double getRotation(AffineTransform t) {
        Point2D.Double srcPt = new Point2D.Double(1, 0);
        Point2D.Double dstPt = new Point2D.Double();   
        t.deltaTransform(srcPt, dstPt);        
        return dstPt.y==0? 0: -Math.signum(dstPt.y)*Math.PI/2 + Math.atan(dstPt.x/dstPt.y);
    }
	
    public static boolean isMereTranslation(AffineTransform t) {
        return 0==t.getShearX() && 0==t.getShearY() && 1==t.getScaleX() && 1==t.getScaleY();
    }
    
    /** 
     * Solves a system of three linear equations with three variables, using Cramer's rule.
     * The fourth parameter should be of length 3 and will contain the solution values, or Double.NaN if the system does 
     * not have a unique solution.
     * @return true iff the system is solveable.
     */
    public static boolean solve(double[] a, double[] b, double[] c, double[] d, double[] solution)	{	
		double aDet = getDeterminate(d,b,c);
		double bDet = getDeterminate(a,d,c);
		double cDet = getDeterminate(a,b,d);
		double dDet = getDeterminate(a,b,c);
		boolean result = false;
		
		if(dDet==0)	{	
			for(int i = 0, n = solution.length; i<n; i++) {
				solution[i] = Double.NaN;
			}
			if(aDet==0 && bDet==0 && cDet==0){ // solution space is infinite
				result = true;
			}
		}   
		else {
			result = true;
			solution[0] = aDet/dDet;
			solution[1] = bDet/dDet;
			solution[2] = cDet/dDet;
		}
		return result;
	}
	
	public static double getDeterminate(double[] a, double[] b, double[] c) {
		double det=0;
		det=a[0]*b[1]*c[2]
			+b[0]*c[1]*a[2]
			+c[0]*a[1]*b[2]
			-a[2]*b[1]*c[0]
			-b[2]*c[1]*a[0]
			-c[2]*a[1]*b[0];
		return det;
	}

    /** 
     * Solves a system of two linear equations with three variables, using Cramer's rule.
     * The third parameter should be of length 2 and will contain the solution values, or Double.NaN if the system does 
     * not have a unique solution.
     * @return true iff the system is solveable.
     */
    public static boolean solve(double a0, double a1, double b0, double b1, double c0, double c1, double[] solution)	{	
		double aDet = getDeterminant(c0, c1, b0, b1);
		double bDet = getDeterminant(a0, a1, c0, c1);
		double cDet = getDeterminant(a0, a1, b0, b1);
		boolean result = false;
		
		if(cDet==0)	{	
			for(int i = 0, n = solution.length; i<n; i++) {
				solution[i] = Double.NaN;
			}
			if(aDet==0 && bDet==0){ // solution space is infinite
				result = true;
			}
		}   
		else {
			result = true;
			solution[0] = aDet/cDet;
			solution[1] = bDet/cDet;
		}
		return result;
	}
	
	public static double getDeterminant(double a0, double a1, double b0, double b1) {
		double det=0;
		det=a0*b1 - a1*b0;
		return det;
	}
	
	public static Shape round(CubicCurve2D c) {
		return new CubicCurve2D.Float(Math.round(c.getX1()), Math.round(c.getY1()), 
				Math.round(c.getCtrlX1()), Math.round(c.getCtrlY1()),
				Math.round(c.getCtrlX2()), Math.round(c.getCtrlY2()), 
				Math.round(c.getX2()), Math.round(c.getY2()));
	}

	public static Shape round(Line2D l) {
		return new Line2D.Float(Math.round(l.getX1()), Math.round(l.getY1()),
				Math.round(l.getX2()), Math.round(l.getY2()));
	}
	
	public static Shape round(Arc2D a) {
		return new Arc2D.Float(Math.round(a.getX()), Math.round(a.getY()), (float)a.getWidth(), (float)a.getHeight(), 
				(float)a.getAngleStart(), (float)a.getAngleExtent(), a.getArcType());
	}
	
	public static Shape round(Shape s) {
		if(s instanceof CubicCurve2D) {
			return round((CubicCurve2D) s);
		} else if(s instanceof Line2D) {
			return round((Line2D) s);
		} else if(s instanceof Arc2D) {
			return round((Arc2D) s);
		} else {
			throw new IllegalArgumentException("Cannot round Shape");
		}
	}


}