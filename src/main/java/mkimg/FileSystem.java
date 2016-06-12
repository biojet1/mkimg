/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkimg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 *
 * @author U-1
 */
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
    LinkedHashMap<Object, Inode> inoCache = new LinkedHashMap<>();

    void addPath(String target) throws IOException {
        addPath(getRoot(), target);
    }

    synchronized TreeNode getRoot() {
        if (root == null) {
            root = new TreeNode.Directory("", null);
            root.setData(new Inode(true));
        }
        return root;
    }

    void addPath(TreeNode parent, String target) throws IOException {
        Path path = Paths.get(target);
        String name = path.getFileName().toString();
        BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
        if (a.isDirectory()) {
            walk(parent, path);
        } else {
            Node child = parent.internFile(name);
            child.setData(path.toAbsolutePath().toString());
        }
    }

    TreeNode addPath(TreeNode parent, Path path) throws IOException {
        String name = path.getFileName().toString();
        if (Files.isDirectory(path)) {
            Node child = parent.internTree(name);
            child.setData(fetch(path));
            return (TreeNode) child;
        } else {
            Node child = parent.internFile(name);
            child.setData(fetch(path));
            return (TreeNode) child;
        }
    }

    void walk(TreeNode parent, Path dir) throws IOException {
        Iterator<Path> it = Files.list(dir).iterator();
        while (it.hasNext()) {
            Path path = it.next();
            Node child = addPath(parent, path);
            Object data = child.getData();
            if (data != null && ((Inode) data).isDirectory()) {
                walk((TreeNode) child, path);
            }
        }
    }

    Inode fetch(Path path) throws IOException {
        if (inoCache != null) {
            BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
            Object id = a.fileKey();
            Inode ino = inoCache.get(id);
            if (ino == null) {
                ino = Inode.fromFile(path);
                inoCache.put(id, ino);
            }
            return ino;
        } else {
            return Inode.fromFile(path);
        }
    }

}
