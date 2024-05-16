package util;

import java.awt.geom.Dimension2D;


public class DimensionF extends Dimension2D {

	public float width = 0f;
	public float height = 0f;

	public DimensionF() {
		super();
	}

	public DimensionF(float w, float h) {
		width = w;
		height = h;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.geom.Dimension2D#getWidth()
	 */
	public double getWidth() {
		return width;
	}

	/* (non-Javadoc)
	 * @see java.awt.geom.Dimension2D#getHeight()
	 */
	public double getHeight() {
		return height;
	}

	/* (non-Javadoc)
	 * @see java.awt.geom.Dimension2D#setSize(double, double)
	 */
	public void setSize(double w, double h) {
		width = (float)w;
		height = (float)h;
	}

}
