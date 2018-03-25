package com.roposo.core.util;

/**
 * Created by anilshar on 4/1/16.
 * <p>
 * A generic callback listener when an action gets finished
 */

public interface ActionListener {
    void onSuccess(Object... data);

    void onFailure(Object... data);
}
