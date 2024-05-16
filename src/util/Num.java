package util;

/**
 * An extension of <tt>Primitive</tt> to support simple wrappings of primitive number types
 * like <tt>int</tt>, <tt>float</tt>, <tt>long</tt> and so on.
 * @author  Jan Plate
 */

public abstract class Num extends Primitive {

  public abstract double getVal();

  public int compareTo (Num n) {
    double v = getVal(), nv = n.getVal();
    return v<nv? -1: v==nv? 0: 1;
  }
}

