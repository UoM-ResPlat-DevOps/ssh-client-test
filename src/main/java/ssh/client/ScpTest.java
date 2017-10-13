package ssh.client;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPOutputStream;
import ch.ethz.ssh2.Session;
import ssh.client.util.PathUtils;
import ssh.client.util.StreamUtils;

public class ScpTest {

    public static final String APP = "scp-test";

    public static final String ENCODING = "UTF-8";

    public static ch.ethz.ssh2.Connection connect(String host, int port, String user, String password)
            throws Throwable {
        ch.ethz.ssh2.Connection cxn = new ch.ethz.ssh2.Connection(host, port);
        long t = System.currentTimeMillis();
        cxn.connect(null);
        System.out.println(String.format("%32s    %8d ms", "ssh.connect: ", (System.currentTimeMillis() - t)));
        t = System.currentTimeMillis();
        cxn.authenticateWithPassword(user, password);
        System.out.println(String.format("%32s    %8d ms", "ssh.authenticate: ", (System.currentTimeMillis() - t)));
        return cxn;
    }

    public static void scpPut(Session session, InputStream in, long length, String dstPath, String mode)
            throws Throwable {

    }

    public static void scpPut(Connection cxn, InputStream in, long length, String dstPath, String mode)
            throws Throwable {
        String path = PathUtils.normalise(dstPath);
        String dstFileName = PathUtils.getLastComponent(path);
        String dstDirPath = PathUtils.getParent(path);
        /*
         * mkdirs
         */
        long t = System.currentTimeMillis();
        Session session = cxn.openSession();
        System.out.println(String.format("%32s    %8d ms", "ssh.mkdir.session.open: ", (System.currentTimeMillis() - t)));
        try {
            t = System.currentTimeMillis();
            session.execCommand("mkdir -p " + dstDirPath);
            System.out.println(String.format("%32s    %8d ms", "ssh.mkdir: ", (System.currentTimeMillis() - t)));
        } finally {
            t = System.currentTimeMillis();
            session.close();
            System.out.println(
                    String.format("%32s    %8d ms", "ssh.mkdir.session.close: ", (System.currentTimeMillis() - t)));
        }
        /*
         * scp
         */
        SCPClient scpc = cxn.createSCPClient();
        t = System.currentTimeMillis();
        Session scpsession = cxn.openSession();
        System.out.println(String.format("%32s    %8d ms", "scp.session.open: ", (System.currentTimeMillis() - t)));
        t = System.currentTimeMillis();
        OutputStream scpos = createScpOutputStream(scpc, scpsession, length, dstFileName, dstDirPath, mode);
        System.out.println(String.format("%32s    %8d ms", "scp.initial.response: ", (System.currentTimeMillis() - t)));
        try {
            t = System.currentTimeMillis();
            StreamUtils.copy(in, scpos);
            System.out.println(String.format("%32s    %8d ms", "scp.transfer: ", (System.currentTimeMillis() - t)));
        } finally {
            t = System.currentTimeMillis();
            scpos.close();
            System.out.println(String.format("%32s    %8d ms", "scp.session.close: ", (System.currentTimeMillis() - t)));
        }
    }

    private static OutputStream createScpOutputStream(SCPClient client, Session session, long fileLength,
            String dstFileName, String dstDirPath, String mode) throws Throwable {
        if (dstFileName == null) {
            throw new IllegalArgumentException("Destination file name is null.");
        }
        if (dstDirPath == null || dstDirPath.trim().isEmpty()) {
            dstDirPath = ".";
        }
        if (mode == null) {
            mode = "0600";
        }
        if (!mode.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Invalid file mode: " + mode);
        }
        // String cmd = "scp -t -d \"" + dstDirPath.trim() + "\"";
        String cmd = "scp -t " + PathUtils.join(dstDirPath, dstFileName);
        session.execCommand(cmd, ENCODING);
        return new SCPOutputStream(client, session, dstFileName, fileLength, mode);
    }

    public static void scpPut(Connection cxn, File file, String dstPath) throws Throwable {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            scpPut(cxn, in, file.length(), dstPath, "0644");
        } catch (Throwable e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e);
            }
        } finally {
            in.close();
        }
    }

    public static void scpPutFile(Connection cxn, Path file, String dstDirPath) throws Throwable {
        String fileName = file.toFile().getName();
        String dstPath = dstDirPath != null ? PathUtils.join(dstDirPath, fileName) : fileName;
        scpPut(cxn, file.toFile(), dstPath);
    }

    public static void scpPutDir(final Connection cxn, final Path dir, final String dstBaseDirPath) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                String relativePath = PathUtils.getRelativePath(file, dir);
                String dstPath = dstBaseDirPath != null ? PathUtils.join(dstBaseDirPath, relativePath) : relativePath;
                try {
                    scpPut(cxn, file.toFile(), dstPath);
                } catch (Throwable e) {
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    } else {
                        throw new IOException(e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException ioe) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException ioe) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void main(String[] args) throws Throwable {
        try {

            long startTime = System.currentTimeMillis();

            if (args.length < 2) {
                throw new IllegalArgumentException("Missing arguments.");
            }

            String[] dst = parseDestination(args[args.length - 1]);
            String user = dst[0];
            String host = dst[1];
            int port = 22;
            String dstDir = dst[2];

            List<Path> inputs = new ArrayList<Path>();
            for (int i = 0; i < args.length - 1;) {
                if ("-P".equals(args[i])) {
                    String p = args[i + 1];
                    try {
                        port = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid port: " + p);
                    }
                    i += 2;
                } else {
                    Path input = Paths.get(args[i]);
                    if (!Files.exists(input)) {
                        throw new IllegalArgumentException(
                                new FileNotFoundException("Input file/directory " + input + " does not exist."));
                    }
                    inputs.add(input);
                    i++;
                }
            }
            if (user == null) {
                user = System.getProperty("user.name");
            }

            if (user == null) {
                user = readUsernameFromConsole();
            }

            String password = readPasswordFromConsole();

            Connection cxn = connect(host, port, user, password);

            try {
                for (Path input : inputs) {
                    if (Files.isDirectory(input)) {
                        scpPutDir(cxn, input, dstDir);
                    } else {
                        scpPutFile(cxn, input, dstDir);
                    }
                }
            } finally {
                cxn.close();
            }
            long endTime = System.currentTimeMillis();
            System.out.println(
                    String.format("%32s    %8d ms", "total: ", endTime-startTime));
        } catch (IllegalArgumentException iae) {
            System.err.println("Error: " + iae.getMessage());
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("");
        System.out.println("Usage: " + APP + " [-P port] <file|dir> [user@]host:dir");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("    " + APP + " ~/file1.txt spartan.hpc.unimelb.edu.au:test");
        System.out.println("    " + APP + " ~/dir1/ spartan.hpc.unimelb.edu.au:test");
        System.out.println("");
    }

    private static String[] parseDestination(String dst) {
        String[] r = new String[3];
        dst = dst.trim();
        int idx1 = dst.indexOf('@');
        if (idx1 >= 0) {
            r[0] = idx1 == 0 ? null : dst.substring(0, idx1);
            dst = dst.substring(idx1 + 1);
        } else {
            r[0] = null;
        }
        int idx2 = dst.indexOf(':');
        if (idx2 >= 0) {
            r[1] = dst.substring(0, idx2);
            r[2] = idx2 == dst.length() - 1 ? null : dst.substring(idx2 + 1);
        } else {
            r[1] = dst;
            r[2] = null;
        }
        return r;
    }

    private static String readUsernameFromConsole() {
        String user = null;
        Scanner scanner = new Scanner(System.in);
        try {
            while (user == null || user.trim().isEmpty()) {
                System.out.print("login: ");
                user = scanner.nextLine().trim();
            }
        } finally {
            scanner.close();
        }
        return user;
    }

    private static String readPasswordFromConsole() {
        Console console = System.console();
        String password = null;
        while (password == null || password.trim().isEmpty()) {
            password = new String(console.readPassword("password: "));
        }
        return password;
    }

}
