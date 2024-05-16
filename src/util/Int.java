package util;

/**
 * Realises a wrapping for <code>int</code>s that is more convenient than the <code>Integer</code>-class.
 * E.g., to increment an <code>Int</code>-Object <code>i</code>, you can simply say <code>i.val++</code>.
 *
 * @author  Jan Plate
 */


public class  Int  extends Num {


public  int  val = 0;


public Int () {}

public  Int  ( int  v)	{	val = v;	}

public double getVal() {return val;}

public int hashCode ()	{	return val;	}

public boolean equals (Num n)	{
  return n instanceof Int && ((Int)n).val==val;
}

public String toString ()	{	return String.valueOf(val);	}

}
