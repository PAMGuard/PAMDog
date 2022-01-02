package gui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

public class PamDogGUI {



	public static Image getIconImageSmall() {
		BufferedImage img = null;
		try {
//			ImageIcon icon = new ImageIcon(ClassLoader
//				.getSystemResource("resources/pamguardModelIcon.png"));
			ImageIcon icon = new ImageIcon(ClassLoader
					.getSystemResource("resources/pamguardIconSmall.png"));
			return icon.getImage();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return img;
	}
	public static Image getIconImageLarge() {
		BufferedImage img = null;
		try {
			ImageIcon icon = new ImageIcon(ClassLoader
				.getSystemResource("resources/pamguardIcon.png"));
			return icon.getImage();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return img;
	}
}
