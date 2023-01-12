package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.HTML.PageTemplate;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;

public class SLPageTemplate extends PageTemplate {
	private PAGELAYOUT layout=PAGELAYOUT.NONE;
	
	public SLPageTemplate() {
	}
	
	public SLPageTemplate(final PAGELAYOUT layout) {
		this.layout=layout;
	}
	
	public void layout(final PAGELAYOUT layout) {
		this.layout=layout;
	}
	
	@Override
	public String getHeader() {
		final State state=State.get();
		String r="<html><head><title>"+(Config.getDevelopment()?"DEV ":"")+SL.brandNameUniversal()+"</title>"+
		         "<link rel=\"shortcut icon\" href=\"/resources/icon-cluster"+(Config.getDevelopment()?"-dev":"")+
		         ".png\">"+"</head><body>"+"<p align=center>"+SL.getBannerHREF()+"</p><p><hr>";
		r+="<table width=100%><tr width=100%><td align=left width=400px>"+"Greetings";
		if (state.userNullable()!=null) {
			r+=",&nbsp;"+state.user().getUsername().replaceAll(" ","&nbsp;");
		}
		r+="</td><td align=center>";
		r+="<a href=\"/\">[&nbsp;Home&nbsp;]</a>";
		r+="</td><td align=right width=400px>";
		r+="<a href=\"/Logout\">[&nbsp;Logout&nbsp;]</a>"+"&nbsp;&nbsp;&nbsp;"+"</span>";
		r+="</td></tr></table>";
		r+="<hr></p>";
		if (layout==PAGELAYOUT.CENTERCOLUMN) {
			r+="<p align=center><table><tr><td style=\"max-width: 800px;\">";
		}
		return r;
	}
	
	@Override
	public String getFooter() {
		//State state=State.get();
		String ret="<div style='position:absolute;bottom:5;right:5;left:5;'><hr>";
		ret+=(Config.getDevelopment()?"DEVELOPMENT":"Production");
		ret+=" // "+Config.getHostName();
		ret+=" // <a href=\"/versions\">"+SL.getStackBuildDate()+" </a>";
		if (!Config.isOfficial()) {
			ret+=" // Operated by "+Config.getBrandingOwnerHumanReadable();
		}
		ret+="<span style='display:block;float:right;'>(C) ";
		if (Config.isOfficial()) {
			ret+="Coagulate SL (Iain Price)";
		} else {
			ret+="<a href=\"https://sl.coagulate.net/landingpage\">Coagulate SL (Iain Price)</a>";
		}
		ret+=", Iain Maltz @ SecondLife</span></div></body></html>";
		if (layout==PAGELAYOUT.CENTERCOLUMN) {
			ret+="</td></td></table></p>";
		}
		return ret;
	}
	
	public enum PAGELAYOUT {NONE,CENTERCOLUMN}
}
