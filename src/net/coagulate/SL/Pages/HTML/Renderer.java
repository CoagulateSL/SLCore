package net.coagulate.SL.Pages.HTML;

import java.util.Map;

/**
 *
 * @author Iain Price
 */
public abstract class Renderer implements Element {

    protected String value=null;
    public void set(String value) { this.value=value; }
    public String get() { return value; }
    @Override
    public String toHtml(State st) { return render(st,value); }
    @Override
    public void load(Map<String, String> map) {}
    public abstract String render(State st,String value);
}
