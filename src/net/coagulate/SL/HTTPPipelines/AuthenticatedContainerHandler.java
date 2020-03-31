package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Pages.HTML.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public abstract class AuthenticatedContainerHandler extends AuthenticatedStringHandler {

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String handleString(@Nonnull final State state) {
		final Page container=new Page();
		run(state,container);
		return container.toHtml(state);
	}

	// ----- Internal Instance -----
	protected abstract void run(State state,
	                            Page page);

}
