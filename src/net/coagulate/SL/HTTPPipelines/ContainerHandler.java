package net.coagulate.SL.HTTPPipelines;

/**
 *
 * @author Iain Price
 */
public abstract class ContainerHandler extends StringHandler {

    @Override
    protected String handleString(State state) {
        Page container=new Page();
        run(state,container);
        return container.toHtml();
    }
    
    protected abstract void run(State state,Page page);
    
}
