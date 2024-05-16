/*
 * Created on 18.01.2007
 *
 */
package jPasi.edit;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;


/**
 * @author Jan Plate
 *
 * TODO: Delegation is not yet fully implemented, because it turned out not to be needed.
 */
public class EditorEntry {
    
    public static interface Notifier {
        public void run(Editable target);
    }

    public static enum Type {
    	RESET(null),
    	LABEL(null),
    	BOOLEAN(Boolean.TYPE),
    	TOGGLE(Boolean.TYPE),
    	INTEGER(Integer.TYPE),
    	FLOAT(Float.TYPE),
    	STRING(String.class),
    	TEXT(String.class),
    	ACTION(null),
    	CHOICE(Object.class);
    	
    	Class<?> valueType;
    	Type(Class<?> cl) {
    		valueType = cl;
    	}
    }   
    
    public String access;
    
    protected Type type;  
    protected Notifier notifier;
    protected Object defaultVal;
    protected Number increment;
    protected Vector<Object> items;
    protected String label;
    protected Map<Object, String[]> map;
    protected Comparable<?> max; 
    protected Comparable<?> min; 
    
    /**
     * The component created and used by an EditorPane for editing. If this EditorEntry is used by multiple
     * objects, the value will be the last editor component created.  
     */
    protected Component editorComponent;
	
    protected EditorEntry delegate;

    protected int leftSpace;
    protected int topSpace;
    protected int bottomSpace;

    protected boolean global;
    protected boolean cyclicRange;
    protected boolean defaultSet;
    protected boolean potentiallyVisible = true;
	protected boolean potentiallyEnabled = true;
    protected boolean visibleWhenDisabled = true;
    protected boolean hasReconfigurativePower;
    protected boolean hasOwnPanel;
    protected boolean needsNotifier;
    
	private boolean delegateLeftSpace = true;
	private boolean delegateTopSpace = true;
	private boolean delegateVisibleWhenDisabled = true;
	private boolean delegatePotentiallyVisible = true;
	private boolean delegatePotentiallyEnabled = true;
	private boolean delegateBottomSpace = true;
    
    public EditorEntry(String access, List<Object> list) {
    	this(access, Type.CHOICE);
        this.items = new Vector<Object>(); 
        items.addAll(list);
    }
    
    public EditorEntry(String access, Map<Object, String[]> map) {
    	this(access, Type.ACTION);
        this.map = map;
    }
    
    public EditorEntry(String access, Type type) {
        this.access = access;
        this.type = type;
    	
    }
    
    public EditorEntry(String access, Type type, String label) {
    	this(access, type);
        this.label = label;
        if(type==Type.ACTION) {
            map = new HashMap<Object, String[]>();
            map.put(null, new String[] {access, label});
        } else if(type==Type.CHOICE) {
            items = new Vector<Object>();
            items.add(label);            
        }
    }

    public EditorEntry createDelegate() {
    	EditorEntry result = new EditorEntry(access, getType(), getLabel());
    	result.setDelegate(this);
    	return result;
    }
    
    public int getBottomSpace() {
    	return delegate!=null && delegateBottomSpace? delegate.getBottomSpace(): bottomSpace;
    }
    
    public Object getDefaultValue() {
        return delegate!=null? delegate.getDefaultValue(): defaultVal;
    }

	public Component getEditorComponent() {
        return delegate!=null? delegate.getEditorComponent() : editorComponent;
    }
    public Number getIncrement() {
        return delegate!=null? delegate.getIncrement() : increment;
    }
    public Vector<?> getItems() {
        return delegate!=null? delegate.getItems() : items;
    }

    public String getLabel() {
        return delegate!=null? delegate.getLabel() : label;
    }
    
    public String getLabelString(Object state) {
    	if(delegate!=null) {
    		return delegate.getLabelString(state);
    	}
        if(map==null) throw new IllegalStateException("Type should be IIE.ACTION");
        else return (map.get(state))[1];
    }
    
    public int getLeftSpace() {
        return delegate!=null && delegateLeftSpace ? delegate.getLeftSpace(): leftSpace;
    }
    public Comparable<?> getMax() {
        return delegate!=null? delegate.getMax(): max;
    }
    public String getMethodString(Object state) {
    	if(delegate!=null) {
    		return delegate.getMethodString(state);
    	}
    	if(map==null) throw new IllegalStateException("Type should be IIE.ACTION");
        else return (map.get(state))[0];
    }
    
    public Comparable<?> getMin() {
        return delegate!=null? delegate.getMin(): min;
    }
    public Notifier getNotifier() {
        return delegate!=null? delegate.getNotifier(): notifier;
    }
    public int getTopSpace() {
        return delegate!=null && delegateTopSpace ? delegate.getTopSpace(): topSpace;
    }
    public Type getType() {
        return delegate!=null? delegate.getType(): type;
    }
    public boolean hasCyclicRange() {
        return delegate!=null? delegate.hasCyclicRange(): cyclicRange;
    }
    public boolean hasOwnPanel() {
        return delegate!=null? delegate.hasOwnPanel(): hasOwnPanel;
    }
    public boolean hasReconfigurativePower() {
        return delegate!=null? delegate.hasReconfigurativePower(): hasReconfigurativePower;
    }
    public boolean isDefaultSet() {
        return delegate!=null? delegate.isDefaultSet(): defaultSet;
    }
    public boolean isGlobal() {
    	return delegate!=null? delegate.isGlobal(): global;
    }
    public boolean isPotentiallyEnabled() {
        return delegate!=null && delegatePotentiallyEnabled ? delegate.isPotentiallyEnabled(): potentiallyEnabled;
    }
    public boolean isPotentiallyVisible() {
        return delegate!=null && delegatePotentiallyVisible ? delegate.isPotentiallyVisible(): potentiallyVisible;
    }
    public boolean isVisibleWhenDisabled() {
        return delegate!=null && delegateVisibleWhenDisabled? delegate.isVisibleWhenDisabled(): visibleWhenDisabled;
    }
    public boolean needsNotifier() {
        return delegate==null && needsNotifier;
    }
    public void notify(Editable target) {
        if(notifier!=null) {
            notifier.run(target);
        }
    }
  
    public void requestNotifier() {
        needsNotifier = true;
    }
    
    public void requestOwnPanel(boolean b) {
        this.hasOwnPanel = b;
    }
    
    public void setBottomSpace(int n) {
    	this.bottomSpace = n;
        delegateBottomSpace = false;
    }
    
    public void setDefaultValue(Object defaultVal) {
        this.defaultVal = defaultVal;
        defaultSet = true;
    }
    protected void setDelegate(EditorEntry ee) {
    	delegate = ee;
    }
    
    public void setEditorComponent(Component comp) {
        editorComponent = comp;
    }
    public void setGlobal(boolean global) {
    	this.global = global;
    }
    public void setIncrement(float val) {
        setIncrement(Float.valueOf(val));
    }
    
    public void setIncrement(int val) {
        setIncrement(Integer.valueOf(val));
    }
    
    public void setIncrement(Number val) {
        this.increment = val;
        if(editorComponent instanceof JSpinner) {
            SpinnerModel sm = ((JSpinner)editorComponent).getModel();
            if(sm instanceof SpinnerNumberModel) {
                SpinnerNumberModel snm = (SpinnerNumberModel)sm;
                snm.setStepSize(val);
            }
        }
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setLeftSpace(int sp) {
        leftSpace = sp;
        delegateLeftSpace = false;
    }
    public void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }

    public void setPotentiallyEnabled(boolean b) {
    	this.potentiallyEnabled = b;
    	delegatePotentiallyEnabled = false;
    }
    public void setPotentiallyVisible(boolean b) {
        potentiallyVisible = b;
        delegatePotentiallyVisible = false;
    }
    public void setReconfigurationAction() {
        hasReconfigurativePower = true;
    }

	public void setSpinnerValues(float val, float min, float max, float increment, boolean cyclicRange) {
        if(Float.isNaN(val)) {
            val = min;
            defaultSet = false;
        }
        else {
            defaultSet = true;
        }
        this.defaultVal = Float.valueOf(val);
        this.min = Float.valueOf(min);
        this.max = Float.valueOf(max);
        this.increment = Float.valueOf(increment);
        this.cyclicRange = cyclicRange;
    }

	public void setSpinnerValues(float val, int min, int max, int increment, boolean cyclicRange) {
        if(Float.isNaN(val)) {
            val = min;
            defaultSet = false;
        }
        else {
            defaultSet = true;
        }
        this.defaultVal = Integer.valueOf((int)val);
        this.min = Integer.valueOf(min);
        this.max = Integer.valueOf(max);
        this.increment = Integer.valueOf(increment);
        this.cyclicRange = cyclicRange;
    }

	public void setTopSpace(int sp) {
        topSpace = sp;
        delegateTopSpace = false;
    }

	public void setType(Type type) {
        this.type = type;
    }

	public void setVisibleWhenDisabled(boolean b) {
        visibleWhenDisabled = b;
        delegateVisibleWhenDisabled = false;
    }

	@Override
    public String toString() {
        return "EE["+access+", "+type+", "+label+"]";
    }
}
