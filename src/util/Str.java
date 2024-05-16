package util;

/**
 * A simple wrapping for <code>String</code> objects.
 *
 * @author  Jan Plate
 */

public class Str extends Primitive {

  public String val = null;

  public Str () {}

  public Str (String s) {
    this.val = s;
  }

  public boolean equals(Object o) {
    return o instanceof Str && val.equals(((Str)o).val);
  }
  
  public String toString () {
    return val;
  }

}
