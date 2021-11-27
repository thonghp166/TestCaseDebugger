package com.dse.report.element;

public class CodeView implements IElement {
    private String html;

    public CodeView(String html) {
        this.html = html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Override
    public String toHtml() {
        return String.format("<div class=\"code-view\">%s</div>", html);
    }
}
