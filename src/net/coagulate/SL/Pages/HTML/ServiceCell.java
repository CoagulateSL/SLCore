package net.coagulate.SL.Pages.HTML;

/**
 *
 * @author Iain Price
 */
public class ServiceCell extends Container {
    
    private String target; private String title;
    public ServiceCell(String title,String targeturl) { target=targeturl; this.title=title; }
    @Override
    public String toHtml(State st) {
        return 
                "<a href=\""+target+"\">"+
                "<li style=\"vertical-align: top; white-space: normal; color:black; border-style: solid; border-width: 5px; height: 250px; width: 200px; text-align: center; margin: 5px; padding: 5px; list-style-type: none; display: inline-block;\">"+
                "<h3 align=center>"+title+"</h3>"+
                super.toHtml(st)+
                "</li></a>";
    }
}
