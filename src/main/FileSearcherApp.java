package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

public class FileSearcherApp extends JFrame implements FileSearchObserver {

	private static final long serialVersionUID = 1L;

	final static String windowTitle = "FileSearcher";

	List<FileSearcher> fileSearchers = new ArrayList<>();
	StyledDocument doc;
	FileSearcherApp app = this;

	// input text
	int inputTextSize = 18;
	Font inputFont = new Font("Courier", Font.PLAIN, inputTextSize);
	Color inputTextColor = new Color(52, 80, 100).darker().darker();

	// prompt text
	int promptTextSize = inputTextSize;
	Font promptFont = inputFont;
	Color promptTextColor = Color.GRAY.darker();

	// output text
	int resultTextSize = 17;
	Font resultFont = new Font("Dialog", Font.BOLD, resultTextSize);
	Color resultTextColor = new Color(41, 128, 250).darker();

	// button text
	int buttonTextSize = 20;
	Font buttonFont = new Font("Dialog", Font.PLAIN, buttonTextSize);
	Color buttonTextColor = Color.LIGHT_GRAY;

	// graficke komponente
	JTextField searchBar;
	JTextField searchFolder;
	JTextArea resultArea;
	JCheckBox foldersCheckBox;
	JButton searchButton, stopButton;

	Color backgroundColor = new Color(52, 73, 94);
//	Color backgroundColor = Color.WHITE;
	Color textAreaColor = new Color(236, 240, 241).darker(); // new Color(127, 140, 141);
	Color searchButtonColor = new Color(52, 152, 219);
	Color stopButtonColor = new Color(231, 76, 60);

	String searchBarPrompt = "Search pattern";
	String folderPathPrompt = "Folder path (default is C:\\)";
	String noPatternWarning = "Enter search pattern!";

	Desktop desktop = Desktop.getDesktop();
	int numOfCores = Runtime.getRuntime().availableProcessors();

	public FileSearcherApp() {
		initGUI();
		addPromptText();
		bindActions();
		for (int i = 0; i < numOfCores; i++)
			fileSearchers.add(new SearchingFileVisitor(this));
	}

	private void initGUI() {
		JPanel mainPanel = new JPanel();
		this.setTitle(windowTitle);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setResizable(true);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
		mainPanel.setLayout(new BorderLayout(0, 10));

		setMinimumSize(new Dimension(400, 300));

		searchBar = new JTextField();
		searchFolder = new JTextField();
		resultArea = new JTextArea();
		;
		foldersCheckBox = new JCheckBox("Include folders");
		searchButton = new JButton("Search");
		stopButton = new JButton("Stop");

		doc = new DefaultStyledDocument();
		resultArea.setDocument(doc);
		resultArea.setBackground(textAreaColor);
		resultArea.setForeground(resultTextColor);
		resultArea.setFont(resultFont);
		resultArea.setLineWrap(false);
		resultArea.setEditable(false);
		resultArea.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		mainPanel.setBackground(backgroundColor);

		stopButton.setEnabled(false);

		Border emptyB = BorderFactory.createEmptyBorder();

		JPanel topPanel = new JPanel(new GridLayout(1, 2, 20, 0));
		topPanel.setPreferredSize(new Dimension(800, 35));
		topPanel.setBackground(backgroundColor);
		topPanel.setBorder(emptyB);
		searchBar.setBackground(textAreaColor);
		searchBar.setBorder(emptyB);
		searchFolder.setBackground(textAreaColor);
		searchFolder.setBorder(emptyB);
		topPanel.add(searchBar);
		topPanel.add(searchFolder);

		JScrollPane scrollPanel = new JScrollPane(resultArea);
		scrollPanel.setBorder(BorderFactory.createEmptyBorder());
		scrollPanel.setPreferredSize(new Dimension(800, 300));

		JPanel bottomPanel = new JPanel(new GridLayout(1, 3, 20, 0));
		bottomPanel.setPreferredSize(new Dimension(800, 35));
		bottomPanel.setBackground(backgroundColor);
		bottomPanel.setBorder(emptyB);
		foldersCheckBox.setBackground(backgroundColor);
		foldersCheckBox.setForeground(buttonTextColor);
		foldersCheckBox.setFont(buttonFont);
		bottomPanel.add(foldersCheckBox);
		searchButton.setBackground(searchButtonColor);
		searchButton.setFont(buttonFont);
		searchButton.setForeground(buttonTextColor);
		bottomPanel.add(searchButton);
		stopButton.setBackground(stopButtonColor);
		stopButton.setFont(buttonFont);
		stopButton.setForeground(buttonTextColor);
		bottomPanel.add(stopButton);

		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		this.add(mainPanel);
		this.pack();
	}

	private void bindActions() {

		searchButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				Thread waiting = new Thread(() -> {

					String folderInput = searchFolder.getText();
					if (folderInput.isBlank() || folderInput.equals(folderPathPrompt))
						folderInput = "C:\\";

					String pattern = searchBar.getText().toLowerCase(getLocale());
					String[] folders = folderInput.split(",");

					if (!pattern.isBlank() && !pattern.equals(searchBarPrompt)) {
						searchButton.setEnabled(false);
						stopButton.setEnabled(true);
						resultArea.setText("");
					} else {
						new JOptionPane(noPatternWarning, JOptionPane.ERROR_MESSAGE);
						return;
					}

					LinkedList<Thread> threads = new LinkedList<>();
					DequeSynchronizedStack<Path> pathTrees = new DequeSynchronizedStack<>();

					// preparing path collection
					Path parentDir;
					List<Path> pathList = new ArrayList<>();
					for (String folder : folders) {
						// Drive search(if user enters only drive letter)
						if (folder.length() < 3)
							folder = folder.substring(0, 1).toUpperCase() + ":\\";

						parentDir = Paths.get(folder).normalize().toAbsolutePath();
						if (parentDir.toFile().exists()) {
							try {
								pathList.addAll(Files.list(parentDir).collect(Collectors.toList()));
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println("Non existent directory: " + parentDir.toString());
							continue;
						}
					}

					Collections.shuffle(pathList);
					for (Path p : pathList)
						pathTrees.push(p);

					for (int i = 0; i < numOfCores; i++) {
						FileSearcher searcher = fileSearchers.get(i);
						if (foldersCheckBox.isSelected())
							searcher.setFoldersIncluded(true);
						else
							searcher.setFoldersIncluded(false);

						Thread t = new Thread(() -> {
							while (pathTrees.size() > 0) {
								Optional<Path> optPath = pathTrees.pop();
								if (optPath.isPresent()) {
//									System.out.println("Searching tree: " + optPath.get());
									searcher.search(pattern, optPath.get());
								}
							}
						});
						threads.add(t);
						t.start();
//						System.out.println("Started thread: " + t.getName());
					}

					for (Thread t : threads) {
						try {
							t.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					threads.clear();

					stopButton.setEnabled(false);
					searchButton.setEnabled(true);
//					System.gc();
				});

				waiting.start();
			};
		});

		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (FileSearcher f : fileSearchers)
					f.stop();
				stopButton.setEnabled(false);
				searchButton.setEnabled(true);
			}
		});

		resultArea.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			public void mouseClicked(MouseEvent e) {
				Element ele = (doc.getCharacterElement(resultArea.viewToModel2D(e.getPoint())));
				int start = ele.getStartOffset();
				int end = ele.getEndOffset();
				try {
					String path = resultArea.getText(start, end - start - 1);
					if (!path.isBlank())
						desktop.open(new File(path));
				} catch (Exception ex) {
					System.err.println(ex);
				}
			}
		});
	}

	private void addPromptText() {

		searchBar.setFont(promptFont);
		searchBar.setForeground(promptTextColor);
		searchBar.setText(searchBarPrompt);
		searchFolder.setFont(promptFont);
		searchFolder.setForeground(promptTextColor);
		searchFolder.setText(folderPathPrompt);
		searchBar.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent fe) {
				if (searchBar.getText().equals(searchBarPrompt)) {
					searchBar.setText("");
					searchBar.setFont(inputFont);
					searchBar.setForeground(inputTextColor);
				}
			}

			@Override
			public void focusLost(FocusEvent fe) {
				if (searchBar.getText().isEmpty()) {
					searchBar.setFont(promptFont);
					searchBar.setForeground(promptTextColor);
					searchBar.setText(searchBarPrompt);
				}
			}
		});
		searchFolder.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent fe) {
				if (searchFolder.getText().equals(folderPathPrompt)) {
					searchFolder.setText("");
					searchFolder.setFont(inputFont);
					searchFolder.setForeground(inputTextColor);
				}
			}

			@Override
			public void focusLost(FocusEvent fe) {
				if (searchFolder.getText().isEmpty()) {
					searchFolder.setForeground(promptTextColor);
					searchFolder.setText(folderPathPrompt);
				}
			}
		});
	}

	@Override
	public synchronized void update(List<Path> pathBuffer) {
		StringBuilder sb = new StringBuilder();
		try {
			for (Path path : pathBuffer)
				sb.append(path.toString()).append('\n');
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed update in thread: " + Thread.currentThread().getName() + ", failed update for " + pathBuffer.size() + " paths.");
		}

		resultArea.append(sb.toString());
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			FileSearcherApp app = new FileSearcherApp();
			app.setLocationRelativeTo(null);
			app.setVisible(true);
		});
	}
}
