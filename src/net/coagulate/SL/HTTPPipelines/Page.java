package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.SL.Config;
import net.coagulate.SL.Pages.HTML.*;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Page extends Container {

	private PAGELAYOUT layout = PAGELAYOUT.NONE;

	@Nonnull
	public static String pageHeader(@Nonnull State state) {
		//new Exception().printStackTrace();
		String r = "<html><head><title>"+(SL.DEV?"DEV ":"")+"Coagulate SL</title>"
				+ "<link rel=\"shortcut icon\" href=\"/resources/icon-cluster" + (SL.DEV ? "-dev" : "") + ".png\">"
				+ "</head><body>"
				+ "<p align=center>" + SL.getBannerHREF() + "</p><p><hr>";
		r += "<table width=100%><tr width=100%><td align=left width=400px>"
				+ "Greetings";
		if (state.userNullable() != null) { r += ",&nbsp;" + state.userNullable().getUsername().replaceAll(" ", "&nbsp;"); }
		r += "</td><td align=center>";
		r += "<a href=\"/\">[&nbsp;Home&nbsp;]</a>";
		r += "</td><td align=right width=400px>";
		r += "<a href=\"/Info\">[Info]</a>" + "&nbsp;&nbsp;&nbsp;";
		if (state.userNullable() != null) {
			r += "<a href=\"/Billing\">[&nbsp;Billing&nbsp;(L$" + state.userNullable().balance() + ")&nbsp;]</a>"
					+ "&nbsp;&nbsp;&nbsp;"
					+ "<a href=\"/Account\">[&nbsp;Account&nbsp;]</a>"
					+ "&nbsp;&nbsp;&nbsp;"
					+ "<a href=\"/Logout\">[&nbsp;Logout&nbsp;]</a>"
					+ "&nbsp;&nbsp;&nbsp;"
					+ "</span>";
		}
		r += "</td></tr></table>";
		r += "<hr></p>";
		return r;
	}

	@Nonnull
	public static String pageFooter(State state) {
		String ret = "<div style='position:absolute;bottom:5;right:5;left:5;'><hr>";
		ret += (SL.DEV ? "DEVELOPMENT" : "Production");
		ret += " // " + Config.getHostName();
		ret += "<span style='display:block;float:right;'>(C) Iain Maltz @ Second Life</span></div></body></html>";
		return ret;
	}

	public void layout(PAGELAYOUT layout) { this.layout = layout; }

	@Nonnull
	public String preLayout() {
		if (layout == PAGELAYOUT.NONE) { return ""; }
		if (layout == PAGELAYOUT.CENTERCOLUMN) { return "<p align=center><table><tr><td style=\"max-width: 800px;\">"; }
		throw new SystemException("Unhandled pre-layout " + layout);
	}

	@Nonnull
	public String postLayout() {
		if (layout == PAGELAYOUT.NONE) { return ""; }
		if (layout == PAGELAYOUT.CENTERCOLUMN) { return "</td></td></table></p>"; }
		throw new SystemException("Unhandled post-layout " + layout);
	}

	@Nonnull
	public Header1 header(String header) {
		Header1 h = new Header1(header);
		add(h);
		return h;
	}

	@Nonnull
	public Paragraph paragraph() {
		Paragraph p = new Paragraph();
		add(p);
		return p;
	}

	@Nonnull
	public Paragraph paragraph(String s) {
		Paragraph p = new Paragraph(s);
		add(p);
		return p;
	}

	@Nonnull
	public Form form() {
		Form f = new Form();
		add(f);
		return f;
	}

	@Nonnull
	public ServiceCell serviceCell(String title, String targeturl) {
		ServiceCell sc = new ServiceCell(title, targeturl);
		add(sc);
		return sc;
	}

	@Nonnull
	public URLButton urlbutton(String label, String url) {
		URLButton ub = new URLButton(label, url);
		add(ub);
		return ub;
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

	@Nonnull
	public String toHtml(@Nonnull State st) {
		return
				pageHeader(st) +
						preLayout() +
						super.toHtml(st) +
						postLayout() +
						pageFooter(st);
	}

	public enum PAGELAYOUT {NONE, CENTERCOLUMN}
}
