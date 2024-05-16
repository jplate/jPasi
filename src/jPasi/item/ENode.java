/*
 * Created on 22.10.2006
 *
 */
package jPasi.item;

import static jPasi.codec.CodePortion.HINT;
import static jPasi.codec.CodePortion.TEX;
import static jPasi.edit.EditorEntry.Type.RESET;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import jPasi.Canvas;
import jPasi.TransformModifier;
import jPasi.codec.Codec1;
import jPasi.codec.ParseException;
import jPasi.codec.Texdraw;
import jPasi.edit.EditorEntry;
import jPasi.item.group.Group;
import jPasi.item.group.GroupMember;
import jPasi.item.group.OrderedGroup;
import jPasi.item.group.StandardGroup;
import util.DimensionF;
import util.MathTools;

/**
 * @author Jan Plate
 *
 */
public class ENode extends IndependentItem<StandardGroup<GroupMember<?>>> {

    private static final long serialVersionUID = 4639593899289796984L;
    
    public static final float DEFAULT_RADIUS = 10;
	public static final double D0 = 2*Math.PI/100; // absolute minimal angle between two contact points on the periphery of an Entity
	public static final double D1 = 2*Math.PI/12; // 'comfortable' angle between two contact points on the periphery of an Entity
	public static final int HALF_DISTANCE_PENALTY = 48;
	public static final int SWITCH_PENALTY = 16;
    public static final double SWITCH_TOLERANCE = 0.1;
	public static final int DISTANCE_PENALTY = 4;
	public static final int CLOSENESS_TO_BASE_ANGLE_PENALTY = 9;
	
	private static final float LINE_WIDTH_FACTOR = .9f;
    
	public static float negFactor = .75f;
	
	private Thread contactArranger = null;	
	private volatile Contact currentContact;
	
	protected boolean negated;
	protected boolean negatable;
	
	protected static class Contact implements Comparable<Contact> {

		public static final Comparator<Contact> priorityComparator = new Comparator<Contact>() { 
			public int compare(Contact o0, Contact o1) {
				float p0 = o0.p;
				float p1 = o1.p;
				if(!Float.isNaN(p0) && !Float.isNaN(p1)) { 
					return p0<p1? -1: p0==p1? 0: 1; 
				}
				else if(!Float.isNaN(p0)) {
					return -1;
				}
				else if(!Float.isNaN(p1)) {
					return 1;
				}
				else return 0;
			}
		};

		public DependentItem s = null;
		public int k = 0; // the place of Entity.this in this.s, as represented by this Contact
		public float p; // the priority of this Contact, corresponding to that of this.s

		private double a = 0; // angle, if State is null
		private boolean fixed = false;
		
		public Contact(double a, float p) {
			this.a = a;
			this.p = p;
		}
		
		public Contact(DependentItem s, int i, float p) {
			this.s = s;
			this.k = i;
			this.p = p;
		}

		public double getAngle() {
			return MathTools.normalize(s==null? a: fixed? s.getAngle(this.k): s.getPreferredAngle(this.k), 0);
		}
		
		public int compareTo(Contact c) {
			double a0 = getAngle();
			double a1 = c.getAngle();
			return a0<a1? -1: a0==a1? 0: 1;
		}
		
		@Override
        public String toString() {
			return "(s: "+s+" a: "+getAngle()+" p: "+p+")";
		}
		
		boolean isFixed() {
			return fixed;
		}
		
		void setFixed(boolean fixed) {
			this.fixed = fixed;
		}
	}

	protected class Circumference {
		
		class Iterator implements java.util.Iterator<Contact> {
			private boolean reverse = false;
			private int index = 0;
			private int start = 0;
			private double a;
			private boolean hasNext = false;
			
			protected Iterator(double a, boolean reverse) { // assumption: v is sorted
				this.a = MathTools.normalize(a, 0);
				this.reverse = reverse;
				int s = contacts.size();
				if(s>0) { // find start index
					hasNext = true;
					boolean b = false;
					int i = reverse? s-1: 0;
					int incr = reverse? -1: 1;
					for(; (reverse? i>=0: i<s) && !b; i+=incr) {
						Contact c = contacts.get(i);
						double ca = c.getAngle(); 
						b = reverse? ca<=a: ca>=a; 
					}
					index = b? i-incr: i>=s? 0: s-1;
					start = index;
				}
			}
			
			public Contact next() {
				if(!hasNext) {
					throw new NoSuchElementException();
				}
				Contact c = contacts.get(index);
				index+=(reverse? -1: 1);
				if(index<0) {
					index = contacts.size()-1; 
			}
				else if(index>=contacts.size()) {
					index = 0;
				}
				hasNext = index!=start;
				return c;
			}
			
			public boolean hasNext() {
				return hasNext;
			}
			
			public void remove() {
				contacts.remove(index);
			}
		}
		
		private ArrayList<Contact> contacts;
		
		public Circumference() {
			this.contacts = new ArrayList<Contact>();
		}
		
		public Circumference(ArrayList<Contact> v) {
			this.contacts = v;
		}
		
		public void add(Contact c) {
			contacts.add(c);
		}
		
		public void addAll(Collection<Contact> c) {
			contacts.addAll(c);
		}
		
		public boolean contains(Contact c) {
			return contacts.contains(c);
		}
		
		public Iterator iterator(double a, boolean reverse) { // v should be sorted by angle first
			return new Iterator(a, reverse);
		}
		
		public java.util.Iterator<Contact> iterator() { 
			return contacts.iterator();
		}
		
		public void sortByAngle() {
			Collections.sort(contacts);
		}
		
		public void sortByPriority() {
			Collections.sort(contacts, Contact.priorityComparator);			
		}
		
		public Contact[] getContacts() {
			return (Contact[])contacts.toArray();
		}
		
		@Override
        public String toString() {
			StringBuilder sb = new StringBuilder("CC ");
			for(java.util.Iterator<Contact> i = iterator(); i.hasNext();) {
				sb.append(i.next());
			}
			return sb.toString();
		}
	}

	public ENode() {
	    this(null, null, false);
	}

    public ENode(Canvas canvas, Item governor, boolean hidden) {	
		super(canvas, governor, hidden);	
        radius = getDefaultRadius();
		shading = getDefaultShading();
        setResizeable(true);
		setSizeWhenNotHidden(new DimensionF(2*radius, 2*radius));
	}
	
	
	@Override
    public void addDependentItem(DependentItem s) {
		super.addDependentItem(s);
		if(hidden) setHidden(false);
	}

    /* (non-Javadoc)
     * @see util.MapCloneable#clone(java.util.Map)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException {
	    ENode clone = (ENode)super.clone(map);
	    if(clone!=this && group!=null) {
	    	clone.group = (StandardGroup<GroupMember<?>>)group.clone(map);
	    }
	    
	    if(group!=null) {
		    /*
		     *  clone.group is a clone of group. Our own clone is put into this group-clone if we are inGroup 
		     * (i.e. active member); if not, it is put into our own group.
		     */
	        int index = -1;
	        if(group instanceof OrderedGroup) {
	            index = ((OrderedGroup)group).indexOf(this);
	        }
		    clone.group.getMembers().remove(this);
	        if(group instanceof OrderedGroup) {
	            ((OrderedGroup)(inGroup? clone.group: group)).add(index, clone);
	        } else {
	            (inGroup? clone.group: group).add(clone);
	        }
		}
	    return clone;
    }

    @Override
    public float getDefaultRadius() {
        return DEFAULT_RADIUS;
    }
	
	public Contact getCurrentContact() {
		return currentContact;
	}
	
	@Override
    public void setBounds2D(float x, float y, float w, float h) {
	    float h0 = bounds2d.height;
	    float w0 = bounds2d.width;
	    super.setBounds2D(x, y, w, h);
	    radius = w/2;
	    if(radiusInfo!=null && h0!=h) radiusInfo.notify(this);
	}

	public void setSize(float s) {
		if(isHidden()) {
			setSizeWhenHidden(s);
		}
		else {
			setSizeWhenNotHidden(s);
		}
	}
	
	public void setSizeWhenNotHidden(float s)
	{
		radius = s/2;
		setSizeWhenNotHidden(new DimensionF(s, s));
	}
   	
	public void setSizeWhenHidden(float s)
	{
		setSizeWhenHidden(new DimensionF(s, s));
	}

	@Override
    public boolean shouldBeHidden() {
	    return super.shouldBeHidden() && !negated;
	}
	

    /**
     * To be called by Canvas#moveSelection() in the case of a transformation.
     */
    @Override
    public void adjustToTransform(AffineTransform t, int modifier) {
    	super.adjustToTransform(t, modifier);
    	if((t.getType() & AffineTransform.TYPE_UNIFORM_SCALE) > 0) {
    		float factor = (float)t.getScaleX();
    		if(TransformModifier.isScalingNodes(modifier)) {
    			float r = factor*radius;
    			float w = r*2;
    			if(w>MIN_VALUE && w<MAX_VALUE) {
	    			setRadius((float)r);
	    			if(hidden) {
	    				sizeWhenNotHidden.width = w;
	    				sizeWhenNotHidden.height = w;    				
	    			}
    			}
    		}
    	}
    }
	
	public boolean isDescendantOf(ENode ancestor) {
		boolean b = false;
		Item gov = getGovernor();
		if(gov!=null && gov instanceof DependentItem) {
			ENode[] e = (ENode[])((DependentItem)gov).getInvolutes(); // like 'getParents', except that
			for(int i = 0; i<e.length && !b; i++) { // the last element will be the entity itself
			    b = e[i]==ancestor || (e[i]!=this && e[i].isDescendantOf(ancestor));
			}
		}
		return b;
	}
	
	public ArrayList<ENode[]> getDependencies(ENode ancestor) { // returns the ways in which this Entity depends on the specified 'ancestor'
		ArrayList<ENode[]> result = new ArrayList<ENode[]>();
		getDependencies(ancestor, new Stack<ENode>(), result);
		return result;
	}
	
	private void getDependencies(ENode ancestor, Stack<ENode> current, ArrayList<ENode[]> result) {
		Item gov = getGovernor();
		if(this==ancestor) {
			result.add(current.toArray(new ENode[0]));
		}
		else if(gov!=null && gov instanceof DependentItem) {
			current.push(this);
			ENode[] e = (ENode[])((DependentItem)gov).getInvolutes(); // like 'getParents', except that
			for(int i = 0; i<e.length-1; i++) { // the last element will be the entity itself				
				e[i].getDependencies(ancestor, current, result);
			}
			current.pop();
		}	
	}
	
	
	/**
	 * @return a Circumference object containing all the Contacts of this Entity with a priority that is higher than
	 *  the one specified (depending on the second parameter).
	 */
	public Circumference getCircumference(float priority, boolean setFixed) {
		Circumference cc = new Circumference();		
		Item gov = getGovernor();
		if(gov!=null && gov instanceof Connector && -1 < priority) {
			Connector g = (Connector)gov;
			CubicCurve2D.Float curve = (CubicCurve2D.Float)g.getLine();
			Point2D p = getCenter();
			double t = g.getItemLocationParameter();
			
			Point2D p0 = MathTools.circleIntersection(p, radius, curve, t, 0, 4);
			Point2D p1 = MathTools.circleIntersection(p, radius, curve, t, 1, 4);
			
			try {
				cc.add(new Contact(MathTools.angle(p, p0), -1));
				cc.add(new Contact(MathTools.angle(p, p1), -1));
			}
			catch(NullPointerException npe) { // in such a case, all we have to expect are Contacts ending up too close together.
				// System.err.println(this+" "+this.getWidth()+" c: "+p+" ct: "+MathTools.getPoint(curve, t));
			}
		}
		
		for (Object element : dependentItems) {
			DependentItem s = (DependentItem)element;
			float p = s.getPriority();			
			if(p < priority) {
				ENode[] e = (ENode[])s.getInvolutes();
				for(int j = 0; j<e.length; j++) {
					Contact c = new Contact(s, j, p);
					if(this==e[j]) cc.add(c);
					if(setFixed) {
						c.setFixed(true);
					}					
				}
			}
		}
		return cc;
	}
	
	/**
	 * Re-arrange the contact points (Contacts) of this Entity's States. The specified Set will contain all the Items affected 
	 * by the subsequent changes. If this Entity is already contained in that Set, there will be no effect. <br>
	 * The present, concurrent implementation is not fully correct, and has fairly high time complexity for smallish graphs when
	 * compared to an implementation by way of a central queue. However, by going into recursion only where necessary, it has
	 * a way of avoiding the exponential complexity (in the size of the affected clique) from which a queue implementation would 
	 * inevitably suffer. (Moreover, it would require only a little more work to make the present implementation fully correct,
	 * as well as to make it use less time. But it seems good enough to me as it is.)
	 * 
	 * Contact points of states with a negative priority will not be subjected to any changes.
	 */
	@Override
    public void arrangeContacts() {
	    HashSet<Item> affected = new HashSet<Item>();
	    arrangeContacts(affected, null);
	    Canvas canvas = getCanvas();
	    for(Item item: affected) {
	    	canvas.markForRelocation(item);
	    }
	}
	
	@Override
    public void arrangeContacts(Set<Item> affected, Set<DependentItem> immuneStates) {		
	    //long time = System.nanoTime();
		arrangeContacts1(affected, immuneStates);
		
		// wait for Threads to die:
		boolean b = true;
		Thread t = null;
		while(b) {
			b = false;
			Object item = null;
			synchronized(affected) {
				for(Iterator<Item> i = affected.iterator(); !b && i.hasNext();) {
					item = i.next();
					if(item instanceof ENode) {
						t = ((ENode)item).getContactArranger();
						if(t!=null && t.isAlive()) {
							b = true;
						}
					}
				}
			}
			if(b) {
				// System.err.println("T: "+t+" E: "+item);
				try {
					t.join(5);
				}
				catch(InterruptedException ie) {
					ie.printStackTrace(System.err);
				}
			}
		}
		//System.err.println("done: "+((System.nanoTime()-time)/1000000)+"ms");		
	}

	/**
	 * Re-arrange the contact points (Contacts) of this Entity's States. The first Set will contain all the Items affected 
	 * by the subsequent changes. All States with a negative priority are considered unalterable in their contact points (i.e.,
	 * angles at which they 'attach' to the involved Entities). All States contained in the second Set are 'immune'  
	 */
	@SuppressWarnings("unchecked")
	private void arrangeContacts1(Set<Item> affected, Set<DependentItem> immune) {
		if(!isHidden()) {
			/*
			 * Invalidate DIs, since the ENode may have been moved, but only invalidate the _Shapes_ of 
			 * immune Connectors.
			 */
			for(Iterator<DependentItem> i = ((Set)((HashSet)dependentItems).clone()).iterator(); i.hasNext();) {  
				DependentItem s = i.next();
				if(s.getPriority()<0 || (s instanceof Connector && immune!=null && immune.contains(s))) {
				    ((Connector)s).invalidateShape();
				} else {
				    s.invalidate();
				}
			}		
			Circumference cc = getCircumference(0, true);
			arrangeContacts(cc, affected, immune);

			for (Object element : dependentItems) {
				DependentItem s = (DependentItem)element;
				synchronized (affected) {
					affected.add(s);
				}								
				s.defineBounds();
			    if(s instanceof Connector) {
				    Connector rel = (Connector)s;				    
			        Item item = rel.getItem();
					Rectangle2D rect = item.getBounds2D(); 
					Point2D pi = rel.getItemLocation();
					if(pi!=null && (pi.getX()!=rect.getX() ||  pi.getY()!=rect.getY())) {
						item.setBounds2D((float)pi.getX(), (float)pi.getY(), item.getWidth2D(), item.getHeight2D());
						
						if(item instanceof ENode) {
							((ENode)item).arrangeContacts1(affected, immune);
						} 
						else synchronized(affected) {
							affected.add(item);
						}
					}
			    }
			}
		}
		else synchronized(affected) {
			affected.add(this);
		}
	}

	/**
	 * @return the next available Contact from the specified Iterator (where 'available' means that no Contact
	 * with a higher or equal priority is currently being processed by another Entity), or null if there is none. 
	 */
	private Contact nextContact(Iterator<Contact> i, Circumference cc, Set<Item> affected, Set<DependentItem> immune) {
		Contact c = null, sc = null;
		do {
			if(!i.hasNext()) {
				currentContact = null;
				return null;
			}
			c = (Contact)i.next();
			if(c.s==null || c.p<0 || (immune!=null && immune.contains(c.s))) {
				c.setFixed(true);
			}
		}
		while(c.isFixed());
		
		boolean b = true;
		while(b) {
			b = false;
			Object item = null;
			currentContact = c;				
			synchronized(affected) {
				//System.err.println("E: "+this+" c.s: "+c.s+"  c.p: "+c.p);
				for(Iterator<Item> j = affected.iterator(); !b && j.hasNext();) {
					item = j.next();
					if(item instanceof ENode && item!=this) {
						sc = ((ENode)item).getCurrentContact();
						if(sc!=null && sc.p<c.p) {
							b = true;
						}							
					}
				}
			}
			if(b && sc!=null && sc.s!=c.s) {
				// System.err.println("waiting for "+item+", State #"+sc.p);
				try {
					Thread.sleep(2);
				}
				catch(InterruptedException ie) {}
			} 
			else if(b && sc!=null && sc.s==c.s) {
				cc.add(c);
				c.setFixed(true);
				b = false; // no need to stay in the loop in this case
			}			
		}
		return sc==null || sc.s!=c.s? c: nextContact(i, cc, affected, immune);
	}

	/**
	 * Re-arrange the contact points (Contacts) of this Entity's States, with cc containing those Contacts that are already 
	 * in place. The specified HashSet will contain all the Items affected by the subsequent changes. The method returns without
	 * doing anything if this Entity is already contained in that set.
	 */
	private void arrangeContacts(final Circumference cc, final Set<Item> affectedSet, final Set<DependentItem> immune) {	
		
		synchronized(affectedSet) {
			if(affectedSet.contains(this)) return;		
			affectedSet.add(this);
		}
		if(dependentItems==null) return;

		contactArranger = new Thread() {
			@Override
            @SuppressWarnings("unchecked")
            public void run() {
				Set affected = affectedSet;
				
				float m = -1;
				if(!cc.contacts.isEmpty()) {
					Contact c = Collections.max(cc.contacts, Contact.priorityComparator);
					m = c.p;
				}
				Circumference cc0 = getCircumference(Float.POSITIVE_INFINITY, false); // contains all Contacts
				cc0.sortByPriority();
				Circumference cc1 = getCircumference(Float.POSITIVE_INFINITY, false); // contains all Contacts
				cc1.sortByAngle();
				
				Iterator i = cc0.iterator();
				Contact c0 = null, c = null; 
				while(i.hasNext() || c0!=c) {
					if(c0==c) {
						c = nextContact(i, cc, affected, immune);
					}
					c0 = c;
					if(c!=null && c.p > m) { // a Contact should be altered only if not already in cc
						jPasi.item.DependentItem s = c.s;
					    
						if(s instanceof Ornament) {
							Ornament us = (Ornament)c.s;
							
							// find angle at which to place c
							Circumference ccx = new Circumference((ArrayList)cc.contacts.clone());
							ccx.sortByAngle();														
							double a = findAngle(ccx, cc1, us);
														
							// update Circumferences of fixed Contacts
							c.setFixed(true);
							cc.add(c);

							// if necessary, change State 
							if(a!=us.getAngle(0)) {															
								us.setAngle(a, c0.k);
								us.invalidate();
								us.defineBounds();
								Item item = us.getItem();
								Point2D pi = us.getItemLocation();
								item.setBounds2D((float)pi.getX(), (float)pi.getY(), item.getWidth2D(), item.getHeight2D());
							}
						}						
						else if(s instanceof Connector) {
							Connector rel = (Connector)c.s;
							ENode other = rel.otherInvolute(ENode.this);
							int l = c.k==0? 1: 0;
							
							// find angle at which to place c
							Circumference ccx = new Circumference((ArrayList)cc.contacts.clone());
							Circumference cc_ = other.getCircumference(c.p, true);
							ccx.addAll(translateContacts(cc_, rel, c.k));
							ccx.sortByAngle();														
							double a = findAngle(ccx, cc1, rel, c.k);
														
							// update Circumferences of fixed Contacts
							c.setFixed(true);
							cc.add(c);
							
							// update currentContact
							c = nextContact(i, cc, affected, immune);

							// if necessary, change State and go into recursion 
							if(a!=rel.getAngle(c0.k)) {
								rel.invalidateShape();
								rel.setAngle(a, c0.k);
								rel.setAngle(rel.getPreferredAngle(l, a), l);
								rel.defineBounds();
								
								// arrange States of other Entity
								if(ENode.this!=other) {								
									Contact c1 = new Contact(rel, c0.k==0? 1: 0, c0.p);
									c1.setFixed(true);
									cc_.add(c1);
									other.arrangeContacts(cc_, affected, immune);
								}
							}
							
							// arrange States of current State's Item
					        Item item = rel.getItem();
							Rectangle2D rect = item.getBounds2D();
							
							//rel.defineBounds();
							Point2D pi = rel.getItemLocation();
							if(pi!=null && (pi.getX()!=rect.getX() ||  pi.getY()!=rect.getY())) {
								item.setBounds2D((float)pi.getX(), (float)pi.getY(), item.getWidth2D(), item.getHeight2D());
								
								if(item instanceof ENode) {
									((ENode)item).arrangeContacts1(affected, immune);
								} 
								else synchronized(affected) {
									affected.add(item);
								}
							}
						} // end of else-if
						synchronized (affected) {
							affected.add(s);
						}								
					}
				} // end of while
				currentContact = null;
			}
		};
		contactArranger.start();
	}

	protected ArrayList<Contact> translateContacts(Circumference cc, Connector rel, int k) {
		ArrayList<Contact> v = cc.contacts;
		ArrayList<Contact> w = new ArrayList<Contact>();
		for (Object element : v) {
			Contact c = (Contact)element;
			w.add(new Contact(rel.getPreferredAngle(k, c.getAngle()), c.p));
		}
		return w;
	}
	
	protected double findAngle(Circumference ccx, Circumference cc1, Ornament us) {
		double e0 = D0; // absolute minimal angle between two Contacts
		double alpha = us.getPreferredAngle(0); 
		double a = alpha;
		Iterator<Contact> i = ccx.iterator(0, true);
		if(i.hasNext()) {

			float badness = Float.POSITIVE_INFINITY;
			Contact c0 = i.next();
			double a0 = c0.getAngle();
			
			alpha = MathTools.normalize(alpha, a0+Math.PI);
			a = alpha;
		
			Contact c1 = null;				
			Contact c2 = null;
			i = ccx.iterator(a0, false);
			do { 
				c1 = i.next(); // those skipped here will not be missed, since they coincide with c0.
			} while(i.hasNext() && c1!=c0); // the next item (if it exists) will lie beyond alpha.
			
			//System.err.println("s E: "+this+"  a0: "+(float)a0);
			
			while(i.hasNext() || c2!=c0) { 
				c2 = i.hasNext()? i.next(): c0; // walk once all the way round the circle
				
				double a1 = MathTools.normalize(c1.getAngle()+e0, a0+Math.PI)-e0;
				double a2 = MathTools.normalize(c2.getAngle(), a0+Math.PI);
				double e1 = preferredAngularDistance(cc1, a1, Direction.COUNTERCLOCKWISE); // 'comfortable' angle between two Contacts
				double e2 = preferredAngularDistance(cc1, a2, Direction.CLOCKWISE); // ditto, in the other direction
				if(a2<a1 || c2==c0) a2 += 2*Math.PI;

				//System.err.println("c1: ("+c1.s+" "+c1.p+")  a1: "+(float)a1+" a2: "+(float)a2);
				
				float bx = closenessPenalty(a2-a1, Math.min(e1, e2)); 
				//System.err.println("bx: "+bx+"  e1: "+e1+" e2: "+e2+" alpha: "+alpha);
				if(bx<badness) {
					double ax = (a1+a2)/2;
					if(a1<=alpha && a2>=alpha) {
						ax = alpha;
					} 
					if(ax<a1+e1 || a2-e2<ax) {
					    if(ax<a1+e1 && a2-e2<ax) ax = (a1+a2)/2;
					    else if(ax<a1+e1) ax = a1+e1;
					    else ax = a2-e2;
					}
					double[] axs = new double[] {ax, a1+e1, a2-e2};						
					float p = Float.POSITIVE_INFINITY;
					for (double ax1 : axs) {
						float dpx = distancePenalty(ax1, alpha);
						float spx = switchPenalty(us, ax1, alpha);
						float px = dpx + spx;
						//System.err.println("ax1: "+ax1+" dpx: "+dpx+" spx: "+spx);
						if(px<p) {
						    p = px;
						    ax = ax1;
						}							
					}
					bx += p;
					
					float dp = distancePenalty(ax, alpha);
					//System.err.println("  ax: "+ax+"  bx: "+bx+"   dp: "+dp);
					
					if(bx<badness) {
						badness = bx;
						a = ax;
					}				
				}				
				c1 = c2;
			}
		}
		return a;
	}

	protected double findAngle(Circumference ccx, Circumference cc1, Connector rel, int k) {

	    ENode other = rel.otherInvolute(this);
	    boolean desc = other.isDescendantOf(this) || this.isDescendantOf(other);
	    
	    //boolean _b = rel instanceof Adjunction && k==0; //other.isDelegate() && other.getGovernor() instanceof Adjunction;  // just for debugging
	    
		double e0 = D0; // absolute minimal angle between two Contacts
		double alpha0 = rel.getPreferredAngle(k);
		double gamma = rel.getBaseAngle() + (k==0? 0: Math.PI);
		double a = alpha0; // the currently best candidate for being c's angle

		Iterator<Contact> i = ccx.iterator(0, true);
		if(i.hasNext()) {
			
			float badness = Float.POSITIVE_INFINITY;
			Contact c0 = i.next();
			double a0 = c0.getAngle();
			
			gamma = MathTools.normalize(gamma, a0+Math.PI);
			alpha0 = MathTools.normalize(alpha0, a0+Math.PI);
			a = alpha0;
			double alpha1 = MathTools.normalize(2*gamma-alpha0, a0+Math.PI);
			
			Contact c1 = null;				
			Contact c2 = null;
			i = ccx.iterator(a0, false);
			do { 
				c1 = i.next(); // those skipped here will not be missed, since they coincide with c0.
			} while(i.hasNext() && c1!=c0); // the next item (if it exists) will lie beyond alpha.
			
			//if(_b) System.err.println("s E: "+this+"  a0: "+(float)a0+"  gamma:"+(float)gamma+"  alpha0: "+(float)alpha0);
			
			while(i.hasNext() || c2!=c0) { 
				c2 = i.hasNext()? i.next(): c0; // walk once all the way round the circle
				
				double a1 = MathTools.normalize(c1.getAngle()+e0, a0+Math.PI)-e0;
				double a2 = MathTools.normalize(c2.getAngle(), a0+Math.PI);
				double e1 = preferredAngularDistance(cc1, a1, 
                        Direction.COUNTERCLOCKWISE); // 'comfortable' angle between two Contacts
				double e2 = preferredAngularDistance(cc1, a2, 
                        Direction.CLOCKWISE); // 'comfortable' angle between two Contacts
				if(a2<a1 || c2==c0) a2 += 2*Math.PI;

				//if(_b) System.err.println("c1: ("+c1.s+" "+c1.p+")  a1: "+(float)a1+" a2: "+(float)a2+" e1: "+e1+" e2: "+e2);
				
				float bx = closenessPenalty(a2-a1, Math.min(e1, e2)); 
				if(bx<badness) {
					double ax = (a1+a2)/2;
					if(a1<=alpha0 && a2>=alpha0) {
						ax = alpha0;
					} 
					else if(a1<=alpha1 && a2>=alpha1) {
						ax = alpha1;
					}
					if(ax<a1+e1 || a2-e2<ax) {
					    if(ax<a1+e1 && a2-e2<ax) ax = (a1+a2)/2;
					    else if(ax<a1+e1) ax = a1+e1;
					    else ax = a2-e2;
					}
					double[] axs = new double[] {ax, a1+e1, a2-e2};						
					float p = Float.POSITIVE_INFINITY;
					for (double element : axs) {
						float dp = distancePenalty(element, alpha0);
						float sp = switchPenalty(rel, element, gamma, alpha0, k, desc);
						float cbp = closenessToBaseAnglePenalty(element, alpha0, gamma);				
						float px = dp + sp + cbp;
						//if(_b) System.err.println("   axs[j]: "+axs[j]+"  px: "+px+"   dp: "+dp+" sp: "+sp+" cbp: "+cbp);
						if(px<p) {
						    p = px;
						    ax = element;
						}							
					}
					bx += p;

					// for debugging:									 
					/*
					float dp = distancePenalty(ax, alpha0);
					float sp = switchPenalty(rel, ax, gamma, alpha0, k, desc);
					float cbp = closenessToBaseAnglePenalty(ax, alpha0, gamma);				
					if(_b) System.err.println("  ax: "+ax+"  bx: "+bx+"   dp: "+dp+" sp: "+sp+" cbp: "+cbp);
					*/
					if(bx<badness) {
						badness = bx;
						a = ax;
					}				
				}				
				c1 = c2;
			}
		}
		return a;
	}

	protected final float closenessToBaseAnglePenalty(double ax, double alpha, double gamma) {
		double d0 = Math.min(MathTools.d(gamma, alpha), MathTools.d(alpha, gamma));
		double d1 = Math.min(MathTools.d(gamma, ax), MathTools.d(ax, gamma));		
		return (float)(d1>d0 || d0==0? 0: CLOSENESS_TO_BASE_ANGLE_PENALTY * (1 - d1/d0));
	}

	protected final float closenessPenalty(double d, double e) {
		d -= 2*D0;
		return (float)(d<=0? Float.POSITIVE_INFINITY: d>=2*e? 0: HALF_DISTANCE_PENALTY*(2*e/d - 1));
	}
	
	protected final float distancePenalty(double ax, double alpha) {
		double d = Math.min(MathTools.d(alpha, ax), MathTools.d(ax, alpha));
		return (float)(DISTANCE_PENALTY * d * radius);
	}
	
	protected final float switchPenalty(Ornament us, double ax, double alpha) {
	    double d = MathTools.d(alpha, ax);
	    return d>0 && (flexDirection==Direction.CLOCKWISE)==(d<=Math.PI)? Float.POSITIVE_INFINITY: 0;
	}

	protected final float switchPenalty(Connector rel, double ax, double gamma, double alpha, int k, boolean desc) {
	    double d = MathTools.d(gamma+(flexDirection==Direction.CLOCKWISE? 1: -1)*SWITCH_TOLERANCE/2, ax);
	    return !desc && !rel.isSymmetric() && d>SWITCH_TOLERANCE && 
	    	(flexDirection==Direction.CLOCKWISE)!=((k==0)==(d<=Math.PI))? SWITCH_PENALTY: 0;
	}
	
	/*
	 * difference between a and orientation, in the direction specified. Ranges over [0..2*Math.PI[
	 */
	private static double d(double a, double b, Direction direction) {
		double d = MathTools.d(a, b);
		return d==0 || direction==Direction.COUNTERCLOCKWISE? d: 2d*Math.PI - d;
	}

	/**
	 * Returns the distance a contact at the specified angle should be displaced in the specified direction, given 
	 * the density of Contacts on the specified Circumference (which should be sorted by angle).
	 */
	private float preferredAngularDistance(Circumference cc, double ax, Direction direction) { 
	    Iterator<Contact> i = cc.iterator(ax, direction!=Direction.COUNTERCLOCKWISE);
	    int k0 = 0,
	        k1 = 0,
	        k2 = 0;
	    while(i.hasNext()) {
	        Contact c1 = (Contact)i.next();
	        double a1 = c1.getAngle(); 
	        double d = d(ax, a1, direction);
	        if(d<Math.PI/2) k0++;
	        if(d<Math.PI) k1++;
	        k2++;
	    }
	    double ad = (2d*Math.PI/k0 + 2d*Math.PI/k1 + 2d*Math.PI/k2)/(4 + 2 + 1);
	    //if(getStates().size()>3) System.err.println("k0: "+k0+"  ax: "+ax+"  ad: "+ad);
	    return (float)Math.min(D1, ad);
	}
	
	/*
	public boolean canChangeNegated() {
		Item gov = getGovernor();
	    return negatable || gov instanceof DependentItem;
	}

	public boolean canChangeNegated_g() {
		return canChangeNegated();
	}
	
	public void setNegatable(boolean b) {
        negatable = b;
        if(!b) {
            setNegated(false);
        }
    }
	
    public boolean isNegated() {
        return negated;
    }
    
    public static void setNegated_g(Set<Item> items, boolean b) {
        for(Item item: items) {
            if(item instanceof ENode) {
            	ENode e = (ENode)item; 
    	        if(e.canChangeNegated()) {
    	        	e.setNegated(b);
    	        }
            }
        }
	}	

	public void setNegated_g(boolean b) {
        Canvas c = getCanvas();
        setNegated_g(c.getSelectedSet(), b);
	}	

	public void setNegated(boolean b) {
        negated = b;
        setHidden(shouldBeHidden());
    }
	*/
	
    public boolean canChangeRadius() {
        return true;
    }
    
	protected Thread getContactArranger() {
		return contactArranger;
	}

	@Override
    public void paintShape(Graphics g) {
		if(!isHidden()) {
			Graphics2D g2 = (Graphics2D)g;
			float lw = stroke.getLineWidth();
			
			if(!negated) {
		        Shape s = new Ellipse2D.Float(lw/2 + 1, lw/2 + 1, 2*radius - lw - 1, 2*radius - lw - 1);
		        if(shading<1) {
		            g2.setPaint(shadePaint);
		            g2.fill(s);
		        }
			    if(lw>0) {
			        g2.setColor(getForeground());
			        g2.draw(s);
			    }
			}
			else {
			    float r = negFactor*radius;
			    int w = getWidth();
			    float x = (w - 2*r)/2 + 1;
			    float y = x;
			    g2.fill(new Ellipse2D.Float(x, y, 2*r - 1, 2*r - 1));
			}
		}
	}
	
	@Override
    public String toString() {
	    return getName()!=null && getName().length()>0? getName(): String.valueOf(hashCode());
	}

	/* (non-Javadoc)
	 * @see pasi.item.Item#getTexdrawCode()
	 */
	@Override
    public String getTexdrawCode(int ch) {
	    float r = radius-LINE_WIDTH_FACTOR*stroke.getLineWidth();
	    float[] dash = stroke.getDashArray();
		return super.getTexdrawCode(ch) + 
			(shading<1 || stroke.getLineWidth()>0? Texdraw.move(bounds2d.x + radius, ch - bounds2d.y - radius): "") + 
			(!negated? ((shading<1? Texdraw.fcirc(r, Math.round(1000*shading)/1000f): "") +
			            (stroke.getLineWidth()>0? 
			                    (dash!=null? Texdraw.linePattern(dash): "") +
			                    Texdraw.circ(r): "") +
			                    (dash!=null? Texdraw.linePattern(null): "")): 
			         Texdraw.fcirc(negFactor*r, 0));
			        
	}	
		
	 /**
     * @see jPasi.item.group.GroupMember#acceptsAsGroup(pasi.item.Group)
     */
    public boolean acceptsAsGroup(Group<?> g) {
        return g instanceof StandardGroup;
    }

    @SuppressWarnings("unchecked")
	public void setGroup(Group<?> g) {
	    group = (StandardGroup<GroupMember<?>>)g;
    }

    /**
     * For use with EditorPane etc.
     */
    @Override
    public void setRadius(float r) {
        float w = 2*r;
        float h = w;
        Rectangle2D.Float oldBounds = (Rectangle2D.Float)getBounds2D();
        setBounds2D(oldBounds.x+(oldBounds.width-w)/2, 
                oldBounds.y+(oldBounds.height-h)/2, 
                w, h);
        arrangeContacts();
    }
    
    @Override
    public List<EditorEntry> getInfo() {
        if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    
		    /*
		    EditorEntry negInfo = new EditorEntry("Negated", TOGGLE, "Negated");
		    negInfo.setTopSpace(2);
		    result.add(negInfo);
		    */
		    
		    result.addAll(super.getInfo());

		    EditorEntry resetInfo = new EditorEntry(null, RESET, "Defaults");
		    result.add(resetInfo);
		    
		    result.trimToSize();
		    this.info = result;
	    }
	    return info;
    }
    

	@Override
    public String getInfoString() {
	    if(shading<1 || stroke.getLineWidth()>0) {
	        return null;
	    }
	    else {
	        return Codec1.encodeFloat(2*radius) + " " +
	        	Codec1.encodeFloat(bounds2d.x) + " " +  
	        	Codec1.encodeFloat(bounds2d.y);
	    }
	}

	@Override
    public void parse(String code, String info) throws ParseException {
	    Texdraw.StrokedShape[] s = Texdraw.getStrokedShapes(code, DEFAULT_LINEWIDTH);	    
	    Texdraw.Circle[] c = new Texdraw.Circle[s.length];
	    for(int i = 0; i<Math.min(2, s.length); i++) {
		    if(!(s[i].shape instanceof Texdraw.Circle)) {
		        throw new ParseException(TEX, "Circle expected, not "+s[i].shape.genericDescription());
		    }
		    c[i] = (Texdraw.Circle)s[i].shape;
	    }
	    int k = 0;
	    if(s.length>0) {
            setShading(c[0].fillLevel);
    	    if(s.length>1) {
    	        k = 1;
    	        if(s.length>2) {
    	            throw new ParseException(TEX, "Too many shapes");
    	        }
    	    }
    	    stroke = newStroke(stroke, s[k].stroke.lineWidth, s[k].stroke.pattern);
    	    float w = 2*(c[k].radius+LINE_WIDTH_FACTOR*s[k].stroke.lineWidth);
    	    float x = c[k].location.x;
    	    float y = c[k].location.y; 
    	    setBounds2D(x-w/2, getCanvas().getHeight()-y-w/2, w, w);
        }
	    else {
	        stroke = newStroke(stroke, 0, DEFAULT_DASH);
	        if(info==null) {	        	
	            throw new ParseException(HINT, "No circle command found, info string required");
	        }
	        String[] sp = info.split("\\s+");
			if(sp.length!=3) {
			    throw new ParseException(TEX, "Invalid info string: "+info);
			}
			float w = Codec1.decodeFloat(sp[0]);
			float x = Codec1.decodeFloat(sp[1]);
			float y = Codec1.decodeFloat(sp[2]);
	        setBounds2D(x, y, w, w);
	    }
	}

  
}

