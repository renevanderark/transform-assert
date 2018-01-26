package nl.kb.xml.transformassert;

import java.util.function.Consumer;

class LogUtil {
    static void indent(String lines, int whitespace, Consumer<String> logBack) {

        for(String line : lines.split("\\r\\n|\\n|\\r")) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < whitespace; i++) {
                sb.append(" ");
            }
            logBack.accept(sb.append(line).toString());
        }
    }

    static String mkRule(String defaultRule, String[] rule) {
        return rule.length > 0 ? rule[0] : defaultRule;
    }
}
