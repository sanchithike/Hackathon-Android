package com.roposo.core.util;

/**
 * Created by anilshar on 4/1/16.
 */

/**
 * This adapter class provides empty implementations of the methods from {@link ActionListener}.
 * Any custom listener that cares only about a subset of the methods of this listener can
 * simply subclass this adapter class instead of implementing the interface directly.
 */

public abstract class ActionListenerAdapter implements ActionListener {
    @Override
    public void onSuccess(Object... data) {
    }

    @Override
    public void onFailure(Object... data) {
    }

}
