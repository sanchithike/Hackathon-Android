package com.roposo.creation.models;

import android.support.annotation.NonNull;

/**
 * Created by Anil Sharma on 13/12/16.
 */
public class NamedProperty {
    @NonNull
    public String name;

    protected NamedProperty(@NonNull String name) {
        this.name = name;
    }
}
