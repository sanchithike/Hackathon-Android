package com.roposo.core.constants;

import com.roposo.core.R;
import com.roposo.core.util.ContextHelper;

/**
 * Created by muddassir on 1/21/16.
 * Constant for all messages used in the app
 */
public interface MessageConstant {
    String FAILURE_MSG = ContextHelper.getContext().getString(R.string.something_went_wrong);
    String NO_INTERNET_MSG = ContextHelper.getContext().getString(R.string.no_internet_msg);
    String VIDEO_ERROR_MSG = ContextHelper.getContext().getString(R.string.download_error_msg);
    String DELETE = ContextHelper.getContext().getString(R.string.delete);
    String CANCEL = ContextHelper.getContext().getString(R.string.cancel);

}
