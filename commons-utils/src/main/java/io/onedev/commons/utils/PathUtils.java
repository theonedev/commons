package io.onedev.commons.utils;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Splitter;

public class PathUtils {

    /**
     * Match specified path against a collection of base paths to find out the longest match.
     * For instance, if collection contains /path1/path2/path3 and /path1/path2, the eager
     * match for /path1/path2/path3/path4 should be /path1/path2/path3. 
     * 
     * @param basePaths
     * 			Collection of base paths to match against specified path.
     * @param path
     * 			The path used for eager matching.
     * @return
     * 			The longest matching base path. Null if no any matched base paths.
     */
    public static String matchLongest(Collection<String> basePaths, String path) {
    	int unmatchedSegments = Integer.MAX_VALUE;
    	String matchedBasePath = null;
    	
    	for (String each: basePaths) {
    		String relative = parseRelative(path, each);
    		if (relative != null) {
    			int currentSegments = Splitter.on("/").omitEmptyStrings().splitToList(relative).size();
    			if (unmatchedSegments > currentSegments) {
    				unmatchedSegments = currentSegments;
    				matchedBasePath = each;
    			}
    		}
    	}
    	
    	return matchedBasePath;
    }

    /**
     * Determines if specified longer path is relative to the shorter path. 
     *
     * @param shortPath 
     * 			A string representing shorter path, can be a relative path. The path 
     * 			separator does not matter, and will be converted to "/" at comparing time. 
     * 
     * @param longPath 
     * 			A string representing longer path, can be a relative path. The path 
     * 			separator does not matter, and will be converted to "/" at comparing time.
     * @return 
     * 			path of longer relative to shorter, with path separator being converted to "/", 
     * 			and leading character will be "/" if longer is relative to shorter. A null value will be 
     * 			returned if longer is not relative to shorter, or "" if shorter is the same as longer
     */
    public static String parseRelative(String longPath, String shortPath) {
    	shortPath = FilenameUtils.normalizeNoEndSeparator(shortPath);
		if (shortPath == null)
			return null;
    	shortPath = StringUtils.replace(shortPath.trim(), "\\", "/");
    	if (shortPath.length() != 0 && shortPath.charAt(0) != '/')
    		shortPath = "/" + shortPath;
    	if (shortPath.endsWith("/"))
    		shortPath = StringUtils.stripEnd(shortPath, "/");

    	longPath = FilenameUtils.normalizeNoEndSeparator(longPath);
		if (longPath == null)
			return null;
    	longPath = StringUtils.replace(longPath.trim(), "\\", "/");
    	if (longPath.length() != 0 && longPath.charAt(0) != '/')
    		longPath = "/" + longPath;
    	if (longPath.endsWith("/"))
    		longPath = StringUtils.stripEnd(longPath, "/");
        
		if (!longPath.startsWith(shortPath))
			return null;
		
		String relativePath = longPath.substring(shortPath.length());
		if (relativePath.length() == 0)
			return relativePath;
		
		if (relativePath.charAt(0) != '/')
			return null;
		else
			return relativePath;
    }
    
    public @Nullable static LinearRange matchSegments(String path, String match, boolean ignoreExt) {
    	int beginIndex = path.indexOf(match);
    	while (beginIndex != -1) {
    		int endIndex = beginIndex+match.length();
    		char leftCh = beginIndex==0?'/':path.charAt(beginIndex-1);
    		char rightCh = endIndex==path.length()?'/':path.charAt(endIndex);
    		if (leftCh == '/' && (rightCh=='/' || ignoreExt&&rightCh=='.'))
    			return new LinearRange(beginIndex, endIndex);
    		beginIndex = path.indexOf(match, beginIndex+1);
    	}
    	return null;
    }
    
    /**
	 * Normalize a path separated with forward slash to remove single and double dots. 
	 * Adapted from FilenameUtils.normalize(filename)
	 * 
	 * @param path
	 * @return
	 */
	@Nullable
	public static String normalizeDots(@Nullable String path) {
        if (path == null) {
            return null;
        }
        int size = path.length();
        if (size == 0) {
            return path;
        }
        
        int prefix = path.startsWith("/")?1:0;
        
        char[] array = new char[size + 2];  // +1 for possible extra slash, +2 for arraycopy
        path.getChars(0, path.length(), array, 0);
        
        // add extra separator on the end to simplify code below
        boolean lastIsDirectory = true;
        if (array[size - 1] != '/') {
            array[size++] = '/';
            lastIsDirectory = false;
        }
        
        // adjoining slashes
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == '/' && array[i - 1] == '/') {
                System.arraycopy(array, i, array, i - 1, size - i);
                size--;
                i--;
            }
        }
        
        // dot slash
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == '/' && array[i - 1] == '.' &&
                    (i == prefix + 1 || array[i - 2] == '/')) {
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                System.arraycopy(array, i + 1, array, i - 1, size - i);
                size -=2;
                i--;
            }
        }
        
        // double dot slash
        outer:
        for (int i = prefix + 2; i < size; i++) {
            if (array[i] == '/' && array[i - 1] == '.' && array[i - 2] == '.' &&
                    (i == prefix + 2 || array[i - 3] == '/')) {
                if (i == prefix + 2) {
                    return null;
                }
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                int j;
                for (j = i - 4 ; j >= prefix; j--) {
                    if (array[j] == '/') {
                        // remove b/../ from a/b/../c
                        System.arraycopy(array, i + 1, array, j + 1, size - i);
                        size -= i - j;
                        i = j + 1;
                        continue outer;
                    }
                }
                // remove a/../ from a/../c
                System.arraycopy(array, i + 1, array, prefix, size - i);
                size -= i + 1 - prefix;
                i = prefix + 1;
            }
        }
        
        if (size <= 0) {  // should never be less than 0
            return "";
        }
        if (size <= prefix) {  // should never be less than prefix
            return new String(array, 0, size);
        }
        if (lastIsDirectory) {
            return new String(array, 0, size);  // keep trailing separator
        }
        return new String(array, 0, size - 1);  // lose trailing separator		
	}
	
	public static String resolveSibling(@Nullable String base, @Nullable String path) {
		if (base == null)
			base = "";
		if (path == null)
			path = "";
		
		if (base.indexOf('/') == -1 || path.startsWith("/")) {
			return path;
		} else if (base.endsWith("/")) {
			return base + path;
		} else {
			return StringUtils.substringBeforeLast(base, "/") + "/" + path;
		} 
	}
	
	public static String relativize(@Nullable String base, @Nullable String path) {
		if (base == null)
			base = "";
		if (path == null)
			path = "";
		
		List<String> baseSegments = Splitter.on("/").splitToList(base);
		List<String> pathSegments = Splitter.on("/").splitToList(path);
		int index = baseSegments.size();
		for (int i=0; i<baseSegments.size(); i++) {
			if (i >= pathSegments.size() || !baseSegments.get(i).equals(pathSegments.get(i))) {
				index = i;
				break;
			}
		}
		StringBuilder builder = new StringBuilder();
		for (int i=index; i<baseSegments.size(); i++) {
			if (baseSegments.get(i).length() != 0) {
				if (builder.length() != 0)
					builder.append("/");
				builder.append("..");
			}
		}
		for (int i=index; i<pathSegments.size(); i++) {
			if (pathSegments.get(i).length() != 0) {
				if (builder.length() != 0)
					builder.append("/");
				builder.append(pathSegments.get(i));
			}
		}
		return builder.toString();
	}

	public static int compare(List<String> pathSegments1, List<String> pathSegments2) {
		for (int i=0; i<pathSegments1.size(); i++) {
			if (i<pathSegments2.size()) {
				int compareResult = pathSegments1.get(i).compareTo(pathSegments2.get(i));
				if (compareResult != 0)
					return compareResult;
			} else {
				return 1;
			}
		}
		return pathSegments2.size()==pathSegments1.size()? 0: -1;
	}

	public static String resolve(@Nullable String basePath, @Nullable String path) {
		if (basePath == null)
			basePath = "";
		if (path == null)
			path = "";
		if (FilenameUtils.getPrefixLength(path) != 0) 
			return path;
		else if (basePath.endsWith("/") || basePath.endsWith("\\")) 
			return basePath + path;
		else 
			return basePath + "/" + path;
	}
	
	public static boolean isCurrent(@Nullable String path) {
		return StringUtils.isBlank(path) || FilenameUtils.normalize(path).length() == 0;
	}	
	
	public static boolean isSelfOrAncestor(String ancestor, String path) {
		return path.equals(ancestor) || path.startsWith(ancestor+"/");
	}

	public static String substituteSelfOrAncestor(String path, String oldAncestor, String newAncestor) {
		if (isSelfOrAncestor(oldAncestor, path)) 
			return newAncestor + path.substring(oldAncestor.length());
		else
			return path;
	}

	public static boolean isSubPath(String path) {
		return !path.contains("..") && !new File(path).isAbsolute();
	}

}
