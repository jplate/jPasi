package util;

import java.util.*;


/**
 * A utility class containing static methods for handling Vectors
 */
public class Vectors {

      @SuppressWarnings("unchecked")
    public static <T> Vector<T> sort (Vector<T> v, Comparator<T> c) {
        shuttlesort((Vector)v.clone(), v, c, 0, v.size());
        return v;
      }

      public static <T> void shuttlesort (Vector<T> from, Vector<T> to, Comparator<T> c, int low, int high) {
        if (high - low < 2) return;

        int middle = (low + high)/2;
        shuttlesort(to, from, c, low, middle);
        shuttlesort(to, from, c, middle, high);

        int p = low;
        int q = middle;

        /* This is an optional short-cut; at each recursive call,
        check to see if the elements in this subset are already
        ordered.  If so, no further comparisons are needed; the
        sub-Vector can just be copied.  The Vector must be copied rather
        than assigned otherwise sister calls in the recursion might
        get out of sinc.  When the number of elements is three they
        are partitioned so that the first set, [low, mid), has one
        element and and the second, [mid, high), has two. We skip the
        optimisation when the number of elements is three or less as
        the first compare in the normal merge will produce the same
        sequence of steps. This optimisation seems to be worthwhile
        for partially ordered lists but some analysis is needed to
        find out how the performance drops to Nlog(N) as the initial
        order diminishes - it may drop very quickly.  */

        if (high - low >= 4 && c.compare(from.get(middle-1), from.get(middle)) <= 0) {
            for (int i = low; i < high; i++) {
                to.set(i, from.get(i));
            }
            return;
        }

        // A normal merge.

        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && c.compare(from.get(p), from.get(q)) <= 0)) {
                to.set(i, from.get(p++));
            }
            else {
                to.set(i, from.get(q++));
            }
        }
    }

    public void swap(Vector<Object> v, int i, int j) {
        Object tmp = v.get(i);
        v.set(i, v.get(j));
        v.set(j, tmp);
    }

}
