/*
 * Created on 23.05.2007
 *
 */
package util;

import java.util.Map;

/**
 * @author Jan Plate
 *
 */
public interface MapCloneable extends java.lang.Cloneable {

    /**
     * Clones <code>this</code> object using the supplied map to find clones of constituents (typically with the aim of making the
     * corresponding constituents of the clone identical with those clones). The resultant clone should then itself be stored in
     * the map as the value corresponding to <code>this</code> object. 
     */
    public Object clone(Map<Object, Object> map) throws CloneNotSupportedException ;
    
}
