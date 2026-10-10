package com.vita.web.xss;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.io.IOException;

/** 解析式纯文本检测，不使用标签正则、不生成清洗或转义后的业务值。 */
public final class XssValidator {
    private final JsonFactory jsonFactory;
    private final Cleaner cleaner = new Cleaner(Safelist.none());

    public XssValidator(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    public boolean isPlainText(String value) {
        // 只判断解析树中是否有不允许的 HTML，不把普通比较表达式的解析提示当成攻击。
        return value == null || cleaner.isValid(Jsoup.parseBodyFragment(value));
    }

    public boolean isValidJson(byte[] body, boolean credentialRequest) throws IOException {
        if (body.length == 0) {
            return true;
        }
        // 流式遍历所有字符串（含字段名），重复字段也逐个检查，不因树覆盖而漏检。
        try (var parser = jsonFactory.createParser(body)) {
            int depth = 0;
            while (parser.nextToken() != null) {
                JsonToken token = parser.currentToken();
                if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                    depth++;
                } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                    depth--;
                } else if (token == JsonToken.FIELD_NAME) {
                    if (!isPlainText(parser.currentName())) {
                        return false;
                    }
                } else if (token == JsonToken.VALUE_STRING) {
                    // 仅已核实凭据入口的顶层 password 豁免；嵌套同名字段仍检测。
                    boolean password = credentialRequest && depth == 1
                            && parser.getParsingContext().inObject() && "password".equals(parser.currentName());
                    if (!password && !isPlainText(parser.getText())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
