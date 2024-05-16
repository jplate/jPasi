package util.swing;


public abstract class Verifier
{

    public String message = null;

    public Verifier() {}

    public Verifier(String m) {
        this.message = m;
    }

    public static Verifier acceptPositiveFloats()
    {
        return new Verifier()
        {
            public boolean verify( Object o )
            {
                float f = -1;
                try {
                    f = Float.parseFloat((String)o);
                }
                catch(NumberFormatException nfe) {
                }
                catch(ClassCastException cce) {
                }
                return f>=0;
            }
        };
    }

    public static Verifier acceptPositiveInts()
    {
        return new Verifier()
        {
            public boolean verify( Object o )
            {
                int i = -1;
                try {
                    i = Integer.parseInt((String)o);
                }
                catch(NumberFormatException nfe) {
                }
                catch(ClassCastException cce) {
                }
                return i>=0;
            }
        };
    }

    /**
     * Verifies if the specified object satisfies a certain criterion.
     * @param o the object
     * @return whether the specified object satisfies the criterion.
     */
    public abstract boolean verify( Object o );

    public String getMessage() {
        return message;
    }

    public void setMessage(String m) {
        this.message = m;
    }
}
