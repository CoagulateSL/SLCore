package net.coagulate.SL.HTTPPipelines;

import java.util.Map;

/**
 *
 * @author Iain Price
 */
public abstract class Page extends AuthenticatedStringHandler {

    // NEVER USE CLASS LOCAL STATE, there is only one instance for all users, at the same time :/
    @Override
    public String handleAuthenticated() {
        State.get().page="";
        State.get().page_firstinput=true;
        content();
        String page=State.get().page;
        if (State.get().pagetype==PAGETYPE.NONE) { return page; }
        if (State.get().pagetype==PAGETYPE.CENTERPANEL) { 
            return "<table style=\"margin-left: auto; margin-right: auto; min-width: 600px;vertical-align: top;\"><tr style=\"width: 100%;\"><td style=\"width: 100%;\">\n"+page+"</td></tr></table>\n";
        }
        throw new AssertionError("Page Type must be one of the above? "+State.get().pagetype);
    }
    private boolean firstinput() { return State.get().page_firstinput; }
    private String autofocusString() {
        if (!firstinput()) { return ""; }
        State.get().page_firstinput=true;
        return " autofocus ";
    } 
    public enum PAGETYPE {NONE,CENTERPANEL};

    public Page centralisePage() { State.get().pagetype=PAGETYPE.CENTERPANEL; return this; }
    
    public abstract void content();
    
    public Page raw(String string) { State.get().page+=string+"\n"; return this; }
    public Page pageHeader(String header) {
        return raw("<h3 align=center><u>"+header+"</u></h3><br><br>");
    }
    public Page para(String content) { return raw("<p>"+content+"</p>"); }
    public Page startForm() { return raw("<form method=post>"); }
    public Page endForm() { return raw("</form>"); }
    public Page label(String label) { if (!label.endsWith(":")) { label+=":"; } return raw("<b>"+label+"</b> "); }
    public Page textInput(String fieldname) { return raw("<input "+autofocusString()+" type=text name=\""+fieldname+"\">"); }
    public Page passwordInput(String fieldname) { return raw("<input "+autofocusString()+" type=password name=\""+fieldname+"\">"); }
    public Page linebreak() { return raw("<br>"); }
    public Page submit(String label) { return raw("<button type=submit name=\""+label+"\" value=\""+label+"\">"+label+"</button>"); }
    public Page dumpParameters() {
        Map<String, String> p = State.get().parameters;
        for (String k:p.keySet()) { raw("<p>"+k+"="+p.get(k)+"</p>"); }
        return this;
    }
    public Page error(String errormessage) { return raw("<font color=red><b>"+errormessage+"</b></font>"); }
    
}
