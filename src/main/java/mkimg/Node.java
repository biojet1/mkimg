package mkimg;

import java.util.Iterator;
import java.util.List;

public interface Node extends Iterable<Node> {

    public String getName();

    public String getPath();

    default public boolean isLeaf() {
        return false;
    }

    public Object getData();

    public void setData(Object data);

    public Node getParent();

//    default public boolean isRoot() {
//        return getParent() == null;
//    }
    static class EmptyIterator implements Iterator<Node> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Node next() {
            return null;
        }
    }

    @Override
    default public Iterator<Node> iterator() {
        return new EmptyIterator();
    }

    default public List<Node> getChildren() {
        throw new UnsupportedOperationException();
    }
}
