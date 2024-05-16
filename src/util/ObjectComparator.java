package util;

import java.util.Date;
import java.util.Vector;
import java.text.CollationKey;

public class ObjectComparator implements java.util.Comparator<Object> {

  public int ascending = 1;

  public ObjectComparator () {}

  public ObjectComparator (boolean ascending) {
    this.ascending = ascending? 1: -1;
  }

  public boolean equals (Object o1, Object o2) {
   	return compare(o1,o2)==0;
  }

  int compareObjects(Object o1, Object o2) {

    // If both values are null, return 0.
    if(o1 == null && o2 == null) {
      return 0;
    } else if(o1 == null) { // Define null less than everything.
      return -ascending;
    } else if(o2 == null) {
      return ascending;
    }

    Class<?> type = o1.getClass();
    if(!type.equals(o2.getClass())) throw
    	new IllegalArgumentException("The Objects to be compared have to be of the same class.");

    if(type.getSuperclass() == java.lang.Number.class) {
      Number n1 = (Number)o1;
      double d1 = n1.doubleValue();
      Number n2 = (Number)o2;
      double d2 = n2.doubleValue();

      if(d1 < d2) {
        return -ascending;
      } else if(d1 > d2) {
        return ascending;
      } else {
        return 0;
      }
    } else if(type == Date.class) {
      Date d1 = (Date)o1;
      long n1 = d1.getTime();
      Date d2 = (Date)o2;
      long n2 = d2.getTime();

      if(n1 < n2) {
          return -1*ascending;
      } else if (n1 > n2) {
          return 1*ascending;
      } else {
          return 0;
      }
    } else if(type == String.class) {
      String s1 = (String)o1;
      String s2    = (String)o2;
      int result = s1.compareTo(s2);

      if(result < 0) {
          return -1*ascending;
      } else if(result > 0) {
          return 1*ascending;
      } else {
          return 0;
      }
    } else if(type == Boolean.class) {
      Boolean bool1 = (Boolean)o1;
      boolean b1 = bool1.booleanValue();
      Boolean bool2 = (Boolean)o2;
      boolean b2 = bool2.booleanValue();

      if(b1 == b2) {
          return 0;
      } else if(b1) { // Define false < true
          return 1*ascending;
      } else {
          return -1*ascending;
      }
    } else if(type==CollationKey.class) {
      CollationKey k1 = (CollationKey)o1;
      CollationKey k2 = (CollationKey)o2;

      return k1.compareTo(k2);

    } else if(type==java.util.Vector.class) {

      return compareVectors((Vector<?>)o1, (Vector<?>)o2, 0);

    } else {
      Object v1 = o1;
      String s1 = v1.toString();
      Object v2 = o2;
      String s2 = v2.toString();
      return (int)Math.signum(s1.compareTo(s2))*ascending;
    }
  }

	int compareVectors(Vector<?> v1, Vector<?> v2, int pos) {

    if(v1.size()==pos) {
    	if(v2.size()==pos) return 0;
      else return -ascending;
    } else if(v2.size()==pos) return ascending;

    int result = compare(v1.get(pos), v2.get(pos));
    if(result==0) {
    	return compareVectors(v1, v2, pos+1);
    } else return result;

  }

  public int compare(Object o1, Object o2) {

		return compareObjects(extractKey(o1), extractKey(o2));

  }

  public Object extractKey(Object o) {

  	return o;

  }

}