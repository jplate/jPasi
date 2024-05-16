package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;


/**
 * <p>Generic configuration object. Represents a <code>HashMap</code> mapping classes to <code>HashMaps</code>, so-called 'sub-configs'.
 * If an object is created with a <code>Config</code> object as initialization parameter, the constructor should invoke
 * <code>setConfigurand()</code> using '<code>this</code>' as argument, so that the created object then is the 'configurand' of the <code>Config</code>.
 * A sub-config is considered 'relevant' for a given configurand iff it is associated
 * with the configurand's class or one of its direct or indirect superclasses. Such associations may be established
 * and verified, respectively, by using <code>prepareFor()</code> and <code>isPreparedFor()</code>.
 * <p>Cloning works as follows: if a <code>Config</code> object is cloned, it thereby copies all those of its sub-configs
 * that are relevant (in the sense just specified) for its configurand, and sets the configurand of the clone to be
 * identical with its own. The sub-configs are copied in such a way as to preserve the keys, but to clone those of
 * their values whose classes implement the <code>Cloneable</code>-interface. If, in addition, a value's class implements the
 * <code>Configurable</code>-interface, the value will also be configured (using <code>configure</code>). This configuration takes place after
 * the copying of the sub-configs, and the order of the values being configured proceeds from superclasses to
 * subclasses, thus respecting the order in which they would be configured in effect of a call to the configurand's
 * <code>configure</code>-method.
 *
 * <p>Copyright: Copyright (c) 2003</p>
 * @author J. Plate
 * @version 1.0
 */
public class Config<Type> extends HashMap<Object, Object> implements util.Cloneable
{
    /**
     * 
     */
    private static final long serialVersionUID = 524630003173925653L;
	private Type configurand = null;
    private Vector<Class<? extends Type>> classes = null;

    public Config()
    {
        super();
        classes = new Vector<Class<? extends Type>>();
    }

    @Override
    public Object get( Object key )
    {
        if ( configurand == null )
        {
            throw new IllegalStateException( "null configurand." );
        }
        Class<?> c = configurand.getClass();
        HashMap<Object, Object> map = get( c, key );
        Object result = null;
        if ( map != null )
        {
            result = map.get( key );
        }
        else
        {
            throw new IllegalArgumentException( "key not found in configuration." +
                                                "\n Class: " + c +
                                                "\n Key: " + key );
        }
        return result;
    }

    public Object set( Object key, Object value )
    {
        if ( configurand == null )
        {
            throw new IllegalStateException( "null configurand." );
        }
        Class<?> c = configurand.getClass();
        HashMap<Object, Object> map = get( c, key );
        Object oldValue = null;
        if ( map != null )
        {
            oldValue = map.put( key, value );
        }
        else
        {
            throw new IllegalArgumentException( "key not found in configuration." +
                                                "\n Class: " + c +
                                                "\n Key: " + key );
        }
        return oldValue;
    }

    /**
     * @param c a class
     * @param key   the key to be searched for
     * @return The HashMap registered for the first one of the specified class or its superclasses
     * that contains the specified key, or null if no such HashMap exists.
     */
    @SuppressWarnings("unchecked")
    protected HashMap<Object, Object> get( Class c, Object key )
    {
        HashMap<Object, Object> result = null;
        if ( c != null )
        {
            if ( containsKey( c ) )
            {
                HashMap<Object, Object> map = ( HashMap<Object, Object> )super.get( c );
                if ( map.containsKey( key ) )
                {
                    result = map;
                }
            }
            if ( result == null )
            {
                result = get( c.getSuperclass(), key );
            }
        }
        return result;
    }

    public Object getConfigurand()
    {
        return configurand;
    }

    public void setConfigurand( Type o )
    {
        this.configurand = o;
    }

    public boolean isPreparedFor( Class<Type> c )
    {
        return containsKey( c );
    }

    public void prepareFor( Class<? extends Type> c, HashMap<Object, Object> map )
    {
        super.put( c, map );

        // sort on insertion:
        boolean found = false;
        int i = 0;
        for ( Iterator<Class<? extends Type>> iter = classes.iterator(); !found && iter.hasNext(); )
        {
            Class<? extends Type> c1 = iter.next();
            if ( c.isAssignableFrom( c1 ) )
            {
                found = true;
            }
            else
            {
                i++;
            }
        }
        classes.add( i, c );
    }

    /**
     * This method is not to be used. Use set() or prepareFor() instead.
     * @param key   a key
     * @param val   a value
     * @return  nothing (throws an Exception if called)
     */
    @Override
    public Object put( Object key, Object val )
    {
        throw new RuntimeException( "Do not use Config.put(Object, Object)." +
                                    " Use set(Object, Object) or prepareFor(Class, HashMap) instead." );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone()
    {
        Config<Type> result = new Config<Type>();
        result.setConfigurand( configurand );
        Vector<Object> v = new Vector<Object>();
        for (Class<? extends Type> c : classes) {
            HashMap<Object, Object> map = ( HashMap)super.get( c );
            HashMap<Object, Object> map1 = null;

            if ( c.isAssignableFrom( configurand.getClass() ) )
            { // clone only those sub-configs that are relevant to the configurand
                map1 = new HashMap<Object, Object>();
                for (Object key : map.keySet()) {
                    Object val = map.get( key );
                    if ( val instanceof util.Cloneable )
                    {
                        val = ( ( util.Cloneable ) val ).clone();
                        if ( val instanceof Configurable )
                        {
                            v.add( val );
                        }
                    }
                    map1.put( key, val );
                }
            }
            else
            {
                map1 = map;
            }
            result.prepareFor( c, map1 );
        }

        for (Object object : v) {
            Configurable item = ( Configurable ) object;
            item.configure( result );
        }
        return result;
    }

    //***************************
     // Convenient get-methods
     //***************************

      public Object get( Object key, Object defaultVal )
      {
          Object result = get( key );
          return result == null ? defaultVal : result;
      }

    public int getInt( Object key )
    {
        return ( ( Integer ) get( key ) ).intValue();
    }

    public int getInt( Object key, int defaultVal )
    {
        Object result = get( key );
        return result == null ? defaultVal : ( ( Integer ) result ).intValue();
    }

    public float getFloat( Object key )
    {
        return ( ( Float ) get( key ) ).floatValue();
    }

    public float getFloat( Object key, float defaultVal )
    {
        Object result = get( key );
        return result == null ? defaultVal : ( ( Float ) result ).floatValue();
    }

    public double getDouble( Object key )
    {
        return ( ( Double ) get( key ) ).doubleValue();
    }

    public double getDouble( Object key, double defaultVal )
    {
        Object result = get( key );
        return result == null ? defaultVal : ( ( Double ) result ).doubleValue();
    }

    public boolean getBoolean( Object key )
    {
        return ( ( Boolean ) get( key ) ).booleanValue();
    }

    public boolean getBoolean( Object key, boolean defaultVal )
    {
        Object result = get( key );
        return result == null ? defaultVal : ( ( Boolean ) result ).booleanValue();
    }

}