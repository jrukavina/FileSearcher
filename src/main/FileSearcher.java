package main;

import java.nio.file.Path;
import java.util.List;

public interface FileSearcher {
	
	public void search(String Pattern, Path dir);
	
	public boolean isFoldersIncluded();
	
	public void setFoldersIncluded(boolean foldersIncluded);

	public String getPattern();

	public List<Path> getFoundPaths();
	
	public void stop();
	
	public void attach(FileSearchObserver o);
	
	public void detach(FileSearchObserver o);
	
}
