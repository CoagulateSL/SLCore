package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.HTML.PageTemplate;
import net.coagulate.Core.HTTP.URLMapper;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static java.util.logging.Level.WARNING;

public class PlainTextMapper extends URLMapper<Method> {

    public static class PlainTextTemplate extends PageTemplate {

        @Override
        public String getHeader() {
            return "";
        }

        @Override
        public String getFooter() {
            return "";
        }
    }

    private static PlainTextMapper singleton;
    public static synchronized URLMapper<Method> get() {
        if (singleton==null) { singleton=new PlainTextMapper(); }
        return singleton;
    }

    private PlainTextMapper() {
    }

    @Override
    protected void processPostEntity(final HttpEntity entity, final Map<String, String> parameters) {
    }

    @Override
    protected void initialiseState(final HttpRequest request, final HttpContext context, final Map<String, String> parameters, final Map<String, String> cookies) {
        final State state = State.get();
        state.setupHTTP(request, context);
        state.parameters(parameters);
        state.cookies(cookies);
        state.page(Page.page());
        state.page().template(new PlainTextTemplate());
    }

    @Override
    protected void loadSession() {
    }

    @Override
    protected boolean checkAuthenticationNeeded(final Method content) {
        return false;
    }

    @Override
    protected Method authenticationPage() {
        throw new SystemImplementationException("The plain text mapper does not support an authentication step");
    }

    @Override
    protected void executePage(final Method content) {
        // it's a static method with no parameters :)
        try {
            content.invoke(null, State.get());
        } catch (final IllegalAccessException e) {
            throw new SystemImplementationException("Method " + content.getDeclaringClass().getCanonicalName() + "." + content.getName() + " does not have public access", e);
        } catch (final InvocationTargetException e) {
            throw new SystemImplementationException("Method " + content.getDeclaringClass().getCanonicalName() + "." + content.getName() + " thew an exception", e);// todo
        }
    }


    @Override
    protected int processOutput(final HttpResponse response, final Method content) {
        String stringOutput;
        try {
            stringOutput = Page.page().render();
        } catch (@Nonnull final UserException ue) {
            SL.log().log(WARNING, "PageHandlerCaught", ue);
            final JSONObject error = new JSONObject();
            error.put("error", ue.getLocalizedMessage());
            stringOutput = error.toString();
        }
        response.setEntity(new StringEntity(stringOutput, getContentType()));
        response.setStatusCode(HttpStatus.SC_OK);
        return stringOutput.length();
    }

    protected ContentType getContentType() {
        return Page.page().contentType()==null?ContentType.TEXT_PLAIN:Page.page().contentType();
    }

    @Override
    protected void cleanup() {
        State.cleanup();
    }

    private State state() {
        return State.get();
    }

    @Override
    protected void renderUnhandledError(final HttpRequest request, final HttpContext context, final HttpResponse response, final Throwable t) {
        SL.report("PText UnkEx: " + t.getLocalizedMessage(), t, state());
        String text = "Error: Sorry, an unhandled internal error occurred.\n";
        text += "Type: UnhandledException";
        response.setEntity(new StringEntity(text, getContentType()));
        response.setStatusCode(200);
    }

    @Override
    protected void renderSystemError(final HttpRequest request, final HttpContext context, final HttpResponse response, final SystemException t) {
        SL.report("PText SysEx: " + t.getLocalizedMessage(), t, state());
        String text = "Error: Sorry, an internal error occurred.\n";
        text += "Type: SystemException";
        response.setEntity(new StringEntity(text, ContentType.TEXT_PLAIN));
        response.setStatusCode(200);
    }

    @Override
    protected void renderUserError(final HttpRequest request, final HttpContext context, final HttpResponse response, final UserException t) {
        SL.report("PText User: " + t.getLocalizedMessage(), t, state());
        String text = "Error: " + t.getLocalizedMessage() + "\n";
        text += "Type: UserException\n";
        text += "Class: " + t.getClass().getName();
        response.setEntity(new StringEntity(text, ContentType.TEXT_PLAIN));
        response.setStatusCode(200);
    }
}
