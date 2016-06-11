/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkimg;

/**
 *
 * @author U-1
 */
public class INodeEntry {
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

    public INodeEntry(int mode) {
        this.mode = mode;
    }

    public INodeEntry(boolean dir) {
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
}
