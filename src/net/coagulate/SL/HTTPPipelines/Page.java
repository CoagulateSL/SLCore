package net.coagulate.SL.HTTPPipelines;

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
            return "<table style=\"min-width: 600px;vertical-align: top;\">"+page+"</table>";
        }
        throw new AssertionError("Page Type must be one of the above? "+pagetype);
    }
    private enum PAGETYPE {NONE,CENTERPANEL};
    private PAGETYPE pagetype=PAGETYPE.NONE;

    public Page centralisePage() { pagetype=PAGETYPE.CENTERPANEL; return this; }
    
    public abstract void content();
    
    public Page raw(String string) { State.get().page+=string; return this; }
    public Page pageHeader(String header) {
        return raw("<h3 align=center><u>"+header+"</u></h3>");
    }
    public Page startForm() { return raw("<form>"); }
    public Page endForm() { return raw("</form>"); }
    public Page label(String label) { if (!label.endsWith(":")) { label+=":"; } return raw("<b>"+label+"</b> "); }
    public Page passwordInput(String fieldname) { return raw("<input type=password name=\""+fieldname+"\">"); }
    public Page linebreak() { return raw("<br>"); }
    public Page submit(String label) { return raw("<button type=submit name=\""+label+"\" value=\""+label+"\">"+label+"</button>"); }
    
}
