package udf;

import mkimg.Inode;
import mkimg.Node;

abstract public class DataDesc {

    public final Node<Inode> node;
    public final int seq;

    public DataDesc(Node<Inode> node, int seq) {
        this.node = node;
        this.seq = seq;
    }

    abstract public boolean isEntry();

    public static class Entry extends DataDesc {

        public Entry(Node<Inode> node, int seq) {
            super(node, seq);
        }

        @Override
        public boolean isEntry() {
            return true;
        }

    }

    public static class Data extends DataDesc {

        public Data(Node<Inode> node, int seq) {
            super(node, seq);
        }

        @Override
        public boolean isEntry() {
            return false;
        }
    }

}
