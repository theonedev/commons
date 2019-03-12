package io.onedev.commons.jsymbol.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class IconGenerator {
	
	public static void main(String[] args) {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = null;
		try {
			graphics = (Graphics2D) image.getGraphics();
			RenderingHints rh = new RenderingHints(
		             RenderingHints.KEY_ANTIALIASING,
		             RenderingHints.VALUE_ANTIALIAS_ON);
		    graphics.setRenderingHints(rh);

			 graphics.setColor(Color.decode("0x7F2AFF")); //normal
//			graphics.setColor(Color.decode("0xFF557F")); //local
			graphics.fillOval(0, 0, 16, 16);
			int fontSize = new Double(16).intValue();
			Font font = new Font(Font.MONOSPACED, Font.BOLD, fontSize);
			graphics.setFont(font);
			graphics.setColor(Color.WHITE);
			graphics.drawString("m", 4, 12);
			ImageIO.write(image, "PNG", new File("W:\\onedev\\jsymbol\\src\\main\\java\\com\\onedev\\jsymbol\\c\\symbols\\ui\\icon\\member.png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (graphics != null)
				graphics.dispose();
		}
	}
}
