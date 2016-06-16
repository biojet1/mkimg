/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkimg;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalField;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author U-1
 */
public abstract class Inode {
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
    static ZoneOffset tzOffset = null;
// extra
    static final int X_HIDDEN = 1 << 16;
    static final int X_IGNORE = 1 << 17;
// Attributes
    public int mode = 0;
    public int uid = 0;
    public int gid = 0;
    public long size = Long.MIN_VALUE;
    public long mtime = Long.MIN_VALUE;
    public long ctime = Long.MIN_VALUE;
    public long atime = Long.MIN_VALUE;
    public short mtimeo = Short.MIN_VALUE;
    public short ctimeo = Short.MIN_VALUE;
    public short atimeo = Short.MIN_VALUE;
// Build Attributes
    public int sort = 0;
    public int nlink = 0;
    public int flag = 0;
    public long auxA = 0;
    public long auxB = 0;
    public byte[] hash = null;
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

    public int getFileType() {
        return (0xF000 & mode);
    }

    public int getFilePermission() {
        return (0x0FFF & mode);
    }

    public boolean hasModifiedTime() {
        return this.mtimeo != Short.MIN_VALUE;
    }

    public void setModifiedTime(FileTime t) {
        if (tzOffset == null) {
            this.mtime = t.to(TimeUnit.MICROSECONDS);
            this.mtimeo = 0;
        } else {
            this.mtime = t.to(TimeUnit.MICROSECONDS);
            this.mtimeo = (short) (tzOffset.getTotalSeconds() / 60);
            assert (this.mtimeo == Short.MIN_VALUE);
        }
    }

    public OffsetDateTime getModifiedTime() {
        if (hasModifiedTime()) {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(this.mtime / (1000 * 1000), this.mtime % (1000 * 1000)), ZoneOffset.ofTotalSeconds(this.mtimeo * 60));
        }
        throw new NoSuchElementException("ModifiedTime");
    }

    abstract public InputStream getInputStream() throws IOException;

    public void supply(BasicFileAttributes a) {
        if (a.isDirectory()) {
            if (this.getFileType() == 0) {
                this.mode |= S_IFDIR;
            }
        } else {
            if (this.getFileType() == 0) {
                this.mode |= S_IFREG;
            }
            if (size == Long.MIN_VALUE) {
                this.size = a.size();
            }
        }
        if (this.atime == Long.MIN_VALUE) {
            this.atime = a.lastAccessTime().toMillis();
        }
        if (this.mtime == Long.MIN_VALUE) {
            this.mtime = a.lastModifiedTime().toMillis();
        }
        if (this.ctime == Long.MIN_VALUE) {
            this.ctime = a.creationTime().toMillis();
        }
    }

    public void supply(PosixFileAttributes a) {
        for (PosixFilePermission b : a.permissions()) {
            switch (b) {
                case GROUP_EXECUTE:
                    this.mode |= Inode.S_IXGRP;
                    break;
                case GROUP_READ:
                    this.mode |= Inode.S_IRGRP;
                    break;
                case GROUP_WRITE:
                    this.mode |= Inode.S_IWGRP;
                    break;
                case OWNER_EXECUTE:
                    this.mode |= Inode.S_IXUSR;
                    break;
                case OWNER_READ:
                    this.mode |= Inode.S_IRUSR;
                    break;
                case OWNER_WRITE:
                    this.mode |= Inode.S_IWUSR;
                    break;
                case OTHERS_EXECUTE:
                    this.mode |= Inode.S_IXOTH;
                    break;
                case OTHERS_READ:
                    this.mode |= Inode.S_IROTH;
                    break;
                case OTHERS_WRITE:
                    this.mode |= Inode.S_IWOTH;
                    break;
                default:
                    assert (false);
            }
        }
    }

    static Inode fromFile(Path path) throws IOException {
        Inode ino;
        BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
//        System.err.print(path);
//        System.err.print(' ');
        if (a.isDirectory()) {
//            System.err.println("DIR");
            ino = new File(S_IFDIR, path.toString());
            if (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                ino.mode |= S_IFLNK;
                ino.size = target.toString().getBytes("UTF-8").length;
            } else {
                ino.size = a.size();
            }
        } else {
//            System.err.println("REG");
            ino = new File(S_IFREG, path.toString());
            if (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                ino.mode |= S_IFLNK;
                ino.size = target.toString().getBytes("UTF-8").length;
            } else {
                ino.size = a.size();
            }
        }
        ino.supply(a);
        //
        PosixFileAttributeView pv = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (pv != null) {
            ino.supply(pv.readAttributes());
        }
        return ino;
    }

    static public class File extends Inode {

        public String path;

        public File(int mode, String path) {
            super(mode);
            this.path = path;
        }

        public File(boolean mode, String path) {
            super(mode);
            this.path = path;
        }

        public File(boolean mode) {
            super(mode);
        }

        @Override
        public InputStream getInputStream() throws FileNotFoundException {
            return new FileInputStream(path);
        }

    }
}
