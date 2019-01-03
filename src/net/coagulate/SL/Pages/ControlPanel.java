package net.coagulate.SL.Pages;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.HTTPPipelines.State;
import net.coagulate.SL.Maintenance;

/**
 *
 * @author Iain Price
 */
public class ControlPanel extends Page {

    @Url("/ControlPanel")
    public ControlPanel(){super();}
    @Override
    public void content() {
        if (!State.get().user().superuser()) {
            throw new SystemException("Unauthorised access to Control Panel from "+State.get().user());
        }
        pageHeader("Control Panel");
        centralisePage();
        State state=State.get();
        if (state.get("Test Mail").equals("Test Mail"))
        {
            para("Sending mail");
            try { MailTools.mail("CL Cluster "+Config.getHostName(),"sl-cluster-alerts@predestined.net","SL Mail Tester", "sl-cluster-alerts@predestined.net", "SL Cluster mail test", "Test OK"); }
            catch (MessagingException ex) {
                Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE, null, ex);
                raw(ExceptionTools.toHTML(ex));
            }
            para("Sent mail");
        }
        if (state.get("UserException").equals("UserException")) { throw new UserException("Manually triggered user exception"); }
        if (state.get("SystemException").equals("SystemException")) { throw new SystemException("Manually triggered system exception"); }
        if (state.get("Region Stats Archival").equals("Region Stats Archival")) {
            para("Running Region State");
            Maintenance.regionStatsArchival();
        }
        startForm();
        submit("Test Mail");
        submit("Region Stats Archival");
        submit("UserException");
        submit("SystemException");
        endForm();
    }

    
}
