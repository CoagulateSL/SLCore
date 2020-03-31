package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import javax.annotation.Nonnull;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Process a page into a String input/output
 *
 * @author Iain Price
 */
public abstract class StringHandler extends Handler {
	private static final boolean DEBUG_PARAMS=false;

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public StringEntity handleContent(@Nonnull final State state) {
		try {
			String content;
			try { content=handleString(state); }
			catch (@Nonnull final UserException ue) {
				SL.getLogger().log(WARNING,"PageHandlerCaught",ue);
				content="<p>Exception: "+ue.getLocalizedMessage()+"</p>";
			}
			return new StringEntity(content,ContentType.TEXT_HTML);
		}
		catch (@Nonnull final Exception ex) {
			SL.getLogger().log(SEVERE,"PageHandler",ex);
			state.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML);
		}
	}

	// ----- Internal Instance -----
	@Nonnull
	protected abstract String handleString(State state);

}
