package gui;

import java.awt.GridBagConstraints;
import java.awt.Insets;

public class DogContraints extends GridBagConstraints {

	public DogContraints() {
		gridx = gridy = 0;
//		fill = HORIZONTAL;
		anchor = WEST;
		insets = new Insets(2,2,2,2);
	}

}
