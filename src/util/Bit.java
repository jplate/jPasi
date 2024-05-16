package util;

/**
 * A wrapping for <code>boolean</code>s that is in some ways more convenient than the <code>Boolean</code>-class.
 * E.g., you can just say <code>orientation.val && c.val</code> if <code>orientation</code> and <code>c</code> are <code>Bit</code>s instead of
 * <p><code>orientation.booleanValue() && c.booleanValue()</code>,<p> which would be necessary if they were
 * <code>Boolean</code>s.
 *
 * @author  Jan Plate
 */

public class Bit extends Primitive {

	public boolean val = false;

	public Bit () {}

	public Bit (boolean v) {
		val = v;
	}

	public String toString() {
		return String.valueOf(val);
	}

	public int hashCode () {
		return val? 1: 0;
	}

	public boolean equals (Object o) {
		return o instanceof Bit && ((Bit)o).val==val;
	}
}
