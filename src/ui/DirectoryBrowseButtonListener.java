package ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JTextField;

public class DirectoryBrowseButtonListener implements ActionListener {
	private JTextField associatedTextField;
	
	public DirectoryBrowseButtonListener(JTextField associatedTextField) {
		this.associatedTextField = associatedTextField;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser filePicker = new JFileChooser();
		filePicker.setCurrentDirectory(new File(System.getProperty("user.dir")));
		filePicker.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		
		int path = filePicker.showOpenDialog(null);
		
		if (path == JFileChooser.APPROVE_OPTION) {
			File file = filePicker.getSelectedFile();
			associatedTextField.setText(file.getPath());
		}
	}
}
