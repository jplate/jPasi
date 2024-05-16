package util.swing;

import java.awt.Dimension;

import javax.swing.JTextField;

/**
 * @author Jan Plate
 */
public class TextField extends JTextField {

    private static final long serialVersionUID = 1412262347117934875L;
	Dimension preferredSize;
    
    public TextField(String text, Dimension size) {
        super(text);
        this.preferredSize = size;
        setPreferredSize(size);
    }

}