package io.onedev.commons.utils;

public class ColorUtils {
	
	public static boolean isLight(String color) {
		int red = Integer.valueOf(color.substring( 1, 3 ), 16);
		int green = Integer.valueOf(color.substring( 3, 5 ), 16);
		int blue = Integer.valueOf(color.substring( 5, 7 ), 16);
	    double a = 1 - ( 0.299 * red + 0.587 * green + 0.114 * blue)/255;

	    return a < 0.5;
	}

}
