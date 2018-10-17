package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.StringHandler;
import org.apache.http.HttpStatus;

/**
 *
 * @author Iain Price
 */
public class FourZeroFour extends StringHandler {

    @Override
    protected String handleString() {
        return "<h1 align=center>Four Hundred and Four</h1><br><br><p align=center>As in, 404, Page Not Found</p><br><br><br><br><p align=center>The requested URI was not mapped to a page handler.</p>";
    }
 
    public int getReturnStatus() {
        return HttpStatus.SC_NOT_FOUND;
    }
}
