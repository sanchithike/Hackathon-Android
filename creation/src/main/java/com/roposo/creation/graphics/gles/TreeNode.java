package com.roposo.creation.graphics.gles;

/**
 * Created by bajaj on 12/07/16.
 */

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeNode implements Cloneable {

    private TreeNode mParent;
    private SparseArray<TreeNode> mChildren;
    @NonNull
    private String mId;

    public TreeNode() {
        mId = String.valueOf(System.currentTimeMillis()) + String.valueOf(Math.random() * Byte.MAX_VALUE);
        mChildren = new SparseArray<>();
    }

    public TreeNode(TreeNode data) {
        this();
        clone(data);
    }

    private void clone(TreeNode node) {
        mParent = node.mParent;
        mChildren = node.getChildren();
    }

    public TreeNode getParent() {
        return mParent;
    }

    public SparseArray<TreeNode> getChildren() {
        return this.mChildren;
    }

    public int getNumberOfChildren() {
        return getChildren().size();
    }

    public boolean hasChildren() {
        return (getNumberOfChildren() > 0);
    }

    public void setParent(TreeNode parent) {
        mParent = parent;
    }

    public void setChildren(SparseArray<TreeNode> children) {
        mChildren = children;
        int childrenCount = children.size();
        for (int i = 0; i < childrenCount; i++) {
            TreeNode child = mChildren.valueAt(i);
            if (child != null) {
                child.setParent(this);
            }
        }
    }

    public void addChild(int key, TreeNode child) {
        mChildren.put(key, child);
        child.setParent(this);
    }

    public void setChildAt(int index, TreeNode child) throws IndexOutOfBoundsException {
        mChildren.setValueAt(index, child);
        child.setParent(this);
    }

    public void removeChildren() {
        this.mChildren = new SparseArray<>();
    }

    public void removeChild(int index) throws IndexOutOfBoundsException {
        mChildren.remove(index);
    }

    public TreeNode getChild(int key) {
        return mChildren.get(key);
    }

    public TreeNode getChildAt(int index) throws IndexOutOfBoundsException {
        return mChildren.get(index);
    }

    @CallSuper
    void copy(TreeNode src) {
        mParent = src.getParent();
        mChildren = src.getChildren();
        mId = src.getId();
    }

    @CallSuper
    public String toStringVerbose() {
        StringBuilder stringRepresentation = new StringBuilder(toString());
        stringRepresentation.append("[\n");
        int childrenCount = getChildren().size();
        SparseArray<TreeNode> children = getChildren();
        for (int i = 0; i < childrenCount; i++) {
            TreeNode node = children.valueAt(i);
            if (node != null) {
                stringRepresentation.append(node.toStringVerbose()).append(", ");
            }
        }

        //Pattern.DOTALL causes ^ and $ to match. Otherwise it won't. It's retarded.
        Pattern pattern = Pattern.compile(", $", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stringRepresentation.toString());

        stringRepresentation = new StringBuilder(matcher.replaceFirst(""));
        stringRepresentation.append("]\n");

        return stringRepresentation.toString();
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public void cleanup() {
        mChildren.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TreeNode) {
            return this.mId.equals(((TreeNode) obj).mId);
        }
        return super.equals(obj);
    }

    @Override
    public TreeNode clone() {
        Object clone = null;
        try {
            clone = super.clone();
            ((TreeNode)clone).copy(this);
            return (TreeNode) clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    //    @Override
//    public String toString() {
//        return mId;
//    }
}
