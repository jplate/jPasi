/*
 * Created on 15.12.2006
 *
 */
package jPasi.codec;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Jan Plate
 *
 */
public class Texdraw {
    
    public static class Arc extends Shape {
        public float radius;
        public float start;
        public float end;
        public Arc(Point2D p, float r, float st, float ed) {
            location = (Point2D.Float)p;
            radius = r;
            start = st;
            end = ed;
        }
        public String genericDescription() {
            return "an arc";
        }
        public boolean isIndependent() {
            return true;
        }
        public String toString() {
            return "(Arc: "+location+", r:"+radius+", st:"+start+", ed:"+end+")";
        }
    }
    public static class Circle extends Shape {
        public float radius;
        public boolean filled;
        public float fillLevel;
        public Circle(Point2D p, float r, boolean f, float fl) {
            location = (Point2D.Float)p;
            radius = r;
            filled = f;
            fillLevel = fl;
        }
        public String genericDescription() {
            return "a circle";
        }
        public boolean isIndependent() {
            return true;
        }
        public String toString() {
            return "(Circle: "+location+", r:"+radius+", f:"+fillLevel+")";
        }
    }
    public static class CubicCurve extends Shape implements Fillable {
        public Point2D.Float p1;
        public Point2D.Float p1a;
        public Point2D.Float p2a;
        public Point2D.Float p2;
        public boolean filled;
        public float fillLevel;

        public CubicCurve() {
        }
        public CubicCurve(Point2D p1, Point2D p1a, Point2D p2a, Point2D p2) {
            this.p1 = (Point2D.Float)p1;
            this.p1a = (Point2D.Float)p1a;
            this.p2a = (Point2D.Float)p2a;
            this.p2 = (Point2D.Float)p2;
        }
        
        public String genericDescription() {
            return "a curve";
        }
        public float getFillLevel() {
            return fillLevel;
        }
        public boolean isFilled() {
            return filled;
        }
        public void setFilled(boolean b) {
            this.filled = b;
        }
        public void setFillLevel(float l) {
            this.fillLevel = l;
        }
        public String toString() {
            return "(CubicCurve: "+p1+", "+p1a+", "+p2a+", "+p2+(filled? "filled": "")+")";
        }
        public Shape transform(int ch) {
            p1.y = ch-p1.y;
            p1a.y = ch-p1a.y;
            p2a.y = ch-p2a.y;
            p2.y = ch-p2.y;
            return this;
        }
    }
    public static interface Fillable {
	    public float getFillLevel();
	    public boolean isFilled();
	    public void setFilled(boolean b);
	    public void setFillLevel(float l);
	} 
    public static class Line extends Shape {
        public Point2D.Float p1;
        public Point2D.Float p2;
        public Line(Point2D p1, Point2D p2) {
            this.p1 = (Point2D.Float)p1;
            this.p2 = (Point2D.Float)p2;
        }
        public String genericDescription() {
            return "a line";
        }
        public String toString() {
            return "(Line: "+p1+", "+p2+")";
        }
        public Shape transform(int ch) {
            p1.y = ch-p1.y;
            p2.y = ch-p2.y;
            return this;
        }
    } 
    public static class Path extends Shape implements Fillable {
        public Shape[] shapes;
        public boolean drawn;
        public boolean filled;
        public float fillLevel;
        public Path(Shape[] s, boolean d, boolean f, float fl) {
            shapes = s;
            drawn = d;
            filled = f;
            fillLevel = fl;
        }
        public String genericDescription() {
            return "a curve/line sequence";
        }
        public float getFillLevel() {
            return fillLevel;
        }
        public boolean isFilled() {
            return filled;
        }
        public boolean isIndependent() {
            return true;
        }
        public void setFilled(boolean b) {
            this.filled = b;
        }
        public void setFillLevel(float l) {
            fillLevel = l;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i<shapes.length; i++) {
                sb.append(shapes[i].toString()+(i<shapes.length-1? ", ": ""));
            }
            return "(Path: "+(drawn? "drawn": "")+(filled? drawn? ", filled": "filled": "")+sb.toString()+")";
        }
    }
    public abstract static class Shape {
        public Point2D.Float location;
        public String genericDescription() {
            return "a shape";
        }
        public boolean isIndependent() {
            return false;
        }
        /**
         * Transforms the y coordinate of this Shape and returns the result.
         */
        public Shape transform(int ch) {
            location.y = ch-location.y;
            return this;
        }
    }

	public static class Stroke {
        public float lineWidth;
        public float[] pattern;
        public Stroke(float lw, float[] p) {
            lineWidth = lw;
            pattern = p;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if(pattern!=null) {
	            for(int i = 0; i<pattern.length; i++) {
	                sb.append(Float.toString(pattern[i])+(i<pattern.length-1? ", ": ""));
	            }
            }
            return "(Stroke: "+lineWidth+" ("+sb+"))";
        }
    }
    
    public static class StrokedShape {
        public Stroke stroke;
        public Shape shape;
        public StrokedShape(Shape s, Stroke st) {
            stroke = st;
            shape = s;
        }
        public String toString() {
            return shape.toString()+", "+stroke.toString();
        }
    }
    
    public static class Text {
        public String text;
        public String vref;
        public String href;
        public Point2D.Float location;
        public Text(String href, String vref, float x, float y, String text) {
            this.text = text;
            this.href = href;
            this.vref = vref;
            location = new Point2D.Float(x, y);
        }
        public String genericDescription() {
            return "a text element";
        }
    }
    
    private static Pattern shapePattern = Pattern.compile( 
            "(.*?)(?:\\\\lvec\\s*\\((\\S+)\\s+(\\S+)\\)|" + // 2,3: p1
    		"\\\\clvec\\s*\\((\\S+)\\s+(\\S+)\\)\\s*\\((\\S+)\\s+(\\S+)\\)\\s*\\((\\S+)\\s+(\\S+)\\)|" + // 4,5: p1a; 6,7: p2a; 8,9: p2
        	"\\\\larc\\s+r:(\\S+)\\s+sd:(\\S+)\\s+ed:(\\S+)|" + // 10: radius; 11: start; 12: end
        	"(?:\\\\lcir|\\\\fcir\\s+f:(\\S+))\\s+r:(\\S+))"); // 13: fill; 14: radius 
    
    private static Pattern textPattern = Pattern.compile(
    		"\\\\textref\\s+h:(.)\\s+v:(.)(?!.*\\\\textref.*).*\\\\htext\\s*\\((\\S+)\\s+(\\S+)\\)\\{(.*)\\}");

    private static Pattern linewdPattern = Pattern.compile(".*\\\\linewd\\s+(\\S+)");

    private static Pattern movePattern = Pattern.compile(".*\\\\move\\s*\\((\\S+)\\s+(\\S+)\\)");
    
    private static Pattern movePattern1 = Pattern.compile("\\\\move\\s*\\((\\S+)\\s+(\\S+)\\)");
    
    private static Pattern lpattPattern = Pattern.compile(".*\\\\lpatt\\s+\\(([\\s0-9.]*)\\)");
    
    private static Pattern fillPattern = Pattern.compile(".*\\\\(i|l)fill\\s+f:([\\s0-9.]*)");
       
    
    public static final String TOP = "T",
    						   LEFT = "L",
    						   RIGHT = "R",
    						   BOTTOM = "B",
    						   CENTER = "C";
    
    public static String arc(float r, float sd, float ed) {
        return "\\larc r:"+encodeFloat(r)+" sd:"+encodeFloat(sd)+" ed:"+encodeFloat(ed)+" ";
    }

    public static String circ(float r) {
        return "\\lcir r:"+encodeFloat(r)+" ";
    }
    
    public static String curve(float p0ax, float p0ay, float p1ax, float p1ay, float p1x, float p1y) {
        return "\\clvec("+encodeFloat(p0ax)+" "+encodeFloat(p0ay)+")("+encodeFloat(p1ax)+" "+
        	encodeFloat(p1ay)+")("+encodeFloat(p1x)+" "+encodeFloat(p1y)+") ";        
    }
    
    protected static float decodeFloat(String s) throws ParseException {    	
    	return Codec1.decodeFloat(s);
    }
    
    protected static String encodeFloat(float f) {
    	return Codec1.encodeFloat(f);
    }

    public static String fcirc(float r, float f) {
        return "\\fcir f:"+encodeFloat(f)+" r:"+encodeFloat(r)+" ";
    }
    
    public static StrokedShape[] getStrokedShapes(String s, float defaultLineWidth) throws ParseException {
        Pattern pp = Pattern.compile("(\\S+)\\s*");
        
        Matcher m = shapePattern.matcher(s);
		
        LinkedList<StrokedShape> l = new LinkedList<StrokedShape>(); // list of Shapes, incl. Paths
		LinkedList<Shape> lx = new LinkedList<Shape>(); // current list of Shapes, possibly elements of a Path
	    float lw = defaultLineWidth;
	    float[] pattern = null;
		Stroke stroke = new Stroke(lw, pattern);
		Point2D.Float currentPoint = null;
		Point2D.Float newPoint = null;
		
		while(m.find() || lx.size()>0) {
		    //for(int i = 0; i<1; i++) {System.err.println(" "+i+": "+m.group(i));}
		    
		    boolean found = true;
		    int end = 0;
		    try {
		        end = m.end();
		    } catch(IllegalStateException ise) {
		        found = false;
		    }
		    String toSearch = found? m.group(1): s.substring(end);
		    
		    Matcher mF = fillPattern.matcher(toSearch);
		    Matcher mL = linewdPattern.matcher(toSearch);
		    Matcher mM = movePattern.matcher(toSearch);
		    Matcher mP = lpattPattern.matcher(toSearch);
		    
		    boolean pathClosed = false;
		    boolean filled = false;
		    float fillLevel = 0;
		    boolean drawn = true;
		    if(mF.find()) {
		        pathClosed = true;
		        filled = true;
		        fillLevel = decodeFloat(mF.group(2));
		        drawn = mF.group(1).equals(l);
		    }
		    if(mM.find()) { // move command	        
		        pathClosed = true;
	        	currentPoint = new Point2D.Float(decodeFloat(mM.group(1)), decodeFloat(mM.group(2)));
		        newPoint = currentPoint;
		    }		    
		    if(mL.find()) {
		        pathClosed = true;
		        lw = decodeFloat(mL.group(1));
		    }
		    if(mP.find()) { // pattern command
		        pathClosed = true;
	            Matcher mp = pp.matcher(mP.group(1));
	            LinkedList<Float> pl = new LinkedList<Float>();
	            while(mp.find()) {
	                pl.add(decodeFloat(mp.group(1)));
	            }
	            if(pl.size()>0) {
		            pattern = new float[pl.size()];
		            int j = 0;
		            for(Iterator<Float> i = pl.iterator(); i.hasNext();) {
		                pattern[j++] = i.next().floatValue();
		            }
	            } else { 
	            	pattern = null; // no numbers found, so it's a solid line
	            }
		    }
		    if(pathClosed || !found) {
		        int lxs = lx.size();
		        //System.err.println("lx: "+lxs+" "+lx);
	            if(lxs>0) {
	                Shape s0 = lx.get(0);
	                //System.err.println("s0: "+s0+" "+filled);
	                if(lxs==1 && s0 instanceof Fillable) {
	                    ((Fillable)s0).setFilled(filled);
	                    ((Fillable)s0).setFillLevel(fillLevel);
	                }
			        StrokedShape ss = new StrokedShape(
			                lxs>1?
		                    	new Path(lx.toArray(new Shape[] {}), 
		                    	        drawn, filled, fillLevel):
		                    	s0, 
		                    stroke);
			        l.add(ss);
		        }
		        lx.clear();
		        pathClosed = false;
		    }
		    
		    if(found) {
			    if(currentPoint==null) {
			        int max = 50;
			        throw new ParseException("No starting point specified in: "+s.substring(0, max)+
			                (s.length()>max+1? "...": ""));
			    }
			    
			    stroke = new Stroke(lw, pattern);
			    Shape sh = null;		    
			    if(m.group(2)!=null) {
			        newPoint = new Point2D.Float(decodeFloat(m.group(2)), decodeFloat(m.group(3)));
			        sh = new Line(currentPoint, newPoint);
			    }
			    else if(m.group(4)!=null) {
			        newPoint = new Point2D.Float(decodeFloat(m.group(8)), decodeFloat(m.group(9)));
			        sh = new CubicCurve(currentPoint,
			                new Point2D.Float(decodeFloat(m.group(4)), decodeFloat(m.group(5))),
			                new Point2D.Float(decodeFloat(m.group(6)), decodeFloat(m.group(7))),
			                newPoint);
			    }
			    else if(m.group(10)!=null) {
			        sh = new Arc(currentPoint,
			                decodeFloat(m.group(10)),
			                decodeFloat(m.group(11)),
			                decodeFloat(m.group(12)));
			    }
			    else if(m.group(14)!=null) {
				    boolean filled1 = m.group(13)!=null && m.group(13).length()>0;
				    sh = new Circle(currentPoint,
				            decodeFloat(m.group(14)), filled1, 
				            filled1? decodeFloat(m.group(13)): 1);
			    }
			    else {
			        throw new Error("Pattern should not have matched "+s);
			    }
			    
			    if(sh!=null) {
			        if(sh.isIndependent()) {
			            l.add(new StrokedShape(sh, stroke));
			        } else {
			            lx.add(sh);
			        }
			        //System.err.println(sh);
			    }
			    currentPoint = (Point2D.Float)newPoint.clone();
		    }
		}
		/*
		System.err.println("Shapes:");
	    for(Iterator i = l.iterator(); i.hasNext();) {
	        System.err.println((StrokedShape)i.next());
	    }
	    */
		return l.toArray(new StrokedShape[]{});        
    }

    public static Text[] getTexts(String s) throws ParseException{
        Matcher m = textPattern.matcher(s);
        LinkedList<Text> l = new LinkedList<Text>();
        while(m.find()) {
            l.add(new Text(m.group(1), m.group(2),
                    decodeFloat(m.group(3)),
                    decodeFloat(m.group(4)),
                    m.group(5)));
        }
        return l.toArray(new Text[]{});
    }
    
    public static float[] getUnitScales(String s)  throws ParseException {
        Pattern p = Pattern.compile("\\\\setunitscale\\s+(\\S+)");
        Matcher m = p.matcher(s);
        LinkedList<Float> l = new LinkedList<Float>();
        while(m.find()) {
            l.add(Float.valueOf(decodeFloat(m.group(1))));
        }
        float[] result = new float[l.size()];
        for(int i = 0; i<result.length; i++) {
            result[i] = l.get(i).floatValue();
        }
        return result;
    }
    
    public static boolean hasOwnLinePattern(String s) {
        Matcher m = shapePattern.matcher(s);
        return m.find() && lpattPattern.matcher(m.group(1)).find();
    }
    
    public static String htext(float px, float py, String text) {
        return "\\htext("+encodeFloat(px)+" "+encodeFloat(py)+")"+"{"+text+"} ";
    }
    
    public static String ifill(float level) {
        return "\\ifill f:"+encodeFloat(level)+" ";
    }
    
    public static Point2D.Float lastPoint(String s) throws ParseException {
        Pattern p = Pattern.compile(".*\\((\\S+)\\s+(\\S+)\\)");
        Matcher m = p.matcher(s);
        return m.find()? new Point2D.Float(decodeFloat(m.group(1)), decodeFloat(m.group(2))): null;
    }

    public static String lfill(float level) {
        return "\\lfill f:"+encodeFloat(level)+" ";
    }
    
    public static String line(float x, float y) {
        return "\\lvec("+encodeFloat(x)+" "+encodeFloat(y)+") ";
    }
    
    public static String linePattern(float[] p) {
        StringBuilder sb = new StringBuilder("\\lpatt (");
        if(p!=null) {
            for(int i = 0; i<p.length; i++) {        
                sb.append(encodeFloat(p[i]) + (i<p.length-1? " ": ""));
            }
        }
        return sb.append(") ").toString();
    }

    public static String lineWidth(float lw) {
        return "\\linewd "+encodeFloat(lw)+" ";
    }

    public static String move(float x, float y) {
        return "\\move("+encodeFloat(x)+" "+encodeFloat(y)+") ";
    }
    
    public static Point2D.Float movePoint(String s) throws ParseException {
        Matcher m = movePattern1.matcher(s);
        return m.find()? new Point2D.Float(decodeFloat(m.group(1)), decodeFloat(m.group(2))): null;
    }
    
    public static String textref(String horizontal, String vertical) {
        return "\\textref h:"+horizontal+" v:"+vertical+" ";
    }
}
