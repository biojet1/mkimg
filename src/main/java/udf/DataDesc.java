/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udf;

import mkimg.Inode;
import mkimg.Node;

/**
 *
 * @author U-1
 */
public class DataDesc {

    public final Node<Inode> node;
    public final int seq;
    private final boolean is_entry;

    public DataDesc(Node node, int seq, boolean entry) {
        this.node = node;
        this.seq = seq;
        this.is_entry = entry;
    }

    public boolean isEntry() {
        return this.is_entry;
    }
}
