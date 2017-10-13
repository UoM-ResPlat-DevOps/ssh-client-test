package ssh.client.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PathUtils {

    public final static char SLASH_CHAR = '/';
    public final static char BACK_SLASH_CHAR = '\\';

    public static final String getParent(String path) {
        return getParent(path, SLASH_CHAR);
    }

    public static final String getParent(String path, char separator) {
        if (path != null) {
            String p = normalise(path, separator);
            int idx = p.lastIndexOf(separator);
            if (idx <= 0) {
                return p;
            } else {
                return p.substring(0, idx);
            }
        }
        return null;
    }

    public static final String normalise(String path) {
        return normalise(path, BACK_SLASH_CHAR, SLASH_CHAR);
    }

    public static final String normalise(String path, char separator) {
        return normalise(path, separator, separator);
    }

    public static final String normalise(String path, char separator, char newSeparator) {
        if (path != null) {
            String p = path.trim();
            if (separator != newSeparator) {
                p = p.replace(separator, newSeparator);
            }
            p.replaceAll("\\" + newSeparator + "{2,}", "\\" + newSeparator);
            return p;
        }
        return null;
    }

    public static final String trimTrailingSlash(String path) {
        return trimTrailingSeparator(path, SLASH_CHAR);
    }

    public static final String trimTrailingSeparator(String path, char separator) {
        if (path != null) {
            String p = normalise(path, separator);
            if (p.length() > 1) {
                return p.replaceAll("\\" + separator + "+$", "");
            } else {
                return p;
            }
        }
        return null;
    }

    public static final String trimLeadingSlash(String path) {
        return trimLeadingSeparator(path, SLASH_CHAR);
    }

    public static final String trimLeadingSeparator(String path, char separator) {
        if (path != null) {
            String p = normalise(path, separator);
            if (p.length() > 1) {
                return p.replaceAll("^\\" + separator + "+", "");
            } else {
                return p;
            }
        }
        return null;
    }

    public static final String trimSlash(String path) {
        return trimSeparator(path, SLASH_CHAR);
    }

    public static final String trimSeparator(String path, char separator) {
        return trimLeadingSeparator(trimTrailingSeparator(path, separator), separator);
    }

    public static String getRelativePath(Path file, Path baseDir) {
        return getRelativePath(file != null ? file.toString() : null, baseDir != null ? baseDir.toString() : null);
    }

    public static final String getRelativePath(String path, String baseDirPath) {
        return getRelativePath(path, baseDirPath, SLASH_CHAR);
    }

    public static final String getRelativePath(String path, String baseDirPath, char separator) {
        if (path == null) {
            return path;
        }
        String p = normalise(path, separator);
        if (baseDirPath == null) {
            return p;
        }
        String base = normalise(baseDirPath, separator);
        base = trimTrailingSeparator(base, separator);
        if (p.equals(base) || base.length() >= p.length()) {
            return p;
        }
        if (p.startsWith(base)) {
            String relativePath = p.substring(base.length());
            relativePath = trimLeadingSeparator(relativePath, separator);
            return relativePath;
        }
        return p;
    }

    public static final String join(char separator, String... paths) {
        if (paths == null || paths.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            if (i == 0) {
                sb.append(trimTrailingSeparator(paths[i], separator));
            } else {
                sb.append(separator).append(trimSeparator(paths[i], separator));
            }
        }
        return sb.toString();
    }

    public static final String join(String... paths) {
        return join(SLASH_CHAR, paths);
    }

    public static final String join(char separator, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int size = paths.size();
        for (int i = 0; i < size; i++) {
            if (i == 0) {
                sb.append(trimTrailingSeparator(paths.get(i), separator));
            } else {
                sb.append(separator).append(trimSeparator(paths.get(i), separator));
            }
        }
        return sb.toString();
    }

    public static final String join(List<String> paths) {
        return join(SLASH_CHAR, paths);
    }

    public static String[] split(String path, char separator) {
        if (path == null) {
            return new String[] { path };
        }
        String p = normalise(path, separator);
        if (p.isEmpty() || p.equals(String.valueOf(separator)) || path.indexOf(separator) == -1) {
            return new String[] { p };
        }
        return trimSeparator(p, separator).split(String.valueOf(separator));
    }

    public static String[] split(String path) {
        return split(path, SLASH_CHAR);
    }

    public static String getLastComponent(String path, char separator) {
        if (path == null) {
            return path;
        }
        String p = normalise(path, separator);
        if (p.isEmpty()) {
            return p;
        }
        p = trimTrailingSeparator(p, separator);
        int idx = p.lastIndexOf(separator);
        if (idx == -1 || p.equals(String.valueOf(separator))) {
            return p;
        }
        return p.substring(idx + 1);
    }

    public static String getLastComponent(String path) {
        return getLastComponent(path, SLASH_CHAR);
    }

    public static String getFileExtension(String path) {
        if (path == null) {
            return path;
        }
        String p = path.trim();
        int idx = p.lastIndexOf('.');
        if (idx <= 0) {
            return null;
        } else {
            return p.substring(idx + 1);
        }
    }

    public static String removeFileExtension(String path) {
        if (path == null) {
            return path;
        }
        String p = path.trim();
        int idx = p.lastIndexOf('.');
        if (idx <= 0) {
            return null;
        } else {
            return p.substring(0, idx);
        }
    }

    public static boolean equalsOrIsDescendant(String a, String b) {
        String sa = b == null ? null : normalise(b);
        String sb = a == null ? null : normalise(a);
        if (sa != null && sb != null) {
            return sb.startsWith(sa + SLASH_CHAR);
        }
        return false;
    }

    public static boolean equals(String a, String b) {
        String sa = b == null ? null : normalise(b);
        String sb = a == null ? null : normalise(a);
        if (sa != null && sb != null) {
            return sa.equals(sb);
        } else if (sa == null && sb == null) {
            return true;
        }
        return false;
    }

    public static List<String> listParents(String path, boolean ascending) {
        return listParents(path, SLASH_CHAR, ascending);
    }

    public static List<String> listParents(String path, char separator, boolean ascending) {
        if (path == null) {
            return null;
        }
        String p = normalise(path, separator);
        if (p.isEmpty() || p.indexOf(separator) == -1 || p.equals(String.valueOf(separator))) {
            return null;
        }
        boolean leadingSeparator = p.startsWith(String.valueOf(separator));
        List<String> parents = new ArrayList<String>();
        String[] components = split(p, separator);
        if (ascending) {
            for (int i = 0; i < components.length - 1; i++) {
                StringBuilder sb = new StringBuilder();
                if (leadingSeparator) {
                    sb.append(separator);
                }
                sb.append(join(components, separator, 0, i + 1));
                parents.add(sb.toString());
            }
        } else {
            for (int i = components.length - 1; i >= 0; i--) {
                StringBuilder sb = new StringBuilder();
                if (leadingSeparator) {
                    sb.append(separator);
                }
                sb.append(join(components, separator, 0, i));
                parents.add(sb.toString());
            }
        }
        return parents;
    }

    private static String join(String[] components, char separator, int offset, int length) {
        if (components == null || components.length == 0 || offset >= components.length) {
            return null;
        }
        int end = offset + length;
        if (end > components.length) {
            end = components.length;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < end; i++) {
            if (i == 0) {
                sb.append(components[i]);
            } else {
                sb.append(separator).append(components[i]);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Throwable {
        List<String> ps = listParents("/a/b/c", SLASH_CHAR, true);
        for (String p : ps) {
            System.out.println(p);
        }
    }

}
