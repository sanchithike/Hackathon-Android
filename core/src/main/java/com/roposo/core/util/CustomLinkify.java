package com.roposo.core.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import com.roposo.core.R;

public class CustomLinkify {

    public static final void addLinkWithRange(TextView text, String customUrl, int start,
                                              int end, BasicCallBack basicCallBack) {
        addLinkWithRange(text, customUrl, start, end, basicCallBack, true, true, 0);
    }

    public static final void addLinkWithRangeWithColor(TextView text, String customUrl, int start,
                                                       int end, BasicCallBack basicCallBack, int color) {
        addLinkWithRange(text, customUrl, start, end, basicCallBack, true, true, color);
    }

    public static final void addLinkWithRange(TextView text, String customUrl, int start,
                                              int end, BasicCallBack basicCallBack, boolean removeUnderline, boolean makeBold, int colorID) {
        SpannableString s = SpannableString.valueOf(text.getText());
        applyLink(customUrl, start, end, s, basicCallBack, removeUnderline, makeBold, colorID);
        text.setText(s);
        addLinkMovementMethod(text);
    }

    private static final void applyLink(String url, int start, int end, Spannable text,
                                        final BasicCallBack basicCallBack, boolean removeUnderline, boolean makeBold, int colorID) {
        CustomURLSpan span = new CustomURLSpan(url, removeUnderline, makeBold, colorID) {
            @Override
            public void onClick(View widget) {
                if (basicCallBack != null) {
                    basicCallBack.callBack(BasicCallBack.CallBackSuccessCode.SUCCESS, null);
                }
            }
        };
        text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static final void addLinkMovementMethod(TextView t) {
        MovementMethod m = t.getMovementMethod();

        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (t.getLinksClickable()) {
                t.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private static class CustomURLSpan extends URLSpan {
        final int DEFAULT_LINK_COLOR = ContextHelper.getContext().getResources().
                getColor(R.color.default_link_color);
        boolean removeUnderline;
        boolean makeBold;
        int colorID;

        public CustomURLSpan(String url, boolean removeUnderline, boolean makeBold, int colorID) {
            super(url);
            this.removeUnderline = removeUnderline;
            this.colorID = colorID;
            this.makeBold = makeBold;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            if (this.removeUnderline) {
                ds.setUnderlineText(false);
            }
            if (this.colorID != 0) {
                ds.setColor(colorID);
            } else {
                ds.setColor(DEFAULT_LINK_COLOR);
            }
            if (this.makeBold) {
                ds.setFakeBoldText(true);
            }
        }
    }

}