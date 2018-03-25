package com.roposo.core.util;

import android.os.AsyncTask;

/**
 * Created by rahul on 11/5/14.
 */
public class UniversalAsyncTask extends AsyncTask<String, Void, Void>  {

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Void doInBackground(String... strings) {
        return null;
    }

    public void hardCancel() {
        cancel(true);
    }

}
