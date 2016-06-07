package mkimg;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TreeEntry extends BaseEntry {

    private List<Node> children;

    public TreeEntry(String name, TreeEntry parent) {
        this.name = name;
        this.parent = parent;
        this.children = new LinkedList<>();
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public Iterator<Node> iterator() {
        return this.children.iterator();
    }

    public Node getChild(String name) {
        for (Node child : this) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    public Node internTree(String name) {
        if (name.indexOf(SEPARATOR) >= 0) {
            throw new RuntimeException("Name has separator : \"" + name + "\"");
        }
        Node child = getChild(name);
        if (child == null) {
            child = new TreeEntry(name, this);
            children.add(child);
        } else if (child.isLeaf()) {
            throw new RuntimeException("Directory expected : \"" + name + "\"");
        }
        return child;
    }

    public Node internFile(String name) {
        if (name.indexOf(SEPARATOR) >= 0) {
            throw new RuntimeException("Name has separator : \"" + name + "\"");
        }
        Node child = getChild(name);
        if (child == null) {
            child = new FileEntry(name, this);
            children.add(child);
        } else if (!child.isLeaf()) {
            throw new RuntimeException("File expected : \"" + name + "\"");
        }
        return child;
    }

}
/*


 */
