package mkimg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;

public class INodeFile extends INodeEntry {

    public INodeFile(int mode) {
        super(mode);
    }

    static INodeEntry fromFile(Path path) throws IOException {
        INodeEntry ino;
        BasicFileAttributes a = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();

        if (a.isDirectory()) {
            ino = new INodeFile(0);
            if (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                ino.mode = S_IFDIR | S_IFLNK;
                ino.size = target.toString().getBytes("UTF-8").length;
            } else {
                ino.mode = S_IFDIR;
                ino.size = a.size();
            }
        } else {
            ino = new INodeFile(0);
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
                        ino.mode |= INodeEntry.S_IXGRP;
                        break;
                    case GROUP_READ:
                        ino.mode |= INodeEntry.S_IRGRP;
                        break;
                    case GROUP_WRITE:
                        ino.mode |= INodeEntry.S_IWGRP;
                        break;
                    case OWNER_EXECUTE:
                        ino.mode |= INodeEntry.S_IXUSR;
                        break;
                    case OWNER_READ:
                        ino.mode |= INodeEntry.S_IRUSR;
                        break;
                    case OWNER_WRITE:
                        ino.mode |= INodeEntry.S_IWUSR;
                        break;
                    case OTHERS_EXECUTE:
                        ino.mode |= INodeEntry.S_IXOTH;
                        break;
                    case OTHERS_READ:
                        ino.mode |= INodeEntry.S_IROTH;
                        break;
                    case OTHERS_WRITE:
                        ino.mode |= INodeEntry.S_IWOTH;
                        break;
                    default:
                        assert (false);
                }
            }
        }
        return ino;
    }
}
