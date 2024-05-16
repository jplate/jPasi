/*
 * Created on 21.06.2007
 *
 */
package jPasi.codec;


import jPasi.Canvas;

public interface Codec {
	
	public static final String VERSION_PREFIX = "pasi_codec_version_";
	
	public void load(Canvas canvas, String s, boolean replace) throws ParseException;
	
	public String getCode(Canvas canvas);
	
}
