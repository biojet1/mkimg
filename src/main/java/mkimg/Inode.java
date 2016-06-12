/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkimg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;

/**
 *
 * @author U-1
 */
public class Inode {
// permission

    static final short S_IRWXU = 00700;
    static final short S_IRUSR = 00400;
    static final short S_IWUSR = 00200;
    static final short S_IXUSR = 00100;
    static final short S_IRWXG = 00070;
    static final short S_IRGRP = 00040;
    static final short S_IWGRP = 00020;
    static final short S_IXGRP = 00010;
    static final short S_IRWXO = 00007;
    static final short S_IROTH = 00004;
    static final short S_IWOTH = 00002;
    static final short S_IXOTH = 00001;
// file type
    static final int S_IFDIR = 0040000;
    static final int S_IFCHR = 0020000;
    static final int S_IFBLK = 0060000;
    static final int S_IFREG = 0100000;
    static final int S_IFIFO = 0010000;
    static final int S_IFLNK = 0120000;
// extra
    static final int X_HIDDEN = 1 << 16;
    static final int X_IGNORE = 1 << 17;
//
    public long size = Long.MIN_VALUE;
    public long mtime = Long.MIN_VALUE;
    public long ctime = Long.MIN_VALUE;
    public long atime = Long.MIN_VALUE;
    public int mode = 0;
    public int uid = 0;
    public int gid = 0;
//
    public int sort = 0;
    public int nlink = 0;
// buid flag
    int flag = 0;
//
    public long aux1 = Long.MIN_VALUE;
    public long aux2 = Long.MIN_VALUE;
// 

    public Inode(int mode) {
        this.mode = mode;
    }

    public Inode(boolean dir) {
        if (dir) {
            this.mode = S_IFDIR;
        } else {
            this.mode = S_IFREG;
        }
    }

//
    public boolean isDirectory() {
        return (S_IFDIR & mode) == S_IFDIR;
    }

    public boolean isHidden() {
        return (X_HIDDEN & mode) == X_HIDDEN;
    }

    static Inode fromFile(Path path) throws IOException {
        Inode ino;
        BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();

        if (a.isDirectory()) {
            ino = new Inode(0);
            if (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                ino.mode = S_IFDIR | S_IFLNK;
                ino.size = target.toString().getBytes("UTF-8").length;
            } else {
                ino.mode = S_IFDIR;
                ino.size = a.size();
            }
        } else {
            ino = new Inode(0);
            if (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                ino.mode = S_IFREG | S_IFLNK;
                ino.size = target.toString().getBytes("UTF-8").length;
            } else {
                ino.mode = S_IFREG;
                ino.size = a.size();
            }
        }

        ino.atime = a.lastAccessTime().toMillis();
        ino.mtime = a.lastModifiedTime().toMillis();
        ino.ctime = a.creationTime().toMillis();
        PosixFileAttributeView pv = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        //
        if (pv != null) {
            PosixFileAttributes p = pv.readAttributes();
            for (PosixFilePermission b : p.permissions()) {
                switch (b) {
                    case GROUP_EXECUTE:
                        ino.mode |= Inode.S_IXGRP;
                        break;
                    case GROUP_READ:
                        ino.mode |= Inode.S_IRGRP;
                        break;
                    case GROUP_WRITE:
                        ino.mode |= Inode.S_IWGRP;
                        break;
                    case OWNER_EXECUTE:
                        ino.mode |= Inode.S_IXUSR;
                        break;
                    case OWNER_READ:
                        ino.mode |= Inode.S_IRUSR;
                        break;
                    case OWNER_WRITE:
                        ino.mode |= Inode.S_IWUSR;
                        break;
                    case OTHERS_EXECUTE:
                        ino.mode |= Inode.S_IXOTH;
                        break;
                    case OTHERS_READ:
                        ino.mode |= Inode.S_IROTH;
                        break;
                    case OTHERS_WRITE:
                        ino.mode |= Inode.S_IWOTH;
                        break;
                    default:
                        assert (false);
                }
            }
        }
        return ino;
    }
}
