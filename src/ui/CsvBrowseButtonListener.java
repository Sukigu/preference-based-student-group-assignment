package ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

class CsvBrowseButtonListener implements ActionListener {
	private JTextField associatedTextField;
	
	public CsvBrowseButtonListener(JTextField associatedTextField) {
		this.associatedTextField = associatedTextField;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser filePicker = new JFileChooser();
		filePicker.setCurrentDirectory(new File(System.getProperty("user.dir")));
		filePicker.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (file.isDirectory()) return true;
				
				String fileName = file.getName();
				int i = fileName.lastIndexOf('.');
				
				if (i > 0) {
				    return fileName.substring(i+1).toLowerCase().equals("csv");
				}
				else return false;
			}

			@Override
			public String getDescription() {
				return "Ficheiros CSV (*.csv)";
			}
			
		});
		
		int path = filePicker.showOpenDialog(null);
		
		if (path == JFileChooser.APPROVE_OPTION) {
			File file = filePicker.getSelectedFile();
			associatedTextField.setText(file.getPath());
		}
	}
}
