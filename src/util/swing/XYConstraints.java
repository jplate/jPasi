package util.swing;


import java.io.Serializable;

public class XYConstraints
    implements Cloneable, Serializable
{

    private static final long serialVersionUID = 8077408726002648407L;
	float x;
    float y;
    float width;
    float height;

    public XYConstraints()
    {
        this(0, 0, 0, 0);
    }

    public XYConstraints(float x, float y, float width, float height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public float getY()
    {
        return y;
    }

    public void setY(float y)
    {
        this.y = y;
    }

    public float getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public float getHeight()
    {
        return height;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    @Override
    public int hashCode()
    {
        return (int)x ^ (int)y * 37 ^ (int)width * 43 ^ (int)height * 47;
    }

    @Override
    public boolean equals(Object that)
    {
        if(that instanceof XYConstraints)
        {
            XYConstraints other = (XYConstraints)that;
            return other.x == x && other.y == y && other.width == width && other.height == height;
        } else
        {
            return false;
        }
    }

    @Override
    public Object clone()
    {
        return new XYConstraints(x, y, width, height);
    }

    @Override
    public String toString()
    {
        return String.valueOf(String.valueOf((new StringBuffer("XYConstraints[")).append(x).append(",").append(y).append(",").append(width).append(",").append(height).append("]")));
    }
}
