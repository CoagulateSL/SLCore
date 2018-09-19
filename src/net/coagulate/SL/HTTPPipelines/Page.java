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
        
        return State.get().page;
    }
    
    public abstract void content();
    
    public Page raw(String string) { State.get().page+=string; return this; }
    public Page pageHeader(String header) {
        return raw("<h3 align=center><u>"+header+"</u></h3>");
    }
    public Page startForm() { return raw("<form>"); }
    public Page label(String label) { if (!label.endsWith(":")) { label+=":"; } return raw("<b>"+label+"</b>"); }
    public Page passwordInput(String fieldname) { return raw("<input type=password name=\""+fieldname+"\">"); }
    public Page linebreak() { return raw("<br>"); }
    public Page submit(String label) { return raw("button type=submit name=\""+label+"\" value=\""+label+"\">"+label+"</button>"); }
    
}
