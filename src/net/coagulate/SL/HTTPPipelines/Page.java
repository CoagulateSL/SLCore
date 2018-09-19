package net.coagulate.SL.HTTPPipelines;

import java.util.Map;

/**
 *
 * @author Iain Price
 */
public abstract class Page extends AuthenticatedStringHandler {

    @Override
    public String handleAuthenticated() {
        State.get().page="";
        content();
        String page=State.get().page;
        if (pagetype==PAGETYPE.NONE) { return page; }
        if (pagetype==PAGETYPE.CENTERPANEL) { 
            return "<table style=\"margin-left: auto; margin-right: auto; min-width: 600px;vertical-align: top;\"><tr style=\"width: 100%;\"><td style=\"width: 100%;\">\n"+page+"</td></tr></table>\n";
        }
        throw new AssertionError("Page Type must be one of the above? "+pagetype);
    }
    private enum PAGETYPE {NONE,CENTERPANEL};
    private PAGETYPE pagetype=PAGETYPE.NONE;

    public Page centralisePage() { pagetype=PAGETYPE.CENTERPANEL; return this; }
    
    public abstract void content();
    
    public Page raw(String string) { State.get().page+=string+"\n"; return this; }
    public Page pageHeader(String header) {
        return raw("<h3 align=center><u>"+header+"</u></h3><br><br>");
    }
    public Page para(String content) { return raw("<p>"+content+"</p>"); }
    public Page startForm() { return raw("<form>"); }
    public Page endForm() { return raw("</form>"); }
    public Page label(String label) { if (!label.endsWith(":")) { label+=":"; } return raw("<b>"+label+"</b> "); }
    public Page passwordInput(String fieldname) { return raw("<input type=password name=\""+fieldname+"\">"); }
    public Page linebreak() { return raw("<br>"); }
    public Page submit(String label) { return raw("<button type=submit name=\""+label+"\" value=\""+label+"\">"+label+"</button>"); }
    public Page dumpParameters() {
        Map<String, String> p = State.get().parameters;
        for (String k:p.keySet()) { raw("<p>"+k+"="+p.get(k)+"</p>"); }
        return this;
    }
    
}
