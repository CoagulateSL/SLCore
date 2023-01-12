package net.coagulate.SL.Pages;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.HTML.Elements.Paragraph;
import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * Provides functions that can only be called from the server its self, for managing the system.
 *
 * @author Iain Price
 */
public class SystemManagement {
	
	/**
	 * Allows the local server to initiate a shutdown of the application.
	 *
	 * @param state State
	 */
	@Url(url="/shutdown", authenticate=false)
	public static void shutdown(@Nonnull final State state) {
		final String ip=State.get().getClientIP();
		if (ip!=null&&ip.isEmpty()) {
			SL.shutdown();
			state.add(new Paragraph("SHUTDOWN INITIATED"));
		} else {
			throw new UserAccessDeniedException("Unauthorised access to shutdown from "+State.get().getClientIP());
		}
	}
	
	/**
	 * Restricts the caches to a minimal refresh period
	 *
	 * @param state State
	 */
	@Url(url="/cacheoff", authenticate=false)
	public static void cacheOff(@Nonnull final State state) {
		final String ip=State.get().getClientIP();
		if (ip!=null&&ip.isEmpty()) {
			net.coagulate.SL.Data.SystemManagement.restrictCaches();
			state.add(new Paragraph("Cache disabled"));
		} else {
			throw new UserAccessDeniedException(
					"Unauthorised access to cache management from "+State.get().getClientIP());
		}
	}
	
	/**
	 * Re-enables normal cache operation.
	 *
	 * @param state State
	 */
	@Url(url="/cacheon", authenticate=false)
	public static void cacheOn(@Nonnull final State state) {
		final String ip=State.get().getClientIP();
		if (ip!=null&&ip.isEmpty()) {
			net.coagulate.SL.Data.SystemManagement.unrestrictCaches();
			state.add(new Paragraph("Cache enabled"));
		} else {
			throw new UserAccessDeniedException(
					"Unauthorised access to cache management from "+State.get().getClientIP());
		}
	}
	
	
}