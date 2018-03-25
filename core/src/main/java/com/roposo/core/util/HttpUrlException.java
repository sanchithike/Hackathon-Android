package com.roposo.core.util;

/**
 * @author muddassir on 12/1/17.
 */

class HttpUrlException extends Throwable {
    public HttpUrlException(String message) {
        super(message);
    }

    public HttpUrlException(Throwable e) {
        super(e);
    }
}
