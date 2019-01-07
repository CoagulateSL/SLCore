package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Pages.HTML.State;

/**
 *
 * @author Iain Price
 */
public abstract class ContainerHandler extends StringHandler {

    @Override
    protected String handleString(State state) {
        Page container=new Page();
        run(state,container);
        return pageHeader(state)+container.toHtml(state)+pageFooter(state);
    }
    
    protected abstract void run(State state,Page page);
    
}
