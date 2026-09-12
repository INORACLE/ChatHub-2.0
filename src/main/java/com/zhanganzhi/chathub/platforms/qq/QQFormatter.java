package com.zhanganzhi.chathub.platforms.qq;

import com.zhanganzhi.chathub.core.formatter.AbstractFormatter;
import com.zhanganzhi.chathub.core.formatter.FormattingContent;
import com.zhanganzhi.chathub.platforms.Platform;

public class QQFormatter extends AbstractFormatter {

    public QQFormatter() {
        super(Platform.QQ);
    }

    @Override
    protected String replaceAll(String message, FormattingContent content) {
        return this.getPlainString(super.replaceAll(message, content));
    }
}