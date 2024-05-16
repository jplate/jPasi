/*
 * Created on 22.10.2006
 */
package jPasi;

import static jPasi.Canvas.Orientation.HORIZONTAL;
import static jPasi.Canvas.Orientation.VERTICAL;
import static jPasi.edit.EditorEntry.Type.ACTION;
import static jPasi.edit.EditorEntry.Type.BOOLEAN;
import static jPasi.edit.EditorEntry.Type.FLOAT;
import static jPasi.edit.EditorEntry.Type.INTEGER;
import static jPasi.edit.EditorEntry.Type.LABEL;
import static jPasi.edit.EditorEntry.Type.RESET;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;

import jPasi.edit.EditEvent;
import jPasi.edit.EditListener;
import jPasi.edit.Editable;
import jPasi.edit.EditorEntry;
import jPasi.edit.EditorPane;
import jPasi.edit.EditorTopic;
import jPasi.item.Axis;
import jPasi.item.CNode;
import jPasi.item.Connector;
import jPasi.item.Contour;
import jPasi.item.DependentItem;
import jPasi.item.Direction;
import jPasi.item.ENode;
import jPasi.item.IndependentItem;
import jPasi.item.Item;
import jPasi.item.Label;
import jPasi.item.Ornament;
import jPasi.item.Point;
import jPasi.item.group.Group;
import jPasi.item.group.GroupMember;
import jPasi.item.group.GroupUIDelegator;
import jPasi.item.group.Groups;
import jPasi.item.group.OrderedGroup;
import jPasi.item.group.StandardGroup;
import jPasi.laf.MarkBorder;
import util.MathTools;
import util.swing.FloatSpinner;
import util.swing.XYConstraints;
import util.swing.XYLayout;

 /**
 * @author Jan Plate
 *
 */
public class Canvas extends JPanel implements MouseInputListener, MouseWheelListener, Editable {
  
    private class Blinker extends Thread {
        
		private volatile boolean restartRequested;
        
        public void requestRestart() {
            restartRequested = true;
        }
        
        //@Override
        public void run() {
            final int fc = BLINK_FRAME_COUNT;
            Item item = null;
            String title = null;
            MarkBorder[] borders = null;
            while(true) {
                while(itemToBeEdited==null) {
                    try {
                        sleep(1000);
                    } catch(InterruptedException ie) {}                        
                }
                
                item = itemToBeEdited;
                if(item==null) continue;
                
                if(item instanceof ENode) {
                    String newTitle = getTitleString((ENode)item);
                    if(!newTitle.equals(title)) {
                        float[] c = new float[3];
                		MARK_COLOR1.getColorComponents(c);
                		borders = new MarkBorder[fc];
                		for(int i = 0; i<fc; i++) {
                			Color c0 = new Color(c[0], c[1], c[2], (float)i/fc);
                			borders[i] = new MarkBorder(c0, Color.white, MARK_COLOR1, newTitle);
                		}
                    }
                    title = newTitle;
                } else {
                    borders = markBorders0b;
                    title = null;
                }

                int start = 0;
                if(restartRequested) {
                	MarkBorder mb = (MarkBorder)item.getBorder();
                	if(mb!=null) {
                		start = matchingBorderIndex(mb, borders);
                	}
                	restartRequested = false;
                }

                for(int i = start; i<borders.length && item==itemToBeEdited && !restartRequested; i++) {
                    item.setBorder(borders[i]);
                    try {
                        sleep(BLINK_SLEEP);
                    } catch(InterruptedException ie) {}
                }
                try {
                    sleep(20);
                } catch(InterruptedException ie) {}
                for(int i = borders.length-1; i>0 && item==itemToBeEdited && !restartRequested; i--) {
                    item.setBorder(borders[i]);
                    try {
                        sleep(BLINK_SLEEP);
                    } catch(InterruptedException ie) {}
                }
                if(item!=itemToBeEdited) {
                    if(selection.contains(item)) {
                        markAsSelected(item);
                    }
                }
            }
        }
    }
    
    /**
     * The run-method is called at the end of CompoundArrow.doDissolve().
     */
    public class CompoundArrowDissolve implements Runnable {
        public boolean addListeners;
        public List<Connector> connectors;
        public boolean setPriority;
        
        public CompoundArrowDissolve(List<Connector> con, boolean setPriority, boolean addListeners) {
            this.connectors = con;
            this.setPriority = setPriority;
            this.addListeners = addListeners;
        }
                
	    public void add(Connector rel) {
	        IndependentItem<?>[] e = rel.getInvolutes();
    		e[0].addDependentItem(rel);
    		if(e[0]!=e[1]) {
    			e[1].addDependentItem(rel);
    		}
            addDependentItem(rel, setPriority, addListeners, true);  				            
	    }
	    
	    public void run() {
            clearSelection();
	        java.util.Collections.sort(connectors, DependentItem.priorityComparator);
	        Connector c = null;
	        for (Iterator<Connector> i = connectors.iterator(); i.hasNext();) {
	        	c = i.next();
	            add(c);
	            c.setVisible(true);
	            select1(c.getItem(), -1);
	            c.getItem().setVisible(true);
	        }
	        setItemToBeEdited(c.getItem());
	        fireSelectionChanged();
	    }
    }
    
    public class Grid {
        public static final int DEFAULT_HGAP = 5;
        public static final int MAX_HGAP = 999;
        public static final int DEFAULT_VGAP = 5;
        public static final int MAX_VGAP = 999;

        public static final int DEFAULT_HSHIFT = 0;
        public static final int MAX_HSHIFT = 999;
        public static final int DEFAULT_VSHIFT = 0;
        public static final int MAX_VSHIFT = 999;
        
        public static final int CONTOUR_CENTER_SNAP_RADIUS = 10;
		public static final double CONTOUR_NODE_SNAP_RADIUS = 15;
		
        private static final byte CENTER_SNAP = 1; // used for encoding, should be a power of 2
        private static final byte NODE_SNAP = 2; // used for encoding, should be a power of 2
		
        public int hGap = DEFAULT_HGAP;
        public int hShift = DEFAULT_HSHIFT;
        
        public boolean snapToContourCenters = true;
		public boolean snapToCNodes = true;
        public int vGap = DEFAULT_VGAP;
        public int vShift = DEFAULT_VSHIFT;

        public Grid() {}
        
        public Grid(int hGap, int vGap, int modifier, int hShift, int vShift) {
            this.hGap = hGap;
            this.vGap = vGap;
            this.snapToContourCenters = (modifier & CENTER_SNAP) > 0;
            this.snapToCNodes = (modifier & NODE_SNAP) > 0;
            this.hShift = hShift;
            this.vShift = vShift;            
        }
        
        public byte getModifier() {
        	return (byte) (
        		(snapToContourCenters? CENTER_SNAP: 0) | 
        		(snapToCNodes? NODE_SNAP: 0));
        }
        
        /**
         * Returns the nearest point on the grid to the one specified, in tex coordinates. This may include the centers of
         * contours and other points.
         * 
         * @param the center of the contour of which the primarily selected Item is a node (if applicable), in tex coordinates
         * @param the center of the primarily selected Item itself
         * @param the set of the components that are being dragged
         * @param a one-element boolean array which will contain whether the 'snapped point' was the first or the second 
         * parameter. 
         * 
         */
        public java.awt.Point getNearest(Point2D nodeCenter, Point2D contourCenter, Set<?> draggedComps, boolean[] cc) {
        	java.awt.Point result = null;
        	int h = getHeight();
        	boolean snapToNodes = snapToCNodes && nodeCenter!=null;
        	boolean snapToCenters = snapToContourCenters;
            Point2D gridPoint = new Point2D.Double(
                    nearest(hGap, hShift, nodeCenter.getX()), 
                    nearest(vGap, vShift, nodeCenter.getY()));
        	
            if(snapToCenters || snapToNodes) {
                Component[] comps = Canvas.this.getComponents();
                Point2D bestC = null; // best Contour center; NOT in tex coordinates 
                Point2D bestN = null; // best CNode; NOT in tex coordinates 
	            
                boolean contour = contourCenter!=null;
                Point2D stcPoint = contour? contourCenter: nodeCenter; // the reference point relevant for 
                		// snap-to-contour-center
                double distG = gridPoint.distance(nodeCenter);
                double distC = Math.max(CONTOUR_CENTER_SNAP_RADIUS, distG);
                double distN = Math.max(CONTOUR_NODE_SNAP_RADIUS, distG);

                for(Component comp: comps) {	                
	            	if(snapToCenters && comp instanceof Contour) {
	                    Contour cont = (Contour)comp;
	                    Point2D c = cont.getCenter(); // NOT in tex coordinates
	                    double d = stcPoint.distance(c.getX(), h-c.getY());  
	                    if(d<distC && (draggedComps==null || !cont.containsSome(draggedComps))) {
	                        distC = d;
	                        bestC = c;
	                        cc[0] = contour;
	                    }
	                } else if(snapToNodes && comp instanceof CNode) {
	                    CNode cn = (CNode)comp;
	                    Contour cont = cn.getContour();
	                    Point2D c = cn.getCenter(); // NOT in tex coordinates
	                    double d = nodeCenter.distance(c.getX(), h-c.getY());                        
	                    /*
	                     * Don't let sn snap to other CNodes that are being dragged, and neither to members of Contours
	                     * that are currently being dragged.
	                     */
	                    if(d<distN && (draggedComps==null || !draggedComps.contains(cn))) {
	                        distN = d;
	                        bestN = c;
	                        cc[0] = false;
	                    }
	                }
	            }
	            if(bestC!=null || bestN!=null) {
	            	if(bestC==null) {
	            		result = getTexPoint(bestN);
	            	} else if(bestN==null) {
	            		result = getTexPoint(bestC);
	            	} else {
	            		if(distC<=distN) {
	            			result = getTexPoint(bestC);
	            		} else {
	            			result = getTexPoint(bestN);
	            		}
	            	}
	            }
            } 
            if(result==null) {
            	result = new java.awt.Point((int)gridPoint.getX(), (int)gridPoint.getY());
            }
            return result;
        }
    	
        private java.awt.Point getTexPoint(Point2D p) {
        	return new java.awt.Point((int)Math.round(p.getX()), getHeight()-(int)Math.round(p.getY()));
        }
        
        private double nearest(double gap, double shift, double val) {            
            return val - (val + shift - gap/2)%gap + gap/2;
        }
        
        @Override
        public String toString() {
            return "Grid["+hGap+", "+vGap+", "+hShift+", "+vShift+"]";
        }
    }
    
    private class ItemInfo implements Comparable<ItemInfo> {
    	boolean add; // whether item needs to be added, rather than just have its z-order set.
    	Item item;
    	float z;
    	
    	ItemInfo(Item item, boolean b) {
    		this.item = item;
    		this.add = b;
    		this.z = item.getZValue();
    	}

		public int compareTo(ItemInfo ii) {
	        return ii.z==this.z? 0: ii.z<this.z? 1: -1;
        }
    }
    
    public class Lasso extends JComponent {

        private static final long serialVersionUID = -7237436490391051373L;
            
        ItemIndex index;
        Set<Item> content = new LinkedHashSet<Item>(); // preserves iteration order, so that after lassoing, 
				// items will get selected in the same order as they got preselected.
        boolean deselect;
        
        int x0 = 0;
        int y0 = 0;
        
        public Lasso(int x, int y, ItemIndex ii, boolean deselect) {
        	x0 = x;
        	y0 = y;
        	index = ii;
        	this.deselect = deselect;
        	setBounds(x0, y0, 0, 0); 
        }
        
        public void paint(Graphics g) {
        	Graphics2D g2 = (Graphics2D)g;
        	Rectangle r = getBounds();
        	g2.setPaint(LASSO_PAINT0);
        	g2.setStroke(LASSO_STROKE);
        	int w = (int)r.getWidth()-1;
        	int h = (int)r.getHeight()-1;
        	g2.drawRect(0, 0, w, h);
        	if(deselect) {
        		g2.setPaint(LASSO_PAINT1);
        		g2.fillRect(1, 1, w-1, h-1);
        	}
    	}

		public void setEndPoint(java.awt.Point p, boolean includeHiddenConnectorNodes) {
			int x = (int)p.getX();
			int y = (int)p.getY();
			
			int x1 = x0;
			int y1 = y0;
			int x2 = x;
			int y2 = y;
			if(x<x0) {
				x1 = x;
				x2 = x0;
			}
			if(y<y0) {
				y1 = y;
				y2 = y0;
			}
			
			Rectangle r = getBounds();
			int x01 = (int)r.getX();
			int y01 = (int)r.getY();
			int x02 = x01+(int)r.getWidth();
			int y02 = y01+(int)r.getHeight();
			
			setBounds(x1, y1, x2-x1, y2-y1);
			
			if(index!=null) {
				Set<Item> removal = new HashSet<Item>();
				Set<Item> addition = new HashSet<Item>();
				index.adjustBounds(x1, y1, x2, y2, includeHiddenConnectorNodes, removal, addition);
				
				content.removeAll(removal);
				content.addAll(addition);
				
				// As for the method of de-preselection: basically as in dePreselect, but we do not need to care about 
				// GroupMembers, since the preselection process ignores group memberships, too.
				for(Iterator<Item> i = removal.iterator(); i.hasNext();) {
					preselection.remove(i.next());
				}
				dePreselect1(removal);
				if(!deselect) {
        			for(Iterator<Item> i = addition.iterator(); i.hasNext();) { 
        				// make sure those that are selected don't get preselected.
        				if(i.next().isSelected()) {
        					i.remove();
        				}
        			}
        			preselect1(addition, null);
				}
			}
        }
		
		public void select() {			
			Item toBeEdited = null;
			boolean changeItemToBeEdited = false;
			for(Iterator<Item> i = content.iterator(); i.hasNext();) {
				Item it = i.next();
				if(deselect && it.isSelected()) {
					if(itemToBeEdited==it) {
						toBeEdited = null;
						changeItemToBeEdited = true;
					}
					deselect1(it);
				} else if(!deselect) {
					select1(it, -1);
					toBeEdited = it;
					changeItemToBeEdited = true;
				}
			}
			if(changeItemToBeEdited) {
				setItemToBeEdited(toBeEdited);
			}
			fireSelectionChanged();
			relocateItems();			
		}
    }
    
    public static interface IndexItem {
    	public Object getItem();
    	public IndexItem getPrevious();
    	public IndexItem getNext();
    	public int getPos();
    }
    
    /**
     * Represents an interval on a sequence of interlinked IndexItems, which may themselves be Indices.
     */
    public static class Index {
    	
    	public static enum Dimension {
    		X,
    		Y
    	}

        private static final long serialVersionUID = -933110428012259459L;

        public IndexItem anchor; // the first IndexItem of the underlying chain
        public IndexItem first; // may be null
        public IndexItem last; // may be null
        
        public void adjustBounds(int x0, int x1, Set<IndexItem> removal, Set<IndexItem> addition) {
        	if(anchor==null) return;
        	
        	if(first==null) {        		
        		assert(last==null);
        		IndexItem i0 = anchor;        		
        		for(; i0!=null && i0.getPos()<=x0; i0 = i0.getNext());
        		if(i0!=null) {
            		int p = i0.getPos();
            		if(p>x0 && p<x1) {
            			first = i0;
                		IndexItem i1 = i0;
                		for(IndexItem i = i0; i!=null && i.getPos()<x1; i = i.getNext()) {
                			addition.add(i);
                			i1 = i;
                		}
                		last = i1;
            		}
        		}
        	} 
        	else {
            	int x00 = first.getPos();
            	int x01 = last.getPos();
            	int[] x = new int[] {x0, x1};
            	boolean[] b = new boolean[] {false, true};
            	for(int j = 0; j<b.length; j++) {
            		boolean upper = b[j];
                	boolean backward = x[j]<(upper? x01: x00); // if we should go backwards
                	IndexItem i1 = null; // IndexItem reached at the end of the loop, which will become either the new 
                		// first or the new last IndexItem 
                	boolean change = false;
                	
                	Set<IndexItem> set = upper!=backward? addition: removal;
                	
                	IndexItem i = upper? last: first;
                	if(set==addition && i!=null) { // no need to include the starting item
                		i = backward? i.getPrevious(): i.getNext();
                	}
                	for(;	i!=null && (backward && i.getPos()>x[j] || !backward && i.getPos()<x[j]); 
                			i = backward? i.getPrevious(): i.getNext()) {
                		set.add(i); 
                		change = true;
                		i1 = i;
                	}
                	if(change) {
                		if(set==addition) {
                			if(upper) {
                				last = i1;
                			} else {
                				first = i1;
                			}
                		} else {
                			if(upper) {
                				last = i1.getPrevious();
                    			if(last==null) {
                    				first = null;
                    			}
                			} else {
                				first = i1.getNext();
                    			if(first==null) {
                    				last = null;
                    			}
                			}
                		}
                	}
            	}
        	}
        }        
    }

   	static class ItemIndex extends Index {
   		
   	    static class Row extends Index implements IndexItem {
   	    	
   	    	Row previous;
   	    	Row next;
   	    	
   	    	int y = 0;
   	    	
   	    	Row(int y) {
   	    		this.y = y;
   	    	}
   	    	
   	    	public Object getItem() {
   	    		return this;    	
   	    	}    	
   	    	public IndexItem getPrevious() {
   	    		return previous;
   	    	}
   	    	public IndexItem getNext() {
   	    		return next;
   	    	}
   	    	
   	    	public int getPos() {
   	    		return y;
   	    	}
   	    	
   	    	public String toString() {
   	    		StringBuffer sb = new StringBuffer();
   	    		sb.append("y="+y+" ");
   	    		for(Entry e = (Entry)anchor; e!=null; e = (Entry)e.getNext()) {
   	    			sb.append(e.toString()+" ");
   	    		}
   	    		return sb.toString();
   	    	}
   	    }
   	    
   	   	static class Entry implements IndexItem {
   			Item item;
   			int x = 0;
   			
   	   		Entry previous;
   			Entry next;
   			
   			Entry(Item it, int x) {
   				item = it;
   				this.x = x;
   			}
   			
   	    	public Object getItem() {
   	    		return item;    	
   	    	}    	
   	    	public IndexItem getPrevious() {
   	    		return previous;
   	    	}
   	    	public IndexItem getNext() {
   	    		return next;
   	    	}
   	    	
   	    	public int getPos() {
   	    		return x;
   	    	}
   	    	
   	    	public String toString() {
   	    		return item.toString()+"(x="+x+")";
   	    	}
   		}

   		ItemIndex(Canvas canvas) {
   			Component[] cs = canvas.getComponents();
   			for(Component c : cs) {
   				if(c instanceof ENode) { 
   						// add top-left and bottom-right corner, since an ENode should only be marked if it's
						// completely encircled. 
   					ENode en = (ENode)c;   					
   					int x = en.getX();
   					int y = en.getY();
   					addPoint(en, x, y);
   					addPoint(en, x+en.getWidth(), y+en.getHeight());
   				}
   				else if(c instanceof CNode) {
   					CNode cn = (CNode)c;
   					Point2D p = cn.getCenter();
   					addPoint(cn, (int)p.getX(), (int)p.getY());
   				}
   			}
   			
   			// create backlinks between Rows and the entries of each Row
   			for(Row row = (Row)anchor; row!=null; row = row.next) {
   				if(row.next!=null) {
   					row.next.previous = row;
   				}
   				for(Entry e = (Entry)row.anchor; e!=null; e = (Entry)e.getNext()) {
   	    			if(e.next!=null) {
   	    				e.next.previous = e;
   	    			}
   	    		}   				
   			}	
   		}
   		
   		private void addPoint(Item item, int x, int y) {
   			Entry newEntry = new Entry(item, x);
   			if(anchor==null) {
   				Row row = new Row(y);
   				row.anchor = newEntry;
   				anchor = row;
   			} else {
   				Row row = (Row)anchor;
   				if(row.getPos()>y) {
   					Row row1 = new Row(y);
   					row1.anchor = newEntry;
   					row1.next = row;
   					anchor = row1;
   				}
   				else {
   					while(row.next!=null && row.next.getPos()<=y) {
   						row = row.next;
   					}
   					if(row.getPos()==y) {
   						Entry entry = (Entry)row.anchor;
   						if(entry.getPos()>x) {
   							newEntry.next = entry;
   							row.anchor = newEntry;
   						}
   						else {
   							while(entry.next!=null && entry.next.getPos()<=x) {
   								entry = entry.next;
   							}
   							newEntry.next = entry.next;
   							entry.next = newEntry;   							
   						}
   					}
   					else {
   	   					Row row1 = new Row(y);
   	   					row1.anchor = newEntry;
   	   					row1.next = row.next;
   	   					row.next = row1;
   					}   					
   				}
   			}
   		}
   		
   		/**
   		 * First gathers the Items out of newly added or removed Rows, then the newly added or removed Items of
   		 * already included Rows into the supplied sets. In the case of adding ENodes, only those will be added that 
   		 * lie completely within the indicated bounds.
   		 */
   		@SuppressWarnings("unchecked")
        public void adjustBounds(int x0, int y0, int x1, int y1, boolean includeHiddenConnectorNodes, 
        		Set<Item> removal, Set<Item> addition) {
   			Set<IndexItem> removedRows = new HashSet<IndexItem>();
   			Set<IndexItem> addedRows = new HashSet<IndexItem>();
   			adjustBounds(y0, y1, removedRows, addedRows);

   			Set<IndexItem>[] sets = new Set[]{removedRows, addedRows};
   			for(Set<IndexItem> rows: sets) {
   				for(IndexItem ii: rows) {   					
   					Row row = (Row)ii;	   					
   		   			Set<Item> set = rows==removedRows? removal: addition; 
   					for(Entry e = (Entry)row.first; e!=null; e = (Entry)e.next) {
   						Item it = (Item)e.getItem();
   						if(rows==removedRows || !(it instanceof ENode) || 
   								withinBounds(x0, y0, x1, y1, it) && (includeHiddenConnectorNodes || !it.isHidden())) {
   							set.add(it);
   						}
   						if(e==row.last) break;
   					}
   				}
   			}
   			
   			for(Row row = (Row)first; row!=null; row = (Row)row.next) {
   				Set<IndexItem> removedEntries = new HashSet<IndexItem>();
   	   			Set<IndexItem> addedEntries = new HashSet<IndexItem>();
   				row.adjustBounds(x0, x1, removedEntries, addedEntries);
   	   			
   				Set<IndexItem>[] sets_ = new Set[]{removedEntries, addedEntries};
   	   			for(Set<IndexItem> entries: sets_) {
   	   				Set<Item> set = entries==removedEntries? removal: addition; 
   	   				for(IndexItem ii: entries) {
   	   					Item it = (Item)ii.getItem();
   	   					if(entries==removedEntries || !(it instanceof ENode) || 
   	   							withinBounds(x0, y0, x1, y1, it) && (includeHiddenConnectorNodes || !it.isHidden())) {
							set.add(it);
						}
  					}
   	   			}   	   			
   	   			if(row==last) break;
   			}   			
   			
   			addition.removeAll(removal);
   		}   	
        
        private boolean withinBounds(int x0, int y0, int x1, int y1, Item it) {
        	int x = it.getX();
        	int y = it.getY();
        	int w = it.getWidth();
        	int h = it.getHeight();
        	return x>=x0 && x+w<=x1 && y>=y0 && y+h<=y1;        	
        }

   		public String toString() {
    		StringBuffer sb = new StringBuffer();
    		for(Row r = (Row)anchor; r!=null; r = (Row)r.getNext()) {
    			sb.append(r.toString()+"\n");
    		}
    		return sb.toString();
   		}
   	}
   	
   	static enum Orientation {
    	HORIZONTAL,
    	VERTICAL;
    }
    
    public static class SelectionEvent extends EventObject {
        private static final long serialVersionUID = -4467097605568638673L;

        public SelectionEvent(Canvas source) {
            super(source);
        }
    }
    
    public static interface SelectionListener {
        public void selectionChanged(SelectionEvent se);
    }
    
    private static final long serialVersionUID = 4831220712384816250L;
    
    public static final int BLINK_FRAME_COUNT = 10;
    private static final long BLINK_SLEEP = 75;
	
    public static final Color CONTRAST_COLOR0 = new Color(1f, 1f, 1f, 1f);
    public static final Color CONTRAST_COLOR1 = new Color(1f, 1f, 1f, 1f);

	public static final int DEFAULT_HEIGHT = 380;
	public static final int DEFAULT_WIDTH = 540;
	public static final int MAX_WIDTH = 9999;
	public static final int MAX_HEIGHT = 9999;
	
    public static final int DEFAULT_HDISPLACEMENT = 20;
    public static final int MAX_HDISPLACEMENT = 999;
    public static final int DEFAULT_VDISPLACEMENT = 0;
    public static final int MAX_VDISPLACEMENT = 999;
    
    public static final int FADE_FRAME_COUNT = 10;
    private static final long FADE_SLEEP = 75;

	public static final Color MARK_COLOR0 = Color.decode("#8877bb");
	public static final Color MARK_COLOR1 = Color.decode("#b0251a");
	
	private static final MarkBorder[] markBorders0a; // borders for Items, but not Entities
	private static final MarkBorder[] markBorders0b; // borders for to-be-edited Items, but not Entities
	private static final MarkBorder[] markBorders1a; // borders for Entities

    private static final float MIN_SCALING = 1E-6f;

	public static final float DEFAULT_LABEL_Z = 1;
	public static final float DEFAULT_DEPENDENT_ITEM_Z = 2;
	public static final float DEFAULT_ENODE_Z = 3;
	public static final float DEFAULT_CONTOUR_Z = 4;
    
    public static final Paint LASSO_PAINT0 = new Color(MARK_COLOR0.getRed(), MARK_COLOR0.getGreen(), MARK_COLOR0.getBlue(), 200);
    public static final Paint LASSO_PAINT1 = new Color(255, 255, 255, 125);
    public static final float[] LASSO_DASH = new float[] {1f};
    private static final Stroke LASSO_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, LASSO_DASH, 0);
    
	static {
        float[] c = new float[3];
        float[] cc = new float[3];
		int ma = FADE_FRAME_COUNT;
		int mb = BLINK_FRAME_COUNT;
        MARK_COLOR0.getColorComponents(c);
        CONTRAST_COLOR0.getColorComponents(cc);
		markBorders0a = new MarkBorder[ma];
		for(int i = 0; i<ma; i++) {
			Color c0 = new Color(c[0], c[1], c[2], (float)i/ma);
			Color c1 = new Color(cc[0], cc[1], cc[2], (float)i/ma);
			markBorders0a[i] = new MarkBorder(c0, c1);
		}
		markBorders0b = new MarkBorder[mb];
		for(int i = 0; i<mb; i++) {
			Color c0 = new Color(c[0], c[1], c[2], (float)i/mb);
			Color c1 = new Color(cc[0], cc[1], cc[2], (float)i/ma);
			markBorders0b[i] = new MarkBorder(c0, c1);
		}
  		
		MARK_COLOR1.getColorComponents(c);
        CONTRAST_COLOR0.getColorComponents(cc);
		markBorders1a = new MarkBorder[ma];
		for(int i = 0; i<ma; i++) {
			Color c0 = new Color(c[0], c[1], c[2], (float)i/ma);
			Color c1 = new Color(cc[0], cc[1], cc[2], (float)i/ma);
			markBorders1a[i] = new MarkBorder(c0, c1);
		}
	}
    
	public static void divideDIs(Set<Item> set, Set<DependentItem> internal, Set<DependentItem> external, Set<Item> thirds) {
	    for (Item item : set) {
	        item.divideDIs(set, internal, external, thirds);
	    }
	}

    private static int matchingBorderIndex(MarkBorder mb, MarkBorder[] borders) {
		if(mb==null) {
			return 0;
		} else {
	    	float a = mb.color.getAlpha();
			int i = 0;
			for(int n = borders.length; i<n && borders[i].color.getAlpha()<a; i++);
			return Math.min(i, borders.length-1);
		}
	}

	private boolean adding;
    private boolean addMembers;
    
	private final Blinker blinker = new Blinker();
    private volatile int currentDIPriority = 1; // used for numbering DependentItems (giving them their priorities)
	private volatile boolean dragging;

    private boolean draggingBlocked;
	private final EditorPane[] editorPanes;
	private Grid grid = new Grid();

    private Handler handler;
	private int hDisplacement = DEFAULT_HDISPLACEMENT;
    private List<EditorEntry> info;

    private volatile Item itemToBeEdited;
    private final JLayeredPane layeredPane;
    JPanel buttonPanel;
	
	private final Map<Item, Set<GroupMember<?>>> marked = 
        Collections.synchronizedMap(new HashMap<Item, Set<GroupMember<?>>>()); 	
	private final Map<Item, Integer> marked1 = 
        Collections.synchronizedMap(new HashMap<Item, Integer>()); // maps components to Integers indicating the
							// level of highlighting
						
	private volatile int mouseX;
	private volatile int mouseY;
    private final Set<Item> preselection = new HashSet<Item>(); 

    private EditorEntry rotateInfo;
	private EditorEntry scalingIncrementInfo;
	private EditorEntry rotationIncrementInfo;
    private EditorEntry scaleInfo;

    private float rotation;
    private float scaling = 1f;
    
    volatile int selectedEntityCount = 0; // number of Entities in selection
    private final Set<Item> 
        selectedSet = new HashSet<Item>(), // These Sets are all relevant only for dragging:
        thirds = new HashSet<Item>(),
        boundary = new HashSet<Item>(); 
    private final Set<DependentItem> 
        internal = new HashSet<DependentItem>(), 
        external = new HashSet<DependentItem>(), 
        immune = new HashSet<DependentItem>();  
    		 
    
    final LinkedList<Item> selection = new LinkedList<Item>();
    private final List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();

    private List<EditListener> editListeners  = new ArrayList<EditListener>();

    private final AffineTransform selectionTransform = new AffineTransform();
    
    @SuppressWarnings("unchecked")
    private final Set<Item>[] selectSet = new Set[] {selectedSet, internal, thirds};

    private int transformModifier;
    
    private final Set<Item> toRelocate = Collections.synchronizedSet(new HashSet<Item>());

    private List<EditorEntry> transformInfo;
    
    FloatSpinner unitSpinner;

	private int vDisplacement = DEFAULT_VDISPLACEMENT;

    private Contour draggedContour;

	private EditorEntry scaleLinewidthsInfo;
	private EditorEntry scaleENodesInfo;
	private EditorEntry scaleArrowheadsInfo;
	private EditorEntry flipArrowheadsInfo;
	private EditorEntry rotateLabelsInfo;

	private volatile boolean relocating;

	private Lasso lasso = null;

	Canvas(JLayeredPane lPane, EditorPane[] ePanes, Handler handler) {
	    super();
	    layeredPane = lPane;
	    editorPanes = ePanes;
	    this.handler = handler;
        setLayout(new XYLayout(false, true));
	    setBackground(Color.white);
        addMouseListener(this);
        addMouseMotionListener(this);
		addMouseWheelListener(this);
        setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        
        blinker.start();
	}

    /**
     * Called in order to add a Contour c to this Canvas for the first time. To re-add it, call relocate().
     */
    public void add(Contour c, boolean addListeners) {
        for(CNode cn: c.nodeSet()) {
            addItem(cn, addListeners);
        }
        addItem(c, false);  
        c.adjustCenter();
       
        repaint();
    }
    
    /**
     * accepts tex coordinates.
     */
    public synchronized void add(Item item, float x, float y) {
    	//System.err.println("add: "+item+" gov: "+item.getGovernor()+" z: "+getComponentZOrder(item));
    	// try {throw new Exception();}catch(Exception e) {e.printStackTrace(System.err);}
        Rectangle2D r = item.getBounds2D();
        add(item, new XYConstraints(x, y, (float)r.getWidth(), (float)r.getHeight()));
        setZOrder(item);
    }
    
    /**
     * Adds a Contour at the initial snode locations.
     */
    public Contour addContour() {
        Contour s = new Contour(this, getInitialSNodes());
        add(s, true);
        return s;
    }
	
	public Contour addContourForPoint(Point p) {
        Contour s = addContour();
        Point2D sc = s.getCenter();
        Point2D pc = p.getCenter();
        float dx = (float)(pc.getX() - sc.getX());
        float dy = (float)(pc.getY() - sc.getY());
        Set<?> nodes = s.nodeSet();
        if(nodes.size()>0) {
            clearSelection((CNode)nodes.iterator().next(), true, false, false);            
            prepareForMovingSelection();
            moveSelectionBy(dx, dy);
            relocateItems();
        }
        return s;
    }
	
    public Contour[] addContours() {
	    ArrayList<Contour> result = new ArrayList<Contour>();
	    for(Iterator<?> i = ((List<?>)selection.clone()).iterator(); i.hasNext();) {            
	        Item it = (Item)i.next();
	        if(it instanceof Point) {
	            Point p = (Point)it;	      
	            Contour s = addContourForPoint(p);
	            result.add(s);
	        }
        }
	    return result.toArray(new Contour[]{});
	}
	
    public DependentItem addDependentItem(DependentItem di) {
	    return addDependentItem(di, true, true, true);
	}

	public DependentItem addDependentItem(DependentItem di, 
    		boolean setPriority, boolean addListeners, boolean arrangeContacts) {
	    
    	if(di instanceof Contour) {
    		Contour sp = (Contour)di;
	    	add(sp, addListeners);
	    	return di;
	    }
	    
    	di.setCanvas(this);
    	di.defineBounds();	    
		
		addItem(di, false);
		if(setPriority) {
			di.setPriority(currentDIPriority);
			currentDIPriority++;
		}
		
		Item item = di.getItem();
		if(item!=null) {
			addItem(item, addListeners);
		}
		
		if(arrangeContacts) {
		    IndependentItem<?>[] e = di.getInvolutes();
			for (IndependentItem<?> element : e) {
			    element.arrangeContacts();
			}
		}
				
		return di;
	}
	
    /**
     * To be called after a cloning process has produced new States that now have to be added to the Canvas.
     */
    public void addDependentItems(Map<Object, Object> cloneMap, boolean arrangeStates) {
        /*
         * Determine new priority values.
         */
        float minp = Float.POSITIVE_INFINITY;
        float maxp = Float.NEGATIVE_INFINITY;
        boolean found = false;
        for(Object o: cloneMap.keySet()) {
            if(o instanceof DependentItem) {
                found = true;
                DependentItem di = (DependentItem)o;
                float p = di.getTruePriority();
                if(p<minp) minp = p;
                if(p>maxp) maxp = p;
            }
        }
        
        /*
         * Add the States and their items.
         */
        for(Object o: cloneMap.values()) {
            if(o instanceof DependentItem) {
                DependentItem di = (DependentItem)o;
                float p = di.getTruePriority();
                di.setTruePriority(currentDIPriority + p - minp);
                /*
                 * In the case of a Contour, its SNodes have already been added, and MouseListeners have been added
                 * to them in the process.
                 */
                if(di instanceof Contour) {
            		Contour sp = (Contour)di;
            		Map<Object, Object> cloneMap1 = new HashMap<Object, Object>();
            		cloneMap1.putAll(cloneMap);
            		sp.cleanUp(cloneMap1);
                    addDependentItem(sp, false, false, arrangeStates);                
                } else {
                    addDependentItem(di, false, true, arrangeStates);                
                }
            }
        }
        currentDIPriority += found? 1 + maxp - minp: 0;    
    }
    
    public void addEditListener(EditListener el) {
    	editListeners.add(el);
    }
    
    public ENode[] addEntities() {
	    ArrayList<ENode> result = new ArrayList<ENode>();
	    for(Item it: selection) {            
	        if(it instanceof Point) {
	            Point p = (Point)it;	      
	            ENode e = addEntityForPoint(p);
	            result.add(e);
	        }
        }
	    return result.toArray(new ENode[]{});
	}

    /** 
	 * Accepts tex coordinates of the Entity's center
	 */
    public ENode addEntity(int posX, int posY) {
		ENode e = new ENode(this, null, false);
		float w = e.getWidth2D();
		float h = e.getHeight2D();
		e.setBounds2D(posX-w/2, getHeight()-h/2-posY, w, h);
		addItem(e, posX-w/2, posY-h/2, true);
        return e;
	}

    public ENode addEntityForPoint(Point p) {
        Rectangle2D.Float r = (Rectangle2D.Float)p.getBounds2D();
        return addEntity((int)(r.x+r.width/2), getHeight()-(int)(r.y+r.height/2));
    }

    public void addItem(Item item, boolean addListeners) {    	
        Rectangle2D.Float r = (Rectangle2D.Float)item.getBounds2D();
        addItem(item, r.x, getHeight() - r.y - r.height, addListeners);
    }
	
    /**
	 *  accepts tex coordinates of the lower left corner.
	 */
    public void addItem(Item item, float x, float y, boolean addListeners) {
    	add(item, x, y);
    	//System.err.println("add: "+item+" "+addListeners);
		if(addListeners) {
		    item.addMouseListener(this);
		    item.addMouseMotionListener(this);
		}
	}
    
    public Point addPoint(int posX, int posY) {
        Point p = new Point(this, MARK_COLOR0);        
		float w = p.getWidth2D();
		float h = p.getHeight2D();
		java.awt.Point pt0 = new java.awt.Point(posX, posY);
		java.awt.Point pt1 = grid.getNearest(pt0, null, null, new boolean[] {false});
		add(p, pt1.x, pt1.y);
		p.setBounds2D(pt1.x-w/2, getHeight()-pt1.y-h/2, w, h);
        repaint();
        return p;
    }

    public void addSelectionListener(SelectionListener cl) {
        selectionListeners.add(cl);
    }	

    public void clearSelection() {
        clearSelection(null);
    }	

    public void clearSelection(Item item) {
        clearSelection(item, true, true, true);
	}	

    public void clearSelection(final Item item, boolean selectGroup, boolean setItemToBeEdited, boolean fireSelectionChanged) {
        if(item!=null && !selectGroup) { // only item will be selected, and nothing else should be preselected 
            dePreselect(item);
            marked1.remove(item); // to avoid interference with the blinker thread
        }
        HashSet<Item> toDeselect = new HashSet<Item>();
	    synchronized(selection) {
	        Iterator<Item> i = selection.iterator();
	    	for(;i.hasNext();) {
	        	Item it = i.next();
	        	preselection.remove(it);
	        	toDeselect.add(it);
	    	}
	    	selectedEntityCount = 0;    	
	        selection.clear();
		    for(Item it: toDeselect) {
		    	deselect1(it);
		    }
		    adjustMarkings();
	    }	    
    	
        if(item!=null) {
        	select1(item, -1);
        	if(selectGroup) {
        		selectGroup((GroupMember<?>)item);
        	}       		
        }
        if(setItemToBeEdited) {
            setItemToBeEdited(item);
        }        
	    if(fireSelectionChanged) {
	    	fireSelectionChanged();
	    }
	    
        resetTransform();
        
        repaint();
    }

    public void copySelection() {
        Map<Object, Object> cloneMap = Collections.synchronizedMap(new HashMap<Object, Object>());
        Map<CNode, Boolean> cNodeMap = new HashMap<CNode, Boolean>();
        Map<Contour, Boolean> contourMap = new HashMap<Contour, Boolean>();
        
        /*
         *  Copy selection.
         */
        for (Item item : selection) {
            if(item instanceof IndependentItem && item.getGovernor()==null) {
                IndependentItem<?> e = (IndependentItem<?>)item;
                try {
                	IndependentItem<?> ec = (IndependentItem<?>)e.clone(cloneMap);
                    Rectangle2D.Float r = (Rectangle2D.Float)e.getBounds2D();
                    addItem(ec, r.x, getHeight() - r.height - r.y, true);	                
                } catch(CloneNotSupportedException cnse) {
                	handler.handle(cnse, this, "Copying not supported");
                }
            }
        }
        
        /*
         * Record clones of the selected Items and of itemToBeEdited.
         */
        List<Item> selection1 = new LinkedList<Item>();
        for(Item item: selection) {
            Item itemc = (Item)cloneMap.get(item);
            if(itemc!=null) selection1.add(itemc);
        }
        Item item1 = (Item)cloneMap.get(getItemToBeEdited());
        
        clearSelection();

        addDependentItems(cloneMap, false);        

        /*
         * Turn off fixedAngles and fixedCornerAngle for Contour clones.
         */
        for(Object clone: cloneMap.values()) {
	        if(clone instanceof CNode) {
	        	CNode cn = (CNode)clone;
	        	cNodeMap.put(cn, cn.isCornerAngleFixed());
	        	cn.setCornerAngleFixed(false);
	        } else if(clone instanceof Contour) {
	        	Contour cont = (Contour)clone;
	        	contourMap.put(cont, cont.isFixedAngles());
	        	cont.setFixedAngles(false);
	        }
        }
        
        /*
         * Shift copies to new location.
         */
        prepareForMovingSelection(selection1);
        moveSelectionBy(hDisplacement, vDisplacement);
        relocateItems();
        
        /*
         * Reset variables of CNodes and Contours.
         */
        for(CNode cn: cNodeMap.keySet()) {
        	cn.setCornerAngleFixed(cNodeMap.get(cn));
        }
        for(Contour cont: contourMap.keySet()) {
        	cont.setFixedAngles(contourMap.get(cont));
        }

        /*
         * Select copies.
         */
        for(Item clone: selection1) {
            select1(clone, -1);
        }
        setItemToBeEdited(item1);
	    fireSelectionChanged();
    }
	
    /**
     * De-preselects the supplied Item as well as all those that have been preselected only because of their membership in the 
     * same group as the former.
     */
	public void dePreselect(Item item) {
	    preselection.remove(item);
	    Set<Item> s = new HashSet<Item>(); // the set of Items that are eventually to be de-preselected   
	    s.addAll(marked1.keySet()); 
        for(Iterator<Item> i = s.iterator(); i.hasNext(); ) {
        	Item it = i.next();
            if(isPreselected(it)) {
                i.remove();
            }
        }
        dePreselect1(s);
	}
	
	private void dePreselect1(final Set<Item> items) {
		if(!items.isEmpty()) {
    		new Thread() {
        		@Override
                public void run() {
        			for(Iterator<Item> i = items.iterator(); i.hasNext();) {
        				Item item = i.next();
        		    	if(selection.contains(item)) {
        		    		i.remove();
        		    		marked.remove(item);
        		    		marked1.remove(item);
        		    	}
        		    }
        			if(!items.isEmpty()) {
        				Iterator<Item> iter = items.iterator();
        				/*
        				 * 'Officially', markBorders1a and markBorders0a may have very different lengths, but we are not going to 
        				 * take that into account. We take the first element we find as indicating the level of highlighting for
        				 * the whole set. 
        				 */
        				Item item0 = iter.next();
    	    			MarkBorder[] borders0 = (item0 instanceof ENode)? markBorders1a: markBorders0a;
    	    			MarkBorder border0 = (MarkBorder)item0.getBorder();
    	    			int k = matchingBorderIndex(border0, borders0);
    	    			if(k!=0) {
    	    				Set<Item> toRemove = new HashSet<Item>();
    		    			for(int i = k; i>=0; i--) {
    		    				items.removeAll(toRemove);
    		        			for(Item item: items) {
    		    	    			Border[] borders = (item instanceof ENode)? markBorders1a: markBorders0a;
    		        				if(!isPreselected(item) && !item.isSelected()) {
    				    			    item.setBorder(borders[i]);
    				    				marked1.put(item, i);
    			        			} else {
    			        				toRemove.add(item);
    			        			}
    		        			}
    		    				try{				    					
    		    					sleep(FADE_SLEEP);
    		    				}
    		    				catch(InterruptedException ie) {}
    		    			}
    	    			}
            			for(Item item: items) {
            				if(!isPreselected(item)) {
    			        		marked.remove(item);
    			        		marked1.remove(item);
    			        		if(!selection.contains(item)) {
    			        			item.setBorder(null);
    			        		}
            				}
            			}
        			}
    	        }
        	}.start();
		}
	}
	
    public void deselect(final Item item) {
    	deselect1(item);
        adjustMarkings();
        fireSelectionChanged();
    }
    
    /**
	 * Deselects an item, but does not fire a selectionChanged event. The method will remove the last occurrence of the item from
	 * the selection, if it is still there.
	 */
    protected void deselect1(final Item item) {
        int i = selection.lastIndexOf(item);
        if(i>=0) {
        	selection.remove(i);
        }
        if(!selection.contains(item)) {
            if(item==itemToBeEdited) {
                setItemToBeEdited(null);
                adding = false;
            }
        	item.setSelected(false);
        	if(item instanceof Point) {
        	    removeItem(item);
        	} else {
    			final MarkBorder[] borders = (item instanceof ENode)? markBorders1a: markBorders0a;
    			int k0 = borders.length-(int)((float)borders.length/5); // quicker start
    			if(item==itemToBeEdited) { // adjust the starting point of the fading
    				MarkBorder mb = (MarkBorder)item.getBorder();
    				if(mb!=null) {	    			
    					k0 = Math.min(k0, matchingBorderIndex(mb, borders)-1);
    				}
    			}
    			final int start = k0;
            	new Thread() {
            		@Override
                    public void run() {        			
            			for(int i = start; 
            					i>0 && !item.isSelected() && (!isPreselected(item) || i>borders.length/2); 
            					i--) { 
            				marked1.put(item, i);
            				item.setBorder(borders[i]);        			
    	        			try {
    	        				sleep(FADE_SLEEP/2);
    		        		}	        			
    	        			catch(InterruptedException ie) {}
            			}
            			if(!selection.contains(item) && !isPreselected(item)) {
    	        			marked1.remove(item);
    	    				item.setBorder(null);
    	    				item.repaint(); // may be necessary to ensure that the 'title string' goes away, too (not tested)
            			}
            		}
            	}.start();
        	    relocate(item);
        	    if(i>=0 && item instanceof ENode) {
        	        selectedEntityCount--;
        	    }
        	}
        }
    }
	
    private Set<Item> getAllMembers(Group<?> g) {
    	Set<Item> set = new HashSet<Item>();
    	getAllMembers1(g, set);
    	return set;
    }

    private void getAllMembers1(Group<?> g, Set<Item> set) {
        Collection<?> members = g.getMembers();
    	for(Object o: members) {
            GroupMember<?> gm1 = (GroupMember<?>)o;
            if(gm1.isInGroup()) {
		        if(gm1 instanceof Group) {
		            getAllMembers1((Group<?>)gm1, set);
		        }
		        else {
		        	set.add((Item)gm1);
		        }
            }
        }
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public void doHorizontalFlip() {
        flip(HORIZONTAL);
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public void doVerticalFlip() {
        flip(VERTICAL);
    }

    ENode[] extractEntities(List<Item> l) {
    	ENode[] e = new ENode[selectedEntityCount];
    	int i = 0;
    	for(Iterator<Item> j = l.iterator(); j.hasNext();) {
    		Item it = j.next();
    		if(it instanceof ENode) {
    			e[i++] = (ENode)it;
    		}
    	}
    	return e;
    }
    
    protected void fireEditFailed(EditEvent ee) {
    	for(EditListener el: editListeners) {
    		el.editFailed(ee);
    	}
    }
    
    protected void fireEditing(EditEvent ee) {
    	for(EditListener el: editListeners) {
    		el.editing(ee);
    	}
    }
    
    protected void fireSelectionChanged() {
        SelectionEvent se = new SelectionEvent(this);
        for (SelectionListener selectionListener : selectionListeners) {
            selectionListener.selectionChanged(se);
        }
    }
    
    
    protected void fireStartNewPeriod() {
    	EditEvent ee = new EditEvent(this);
    	for(EditListener el: editListeners) {
    		el.startNewPeriod(ee);
    	}
    }
    
    public void flip(Orientation o) {
        Point2D c = null;
        Item lastSelected = selection.get(selection.size()-1);
        if(lastSelected instanceof Point) {
            c = lastSelected.getCenter();
        } else if(itemToBeEdited!=null) {
            c = itemToBeEdited.getCenter();
        } else {
            c = new Point2D.Float(getWidth()/2, getHeight()/2);
        }        
        
        if(o==HORIZONTAL) {
	        selectionTransform.setToTranslation(2*c.getX(), 0);
	        selectionTransform.scale(-1, 1);
        } else {
	        selectionTransform.setToTranslation(0, 2*c.getY());
	        selectionTransform.scale(1, -1);
        }
        

        prepareForMovingSelection();        
        Map<CNode, Boolean> fcaMap = new HashMap<CNode, Boolean>(); // records the fixedCornerAngle properties
		for(Item item : selectedSet) {
        	if(item instanceof CNode) {
        		CNode cn = (CNode)item;
        		fcaMap.put(cn, cn.hasFixedCornerAngle());
        		cn.setFixedCornerAngle(false);
        	}
		}
		moveSelection();
		for(Item item : selectedSet) {
			if(item instanceof CNode) {
	        	CNode cn = (CNode)item;
	        	cn.setFixedCornerAngle(fcaMap.get(cn));
	        }
		}
        relocateItems();
        selectionTransform.setToIdentity();
    }

    public float getAngularIncrement() {
        return (float)Math.log10(((Float)rotateInfo.getIncrement()).floatValue());
    }
    
    public Runnable getCompoundArrowDissolve(final List<Item> l, final boolean setPriority, final boolean addListeners) {
        return null;
    }
    
    public int getCurrentPriority() {
        return currentDIPriority;
    }
    
    public Editable getEditorDelegate(EditorTopic topic) {
	    return null;
    }
    
    public EditorEntry getFlipArrowheadsInfo() {
    	return flipArrowheadsInfo;
    }
    
    public Set<Editable> getGlobalSet(EditorTopic topic) {
	    HashSet<Editable> result = new HashSet<Editable>();
	    result.add(this);
		return result;
    }
    
    public Grid getGrid() {
        return grid;
    }
    
    public int getHorizontalDisplacement() {
        return hDisplacement;
    }
    
    public int getHorizontalGap() {
        return grid.hGap;
    }
    
    public int getHorizontalShift() {
        return grid.hShift;
    }
	
    public List<EditorEntry> getInfo() {
        if(info==null) {
		    ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
		    
		    EditorEntry dimLabel = new EditorEntry("LabelInfo", LABEL, "Canvas Dimensions");
		    result.add(dimLabel);
		    
		    EditorEntry widthInfo = new EditorEntry("Width", INTEGER, "Width");
		    widthInfo.setSpinnerValues(Float.NaN, 1, MAX_WIDTH, 10, false);
		    result.add(widthInfo);
		    
		    EditorEntry heightInfo = new EditorEntry("Height", INTEGER, "Height");
		    heightInfo.setSpinnerValues(Float.NaN, 1, MAX_HEIGHT, 10, false);
		    result.add(heightInfo);
		    
		    EditorEntry gridLabel = new EditorEntry("LabelInfo", LABEL, "Grid Width");
		    gridLabel.setTopSpace(7);
		    result.add(gridLabel);
		    
		    EditorEntry gridXInfo = new EditorEntry("HorizontalGap", INTEGER, "Horizontal");
		    gridXInfo.setSpinnerValues(1, 1, Grid.MAX_HGAP, 3, false);
		    gridXInfo.setDefaultValue(new Integer(1));
		    result.add(gridXInfo);
		    
		    EditorEntry gridYInfo = new EditorEntry("VerticalGap", INTEGER, "Vertical");
		    gridYInfo.setSpinnerValues(1, 1, Grid.MAX_VGAP, 3, false);
		    gridYInfo.setDefaultValue(new Integer(1));
		    result.add(gridYInfo);
		    
		    EditorEntry shiftLabel = new EditorEntry("LabelInfo", LABEL, "Grid Offset");
		    shiftLabel.setTopSpace(7);
		    result.add(shiftLabel);
		    
		    EditorEntry shiftXInfo = new EditorEntry("HorizontalShift", INTEGER, "Horizontal");
		    shiftXInfo.setSpinnerValues(0, -Grid.MAX_HSHIFT, Grid.MAX_HSHIFT, 1, false);
		    shiftXInfo.setDefaultValue(new Integer(0));
		    result.add(shiftXInfo);
		    
		    EditorEntry shiftYInfo = new EditorEntry("VerticalShift", INTEGER, "Vertical");
		    shiftYInfo.setSpinnerValues(0, -Grid.MAX_VSHIFT, Grid.MAX_VSHIFT, 1, false);
		    shiftYInfo.setDefaultValue(new Integer(0));
		    result.add(shiftYInfo);
		    
		    EditorEntry snInfo = new EditorEntry("ContourNodeGrid", BOOLEAN, "Snap to contour nodes");
		    snInfo.setDefaultValue(Boolean.valueOf(true));
		    snInfo.setTopSpace(4);
		    snInfo.setLeftSpace(-10);
		    result.add(snInfo);	    

		    EditorEntry scInfo = new EditorEntry("ContourCenterGrid", BOOLEAN, "Snap to cont. centers");
		    scInfo.setDefaultValue(Boolean.valueOf(true));
		    scInfo.setTopSpace(-3);
		    scInfo.setLeftSpace(-10);
		    result.add(scInfo);	    
		    
		    EditorEntry dirLabel = new EditorEntry("LabelInfo", LABEL, "Copy Displacement");
		    dirLabel.setTopSpace(7);
		    result.add(dirLabel);
		    
		    EditorEntry displacementXInfo = new EditorEntry("HorizontalDisplacement", INTEGER, "Horizontal");
		    displacementXInfo.setSpinnerValues(DEFAULT_HDISPLACEMENT, -MAX_HDISPLACEMENT, MAX_HDISPLACEMENT, 1, false);
		    displacementXInfo.setDefaultValue(new Integer(DEFAULT_HDISPLACEMENT));
		    result.add(displacementXInfo);
		    
		    EditorEntry displacementYInfo = new EditorEntry("VerticalDisplacement", INTEGER, "Vertical");
		    displacementYInfo.setSpinnerValues(DEFAULT_VDISPLACEMENT , -MAX_VDISPLACEMENT, MAX_VDISPLACEMENT, 1, false);
		    displacementYInfo.setDefaultValue(new Integer(DEFAULT_VDISPLACEMENT));
		    result.add(displacementYInfo);
		    
		    EditorEntry resetInfo = new EditorEntry(null, RESET, "Defaults");
		    resetInfo.setTopSpace(7);
		    result.add(resetInfo);

		    result.trimToSize();
		    this.info = result;
	    }
	    return info;
        
    }
    
    public List<EditorEntry> getInfo(EditorTopic topic) {
        switch(topic) {
        case ITEM: return getInfo();
        case TRANSFORM: return getTransformInfo();
        default: return null;
        }
    }

    private List<CNode> getInitialSNodes() {
	    java.awt.Point c = new java.awt.Point(DEFAULT_WIDTH/2, DEFAULT_HEIGHT/2);
	    int dx = 35;
	    int dy = 25;
	    int s = 16;
	    double a = Math.PI/4;
	    float r = 10;
	    
	    if(Item.DEFAULT_DIRECTION!=Direction.COUNTERCLOCKWISE) {
	    	dx = -dx;
	    	dy = -dy;
	    	s = -s;
	    	a = -a;
	    }
	    
	    List<CNode> result = new LinkedList<CNode>();
	    CNode sn; 
	    sn = new CNode(this, new java.awt.Point(c.x - dx + s, c.y - dy));
	    sn.setValues(0, r, -a, r, false);
	    result.add(sn);
	    sn = new CNode(this, new java.awt.Point(c.x - dx, c.y - dy + s));
	    sn.setValues(a, r, 0, r, false);
	    result.add(sn);
	    sn = new CNode(this, new java.awt.Point(c.x - dx, c.y + dy - s));
	    sn.setValues(0, r, -a, r, false);
	    result.add(sn);
	    sn = new CNode(this, new java.awt.Point(c.x - dx + s, c.y + dy));
	    sn.setValues(a, r, 0, r, false);
	    result.add(sn);
	    sn = new CNode(this, new java.awt.Point(c.x + dx - s, c.y + dy));
	    sn.setValues(0, r, -a, r, false);
	    result.add(sn);
	    sn = new CNode(this, new java.awt.Point(c.x + dx, c.y + dy - s));
	    sn.setValues(a, r, 0, r, false);
	    result.add(sn);
	    sn = new CNode(this, new java.awt.Point(c.x + dx, c.y - dy + s));
	    sn.setValues(0, r, -a, r, false);
	    result.add(sn);
	    sn = new CNode(this, new java.awt.Point(c.x + dx - s, c.y - dy));
	    sn.setValues(a, r, 0, r, false);
	    result.add(sn);

	    return result;
	}
    
	public Item getItemToBeEdited() {
        return itemToBeEdited;
    }
   
    public String getLabelInfo() {
        return "";
    }
    
    public EditorEntry getRotateInfo() {
    	return rotateInfo;
    }
    
    public EditorEntry getRotateLabelsInfo() {
    	return rotateLabelsInfo;
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public float getRotation() {
        return (float)Math.toDegrees(rotation);
    }

    public EditorEntry getRotationIncrementInfo() {
    	return rotationIncrementInfo;
    }

    public EditorEntry getScaleArrowheadsInfo() {
    	return scaleArrowheadsInfo;
    }
	
	public EditorEntry getScaleENodesInfo() {
    	return scaleENodesInfo;
    }
    
    public EditorEntry getScaleInfo() {
    	return scaleInfo;
    }
    
    public EditorEntry getScaleLinewidthsInfo() {
    	return scaleLinewidthsInfo;
    }

    /**
     * Invoked by an EditorPane.
     */
    public float getScaling() {
        return 100f*scaling;
    }

    public float getScalingIncrement() {
        return (float)Math.log10(((Float)scaleInfo.getIncrement()).floatValue());
    }
    
    public EditorEntry getScalingIncrementInfo() {
    	return scalingIncrementInfo;
    }

    public Set<Item> getSelectedSet() {
        Set<Item> result = new HashSet<Item>();
        result.addAll(selection);
        return result;        
    }
    
    public String getTitleString(ENode e) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Item item : selection) {
			if(item instanceof ENode) {
				if(item==e) {
				    if(sb.length()>0) sb.append(", ");
					sb.append(new Integer(i+1).toString());
				}
				i++;
			}
		}
		return sb.toString();		
	}
    
    public List<EditorEntry> getTransformInfo() {
        if(transformInfo==null) {
	        ArrayList<EditorEntry> result = new ArrayList<EditorEntry>();
	 
		    rotateInfo = new EditorEntry("Rotation", FLOAT, "Rotate");
		    rotateInfo.setSpinnerValues(0, -180, 180, 10f, true);
		    rotateInfo.requestNotifier();
		    rotateInfo.setTopSpace(3);
		    result.add(rotateInfo);
		    
		    rotationIncrementInfo = new EditorEntry("AngularIncrement", FLOAT, "  log Increment");
		    rotationIncrementInfo.setSpinnerValues(1f, -3f, 2f, 1f, false);
		    rotationIncrementInfo.requestOwnPanel(true);
		    rotationIncrementInfo.setTopSpace(-1);
		    result.add(rotationIncrementInfo);
	
		    rotateLabelsInfo = new EditorEntry("RotatingLabels", BOOLEAN, "Rotate labels");
		    //result.add(rlInfo);	    
	
		    scaleInfo = new EditorEntry("Scaling", FLOAT, "Scale %");
		    scaleInfo.setSpinnerValues(100, 0, 9999, 10f, false);
		    scaleInfo.requestNotifier();
		    scaleInfo.setTopSpace(5);
		    result.add(scaleInfo);
		    
		    scalingIncrementInfo = new EditorEntry("ScalingIncrement", FLOAT, "  log Increment");
		    scalingIncrementInfo.setSpinnerValues(1f, -3f, 2f, 1f, false);
		    scalingIncrementInfo.requestOwnPanel(true);
		    scalingIncrementInfo.setTopSpace(-1);
		    result.add(scalingIncrementInfo);
		    
		    scaleArrowheadsInfo = new EditorEntry("ScalingArrowheads", BOOLEAN, "Scale arrow heads");
		    scaleArrowheadsInfo.setTopSpace(-1);
		    result.add(scaleArrowheadsInfo);	    
	
		    scaleENodesInfo = new EditorEntry("ScalingNodes", BOOLEAN, "Scale entity nodes");
		    scaleENodesInfo.setTopSpace(-1);
		    result.add(scaleENodesInfo);	    
	
		    scaleLinewidthsInfo = new EditorEntry("ScalingLines", BOOLEAN, "Scale line widths");
		    result.add(scaleLinewidthsInfo);	    
	
		    EditorEntry hfInfo = new EditorEntry("HorizontalFlip", ACTION, "Horizontal Flip");
		    hfInfo.setTopSpace(7);
		    result.add(hfInfo);
	
		    EditorEntry vfInfo = new EditorEntry("VerticalFlip", ACTION, "Vertical Flip");
		    result.add(vfInfo);

		    flipArrowheadsInfo = new EditorEntry("FlippingArrowheads", BOOLEAN, "Flip arrow heads");
		    result.add(flipArrowheadsInfo);	    

		    result.trimToSize();
		    transformInfo = result;
        }
        return transformInfo;
    }
	
    public int getTransformModifier() {
    	return transformModifier;
    }

    public FloatSpinner getUnitSpinner() {
	    return unitSpinner;
    }

    public int getVerticalDisplacement() {
        return -vDisplacement;
    }
    
    public int getVerticalGap() {
        return grid.vGap;
    }    
    
    public int getVerticalShift() {
        return grid.vShift;
    }

	public boolean isAdding() {
        return adding;
    }
	
    public boolean isAddingMembers() {
        return addMembers;
    }

	/**
     * Invoked by an EditorPane.
     */
    public boolean isContourCenterGrid() {
        return grid.snapToContourCenters;
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public boolean isContourNodeGrid() {
        return grid.snapToCNodes;
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public boolean isFlippingArrowheads() {
        return TransformModifier.isFlippingArrowheads(transformModifier);
    }
    
    private boolean isPreselected(Item item) {
    	if(selection.contains(item)) {
	        return false;
	    } 
	    else if(preselection.contains(item)) {
	        return true;
	    }
	    else {
	        Set<GroupMember<?>> s = marked.get(item); 
	        if(s==null) {
	            return false;
	        }
	        else synchronized(s) {
		        s.retainAll(preselection);
		        return !s.isEmpty();
	        }
	    }
	}
 
    /**
     * Invoked by an EditorPane.
     */
	public boolean isRotatingLabels() {
        return TransformModifier.isRotatingLabels(transformModifier);
    }

    /**
     * Invoked by an EditorPane.
     */
    public boolean isScalingArrowheads() {
        return TransformModifier.isScalingArrowheads(transformModifier);
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public boolean isScalingLines() {
        return TransformModifier.isScalingLines(transformModifier);
    }
	    
    /**
     * Invoked by an EditorPane.
     */
    public boolean isScalingNodes() {
        return TransformModifier.isScalingNodes(transformModifier);
    }

    private void markAsSelected(Item it) {
        if(it instanceof ENode) {   
	        it.setBorder(new MarkBorder(MARK_COLOR1, CONTRAST_COLOR1, getTitleString((ENode)it)));
	    } 
		else {
	        it.setBorder(new MarkBorder(MARK_COLOR0, CONTRAST_COLOR0));
		}
    }

	private void adjustMarkings() {
        for(Item it : selection) { 
            markAsSelected(it);
        }
        blinker.requestRestart();
	}

	public void markForRelocation(Item item) {
    	synchronized(toRelocate) {
    		toRelocate.add(item);
    	}
    }
	
	public void markForRelocation(Set<? extends Item> s) {
    	synchronized(toRelocate) {
    		toRelocate.addAll(s);
    	}
    }
	
	public void mouseClicked(MouseEvent me) {}
	
	
    public void mouseDragged(MouseEvent me) {
        
        Component comp = me.getComponent();
        if(comp==this && lasso==null) {
        	prepareForLassoing(me);
        }

        if(draggingBlocked) return;
    	
        
        // deal with lassoing first:
        if(comp==this) {
       		lasso.setEndPoint(me.getPoint(), me.isAltDown());
        }
        
        if(!((comp instanceof Label || comp instanceof IndependentItem) && ((Item)comp).isSelected())) return;
        
        if(comp instanceof Label) { // ensure that the 'governor' ENode will be dragged along with the Label:
        	Label lb = (Label)comp;
        	comp = lb.getGovernor();
        	if(!selection.contains(comp)) {
        		selection.add((Item)comp);
        	}
        }
        
        if(!dragging) prepareForDragging(me);

        /*
         * Determine the point to which the itemToBeEdited is dragged. This point should be a snapping point, or, if the item
         * is a third-relatum node, the closest point to such a snapping point on the corresponding connector; 
         * the other Items are then placed accordingly.
         */
        java.awt.Point p0 = me.getPoint();
        IndependentItem<?> item = (IndependentItem<?>)comp;
        int dx0 = p0.x - mouseX;
        int dy0 = p0.y - mouseY;     
        int h = getHeight();        

        Point2D c1 = item.getCenter();
        float cx1 = (float)c1.getX();
        float cy1 = (float)c1.getY();
        Point2D.Float p1 = new Point2D.Float(cx1 + dx0, h - (cy1 + dy0));

        Point2D.Float p2 = null;
        float cx2 = 0;
        float cy2 = 0;
        if(draggedContour!=null && grid.snapToContourCenters) {
	        Point2D c2 = draggedContour.getCenter();
	        cx2 = (float)c2.getX();
	        cy2 = (float)c2.getY();
	        p2 = new Point2D.Float(cx2 + dx0, h - (cy2 + dy0));
        }        
        
        boolean[] cc = new boolean[] {false};
        java.awt.Point p = grid.getNearest(p1, p2, selectedSet, cc);

        Item gov = item.getGovernor();
    	if(gov instanceof DependentItem) {
            /*
             * Choose the nearest point on the connector to the nearest point on the grid:
             */
    		p.y = h-p.y;
    		int dx1 = (int)Math.round(c1.getX()) - item.getX(); 
    		int dy1 = (int)Math.round(c1.getY()) - item.getY(); 
    	    Point2D p3 = ((DependentItem)gov).setItemLocation(p);
    	    p.setLocation(p3.getX() + dx1, h - p3.getY() - dy1);
    	}
        
        moveSelectionBy(p.x - (cc[0]? cx2: cx1), (h - p.y) - (cc[0]? cy2: cy1));
    }

 	public void mouseEntered(MouseEvent me) 
    {
    	Component c = me.getComponent();
  
	    boolean ctrl = me.isControlDown();

	    if(c instanceof Item && !dragging && lasso==null && !selection.contains(c)) {
		    Item item = (Item)c;
        	if(ctrl) {
        		Set<Item> s = new HashSet<Item>();
        		s.add(item);
        	    preselect1(s, null);
        	} else {
        	    preselect(item);
        	}
        }
    }
	
	public void mouseExited(MouseEvent me) {
    	
		if(lasso!=null) return;
    	
		Component c = me.getComponent();
    	if(c instanceof Item) {
    	    dePreselect((Item)c);
    	}
    }

    public void mouseMoved(MouseEvent me) {}

    public void mousePressed(final MouseEvent me)
    {
		final Component c = me.getComponent();
	    final boolean shift = me.isShiftDown();
	    final boolean ctrl = me.isControlDown();
	    /*
	     * A new thread is necessary for the call(s) to EditorPane#waitTillUnblocked(), and those calls are necessary
	     * because some Items have EditorPane-invoked methods that operate on the selection of this Canvas.
	     */        
	    draggingBlocked = true; // block dragging till the Thread is done with adjusting the selection.
	    new Thread() {
            @SuppressWarnings("unchecked")
			@Override
            public void run() {
                synchronized(Canvas.this) {
	                for(EditorPane pane: editorPanes) {
	                    pane.waitTillUnblocked(); 
	                }
			
				    boolean addingFailed = false;
				    boolean addingMembers = addMembers || 
				    	itemToBeEdited instanceof GroupUIDelegator && 
				    	((GroupUIDelegator)itemToBeEdited).getGroupManager().isAddingMembers();
				    
				    boolean increaseSelection = shift || selection.isEmpty();
				    boolean reduceSelection = ctrl && selection.contains(c) && !increaseSelection;
				    boolean selectSingle = ctrl && !selection.contains(c);
				    boolean selectGroup = !ctrl;
				    
				    if(!(c instanceof Item || increaseSelection || ctrl)) { // ctrl because of its use in lassoing (and that's
				    			// why the following code has to be repeated in mouseReleased();).
				    	clearSelection(null, false, true, false);
						/*
						 * Since the adjustment of the EditorPanes can visually screw up the buttonPanel, revalidate it here.
						 */
						buttonPanel.revalidate();
					}
				    if(!(c instanceof GroupMember)) {
				        adding = false;
				    } else if(adding && !reduceSelection) {
				    	EditEvent ee = new EditEvent(Canvas.this, true);
				    	fireEditing(ee);
				        addingFailed = !performAdding((GroupMember<StandardGroup>)c, selectGroup, addingMembers);
				        if(addingFailed) {
				        	fireEditFailed(ee);
				        }
				    }	
					if(c instanceof Item && !addingFailed) { // handle the selection		    
					    Item item = (Item)c;
					    
					    boolean inSameGroupAsITBE = false;
					    if(itemToBeEdited instanceof IndependentItem && item instanceof IndependentItem) {
				            Group g0 = Groups.getHighestActiveGroup((GroupMember)itemToBeEdited); 
				            Group g1 = Groups.getHighestActiveGroup((GroupMember)item);
				            inSameGroupAsITBE = g0!=null && g1!=null && g0==g1;
				        }
					    if(reduceSelection) {
					    	deselect(item);
					    } else if(increaseSelection && !selectGroup) {
			                select1(item, -1);
						    setItemToBeEdited(item);
						    fireSelectionChanged();
			            } else if(selectSingle) { // select only item, deselect everything else
			                clearSelection(item, false, true, true);
					    } else if(increaseSelection) {
					        select(item, true);		        
						    setItemToBeEdited(item);
					    } else if(adding) { 
					        if(!item.isSelected()) select(item, true);
						    setItemToBeEdited(item);
					    } else if(item.isSelected() && inSameGroupAsITBE) {
						    setItemToBeEdited(item);
					    } else {				    	
					        clearSelection(item);
					    }
					}
					relocateItems(); // some items (e.g., Axes) might have been added to toRelocate during selection.
					draggingBlocked = false;
	            }
            }
        }.start();
    }

    public void mouseReleased(MouseEvent me)
    {
    	if(!dragging && lasso==null) {
    		Component c = me.getComponent();
		    if(!(c instanceof Item || me.isShiftDown())) { 
		    	clearSelection(null, false, true, false);
				/*
				 * Since the adjustment of the EditorPanes can visually screw up the buttonPanel, revalidate it here.
				 */
				buttonPanel.revalidate();
			}
    		if(c==this) {
    			select(addPoint(me.getX(), getHeight()-me.getY()), false);
    		}
    	} else {        	
            if(lasso!=null) {
            	lasso.select();
            	lasso.setVisible(false);
            	remove(lasso);        	
            	lasso = null;
            }
            
            dragging = false;
            
            relocateItems();
    	}
	}
    
    public void mouseWheelMoved(MouseWheelEvent mwe) {		
	}
	
    public void moveSelection() {
        moveSelectionBy(0, 0);    
    }
    
    /**
     * Performs a translation by dx and dy, in addition to selectionTransform.
     */
    public void moveSelectionBy(float dx, float dy) {
        AffineTransform t = selectionTransform;
        boolean mereTranslation = t==null || MathTools.isMereTranslation(t);
                
        /*
         * Move the selected Entities to their new positions (which, for third relata, will only be tentative).
         */        
        Point2D.Float c0 = new Point2D.Float();
        Point2D.Float c1 = new Point2D.Float();
        Map<Contour, Set<CNode>> map = new HashMap<Contour, Set<CNode>>();
		for(Set<Item> set : selectSet) {
	        for(Item item : set) { // selectSet = {selectedSet, internal, thirds}
	            if(item instanceof IndependentItem) {
		            if(!mereTranslation) {
		                Rectangle.Float r = (Rectangle2D.Float)item.getBounds2D();
		                c0.setLocation(r.x+r.width/2, r.y+r.height/2);
		                t.transform(c0, c1);
		                item.move(c1.x-r.width/2, c1.y-r.height/2);
		            }
		            if(item instanceof CNode) {
		            	CNode cn = (CNode)item;
		            	Contour cont = cn.getContour();
		            	Set<CNode> nodes = map.get(cont);
		            	if(nodes==null) {
		            		nodes = new HashSet<CNode>();
		            		map.put(cont, nodes);
		            	}
		            	nodes.add(cn);
		            } else {
		            	item.moveBy(dx, dy);
		            }
	            } else if(item instanceof Connector) {
	                Connector rel = (Connector)item;
	                rel.invalidateShape(false);
	            } else {
	                item.invalidate();
	            }
                item.adjustToTransform(t, transformModifier);
	        }
        }
        for(Item item: selectedSet) {
        	Item gov = item.getGovernor();
        	if(gov instanceof Connector) {
        		Connector con = (Connector)gov;
        		con.invalidateShape(false);
        		con.adjustToTransform(t, transformModifier);
        		con.defineBounds();        		
        	}
        }
        for(Contour sp: map.keySet()) {
        	// The fixedCornerAngle property of the CNodes that are being moved can be ignored, and *should* be ignored
        	// in the case of flipping.
        	sp.moveNodes(map.get(sp), dx, dy);
        }
        
        /*
         * Set the locations of the selected items of DIs that are not in the set of internal DIs, and arrange the 
         * contacts of all other Items contained in the boundary set.
         */
        Set<Item> affected = new HashSet<Item>();
        for(Item item : boundary) {
	        Rectangle2D.Float r = (Rectangle2D.Float)item.getBounds2D();
	        float w = r.width;
	        float h = r.height;
	        if(item instanceof IndependentItem) {
	            IndependentItem<?> i2 = (IndependentItem<?>)item;
		        Item gov = i2.getGovernor();
	        	if(gov!=null && !internal.contains(gov) && !(dragging && i2==itemToBeEdited)) {
	        		// as for the last condition: the primarily selected item should be left where it has just been dragged.
	        	    c0.setLocation(r.x + w/2, r.y + h/2);
	        	    Point2D.Float p2 = (Point2D.Float)((DependentItem)gov).setItemLocation(c0);
	    	        i2.setBounds2D(p2.x, p2.y, w, h);
	    	        i2.arrangeContacts(affected, immune);
	        	}
	        	else {
	    	        i2.arrangeContacts(affected, immune);	    	        
	        	}
	        } 
        }
        toRelocate.addAll(affected);

        /*
         * Redefine the shapes and bounds of the DependentItems.
         */
        for(Set<DependentItem> set: new Set[] {internal, external}) {
	        for (DependentItem di : set) { 
	            if(!affected.contains(di)) {
		            if(di instanceof Connector) {
		                ((Connector)di).invalidateShape(false);
		            } else {
		                di.invalidate();
		            }
		            di.defineBounds();
	            }
	        }
        }
   }
    
    /**
     * ascendToGroup && addMembers: add to g the members of the highest active group of gm, which may themselves be groups.
     * ascendToGroup && !addMembers: add to g the highest active group of gm.
     * !ascendToGroup && addMembers: same as !ascendToGroup && !addMembers.
     * !ascendToGroup && !addMembers: add to g only gm itself.
     */
	@SuppressWarnings("unchecked")
    protected boolean performAdding(GroupMember<StandardGroup> gm, boolean ascendToGroup, boolean addMembers) {
	    
		if(itemToBeEdited==null) {
			adding = false;
			return false;
		}
		
	    StandardGroup g = (StandardGroup)Groups.getHighestActiveGroup((GroupMember)itemToBeEdited);
	    if(g==null) return false;
	    
	    OrderedGroup ordg = null;
	    if(g instanceof OrderedGroup) ordg = (OrderedGroup)g;

	    GroupMember newMember = null;
	    StandardGroup<?> old = null;
        Set<?> oldSet = null;
		StandardGroup gg = gm.getGroup(); 
	    if(ascendToGroup) {	    	
		    StandardGroup<GroupMember> hg = (StandardGroup<GroupMember>)Groups.getHighestActiveGroup(gm);
	    	StandardGroup<GroupMember> hg1 = (StandardGroup<GroupMember>)Groups.getHighestActiveGroupBut(1, gm);
		    if(addMembers && hg!=null) {
	    		if(hg1==null) {
	    			newMember = gm;
	    			old = hg;
	    		} else {
		    		newMember = hg1;
			    	old = hg;
	    		}
		    } else {
		    	if(hg==null) {
		    		newMember = gm;
		    		old = gg;
		    	} else if(!(hg instanceof GroupMember)) {
		    		newMember = hg1;
		    		old = hg1.getGroup();
		    	} else {
		    		newMember = hg;
		    		old = hg.getGroup();
		    	}
		    }
	    } else {
	        newMember = gm;
	        old = gg;
	    }	
        if(old!=null) {
            oldSet = old.memberSet();
        }
        
        addMembers = addMembers && ascendToGroup && old!=null;

        boolean success = false;
        try {
	        Iterator<?> i = null; // if addMembers, the iterator over the members of the old group
            List<GroupMember> initialSegment = null;
            if(addMembers) { 
                i = oldSet.iterator();
                if(old instanceof OrderedGroup) {	                
                	initialSegment = new LinkedList<GroupMember>();
                    GroupMember current = null;
			        assert current!=newMember;
	                while(i.hasNext() && current!=newMember) {
	                    current = (GroupMember)i.next();
	                    if(current!=newMember) {
	                        initialSegment.add(current);
	                    }
	                }
                }					                
            }

            int index = 0; // if g instanceof OrderedGroup, points at the beginning of the insertion
            if(g instanceof OrderedGroup) {
	            index = ordg.indexOf((GroupMember)itemToBeEdited) + 1;
            }
            
	        int n = addMembers? oldSet.size(): 1;
            boolean beenThere = false;
	        for(int j = 0; j<n; j++) {
	            if(!(newMember instanceof Group && g instanceof GroupMember &&
	            		((Group)newMember).indirectlyContains((GroupMember)g))) { 
	            	boolean added = false;
	            	try {
		            	if(g instanceof OrderedGroup) {
		            		added = ordg.add(index+j, newMember);
				        } else {
				        	added = g.add(newMember);
				        }
			            newMember.setInGroup(true);
				        if(added && old!=null) {
				        	old.remove(newMember);
				        }
	                	success = true;
	            	} catch(IllegalArgumentException iae) {
	            		handler.handle(iae, this, "Illegal Argument");
	            		break; // avoid multiple error messages
	            	}
		        }
	            
	            if(addMembers) { // get next new member
	                if(old instanceof OrderedGroup && !i.hasNext() 
	                        && !beenThere && !initialSegment.isEmpty()) {
	                    i = initialSegment.iterator();
	                    beenThere = true;
	                }
	                if(i.hasNext()) {
	                    newMember = (GroupMember)i.next();
	                } 
	            }
            }
        } catch(IllegalArgumentException iae) {
            handler.handle(iae, Canvas.this, "Illegal Argument");
        }
        
	    return success;
	}
	
	private void prepareForLassoing(MouseEvent me) {
		
		int x = (int)me.getX();
		int y = (int)me.getY();
		lasso = new Lasso(x, y, new ItemIndex(this), me.isControlDown());
		
		layeredPane.add(lasso, JLayeredPane.DRAG_LAYER);
	}
    
    private void prepareForDragging(MouseEvent me) {
		EditEvent ee = new EditEvent(this, false);
		fireEditing(ee);
		
        dragging = true;
        mouseX = me.getX();
        mouseY = me.getY();	
        prepareForMovingSelection();
        
        if(me.getComponent() instanceof Item) {
        	Item item = (Item)me.getComponent();
        	/*
        	 * If the item is an CNode but the selectedSet does not contain all the other Contour nodes,
        	 * there will be a constant recomputing of the Contour's center, which will interfere with the 
        	 * dragging if that Contour (and hence its center) is made the reference point for the 
        	 * snap-to-shape-center function in mouseDragged().
        	 */
        	if(item instanceof CNode && selectedSet.containsAll(((CNode)item).getGroup().getMembers())) {
        		draggedContour = ((CNode)item).getContour();
        	} else {
        		draggedContour = null;
        	}
        }
    }
    
    public void prepareForMovingSelection() {
		prepareForMovingSelection(selection);
	}

    public void prepareForMovingSelection(AffineTransform t) {
        selectionTransform.setTransform(t);
        prepareForMovingSelection();
    }
    
    /**
	 * Prepares the Canvas for moving the specified 'selection'. This list need not be the Canvas' actual selection.
	 */
	public void prepareForMovingSelection(List<Item> selection1) {
        AffineTransform t = selectionTransform;
        boolean mereTranslation = t==null || MathTools.isMereTranslation(t);
        
        selectedSet.clear();
        internal.clear(); 
        external.clear(); 
        thirds.clear();
        boundary.clear();
        immune.clear();	        

        /*
         * Compose selectedSet, internal, external, and thirds.
         */
        for(Item item : selection1) {
            if(item instanceof IndependentItem) {
                selectedSet.add(item);
            }
        }
        divideDIs(selectedSet, internal, external, thirds);
        selectedSet.removeAll(thirds);
        
        /*
         * Compute the highest (numerically least) priority of the external States.
         */
        float pmin = external.isEmpty()? Float.POSITIVE_INFINITY: 
            (Collections.min(external, DependentItem.priorityComparator)).getPriority();
        
        /*
         * Compose boundary, toRelocate, and immune.
         */
        for(Set<Item> set : selectSet) { // selectSet = {selectedSet, internal, thirds}
	        for(Item item : set) {
	            toRelocate.add(item);
	            if(item instanceof CNode) {
	            	Contour cont = ((CNode)item).getContour();
	            	Axis[] axes = new Axis[] {cont.getXAxis(), cont.getYAxis()};
	            	for(Axis a: axes) { 
	            		if(a!=null) {
	            			layeredPane.add(a, JLayeredPane.DRAG_LAYER);
	            			layeredPane.setComponentZOrder(a, 0);
	            		}
	            	}
	            }
	            layeredPane.add(item, JLayeredPane.DRAG_LAYER);
	            if(mereTranslation) {
		            Item gov = item.getGovernor();
		            if(gov!=null && gov instanceof Connector && !internal.contains(gov)) {
		                boundary.add(item);
		            }
		            boolean added = false;
		            for(Iterator<DependentItem> j = item.getDependentItems().iterator(); !added && j.hasNext();) {
		                DependentItem s = j.next();
		                if(external.contains(s)) {
		                    boundary.add(item);
		                    added = true;
		                }
		            }
		            if(item instanceof DependentItem && ((DependentItem)item).getPriority()<pmin) {
		                immune.add((DependentItem)item);
		            }
	            } else if(item instanceof IndependentItem) {
	                boundary.add(item);
	            }
	        }
        }        
        //System.err.println("selected: "+selectedSet);
        //System.err.println("boundary: "+boundary);
        //System.err.println("internal: "+internal);
    }

    /**
     * Preselects the specified Item and all the other Items in its group. 
     */
	public void preselect(final Item it) {
        Set<Item> s = new HashSet<Item>();
        s.add(it);
	    if(it instanceof GroupMember) {
	        GroupMember<?> gm = (GroupMember<?>)it;
	        Group<?> g = Groups.getHighestActiveGroup(gm);
	        if(g!=null) {
	        	getAllMembers1(g, s);
	        }	        
	    	preselect1(s, gm);
	    } else {
	    	preselect1(s, null);
	    }
	}
    
    /**
	 * Preselects the specified Items 'on account of' their membership in the same group as the specified GroupMember.
	 * If that object is null, the Items are preselected 'on their own account'.
	 * Also see isPreselected().
	 */
	public void preselect1(final Set<Item> items, GroupMember<?> gm) { 
	    if(gm==null) {
	    	for(Item item: items) {
	    		preselection.add(item);
	    	}
	    } 
	    else {
	    	preselection.add((Item)gm);
	    	for(Item item: items) {
			    Set<GroupMember<?>> s = marked.get(item);
			    if(s==null) {
			        s = new HashSet<GroupMember<?>>();
			        marked.put(item, s);
			    }
			    s.add(gm);
	    	}
	    }
	    new Thread() {
    		@Override
            public void run() {
				if(!items.isEmpty()) {
    				Iterator<Item> iter = items.iterator();
    				/*
    				 * We take the first element we find as indicating the level of highlighting for
    				 * the whole set. 
    				 */
    				Item item0 = iter.next();
	    			MarkBorder[] borders0 = (item0 instanceof ENode)? markBorders1a: markBorders0a;
	    			MarkBorder border0 = (MarkBorder)item0.getBorder();
	    			int k = matchingBorderIndex(border0, borders0);
	    			int n = Math.max(markBorders0a.length, markBorders1a.length);
	    			for(int i = Math.max(k, n/5); i<n/2; i++) {
	        			for(Item item: items) {
			    			Border[] borders = (item instanceof ENode)? markBorders1a: markBorders0a;
			    			if(isPreselected(item)) {
			    				item.setBorder(borders[i]);
			        			marked1.put(item, i);
		        			}
	        			}
	    				try{
	    					sleep(FADE_SLEEP);
	    				}
	    				catch(InterruptedException ie) {}
					}
    			}
    		}
    	}.start();
	}
    
    void printComponents() { // for debugging
		Component[] cs = getComponents();
		for(Component c : cs) {
			System.err.println(c.toString() + c.getBounds() + " " + ((Item)c).getBounds2D());
		}
		System.err.println(cs.length);
	}
    
    /*
     * Adds the specified Item back to the canvas, with new XYConstraints. 
     * We only have to deal with XYConstraints in order to guarantee sensible behavior in the case of canvas resizings,
     * so it makes little sense to call this method during dragging, etc.
     */
    public void relocate(Item item) {
    	Rectangle2D r = item.getBounds2D(); 
        add(item, (float)r.getX(), (float)getHeight() - (float)(r.getY() + r.getHeight()));        
    }
    
    /**
     * Relocates all Items that are marked as to-be-relocated.
     */
    public void relocateItems() {
    	if(relocating) return;
    	synchronized(toRelocate) {
    		relocating = true;
	    	List<ItemInfo> items = new ArrayList<ItemInfo>();	    	
	    	Component[] comp = getComponents();
	    	for(Component c: comp) {
	    		if(c instanceof Item) {
	    			Item item = (Item)c;
	    			if(!toRelocate.contains(item)) {
	    				items.add(new ItemInfo(item, false));
	    			}
	    		}
	    	}
	    	for(Item item: toRelocate) {
	    		items.add(new ItemInfo(item, true));
	    	}
	    	Collections.sort(items);
	    	int i = 0;
    		for(ItemInfo ii: items) {
    			if(ii.add) {
	    			relocate(ii.item); 
	    			if(ii.item instanceof Contour) {
	    				Contour cont = (Contour)ii.item;
	    				Axis[] axes = new Axis[] {cont.getXAxis(), cont.getYAxis()};
	    				for(Axis a: axes) {
	    					if(a!=null) relocate(a);
	    				}
	    			}
	    		}
	    		setComponentZOrder(ii.item, i);
	    		i++;
	    	}
	    	toRelocate.clear();
	    	relocating = false;
    	}
    	repaint();
    }
    
    @Override
    public void removeAll() {
    	adding = false;
	    clearSelection();
	    dragging = false;
	    toRelocate.clear();
	    super.removeAll();	    
	}

    public void removeEditListener(EditListener el) {
    	editListeners.remove(el);
    }
    @SuppressWarnings("unchecked")
    public synchronized void removeItem(Item item) {
		Iterator j = ((Set)((HashSet)item.getDependentItems()).clone()).iterator();
		for(;j.hasNext();) {
			DependentItem s = (DependentItem)j.next();	        				
			s.removeInvolute(item);
		}
        if(item instanceof GroupMember) {
            GroupMember gm = (GroupMember)item;
            
            Group g = gm.getGroup();
            if(g!=null) {
                g.remove(gm);
            }
        }
		Item gov = item.getGovernor();
		if(gov!=null) {
		    if(gov instanceof ENode) {
			    ((ENode)gov).removeDependentItem((Ornament)item);
			}
		    else if(gov instanceof Connector) {
				removeItem(gov);
				IndependentItem[] inv = ((DependentItem)gov).getInvolutes();
				for (IndependentItem element : inv) {
					element.removeDependentItem((DependentItem)gov);
				}
			}
		    else if(gov instanceof Ornament) {
		        gov.removeOrnament((Ornament)item);
		    }
		}
		toRelocate.remove(item);
		selection.remove(item);
		preselection.remove(item);
		marked.remove(item);
		marked1.remove(item);
		remove(item); 
	}
    
    /**
     * Removes all selected Items.
     */
    public void removeSelection() {
	    dragging = false;
	    if(!selection.isEmpty()) {
			HashSet<Item> toRemove = new HashSet<Item>();
	    	Iterator<Item> i = selection.iterator();
			for(;i.hasNext();) {
				toRemove.add(i.next());	
    			i.remove();
			}
			for(Item it: toRemove) {
				removeItem(it);
			}
			repaint();
			selectedEntityCount = 0; // selection is empty now
		}
        setItemToBeEdited(null);        
	    fireSelectionChanged();
    }
    
    /* (non-Javadoc)
     * @see jp.pasi.Editable#requiresResetButton(java.lang.Object)
     */
    public boolean requiresResetButton(EditorTopic infoCode) {
        return false;
    }

    private void resetTransform() {
        scaling = 1;
        rotation = 0;
        if(scaleInfo!=null) scaleInfo.notify(this);
        if(rotateInfo!=null) rotateInfo.notify(this);
    }

    public void select(Item it, boolean withGroup) {
        select(it, -1, withGroup);
    }

    /**
     * Selects the specified Item and all the other Items in its group, if not yet selected. 
     * The specified Item is placed at the specified position in the selection list and prepared for editing.
     * Fires a selectionChanged event.
     */
    public void select(Item it, int pos, boolean withGroup) {
	    select1(it, pos);
	    if(withGroup && it instanceof GroupMember) {
	        GroupMember<?> gm = (GroupMember<?>)it;
	        selectGroup(gm);
	    }
	    fireSelectionChanged();
    }
    
    /**
     * Selects the specified Item without firing a selectionChanged event. A negative position will result in the 
     * item's being put at the end of the selection list.
     * Honors the constraint that only ENodes should be able to occur more than once in the selection.
     */
    protected void select1(final Item it, final int pos) {
	    preselection.remove(it);
        for(EditorPane pane: editorPanes) {
            pane.waitTillUnblocked(); 
        }
        if(!(it instanceof ENode)) {
        	selection.remove(it);
        }
	    if(pos<0) {	    	
            selection.add(it);
        } else {
            selection.add(pos, it);
        }
        if(it instanceof ENode) {
            blinker.requestRestart();
            selectedEntityCount++;
        }
        it.setSelected(true);
        relocate(it); // necessary because of possible size change
        markAsSelected(it);
    }
    
    public void selectGroup(GroupMember<?> gm) {
        Group<?> g = Groups.getHighestActiveGroup(gm);
        if(g!=null) {
        	Set<Item> s = getAllMembers(g);
        	for(Item item: s) {
	            if(!selection.contains(item)) select1((Item)item, -1);
        	}
        }
    }
    
    public void setAdding(boolean b) {
        adding = b;
    }
    
    public void setAddMembers(boolean b) {
        addMembers = b;
    }
    
    public void setAngularIncrement(float a) {
        rotateInfo.setIncrement((float)Math.pow(10, a));
    }

    public void setContourCenterGrid(boolean b) {
        grid.snapToContourCenters = b;
    }
    
    public void setContourNodeGrid(boolean b) {
        grid.snapToCNodes = b;
    }

    public void setCurrentPriority(int n) {
        this.currentDIPriority = n;
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public void setFlippingArrowheads(boolean b) {
        int m = TransformModifier.flippingArrowheads; 
        transformModifier = (b? transformModifier | m: transformModifier ^ m);
    }

    public void setGrid(Grid grid) {        
        this.grid = grid;
    }
    
    public void setHeight(int h) {
        setSize(new Dimension(getWidth(), h));
    }
    
    public void setHorizontalDisplacement(int displacement) {
        hDisplacement = displacement;
    }
    
    public void setHorizontalGap(int g) {
        grid.hGap = g;
    }

	public void setHorizontalShift(int g) {
        grid.hShift = g;
    }

	public void setItemToBeEdited(Item item) {
    	Item old = itemToBeEdited;
    	if(old instanceof CNode) {
    		Contour cont = ((CNode)old).getContour(); 
    		if(cont!=null) cont.setAxesVisible(false);
    	}
    	if(item instanceof CNode) {
    		((CNode)item).getContour().setAxesVisible(true);
    	}
    	itemToBeEdited = item;
        if(item==null) {
            updateEditorPanes(this);
        } else {
            blinker.interrupt();
            updateEditorPanes(item);
            fireStartNewPeriod();
        }
    }

	public void setRotatingLabels(boolean b) {
        int m = TransformModifier.rotatingLabels; 
        transformModifier = (b? transformModifier | m: transformModifier ^ m);
    }

	/**
     * Invoked by an EditorPane.
     */
    public void setRotation(float val) {
        if(selection.isEmpty()) return;

    	float oldRotation = rotation;
        rotation = (float)Math.toRadians(val);

        Point2D c = null;
        Item lastSelected = selection.get(selection.size()-1);
        if(lastSelected instanceof Point) {
            c = lastSelected.getCenter();
        } else if(itemToBeEdited!=null) {
            c = itemToBeEdited.getCenter();
        } else {
            c = new Point2D.Float(getWidth()/2, getHeight()/2);
        }        
        selectionTransform.setToRotation(-(rotation - oldRotation), c.getX(), c.getY());
        
        prepareForMovingSelection();
        moveSelection();
        relocateItems();
        
        selectionTransform.setToIdentity();
    }
	
    /**
     * Invoked by an EditorPane.
     */
    public void setScaling(float val) {
        float oldScaling = scaling;
        scaling = val/100;
        if(scaling==0) scaling = MIN_SCALING;
        double ds = scaling/(oldScaling==0? 1: oldScaling);
        
        Point2D c = null;
        Item lastSelected = selection.get(selection.size()-1);
        if(lastSelected instanceof Point) {
            c = lastSelected.getCenter();
        } else if(itemToBeEdited!=null) {
            c = itemToBeEdited.getCenter();
        } else {
            c = new Point2D.Float(getWidth()/2, getHeight()/2);
        }        
        double dx = c.getX() - ds*c.getX(); 
        double dy = c.getY() - ds*c.getY();        
        
        selectionTransform.setToTranslation(dx, dy);
        selectionTransform.scale(ds, ds);
        
        prepareForMovingSelection();
        moveSelection();
        relocateItems();
        selectionTransform.setToIdentity();
    }

    /**
     * Invoked by an EditorPane.
     */
    public void setScalingArrowheads(boolean b) {
        int m = TransformModifier.scalingArrowheads; 
        transformModifier = (b? transformModifier | m: transformModifier ^ m);
    }

    /**
     * Invoked by an EditorPane.
     */
    public void setScalingIncrement(float a) {
        scaleInfo.setIncrement((float)Math.pow(10, a));
    }

    /**
     * Invoked by an EditorPane.
     */
    public void setScalingLines(boolean b) {
        int m = TransformModifier.scalingLines; 
        transformModifier = (b? transformModifier | m: transformModifier ^ m);
    }
    
    /**
     * Invoked by an EditorPane.
     */
    public void setScalingNodes(boolean b) {
        int m = TransformModifier.scalingNodes; 
        transformModifier = (b? transformModifier | m: transformModifier ^ m);
   }

	@Override
    public void setSize(Dimension d) {
        Dimension old = getSize();
        int dy = d.height - old.height;
        
    	XYLayout l = (XYLayout)getLayout();
    	l.setWidth(d.width);
    	l.setHeight(d.height);
    	layeredPane.setPreferredSize(d);
    	layeredPane.setSize(d);
    	setPreferredSize(d);
    	super.setSize(d);
    	
    	Component[] comp = getComponents();
    	Set<DependentItem> dIs = new HashSet<DependentItem>();
    	for(Component c: comp) {
    	    if(c instanceof Item) {    	    	
    	        if(c instanceof IndependentItem) {
    	        	((Item)c).moveBy(0, dy);
    	        } else if(c instanceof DependentItem) {
    	        	dIs.add((DependentItem)c);
    	        }
    	    }
    	}    	
    	for(DependentItem di: dIs) {
        	if(di instanceof Connector) {
        		((Connector)di).invalidateShape(false);
        	} else {
                di.invalidate();
            }
        	di.defineBounds();
    	}
    	revalidate();
    }

	public void setTransformModifier(int m) {
    	this.transformModifier = m;
    }

	public void setUnitSpinner(FloatSpinner unitSp) {
        this.unitSpinner = unitSp;
    }

	public void setVerticalDisplacement(int displacement) {
        vDisplacement = -displacement;
    }

	public void setVerticalGap(int g) {
        grid.vGap = g;
    }

	public void setVerticalShift(int g) {
        grid.vShift = g;
    }

	public void setWidth(int w) {
        setSize(new Dimension(w, getHeight()));
    }

	/**
     * Sets the appropriate z-order for the specified Item, both modifying the Item's internally represented z-order
     * and the z-order it has as a component of this Canvas. If the former already has a value greater than 0, it is left
     * unchanged; otherwise, it is set to a new value according to the class of the Item. In any event, its component z-order
     * for this Canvas is set to the closest available value.
     * 
     * The method should be called soon after a new Item has been added to the Canvas, but not before, since it assumes that 
     * the specified Item has already been added.
     */
    public void setZOrder(Item item) {
    	float value = item.getZValue();
    	if(value==-1) {
    		if(item instanceof Contour) {
    			value = DEFAULT_CONTOUR_Z ;
    		} else if(item instanceof ENode) {
    			value = DEFAULT_ENODE_Z;
    		} else if(item instanceof Label) {
    			value = DEFAULT_LABEL_Z;
    		} else if(item instanceof DependentItem) {
    			value = DEFAULT_DEPENDENT_ITEM_Z;
    		} else {
    			value = 0;
    		}
    		item.setZValue(value);
    	}
    	int c = getComponentCount();
    	if(value >= c) {
    		value = c - 1;
    	}
    	setComponentZOrder(item, (int)value);
    }

	protected void updateEditorPanes(Editable item) {
        for (EditorPane pane : editorPanes) {
            pane.showPanel(item);
        }
    }

}
