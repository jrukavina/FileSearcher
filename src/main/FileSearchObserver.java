package main;

import java.nio.file.Path;
import java.util.List;

public interface FileSearchObserver {
	
	public void update(List<Path> pathBuffer);

}
