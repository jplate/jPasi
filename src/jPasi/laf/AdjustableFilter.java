/*
 * Created on 07.02.2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jPasi.laf;

/**
 * @author Jan Plate
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface AdjustableFilter {

    public void setLinearFilter(double dy, double y0);
    public double getDY();
    public double getY0();
   
}
