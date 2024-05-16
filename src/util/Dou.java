package util;

/**
 * Realises a wrapping for <code>double<code>s that is in some ways more convenient than the <code>Double</code>-class
 * (for example, to increment a <code>Double</code>-Object <code>d</code>, you can simply say <code>d.val++</code>).
 */


public class  Dou  extends Num {


public double  val;

public Dou () {}

public  Dou  ( double  v)	{	val = v;	}

public double getVal() {return val;}

public int hashCode ()	{	return (int)val;	}

public boolean equals (Object o)	{
  return o instanceof Dou && ((Dou)o).val==val;
}

public String toString ()	{	return String.valueOf(val);	}

}
