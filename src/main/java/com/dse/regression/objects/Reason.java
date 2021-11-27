package com.dse.regression.objects;

import com.dse.parser.object.INode;

public class Reason {
    private Object item; // item can be an INode or an TestNameNode
    private String statusOfItem;
    private Object source; // source can be an INode or an TestNameNode

    public Reason(Object item, String statusOfItem, Object source) {
        this.item = item;
        this.statusOfItem = statusOfItem;
        this.source = source;
    }

    public String getStatusOfItem() {
        return statusOfItem;
    }

    public Object getSource() {
        return source;
    }

    public final static String STATUS_DELETED = "deleted";
    public final static String STATUS_MODIFIED = "modified";
    public final static String STATUS_AFFECTED = "affected";
    public final static String STATUS_NA = "N/A";
    public final static String STATUS_NONE_AFFECTION = "not affected";
}
