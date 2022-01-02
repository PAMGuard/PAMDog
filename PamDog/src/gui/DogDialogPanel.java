package gui;

import java.awt.Component;

import pamdog.DogParams;

public interface DogDialogPanel {

	public Component getComponent();
	public String getTitle();
	public void setParams(DogParams dogParams);
	public boolean getParams(DogParams dogParams);
}
