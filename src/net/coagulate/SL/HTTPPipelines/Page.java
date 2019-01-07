package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.SL.Pages.HTML.Container;
import net.coagulate.SL.Pages.HTML.Form;
import net.coagulate.SL.Pages.HTML.Header1;
import net.coagulate.SL.Pages.HTML.Paragraph;
import net.coagulate.SL.Pages.HTML.ServiceCell;
import net.coagulate.SL.Pages.HTML.State;

/**
 *
 * @author Iain Price
 */
public class Page extends Container {

    public enum PAGELAYOUT {NONE,CENTERCOLUMN};
    private PAGELAYOUT layout=PAGELAYOUT.NONE;
    public void layout(PAGELAYOUT layout) { this.layout=layout; }

    public String preLayout() {
        if (layout==PAGELAYOUT.NONE) { return ""; }
        if (layout==PAGELAYOUT.CENTERCOLUMN) { return "<p align=center><table><tr><td style=\"max-width: 800px;\">"; }
        throw new SystemException("Unhandled pre-layout "+layout);
    }
    public String postLayout() {
        if (layout==PAGELAYOUT.NONE) { return ""; }
        if (layout==PAGELAYOUT.CENTERCOLUMN) { return "</td></td></table></p>"; }
        throw new SystemException("Unhandled post-layout "+layout);
    }

    public Header1 header(String header) { Header1 h=new Header1(header); add(h); return h; }
    public Paragraph paragraph() { Paragraph p=new Paragraph(); add(p); return p; }
    public Paragraph paragraph(String s) { Paragraph p=new Paragraph(s); add(p); return p; }
    public Form form() { Form f=new Form(); add(f); return f; }
    public ServiceCell serviceCell(String title, String targeturl) { ServiceCell sc=new ServiceCell(title,targeturl); add(sc); return sc; }

    public String toHtml(State st) {
        return super.toHtml(st);
    }
    
    /*
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
    public Page p(String s) { return para(s); }
    public Page p() { return p(""); }
    public Page para(String content) { return raw("<p>"+content+"</p>"); }
    public Page startForm() { return raw("<form method=post>"); }
    public Page endForm() { return raw("</form>"); }
    public Page label(String label) { if (!label.endsWith(":")) { label+=":"; } return raw("<b>"+label+"</b> "); }
    public Page textInput(String fieldname) { return raw("<input "+autofocusString()+" type=text name=\""+fieldname+"\">"); }
    public Page passwordInput(String fieldname) { return raw("<input "+autofocusString()+" type=password name=\""+fieldname+"\">"); }
    public Page linebreak() { return raw("<br>"); }
    public Page br() { return linebreak(); }
    public Page submit(String label) { return raw("<button type=submit name=\""+label+"\" value=\""+label+"\">"+label+"</button>"); }
    public Page dumpParameters() {
        Map<String, String> p = State.get().getParameters();
        for (String k:p.keySet()) { raw("<p>"+k+"="+p.get(k)+"</p>"); }
        return this;
    }
    public Page error(String errormessage) { return raw("<font color=red><b>"+errormessage+"</b></font>"); }
    public Page buttonGET(String buttonlabel,String url) { return raw("<a href=\""+url+"\"><button type=submit>"+buttonlabel+"</button></a>"); }
    public Page errorBlock(String error) { return raw("<br><span style=\"margin: 10px; padding:5px; border-style: solid; border-width: 2; border-color: red;\">"+error+"</span><br><br>"); }
*/
}
