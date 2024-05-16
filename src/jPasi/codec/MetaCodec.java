/*
 * Created on 21.06.2007
 *
 */
package jPasi.codec;

import static jPasi.codec.CodePortion.*;

import java.util.*;
import java.util.regex.*;

import jPasi.Canvas;

public class MetaCodec implements Codec {

	Codec codec;
	Map<String, Codec> codecMap = findCodecs();
	
	public static Map<String, Codec> findCodecs() {
		Map<String, Codec> result = new HashMap<String, Codec>();
		result.put(Codec1.ID, new Codec1());
		return result;
	}
	
	public MetaCodec(Codec delegate) {
		this.codec = delegate;
	}
	
	public String getCode(Canvas canvas) {
		return codec.getCode(canvas);
	}

	public void load(Canvas canvas, String s, boolean replace) throws ParseException {
		Codec codec;
		Pattern p = Pattern.compile(VERSION_PREFIX+"(.*?)\\W");
		Matcher m = p.matcher(s);
		if(m.find()) {
			String vs = m.group(1);
			codec = codecMap.get(vs);
		} else {
			ParseException pe = new ParseException(HINT, "No codec version specified");
			pe.setCausingText(s);
			throw pe;
		}
		if(codec!=null) {
			codec.load(canvas, s, replace);
		} else {
			throw new ParseException(HINT, "Failure to access codec version "+m.group(1));
		}
	}
}
