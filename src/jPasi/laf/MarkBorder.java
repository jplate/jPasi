/*
 * Created on 06.11.2006
 *
 */
package jPasi.laf;

import javax.swing.border.AbstractBorder;

import jPasi.item.Item;

import java.awt.*;
import java.awt.geom.*;

import util.swing.TextTools;

/**
 * @author Jan Plate
 *
 */
public class MarkBorder extends AbstractBorder {

    private static final long serialVersionUID = -6048807702536441643L;

    public Color color = null;
	public Color contrastColor = null;
	public Paint titleColor = null;
	public String title = null;
	public BasicStroke stroke = new BasicStroke();
	public Font font = new Font(null, Font.PLAIN, 9);

	public MarkBorder(Color p, Color cp) {
		color = p;
		contrastColor = cp;
	}
	
	public MarkBorder(Color p, Color cp, String s) {
	    this(p, cp, p, s);
	}

	public MarkBorder(Color p, Color cp, Paint tp, String s) {
		color = p;
		contrastColor = cp;
		titleColor = tp;
		this.title = s;
	}

	public void paintBorder2D(Component comp, Graphics g, float x0, float y0, float w0, float h0) {
	    Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);	    
		g2.setStroke(stroke);
		Paint p = g2.getPaint();
		
		// convert values to interior rectangle
		float x = x0+1;
		float y = y0+1;
		float w = w0-2;
		float h = h0-2;
		
		float l = Math.min(Math.max(w/5, 5), 25);
		float m = 0f;
		if(comp instanceof Item && ((Item)comp).isHidden()) {
			m = (int)(.9*l);
		}

		GeneralPath gp0 = new GeneralPath();
		gp0.moveTo(x,y+l);
		gp0.lineTo(x+m,y+m); // corner #1
		gp0.lineTo(x+l, y);
		gp0.moveTo(x+w-l, y);
		gp0.lineTo(x+w-m, y+m); // corner #2
		gp0.lineTo(x+w, y+l);
		gp0.moveTo(x+w, y+h-l);
		gp0.lineTo(x+w-m, y+h-m); // corner #3
		gp0.lineTo(x+w-l, y+h);
		gp0.moveTo(x+l, y+h);
		gp0.lineTo(x+m, y+h-m); // corner #4
		gp0.lineTo(x, y+h-l);

		if(comp instanceof Item && ((Item)comp).isHidden()) {
			l-=1; m-=1;
		}
		else {
			x-=1; y-=1; l+=1; w+=2; h+=2;
		}
		GeneralPath gp1 = new GeneralPath();
		gp1.moveTo(x,y+l);
		gp1.lineTo(x+m,y+m);
		gp1.lineTo(x+l, y);
		gp1.moveTo(x+w-l, y);
		gp1.lineTo(x+w-m, y+m);
		gp1.lineTo(x+w, y+l);
		gp1.moveTo(x+w, y+h-l);
		gp1.lineTo(x+w-m, y+h-m);
		gp1.lineTo(x+w-l, y+h);
		gp1.moveTo(x+l, y+h);
		gp1.lineTo(x+m, y+h-m);
		gp1.lineTo(x, y+h-l);
		
		g2.setPaint(color);
		g2.draw(gp0);
		g2.setPaint(contrastColor);
		g2.draw(gp1);
		
		if(title!=null && title.length()>0) {
			g2.setPaint(titleColor);
			Font f = g2.getFont();
			g2.setFont(font);
			float sw = (float)TextTools.stringWidth(title, font, g2.getFontRenderContext());
			g2.drawString(title, x0+(w0-sw)/2, y0+getTitleHeight());
			g2.setFont(f);		
		}
		
		g2.setPaint(p);
	}
	
	public int getTitleHeight() {
		return font.getSize();
	}
	
	@Override
    public Insets getBorderInsets(Component c) {
		return new Insets(2,2,2,2);
	}
	
	public boolean isOpaque() {
		return false;
	}



}
