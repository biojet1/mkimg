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
import java.util.Iterator;

/**
 *
 * @author U-1
 */
public class FileSystem {
    // sources tree   

    TreeEntry root = new TreeEntry("", null);

    void addPath(String target) throws IOException {
        addPath(root, target);
    }

    void addPath(TreeEntry parent, String target) throws IOException {
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

    void walk(TreeEntry parent, Path dir) throws IOException {
        Iterator<Path> it = Files.list(dir).iterator();
        while (it.hasNext()) {
            Path path = it.next();
            String name = path.getFileName().toString();
            BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
            if (a.isDirectory()) {
                Node child = parent.internTree(name);
                child.setData(path.toAbsolutePath().toString());
                walk((TreeEntry) child, path);
            } else {
                Node child = parent.internFile(name);
                child.setData(path.toAbsolutePath().toString());
            }
        }
    }
}
