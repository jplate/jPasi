/*
 * Created on 26.02.2007
 *
 */
package jPasi.item;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

import jPasi.Canvas;

/**
 * @author Jan Plate
 *
 */
public class Point extends Item {
    
    private static final long serialVersionUID = 1182619166373339950L;

    public Point() {}
    
    public Point(Canvas c, Paint paint) {
        super(c);
        this.paint = paint;
        setBounds2D(0, 0, 8, 8);
    }

    public void paintShape(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.drawLine(4, 4, 4, 4);
    }

	public String getTexdrawCode(int canvasHeight) {
		return null;
	}
    
}
