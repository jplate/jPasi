package util.swing;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


/**
 * @author Jan Plate
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TextTools {

    public static void insertAttributedString(StyledDocument doc, AttributedString as) {
        try
        {
            AttributedCharacterIterator ci = as.getIterator();
            int i = 0;
            while ( ci.current() != CharacterIterator.DONE )
            {
                doc.insertString( i++, Character.valueOf( ci.current() ).toString(),
                                  getCurrentAttributeSet( ci ) );
                ci.next();
            }
        }
        catch ( BadLocationException ble )
        {
            ble.printStackTrace( System.err );
        }
    }

    private static SimpleAttributeSet getCurrentAttributeSet( AttributedCharacterIterator aci )
    {
        SimpleAttributeSet attr = new SimpleAttributeSet();

        // Font:
        Font font = ( Font ) aci.getAttribute( TextAttribute.FONT );
        if(font!=null) {
            attr.addAttribute( StyleConstants.Bold, Boolean.valueOf( font.isBold() ) );
            attr.addAttribute( StyleConstants.Italic, Boolean.valueOf( font.isItalic() ) );
            attr.addAttribute( StyleConstants.FontSize, Integer.valueOf( font.getSize() ) );
            attr.addAttribute( StyleConstants.FontFamily, font.getFamily() );
        }

        // Underline:
        Integer n = (Integer) aci.getAttribute(TextAttribute.UNDERLINE);
        if(n!=null) {
            attr.addAttribute( StyleConstants.Underline, Boolean.valueOf(true));
        }

        return attr;
    }

    public static AttributedString getAttributedString( StyledDocument doc )
    {
        String s = null;
        try
        {
            s = doc.getText( 0, doc.getLength() );
        }
        catch ( BadLocationException ble )
        {
            ble.printStackTrace( System.err );
        }
        AttributedString as = new AttributedString( s );
        int pos = 0;
        while ( pos < doc.getLength() )
        {
            AttributeSet attr = doc.getCharacterElement(pos).getAttributes();
            addAttributes( as, attr, pos, pos+1 );
            pos++;
        }
        return as;
    }

    public static void addAttributes( AttributedString as, AttributeSet attr, int pos0, int pos1 )
    {
        if(pos1>pos0) {

            // Font:
            Object o0 = attr.getAttribute( StyleConstants.Bold );
            int bold = o0!=null && ( ( Boolean ) o0).booleanValue() ? Font.BOLD : 0;
            o0 = attr.getAttribute( StyleConstants.Italic );
            int italic = o0!=null && ( ( Boolean ) o0 ).booleanValue() ? Font.ITALIC: 0;
            o0 = attr.getAttribute( StyleConstants.FontFamily );
            Object o1 = attr.getAttribute( StyleConstants.FontSize );
            Font font = new Font( o0!=null? ( String )o0: "Default",
                                  bold | italic,
                                  o1!=null? ( ( Integer ) o1 ).intValue(): 10 );
            as.addAttribute( TextAttribute.FONT, font, pos0, pos1 );

            // Underline:
            o0 = attr.getAttribute( StyleConstants.Underline );
            if(o0!=null && ( ( Boolean ) o0 ).booleanValue()) {
                as.addAttribute( TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, pos0, pos1 );
            }
        }
    }

    public static void addAttributes( AttributedString as, AttributeSet attr) {
        addAttributes(as, attr, 0, as.getIterator().getEndIndex());
    }

    public static double stringWidth( String s, Font font, FontRenderContext frc )
    {
        return new TextLayout( s, font, frc ).getBounds().getWidth();
    }

    public static double stringHeight( String s, Font font, FontRenderContext frc )
    {
        return new TextLayout( s, font, frc ).getBounds().getHeight();
    }

    public static String longestString( Object[] objects, Font font, FontRenderContext frc )
    {
        double max = 0;
        String result = null;
        for ( int i = 0; i < objects.length; i++ )
        {
            if ( objects[i] != null )
            {
                String s = objects[i].toString();
                double w = 0.;
                try
                {
                    w = ( new TextLayout( s, font, frc ) ).getBounds().getWidth();
                }
                catch ( Exception e )
                {
                }
                if ( w > max )
                {
                    max = w;
                    result = s;
                }
            }
        }
        return result;
    }

    // compute the maximal number of elements per array[i]
    public static int getArrayWidth( double[][] array )
	{
	    int retVal = 0;
	    for ( int i = 0; i < array.length; i++ )
	    {
	
	        if ( array[i].length > retVal )
	        {
	            retVal = array[i].length;
	        }
	    }
	    return retVal;
	}

	/**
	 * Sets the font of the specified Graphics2D object to a font that fits the longest element
	 * of the strings parameter into the rectangle defined by xSpace and ySpace.
	 * @param g2
	 * @param strings
	 * @param xSpace
	 * @param ySpace
	 * @return the metrics of the resulting font
	 */
	public static FontMetrics pickFont( Graphics2D g2, Object[] objects, // objects: array of objects to be represented
	                       int xSpace, int ySpace, int minFontSize )
	{
	    boolean fontFits = false;
	    Font font = g2.getFont();
	    FontMetrics fontMetrics = g2.getFontMetrics();
	    String longString = longestString( objects, font, g2.getFontRenderContext() );
	    if ( longString != null )
	    { // if longString is null, then the metrics don't matter.
	        int size = font.getSize();
	        String name = font.getName();
	        int style = font.getStyle();
	
	        while ( !fontFits )
	        {
	            if ( ( fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent() <= ySpace )
	                 && ( fontMetrics.stringWidth( longString ) <= xSpace ) )
	            {
	                fontFits = true;
	            }
	            else
	            {
	                if ( size <= minFontSize )
	                {
	                    fontFits = true;
	                }
	                else
	                {
	                    g2.setFont( font = new Font( name,
	                        style,
	                        --size ) );
	                    fontMetrics = g2.getFontMetrics();
	                }
	            }
	        }
	    }
	    return fontMetrics;
	}

    public static String trim( String s, Graphics g, int w )
    {
        FontMetrics fm = g.getFontMetrics();
        String s0 = " ";
        String r = "";
        if(w>0) {
            r = s0 + s.trim() + s0;
            if ( fm.stringWidth( r ) > w )
            {
                String s1 = "..." + s0;
                double d0 = fm.stringWidth( s1 );
                while ( fm.stringWidth( r ) + d0 > w )
                {
                    r = s.substring( 0, r.length() / 2 );
                }
                double d1 = fm.stringWidth( s.substring( r.length(), r.length() + 1 ) );
                while ( fm.stringWidth( r ) + d0 + d1 < w )
                {
                    r = s.substring( 0, r.length() + 1 );
                }
                r = s0 + r.trim() + s1;
            }
        }
        return r;
    }

}
