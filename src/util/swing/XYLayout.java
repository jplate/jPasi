package util.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Hashtable;

public class XYLayout implements LayoutManager2, Serializable {
    
    static final XYConstraints defaultConstraints = new XYConstraints();
    
    private static final long serialVersionUID = 200L;
    int height;
    Hashtable<Component, Object> info;
    int width;
    boolean xInverted = false; // if true, reference point of component is a right corner
    boolean yInverted = false; // if true, reference point of component is a bottom corner
    
    public XYLayout() {
        info = new Hashtable<Component, Object>();
    }
    
    public XYLayout(boolean xInv, boolean yInv) {
    	info = new Hashtable<Component, Object>();
    	this.xInverted = xInv;
    	this.yInverted = yInv;
    }

    public XYLayout(int width, int height) {
        info = new Hashtable<Component, Object>();
        this.width = width;
        this.height = height;
    }

    public void addLayoutComponent(Component component, Object constraints) {
        if (constraints instanceof XYConstraints) {
            info.put(component, constraints);
        }
    }

    public void addLayoutComponent(String s, Component component1) {}

    Rectangle2D.Float getComponentBounds(Component component, boolean doPreferred) {
        XYConstraints constraints = (XYConstraints) info.get(component);
        
        if (constraints == null) {
            constraints = defaultConstraints;
        }
        Rectangle2D.Float r = new Rectangle2D.Float(constraints.x, constraints.y,
                constraints.width, constraints.height);

        if (r.width <= 0 || r.height <= 0) {
            Dimension d = doPreferred? component.getPreferredSize(): component.getMinimumSize();

            if (r.width <= 0) {
                r.width = d.width;
            }
            if (r.height <= 0) {
                r.height = d.height;
            }
        }
        
        if(xInverted) {
        	r.x = width - r.width - r.x;
        }
        if(yInverted) {
        	r.y = height - r.height - r.y;
        }
        
        return r;
    }

    public int getHeight() {
        return height;
    }

    public float getLayoutAlignmentX(Container target) {
        return 0.5F;
    }

    public float getLayoutAlignmentY(Container target) {
        return 0.5F;
    }

    Dimension getLayoutSize(Container target, boolean doPreferred) {
        Dimension dim = new Dimension(0, 0);

        if (width <= 0 || height <= 0) {
            int count = target.getComponentCount();

            for (int i = 0; i < count; i++) {
                Component component = target.getComponent(i);

                if (component.isVisible()) {
                    Rectangle2D.Float r = getComponentBounds(component, doPreferred);

                    dim.width = (int)Math.max(dim.width, Math.ceil(r.x + r.width));
                    dim.height = (int)Math.max(dim.height, Math.ceil(r.y + r.height));
                }
            }
        }
        if (width > 0) {
            dim.width = width;
        }
        if (height > 0) {
            dim.height = height;
        }
        Insets insets = target.getInsets();

        dim.width += insets.left + insets.right;
        dim.height += insets.top + insets.bottom;
        return dim;
    }

    public int getWidth() {
        return width;
    }

    public void invalidateLayout(Container container) {}

    public void layoutContainer(Container target) {
        Insets insets = target.getInsets();
        int count = target.getComponentCount();

        for (int i = 0; i < count; i++) {
            Component component = target.getComponent(i);
            if (component.isVisible()) {
                Rectangle2D.Float r = getComponentBounds(component, true);
                component.setBounds(insets.left + (int)Math.round(r.x), insets.top + (int)Math.round(r.y), 
                		(int)Math.round(r.width), (int)Math.round(r.height));                
            }
        }
    }

    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(0x7fffffff, 0x7fffffff);
    }

    public Dimension minimumLayoutSize(Container target) {
        return getLayoutSize(target, false);
    }

    public Dimension preferredLayoutSize(Container target) {
        return getLayoutSize(target, true);
    }

    public void removeLayoutComponent(Component component) {
        info.remove(component);
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String toString() {
        return String.valueOf(String.valueOf((new StringBuffer("XYLayout[width=")).append(width).
                append(",height=").append(height).append("]")));
    }
}
