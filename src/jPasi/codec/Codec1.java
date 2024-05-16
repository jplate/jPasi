/*
 * Created on 11.03.2007
 *
 */
package jPasi.codec;

import static jPasi.codec.CodePortion.HINT;
import static jPasi.codec.CodePortion.TEX;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jPasi.Canvas;
import jPasi.item.Adjunction;
import jPasi.item.BinaryRelationship;
import jPasi.item.CompoundArrow;
import jPasi.item.Connector;
import jPasi.item.Containment;
import jPasi.item.Contour;
import jPasi.item.DependentItem;
import jPasi.item.ENode;
import jPasi.item.Entailment;
import jPasi.item.Identity;
import jPasi.item.Inclusion;
import jPasi.item.IndependentItem;
import jPasi.item.Item;
import jPasi.item.Label;
import jPasi.item.Negation;
import jPasi.item.Obtainment;
import jPasi.item.Order;
import jPasi.item.Ornament;
import jPasi.item.Pointer;
import jPasi.item.Predication;
import jPasi.item.Restriction;
import jPasi.item.Subsumption;
import jPasi.item.Transition;
import jPasi.item.UniversalQuantification;
import jPasi.item.group.Group;
import jPasi.item.group.GroupMember;
import jPasi.item.group.StandardGroup;

/**
 * @author Jan Plate
 *
 * An implementation of pasi.codec.Codec; doubles as a repository for convenience methods (to be called, e.g., by members of pasi.item).
 * 
 */
public class Codec1 implements Codec {
	
	public static final String ID = "1";
	
	protected static class DimensionInfo {
	    Dimension dimension;
	    Canvas.Grid grid;
	    int hDisplacement;
	    float unitScale;
	    int vDisplacement;
	    int transformModifier;
	}

	protected static class GroupInfo {
	    String groupName;
	    String[] impGroupNames;
	    boolean inGroup;
	    
	    public String toString() {
	    	StringBuilder sb = new StringBuilder("GroupInfo '"+groupName+"' "+inGroup+"; ");
	    	if(impGroupNames!=null) {
	    		for(String s: impGroupNames) sb.append("'"+s+"', ");
	    	}
	    	return sb.toString();
	    }
	}

	public static class DIInfo1 extends DIInfo {
        
        public DIInfo1(Class<? extends DependentItem> cl, float priority, String gapInfo, String info, 
                LinkedList<DIInfo> composition, boolean b) throws ParseException {
            this.cl = cl;
            this.priority = priority;
            this.info = info;
            this.composition = composition;
            inverse = b;
            
            if(gapInfo!=null) {
                String[] s = gapInfo.split("\\s+");
                try {
	                if(s.length==1) {
	                    gap1 = Float.parseFloat(Codec1.extractFloat(s[0], 0));
	                }
	                else if(s.length>1) {
	                    gap0 = Codec1.decodeFloat(Codec1.extractFloat(s[0], 0));
	                    gap1 = Codec1.decodeFloat(Codec1.extractFloat(s[1], 0));
	                }
	            } catch (NumberFormatException nfe) {
		            throw new ParseException(HINT, "Illegal number format");
	            }   	       
            }
        }
        
    }

	private static final HashMap<Character, Class<? extends DependentItem>> itemClasses = 
            new HashMap<Character, Class<? extends DependentItem>>(); 
	private static final HashMap<Class<?>, Character> itemCodes = new HashMap<Class<?>, Character>(); 
	public static final int PRECISION = 10000;
	public static final int RADIX = 26;	
	public static final char RADIX_CHAR = 'z'; // used for the encoding/decoding of Entity names

	static {
		addCode(Adjunction.class, 'A');
		addCode(Containment.class, 'C');
		addCode(Contour.class, 'K');
		addCode(Entailment.class, 'E');
		addCode(Identity.class, 'J');
		addCode(Inclusion.class, 'I');
		addCode(Subsumption.class, 'Y');
		addCode(Label.class, 'L');
		addCode(Negation.class, 'N');
		addCode(Obtainment.class, 'O');
		addCode(Order.class, 'S');
		addCode(Pointer.class, 'Z');	    
		addCode(Predication.class, 'P');
		addCode(Restriction.class, 'R');
		addCode(Transition.class, 'T');
		addCode(UniversalQuantification.class, 'U');
	}
    
    protected static void addCode(Class<? extends DependentItem> cl, char c) {
        Character ch = Character.valueOf(c);
        itemCodes.put(cl, ch);
        itemClasses.put(ch, cl);
    }
    
    /**
     * Counterpart to encode(int, char, int).
     */
    public static int decode(String s, char radixC, int radix) {
        int n = 0, m = 1;
        int l = s.length();        
        for(int i = 0; i<l; i++) {
            char c = s.charAt(l-i-1);
            if(c<=radixC-radix || c>radixC) {
                throw new IllegalArgumentException("Character out of range: "+c);
            }
            n += m*(c - radixC + radix);
            m *= radix;
        }
        return n;
    }
   
    protected static strictfp double decode(String s, Map<Character, Integer> decode, int radix, boolean lt1) throws ParseException {
		double d = 0, m = lt1? 1d/radix: 1;
        int l = s.length();        
		for(int i = 0; i<l; i++) {
        	char c = s.charAt(lt1? i: l-i-1);
            Integer k = decode.get(c);
            if(k==null) {
                throw new ParseException(HINT, "Character out of range: "+c);
            }
            d += m*k;
            m *= lt1? 1d/radix: radix;
        }
        return d;
	}        
    
    public static strictfp double decode(String s0, String s1, 
			Map<Character, Integer> decode, int n, Map<Character, Integer> fpDecode) throws ParseException {
		return decode(s0, decode, n, false) +	(s1==null? 0: decode(s1, decode, n, true));
	}
    
    public static float decodeFloat(String s) throws ParseException {
        if(s.equals("I")) return Float.POSITIVE_INFINITY;
        else if(s.equals("i")) return Float.NEGATIVE_INFINITY; 
        else try {
    		return Float.parseFloat(s);
    	} catch(NumberFormatException nfe) {
    		String m = nfe.getMessage();
    		int j0 = m.indexOf('\"');
    		int j1 = m.lastIndexOf('\"');
    		String m1;
    		if(j0>0 && j1>0) {
    			m1 = "Unable to read as number: "+m.substring(j0, j1)+"\"";
    		} else {
    			m1 = "Unable to read number";
    		}
    		throw new ParseException(TEX, m1, nfe);
    	}
    }
    
    public static String encode(double d, boolean fpAlways, char[] code, char[] fpCode, int precision) {
    	d = Math.round(d*precision)/(double)precision;
	    boolean negative = d<0;
	    String s0 = encodeLong((long)(negative? -d: d), code, !fpAlways);
	    String s1 = encode0to1((negative? -d: d), code, fpCode.length/2 - 1);
	    char fp = fpCode[s1.length()*2 + (negative? 1: 0)];
	    String result =  s0 +
	    	(s1.length()>0 || negative || fpAlways? fp + s1: "");
	    return result;
    }

	/**
     * Encodes an integer n>0 into a String according to a representation scheme in which there is no character for zero, but 
     * instead a character for zero plus the radix. E.g., if the radix is 10 and the radix character ':' (i.e., the character
     * that comes after '9'), the number 20 will be encoded as '1:'.
     */
    public static String encode(int n, char radixC, int radix) {
        ArrayList<Character> l = new ArrayList<Character>();
        for(; n>0; n/=radix) {
            int m = n%radix;
            if(m==0) {
                m = radix;
                n -= radix;
            }
            l.add(Character.valueOf((char)(radixC-radix+m)));
        }
        StringBuilder sb = new StringBuilder();
        for(int i = l.size()-1; i>=0; i--) {
            sb.append(l.get(i));
        }
        return sb.toString();
    }
    
    /**
	 * encodes a double between 0 and 1.
	 */
    protected static strictfp String encode0to1(double d, char[] code, int maxFPDigits) {
	    ArrayList<Character> list = new ArrayList<Character>(); 
		int n = code.length;
		int k = 0; // last non-zero digit
		d -= (long)d;
		for(int i = 0; i<maxFPDigits; i++) {
	    	d *= n;
	    	int m = (int)d;
	    	if(m>0) k++;
	    	list.add(Character.valueOf(code[m]));
	    	d -= m;
	    }
        StringBuilder sb = new StringBuilder();
        int ls = list.size();
        if(ls>0) {
	        for(int i = 0; i<k; i++) {
	            sb.append(list.get(i));
	        }
        }
        return sb.toString();
	}
    
    public static strictfp String encodeFloat(float f) {
        String result;
        if(f==Float.POSITIVE_INFINITY) {
        	result = "I"; 
        } else if(f==Float.NEGATIVE_INFINITY) {
        	result = "i"; 
        } else {
        	f = (float)Math.round(f*PRECISION)/PRECISION;        	
        	result = f==(int)f? String.valueOf((int)f): String.valueOf(f); 
        }
        return result;            
    }

    /**
	 * encodes a long >= 1.
	 */
    protected static strictfp String encodeLong(long l, char[] code, boolean putZero) {
	    ArrayList<Character> list = new ArrayList<Character>(); 
		int n = code.length;
	    for(; l>0; l/=n) {
	    	int m = (int)l%n;
	    	list.add(Character.valueOf(code[m]));
	    }
        StringBuilder sb = new StringBuilder();
        int ls = list.size(); 
        if(ls==0 && putZero) {        	
        	sb.append(code[0]);
        } else {
	        for(int i = ls-1; i>=0; i--) {
	            sb.append(list.get(i));
	        }
        }
        return sb.toString();
	}
    
    public static String extractFloat(String s, int offset) {
        return extractString(s, "(i|I|-?[0-9]+\\.?[0-9]*)", offset); // 'i' and 'I' interpreted as neg./pos. Infinity
	}
	
	public static String extractString(String s, String ps, int offset) {
        Pattern p = Pattern.compile("\\A.{"+offset+"}"+ps);
        Matcher m = p.matcher(s);
        boolean found = m.find();
        return found? m.group(1): null;	    
	}

	/**
	 * Returns a list of DIInfo1 objects. This list contains normally only one element, but in the case of CompoundArrow,
	 * every element represents an element Connector in the CompoundArrow.
	 */
	protected static LinkedList<DIInfo> getComposition(
                        String hint, 
                        int[] offsetC, 
                        LinkedList<DIInfo> result, 
                        boolean inList) 
			    throws ParseException {
	    
        if(offsetC[0]>=hint.length()) {
	        return result;
	    }
	    
	    char ch = hint.charAt(offsetC[0]);
	    Class<? extends DependentItem> cl = null;
	    LinkedList<DIInfo> composition = null;
	    float priority;
	    if(ch=='[') {
	        cl = CompoundArrow.class;
	        offsetC[0] += 1;
	        composition = getComposition(hint, offsetC, new LinkedList<DIInfo>(), true);	        
	    }
	    else if(ch==']') {
	        offsetC[0] += 1;
	        return result;
	    }
	    else {
	        cl = itemClasses.get(Character.valueOf(ch));
		    if(cl==null) {
		        throw new ParseException("No class corresponding to symbol: "+ch);
			}
		    offsetC[0] += 1;
	    }
        
	    String fs = extractFloat(hint, offsetC[0]);
        if(fs==null) {
            throw new ParseException("No priority value found.");
        }
        priority = decodeFloat(fs);
        offsetC[0] += fs.length();
        
        String gapInfo = extractString(hint, "\\{(.*?)\\}", offsetC[0]);
	    if(gapInfo!=null) offsetC[0] += gapInfo.length()+2;
        
	    String info = extractString(hint, "\\((.*?)\\)", offsetC[0]);
	    if(info!=null) offsetC[0] += info.length()+2;
	    
	    boolean inverse = false;
	    if(offsetC[0]<hint.length() && hint.charAt(offsetC[0])=='\'') {
	        inverse = true;
	        offsetC[0] += 1;
	    }     
	    
	    DIInfo dii = new DIInfo1(cl, priority, gapInfo, info, composition, inverse);
	    //System.err.println("dii: "+dii+" hint: "+hint);
	    result.add(dii);
	    return !inList? result: getComposition(hint, offsetC, result, inList);
	}
	
	protected static String getHint(Item item, Map<ENode, String> eMap, Map<Group<GroupMember<?>>, Object> gMap, 
            Map<Component, Set<Group<?>>> gsMap) {
        String result = null;
        String cc = getItemInfoString(item, false);
        if(item instanceof DependentItem) {
            DependentItem s = (DependentItem)item;
            IndependentItem<?>[] e = s.getInvolutes();            
            String[] h = new String[e.length];
            
            for(int i = 0; i<e.length; i++) {
                h[i] = eMap.get(e[i]);
            }
            boolean b0 = s.canResetPriority();
            if(s instanceof Ornament) {
                Ornament us = (Ornament)s;
                String cc1 = b0? "^": "";
                result = cc+cc1+h[0]+"<"+encodeFloat((float)Math.toDegrees(us.getPreferredAngle(0)));
            } 
            else if(s instanceof Connector) {
                // priority==-1: ^; manualCPR: `; both: ~, and so on.
                boolean b1 = ((Connector)s).manualCPR;
                boolean b2 = ((Connector)s).manualBaseAngle;
                String cc1 = (b0? b1? b2? "*": "~": b2? "/": "^": b1? b2? "+": "`": b2? "-": "");
               if(s instanceof BinaryRelationship) {
	                result = cc+cc1+h[0]+","+h[1]+","+h[2];
	            }
            } else {
                result = cc; 
            }
        } else if(item instanceof ENode) {
            ENode e = (ENode)item;
            result = eMap.get(item)+cc;
        }
        
        if(item instanceof GroupMember) {
            GroupMember<?> gm = (GroupMember<?>)item;
            String groupName = (String)gMap.get(gm.getGroup());
            String gString0 = groupName!=null? (gm.isInGroup()? ":": ".")+groupName: "";
            
            StringBuilder gsb = new StringBuilder();
            Set<Group<?>> gs = gsMap.get(gm);
            if(gs.size()>0) {
                gsb.append(";");
	            for(Iterator<Group<?>> i = gs.iterator(); i.hasNext();) {
	                Group<?> g = i.next();
	                gsb.append((String)gMap.get(g)+(i.hasNext()? ",": ""));                        
	            }
            }
            result += gString0 + gsb.toString();
        }
        
        return result;
    }
    
	protected static String getItemInfoString(Item item, boolean inCompoundArrow) {
        StringBuilder sb = new StringBuilder();
        String result = null;
        Character ch = itemCodes.get(item.getClass());
        
        if(ch!=null) {
            sb.append(ch.toString());
        } 
        
        if(item instanceof CompoundArrow) {
            CompoundArrow ca = (CompoundArrow)item;
            IndependentItem<?>[] e = ca.getInvolutes();
            Connector[] r = ca.getElements();
            boolean[] co = ca.getOrientations();
            sb.append("[");
            for (int i = 0, n = r.length; i<n; i++) {
            	Connector c = r[i];
                IndependentItem<?>[] ee = c.getInvolutes();
                sb.append(getItemInfoString(c, true));
                if(!co[i]) sb.append("'");
            }
            sb.append("]").toString();
        }
        
        if(item instanceof DependentItem) {
            sb.append(encodeFloat(((DependentItem)item).getTruePriority()));
        }
        
        if(inCompoundArrow) {
	        String gapInfo = null;        
	        if(item instanceof CompoundArrow) {
	            CompoundArrow ca = (CompoundArrow)item;
	            gapInfo = encodeFloat(ca.gap0)+" "+encodeFloat(ca.gap1);
	        }
	        else {
	            gapInfo = encodeFloat(((Connector)item).gap1);
	        }
	        if(gapInfo!=null) {
	            sb.append("{"+gapInfo+"}");
	        }      
        }
        
        String info = item.getInfoString();
        if(info!=null) {
            sb.append("("+info+")");
        }      
        
        return sb.toString();
    }
    
    /**
	 * Reads a String of encoded numbers.
	 * @return the array of those numbers.
	 * @throws ParseException 
	 */
	public static double[] read(String s, Map<Character, Integer> decode, int n, 
			Map<Character, Integer> fpDecode, Pattern fpp) throws ParseException {

		List<Double> list = new ArrayList<Double>();
		
		Matcher matcher = fpp.matcher(s);
		StringBuilder sb = new StringBuilder();
		String carryOver = "";
		char fp = 0;
		int din = 0;
		boolean negative = false;
		int end = 0;
		while(matcher.find()) {
			String sx = matcher.group(1);
			if(end>0) {
				double d = decode(carryOver, sx.substring(0, din), decode, n, fpDecode);
				list.add(negative? -d: d);
			}
			carryOver = sx.substring(din);
			
			fp = matcher.group(2).charAt(0);
			end = matcher.end();
			int fpi = fp==0? 0: fpDecode.get(fp);
			negative = fpi%2>0;
			din = fpi/2; 
		}
		double d = decode(carryOver, s.substring(end, end+din), decode, n, fpDecode); 
		list.add(negative? -d: d);
		
		double[] result = new double[list.size()];
		//System.err.println("ls: "+list.size()+" "+list);
		for(int i = 0, ls = list.size(); i<ls; i++) {
			result[i] = list.get(i);
		}
		
		return result;
	}
	
	protected static void sortEntities(List<ENode> l, Collection<ENode> ground) {
	    LinkedList<ENode> r = new LinkedList<ENode>();
	    sortEntities1(l, ground, r);
	    l.clear();
	    l.addAll(r);
	}
	
	/**
	 * Returns a sorted list of the members of l0, in such a way that the more 'fundamental' ones precede those that are 
	 * less 'fundamental'.
	 */
	@SuppressWarnings("unchecked")
	protected static List<ENode> sortEntities1(List<ENode> l0, Collection<ENode> ground, List<ENode> result) {
	    List<ENode> l1 = new LinkedList<ENode>();
	    Collection<ENode>[] collections = ground!=null? 
	            	new Collection[] {ground, result}: 
	                new Collection[] {result};
	    for (Component component : l0) {
	        ENode e = (ENode)component;
	        DependentItem g = (DependentItem)e.getGovernor();
	        if(g==null) {
	            result.add(e);
	        }
	        else {
	            IndependentItem<?>[] inv = g.getInvolutes();
	            boolean addToResult = true;
	            for(int k = 0; addToResult && k<inv.length-1; k++) {
		            boolean found1 = false; // all inv[k] have to be found for addToResult to stay true.
		            for (Collection<ENode> collection : collections) {
		                for(Iterator<ENode> j = collection.iterator(); !found1 && j.hasNext();) {
			                ENode x = j.next();
			                if(inv[k]==x) {
			                    found1 = true;
			                }
			            }
		            }
		            if(!found1) addToResult = false;	                
	            }
                if(addToResult) {
                    result.add(e);
                }
                else {
                    l1.add(e);
                }
	        }	        
	    }
	    if(l1.size()>0) {
	        return sortEntities1(l1, ground, result);
	    }
	    else {
	        return result;
	    }
	}

    public Codec1() {
    }
    
	@SuppressWarnings("unchecked")
	public String getCode(Canvas canvas) {
	    Canvas.Grid grid = canvas.getGrid();
		StringBuilder sb = new StringBuilder("\\begin{texdraw}%"+VERSION_PREFIX+ID+"\n");
		sb.append("\\drawdim pt \\setunitscale "+encodeFloat(canvas.getUnitSpinner().floatValue()));
		sb.append(" %"+canvas.getWidth()+","+canvas.getHeight()).
		   append(","+grid.hGap+","+grid.vGap+","+grid.getModifier()+","+grid.hShift+","+grid.vShift+","+
		           canvas.getHorizontalDisplacement()+","+canvas.getVerticalDisplacement()+","+
		           canvas.getTransformModifier()+" ");
		int h = canvas.getHeight();
		Component[] comps = canvas.getComponents();
		List<ENode> entities = new LinkedList<ENode>();
		List<DependentItem> dependent = new LinkedList<DependentItem>();
		List<Item> items = new LinkedList<Item>();
		Map<ENode, String> eMap = new HashMap<ENode, String>(); // maps Entities to their names.
		Map<Group<GroupMember<?>>, String> gMap = new HashMap<Group<GroupMember<?>>, String>(); // maps Groups to their names.
	    Map<Component, Set<Group<?>>> gsMap = new HashMap<Component, Set<Group<?>>>(); // maps bottom-level GroupMembers to Sets of 
                // Groups that list the former as members but are not themselves the 'groups' of those members.
        Map<Group<GroupMember<?>>, Object> hogMap = new HashMap<Group<GroupMember<?>>, Object>(); // maps 'higher order' groups to their names. Kept separate 
                // from gMap because the preamble should contain information about the composition of higher order groups only.
		
        /*
         * compose lists of Entities, States, and prepare the group-set map
         */
		for(Component c : comps) {
			if(c instanceof Item) {
				items.add((Item)c);
			    if(c instanceof ENode) {
			        entities.add((ENode)c);
			    }
			    else if(c instanceof DependentItem) {
		            dependent.add((DependentItem) c);
			    }
			    if(c instanceof GroupMember) {
				    Set<Group<?>> gSet = new HashSet<Group<?>>();
				    gsMap.put(c, gSet);
			    }
			}
		}
		
		/*
		 * Produce sorted lists of Entities and States, compose gMap
		 */
		int k = 1; // group counter
		Collections.sort(items, Item.zOrderComparator);
		{
		    int j = 1;
			for(ENode e: entities) {
			    eMap.put(e, encode(j++, RADIX_CHAR, RADIX));
			    Group<GroupMember<?>> g = e.getGroup();
			    if(g!=null && !gMap.containsKey(g)) {
			        gMap.put(g, encode(k++, RADIX_CHAR, RADIX));
			    }
			}
		}		
	    for(Item item: dependent) {
	        if(item instanceof GroupMember) {
	            Group<GroupMember<?>> g = ((GroupMember<? extends Group<GroupMember<?>>>)item).getGroup();
			    if(g!=null && !gMap.containsKey(g)) {
			        gMap.put(g, encode(k++, RADIX_CHAR, RADIX));
			    }
	        }
	    }
		
		/*
		 *  Compose the sets in gsMap
		 */
        Map<Group<GroupMember<?>>, Object> groupMap = new HashMap<Group<GroupMember<?>>, Object>(gMap); // will contain both gMap and hogMap
		for (Group<GroupMember<?>> group : gMap.keySet()) {            
            getHigherOrderGroups(group, groupMap, hogMap);
        }
		for (Group<GroupMember<?>> g : groupMap.keySet()) {
		    for (Iterator<GroupMember<?>> i = g.getMembers().iterator(); i.hasNext();) {
		    	GroupMember<?> gm = (GroupMember<?>)i.next();
		        if(!(gm instanceof Group) && gm.getGroup()!=g) {
			        Set<Group<?>> set = gsMap.get(gm);
			        set.add(g);
		        }
		    }
		}
		
		/*
		 * Generate the texdraw code
		 */
		sb.append(getGroupInfo(eMap, gMap, hogMap)+"\n");
		for(Iterator<Item> j = items.iterator(); j.hasNext();) {
			Item item = j.next();
			if(!item.isHidden()) {
				String s = item.getTexdrawCode(h);
				if(s!=null && s.length()>0) {
					sb.append(s).append("%"+getHint(item, eMap, groupMap, gsMap)).append("\n");
				}
			}
		}
		return sb.append("\\end{texdraw}").toString();
	}

    /**
     * Constructs a Connector object specified by the DIInfo1 parameter, with the involutes given by the ENode array.
     */
    protected Connector getConnector(Canvas canvas, DIInfo si, ENode[] e) throws ParseException {
       Connector result = null;
       boolean inverse = si.getInverse();
       Class<?> cl = si.getDIClass();
       List<DIInfo> composition = si.getComposition();
	   ENode[] e1 = inverse? new ENode[]{e[1], e[0]}: new ENode[]{e[0], e[1]};
	   try {
		   if(BinaryRelationship.class.isAssignableFrom(cl)) {
	           Constructor<?> c = null;
	  	       if(cl==CompoundArrow.class) {
	  	           if(composition==null || composition.size()==0) {
	  	               throw new ParseException(HINT, "Composition list of compound arrow is empty");
	  	           }
	  	           LinkedList<Connector> rl = new LinkedList<Connector>();
	  	           for (DIInfo si1 : composition) {
	  	               Connector s1 = getConnector(canvas, si1, e1);
	  	               s1.setPriority(si1.getPriority());
	  	               rl.add(s1);
	  	           }
	  	           Connector[] rels = rl.toArray(new Connector[]{});
				   for (Connector element : rels) {
				       canvas.removeItem(element.getItem()); // via State.parse() and Canvas.relocateItem(), 
				       			// rels[i] may already have added itself to the Canvas.
			           element.getItem().getDependentItems().clear();
				   }
		       	   c = cl.getConstructor(new Class[]{ENode.class, ENode.class, 
		       	           Connector[].class, Boolean.TYPE, Canvas.CompoundArrowDissolve.class});
		       	   result = (Connector)c.newInstance(new Object[]{e1[0], e1[1], 
		       	           rels, Boolean.FALSE, canvas.new CompoundArrowDissolve(rl, true, true)});
	  	       } else {
		       	   c = cl.getConstructor(new Class[]{ENode.class, ENode.class});
		       	   result = (Connector)c.newInstance((Object[])e1);
	  	       }
	           BinaryRelationship rel = (BinaryRelationship)result;
	       	   if(e.length>2 && e[2]!=null) { // e.length might be less than 3 if rel is going to be a CompoundArrow element.
	       	       rel.installItem(e[2]);
	       	       e[2].setGovernor(rel);
		       }
	       }
	   }
	   catch(Exception ex) {
	       ex.printStackTrace(System.err);
	       throw new ParseException(HINT, "Unexpected error ("+ex.getClass().getName()+") while creating connector");
	   }
       result.setCanvas(canvas);
	   return result;
	}
	
    protected DimensionInfo getDimensionInfo(Canvas canvas, String code, String hint) throws ParseException {
        DimensionInfo result = new DimensionInfo();
        
        String[] sp2 = hint.split(",");
        int n = 10;
        int i = 0;
        if(sp2.length!=n) {
            throw new ParseException(HINT, "Corrupt preamble: incorrect number of fields: "+sp2.length);
        }
        try {
            result.dimension = new Dimension(Integer.parseInt(sp2[i++]), Integer.parseInt(sp2[i++]));
        } catch(NumberFormatException nfe) {
            throw new ParseException(HINT, "Preamble contains corrupt dimension specification");
        }
        float[] u = Texdraw.getUnitScales(code);
        if(u==null || u.length!=1) {			            
            throw new ParseException(HINT, "Preamble contains corrupt unit scale specification");
        } 
        result.unitScale = u[0];
        
        try {
        	result.grid = canvas.new Grid(
                Integer.parseInt(sp2[i++]), 
                Integer.parseInt(sp2[i++]),
                Integer.parseInt(sp2[i++]),
                Integer.parseInt(sp2[i++]), 
                Integer.parseInt(sp2[i++]));        
        } catch(NumberFormatException nfe) {
            throw new ParseException(HINT, "Preamble contains corrupt grid specification: ");
        }
        
        try {
            result.hDisplacement = Integer.parseInt(sp2[i++]);
            result.vDisplacement = Integer.parseInt(sp2[i++]);
            result.transformModifier = Integer.parseInt(sp2[i++]);
            if(i!=n) {
            	throw new Error("Mismatch");
            }
        } catch(NumberFormatException nfe) {
            throw new ParseException(HINT, "Preamble contains corrupt copy displacement specification");
        }
        
        return result;
    }
		
    /**
     * @return A String that encodes which groups are contained in which other groups.
     * Bottom-level GroupMembers are ignored here, even though they can be members of the same (higher-order) Groups as Groups.  
     */
    protected String getGroupInfo(Map<ENode, String> eMap, Map<Group<GroupMember<?>>, String> gMap, 
    		Map<Group<GroupMember<?>>, Object> hogMap) {
        StringBuilder sb = new StringBuilder();
        for (Group<GroupMember<?>> hog : hogMap.keySet()) {
            sb.append(" "+(String)hogMap.get(hog));
            for (Iterator<GroupMember<?>> i = hog.getMembers().iterator(); i.hasNext();) {
            	GroupMember<?> g = i.next();
                if(g instanceof Group) {
                    String gName = gMap.get(g);
                    if(gName==null) {
                        gName = (String)hogMap.get(g);
                    }
                    if(gName!=null) { // if the name is null, we could make one up, but then, if the name is null, the group 
                        		// does not have any members (or else it would be either in gMap or hogMap), so we don't bother.  
                        if(g.getGroup()==hog) { // proper member
                            sb.append(g.isInGroup()? ":": ".").append(gName);                
                        } else { // improper member
                            sb.append(";").append(gName);
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    /* 
	 * For parsing the preamble. Returns a Map from names to Groups, containing all higher-order Groups and their member Groups.
	 */
    protected Map<String, StandardGroup<GroupMember<?>>> getGroupMap(String[] s, int begin, int end) 
    			throws ParseException {
        HashMap<String, StandardGroup<GroupMember<?>>> result = new HashMap<String, StandardGroup<GroupMember<?>>>();
        Pattern p0 = Pattern.compile("[^.:;]+");
        Pattern p = Pattern.compile("[.:;][^.:;]+");
        for(int i = begin; i<s.length && i<end; i++) {
            Matcher m0 = p0.matcher(s[i]); 
            boolean b = m0.find();
            if(!b) {
                throw new ParseException("Group name missing in: "+s[i]);
            }
            String name0 = m0.group();            
            StandardGroup<GroupMember<?>> g0 = result.get(name0);
            if(g0==null) {
                g0 = new StandardGroup<GroupMember<?>>(new HashSet<GroupMember<StandardGroup<GroupMember<?>>>>());
                result.put(name0, g0);
            }
            result.put(name0, g0);
            Matcher m = p.matcher(s[i].substring(m0.end()));
            while(m.find()) {
                String code = m.group();
                String name = code.substring(1);
                boolean inGroup = code.startsWith(":");
                boolean improperMember = code.startsWith(";");
                StandardGroup<GroupMember<?>> g = result.get(name);
                if(g==null) {
                    g = new StandardGroup<GroupMember<?>>(new HashSet<GroupMember<?>>());
                    result.put(name, g);
                }
                if(improperMember) { 
                    g0.getMembers().add(g);
                } else {
                    g0.add((GroupMember<?>)g);
                    ((GroupMember<?>)g).setInGroup(inGroup);                
                } 
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
	protected void getHigherOrderGroups(Group<GroupMember<?>> g, Map<Group<GroupMember<?>>, Object> groupMap, 
    		Map<Group<GroupMember<?>>, Object> result) {
        Group<GroupMember<?>> gg = ((GroupMember<? extends Group<GroupMember<?>>>)g).getGroup();
        if(gg!=null) {
            Object name0 = groupMap.get(gg);
            if(name0==null) {
                name0 = encode(groupMap.size()+1, RADIX_CHAR, RADIX);
            }
            Object name1 = result.get(gg);
            if(name1==null) {
                result.put(gg, name0);
                groupMap.put(gg, name0);
                getHigherOrderGroups(gg, groupMap, result);
            }
        }
    }

	public void load(Canvas canvas, String s, boolean replace) throws ParseException, NumberFormatException {
	    
	    /* 
	     * First pass: parse preamble (i.e., first line), collect the Entities into eMap, put remaining 
	     * hint/code pairs into list.
	     */ 
	    Map<Integer, ENode> eMap = new TreeMap<Integer, ENode>(); 
	    Map<String, StandardGroup<GroupMember<?>>> gMap = new HashMap<String, StandardGroup<GroupMember<?>>>(); 
	    ArrayList<String[]> list = new ArrayList<String[]>();
	    StringBuilder sb0 = new StringBuilder();
	    String[] lines = s.split("[\\r\\n]+");
	    DimensionInfo dimInfo = null;
	    float maxPriority = 0;
	    float unitScale = 1;
	    CodePortion cp = TEX;
	    String code = null;
	    for (String line : lines) {
	    	boolean start = line.contains("\\"); // new texdraw command started?
	    	
		    if(start && cp==HINT) { // end of hint
			    /*
			     * Try to parse code/hint pair as ENode
			     */
			    String hint = sb0.toString();
			    sb0 = new StringBuilder();
			    //System.err.println("c: "+code+" h: "+hint);
			    
			    if(dimInfo==null && hint.startsWith(VERSION_PREFIX)) { // do nothing (version already parsed)			    	
			    } else if(dimInfo==null) try { // preamble not yet parsed
			        String[] sp1 = hint.split("\\s+");
			        dimInfo = getDimensionInfo(canvas, code, sp1[0]);
			        gMap = getGroupMap(sp1, 1, sp1.length);
			        //System.err.println(gMap);
			    } catch(ParseException pe) {	           
		            pe.setCausingText(pe.portion==TEX? code: hint);
		            throw pe;
		        } else try {
			        boolean success = parseENode(canvas, hint, code, eMap, gMap);
			        if(!success) {
			            list.add(new String[]{hint, code});
			        }
	 		    } catch(ParseException pe) {	           
		            pe.setCausingText(pe.portion==TEX? code: hint);
		            throw pe;
			    }	    

	 		    cp = TEX;
	    	}

		    /*
		     * Extract tex-comment (i.e., 'hint')
		     */
		    String[] sp = line.split("[^\\\\]%"); 
		    if(line.startsWith("%")) {
		    	sp = new String[] {"", line.substring(1)};
		    }
		    
		    if(sp.length<2) {// assume line break
		        sb0.append(line+" ");
		    } else {
		    	/*
		    	 * New hint found (if cp==TEX).
		    	 */
		        if(sp.length>2) {
		        	/*
		        	 * There are extra unescaped %-signs in the tex code or hint. This shouldn't happen, since these signs
		        	 * serve as indicators of where a hint starts. Hoping for the best, we add all but the last to sb0.
		        	 */
			        for(int j = 0; j<sp.length-1; j++) {
			            sb0.append(sp[j]+(j<sp.length-2? "%": ""));
			        }
			    }
			    else {
			        sb0.append(sp[0]);
			    }
		        if(cp==TEX) {
		        	code = sb0.toString();
			        sb0 = new StringBuilder();
			        cp = HINT;
		        }
		        sb0.append(sp[sp.length-1]);
		    }

	    }
	    
	    /* 
	     * Set Canvas size, scale factor, Grid, etc.
	     */
	    if(dimInfo!=null) {
		    if(replace) {
		        canvas.setSize(dimInfo.dimension);
			    canvas.getUnitSpinner().setValue(Float.valueOf(dimInfo.unitScale));
			    canvas.setGrid(dimInfo.grid);
			    canvas.setHorizontalDisplacement(dimInfo.hDisplacement);
			    canvas.setVerticalDisplacement(dimInfo.vDisplacement);
			    canvas.setTransformModifier(dimInfo.transformModifier);
		    } else {
		        Dimension d0 = canvas.getSize();
		        Dimension d = new Dimension(
		                Math.max(d0.width, dimInfo.dimension.width), 
		                Math.max(d0.height, dimInfo.dimension.height));
		        canvas.setSize(d);
		    }
	    }

	    /* 
	     * Second pass: DIs
	     */
	    ArrayList<DependentItem> dIs = new ArrayList<DependentItem>();
	    for (String[] pair : list) {
	       String hint = pair[0];
	       String tex = pair[1];
	       try {
	           parseDependentItem(canvas, hint, tex, eMap, gMap, dIs);
	       } catch(ParseException pe) {	
	           pe.setCausingText(pe.portion==TEX? tex: hint);
	           throw pe;
	       }
	    }
	    
	    /* 
	     * Add EntityNodes.
	     */
	    if(replace) {
	        canvas.removeAll();
	    }
	    Collection<ENode> eNs = eMap.values(); // really a set
	    for (ENode e : eNs) {
	    	Rectangle2D.Float r = (Rectangle2D.Float)e.getBounds2D();
	        canvas.addItem(e, r.x, canvas.getHeight()-r.height-r.y, e.getGovernor()==null);
	        canvas.markForRelocation(e);
	    }

	    /* 
	     * Add DependentItems.
	     */
        Collections.sort(dIs, DependentItem.priorityComparator);
	    for(DependentItem di: dIs) {
	        canvas.addDependentItem(di, false, true, false); // don't let the Canvas arrange contacts,
	        		// since the angles have been preset.
 		    if(di instanceof Connector) { 	           
 		    	Connector con = (Connector)di; 
 	       	    Item item = con.getItem();
 		        Point2D.Float p;
 		        if(eNs.contains(item)) {
	 		    	Point2D c = item.getCenter();
	 		        p = (Point2D.Float)con.setItemLocation(c);
 		        } else {
 		        	p = (Point2D.Float)con.getItemLocation();
	        	    canvas.markForRelocation(item);
 		        }
	        	item.setBounds2D(p.x, p.y, item.getWidth2D(), item.getHeight2D());
   		    }
	        
	        float p = di.getTruePriority();
	        if(p>maxPriority) maxPriority = p;
	        canvas.markForRelocation(di);
	    }

        canvas.setCurrentPriority(replace? (int)(maxPriority+1): 
            Math.max(canvas.getCurrentPriority(), (int)maxPriority+1));

        for(ENode e: eNs) {
        	e.arrangeContacts();
        }
        canvas.relocateItems();
	}
	
    /**
	 * returns true if the hint indicates an Entity.
	 */
	protected boolean parseENode(Canvas canvas, String hint, String code, 
			Map<Integer, ENode> eMap, Map<String, StandardGroup<GroupMember<?>>> gMap) 
				throws ParseException {
        boolean success = false;
	    int n = 0;
        ENode e = null;
        String name = null, info = null, gString = null;
        GroupInfo gi = new GroupInfo();
        try { // if this goes through, we assume that the thing is an Entity
            int i = hint.indexOf('(');
            if(i>=0) {
                info = extractString(hint, "\\((.*?)\\)", i);
                name = hint.substring(0, i);
            }
            int j = parseGroupInfo(hint, gi);
            name = i<0 && j<0? hint: hint.substring(0, i<0? j: i);
            n = decode(name, RADIX_CHAR, RADIX);
            success = true;
        }
        catch(Exception ex) {
            //ex.printStackTrace(System.err);
            //System.err.println("Exception while processing: "+hint);
            //System.err.println(ex);
        }
        
        if(success) {
	        e = new ENode(canvas, null, false);	        
	        e.setName(name);
	        setGroup(e, gi, gMap);
	        e.parse(code, info);
		    eMap.put(Integer.valueOf(n), e);
        }
        return success;
	}
	
	protected int parseGroupInfo(String hint, GroupInfo gni) {
        String groupName = null;
        String[] impGroupNames = null;
	    boolean inGroup = true;
        int j = hint.indexOf(':');
        if(j<0) {
            j = hint.indexOf('.');
            inGroup = false;
        }
        String gString = null;
        if(j>=0) {
            gString = hint.substring(j+1);
            int k = gString.indexOf(";");
            if(k<0) {
                groupName = gString.trim();
            } else {
                groupName = gString.substring(0, k);
                impGroupNames = gString.substring(k+1).split(",");
            }
        }
        gni.groupName = groupName;
        gni.impGroupNames = impGroupNames;
        gni.inGroup = inGroup;
        return j;
	}
	
	@SuppressWarnings("unchecked")
	protected void parseDependentItem(Canvas canvas, String hint, String code, Map<Integer, ENode> eMap, 
			Map<String, StandardGroup<GroupMember<?>>> gMap, List<DependentItem> list) 
			throws ParseException {
	    ENode[] inv = null;

	    char ch = hint.charAt(0);
	    int[] offsetC = new int[]{0};
        LinkedList<DIInfo> composition = 
            getComposition(hint, offsetC, new LinkedList<DIInfo>(), false);
        int offset = offsetC[0];
        DIInfo dii = composition.get(0);
        GroupInfo gi = new GroupInfo();
        char ch1 = offset<hint.length()? hint.charAt(offset): 0;  
        
        /*
         * Interpret auxiliary info.
         */
        boolean manualCPR = false;
        boolean priorityResettable = false;
        boolean manualBaseAngle = false;
        int add = 0;
        if(ch1=='*' || ch1=='+' || ch1=='~' || ch1=='`') {
            manualCPR = true;
            add = 1;
        }
        if(ch1=='*' || ch1=='/' || ch1=='~' || ch1=='^') {
            priorityResettable = true;
        	add = 1;
        }
        if(ch1=='*' || ch1=='/' || ch1=='+' || ch1=='-') {
            manualBaseAngle = true;
        	add = 1;
        }
        offset += add;
        
        /*
         * Create the State.
         */
        String hintRest = hint.substring(offset); 
        String[] split = hintRest.split(","); // commas separate arguments
        ENode[] e = null;
        DependentItem di = null;
        int k = split.length;
        Class<?> cl = dii.getDIClass();
        if(Connector.class.isAssignableFrom(cl)){
     	    if(!(k==3 && BinaryRelationship.class.isAssignableFrom(cl))) {
        	       throw new ParseException(HINT, "Illegal number of commas");
     	    }
            e = new ENode[k];
            for(int i = 0; i<k; i++) {
                try {
                    e[i] = eMap.get(Integer.valueOf(decode(split[i].trim(), RADIX_CHAR, RADIX)));
                } catch(IllegalArgumentException iae) {
 			       throw new ParseException(HINT, "Illegal name: '"+split[i].trim()+"'");
                }
                if(e[i]==null && i!=2) { // third relata, though named, do not need to occur in the code
                    throw new ParseException(HINT, "Illegal name \'"+split[i].trim()+"\'");
                }
            }
            di = getConnector(canvas, dii, e);
        } else {
        	if(Ornament.class.isAssignableFrom(cl)) {
	
			   if(k!=1) {
			       throw new ParseException(HINT, "Illegal number of commas");
			   }
			   String[] sp1 = split[0].split("<");
			   try {
			       e = new ENode[] {eMap.get(Integer.valueOf(decode(sp1[0].trim(), RADIX_CHAR, RADIX)))};
			   } catch(IllegalArgumentException iax) {
			       throw new ParseException(HINT, "Illegal name: '"+sp1[0].trim()+"'");
			   }
			   try {
			       Constructor<?> c = cl.getConstructor(new Class[]{ENode.class});
			       di = (Ornament)c.newInstance((Object[])e);
			   }
			   catch(Exception ex) {
			       ex.printStackTrace(System.err);
			       throw new ParseException(HINT, "Unexpected error ("+
			    		   ex.getClass().getName()+") while creating ornament");   	           
			   }
			   
			   Ornament o = (Ornament)di;
			   
			   int j = parseGroupInfo(sp1[1], gi);
			   
			   String angleString = j<0? sp1[1].trim(): sp1[1].substring(0, j);   	       
			   int a = 0;
			   
			   try {
		            a = (int)Math.round(Float.parseFloat(angleString));
	            } catch (NumberFormatException nfe) {
		            throw new ParseException(HINT, "Illegal number format");
	            }   	       
			   o.setPreferredAngle(Math.toRadians(a), false);       	      	
			   
	        } else {
		       try {
	   	           Constructor<?> c = cl.getConstructor((Class[])null);
	   	           di = (DependentItem)c.newInstance((Object[])e);
	   	       }
	   	       catch(Exception ex) {
	   	           ex.printStackTrace(System.err);
	   	           throw new ParseException(HINT, "Unexpected error ("+
	   	                   ex.getClass().getName()+") while creating "+cl.getSimpleName());   	           
	   	       }
			   parseGroupInfo(hintRest, gi);
	        }
		
        	di.setCanvas(canvas);
		    if(GroupMember.class.isAssignableFrom(cl)) {
		    	setGroup((GroupMember<StandardGroup<GroupMember<?>>>)di, gi, gMap);
		    }
        }
       
        /*
         * Set the fields of the State just created, a task mostly delegated to State#parse().
         */
        for(int i = 0; i<2; i++) {
           /*
            *  this part performed both before and after the second part, in case the second part should cancel the effect:
            */
	       if(manualCPR) ((Connector)di).manualCPR = true;
	       
	       float p = dii.getPriority();
	       if(priorityResettable) {
	           di.setPriority(di.specialPriority());
	           di.oldPriority = p;
	           di.priorityResettable = true;
	           di.priorityChangeable = false;
	       } else {
	           di.setPriority(p);
	       }
	       if(manualBaseAngle) ((Connector)di).manualBaseAngle = true;
	       
	       /* 
	        * this part performed only once:
	        */
  	       if(i==0) {
  	           if(CompoundArrow.class.isAssignableFrom(dii.getDIClass())) {
  	               ((CompoundArrow)di).parse(code, dii);
  	           }
  	           else {
  	               di.parse(code, dii.getInfoString());
  	           }
  	       }
       }
        
       list.add(di);    
	}

	protected void setGroup(GroupMember<StandardGroup<GroupMember<?>>> gm, GroupInfo gi, 
			Map<String, StandardGroup<GroupMember<?>>> gMap) {
        if(gi.groupName!=null) {
	        StandardGroup<GroupMember<?>> g = gMap.get(gi.groupName);
	        if(g==null) {
	            g = new StandardGroup<GroupMember<?>>(new HashSet<GroupMember<?>>());
	            gMap.put(gi.groupName, g);
	        }
	        g.add(gm);
	        gm.setInGroup(gi.inGroup);
	        if(gi.impGroupNames!=null) {
		        for(String ign : gi.impGroupNames) {
		            StandardGroup<GroupMember<?>> ig = gMap.get(ign);
		            if(ig==null) {
		                ig = new StandardGroup<GroupMember<?>>(new HashSet<GroupMember<?>>());
		                gMap.put(ign, g);
		            }
		            ig.members.add(gm);
		        }
	        }
        }
	}

}
