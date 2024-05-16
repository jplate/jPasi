package util;

/**
 * Realises a wrapping for floats that is more convenient than the Float-class
 * (for example, to increment a Flo-Object f, you can simply say f.val++).
 *
 * @author  Jan Plate
 */


public class  Flo  extends Num {


public  float  val;

public Flo () {}

public  Flo  ( float  v)	{	val = v;	}

public double getVal() {return val;}

public int hashCode ()	{	return (int)val;	}

public boolean equals (Object o)	{
  return o instanceof Flo && ((Flo)o).val==val;
}

public String toString ()	{	return String.valueOf(val);	}

}
