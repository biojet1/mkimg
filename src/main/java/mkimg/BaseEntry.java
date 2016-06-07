/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkimg;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

/**
 *
 * @author U-1
 */
public abstract class BaseEntry implements Node {

    static char SEPARATOR = '/';
    String name;
    String path;
    Node parent;
    Object data;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }

    public void writeTree(final  PrintStream out, final int depth) throws IOException {
        if (depth > 0) {
            for (int i = depth; i-- > 0;) {
                out.write(' ');
            }
            if (this.isLeaf()) {
                out.write('-');
            } else {
                out.write('+');
            }
            out.print(' ');
            out.print(getName());
            out.print('\n');
        }
        if (!this.isLeaf()) {
            final int i = depth + 1;
            for (Node child : this) {
                ((BaseEntry) child).writeTree(out, i);
            }
        }
    }
}
