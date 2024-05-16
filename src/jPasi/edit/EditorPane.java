/*
 * Created on 19.01.2007
 *
 */
package jPasi.edit;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;

import jPasi.edit.EditorEntry.Notifier;
import util.Reflect;
import util.swing.SpinnerNumberModel.IncrementOverride;


public class EditorPane extends JScrollPane {

    private final class Card {
        final EntryInfo[] info;        
        
        int scrollBarValue;
        
        Card(EntryInfo[] c) {
            info = c;
        }
    }

    private class ConfigThread extends Thread {
        protected volatile boolean shouldStop;
        public void requestStop() {
            shouldStop = true;
        }
    }

	private final class EntryInfo {
        Method readMethod;
        Method requestMethod;
        Method writeMethod;

        /*
         * The first component of this array is an editable Component. The following ones are included because they
         * have to be setVisible(true/false) together with the former. 
         */
        JComponent[] components;
        
        public EntryInfo(EditorEntry ee, JComponent[] comps) {
        	Class<?> cl = target.getClass();
        	components = comps;
        	Method rm = null, rqm = null, wm = null;
        	EditorEntry.Type type = ee.getType();
        	try {
	        	switch(type) {
	        	case RESET: {
	        		break;
	        	}
	        	case LABEL: {
	    	        rm = cl.getMethod("get"+ee.access, (Class[])null);
	        		break;
	        	}
	        	case BOOLEAN: 
	        	case TOGGLE: {
	    	        rm = cl.getMethod("is"+ee.access, (Class[])null);
	    	        wm = cl.getMethod("set"+ee.access, new Class[]{type.valueType});
	    	        rqm = cl.getMethod("canChange"+ee.access, (Class[])null);
	    	        break;
	        	}
	        	case INTEGER: 
	        	case FLOAT: 
	        	case STRING:
	        	case TEXT: 
	        	case CHOICE: {
	    	        rm = cl.getMethod("get"+ee.access, (Class[])null);
	    	        wm = cl.getMethod("set"+ee.access, new Class[]{type.valueType});
	    	        rqm = cl.getMethod("canChange"+ee.access, (Class[])null);
	    	        break;
	        	}
	        	case ACTION: {
	    	        try {
	    	        	rm = cl.getMethod("get"+ee.access, (Class[])null);
	    	        } catch(NoSuchMethodException nsme) { 
	    	        	/* 
	    	        	 * The method for reading the 'state' (see EditorEntry) does not need to be declared.
	    	        	 */	    	        	
	    	        }
	    	        break;
	        	}
	        	default: throw new Error("Unexpected entry type: "+type);
	        	}
        	} catch(NoSuchMethodException nsme) {
        		if(rm==null || wm==null) {
        			nsme.printStackTrace();
        			throw new Error("Missing method for class "+cl.getSimpleName());
        		}
        	}
        	init(rm, rqm, wm);
        }
        
        public EntryInfo(Method rm, Method rqm, Method wm, JComponent[] comps) {
        	components = comps;
        	init(rm, rqm, wm);
        } 
        
        private void init(Method rm, Method rqm, Method wm) {
    		Method[] methods = new Method[] {rm, rqm, wm};
    		for(int i = 0, n = methods.length; i<n; i++) {
        		if(methods[i]!=null) {
    	    		String name = methods[i].getName();
    	    		Class<?>[] params = methods[i].getParameterTypes();
    	    		try {
    	    			Class<?> c = Reflect.getOriginallyDeclaringClass(name, params, methods[i].getDeclaringClass());
    	    			methods[i] = c.getMethod(name, params);
    	    		} catch(Exception e) {
    	    			throw new Error(e);
    	    		}
        		}
        	}
    		readMethod = methods[0];
    		requestMethod = methods[1];
    		writeMethod = methods[2];        	
        }
        
        public String toString() {
        	return "EI "+(readMethod!=null? readMethod.getName(): null)+" "+
        		(requestMethod!=null? requestMethod.getName(): null)+" "+
        		(writeMethod!=null? writeMethod.getName(): null)+" "+components[0].getClass().getName()+" ";
        }
    }
	

	
	
    /*
     * Contains some code to make sure that the particular FocusListener specified with setFocusListener() will be added to
     * every editor (or, in fact, the underlying TextField) that might be associated with this Spinner. 
     * (This code is apparently not necessary, since the editor seems to stay constant in all actually occurring cases.)
     */
    public class Spinner extends JSpinner {

        private static final long serialVersionUID = 509811735105992061L;
        
        FocusListener focusListener;
        public Spinner(SpinnerModel sm) {
            super(sm);
            JComponent e = getEditor(); 
            if(e instanceof JSpinner.DefaultEditor) {
                JTextField field = ((JSpinner.NumberEditor)e).getTextField();
                addSpecialKeyBindings(field);
            }
        }
        
		public void fireStateChanged() {
        	super.fireStateChanged();
        }
        
        private void setFL(FocusListener fl, JComponent c) {
            FocusListener[] fls = c.getListeners(FocusListener.class);
            boolean found = false;
            for(int i = 0; !found && i<fls.length; i++) {
                if(fls[i]==fl) found = true;
            }
            if(!found) {
                c.addFocusListener(fl);
            }
        }
        
        public void setFocusListener(FocusListener fl) {
            this.focusListener = fl;
            JComponent e = getEditor();
            if(e instanceof JSpinner.DefaultEditor) {
                e = ((JSpinner.DefaultEditor)e).getTextField();
            }
            setFL(fl, e);
        }
    }        
    
    public class TextField extends JTextField {

        private static final long serialVersionUID = 2681971741687113068L;

        public TextField() {
        	super();
        }
        
        @Override
        public void fireActionPerformed() {
            super.fireActionPerformed();
        }
    }

    private static final long serialVersionUID = -2287170309919528048L;
    private static final String EMPTY = "empty";
    private CardLayout cl = new CardLayout();
    private Hashtable<String, Card> cards = new Hashtable<String, Card>();
    private final GridBagLayout gbl = new GridBagLayout();
    private final Border bevelBorder = BorderFactory.createLoweredBevelBorder();
    private JPanel panel;
    private Color textBackground;
    private Editable target;
    private Card card;
    private ConfigThread configThread;
	private boolean configInProgress;
	private IncrementOverride incrementOverride;
    private HashMap<KeyStroke, Action> specialKeyBindings;
    
    private volatile boolean configBlocked;
    private Hashtable<EntryInfo, EditorEntry.Notifier> notifiers = new Hashtable<EntryInfo, EditorEntry.Notifier>();
    
    private EditorTopic topic;
    
    private List<EditorPaneListener> listeners = new ArrayList<EditorPaneListener>();
    
    private List<EditListener> editListeners  = new ArrayList<EditListener>();
    
    
    public EditorPane() {
        this(null, null, null);
    }
    
    public EditorPane(EditorTopic topic, IncrementOverride incrementOverride, HashMap<KeyStroke, Action> bindings) {
        super();
        
        this.topic = topic;
        this.incrementOverride = incrementOverride;
        this.specialKeyBindings = bindings;
        
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        panel = new JPanel();
        setViewportView(panel);
        panel.setLayout(cl);
       
        JPanel emptyPanel = new JPanel();        
        panel.add(emptyPanel, EMPTY);
        
        verticalScrollBar.getModel().addChangeListener(new ChangeListener() {
	           public void stateChanged(ChangeEvent ce) {
	               if(target!=null && card!=null) {
		               card.scrollBarValue = verticalScrollBar.getValue();
	               }
	           }
	        });        
    }
    
    private void addBooleanControl(final EditorEntry ee, JPanel panel, final EntryInfo[] eis, final int i, 
            final AbstractButton button, int leftInset, int topSpace, int bottomSpace) {
	    
        panel.add(button, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0,
	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
	            new Insets(4+topSpace, leftInset, bottomSpace, 2), 0, 0));
	    
	    eis[i] = new EntryInfo(ee, new JComponent[]{button});
	    button.addItemListener(new ItemListener() {
	    	public void itemStateChanged(ItemEvent ie) {
	           boolean b = button.isSelected();
	    	   if(configInProgress) return; 
	           try {
        	        update(target, ee, eis[i], new Object[]{Boolean.valueOf(b)});
        	    }
        	    catch(Exception e) {
        	        e.printStackTrace(System.err);
        	    }	        	           
	       }    	
	    });
    }
	
    public void addEditListener(EditListener el) {
    	editListeners.add(el);
    }
    
	public void addEditorPaneListener(EditorPaneListener epl) {
    	listeners.add(epl);
    }
	
	private Card addNewPanel(List<EditorEntry> list, String name) {
    
	    final EditorEntry[] info = list==null? new EditorEntry[0]: (EditorEntry[])list.toArray(new EditorEntry[0]);
	    final EntryInfo[] eis = new EntryInfo[info.length];
	    JPanel panel = new JPanel();
	    panel.setLayout(gbl);
	    int rightMargin = 2;
	    
	    int i = 0;
	    
	    for(; i<info.length; i++) {
	        if(info[i]==null) continue;
	        final EditorEntry ee = info[i];
	        int topSpace = ee.getTopSpace();
	        int leftSpace = ee.getLeftSpace();
	        int bottomSpace = ee.getBottomSpace();
	        
	        switch(ee.getType()) {
	        	case RESET: {
	        	    JButton button = addResetButton(info, panel, eis, i, "Defaults", topSpace, bottomSpace);
	        	    eis[i] = new EntryInfo(null, null, null, new JComponent[]{button});
	        	    break;
	        	}
	        	case LABEL: {
	        	    JPanel panel1 = new JPanel();
	        	    panel1.setLayout(gbl);
	    	        JLabel descriptionLabel = new JLabel(ee.getLabel());
	    	        JLabel infoLabel = new JLabel();
	    	        
	    	        if(ee.getLabel()!=null) {
	    	            panel1.add(descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
	        	            GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 2), 0, 0));
	    	        }
	        	    panel1.add(infoLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
	        	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 0), 0, 0));

	        	    panel.add(panel1, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0,
	        	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
	        	            new Insets(4+topSpace, leftSpace, bottomSpace, 2), 0, 0));
	        	    
	        	    eis[i] = new EntryInfo(ee, new JComponent[] {infoLabel, descriptionLabel});
	        	    break;
	        	}
	        	case BOOLEAN: {
	        	    JCheckBox box = new JCheckBox(ee.getLabel());
	        	    addBooleanControl(ee, panel, eis, i, box, 12+leftSpace, topSpace, bottomSpace);
	        	    break;
	        	}
	        	case TOGGLE: {
	        	    JToggleButton button = new JToggleButton(ee.getLabel());
	        	    addBooleanControl(ee, panel, eis, i, button, 2+leftSpace, topSpace, bottomSpace);
	        	    break;
	        	}
	        	case INTEGER: {
	    	        JLabel label = new JLabel(ee.getLabel());
	        	    
	    	        JPanel panel1 = panel;
	        	    int i1 = i;
	        	    if(ee.hasOwnPanel()) {
	        	        panel1 = new JPanel();
	        	        panel1.setLayout(gbl);
	        	        i1 = 0;
	        	    }
	    	        final Spinner spinner = new Spinner(new util.swing.SpinnerNumberModel(
	        	            (Integer)ee.getDefaultValue(), 
	        	            (Integer)ee.getMin(), 
	        	            (Integer)ee.getMax(), 
	        	            (Integer)ee.getIncrement(), 
	        	            ee.hasCyclicRange(),
	        	            incrementOverride));
	        	    panel1.add(label, new GridBagConstraints(0, i1, 1, 1, 0.0, 0.0,
	        	            GridBagConstraints.EAST, GridBagConstraints.NONE, 
	        	            new Insets(4+topSpace, 5+leftSpace, bottomSpace, 2), 0, 0));
	        	    panel1.add(spinner, new GridBagConstraints(1, i1, 1, 1, 0, 0,
	        	            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, 
	        	            new Insets(4+topSpace, 2, bottomSpace, rightMargin), 0, 0));
	        	    if(ee.hasOwnPanel()) {
	        	        panel.add(panel1, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0, 
	        	                GridBagConstraints.EAST, GridBagConstraints.BOTH, 
	        	                new Insets(topSpace, leftSpace, bottomSpace, rightMargin), 0, 0));
	        	    }
	        	    
	        	    eis[i] = new EntryInfo(ee, new JComponent[]{spinner, label});
	        	    final EntryInfo ei = eis[i];
	        	    
	        	    final Runnable updater = new Runnable() {
	        	        public void run() {
	        	            if(configInProgress) return;
		        	        update(target, ee, ei, new Object[]{(Integer)spinner.getValue()});
	        	        }
	        	    };
	        	    
	        	    spinner.setFocusListener(new FocusListener() {
	        	        public void focusGained(FocusEvent fe) {
	        	            setConfigBlocked(true);
	        	        }
	        	        public void focusLost(FocusEvent fe) {
	        	        	boolean changed = false;
	        	        	try {
	                	        Integer val = (Integer)ei.readMethod.invoke(target, (Object[])null);
	                	        changed = !val.equals(spinner.getValue());
	                	    }
	                	    catch(Exception e) {
	                	        e.printStackTrace(System.err);
	                	    }	        	        
	                	    if(changed) {
	                	    	updater.run();
	                	    }
	        	            setConfigBlocked(false);
	        	        }
	        	    });
	        	    spinner.addChangeListener(new ChangeListener() {
            			public void stateChanged(ChangeEvent ce) {
            			    updater.run();
            			}
            		});
	                
	        	    break;
	        	}
	        	case FLOAT: {
	    	        JLabel label = new JLabel(ee.getLabel());
	        	    final Spinner spinner = new Spinner(new util.swing.SpinnerNumberModel(
	        	            (Float)ee.getDefaultValue(), 
	        	            (Float)ee.getMin(), 
	        	            (Float)ee.getMax(), 
	        	            (Float)ee.getIncrement(), 
	        	            ee.hasCyclicRange(),
	        	            incrementOverride));

	        	    JPanel panel1 = panel;
	        	    int i1 = i;
	        	    int rMargin = rightMargin;
	        	    if(ee.hasOwnPanel()) {
	        	        panel1 = new JPanel();
	        	        panel1.setLayout(gbl);
	        	        i1 = 0;
	        	        rMargin = 0;
	        	    }
	        	    panel1.add(label, new GridBagConstraints(0, i1, 1, 1, 0.0, 0.0,
	        	            GridBagConstraints.EAST, GridBagConstraints.NONE, 
	        	            new Insets(4+topSpace, 5+leftSpace, bottomSpace, 2), 0, 0));
	        	    panel1.add(spinner, new GridBagConstraints(1, i1, 1, 1, 0, 0,
	        	            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, 
	        	            new Insets(4+topSpace, 2, bottomSpace, rMargin), 0, 0));
	        	    if(ee.hasOwnPanel()) {
	        	        panel.add(panel1, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0,
		        	            GridBagConstraints.EAST, GridBagConstraints.NONE, 
		        	            new Insets(topSpace, leftSpace, bottomSpace, rightMargin), 0, 0));
	        	    }
	        	    
	                eis[i] = new EntryInfo(ee, new JComponent[]{spinner, label});
	        	    final EntryInfo ei = eis[i];
	        	    
	        	    final Runnable updater = new Runnable() {
	        	        public void run() {
	        	            if(configInProgress) return;
	        	            try {
			        	        update(target, ee, ei, new Object[]{(Float)spinner.getValue()});
			        	    }
			        	    catch(Exception e) {
			        	        e.printStackTrace(System.err);
			        	    }	        	            
	        	        }
	        	    };
	        	    
	        	    spinner.setFocusListener(new FocusListener() {
	        	        public void focusGained(FocusEvent fe) {
	        	            setConfigBlocked(true);
	        	        }
	        	        public void focusLost(FocusEvent fe) {
	        	        	boolean changed = false;
	        	        	try {
	                	        Float val = (Float)ei.readMethod.invoke(target, (Object[])null);
	                	        changed = !val.equals(spinner.getValue());
	                	    }
	                	    catch(Exception e) {
	                	        e.printStackTrace(System.err);
	                	    }	        	        
	                	    if(changed) {
	                	    	updater.run();
	                	    }
	        	            setConfigBlocked(false);
	        	        }
	        	    });
	                spinner.addChangeListener(new ChangeListener() {
            			public void stateChanged(ChangeEvent ce) {
	        	            updater.run();
            			}
            		});	                
	                
	        	    break;
	        	}
	        	case STRING: {
	    	        JLabel label = new JLabel(ee.getLabel());
	    	        final TextField field = new TextField();
	    	        field.setBackground(getTextBackground());
	    	        field.setBorder(bevelBorder);

	    	        if(ee.getLabel()!=null) {
	    	            panel.add(label, new GridBagConstraints(0, i, 1, 1, 0.0, 0.0,
	        	            GridBagConstraints.EAST, GridBagConstraints.NONE, 
	        	            new Insets( 4+topSpace, 2+leftSpace, bottomSpace, 2), 0, 0));
	    	        }
	        	    panel.add(field, new GridBagConstraints(1, i, 1, 1, 0.0, 0.0,
	        	            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
	        	            new Insets( 4+topSpace, 2, bottomSpace, 2), 0, 0));
	        	    
	        	    eis[i] = new EntryInfo(ee, new JComponent[]{field, label});
	        	    final EntryInfo ei = eis[i];

	        	    final Runnable updater = new Runnable() {
	        	        public void run() {
	        	            if(configInProgress) return;
	        	            try {
			        	        update(target, ee, ei, new Object[]{new String(field.getText())});			        	        
			        	    }
			        	    catch(Exception e) {
			        	        e.printStackTrace(System.err);
			        	    }	        	            
	        	        }
	        	    };
	        	    
	        	    field.addFocusListener(new FocusListener() {
	        	        public void focusGained(FocusEvent fe) {
	        	            setConfigBlocked(true);
	        	        }
	        	        public void focusLost(FocusEvent fe) {
	        	            updater.run();
	        	            setConfigBlocked(false);
	        	        }
	        	    });
	        	    field.addActionListener(new ActionListener() {
        	        	public void actionPerformed(ActionEvent ae) {
        	        	    updater.run();
                        }
	        	    });
        	    
	    	        break;
	        	}
	        	case TEXT: {
	    	        JPanel panel1 = new JPanel();
	    	        panel1.setLayout(gbl);
	    	        JLabel label = new JLabel(ee.getLabel());
	    	        JScrollPane scrollPane = new JScrollPane();
	    	        final JEditorPane pane = new JEditorPane();
	    	        pane.setBackground(getTextBackground());
	    	        scrollPane.setViewportView(pane);
	    	        scrollPane.setMinimumSize(new Dimension(110, 180));

	    	        boolean hasLabel = ee.getLabel()!=null;
	    	        if(hasLabel) {
		    	        panel1.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
		        	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
		        	            new Insets(2, 5, 2, 2), 0, 0));
	    	        }
	    	        panel1.add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
	        	            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
	        	            new Insets(0, 2, 2, 2), 0, 0));
	        	    
	        	    eis[i] = new EntryInfo(ee, new JComponent[]{pane, label});
	        	    final EntryInfo ei = eis[i];

	        	    pane.getDocument().addDocumentListener(new DocumentListener() {
	        	        	public void changedUpdate(DocumentEvent arg0) {
                                updateItem();
                            }
	        	        	public void insertUpdate(DocumentEvent arg0) {
	        	        	    updateItem();
                            }
                            public void removeUpdate(DocumentEvent arg0) {
                                updateItem();
                            }
                            private void updateItem() {
		        			    if(configInProgress) return;
		        	            try {
				        	        update(target, ee, ei, new Object[]{new String(pane.getText())});
				        	    }
				        	    catch(Exception e) {
				        	        e.printStackTrace(System.err);
				        	    }	        	            
	        	        	}
		        	    });
	        	    
	        	    panel.add(panel1, new GridBagConstraints(0, i, 2, 1, 1.0, 1.0,
	        	            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
	        	            new Insets(2+topSpace, 2+leftSpace, 2+bottomSpace, 2), 0, 0));
	        	    
	        	    break;
	        	}
	        	case ACTION: {
	        	    JButton button = new JButton(ee.getLabel());
	        	    panel.add(button, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0,
	        	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
	        	            new Insets(4+topSpace, 2+leftSpace, bottomSpace, 2), 0, 0 ) );
	        	    
	        	    eis[i] = new EntryInfo(ee, new JComponent[] {button});
	        	    final EntryInfo ei = eis[i];

	        	    button.addActionListener(new ActionListener() {
		        	        public void actionPerformed(ActionEvent ae) {		        	            
		        	            Object state = null;
		        	            if(ei.readMethod!=null) {
			        	            try {
					        	        state = ei.readMethod.invoke(target, (Object[])null);
			        	            } 
			        	            catch(Exception e) {
					        	        e.printStackTrace(System.err);
			        	            }
		        	            }
		        	            Method request = null;
		        	            try {
				        	        request = target.getClass().getMethod("can"+ee.getMethodString(state), (Class[])null);		        	            	
		        	            } catch(NoSuchMethodException nsme) {		        	            	
		        	            }
		        	            try {
		        	            	Method m = target.getClass().getMethod("do"+ee.getMethodString(state), (Class[])null);
		        	            	update(target, request, m, ee.isGlobal(), (Object[])null, true);
				        	    }
				        	    catch(Exception e) {
				        	        e.printStackTrace(System.err);
				        	    }
				        	    updateElement(target, ee, ei);
				        	    if(ee.hasReconfigurativePower()) {
				        	        reconfigure();
				        	    }
		        	        }
		        	    });
	        	    
	        	    break;
	        	}
	        	case CHOICE: {
	        	    final JComboBox box = new JComboBox(ee.getItems());
	        	    panel.add(box, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0,
	        	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
	        	            new Insets(2+topSpace, 2+leftSpace, 2+bottomSpace, 2), 0, 0));
		        	
	        	    eis[i] = new EntryInfo(ee, new JComponent[] {box});	        	    
	        	    final EntryInfo ei = eis[i];

	        	    box.addItemListener(new ItemListener() {
	        	        public void itemStateChanged(ItemEvent ie) {
	        	            try {
	        	                update(target, ee, ei, new Object[] {box.getSelectedItem()});
	        	            } catch(Exception e) {
	        	                e.printStackTrace(System.err);
	        	            }
	        	        }
	        	    });
	        	    
	        	    break;
	        	}
	        	default: {
	        	    JLabel label = new JLabel("?");
	        	    panel.add(label, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0,
        	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
	        	    eis[i].components = new JComponent[]{label};
	        	}
	        }
	        ee.setEditorComponent(eis[i].components[0]);
	        
	        for(JComponent c: eis[i].components) {
	        	addSpecialKeyBindings(c);
	        }
	        
	        if(ee.needsNotifier()) {
	        	final EntryInfo ei = eis[i];
		        notifiers.put(ei, new Notifier() {
	    	            public void run(Editable item) {
	    	                if(item==target || target==item.getEditorDelegate(topic)) {	    	            	    
	    	                    configInProgress = true;
	    	            	    try {		    
		    	                    updateElement(target, ee, ei);
	    	            	    }
	    	            	    catch(Exception e) {
	    	            	        e.printStackTrace(System.err);
	    	            	    }
	    	            	    configInProgress = false;
	    	                }
	    	            }
	    	        });
		    }
	    }

	    JPanel fillPanel = new JPanel();
	    panel.add(fillPanel, new GridBagConstraints(0, info.length+1, 2, 1, 0, 1.0,
	            GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
	    
	    this.panel.add(panel, name);
	    Card card = new Card(eis);
	    cards.put(name, card);	    
	    return card;
	}
	
	private JButton addResetButton(final Object[] info, JPanel panel, final EntryInfo[] eis, final int i, String label,
            int topSpace, int bottomSpace) {
        JButton resetButton = new JButton(label);
        
	    panel.add(resetButton, new GridBagConstraints(0, i, 2, 1, 0.0, 0.0,
	            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
	            new Insets(5+topSpace, 2, 2+bottomSpace, 2), 0, 0));
	    
	    resetButton.addActionListener(new ActionListener() {
    	       public void actionPerformed(ActionEvent ae) {
	    	       for(int j = 0; j<i; j++) {
	    		      if(info[j]==null) continue;
	    		      EditorEntry ee = (EditorEntry)info[j];
	    		      if(ee.isDefaultSet()) {
		    	          switch(ee.getType()) {
		    	          	case BOOLEAN: {
		    	          	    JCheckBox box = (JCheckBox)eis[j].components[0];
		    	          	    box.setSelected(((Boolean)ee.getDefaultValue()).booleanValue());
		    	          	    break;
		    	          	}
		    	          	case TOGGLE: {
		    	          	    JToggleButton button = (JToggleButton)eis[j].components[0];
		    	          	    button.setSelected(((Boolean)ee.getDefaultValue()).booleanValue());
		    	          	    break;
		    	          	}
		    	          	case INTEGER:
		    	          	case FLOAT: {
		    	                Spinner spinner = (Spinner)eis[j].components[0];
		    	                spinner.setValue(ee.getDefaultValue());
		    	                spinner.fireStateChanged();
		    	          	    break;
		    	          	}
		    	          	case STRING: {
		    	          	    TextField field = (TextField)eis[j].components[0];
		    	          		field.setText((String)ee.getDefaultValue());
		    	          		field.fireActionPerformed();
		    	          		break;
		    	          	}
		    	          	case TEXT: {
		    	          	    JTextComponent c = (JTextComponent)eis[j].components[0];
		    	          		c.setText((String)ee.getDefaultValue());
		    	          		break;
		    	          	}
		    	          	default: {
		    	          	}
		    	          }
	    		      }
	    	       }
    	       }
    	    });
	    
	    return resetButton;
    }
	
	private synchronized void adjustScrollBar(int n) {
	    JScrollBar bar = verticalScrollBar;
        /*
        int m = bar.getValue();
        int sign = m<n? 1: m>n? -1: 0;
        float step = (n-m)/8f;
        for(int l = m; sign*l<sign*n; l+=step) {
            setVisible(false);
            bar.setValue(l);
            setVisible(true);
            try {
                Thread.sleep(20);
            } catch(InterruptedException ie) {}
        }
        */
	    boolean b = isVisible();
        setVisible(false);
        bar.setValue(n);
        setVisible(b);
    }
	
	private void configure(EntryInfo[] eis, Editable target) {
	    List<EditorEntry> list = target.getInfo(topic);
	    if(list==null) return;   	    
	    configInProgress = true;
	    try {		    
		    for(int i = 0; i<eis.length; i++) {
		        EditorEntry ee = list.get(i);
		        if(ee==null) continue;
		        updateElement(target, ee, eis[i]);
			    if(ee.needsNotifier()) {
			        ee.setNotifier(notifiers.get(eis[i]));
			    }
		    }
	    }
	    catch(Exception e) {
	        e.printStackTrace();
	    }
	    configInProgress = false;
	}

	private void addSpecialKeyBindings(JTextComponent c) {
        Keymap kmap =  c.getKeymap();
        if(specialKeyBindings!=null) {
            Set<KeyStroke> keys = specialKeyBindings.keySet();
			
			for(KeyStroke k: keys) {
				kmap.addActionForKeyStroke(k, specialKeyBindings.get(k));
			}
        }
	}
	
	private void addSpecialKeyBindings(JComponent c) {
        ActionMap amap =  c.getActionMap();
        InputMap imap = c.getInputMap();
        if(specialKeyBindings!=null) {
            Set<KeyStroke> keys = specialKeyBindings.keySet();
			
			for(KeyStroke k: keys) {
				Action action = specialKeyBindings.get(k);
				imap.put(k, action);
				amap.put(action, action);
			}
        }
	}

	private boolean elementChangeable(Editable target, EntryInfo ei) {
        boolean b = true;
        if(ei.requestMethod!=null) {
	        try {        	
	            b = ((Boolean)ei.requestMethod.invoke(target, (Object[])null)).booleanValue();
	        }
	        catch(Exception e) {
	            e.printStackTrace();
	        }
        }
        return b;
    }
    
    protected void fireEditing(EditEvent ee) {
    	for(EditListener el: editListeners) {
    		el.editing(ee);
    	}
    }

    protected void fireEditFailed(EditEvent ee) {
    	for(EditListener el: editListeners) {
    		el.editFailed(ee);
    	}
    }

    protected void fireStartNewPeriod() {
    	EditEvent ee = new EditEvent(this);
    	for(EditListener el: editListeners) {
    		el.startNewPeriod(ee);
    	}
    }

    protected void firePanelChanged(List<EditorEntry> entries) {
    	EditorPaneEvent epe = new EditorPaneEvent(this, entries);
    	for(EditorPaneListener epl: listeners) {
    		epl.panelChanged(epe);
    	}
    }

    public Color getTextBackground() {
        return textBackground;
    }    
  
    boolean isScrolling() {
        return verticalScrollBar.getValueIsAdjusting();
    }

    public void reconfigure() {
        if(card!=null) configure(card.info, target);
    }
    
    public void removeEditListener(EditListener el) {
    	editListeners.remove(el);
    }
    
    public void removeEditorPaneListener(EditorPaneListener epl) {
    	listeners.remove(epl);
    }

    private void setConfigBlocked(boolean b) {
        configBlocked = b;
    }
    public void setTextBackground(Color textBackground) {
        this.textBackground = textBackground;
    }
    
    public void showEmptyPanel() {
	    grabFocus(); // necessary to trigger focusLost() for EditorComponents
	    final ConfigThread old;
	    if(configThread!=null) {
	        old = configThread;
	        old.requestStop();
	    } else {
	        old = null;
	    }
	    configThread = new ConfigThread() {
	        @Override
            public void run() {
	            if(old!=null) try {
	                old.join();
	            } catch(InterruptedException ie) {
	            }
	    	    if(!shouldStop) waitTillUnblocked();
	    	    if(!shouldStop) {
		    	    cl.show(panel, EMPTY);
		    	    target = null;
		    	    card = null;
		    	    adjustScrollBar(0);
		    	    firePanelChanged(null);
	    	    }
	        }
	    };
	    configThread.start();
	}
    
    public void showPanel(final Editable t) {	    
	    grabFocus(); // necessary to trigger focusLost() for EditorComponents
	    final ConfigThread old;
	    if(configThread!=null) {
	        old = configThread;
	        old.requestStop();
	    } else {
	        old = null;
	    }
	    configThread = new ConfigThread() {
	        @Override
            public void run() {
	            if(old!=null) try {
	                old.join();
	            } catch(InterruptedException ie) {
	            }
	    	    if(!shouldStop) waitTillUnblocked();
	    	    if(!shouldStop) {
	    	    	Editable delegate = t.getEditorDelegate(topic);
	    	    	target = delegate!=null? delegate: t;
		    	    String name = target.getClass().getName();	    
		    	    
		    	    card = cards.get(name);
		    	    
		    	    if(card==null) {
		                card = addNewPanel(target.getInfo(topic), name);
		                panel.setPreferredSize(panel.getMinimumSize());
		    	    }
		    	    EntryInfo[] info = card.info;
		    	    if(info!=null) {
		            	try {
			        	    cl.show(panel, name); // the JVM sometimes freezes at this point; 
			        	    		// we unfreeze it by eliciting an Error.
		            	} 
		            	catch(Error e) {
		            		//System.err.println("interrupt");
		            		/*  We interrupted an attempt to acquire a read lock. That means that there may be a problem
		            		 *  with the layout of the editor pane, but that should be all.
		            		 */
		            	}
		        	    configure(info, EditorPane.this.target);
		        	    
		        	    if(horizontalScrollBar!=null) { // scroll to the right, where the Spinner buttons are.
		        	        horizontalScrollBar.setValue(horizontalScrollBar.getMaximum());
		        	    }
		        	    adjustScrollBar(card.scrollBarValue);
		        	    
		        	    firePanelChanged(target.getInfo(topic));
		    	    }
		    	    else {
		    	        showEmptyPanel();
		    	    }
	    	    }
	        }
	    };
	    configThread.start();
    	while(configThread.isAlive()) {
		    try {
	            configThread.join(150); 
	            if(configThread.isAlive()) {
            		configThread.interrupt(); // causes an Error to be thrown.
	            }
	        } catch(InterruptedException ie) {
	        }
    	}
	}
    
    /**
     * Just a convenience-method for calling update(Editable, Method, Method, boolean, Object[], boolean), which 
     * passes <code>false</code> as the last parameter.
     */
    private void update(Editable target, EditorEntry ee, EntryInfo ei, Object[] params) {
    	update(target, ei.requestMethod, ei.writeMethod, ee.isGlobal(), params, false);
	}
    
    private void update(Editable target, Method requestMethod, Method writeMethod, boolean global, 
    		Object[] params, boolean major) {
    	
    	Class<?> oc = writeMethod.getDeclaringClass();
        EditEvent ee = new EditEvent(this, major);
    	fireEditing(ee);
    	if(global) {
    		for(Editable e: target.getGlobalSet(topic)) {
    			Class<?> cl = e.getClass();
    			if(oc.isAssignableFrom(cl)) {
        			try {
	    				if(requestMethod==null || (Boolean)requestMethod.invoke(e, (Object[])null)) {
	    					writeMethod.invoke(e, params);
	    				}
        			} catch(InvocationTargetException ite) {
        				fireEditFailed(ee);
        				throw new Error(ite);
        			} catch(IllegalAccessException iae) {
        				fireEditFailed(ee);
        				throw new Error(iae);
        			}
    			}
    		} 
    	} else {
    		try {
	            writeMethod.invoke(target, params);
            } catch(InvocationTargetException ite) {
				fireEditFailed(ee);
				throw new Error(ite);
			} catch(IllegalAccessException iae) {
				fireEditFailed(ee);
				throw new Error(iae);
			}
    	    catch(IllegalArgumentException iae) {
			    fireEditFailed(ee);
			    throw new Error(iae);
    	    }
		}
    }
    
    private void updateElement(Editable target, EditorEntry ee, EntryInfo ei) {
        if(target==null) return;
        
	    boolean enableComponent = elementChangeable(target, ei);
	    boolean b = (enableComponent || ee.isVisibleWhenDisabled()) && ee.isPotentiallyVisible();
	    for (JComponent element : ei.components) {
	        element.setVisible(b);
	    }
	    
	    JComponent comp = ei.components[0];
	    Method m = ei.readMethod;
	    switch(ee.getType()) {
        	case BOOLEAN: {	        	    
        	    JCheckBox box = (JCheckBox)comp;
        	    box.setEnabled(enableComponent);
        	    boolean select = false;
        	    try {
        	        select = ((Boolean)m.invoke(target, (Object[])null)).booleanValue();
        	    }
        	    catch(Exception e) {
        	        e.printStackTrace(System.err);
        	    }
        	    box.setSelected(select);
        	    
        	    break;
        	}
        	case TOGGLE: {	        	    
        	    JToggleButton button = (JToggleButton)comp;
        	    button.setEnabled(enableComponent);
        	    boolean select = false;
        	    try {
        	        select = ((Boolean)m.invoke(target, (Object[])null)).booleanValue();
        	    }
        	    catch(Exception e) {
        	        e.printStackTrace(System.err);
        	    }
        	    button.setSelected(select);
        	    
        	    break;
        	}
        	case INTEGER: {
        	    JSpinner spinner = (JSpinner)comp;
        	    Object value = null;
        	    spinner.setEnabled(enableComponent);
        	    try {
        	        value = m.invoke(target, (Object[])null);
        	    }
        	    catch(Exception e) {
        	        e.printStackTrace(System.err);
        	    }
        	    spinner.setValue(value);	        	    
        	    break;
        	}
        	case FLOAT: {
        	    JSpinner spinner = (JSpinner)comp;
        	    Object value = null;
        	    spinner.setEnabled(enableComponent);
        	    try {
        	        value = m.invoke(target, (Object[])null);
        	    }
        	    catch(Exception e) {
        	        e.printStackTrace(System.err);
        	    }
        	    spinner.setValue(value);	        	    
        	    break;
        	}
        	case STRING: 
        	case TEXT: {
        	    JTextComponent c = (JTextComponent)comp;
        	    String text = null;
        	    comp.setEnabled(enableComponent);
        	    try {
        	        text = (String)m.invoke(target, (Object[])null);
        	    }
        	    catch(Exception e) {
        	        e.printStackTrace(System.err);
        	    }
        	    c.setText(text);
        	    break;
        	}
        	case ACTION: {
        	    JButton button = (JButton)comp;
        	    enableComponent = true;
	            Object state = null;
        	    if(m!=null) {
		            try {
	        	        state = m.invoke(target, (Object[])null);
		            } 
		            catch(Exception e) {
	        	        e.printStackTrace(System.err);
		            }
        	    }
	            button.setText(ee.getLabelString(state));
	            try {
                    Method m1 = target.getClass().getMethod("can"+ee.getMethodString(state), (Class[])null);
                    enableComponent = ((Boolean)m1.invoke(target, (Object[])null)).booleanValue();
                }
                catch(NoSuchMethodException e) {}
                catch(Exception e) {
                    e.printStackTrace(System.err);
                }
        	    button.setEnabled(enableComponent);
        	    button.setVisible(enableComponent || ee.isVisibleWhenDisabled());
        	    break;
        	}
        	case LABEL: {
        	    JLabel label = (JLabel)comp;
        	    String info = null;
        	    try {
        	        info = (String)m.invoke(target, (Object[])null);
	            } 
	            catch(Exception e) {
        	        e.printStackTrace(System.err);
	            }
	            label.setText(info);
	            break;
        	}
        	case CHOICE: {
        	    JComboBox box = (JComboBox)comp;
        	    Object item = null;
        	    try {
        	        item = m.invoke(target, (Object[])null);
	            } 
	            catch(Exception e) {
        	        e.printStackTrace(System.err);
	            }
	            box.setSelectedItem(item);
	            break;
        	}
        	default: {
        	}
        }
    }
    
    /**
	 * Blocks the current Thread until this EditorPane is 'unblocked', or until a certain amount of time (500ms) has elapsed.
	 * (The latter condition is a precaution against the potentially disastrous effects of failures by the objects responsible
	 * to 'unblock' the EditorPane.  
	 */
	public void waitTillUnblocked() {
	    for(int i = 0; configBlocked && i<100; i++) {
	        try {
	            grabFocus(); // should trigger focusLost() for EditorComponents
	            Thread.sleep(5);
	        } catch(Exception e) {
	            configBlocked = false;
	        }
	    }
	}
}