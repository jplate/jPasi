package util.swing;

/**
 * @author Jan Plate
 *
 */
public class SpinnerNumberModel extends javax.swing.SpinnerNumberModel {

    private static final long serialVersionUID = 3309899396149427170L;
	
    protected float min;
    protected float max;
    protected boolean cyclic;
    protected IncrementOverride incrementOverride;
    
    public static class IncrementOverride {
    	protected boolean enabled = false;
    	protected float increment = 0;
    	
		public IncrementOverride(float increment) {
			this.increment = increment;
        }
		public boolean isEnabled() {
        	return enabled;
        }
		public void setEnabled(boolean enabled) {
        	this.enabled = enabled;
        }
		public float getIncrement() {
        	return increment;
        }
		public void setIncrement(float increment) {
        	this.increment = increment;
        }    	
    }
    
    /**
     * incrementOverride may be null.
     */
    public SpinnerNumberModel(int val, int min, int max, int incr, boolean cyclic, IncrementOverride incrementOverride) {
        super(Integer.valueOf(val), Integer.valueOf(min), Integer.valueOf(max), Integer.valueOf(incr));
        this.min = min;
        this.max = max;
        this.cyclic = cyclic;
        this.incrementOverride = incrementOverride;
    }

    public SpinnerNumberModel(float val, float min, float max, float incr, boolean cyclic, IncrementOverride incrementOverride) {
        super(Float.valueOf(val), Float.valueOf(min), Float.valueOf(max), Float.valueOf(incr));
        this.min = min;
        this.max = max;
        this.cyclic = cyclic;
        this.incrementOverride = incrementOverride;
    }
    
    public SpinnerNumberModel(Integer val, Integer min, Integer max, Integer incr, boolean cyclic, IncrementOverride incrementOverride) {
        super(val, min, max, incr);
        this.min = min.floatValue();
        this.max = max.floatValue();
        this.cyclic = cyclic;
        this.incrementOverride = incrementOverride;
    }

    public SpinnerNumberModel(Float val, Float min, Float max, Float incr, boolean cyclic, IncrementOverride incrementOverride) {
        super(val, min, max, incr);
        this.min = min.floatValue();
        this.max = max.floatValue();
        this.cyclic = cyclic;
        this.incrementOverride = incrementOverride;
    }

    public SpinnerNumberModel() {
        super();
    }
    
    @Override
    public Object getNextValue() {
        float val = getNumber().floatValue();
        float increment = computeIncrement();        
        float result = val+increment>max? (cyclic? min+val+increment-max: max): val+increment;
        if(getNumber() instanceof Float) {
            return Float.valueOf(result);
        }
        else {
            return Integer.valueOf((int)result);
        }
    }

    @Override
    public Object getPreviousValue() {
        float val = getNumber().floatValue();
        float increment = computeIncrement();
        float result = val-increment<min? (cyclic? max-(min-(val-increment)): min): val-increment;
        if(getNumber() instanceof Float) {
            return Float.valueOf(result);
        }
        else {
            return Integer.valueOf((int)result);
        }
    }
    
    private float computeIncrement() {
    	float increment = incrementOverride!=null && incrementOverride.isEnabled()? 
    			incrementOverride.getIncrement(): getStepSize().floatValue();
        if(!Float.isInfinite(max) && !Float.isNaN(max) && !Float.isInfinite(min) && !Float.isNaN(min)) {
            increment = increment - (int)(increment/(max-min))*(max-min);        
        }    	
        return increment;
    }
    
    /**
     * Overridden to avoid unnecessary state changes.
     */
    @Override
    public void setValue(Object val) {
        Object obj = getValue();
        if(!obj.equals(val)) {
            super.setValue(val);
        }
    }
    
    public boolean isCyclic() {
        return cyclic;
    }
    public void cyclic(boolean cyclic) {
        this.cyclic = cyclic;
    }
}
