package mkimg;

public interface Node extends Iterable<Node> {

    public String getName();

    public String getPath();

    boolean isLeaf();

    public Object getData();

    public void setData(Object data);

    public Node getParent();

    default public boolean isRoot() {
        return getParent() == null;
    }
}
