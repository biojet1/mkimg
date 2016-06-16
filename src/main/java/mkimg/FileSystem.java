package mkimg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileSystem {

    // sources tree   
    private TreeNode root = null;
    boolean cacheInodes = false;
    boolean followLinks = false;
    boolean linkDuplicates = false;
    boolean checkDuplicates = false;
    boolean addManifest = false;
    boolean calcDigest = false;
    boolean userInteractive = false;
    boolean caseSensitive = false;
    boolean carryOn = false;
    boolean zeroSize = false;
    boolean noEmptyDirs = false;
    boolean noEmptyFiles = false;
    boolean compactSpace = false;
    int sortSize = 2;
    int sortEntries = 2;
    int verbosity = 2;
    int setArchived = 2;
    int fsLayout = 3;
//    Hashtable<Object, Inode> inoCache = null;
    Map<Object, Inode> inoCache = new Hashtable<>();

    void addPath(String target) throws IOException {
        Path path = Paths.get(target);
        BasicFileAttributes a = readAttributes(path);
        TreeNode dir = getRoot();
        if (a.isDirectory()) {
            dir.getData().supply(a);
            walk(dir, path);
        } else {
            addPath(dir, path);
        }
    }

    synchronized TreeNode getRoot() {
        if (root == null) {
            root = new TreeNode.Directory("", null);
            root.setData(new Inode.File(true));
        }
        return root;
    }

    TreeNode addPath(TreeNode parent, Path path) throws IOException {
        String name = path.getFileName().toString();
        BasicFileAttributes a = readAttributes(path);
//        System.err.println(path);
        if (a.isDirectory()) {
            Node child = parent.internTree(name);
            child.setData(fetch(path));
            walk((TreeNode) child, path);
            return (TreeNode) child;
        } else {
            Node child = parent.internFile(name);
            child.setData(fetch(path));
            return (TreeNode) child;
        }
    }

    Stream<Path> list(Path dir) throws IOException {
        if (this.userInteractive) {
            RETRY:
            for (;;) {
                try {
                    return Files.list(dir);
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    for (;;) {
                        System.err.println("[A]bort [R]etry:");
                        switch (System.in.read()) {
                            case 'A':
                            case 'a':
                            case -1:
                                ex.printStackTrace(System.err);
                                System.exit(1);
                                return null;
                            case 'R':
                            case 'r':
                                continue RETRY;
                        }
                    }

                }
            }
        }
        return Files.list(dir);
    }

    BasicFileAttributes readAttributes(Path path) throws IOException {
        if (this.userInteractive) {
            RETRY:
            for (;;) {
                try {
                    return Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    for (;;) {
                        System.err.println("[A]bort [R]etry:");
                        switch (System.in.read()) {
                            case 'A':
                            case 'a':
                            case -1:
                                ex.printStackTrace(System.err);
                                System.exit(1);
                                return null;
                            case 'R':
                            case 'r':
                                continue RETRY;
                        }
                    }

                }
            }
        }
        return Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
    }

    void walk(TreeNode parent, Path dir) throws IOException {
        Iterator<Path> it = list(dir).iterator();
        while (it.hasNext()) {
            Path path = it.next();
            TreeNode child = addPath(parent, path);
            Inode data = child.getData();
//            System.err.println(data.mode);
            if (data != null && data.isDirectory()) {
                walk(child, path);
            }
        }
    }

    Inode fetch(Path path) throws IOException {
        if (inoCache != null) {
            BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
            Object id = a.fileKey();
            if (id != null) {
                Inode ino = inoCache.get(id);
                if (ino == null) {
                    ino = Inode.fromFile(path);
                    inoCache.put(id, ino);
                    System.err.print("CACHE:MIS");
                } else {
                    System.err.print("CACHE:HIT");
                }
                System.err.println(path);
                return ino;
            }
        }
        return Inode.fromFile(path);
    }
}
