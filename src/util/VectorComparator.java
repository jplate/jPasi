package util;

import java.util.Vector;

public class VectorComparator extends ObjectComparator {

	int column = 0;

	public VectorComparator (int col, boolean ascending) {
		this.column = col;
		this.ascending = ascending? 1: -1;
	}

	public int compare (Object v1, Object v2) {
		if(!(v1 instanceof Vector && v2 instanceof Vector))
			throw new IllegalArgumentException("Objects to be " +
			 "compared by VectorComparator.compare(Object, Object) must be Vectors.");
		return super.compare(((Vector<?>)v1).get(column), ((Vector<?>)v2).get(column));
	}

  public boolean equals (Object v1, Object v2) {
   	return compare(v1, v2)==0;
  }

}