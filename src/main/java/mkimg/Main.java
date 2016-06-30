package mkimg;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class Main {

    public static void main(String[] args) throws IOException {
        int blockSize = 0;
        String out = null;
        UDFBuild udf = new UDFBuild();
        TreeNode root;
        boolean compactImage = false;
        {
            String volLabel = null;
            FileSystem fs = new FileSystem();
            Iterator<String> argv = Arrays.stream(args).iterator();
            boolean addManifest = false;
            boolean zeroSize = false;
            for (int dash = 0; argv.hasNext();) {
                String arg = argv.next();
                if (dash > 1 || !arg.startsWith("-")) {
                    fs.addPath(arg);
                } else if ("--".equals(arg)) {
                    dash = 2;
                } else if ("--logs".equals(arg)) {

                } else if (("--output".equals(arg) || "-o".equals(arg)) && argv.hasNext()) {
                    arg = argv.next();
                    out = arg;
                } else if ("--find".equals(arg) && argv.hasNext()) {
                    arg = argv.next();
                } else if ("-V".equals(arg) && argv.hasNext()) {
                    arg = argv.next();
                    volLabel = arg;
                } else if (("--config".equals(arg) || "-p".equals(arg)) && argv.hasNext()) {
                    arg = argv.next();
                } else if (("--include".equals(arg) || "-I".equals(arg)) && argv.hasNext()) {
                    arg = argv.next();
                } else if (("--cache-inodes".equals(arg) || "-h".equals(arg)) && argv.hasNext()) {
                    fs.cacheInodes = true;
                } else if (("--link-duplicates".equals(arg) || "-H".equals(arg)) && argv.hasNext()) {
                    fs.linkDuplicates = true;
                } else if (("--follow-links".equals(arg) || "-f".equals(arg))) {
                    fs.followLinks = true;
                } else if ("--manifest".equals(arg) || "--checksum".equals(arg)) {
                    addManifest = true;
                } else if ("--zero-size".equals(arg)) {
                    zeroSize = true;
                } else if ("--compact".equals(arg)) {
                    compactImage = true;
                } else if ("--hd".equals(arg)) {
                    blockSize = 512;
                } else {
                    throw new RuntimeException("Unexpected argument : \"" + arg + "\"");
                }
            }
            root = fs.getRoot();
            if (addManifest) {
                udf.addManifest = true;
                boolean useEtc = false;
                for (Node<Inode> cur : root) {
                    if (!cur.isLeaf()) {
                        useEtc = true;
                        break;
                    }
                }
                if (useEtc) {
                    Node man = (TreeNode) ((TreeNode) root.internTree(".etc")).internFile("checksum");
                    man.setData(new Inode.Manifest());
                    for (;;) {
                        ((TreeNode) man).flag |= (TreeNode.HIDE | TreeNode.IGNORE);
                        man = man.getParent();
                        if (man == null || man.getChildren().size() > 1) {
                            break;
                        }
                    }
                } else {
                    TreeNode man = (TreeNode) root.internFile("checksum");
                    man.setData(new Inode.Manifest());
                    man.flag |= (TreeNode.HIDE | TreeNode.IGNORE);
                }
            }
            if (zeroSize) {
                fs.zeroSize();
            }
            if (volLabel != null) {
                udf.logicalVolumeIdentifier = volLabel;
                udf.fileSetIdentifier = volLabel;
            }
        }
        BlockSink sink = new BlockSink();
        if (blockSize > 0) {
            sink.setBlockSize(blockSize);
        }
        udf.build(sink, root, out, compactImage);
    }
}
/*
		}else if(opt_get_bool(&o, ('k'), ("check-duplicates"))){
			db.checkDuplicates = !!o.bparam;
		}else if(opt_get_bool(&o, ('i'), ("interactive"))){
			db.userInteractive = o.bparam ? 2 : 0;
		}else if(opt_get_bool(&o, ('b'), ("batch"))){
			db.userInteractive = o.bparam ? 0 : 2;
		}else if(opt_get_bool(&o, 0, ("calc-digest"))){
			db.calcDigest = !!o.bparam;
		}else if(opt_get_bool(&o, 0, ("manifest"))){ // OPT, DOC
			db.addManifest = !!o.bparam;
		}else if(opt_get_bool(&o, 0, ("zero-size"))){ // DOC
			db.zeroSize = o.bparam ? 1 : 0;
		}else if(opt_get_bool(&o, 0, ("empty-file"))){ // OPT, DOC
			db.noEmptyFiles = !o.bparam;
		}else if(opt_get_bool(&o, 0, ("empty-dir"))){ // OPT, DOC
			db.noEmptyDirs = !o.bparam;
		}else if(opt_get_bool(&o, 0, ("empty"))){ // OPT, DOC
			db.noEmptyDirs = db.noEmptyFiles = !o.bparam;
		}else if(opt_get_bool(&o, 0, ("verbose"))){ // DOC
			db.verbosity = (o.bparam ? 1 : 0);
		}else if(opt_get_bool(&o, 0, ("quiet"))){ // DOC
			db.verbosity = (o.bparam ? 0 : 1);
		}else if(opt_get_bool(&o, 0, ("sort-size"))){
			db.sortSize = (o.bparam ? 1 : 0);
		}else if(opt_get_bool(&o, 0, ("carryon"))){
			db.carryOn = (o.bparam ? 1 : 0);
		}else if(opt_get_param(&o, 0, ("system-area"))){
			db.set("system-area", o.sparam);
		}else if(opt_get_bool(&o, 0, ("archive"))){ // OPT, DOC
			db.setArchived = (o.bparam ? 1 : 0);
		}else if(opt_get_bool(&o, 0, ("compact"))){ // OPT, DOC
			db.compactSpace = (o.bparam ? 1 : 0);
 */
