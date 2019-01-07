package net.coagulate.SL.Pages;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.AuthenticatedContainerHandler;
import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Maintenance;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;

/**
 *
 * @author Iain Price
 */
public class ControlPanel extends AuthenticatedContainerHandler {

    @Url("/ControlPanel")
    public ControlPanel(){super();}
    @Override
    protected void run(State state, Page page) {
        if (!state.user().superuser()) {
            throw new SystemException("Unauthorised access to Control Panel from "+state.user());
        }
        page.layout(Page.PAGELAYOUT.CENTERCOLUMN);
        page.header("Control Panel");
        if (state.get("Test Mail").equals("Test Mail"))
        {
            page.paragraph("Sending mail");
            try { MailTools.mail("CL Cluster "+Config.getHostName(),"sl-cluster-alerts@predestined.net","SL Mail Tester", "sl-cluster-alerts@predestined.net", "SL Cluster mail test", "Test OK"); }
            catch (MessagingException ex) {
                Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE, null, ex);
                page.add(new Raw(ExceptionTools.toHTML(ex)));
            }
            page.paragraph("Sent mail");
        }
        if (state.get("UserException").equals("UserException")) { throw new UserException("Manually triggered user exception"); }
        if (state.get("SystemException").equals("SystemException")) { throw new SystemException("Manually triggered system exception"); }
        if (state.get("Region Stats Archival").equals("Region Stats Archival")) {
            page.paragraph("Running Region State");
            Maintenance.regionStatsArchival();
        }
        page.form().
        submit("Test Mail").
        submit("Region Stats Archival").
        submit("UserException").
        submit("SystemException");
    }


    
}
