package net.coagulate.SL.Pages.HTML;

/**
 *
 * @author Iain Price
 */
public class Anchor extends Container {
    String target;
    public Anchor(String target) { this.target=target; }
    public Anchor(String target,Element content) { this.target=target; add(content); }
    public Anchor(String target,String content) { this.target=target; add(new Raw(content)); }

    public String toHtml(State st) {
        return 
                "<a href=\"#"+target+"\">"+
                super.toHtml(st)+
                "</a>";
    }
}
