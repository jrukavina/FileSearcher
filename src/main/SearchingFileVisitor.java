package main;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SearchingFileVisitor extends SimpleFileVisitor<Path> implements FileSearcher {

	private String pattern;
	private List<Path> foundPaths;
	private List<FileSearchObserver> observers;
	private boolean foldersIncluded, isStopped;

	private int bufferSize = 8;
	private int counter;
	private boolean manyMatches;
	private List<Path> pathBuffer = new ArrayList<>(bufferSize);

	public SearchingFileVisitor(FileSearchObserver o) {
		this();
		this.attach(o);
	}

	public SearchingFileVisitor() {
		foundPaths = new LinkedList<>();
		observers = new ArrayList<>();
		foldersIncluded = false;
		isStopped = false;
	}

	public void attach(FileSearchObserver o) {
		observers.add(o);
	}

	public void detach(FileSearchObserver o) {
		if (observers.contains(o))
			observers.remove(o);
	}

	private synchronized void notifyObservers() {
		for (FileSearchObserver o : observers)
			o.update(pathBuffer);
		pathBuffer.clear();
	}

	private void notifyEfficiently(Path path) {
		if (!manyMatches) {
			counter++;
			pathBuffer.add(path);
			notifyObservers();
			if (counter == 4) {
				manyMatches = true;
				counter = 0;
			}
		} else {
			pathBuffer.add(path);
			if (counter != bufferSize) {
				counter++;
			} else {
				counter = 0;
				notifyObservers();
				bufferSize *= 2;
			}
		}
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
		if (file.getFileName().toString().toLowerCase().contains(pattern)) {
			foundPaths.add(file);
			notifyEfficiently(file);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
		if (isStopped) {
			notifyObservers();
			return FileVisitResult.TERMINATE;
		}

		if (foldersIncluded && dir.getFileName() != null
				&& dir.getFileName().toString().toLowerCase().contains(pattern)) {
			foundPaths.add(dir);
			notifyEfficiently(dir);
		}

		return FileVisitResult.CONTINUE;
	}

	public boolean isFoldersIncluded() {
		return foldersIncluded;
	}

	public void setFoldersIncluded(boolean foldersIncluded) {
		this.foldersIncluded = foldersIncluded;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
//		System.err.printf("Visiting failed for %s\n", file);
		return FileVisitResult.SKIP_SUBTREE;
	}

	public String getPattern() {
		return pattern;
	}

	public List<Path> getFoundPaths() {
		return foundPaths;
	}

	public void stop() {
		isStopped = true;
	}

	public void search(String pattern, Path dir) {
		this.pattern = pattern;
		isStopped = false;
		foundPaths.clear();
		counter = 0;
		manyMatches = false;
		try {
			Files.walkFileTree(dir, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (pathBuffer.size() > 0)
			notifyObservers();
	}
}
