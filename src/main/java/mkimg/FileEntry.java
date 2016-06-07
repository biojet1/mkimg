/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkimg;

import java.util.Collections;
import java.util.Iterator;

/**
 *
 * @author U-1
 */
public class FileEntry extends BaseEntry {

    FileEntry(String name, TreeEntry parent) {
        this.name = name;
        this.parent = parent;   }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public Iterator<Node> iterator() {
        return Collections.emptyIterator();
        //   throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
