package util.swing;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JTextField;


/**
 * <p>Überschrift: </p>
 * <p>A simple class for collecting tests to be performed on the input of a component
 * (like javax.swing.InputVerifier, but different).</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * @author Jan Plate
 * @version 1.0
 */

public class InputTest {

    Vector<Verifier> tests = null;
    String title = null;

    public InputTest(String messageTitle) {
        this.tests = new Vector<Verifier>();
        this.title = messageTitle;
    }

    public void addVerifier(String message, Verifier v) {
        v.setMessage(message);
        tests.add(v);
    }

    public void addVerifier(Verifier v) {
        tests.add(v);
    }

    public String verify(JComponent comp) {
        String message = null;
        String s = ((JTextField)comp).getText();
        for (Iterator<Verifier> iter = tests.iterator(); message==null&&iter.hasNext(); ) {
            Verifier v = iter.next();
            if(!v.verify(s)) {
                message = v.getMessage();
            }
        }
        return message;
    }

} // end class InputTest

