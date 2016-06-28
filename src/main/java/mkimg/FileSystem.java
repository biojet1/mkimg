package mkimg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
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
    boolean noDirTime = true;
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
            dir.getData().supply(a, noDirTime);
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
        Inode ino;
        BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
        if (inoCache != null) {
            Object id = a.fileKey();
            if (id != null) {
                ino = inoCache.get(id);
                if (ino == null) {
                    ino = Inode.ofFile(path, a);
                    inoCache.put(id, ino);
                    System.err.print("CACHE:MIS");
                } else {
                    System.err.print("CACHE:HIT");
                }
                System.err.println(path);
                ino.supply(a, ino.isDirectory() ? noDirTime : false);
                return ino;
            }
        }
        ino = Inode.ofFile(path, a);
        ino.supply(a, ino.isDirectory() ? noDirTime : false);
        return ino;
    }

    void zeroSize() throws IOException {
        Iterator<Node<Inode>> w = getRoot().depthFirstIterator();
        while (w.hasNext()) {
            Node<Inode> cur = w.next();
            Inode ino = cur.getData();
            if (!ino.isDirectory()) {
                ino.size = 0;
            }
        }
    }

    void trimEmpty() throws IOException {
        Iterator<Node<Inode>> w = getRoot().depthFirstIterator();
        while (w.hasNext()) {
            Node<Inode> cur = w.next();
            Inode ino = cur.getData();
            if (ino.isDirectory()) {
            } else if (ino.size == 0) {

            } else {
                ino.nlink++;
            }
        }
    }

    void mergeDuplicate() throws IOException {
        // Generate size map
        Iterator<Node<Inode>> w = getRoot().depthFirstIterator();
        HashMap<Long, LinkedHashSet<Inode>> sizeMap = new HashMap<>();
        while (w.hasNext()) {
            Node<Inode> cur = w.next();
            Inode ino = cur.getData();
            if (!ino.isDirectory() && !ino.isManifest() && !ino.isCommand() && ino.size > 0) {
                assert (ino.size <= Long.MAX_VALUE);
                LinkedHashSet<Inode> inoSet = sizeMap.get(ino.size);
                if (inoSet == null) {
                    sizeMap.put(ino.size, inoSet = new LinkedHashSet<>());
                }
                inoSet.add(ino);
            }
        }// For each distinct size list, ...
        if (sizeMap.size() < 1) {
            return;
        }
      //  sizeMap.values().
        for (Map.Entry<Long, LinkedHashSet<Inode>> entry : sizeMap.entrySet()) {
            LinkedHashSet<Inode> inoSet = entry.getValue();
            if (inoSet.size() > 1) {

            }
        }

    }
}
