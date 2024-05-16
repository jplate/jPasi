/*
 * Created on 05.06.2007
 *
 */
package jPasi;

/**
 * @author Jan Plate
 *
 */
public class TransformModifier {

    public static final int scalingLines = 1;
    public static final int scalingArrowheads = 2;
    public static final int scalingNodes = 4;
    public static final int flippingArrowheads = 8;
    public static final int rotatingLabels = 16;
    
    private TransformModifier() {
    }

    public static boolean isScalingLines(int m) {
        return (m & scalingLines)>0;
    }
    
    public static boolean isScalingArrowheads(int m) {
        return (m & scalingArrowheads)>0;
    }

    public static boolean isScalingNodes(int m) {
        return (m & scalingNodes)>0;
    }

    public static boolean isRotatingLabels(int m) {
        return (m & rotatingLabels)>0;
    }

    public static boolean isFlippingArrowheads(int m) {
        return (m & flippingArrowheads)>0;
    }
}
