package gui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class DogHelp {

	private static DogHelp singleInstance;
	
	private static String mainHelpFile = "helpcontent/PamdogHelp.html";
//	private static String mainHelpFolder = "helpcontent";
	
	private JFrame helpFrame;
	
	public static DogHelp getHelp() {
		if (singleInstance == null) {
			singleInstance = new DogHelp();
		}
		return singleInstance;
	}
	
	public void showHelp() {
		if (helpFrame == null) {
			helpFrame = createHelpFrame();
		}
		if (helpFrame != null) {
			helpFrame.setVisible(true);
		}

	}

	/**
	 * Make a frame with the html in it. 
	 * @return Frame to display. 
	 */
	private JFrame createHelpFrame() {
		
		URL url = null;
		try {
			url = ClassLoader.getSystemResource(mainHelpFile);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		JFrame frame = new JFrame("PAMDog Help");
		frame.setIconImage(PamDogGUI.getIconImageSmall());
		frame.setSize(new Dimension(800,600));
		frame.setLocation(50, 60);
		JEditorPane ed1=new JEditorPane();
		ed1.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
		ed1.setEditable(false);
		ed1.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				hyperlinkEvent(e);
			}
		});
		try {
			ed1.setPage(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
		JScrollPane scrollPane = new JScrollPane(ed1);
		frame.add(scrollPane);
		return frame;
	}

	protected void hyperlinkEvent(HyperlinkEvent e) {
        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        	URL url = e.getURL();
        	String link = url.toString();
        	if (link.startsWith("http")) {
        		try {
					Desktop.getDesktop().browse(url.toURI());
				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
        	}
        }		
	}
}
