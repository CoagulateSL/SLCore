package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Pages.HTML.State;

/**
 * @author Iain Price
 */
public abstract class AuthenticatedContainerHandler extends AuthenticatedStringHandler {

	@Override
	public String handleString(State state) {
		Page container = new Page();
		run(state, container);
		return container.toHtml(state);
	}

	protected abstract void run(State state, Page page);

}
