package util.swing;

import javax.swing.JSpinner;

public class FloatSpinner extends JSpinner {
	
	private static final long serialVersionUID = 5666098632546599151L;

	public FloatSpinner(float val, float min, float max, float increment) {
		super(new javax.swing.SpinnerNumberModel(Float.valueOf(val), Float.valueOf(min), Float.valueOf(max), Float.valueOf(increment)));
	}

	public float floatValue() {
		return ((Float)getValue()).floatValue();
	}
    
}
