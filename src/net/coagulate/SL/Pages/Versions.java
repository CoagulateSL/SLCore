package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Versions {
	
	@Url(url="/versions", authenticate=false)
	public static void versionInformation(@Nonnull final State state) {
		//todo set PAGE LAYOUT CENTER COLUMN
		state.add(SL.htmlVersionDump());
	}
	
}
