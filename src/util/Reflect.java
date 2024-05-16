/*
 * Created on 23.05.2007
 *
 */
package util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * @author Jan Plate
 *
 */
public class Reflect {

    /**
     * Convenience front-end for the non-deep cloning of an array, using the class of the supplied array as
     * indicator for the array's 'leaf type'.
     */
    public static Object arrayClone(Object o) 
			throws IllegalAccessException, InvocationTargetException {
        return arrayClone(o, getLeafType(o.getClass()), null, false);
    }
    
    /**
     * Clones the array specified as the first parameter, whose 'leaf type' is supposed to be specified by the 
     * second parameter. The map provided as third parameter serves for mapcloning, and the boolean parameter indicates 
     * whether the components of the array should be (map)cloned as well. If so, they will be mapcloned if and only if the 
     * interface MapCloneable is assignable from the 'leaf type' specified as second parameter. Otherwise they 
     * will be cloned just in case the interface Cloneable is assignable from that type.
     */
    public static Object arrayClone(Object o, Class<?> leafType, Map<Object, Object> map, boolean cloneLeaves) 
    		throws IllegalAccessException, InvocationTargetException {
        
        if(o==null) {
            return null;
        }
        Class<?> ocl = o.getClass();
        if(!ocl.isArray()) {
            throw new IllegalArgumentException("argument should be an array");
        }
        Method cm = null;
        boolean mapclone = false;
        if(cloneLeaves) {
	        try {
		        if(MapCloneable.class.isAssignableFrom(leafType)) {
		            mapclone = true;
		            cm = leafType.getMethod("clone", new Class[] {Map.class});
		        }
		        else if(Cloneable.class.isAssignableFrom(leafType)) {
		            Method m = leafType.getMethod("clone", (Class[])null);
		            cm = Modifier.isPublic(m.getModifiers())? m: null;
		        }
	        } catch(Exception e) { // shouldn't happen
	            e.printStackTrace(System.err);
	        }
        }
        return arrayClone(o, leafType, cm, mapclone? new Object[] {map}: null, map);
    }
    
    public static Object arrayClone(Object o, Class<?> leafType, Method cloneMethod, Object[] cloneParams, Map<Object, Object> map) 
    		throws IllegalAccessException, InvocationTargetException {
        
        int n = Array.getLength(o);
        Class<?> cc1 = o.getClass().getComponentType();
        Object clone = Array.newInstance(cc1, n);
        for(int i = 0; i<n; i++) {
            Object e = Array.get(o, i);
            Object ec = null;            
            if(e!=null) {
	            Class<?> cl = e.getClass();
	            if(ec==null) { 
		            if(cl.isArray() && getLeafType(cl).equals(leafType)) {
		                ec = arrayClone(e, leafType, cloneMethod, cloneParams, map);
		            } else if(cloneMethod!=null && leafType.isAssignableFrom(cl)) {
		                ec = cloneMethod.invoke(e, cloneParams);
		            } else {
		                ec = e;
		            }
		            if(map!=null && e!=ec) {
		                map.put(e, ec);
		            }
	            }
            }
            Array.set(clone, i, ec);
        }
        return clone;
    }
    
    public static Class<?> getLeafType(Class<?> c) {
        if(!c.isArray()) {
            return c;
        } else {
            return getLeafType(c.getComponentType());
        }
    }

    
    public static Class<?> getOriginallyDeclaringClass(String name, Class<?>[] params, Class<?> c0) {
	    Class<?> sc = c0.getSuperclass();
	    Class<?> c1 = null;
	    if(sc!=null) try {
	    	if(sc.getMethod(name, params)!=null) {
	    		c1 = sc;
	    	}
	    } catch(NoSuchMethodException nsme) {	    		
    	}
	    if(c1==null) {
	    	Class<?>[] interfaces = c0.getInterfaces();
		    for(Class<?> c: interfaces) {
		    	try {
			    	if(c.getMethod(name, params)!=null) {
			    		c1 = c;
			    		break;
			    	}
		    	} catch(NoSuchMethodException nsme) {	    		
		    	}	    	
		    }
	    }
	    if(c1==null) {
	    	return c0;
	    } else {
	    	return getOriginallyDeclaringClass(name, params, c1);
	    }
    }
    
	public static Class<?> getOriginallyDeclaringClass(Method m) {
	    return getOriginallyDeclaringClass(m.getName(), m.getParameterTypes(), m.getDeclaringClass());
    }

}
