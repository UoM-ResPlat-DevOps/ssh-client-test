package ssh.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class StreamUtils {

    public static final int BUFFER_SIZE = 8192;

    public static void copy(InputStream in, OutputStream out, boolean closeInput, boolean closeOutput)
            throws Throwable {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            Throwable t = null;
            if (closeInput) {
                try {
                    in.close();
                } catch (Throwable ex) {
                    t = ex;
                }
            }
            if (closeOutput) {
                try {
                    out.close();
                } catch (Throwable ex) {
                    t = ex;
                }
            }
            if (t != null) {
                throw t;
            }
        }
    }

    public static void copy(InputStream in, OutputStream out) throws Throwable {
        copy(in, out, false, false);
    }

    public static void copy(InputStream in, File outputFile, boolean closeInput) throws IOException {
        try {
            Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (closeInput) {
                in.close();
            }
        }
    }

    public static void copy(InputStream in, File outputFile) throws IOException {
        copy(in, outputFile, false);
    }

    public static void copy(File inputFile, OutputStream out, boolean closeOutput) throws IOException {
        try {
            Files.copy(inputFile.toPath(), out);
        } finally {
            if (closeOutput) {
                out.close();
            }
        }
    }

    public static void copy(File inputFile, OutputStream out) throws IOException {
        copy(inputFile, out, false);
    }

    public static void copy(File inputFile, File outputFile) throws IOException {
        Files.copy(inputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void exhaustInputStream(InputStream in) throws IOException {
        if (in == null) {
            return;
        }
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (in.read(buffer) >= 0) {
            }
        } finally {
            in.close();
        }
    }

    public static String readString(InputStream in, String charsetName) throws Throwable {

        // @formatter:off
//        ByteArrayOutputStream result = new ByteArrayOutputStream();
//        byte[] buffer = new byte[1024];
//        int length;
//        while ((length = in.read(buffer)) != -1) {
//            result.write(buffer, 0, length);
//        }
//        return result.toString(charsetName);
        // @formatter:on

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, charsetName == null ? "UTF-8" : charsetName));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    public static String readString(InputStream in) throws Throwable {
        return readString(in, "UTF-8");
    }

}
