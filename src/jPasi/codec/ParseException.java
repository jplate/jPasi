/*
 * Created on 10.03.2007
 *
 */
package jPasi.codec;

/**
 * @author Jan Plate
 *
 */
public class ParseException extends Exception {

    private static final long serialVersionUID = 2440111089261296192L;

    public String text;
    public CodePortion portion;
    
    public ParseException() {
        super();
    }
    public ParseException(String message) {
        super(message);
    }
    
    public ParseException(CodePortion portion, String message) {
        this(portion, message, null);
    }
    
    public ParseException(CodePortion portion, String message, Throwable cause) {
        super(message, cause);
        this.portion = portion;
    }
    
    public String getCausingText() {
        return text;
    }
    public void setCausingText(String text) {
        this.text = text;
    }

}
