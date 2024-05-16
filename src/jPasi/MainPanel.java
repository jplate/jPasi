package jPasi;

import static jPasi.MainPanel.DependentItem.ADJUNCTION;
import static jPasi.MainPanel.DependentItem.CONTAINMENT;
import static jPasi.MainPanel.DependentItem.ENTAILMENT;
import static jPasi.MainPanel.DependentItem.IDENTITY;
import static jPasi.MainPanel.DependentItem.INCLUSION;
import static jPasi.MainPanel.DependentItem.LABEL;
import static jPasi.MainPanel.DependentItem.NEGATION;
import static jPasi.MainPanel.DependentItem.OBTAINMENT;
import static jPasi.MainPanel.DependentItem.ORDER;
import static jPasi.MainPanel.DependentItem.POINTER;
import static jPasi.MainPanel.DependentItem.PREDICATION;
import static jPasi.MainPanel.DependentItem.RESTRICTION;
import static jPasi.MainPanel.DependentItem.SUBSUMPTION;
import static jPasi.MainPanel.DependentItem.TRANSITION;
import static jPasi.MainPanel.DependentItem.UNIVERSALQUANTIFICATION;
import static jPasi.MainPanel.ExportDestination.CLIPBOARD;
import static jPasi.MainPanel.ExportDestination.PANE;
import static jPasi.MainPanel.Transform.ROTATION;
import static jPasi.MainPanel.Transform.SCALING;
import static jPasi.MainPanel.Transform.TRANSLATION;
import static jPasi.edit.EditorTopic.GROUPS;
import static jPasi.edit.EditorTopic.ITEM;
import static jPasi.edit.EditorTopic.TRANSFORM;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerModel;
import javax.swing.SwingConstants;

import jPasi.codec.Codec;
import jPasi.codec.Codec1;
import jPasi.codec.MetaCodec;
import jPasi.codec.ParseException;
import jPasi.edit.DefaultEditorPaneListener;
import jPasi.edit.EditEvent;
import jPasi.edit.EditListener;
import jPasi.edit.EditorEntry;
import jPasi.edit.EditorPane;
import jPasi.item.Adjunction;
import jPasi.item.CNode;
import jPasi.item.CompoundArrow;
import jPasi.item.Connector;
import jPasi.item.Containment;
import jPasi.item.Contour;
import jPasi.item.ENode;
import jPasi.item.Entailment;
import jPasi.item.Identity;
import jPasi.item.Inclusion;
import jPasi.item.IndependentItem;
import jPasi.item.Item;
import jPasi.item.Label;
import jPasi.item.Negation;
import jPasi.item.Obtainment;
import jPasi.item.Order;
import jPasi.item.Ornament;
import jPasi.item.Point;
import jPasi.item.Pointer;
import jPasi.item.Predication;
import jPasi.item.Restriction;
import jPasi.item.Subsumption;
import jPasi.item.Transition;
import jPasi.item.UniversalQuantification;
import jPasi.item.group.GroupManager;
import jPasi.item.group.GroupMember;
import jPasi.item.group.GroupUIDelegator;
import util.swing.FloatSpinner;
import util.swing.SpinnerNumberModel.IncrementOverride;
import netscape.javascript.JSObject;

/**
 * @author Jan Plate
 *
 */
public class MainPanel extends JPanel implements Handler, Canvas.SelectionListener {	

	private class CombiButtonAction extends AbstractAction {
        private static final long serialVersionUID = -3789705302245305481L;
        
        private boolean combine;
    	
        public void actionPerformed(ActionEvent ae) {
    		if(combine) {
                EditEvent ee = new EditEvent(MainPanel.this, true);
        	    CompoundArrow ca = null;
        	    if(canvas.selection.size()>0) {
            		undoManager.editing(ee);

            		final List<Connector> rl = new LinkedList<Connector>();
        			ENode[] e = null; 
        			for (Object element : canvas.selection) {
        			    Item item = (Item)element;
        			    Item gov = item.getGovernor();    				    
        			    if(gov!=null && gov instanceof Connector) {
        			        Connector rel = (Connector)gov;
        			        if(rl.size()==0) {
        			            e = (ENode[])rel.getInvolutes();
        			        }
        			        ENode[] e_ = (ENode[])rel.getInvolutes();
        			        if(!rl.contains(rel) && (e_[0]==e[0] && e_[1]==e[1] || e_[0]==e[1] && e_[1]==e[0])) {
        			            rl.add(rel);
        			        }
        			    }
        			}
        			
    				if(rl.size()>1) {
    				    canvas.clearSelection();
        				canvas.selectedEntityCount = 0;
	    				Canvas.CompoundArrowDissolve dissolve = canvas.new CompoundArrowDissolve(rl, false, false);
    				    for (Connector rel : rl) { 
    				        canvas.removeItem(rel.getItem());
				            rel.getItem().getDependentItems().clear();
	    				}
        				ca = new CompoundArrow(e[0], e[1], 
        				        rl.toArray(new Connector[]{}), true, dissolve);
    				}
        	    }
				else {
				    handler.handle(new Exception("Select at least two ascriptions that involve the same two entity nodes."),
				            canvas, "Illegal Argument");
				}            	    
		        if(ca!=null) {
		            canvas.addDependentItem(ca);
				    ca.setPriority((ca.getElements()[0]).getPriority());
   				    canvas.clearSelection(ca.getItem());       				    
		        }
            } else {
            	copyAction.actionPerformed(ae);
            }
		}
        
    	public void setCombine(boolean b) {
    		combine = b;
    		if(b) {
    			putValue(NAME, "Combine Connectors");
    		} else {
    			putValue(NAME, copyAction.getValue(NAME));
    		}
    	}
    }
	
    private class CopyAction extends AbstractAction {
        private static final long serialVersionUID = -8157220839196772033L;

        private static final int COPY_CONNECTOR = 1;
        private static final int COPY_LABEL = 2;
        private static final int COPY_ORNAMENT = 3;
        private static final int COPY_SELECTION = 0;
        
        int state = 0;        
        
        public void actionPerformed(ActionEvent ae) {
            EditEvent ee = new EditEvent(MainPanel.this, true);
            
        	switch(state) {
            	case COPY_SELECTION: {            		
            		undoManager.editing(ee);
			        canvas.copySelection();
            	    break;
            	}
            	case COPY_CONNECTOR: 
            	case COPY_LABEL: 
            	case COPY_ORNAMENT: {    			    
            		Iterator<?> i = canvas.selection.iterator();    			    
    			    if(i.hasNext()) {                	
    			    	undoManager.editing(ee);
                		
    			    	Item item = (Item)i.next();
	    			    Item gov = item.getGovernor();
	    			    if(item instanceof Ornament || gov!=null && gov instanceof Connector) {
		    			    jPasi.item.DependentItem clone = null;
		    			    if(item instanceof Ornament) { 
		    			        /* 
		    			         * try to add copies to rest of selection
		    			         */
		    			        for(; i.hasNext();) {
		    			            Object o = i.next();
		    			            if(o instanceof IndependentItem) {
		    			                IndependentItem<?> i2 = (IndependentItem<?>)o;
		    			                if(i2 instanceof ENode || item instanceof Ornament) {
			    			                try {
	                                            clone = (jPasi.item.DependentItem)item.clone();
                                            } catch (CloneNotSupportedException cnse) {
                                            	handler.handle(cnse, MainPanel.this, "Copying not supported");
                                            	undoManager.editFailed(ee);
                                            }
				    			            
			    			                gov.removeDependentItem(clone);			    			                
			    			                clone.setGovernor(i2);
				    			            i2.addDependentItem(clone);
								            
				    			            canvas.addDependentItem(clone);
		    			                }
		    			            }
		    			        }	    			        
		    			    } else if(gov instanceof Connector) { 
		    			        /* 
		    			         * try to add copies to pairs of rest of selection
		    			         */
		    			        Object[] o = new Object[2];
		    			        for(; i.hasNext();) {
		    			            o[0] = i.next();	    			            
		    			            if(o[0] instanceof ENode) {
		    			                o[1] = null;
		    			                for(; o[1]==null && i.hasNext();) {
		    			                    Object o2 = i.next();
		    			                    if(o2 instanceof ENode) o[1] = o2;
		    			                }
		    			                if(o[1]!=null) {
			    			                Map<Object, Object> cloneMap = new HashMap<Object, Object>();
			    			                try {
		                                        clone = (Connector)gov.clone(cloneMap);
	                                        } catch (CloneNotSupportedException cnse) {
	                                        	handler.handle(cnse, MainPanel.this, "Copying not supported");
	                                        	undoManager.editFailed(ee);
	                                        }
				    			            ENode[] inv = (ENode[])clone.getInvolutes();			    			            
				    			            for(int j = 0; j<2; j++) {
				    			                inv[j].removeDependentItem(clone);
				    			                ENode e = (ENode)o[j]; 
				    			                ((Connector)clone).setInvolute(j, e);
				    			            }				    			            
				    			            canvas.addDependentItems(cloneMap, true);
		    			                }
		    			            }
		    			        }
		    			    } 
	       				    canvas.clearSelection(clone.getItem()); 
	    			    }
    			    }
            	    break;
            	}
            	default: {}
        	}
        }

        public void setState(int s) {
            state = s;
            switch(s) {
            	case COPY_SELECTION: {
            	    putValue(NAME, "Copy Selection");
            	    break;
            	}
            	case COPY_CONNECTOR: {
            		putValue(NAME, "Copy Connector");
            	    break;
            	}
            	case COPY_LABEL: {
            		putValue(NAME, "Copy Label");
            	    break;
            	}
            	case COPY_ORNAMENT: {
            		putValue(NAME, "Copy Ornament");
            	    break;
            	}
            	default: {}
            }
        }
	}

    private abstract class DBI1 extends DBoxItem {
        DBI1(DependentItem di) {
        	super(di);
        }
    	DBI1(String s, String fn) {
            super(s, fn);
        }
        /*
         * creates a unary DI for every member of the List 
         */
        @Override
        Object[] multiCreate(java.util.List<Item> l) {
            ENode[] inv = canvas.extractEntities(l);
            Object[] result = new Object[inv.length];
            for(int i = 0; i<inv.length; i++) {
                result[i] = create(new ENode[]{inv[i]});
            }
            return result;
        }
        Object[] multiCreate1(java.util.List<Item> l) {
        	return multiCreate(l);
        }
    }
    
    private abstract class DBI2 extends DBoxItem {
        DBI2(DependentItem di) {
        	super(di);
        }
        DBI2(String s, String fn) {
            super(s, fn);
        }
        /*
         * connects every nth member of the List with every (n+1)th 
         */
        @Override
        Object[] multiCreate(java.util.List<Item> l) {
            ENode[] inv = canvas.extractEntities(l);
            if(inv.length<2) return null;
            Object[] result = new Object[inv.length-1];            
            for(int i = 0; i<inv.length-1; i++) {
            	ENode[] args = new ENode[]{inv[i], inv[i+1]};
            	result[i] = create(args);
            }
            return result;
        }
        
        Object[] multiCreate1(java.util.List<Item> l) {
            ENode[] inv = canvas.extractEntities(l);
            if(inv.length<2) return null;
            Object[] result = new Object[inv.length-1];            
            for(int i = 0; i<inv.length-1; i++) {
                result[i] = create(new ENode[]{inv[0], inv[i+1]});
            }
            return result;
        }
        /*
        // 'nesting' variant:
        Object[] multiCreate2(java.util.List<Item> l) {
            ENode[] inv = canvas.extractEntities(l);
            if(inv.length<2) return null;
            Object[] result = new Object[inv.length-1];
            ENode prd = inv[0]; // should be a predicate
            for(int i = 1; i<inv.length; i++) {
            	pasi.item.DependentItem s = 
            		(pasi.item.DependentItem)create(new ENode[]{prd, inv[i]});
                result[i-1] = s;
                prd = (ENode)s.getItem();
            }
            return result;
        }
        */
    }
    private static class DBoxCellRenderer extends DefaultListCellRenderer {
    	
        private static final long serialVersionUID = -9004986565907609912L;
    	
        public DBoxCellRenderer() {
        	setOpaque(true);
        }
        
        public Component getListCellRendererComponent(JList list, Object value, int index, 
        		boolean isSelected, boolean cellHasFocus) {
        	
        	Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        	DBoxItem sbi = (DBoxItem)value;
        	
        	((JLabel)c).setText("   "+sbi.name);
        	((JLabel)c).setIcon(sbi.icon);
        	
        	return c;
        } 
    }
    private abstract class DBoxItem {
        String name;
        Icon icon;
        int minNOfNodes;
        
        DBoxItem(DependentItem di) {
        	if(di!=null) {
	            this.name = di.name;
	            this.icon = di.icon;
	            this.minNOfNodes = di.minNOfNodes;
        	} else {
        		this.name = "  ";
        		this.minNOfNodes = Integer.MAX_VALUE;
        	}
        }
        DBoxItem(String name, String filename) {
            this.name = name;
        }
        abstract Object create(ENode[] inv);
        void multiAdd(List<Item> l, int variant) {
        	Object[] s = variant==0? multiCreate(l): multiCreate1(l);
            if(s!=null) {
			    for (Object element : s) {
			        if(element!=null) canvas.addDependentItem((jPasi.item.DependentItem)element);
			    }
			}
        }
        
        abstract Object[] multiCreate(List<Item> l);
        
        /**
         * A variant of multiCreate(), invoked if Shift is pressed together with the 'Create' button.
         */
        abstract Object[] multiCreate1(List<Item> l);
        
		boolean shouldCreateExtraEntity() {
		    return false;
		}

		@Override
        public String toString() {
            return name;
        }
    }
     
    protected static enum DependentItem {
    	LABEL("Label", "icon/lbl.png", 1),
    	POINTER("Pointer", "icon/ptr.png", 1),
    	ADJUNCTION("Adjunction", "icon/adj.png", 2),
    	CONTAINMENT("Containment", "icon/cnt.png", 2),
    	ENTAILMENT("Entailment", "icon/ent.png", 2),    	
    	IDENTITY("Identity", "icon/idt.png", 2),
    	INCLUSION("Inclusion", "icon/inc.png", 2),
    	NEGATION("Negation", "icon/neg.png", 2),
    	OBTAINMENT("Obtainment", "icon/exs.png", 1),
    	ORDER("Order", "icon/orp.png", 2),
    	PREDICATION("Predication", "icon/prd.png", 2),
    	RESTRICTION("Restriction", "icon/rst.png", 2),
    	SUBSUMPTION("Subsumption", "icon/ins.png", 2),
    	TRANSITION("Transition", "icon/trn.png", 2),
    	UNIVERSALQUANTIFICATION("Univ. Quantification", "icon/unv.png", 2);
    	
    	public String name;
    	public Icon icon;
    	public int minNOfNodes;
    	public Action action;
    	
    	private DependentItem(String name, String fn, int min) {
    		this.name = name;
    		this.icon = getIcon(fn);
    		this.minNOfNodes = min;
    	}
    }
    
    protected static enum ExportDestination {
    	CLIPBOARD,
    	PANE;
    }
    
    private class Spinner extends JSpinner {
        
        private static final long serialVersionUID = -8057679690437123174L;

        public Spinner(SpinnerModel sm) {
    		super(sm);
    	}    	
    	public void setValue(int val) {
    		setValue(new Integer(val));
    	}
    }

    protected static enum Transform {
    	TRANSLATION,
    	ROTATION,
    	SCALING;
    }
    
    
    public class UndoManager implements EditListener {
    	
        protected class RedoAction extends AbstractAction {
        	private static final long serialVersionUID = 8895168119452787054L;

			public RedoAction() {
                super(null, redoIcon);
                setEnabled(false);
            }

            public void actionPerformed(ActionEvent e) {
                redo();
                update();
                undoAction.update();
            }

            protected void update() {
                if (undoManager.canRedo()) {
                    setEnabled(true);
                } else {
                    setEnabled(false);
                    canvas.requestFocus(); // for the sake of symmetric behavior (see UndoAction#update()).
                }
            }
        }    

        protected class UndoAction extends AbstractAction {
    	    private static final long serialVersionUID = 4554313393663705642L;

			public UndoAction() {
                super(null, undoIcon);
                setEnabled(false);
            }

            public void actionPerformed(ActionEvent e) {
               	undoManager.undo();
                update();
                redoAction.update();
            }

            protected void update() {
                if (undoManager.canUndo()) {
                    setEnabled(true);
                } else {
                    setEnabled(false);
                    canvas.requestFocus(); // otherwise, the focus tends to fall onto the disabled undoButton, which 
                       // then means that the redo key-binding won't work.
                }
            }
        }

		private static final boolean DEBUG = false;

        ArrayList<String> history = new ArrayList<String>();
		private List<String> future = new ArrayList<String>();

    	/**
    	 * Points at the 'current' entry in the history, i.e., one ahead of the state that the next undo would restore.
    	 * If there are no entries in the history, the index is -1.
    	 */
    	public int index = -1; 

    	public int max;
    	public int grace;
    	private int j;
    	
    	protected EditEvent current;
    	
    	public UndoAction undoAction;
    	public RedoAction redoAction;

    	/**
    	 * Records whether an editing attempt is currently being performed.
    	 */
    	protected boolean editing;
    	
    	/**
    	 * Records whether an edit has been performed in the current editing period. 
    	 */
    	protected boolean editPerformed;
    	
    	/**
    	 * Records whether the current edit (which may still fail) has resulted in an additional entry to the history, 
    	 * which will have to be deleted if the edit does fail.
    	 */
    	protected boolean historyChanged;
    	
    	/**
    	 * Constructor.
    	 */
    	public UndoManager(int max, int grace) {
    		this.max = max;
    		this.grace = grace;
    		undoAction = new UndoAction();
    		redoAction = new RedoAction();
    	}
    	
    	@SuppressWarnings("unchecked")
        protected void advance() {
			if(DEBUG) System.err.println("advance: history-size: "+history.size()+" index: "+index);
			int n = history.size();
			if(index < n) {
				List<String> future = history.subList(index+1, n);				
				this.future.clear();
				this.future.addAll(future);
				if(DEBUG) System.err.println("future: "+future);
				future.clear();				
			} else {
				this.future.clear();
			}
			String code = getCode();
			history.add(code);
			index++;

			if(++j > grace) {
				trimHistory();
			}
			
			undoAction.update();
			redoAction.update();
		}
    	
    	public boolean canRedo() {
    		return index < history.size()-1;
    	}
    	
    	public boolean canUndo() {
    		return index > 0 || editPerformed;
    	}
    	
    	public void editFailed(EditEvent e) {
    		if(DEBUG) System.err.println("edit failed: "+(e==current)+" "+historyChanged+" index: "+index);
			if(e!=current) {
				throw new IllegalArgumentException("Edit attempt not recorded as current");
			}
			if(historyChanged) {
				history.remove(--index);
				history.addAll(future);
			}
			current = null;
			historyChanged = false;
			editing = false;
			
			undoAction.update();
			redoAction.update();
        }
    	
    	public void editing(EditEvent e) {
    		if(DEBUG) System.err.println("edit: "+e.getSource().getClass().getSimpleName()+
    				", major: "+e.isMajor()+", editing: "+editing+", editPerformed: "+editPerformed);
			current = e;
    		if(e.isMajor()) {
				startNewPeriod(e);
			} else {
				historyChanged = false;
				editPerformed = editing;				
			}
			editing = true;
        }

		public void init() {
    		history.clear();
    		history.add(getCode());
    		index = 0;
    	}

		public void redo() {
    		if(!canRedo()) {
    			throw new IllegalStateException("Cannot redo");
    		}
    		if(DEBUG) System.err.println("redo: "+(index+1));
    		editing = false;
    		editPerformed = false;    		
    		restoreFromCode(history.get(++index));
			current = null;
			historyChanged = false;
    	}
		
		public void startNewPeriod(EditEvent e) {
    		if(DEBUG) System.err.println("new period: "+e.getSource().getClass().getSimpleName()+
    				", editing: "+editing+", editPerformed: "+editPerformed);
			if(editing || editPerformed) {
				advance();
				historyChanged = true;
			} else {
				historyChanged = false;
			}
			editing = false;
			editPerformed = false;
        }

		private void trimHistory() {
			if(DEBUG) System.err.println("trimHistory: size: "+history.size()+" index: "+index+" neu: "+max);
			int n = history.size();
			if(n > max && index > n-max) {
				history.subList(0, n-max).clear();
				index -= n-max;
			}
			j = 0;
		}

		public void undo() {
    		if(!canUndo()) {
    			throw new IllegalStateException("Cannot undo");
    		}
    		if(editing || editPerformed) {
    			advance();
    		}
    		if(DEBUG) System.err.println("undo: "+(index-1)+", editing: "+editing+
    				", editPerformed: "+editPerformed+"\n history: "+history);
    		editing = false;
    		editPerformed = false;    		
			current = null;
			historyChanged = false;
    		restoreFromCode(history.get(--index));
    	}
    	
    }
		
	private static final long serialVersionUID = 1478507511553482715L;
    
    public static final Color CODE_BACKGROUND = Color.decode("#e1dddd");      
    public static final Font CODE_FONT = new Font("SansSerif", Font.PLAIN, 11);
    
    public static final int DEFAULT_BUTTON_PANEL_WIDTH = 180;

    private static final String INPUT = "input";    
    private static final String OUTPUT = "output";
    
    public static void main(String[] args) {
		JFrame f = new JFrame("Pasi");
    	final MainPanel panel = new MainPanel(null);
        f.add(panel);
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.setSize(800, 600);
        f.setVisible(true);
  	}
    
	private static void registerKeyBindingFor(JComponent c, Action action, int key, int mask) {
		ActionMap am = c.getActionMap();
		InputMap im = c.getInputMap();
		im.put(KeyStroke.getKeyStroke(key, mask), action);
		am.put(action, action);
    }
	
	private static ImageIcon getIcon(String path) {
    	InputStream is = MainPanel.class.getResourceAsStream(path);
    	Image img = null;
		try {
			img = ImageIO.read(is);
		} catch (IOException e) {			
			e.printStackTrace();
			img = new BufferedImage(12,12,BufferedImage.TYPE_INT_RGB);
		}
    	return new ImageIcon(img);
    }
	
	private final ImageIcon deleteIcon;
	private final ImageIcon undoIcon;
	private final ImageIcon redoIcon;
	private final ImageIcon deleteDisIcon;
	private final ImageIcon undoDisIcon;
	private final ImageIcon redoDisIcon;
    protected Action createAction0;
    protected Action createAction1;
    protected Action deleteAction;
    protected Action addENodeAction;
    protected AbstractAction addContourAction;
    protected Action inputAction;
    protected Action generateAction;
    protected Action loadAction;
    protected Action pasteAction;
    protected Action closeAreaAction;        
	
	protected CopyAction copyAction;

	protected UndoManager undoManager;
	
	private final JButton addNodeButton;
    private final Canvas canvas;
    private final JPanel canvasPanel;
	
	private CombiButtonAction combineAction = new CombiButtonAction();
	private Codec codec = new MetaCodec(new Codec1());
    
    private JButton combiButton;
    
    final JPanel contentPanel;
    private EditorPane editorPane;
    private HashMap<KeyStroke, Action> specialKeyBindings = new HashMap<KeyStroke, Action>();
    
    private EditorPane groupPane;
	private final GridBagLayout gbl = new GridBagLayout();
    private Handler handler;
	private final JTextArea inputArea;
    private final JMenuBar menuBar;

    private final JTextArea outputArea;
    private final JSplitPane splitPane;
    
    private JSplitPane splitPane1;
    private final JComboBox<DBoxItem> dBox;
    
    private final JButton dButton;
    
    private Transform transform = TRANSLATION;
    private final DBoxItem[] dBoxItems = new DBoxItem[] {
    		new DBI1(LABEL) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<1) return null;
    				String text = "$$";
    				jPasi.item.DependentItem s = new jPasi.item.Label(inv[0], text);
    				return s;
    			}
    		},            
			new DBI1(POINTER) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<1) return null;
    				jPasi.item.DependentItem s = new Pointer(inv[0]);
    				return s;
    			}
    		},
    		new DBI1(null) {
                @Override
                Object create(ENode[] inv) {
                    return null;
                }
    		},
			new DBI2(ADJUNCTION) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Adjunction(inv[0], inv[1]);
    				return s;
    			}
    		},
    		new DBI2(CONTAINMENT) {
    		    @Override
                Object create(ENode[] inv) {
					if(inv.length<2) return null;
					jPasi.item.DependentItem s = new Containment(inv[0], inv[1]);
					return s;
				}
			},
			new DBI2(ENTAILMENT) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Entailment(inv[0], inv[1]);
    				return s;
    			}
    		},
    		new DBI2(IDENTITY) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Identity(inv[0], inv[1]);
    				return s;
    			}
    		},
			new DBI2(INCLUSION) {
				@Override
                Object create(ENode[] inv) {
					if(inv.length<2) return null;
					jPasi.item.DependentItem s = new Inclusion(inv[0], inv[1]);
					return s;
				}
			},
			new DBI2(NEGATION) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Negation(inv[0], inv[1]);
    				return s;
    			}
    		},
			new DBI1(OBTAINMENT) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<1) return null;
    				jPasi.item.DependentItem s = new Obtainment(inv[0]);
    				return s;
    			}
    		},
    		new DBI2(ORDER) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Order(inv[0], inv[1]);
    				return s;
    			}
    		},
			new DBI2(PREDICATION) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Predication(inv[0], inv[1]);
    				return s;
    			}
    		},
			new DBI2(RESTRICTION) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Restriction(inv[0], inv[1]);
    				return s;
    			}
    		},
    		new DBI2(SUBSUMPTION) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new Subsumption(inv[0], inv[1]);
    				return s;
    			}
    		},    		
			new DBI2(TRANSITION) {
				@Override
                Object create(ENode[] inv) {
					if(inv.length<2) return null;
					jPasi.item.DependentItem s = new Transition(inv[0], inv[1]);
					return s;
				}
			},
			new DBI2(UNIVERSALQUANTIFICATION) {
    			@Override
                Object create(ENode[] inv) {
    				if(inv.length<2) return null;
    				jPasi.item.DependentItem s = new UniversalQuantification(inv[0], inv[1]);
    				return s;
    			}
    		}
		};
    private final FloatSpinner unitSp;
    private final EditorPane transformPane;
	private final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
	private final JCheckBox replaceBox;
	private final JPanel codePanel;
    private final IncrementOverride incrementOverride = new IncrementOverride(1f);

    private final JPanel buttonPanel;
	private volatile String shownArea;

	/**
	 * Constructor.
	 */
    MainPanel(Handler h) {
    	handler = h==null? this: h;
        
    	deleteIcon = getIcon("icon/delete16.png");
    	undoIcon = getIcon("icon/undo16.png");
    	redoIcon = getIcon("icon/redo16.png");
    	deleteDisIcon = getIcon("icon/delete_disabled16.png");
    	undoDisIcon = getIcon("icon/undo_disabled16.png");
    	redoDisIcon = getIcon("icon/redo_disabled16.png");
    	
		undoManager = new UndoManager(500, 20);
	    
        this.setLayout(gbl);
        
        defineActions();

        menuBar = new JMenuBar();
        //menuBar.add(new JMenu("File"));
        this.add(menuBar, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
        		GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(1, 2, 2, 1), 0, 0));
        
        
        contentPanel = new JPanel();
  		contentPanel.setLayout(gbl);
  		splitPane = new JSplitPane();
  		splitPane.setResizeWeight(1);
  	  	
        final JScrollPane canvasPane = new JScrollPane();

        editorPane = new EditorPane(ITEM, incrementOverride, specialKeyBindings);
        editorPane.addEditListener(undoManager);
        editorPane.setTextBackground(CODE_BACKGROUND);
        transformPane = new EditorPane(TRANSFORM, incrementOverride, specialKeyBindings);
        transformPane.addEditListener(undoManager);
        groupPane = new EditorPane(GROUPS, incrementOverride, specialKeyBindings);
        groupPane.addEditListener(undoManager);
        groupPane.addEditorPaneListener(new DefaultEditorPaneListener(tabbedPane));

        /*
         * Either define code area and add splitPane1 to splitPane, or add canvasPane to splitPane.
         */
        codePanel = new JPanel(); 
        final CardLayout cardLayout = new CardLayout();
        codePanel.setLayout(cardLayout);
        
        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(gbl);

        JScrollPane outputPane = new JScrollPane();
        
        outputArea = new JTextArea();
        outputArea.setBackground(CODE_BACKGROUND);
        outputArea.setFont(CODE_FONT);
        outputArea.setEditable(false);
        outputArea.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent me) {
        		outputArea.selectAll();
        	}
        });
        
        outputPane.setViewportView(outputArea);
        
        JButton webButton = new JButton("To Webpage");
        
        outputPanel.add(outputPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));        
        
        
        codePanel.add(outputPanel, OUTPUT);
    	
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(gbl);
        
        JScrollPane inputPane = new JScrollPane();
        
        inputArea = new JTextArea();
        inputArea.setBackground(CODE_BACKGROUND);
        inputArea.setFont(CODE_FONT);
        inputArea.setEditable(true);
        inputPane.setViewportView(inputArea);

        JPanel buttonPanel1 = new JPanel();
        replaceBox = new JCheckBox("Replace current diagram");

        JButton loadButton = new JButton(loadAction);	        

        buttonPanel1.add(replaceBox);
        buttonPanel1.add(loadButton);
        
        
        inputPanel.add(inputPane, new GridBagConstraints( 0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));        
        inputPanel.add(buttonPanel1, new GridBagConstraints( 0, 1, 1, 1, 0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        
        codePanel.add(inputPanel, INPUT);	    	        
        
        Action toggleReplace = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				replaceBox.doClick();
			}
        };
        registerKeyBindingFor(inputArea, toggleReplace, KeyEvent.VK_R, Event.CTRL_MASK);
        registerKeyBindingFor(inputArea, closeAreaAction, KeyEvent.VK_ESCAPE, 0);
        registerKeyBindingFor(inputArea, loadAction, KeyEvent.VK_L, Event.CTRL_MASK);
        //registerKeyBindingFor(inputArea, pasteAction, KeyEvent.VK_V, Event.CTRL_MASK);
        registerKeyBindingFor(outputArea, closeAreaAction, KeyEvent.VK_ESCAPE, 0);
        
        splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
  		splitPane1.setResizeWeight(1);  		
  		splitPane1.setOneTouchExpandable(true);
        splitPane1.setTopComponent(canvasPane);
        splitPane1.setBottomComponent(codePanel);
        
        splitPane.setLeftComponent(splitPane1);
    
        
        /*
         * Canvas
         */
  		JLayeredPane layeredPane = new JLayeredPane();
  		
        canvas = new Canvas(layeredPane, new EditorPane[] {editorPane, groupPane}, handler);
        canvas.addSelectionListener(this);
        canvas.addEditListener(undoManager);
        canvas.addMouseListener(new MouseAdapter() {
        	public void mousePressed(MouseEvent me) {        		
        		closeAreaAction.actionPerformed(new ActionEvent(canvas, ActionEvent.ACTION_PERFORMED, null));
        	}
        });
        transformPane.showPanel(canvas);
        layeredPane.add(canvas, JLayeredPane.DEFAULT_LAYER);

        canvasPanel = new JPanel(gbl);
        canvasPanel.setBackground(Color.gray);
        canvasPanel.add(layeredPane, new GridBagConstraints( 0, 0, 1, 1, 0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(1, 2, 2, 1), 3, 3));

        canvasPane.setViewportView(canvasPanel);
        
        /** 
         * ButtonPanel
         */        
        buttonPanel = new JPanel();
        buttonPanel.setLayout(gbl);
        buttonPanel.setPreferredSize(new Dimension(DEFAULT_BUTTON_PANEL_WIDTH, 600));
        
        canvas.buttonPanel = buttonPanel;

        /*
         * The texPanel
         */
        JButton texButton = null;
        JPanel texPanel = new JPanel();
        texPanel.setLayout(gbl);
        texPanel.setBorder(BorderFactory.createTitledBorder("Texdraw Code"));
        
        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(gbl);
        unitSp = new FloatSpinner(1, 0, 99, .1f);
        canvas.setUnitSpinner(unitSp);
        JLabel unitLabel = new JLabel("1 pixel =");
        JLabel ptLabel = new JLabel("pt");

        unitPanel.add(unitLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        unitPanel.add(unitSp, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        unitPanel.add(ptLabel, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        texPanel.add(unitPanel, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        
    	generateAction = new AbstractAction("Generate") {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
        		String code = getCode(); 
        		export(code);      		
        		if(outputArea!=null) {
        			showArea(OUTPUT);
        			splitPane1.setDividerLocation(splitPane1.getHeight()/2);
        			outputArea.requestFocus();
        		}
        	}
    	};
    	inputAction = new AbstractAction("Load...") {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
        	    showArea(INPUT);
        	    splitPane1.setDividerLocation(splitPane1.getHeight()/2);
        	    inputArea.requestFocus();
        	}
    	};
        texButton = new JButton(generateAction);
        loadButton = new JButton(inputAction);

        texPanel.add(loadButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 4));
        texPanel.add(texButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 4));
        
        
        buttonPanel.add(texPanel, new GridBagConstraints(0, 6, 2, 1, 0, 0,
            GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(2, 2, 2, 0), 0, 0));
   
        /*
         * Buttons etc.
         */
        
        addNodeButton = new JButton(addENodeAction);
        JButton addContourButton = new JButton(addContourAction);
        
        copyAction = new CopyAction();
        combiButton = new JButton(combineAction);
        
        JPanel undoPanel = new JPanel();
        undoPanel.setLayout(gbl);
        
        JButton undoButton = new JButton(undoIcon);
        undoButton.setDisabledIcon(undoDisIcon);
        undoButton.setAction(undoManager.undoAction);

        JButton redoButton = new JButton(redoIcon);
        redoButton.setDisabledIcon(redoDisIcon);
        redoButton.setAction(undoManager.redoAction);
        
        JButton deleteButton = new JButton(deleteIcon);
        deleteButton.setDisabledIcon(deleteDisIcon);
        deleteButton.setAction(deleteAction);
        
        undoPanel.add(undoButton, new GridBagConstraints(0, 0, 1, 1, 1, 0,
        		GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 2), 0, 0));
        undoPanel.add(redoButton, new GridBagConstraints(1, 0, 1, 1, 1, 0,
        		GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 2, 0, 2), 0, 0));
        undoPanel.add(deleteButton, new GridBagConstraints(2, 0, 1, 1, 1, 0,
        		GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 2, 0, 0), 0, 0));
        
        
        buttonPanel.add(addNodeButton, new GridBagConstraints(0, 0, 1, 1, 1, 0,
        		GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(4, 4, 2, 2), 0, 4));
        buttonPanel.add(addContourButton, new GridBagConstraints(1, 0, 1, 1, 1, 0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(4, 2, 2, 4), 0, 4));
        buttonPanel.add(combiButton, new GridBagConstraints(0, 3, 2, 1, 1, 0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(4, 15, 4, 15), 0, 0));
        buttonPanel.add(undoPanel, new GridBagConstraints(0, 5, 2, 1, 0, 0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(4, 15, 4, 15), 0, 0));
        
        JPanel diPanel = new JPanel();        
        diPanel.setLayout(gbl);       
        diPanel.setBorder(BorderFactory.createTitledBorder("Ornaments & Connectors"));
        
  	    dBox = new JComboBox<DBoxItem>(dBoxItems);
  	    dBox.setRenderer(new DBoxCellRenderer());
        dBox.setSelectedIndex(0);
        dBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(canvas!=null) {
					adjustDButton(canvas.selection, (DBoxItem)dBox.getSelectedItem());
				}
			}
        });
				
        dButton = new JButton(createAction0);
        
        diPanel.add(dBox, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        diPanel.add(dButton, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));


        dButton.setAction(createAction0);
        buttonPanel.add(diPanel, new GridBagConstraints(0, 2, 2, 1, 0, 0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(2, 2, 1, 2), 0, 0));

        
        createKeyBindingsFor(canvas, editorPane, transformPane, groupPane, undoButton, redoButton, deleteButton, 
        		dBox, dButton, addNodeButton, addContourButton, combiButton, texButton, loadButton, outputArea);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addTab("Edit", editorPane);
        tabbedPane.addTab("Transform", null, transformPane, "Transform selection");
        tabbedPane.addTab("Groups", null, groupPane, "Manage groups");
        
        buttonPanel.add(tabbedPane, new GridBagConstraints(0, 4, 2, 1, 1, 1,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));        
        
        splitPane.setRightComponent(buttonPanel);
        contentPanel.add( splitPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
        	GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 50, 50));
    
        this.add(contentPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 2, 2, 2), 50, 50));
        
        
        undoManager.init();

  	}
    
	private void showArea(String name) {
		CardLayout cl = (CardLayout)codePanel.getLayout();
		cl.show(codePanel, name);
		shownArea = name;
	}

	void addFirstPoint() {
    	canvas.clearSelection();
        jPasi.item.Point p = canvas.addPoint(canvas.getWidth()/2, canvas.getHeight()/2);
        canvas.select(p, false);        
        if(splitPane1!=null) {
            splitPane1.setDividerLocation(splitPane1.getHeight());
        }
   	}
    
    private void adjustDButton(List<Item> selection, DBoxItem sbi) {
        int n2 = count(ENode.class, selection, 0);
        dButton.setEnabled(sbi!=null && sbi.minNOfNodes<=n2);
    }
    
    private boolean canCombineConnectors(List<Item> l) {
        boolean possible = false;
        Iterator<Item> i = l.iterator();
        Item gov0 = null;
        while(i.hasNext() && gov0==null) {
            Item i0 = i.next();
            if(i0 instanceof IndependentItem) {
            	gov0 = i0.getGovernor();
            }
        }
        if(i.hasNext() && gov0 instanceof Connector) {
            possible = true;
            Connector rel = (Connector)gov0;
            ENode[] e = (ENode[])rel.getInvolutes();
            boolean foundOther = false;
    		while(possible && i.hasNext()) {
    		    Item item = (Item)i.next();
    		    if(item instanceof IndependentItem) {
	    		    Item gov = item.getGovernor();    				    
	    		    if(gov instanceof Connector) {
	    		        Connector rel_ = (Connector)gov;		        
	    		        ENode[] e_ = (ENode[])rel_.getInvolutes();
	    		        if(!(e_[0]==e[0] && e_[1]==e[1]) && !(e_[0]==e[1] && e_[1]==e[0])) {
	    		            possible = false;
	    		        } else if(rel!=rel_) {
	    		            foundOther = true;
	    		        }
	    		    } else {
	    		        possible = false;
	    		    }
    		    }
    		}
    		possible = possible && foundOther;
        }
        return possible;
    }
    
    private int count(Class<?> cl, List<Item> l, int offset) {
        int n = 0;
        int j = 0;
        for(Iterator<Item> i = l.iterator(); i.hasNext(); j++) {
            Item item = i.next();
            if(j>=offset && cl.isAssignableFrom(item.getClass())) {
                n++;
            }
        }
        return n;
    }

    protected void createKeyBindingsFor(JComponent... comps) {
        InputMap iMap = getInputMap();
        ActionMap aMap = getActionMap();
        
    	for(JComponent c: comps) {
    		InputMap im = c.getInputMap();
    		ActionMap am = c.getActionMap();
    		im.setParent(iMap);
    		am.setParent(aMap);
    	}
    	
    	/*
    	 * General commands.
    	 */
        Action addCNodeAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof CNode) {
					((CNode)item).doAddNode();
				}
			}
        };              
        Action defocusAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				canvas.clearSelection(item);
			}
        };              
        Action focusAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				canvas.clearSelection(item, false, true, true);
			}
        };
        Action ldAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				if(isInputAreaShowing()) {
					loadAction.actionPerformed(ae);
				} else {
					inputAction.actionPerformed(ae);
				}
			}
        };
        Action fineAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				incrementOverride.setEnabled(!incrementOverride.isEnabled());
			}
        };
        
        registerKeyBinding(getSelectTabAction(0), KeyEvent.VK_F1, 0, true);
        registerKeyBinding(getSelectTabAction(1), KeyEvent.VK_F2, 0, true);
        registerKeyBinding(getSelectTabAction(2), KeyEvent.VK_F3, 0, true);
        registerKeyBinding(combineAction, KeyEvent.VK_C, 0);
        registerKeyBinding(deleteAction, KeyEvent.VK_BACK_SPACE, 0);
        registerKeyBinding(deleteAction, KeyEvent.VK_DELETE, 0);
        registerKeyBinding(undoManager.undoAction, KeyEvent.VK_Z, 0);
        registerKeyBinding(undoManager.redoAction, KeyEvent.VK_Y, 0);
        registerKeyBinding(addENodeAction, KeyEvent.VK_E, 0);
        registerKeyBinding(addContourAction, KeyEvent.VK_K, 0);
        registerKeyBinding(addCNodeAction, KeyEvent.VK_N, 0);
        registerKeyBinding(focusAction, KeyEvent.VK_F, 0);
        registerKeyBinding(defocusAction, KeyEvent.VK_D, 0);
        registerKeyBinding(generateAction, KeyEvent.VK_G, Event.CTRL_MASK);
        registerKeyBinding(ldAction, KeyEvent.VK_L, Event.CTRL_MASK);
        registerKeyBinding(fineAction, KeyEvent.VK_F5, 0, true);
     
        /*
         * Commands for the creation of ornaments and connectors.
         */
        registerKeyBinding(getAction(LABEL), KeyEvent.VK_L, Event.ALT_MASK);
        registerKeyBinding(getAction(POINTER), KeyEvent.VK_Z, Event.ALT_MASK);
        registerKeyBinding(getAction(ADJUNCTION), KeyEvent.VK_A, Event.ALT_MASK);
        registerKeyBinding(getAction(CONTAINMENT), KeyEvent.VK_C, Event.ALT_MASK);
        registerKeyBinding(getAction(ENTAILMENT), KeyEvent.VK_E, Event.ALT_MASK);
        registerKeyBinding(getAction(IDENTITY), KeyEvent.VK_J, Event.ALT_MASK);
        registerKeyBinding(getAction(INCLUSION), KeyEvent.VK_I, Event.ALT_MASK);
        registerKeyBinding(getAction(SUBSUMPTION), KeyEvent.VK_Y, Event.ALT_MASK);
        registerKeyBinding(getAction(NEGATION), KeyEvent.VK_N, Event.ALT_MASK);
        registerKeyBinding(getAction(OBTAINMENT), KeyEvent.VK_O, Event.ALT_MASK);
        registerKeyBinding(getAction(PREDICATION), KeyEvent.VK_P, Event.ALT_MASK);
        registerKeyBinding(getAction(RESTRICTION), KeyEvent.VK_R, Event.ALT_MASK);
        registerKeyBinding(getAction(ORDER), KeyEvent.VK_S, Event.ALT_MASK);
        registerKeyBinding(getAction(TRANSITION), KeyEvent.VK_T, Event.ALT_MASK);
        registerKeyBinding(getAction(UNIVERSALQUANTIFICATION), KeyEvent.VK_U, Event.ALT_MASK);
        registerKeyBinding(createAction0, KeyEvent.VK_SPACE, 0);
        registerKeyBinding(createAction1, KeyEvent.VK_SPACE, Event.SHIFT_MASK);
    
        /*
         * Grouping commands.
         */
        Action groupingAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof GroupMember) {
					if(item instanceof GroupUIDelegator) {
						GroupManager<?> gm = ((GroupUIDelegator)item).getGroupManager();
						if(gm.canCreate()) {
							gm.doCreate();
					        groupPane.reconfigure();
						}
					} else {
						GroupManager.createGroup((GroupMember<?>)item);
				        groupPane.reconfigure();
					}
				}
			}
        };
        Action leaveAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof GroupMember) {
					GroupMember<?> gm = (GroupMember<?>)item;
					if(GroupManager.canLeaveGroup(gm)) {
						GroupManager.leaveGroup(gm);
					}
				}
			}
        };
        Action rejoinAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof GroupMember) {
					GroupMember<?> gm = (GroupMember<?>)item;
					if(GroupManager.canRejoinGroup(gm)) {
						GroupManager.rejoinGroup(gm);
					}
				}
			}
        };
        Action dissolveAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof GroupMember) {
					GroupMember<?> gm = (GroupMember<?>)item;
					if(GroupManager.canDissolveGroup(gm)) {
						GroupManager.dissolveGroup(gm);
					}
				}
			}
        };
        Action restoreAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof GroupMember) {
					GroupMember<?> gm = (GroupMember<?>)item;
					if(GroupManager.canRestoreGroup(gm)) {
						GroupManager.restoreGroup(gm);
					}
				}
			}
        };
        Action toggleAdding = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof GroupUIDelegator) {
					GroupManager<?> gm = ((GroupUIDelegator)item).getGroupManager();
					gm.externalSetAdding(!gm.isAdding());
				}
			}
        };
        Action toggleAddMembers = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				Item item = canvas.getItemToBeEdited();
				if(item instanceof GroupUIDelegator) {
					GroupManager<?> gm = ((GroupUIDelegator)item).getGroupManager();
					gm.externalSetAddingMembers(!gm.isAddingMembers());
				}
			}
        };           
        registerKeyBinding(groupingAction, KeyEvent.VK_G, 0);
        registerKeyBinding(leaveAction, KeyEvent.VK_L, 0);
        registerKeyBinding(rejoinAction, KeyEvent.VK_J, 0);
        registerKeyBinding(dissolveAction, KeyEvent.VK_L, Event.SHIFT_MASK);
        registerKeyBinding(restoreAction, KeyEvent.VK_J, Event.SHIFT_MASK);
        registerKeyBinding(toggleAdding, KeyEvent.VK_A, 0);
        registerKeyBinding(toggleAddMembers, KeyEvent.VK_M, 0);

        /*
         * Transformation commands.
         */
        Action selectTranslation = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				transform = TRANSLATION;
				//System.err.println(transform);
			}
        };           
        Action selectRotation = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {				
				transform = ROTATION;
				//System.err.println(transform);
			}
        };           
        Action selectScaling = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				transform = SCALING;
				//System.err.println(transform);
			}
        };
        Action increaseAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				performIncrease(1);
			}
        };           
        Action decreaseAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				performIncrease(-1);
			}
        };           
        Action shiftLeftAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				performShift(-1);
			}
        };           
        Action shiftRightAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				performShift(1);
			}
        };           
        Action toggleRotateLabels = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				EditorEntry ee = canvas.getRotateLabelsInfo();
				AbstractButton button = (AbstractButton)ee.getEditorComponent();
				button.doClick();
			}
        };
        Action toggleFlipArrowheads = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				EditorEntry ee = canvas.getFlipArrowheadsInfo();
				AbstractButton button = (AbstractButton)ee.getEditorComponent();
				button.doClick();
			}
        };
        Action toggleScaleArrowheads = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				EditorEntry ee = canvas.getScaleArrowheadsInfo();
				AbstractButton button = (AbstractButton)ee.getEditorComponent();
				button.doClick();
			}
        };
        Action toggleScaleLinewidths = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				EditorEntry ee = canvas.getScaleLinewidthsInfo();
				AbstractButton button = (AbstractButton)ee.getEditorComponent();
				button.doClick();
			}
        };
        Action toggleScaleENodes = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				EditorEntry ee = canvas.getScaleENodesInfo();
				AbstractButton button = (AbstractButton)ee.getEditorComponent();
				button.doClick();
			}
        };
        Action horizontalFlip = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				canvas.doHorizontalFlip();
			}
        };
        Action verticalFlip = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				canvas.doVerticalFlip();
			}
        };
        registerKeyBinding(selectTranslation, KeyEvent.VK_T, 0);
        registerKeyBinding(selectRotation, KeyEvent.VK_R, 0);
        registerKeyBinding(selectScaling, KeyEvent.VK_S, 0);
        registerKeyBinding(increaseAction, KeyEvent.VK_UP, 0);
        registerKeyBinding(decreaseAction, KeyEvent.VK_DOWN, 0);
        registerKeyBinding(shiftLeftAction, KeyEvent.VK_LEFT, 0);
        registerKeyBinding(shiftRightAction, KeyEvent.VK_RIGHT, 0);
        registerKeyBinding(toggleScaleArrowheads, KeyEvent.VK_A, Event.SHIFT_MASK);
        registerKeyBinding(toggleScaleLinewidths, KeyEvent.VK_W, Event.SHIFT_MASK);
        registerKeyBinding(toggleScaleENodes, KeyEvent.VK_E, Event.SHIFT_MASK);
        registerKeyBinding(toggleRotateLabels, KeyEvent.VK_R, Event.SHIFT_MASK);
        registerKeyBinding(toggleFlipArrowheads, KeyEvent.VK_F, Event.SHIFT_MASK);
        registerKeyBinding(horizontalFlip, KeyEvent.VK_H, 0);
        registerKeyBinding(verticalFlip, KeyEvent.VK_V, 0);

    }
    
	private void defineActions() {
        closeAreaAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
				shownArea = null;
       	        splitPane1.setDividerLocation(splitPane1.getHeight());
       	        canvas.requestFocus();
        	}
        };
        
        loadAction = new AbstractAction("Load") {
            private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent ae) {
	                String s = inputArea.getText();
	                load(s, replaceBox.isSelected());	                
	    	        splitPane1.setDividerLocation(splitPane1.getHeight());
	            }	        	
        };
        
        pasteAction = new AbstractAction("Paste") {
        	private static final long serialVersionUID = 1L;
        	public void actionPerformed(ActionEvent ae) {
        		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        	    Transferable contents = clipboard.getContents(null);
        	    boolean hasTransferableText =
        	      (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        	    if(hasTransferableText) {
        	      try {
        	        String result = (String)contents.getTransferData(DataFlavor.stringFlavor);        	        
        	        inputArea.setText("paste"+result);
        	      }
        	      catch(UnsupportedFlavorException ex){
        	        handler.handle(ex,  MainPanel.this, "Unsupported data flavor");
        	      }
        	      catch (IOException ex) {
          	        handler.handle(ex,  MainPanel.this, "IO exception");
        	      }
        	    }
        	}
        };
                
        addENodeAction = new AbstractAction("Add Node") {
            private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent ae) {
            	    EditEvent ee = new EditEvent(MainPanel.this, true);
            		undoManager.editing(ee);

            		ENode[] en = canvas.addEntities();
    	        	if(en.length==0) {
    	    			int x = canvas.getWidth()/2;
    	    			int y = canvas.getHeight()/2;
    	        	    en = new ENode[]{canvas.addEntity(x, y)};
            	    }
    	        	canvas.clearSelection(null, false, false, false);
    	        	for(ENode e: en) {
    	        	    canvas.select(e, false);
    	        	}
    	        	canvas.setItemToBeEdited(en[en.length-1]);
        		}
        };
        
        addContourAction = new AbstractAction("Add Contour") {
            private static final long serialVersionUID = 1L;

        	public void actionPerformed(ActionEvent ae) {
        	    EditEvent ee = new EditEvent(MainPanel.this, true);
        		undoManager.editing(ee);

        		Contour[] s = canvas.addContours();
	        	
        	    if(s.length==0) {
	        	    s = new Contour[]{canvas.addContour()};
        	    }
        	    canvas.clearSelection(null, false, false, false);
        	    CNode cn = null;
	        	for (Contour element : s) {
	        	    Set<CNode> nodes = element.nodeSet();
	        	    if(nodes.size()>0) {
		        	    cn = nodes.iterator().next();
		        	    canvas.select(cn, true);
	        	    }
	        	}
	        	if(cn!=null) {
	        		canvas.setItemToBeEdited(cn);
	        	}
    		}
        };
        
        deleteAction = new AbstractAction(null, deleteIcon) {
            	private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent e) {
                	EditEvent ee = new EditEvent(MainPanel.this, true);
                	fireEditing(ee);
                	canvas.removeSelection();        	
                }
        };
        deleteAction.setEnabled(false);
        
        class CreateAction extends AbstractAction {
            private static final long serialVersionUID = 1L;

            int variant = 0;
            
            CreateAction(int variant) {
            	super("Create");
            	this.variant = variant;
            }
            
			public void actionPerformed(ActionEvent ae) {
        	    EditEvent ee = new EditEvent(MainPanel.this, true);
        		undoManager.editing(ee);

        		DBoxItem sbi = (DBoxItem)dBox.getSelectedItem();
        		ENode e = null;
        		if(sbi.shouldCreateExtraEntity()) {
        		    Item item = canvas.selection.get(canvas.selection.size()-1);
        		    if(item instanceof jPasi.item.Point) {
        		        e = canvas.addEntityForPoint((jPasi.item.Point)item);
        		        canvas.deselect1(item);
        		        canvas.select(e, 0, false);
        		    }
        		}
        		sbi.multiAdd(canvas.selection, variant);
        		canvas.relocateItems();
        		if(e!=null) {
        		    canvas.clearSelection();
        		    canvas.select(e, false);
        		}
        	}
        	
        }
        
        createAction0 = new CreateAction(0);
        createAction1 = new CreateAction(1);
        
    }

	private ExportDestination export(String code) {
        outputArea.setText(code);
		StringSelection ss = new StringSelection(code);
		try{
    		Clipboard cb = getToolkit().getSystemClipboard();
			cb.setContents(ss, ss); 
			//if(true) throw new RuntimeException();
		}
		catch(Exception e) {        		    
	        splitPane1.setDividerLocation(splitPane1.getHeight()/2);
		    return PANE;
		}
		return CLIPBOARD;
    }

	protected void fireEditFailed(EditEvent ee) {
    	undoManager.editFailed(ee);
    }

	protected void fireEditing(EditEvent ee) {
    	undoManager.editing(ee);
    }
    
    protected void fireStartNewPeriod() {
    	EditEvent ee = new EditEvent(this);
    	undoManager.startNewPeriod(ee);
    }
    
    private Action getAction(final DependentItem di) {
    	Action result = null;
    	boolean found = false;
    	int i = 0;
    	for(int n = dBoxItems.length; i<n && !found; i++) {
    		if(dBoxItems[i].name.equals(di.name)) {
    			found = true;
    		}
    	}
    	if(found) {
    		final int index = i-1;
    		result = new AbstractAction() {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent ae) {
                	dBox.setSelectedIndex(index);
    			}
    		};
    	}
    	return result;
    }
	
    Canvas getCanvas() {
        return canvas;
    }
	
    public String getCode() {
		String code = codec.getCode(canvas);
        return code;
    }

    private Action getSelectTabAction(final int index) {
    	return new AbstractAction() {
            private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
				if(tabbedPane.isEnabledAt(index)) {
					tabbedPane.setSelectedIndex(index);
				}
			}
    		
    	};
    }

    /**
     * @see jPasi.Handler#handle(java.lang.Exception)
     */
    public void handle(Exception e, Component c, String title) {
        if(e instanceof ParseException) {
            ParseException pe = (ParseException)e;
            System.err.println("Exception while parsing part of: "+pe.text);
        }
        e.printStackTrace(System.err); 
    }
    
    private boolean isInputAreaShowing() {
        return shownArea==INPUT && splitPane1.getDividerLocation()<splitPane1.getHeight()/2+10;
    }
    
    /**
     * To be invoked by the ActionListener of a 'Load' button.
     */
    public void load(String s, boolean replace) {
    	EditEvent ee = new EditEvent(this, true);
        try {            
	        undoManager.editing(ee);
            codec.load(canvas, s, replace);
	        transformPane.reconfigure();
        }
        catch(ParseException pe) {
        	undoManager.editFailed(ee);
        	handler.handle(pe, this, "Parse Error");
        }
        catch(Exception e) {
        	undoManager.editFailed(ee);
        	handler.handle(e, this, "Error");
        }
        repaint();        
    }

	private void performIncrease(int d) {
		switch(transform) {
		case TRANSLATION: {
	        Item item = canvas.getItemToBeEdited();            
			if(item instanceof IndependentItem) {
				IndependentItem<?> i2 = (IndependentItem<?>)item;
				Rectangle2D.Float r = (Rectangle2D.Float)i2.getBounds2D();
				i2.changePosition(r.x, r.y-d);
				i2.getYInfo().notify(i2);
				EditEvent ee = new EditEvent(MainPanel.this, false);
        		undoManager.editing(ee);
			}
			break;
		}
		case ROTATION: {
			EditorEntry re = canvas.getRotateInfo();
			JSpinner sp = (JSpinner)re.getEditorComponent();
			try {
				sp.setValue(d>0? sp.getNextValue(): sp.getPreviousValue());
				EditEvent ee = new EditEvent(MainPanel.this, false);
        		undoManager.editing(ee);
			} catch(IllegalArgumentException iae) {}
			break;
		}
		case SCALING: {
			EditorEntry re = canvas.getScaleInfo();
			JSpinner sp = (JSpinner)re.getEditorComponent();
			try {
				sp.setValue(d>0? sp.getNextValue(): sp.getPreviousValue());
				EditEvent ee = new EditEvent(MainPanel.this, false);
        		undoManager.editing(ee);
			} catch(IllegalArgumentException iae) {}
			break;
		}
		}
    }
	
    private void performShift(int d) {
		switch(transform) {
		case TRANSLATION: {
	        Item item = canvas.getItemToBeEdited();            
			if(item instanceof IndependentItem) {
				IndependentItem<?> i2 = (IndependentItem<?>)item;
				Rectangle2D.Float r = (Rectangle2D.Float)i2.getBounds2D();
				i2.changePosition(r.x+d, r.y);
				i2.getXInfo().notify(i2);

				EditEvent ee = new EditEvent(MainPanel.this, false);
        		undoManager.editing(ee);
			}
			break;
		}
		case ROTATION: {
			EditorEntry re = canvas.getRotationIncrementInfo();
			JSpinner sp = (JSpinner)re.getEditorComponent();
			try {
				sp.setValue(d>0? sp.getNextValue(): sp.getPreviousValue());
			} catch(IllegalArgumentException iae) {}
			break;
		}
		case SCALING: {
			EditorEntry re = canvas.getScalingIncrementInfo();
			JSpinner sp = (JSpinner)re.getEditorComponent();
			try {
				sp.setValue(d>0? sp.getNextValue(): sp.getPreviousValue());
			} catch(IllegalArgumentException iae) {}
			break;
		}
		}
    }

    protected void registerKeyBinding(Action action, int key, int mask) {
		registerKeyBinding(action, key, mask, false);
    }

    protected void registerKeyBinding(Action action, int key, int mask, boolean special) {
		registerKeyBindingFor(this, action, key, mask);
		if(special) {
			specialKeyBindings.put(KeyStroke.getKeyStroke(key, mask), action);
		}
    }

    /**
     * To be invoked by the ActionListener of an 'Undo' or 'Redo' button.
     */
    public void restoreFromCode(String s) {
        try {            
            codec.load(canvas, s, true);
	        transformPane.reconfigure();
        }
        catch(Exception e) {
        	handler.handle(e, this, "Error");
        }
        repaint();        
    }
    
    public void selectionChanged(Canvas.SelectionEvent se) {
        List<Item> selection = ((Canvas)se.getSource()).selection;
        
        /*
         * Adjust combi button.
         */
        int n = selection.size();
        
        boolean found = false;
        boolean combine = false;
        if(n>0) {
	        Item i0 = null;
        	Iterator<Item> i = selection.iterator();
        	while(i.hasNext() && i0==null) {
        		Item it = (Item)i.next();
        		if(!(it instanceof jPasi.item.Point)) {
        			i0 = it;
        		}
        	}
	        Item gov = i0!=null? i0.getGovernor(): null;
	        if(i0 instanceof Ornament) {
	            found = true;
		        if(i0 instanceof Label) {
		            copyAction.setState(CopyAction.COPY_LABEL);
		        }
		        else {
		        	copyAction.setState(CopyAction.COPY_ORNAMENT);		            
		        }
		        boolean enable = count(IndependentItem.class, selection, 1)>0;
                copyAction.setEnabled(enable);
		        combiButton.setEnabled(enable);                
	        } else if(gov instanceof Connector) {
                found = true;
                copyAction.setState(CopyAction.COPY_CONNECTOR);
	            if(canCombineConnectors(selection)) { 
	                combine = true;
	                copyAction.setEnabled(false);
	                combineAction.setEnabled(true);
	            } else {
	            	boolean enable = count(ENode.class, selection, 1)>1;
	                copyAction.setEnabled(enable);
	            	combineAction.setEnabled(enable); 
	            }
	        }
        }
        combineAction.setCombine(combine);
        if(!found) {
            copyAction.setState(CopyAction.COPY_SELECTION);
	        boolean enable = count(IndependentItem.class, selection, 0)>0;
            copyAction.setEnabled(enable);
	        combineAction.setEnabled(enable);                
        }
        
        /*
         * Adjust dButton.
         */
        adjustDButton(selection, (DBoxItem)dBox.getSelectedItem());
        
        /*
         * Adjust deleteAction.
         */
        deleteAction.setEnabled(n - count(Point.class, selection, 0) > 0);
    }

/*// Sandbox code
 
	static class R {
		String s = "df";
		Object lock = null;
		R(String s) {
			this.s = s;
		}	
		R(String s, Object o) {
			this.s = s;
			this.lock = o;
		}	
		public Thread work() {
			Thread t = new Thread() {
				public synchronized void run() {
					synchronized(lock) {
						for(int i = 0; i<10; i++) {
							System.err.println(s);
							try {
								wait(100);
							}
							catch(Exception e) {
								System.err.println(e);
							}
						}
					}
				}
			};
			return t;
		}
	}

	public static void main(String[] args) {
		Object o = new Object();
		new R("sdf", o).work().start();
		new R("ffoi", o).work().start();
		System.err.println("done");
	}
*/

}

