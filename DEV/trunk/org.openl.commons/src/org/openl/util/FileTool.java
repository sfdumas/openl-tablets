package org.openl.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public final class FileTool {

    public static File findCommonParent(File f1, File f2) throws IOException {

        if (f1.equals(f2))
            return f1;

        if (isParentOf(f1, f2))
            return f1;

        if (isParentOf(f2, f1))
            return f2;

        File[] pf1 = parents(f1);
        File[] pf2 = parents(f2);

        int size = Math.min(pf1.length, pf2.length);

        for (int i = 0; i < size; i++) {
            if (!pf1[i].equals(pf2[i])) {
                return (i == 0) ? null : pf1[i - 1];
            }
        }

        return size == 0 ? null : pf1[size - 1];
    }

    public static File buildRelativePath(File startDir, File targetFile) throws IOException {
        if (startDir == null)
            return targetFile.isFile() ? targetFile.getParentFile() : targetFile;

        if (startDir.equals(targetFile))
            return new File(".");

        File[] pfDir = parents(startDir);
        File[] pfTarget = parents(targetFile);

        int size = Math.min(pfDir.length, pfTarget.length);

        int lastEqual = -1;
        for (int i = 0; i < size; i++) {
            if (pfDir[i].equals(pfTarget[i])) {
                lastEqual = i;
            }
        }

        if (lastEqual == -1)
            return targetFile.getAbsoluteFile();

        int stepsToCommonParent = pfDir.length - 1 - lastEqual;

        String path = ".";

        for (int i = 0; i < stepsToCommonParent; ++i) {
            if (i == 0)
                path = "..";
            else
                path += "/..";
        }

        stepsToCommonParent = pfTarget.length - 1 - lastEqual;

        for (int i = 0; i < stepsToCommonParent; i++) {
            path += "/" + pfTarget[lastEqual + i + 1].getName();
        }

        return new File(path);

    }

    static File[] parents(File f) throws IOException {

        f = f.getCanonicalFile();
        List<File> v = new ArrayList<File>();
        v.add(f);

        while ((f = f.getParentFile()) != null)
            v.add(f.getCanonicalFile());

        int size = v.size();
        File[] ff = new File[size];
        for (int i = 0; i < size; i++) {
            ff[i] = v.get(size - 1 - i);
        }

        return ff;

    }

    public static boolean isParentOf(File parent, File child) throws IOException {
        String ps = parent.getCanonicalPath();
        String cs = child.getCanonicalPath();

        return cs.indexOf(ps) == 0;
    }

    public static void copyFileToDir(File src, File dstDir) throws IOException {
        copyFile(src, new File(dstDir.getPath() + File.separator + src.getName()));
    }

    public static void copyFile(File src, File dst) throws IOException {
        File srcCan = src.getCanonicalFile();
        File dstCan = dst.getCanonicalFile();

        if (srcCan.equals(dstCan))
            return;

        dstCan.getParentFile().mkdirs(); // make sure that parent directory
        // exists

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(srcCan);
            fos = new FileOutputStream(dstCan);

            byte[] buf = new byte[8192];
            while (true) {
                int len = fis.read(buf);
                if (len <= 0)
                    break;
                fos.write(buf, 0, len);
            }
        } finally {
            if (fis != null)
                fis.close();
            if (fos != null)
                fos.close();
        }

    }

    static public void copyDir(String srcDir, final String dstDir, FilenameFilter filter) throws Exception {

        DirectoryIterator di = new DirectoryIterator(srcDir, filter);

        DirectoryIterator.FileHandler fh = new DirectoryIterator.FileHandler() {
            public void processFile(File f, String local_path) throws Exception {
                copyFile(f, new File(dstDir + '/' + local_path));
            }

        };

        di.iterate(fh, null);
    }

    static public class CacheMap {

        HashMap<File, LTMValue> _map = new HashMap<File, LTMValue>();

        public void put(File key, Object value) {
            LTMValue ltm = new LTMValue();
            ltm.lastModified = key.lastModified();
            ltm.value = value;
            _map.put(key, ltm);
        }

        public Object get(File key) {
            LTMValue ltm = _map.get(key);
            if (ltm == null || ltm.lastModified != key.lastModified())
                return null;
            return ltm.value;
        }

        static class LTMValue {
            long lastModified;
            Object value;
        }
    }

    public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    public static final int DEFAULT_READ_BUFFER_SIZE = 1024;

    public static String loadFile(Reader reader) throws Exception {
        StringBuffer buf = new StringBuffer(DEFAULT_READ_BUFFER_SIZE);

        char[] c = new char[DEFAULT_READ_BUFFER_SIZE];
        int n;

        while ((n = reader.read(c)) > 0) {
            buf.append(c, 0, n);
        }

        return buf.toString();
    }

    public static void saveFile(String fname, String s) throws Exception {
        saveFile(fname, s, false);
    }

    public static void saveFile(OutputStream os, String s) throws Exception {
        Writer fw = null;

        try {
            fw = new BufferedWriter(new OutputStreamWriter(os, DEFAULT_CHARACTER_ENCODING));
            fw.write(s);
        } finally {
            if (fw != null)
                fw.close();
        }
    }

    public static void saveFile(String fname, String s, boolean compareBeforeSave) throws Exception {
        try {
            if (compareBeforeSave && compareFileSource(fname, s) == 0)
                return;
        } catch (Exception ex) {
            Log.warn(ex);
        }

        File file = new File(fname);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs())
                throw new Exception("Can not create " + parentFile.getAbsolutePath());
        }
        saveFile(new FileOutputStream(file), s);
    }

    public synchronized static String loadFile(String fname) throws Exception {
        return loadFile(new FileInputStream(fname));
    }

    public synchronized static String loadFile(InputStream fs) throws Exception {
        StringBuffer buf = new StringBuffer();
        BufferedReader bis = null;
        try {
            bis = new BufferedReader(new InputStreamReader(fs, DEFAULT_CHARACTER_ENCODING));

            int rd;
            while ((rd = bis.read()) != -1)
                buf.append((char) rd);
        } finally {
            if (bis != null)
                bis.close();
        }
        return buf.toString();
    }

    public synchronized static int compareFileSource(String fname, String src) throws Exception {

        if (!(new File(fname).isFile()))
            return -1;

        FileInputStream fis = null;
        BufferedReader bis = null;

        int result = 0;
        try {
            fis = new FileInputStream(fname);
            bis = new BufferedReader(new InputStreamReader(fis, DEFAULT_CHARACTER_ENCODING));

            int rd;
            int index = -1;
            while ((rd = bis.read()) != -1) {
                index++;
                if (src.length() > index) {
                    if (src.charAt(index) != (char) rd) {
                        result = -1;
                        break;
                    }
                } else {
                    result = 1;
                    break;
                }
            }
            if (result == 0 && src.length() > (index + 1))
                result = -1;
        } finally {
            if (bis != null)
                bis.close();
        }
        return result;
    }

    // added 20.10.03 by SV
    public synchronized static int compareFileSource(String fname, byte[] src) throws Exception {
        if (!(new File(fname).isFile())) {
            return -1;
        }
        FileInputStream fis = null;
        BufferedInputStream bis = null;

        int result = 0;
        try {
            fis = new FileInputStream(fname);
            bis = new BufferedInputStream(fis);

            int rd;
            int index = -1;

            while ((rd = bis.read()) != -1) {
                index++;
                if (src.length > index) {
                    if (src[index] != (byte) rd) {
                        result = -1;
                        break;
                    }
                } else {
                    result = 1;
                    break;
                }
            }
            if (result == 0 && src.length > (index + 1)) {
                result = -1;
            }
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
        return result;
    }

    public static void saveFile(String fname, byte[] src) throws Exception {
        File file = new File(fname);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new Exception("Can not create " + parentFile.getAbsolutePath());
            }
        }
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(src);
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public synchronized static byte[] loadFileToByteArray(String fname) throws Exception {
        FileInputStream fis = null;
        BufferedInputStream bis = null;

        File file = new File(fname);
        byte[] buf = new byte[(int) file.length()];
        // int readBytes = -1;
        try {
            fis = new FileInputStream(fname);
            bis = new BufferedInputStream(fis);
            // readBytes =
            bis.read(buf, 0, buf.length);
        } finally {
            bis.close();
        }
        return buf;
    }

    public synchronized static byte[] loadFileToByteArray(InputStream fis) throws Exception {
        BufferedInputStream bis = null;

        byte[] buf = new byte[fis.available()];
        // int readBytes = -1;
        try {
            bis = new BufferedInputStream(fis);
            // readBytes =
            bis.read(buf, 0, buf.length);
        } finally {
            bis.close();
        }
        return buf;
    }

    public static String internalSeparators(String path) {
        if (path == null)
            return null;
        String tmp = path.replace(File.separatorChar, '/');
        tmp = tmp.replace(File.pathSeparatorChar, ';');
        return tmp;
    }

    public static String systemSeparators(String path) {
        if (path == null)
            return null;
        String tmp = path.replace('/', File.separatorChar);
        tmp = tmp.replace(';', File.pathSeparatorChar);
        return tmp;
    }

    public static void removeDirRecursive(String path) {
        // Log.warn("Removing: "+path);
        File dir = new File(path);
        if (!dir.exists())
            return;
        if (dir.isDirectory()) {
            removeChildren(dir);
            dir.delete();
        }
    }

    /**
     * @param dir
     */
    private static void removeChildren(File dir) {
        File[] file = dir.listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isDirectory())
                removeChildren(file[i]);
            if (file[i].delete() == false)
                Log.warn("Cant delete file/dir: " + file[i].getAbsolutePath());
            // else
            // Log.warn("Deleted: "+file[i].getAbsolutePath());
        }
    }

    final public static String INTERNAL_PATH_SEPARATOR = ";";
    final public static String INTERNAL_SEPARATOR = "/";
    final public static char INTERNAL_PATH_SEPARATOR_CHAR = ';';
    final public static char INTERNAL_SEPARATOR_CHAR = '/';

    static public class DirectoryIterator {

        String _root;
        FilenameFilter _filter;
        Comparator<File> _comp;

        public DirectoryIterator(String root, String[] extensions, Comparator<File> file_comparator) {
            this(root, new ExtensionFilter(extensions), file_comparator);
        }

        public DirectoryIterator(String root, FilenameFilter filter, Comparator<File> file_comparator) {
            _root = root;
            _filter = filter;
            _comp = file_comparator;
            if (_comp == null)
                _comp = new DefaultComparator();
        }

        public DirectoryIterator(String root, FilenameFilter filter) {
            this(root, filter, new DefaultComparator());
        }

        public void iterate(FileHandler fh, DirectoryHandler dh) throws Exception {
            iterateDirectory(_root, "", fh, dh);
        }

        void iterateDirectory(String dir_path, String local_path, FileHandler fh, DirectoryHandler dh) throws Exception {

            File dir = new File(dir_path);

            if (dh != null)
                dh.processDirectory(dir, local_path);

            File[] files = dir.listFiles(_filter);

            Arrays.sort(files, _comp);

            for (int i = 0; i < files.length; ++i) {
                String new_local_path = local_path.length() == 0 ? files[i].getName() : local_path + File.separator
                        + files[i].getName();
                if (files[i].isDirectory()) {
                    iterateDirectory(dir_path + File.separator + files[i].getName(), new_local_path, fh, dh);
                } else {
                    if (fh != null)
                        fh.processFile(files[i], new_local_path);
                }
            }
        }

        static public interface FileHandler {
            void processFile(File f, String local_path) throws Exception;
        }

        static public interface DirectoryHandler {
            void processDirectory(File f, String local_path) throws Exception;
        }

        static class ExtensionFilter implements FilenameFilter {
            String[] _exts;

            ExtensionFilter(String[] exts) {
                _exts = exts;
            }

            public boolean accept(File dir, String name) {

                if (new File(dir.getPath() + File.separator + name).isDirectory())
                    return true;
                for (int i = 0; i < _exts.length; ++i) {
                    if (name.endsWith("." + _exts[i]))
                        return true;
                }

                return false;
            }
        }

        static class DefaultComparator implements Comparator<File> {
            public int compare(File f1, File f2) {

                int res = 0;

                if (f1.isDirectory())
                    res -= 10000;

                if (f2.isDirectory())
                    res += 10000;

                if (res != 0)
                    return res;

                return f1.getName().compareTo(f2.getName());
            }
        }

    }

}