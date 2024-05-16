/*
 * Created on 03.07.2007
 *
 */
package jPasi.edit;

import java.util.EventObject;

public class EditEvent extends EventObject {
	
	private static final long serialVersionUID = -5572883118087880281L;

	protected boolean major;
	
	public EditEvent(Object source) {
		super(source);
	}
	
	public EditEvent(Object source, boolean major) {
		super(source);
		this.major = major;
	}

	public boolean isMajor() {
		return major;
	}
	
}
