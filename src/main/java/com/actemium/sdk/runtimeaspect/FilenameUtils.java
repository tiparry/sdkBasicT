package com.actemium.sdk.runtimeaspect;


import java.io.File;

public class FilenameUtils {

    private static final int NOT_FOUND = -1;
    private static final char UNIX_SEPARATOR = '/';
    private static final char WINDOWS_SEPARATOR = '\\';
    private static final char SYSTEM_SEPARATOR = File.separatorChar;
    private static final char OTHER_SEPARATOR;
    static {
        if (isSystemWindows()) {
            OTHER_SEPARATOR = UNIX_SEPARATOR;
        } else {
            OTHER_SEPARATOR = WINDOWS_SEPARATOR;
        }
    }

    /**
     * Instances should NOT be constructed in standard programming.
     */
    private FilenameUtils() {
        super();
    }

   
    static boolean isSystemWindows() {
        return SYSTEM_SEPARATOR == WINDOWS_SEPARATOR;
    }

    private static boolean isSeparator(final char ch) {
        return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
    }

	private static void failIfNullBytePresent(String path) {
	    int len = path.length();
	    for (int i = 0; i < len; i++) {
	        if (path.charAt(i) == 0) {
	            throw new IllegalArgumentException("Null byte present in file/path name. There are no " +
	                    "known legitimate use cases for such data, but several injection attacks may use it");
	        }
	    }
	}


	/**
	 * Returns the length of the filename prefix, such as <code>C:/</code> or <code>~/</code>.
	 * <p>
	 * This method will handle a file in either Unix or Windows format.
	 * <p>
	 * The prefix length includes the first slash in the full filename
	 * if applicable. Thus, it is possible that the length returned is greater
	 * than the length of the input string.
	 * <pre>
	 * Windows:
	 * a\b\c.txt           --&gt; ""          --&gt; relative
	 * \a\b\c.txt          --&gt; "\"         --&gt; current drive absolute
	 * C:a\b\c.txt         --&gt; "C:"        --&gt; drive relative
	 * C:\a\b\c.txt        --&gt; "C:\"       --&gt; absolute
	 * \\server\a\b\c.txt  --&gt; "\\server\" --&gt; UNC
	 * \\\a\b\c.txt        --&gt;  error, length = -1
	 *
	 * Unix:
	 * a/b/c.txt           --&gt; ""          --&gt; relative
	 * /a/b/c.txt          --&gt; "/"         --&gt; absolute
	 * ~/a/b/c.txt         --&gt; "~/"        --&gt; current user
	 * ~                   --&gt; "~/"        --&gt; current user (slash added)
	 * ~user/a/b/c.txt     --&gt; "~user/"    --&gt; named user
	 * ~user               --&gt; "~user/"    --&gt; named user (slash added)
	 * //server/a/b/c.txt  --&gt; "//server/"
	 * ///a/b/c.txt        --&gt; error, length = -1
	 * </pre>
	 * <p>
	 * The output will be the same irrespective of the machine that the code is running on.
	 * ie. both Unix and Windows prefixes are matched regardless.
	 *
	 * Note that a leading // (or \\) is used to indicate a UNC name on Windows.
	 * These must be followed by a server name, so double-slashes are not collapsed
	 * to a single slash at the start of the filename.
	 *
	 * @param filename  the filename to find the prefix in, null returns -1
	 * @return the length of the prefix, -1 if invalid or null
	 */
	public static int getPrefixLength(final String filename) {
	    if (filename == null) {
	        return NOT_FOUND;
	    }
	    final int len = filename.length();
	    if (len == 0) {
	        return 0;
	    }
	    char ch0 = filename.charAt(0);
	    if (ch0 == ':') {
	        return NOT_FOUND;
	    }
	    if (len == 1) {
	        if (ch0 == '~') {
	            return 2;  // return a length greater than the input
	        }
	        return isSeparator(ch0) ? 1 : 0;
	    } else {
	        if (ch0 == '~') {
	            int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
	            int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
	            if (posUnix == NOT_FOUND && posWin == NOT_FOUND) {
	                return len + 1;  // return a length greater than the input
	            }
	            posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
	            posWin = posWin == NOT_FOUND ? posUnix : posWin;
	            return Math.min(posUnix, posWin) + 1;
	        }
	        final char ch1 = filename.charAt(1);
	        if (ch1 == ':') {
	            ch0 = Character.toUpperCase(ch0);
	            if (ch0 >= 'A' && ch0 <= 'Z') {
	                if (len == 2 || !isSeparator(filename.charAt(2))) {
	                    return 2;
	                }
	                return 3;
	            } else if (ch0 == UNIX_SEPARATOR) {
	                return 1;
	            }
	            return NOT_FOUND;
	
	        } else if (isSeparator(ch0) && isSeparator(ch1)) {
	            int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
	            int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
	            if (posUnix == NOT_FOUND && posWin == NOT_FOUND || posUnix == 2 || posWin == 2) {
	                return NOT_FOUND;
	            }
	            posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
	            posWin = posWin == NOT_FOUND ? posUnix : posWin;
	            return Math.min(posUnix, posWin) + 1;
	        } else {
	            return isSeparator(ch0) ? 1 : 0;
	        }
	    }
	}

	
    /**
     * Normalizes a path, removing double and single dot path steps.
     * <p>
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     * <p>
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, {@code null}
     * is returned.
     * <p>
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               --&gt;   /foo/
     * /foo/./              --&gt;   /foo/
     * /foo/../bar          --&gt;   /bar
     * /foo/../bar/         --&gt;   /bar/
     * /foo/../bar/../baz   --&gt;   /baz
     * //foo//./bar         --&gt;   /foo/bar
     * /../                 --&gt;   null
     * ../foo               --&gt;   null
     * foo/bar/..           --&gt;   foo/
     * foo/../../bar        --&gt;   null
     * foo/../bar           --&gt;   bar
     * //server/foo/../bar  --&gt;   //server/bar
     * //server/../bar      --&gt;   null
     * C:\foo\..\bar        --&gt;   C:\bar
     * C:\..\bar            --&gt;   null
     * ~/foo/../bar/        --&gt;   ~/bar/
     * ~/../bar             --&gt;   null
     * </pre>
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param filename  the filename to normalize, null returns null
     * @return the normalized filename, or null if invalid. Null bytes inside string will be removed
     */
    public static String normalize(final String filename) {
        if (filename == null) {
            return null;
        }

        failIfNullBytePresent(filename);

        int size = filename.length();
        if (size == 0) {
            return filename;
        }
        final int prefix = getPrefixLength(filename);
        if (prefix < 0) {
            return null;
        }

        final char[] array = new char[size + 2];  // +1 for possible extra slash, +2 for arraycopy
        filename.getChars(0, filename.length(), array, 0);

        // fix separators throughout
        final char otherSeparator = OTHER_SEPARATOR;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == otherSeparator) {
                array[i] = SYSTEM_SEPARATOR;
            }
        }

        // add extra separator on the end to simplify code below
        boolean lastIsDirectory = true;
        if (array[size - 1] != SYSTEM_SEPARATOR) {
            array[size++] = SYSTEM_SEPARATOR;
            lastIsDirectory = false;
        }

        // adjoining slashes
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == SYSTEM_SEPARATOR && array[i - 1] == SYSTEM_SEPARATOR) {
                System.arraycopy(array, i, array, i - 1, size - i);
                size--;
                i--;
            }
        }

        // dot slash
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == SYSTEM_SEPARATOR && array[i - 1] == '.' &&
                    (i == prefix + 1 || array[i - 2] == SYSTEM_SEPARATOR)) {
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
            if (array[i] == SYSTEM_SEPARATOR && array[i - 1] == '.' && array[i - 2] == '.' &&
                    (i == prefix + 2 || array[i - 3] == SYSTEM_SEPARATOR)) {
                if (i == prefix + 2) {
                    return null;
                }
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                int j;
                for (j = i - 4 ; j >= prefix; j--) {
                    if (array[j] == SYSTEM_SEPARATOR) {
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
}
