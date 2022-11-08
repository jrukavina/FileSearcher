# FileSearcher
Simple GUI file searcher/explorer app written in Java.

This project was mainly made to circumvent Windows 10's built-in file explorer which relies on indexing and often misses searched files and folders. FileSearcher will always search files and subfolders in a given folder path. This means that it will always find a searched item (if it exists) at the cost of using more CPU and disk resources.

### Base GUI example
![alt text](https://github.com/jrukavina/FileSearcher/blob/main/figs/base_gui.png?raw=true)

Left text field requires a string input to use as a search pattern. Right text field is optional and can be used to search only certain folders. Default will search whole C disk.



### Result of search pattern "obs studio"
![alt text](https://github.com/jrukavina/FileSearcher/blob/main/figs/search_example.png?raw=true)

Note that search patterns are not case sensitive.

You can open found files by clicking on them, clicking on folders will open them in file explorer.
