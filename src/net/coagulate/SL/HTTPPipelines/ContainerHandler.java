package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Pages.HTML.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public abstract class ContainerHandler extends StringHandler {

	// ----- Internal Instance -----
	@Nonnull
	@Override
	protected String handleString(@Nonnull final State state) {
		final Page container=new Page();
		run(state,container);
		return container.toHtml(state);
	}

	protected abstract void run(State state,
	                            Page page);

}