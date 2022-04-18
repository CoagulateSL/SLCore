package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTML.Container;
import net.coagulate.Core.HTML.Elements.*;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.HTTP.URLMapper;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class HTMLMapper extends URLMapper<Method> {

    private static HTMLMapper singleton;
    public static synchronized URLMapper<Method> get() {
        if (singleton==null) { singleton=new HTMLMapper(); }
        return singleton;
    }

    private HTMLMapper() {
    }

    @Override
    protected void initialiseState(final HttpRequest request, final HttpContext context, final Map<String, String> parameters, final Map<String, String> cookies) {
        final State state = State.get();
        state.setupHTTP(request, context);
        state.parameters(parameters);
        state.cookies(cookies);
        state.page(Page.page());
        state.page().template(new SLPageTemplate());
    }

    @Override
    protected void loadSession() {
        if (State.get().cookies().containsKey("coagulateslsessionid")) {
            State.get().loadSession(State.get().cookies().get("coagulateslsessionid"));
        }
    }

    @Override
    protected boolean checkAuthenticationNeeded(@Nonnull final Method content) {
        //System.out.println("Check authentication needed on "+content);
        final Url url = content.getAnnotation(Url.class);
        if (url != null) {
            if (!url.authenticate()) {
                //System.out.println("Check authentication needed on "+content+" url noauth");
                return false;
            }
        }
        final UrlPrefix urlprefix = content.getAnnotation(UrlPrefix.class);
        if (urlprefix != null) {
            if (!urlprefix.authenticate()) {
                //System.out.println("Check authentication needed on "+content+" urlprefix noauth");
                return false;
            }
        }
        //System.out.println("Check authentication needed on "+content+" try logon");
        logon();
        //System.out.println("Check authentication needed on "+content+" returning state of user which is present=="+State.get().userNullable()==null?"NO":"YES");
        if (State.get().sessionId()==null) {
            Page.page().addHeader("Set-Cookie","coagulateslsessionid=none; HttpOnly; Path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure;");
        } else {
            Page.page().addHeader("Set-Cookie","coagulateslsessionid="+State.get().sessionId()+"; HttpOnly; Path=/; Secure;");
        }
        return State.get().userNullable() == null;
    }

    private void logon() {
        final State state = State.get();
        final Map<String, String> parameters = state.parameters();

        final String username = parameters.getOrDefault("login_username", "");
        final String password = parameters.getOrDefault("login_password", "");
        if (!password.isEmpty()) {
            state.parameters().put("login_password", "***CENSORED***");
        }

        if (!username.isBlank() && !password.isBlank()) {
            // direct password authentication!
            User u = null;
            try {
                u = User.findUsernameNullable(username, false);
            } catch (@Nonnull final NoDataException ignore) {
            }
            if (u == null) {
                SL.log().warning("Attempt to authenticate as invalid user '" + username + "' from " + state.getClientIP());
                return; // no login attempted
            }
            if (u.checkPassword(password)) {
                state.user(u);
                return; // successful login
            }
            SL.log().warning("Attempt to authenticate with incorrect password as '" + username + "' from " + state.getClientIP());
            return; // bad password
        }
        if (password.isBlank() && !username.isBlank() && SL.canIM()) {
            // password-less authentication, assuming we have an IM conduit
            final User target = User.findUsernameNullable(username, false);
            if (target != null) {
                final String token = target.generateSSO();
                final String message;
                if (Config.getDevelopment()) {
                    message = "\n\n===== DEVELOPMENT SSO ENTRY POINT =====\n\n\n[https://dev.sl.coagulate.net/SSO/" + token + " Log in to Coagulate SL DEVELOPMENT " + "ENVIRONMENT]\n\n";
                } else {
                    message = "\n\nPlease click the link below to log in to " + SL.brandNameUniversal() + "\n\nThis link will be valid for 5 minutes only, and one use.\n\n[https://" + Config.getURLHost() + "/SSO/" + token + " Log in to " + SL.brandNameUniversal() + "]\n\n";
                }

                SL.im(target.getUUID(), message);
            }
        }
    }

    @Override
    protected void executePage(final Method content) throws InvocationTargetException {
        // it's a static method with no parameters :)
        try {
            content.invoke(null, State.get());
        } catch (final IllegalAccessException e) {
            throw new SystemImplementationException("Method " + content.getDeclaringClass().getCanonicalName() + "." + content.getName() + " does not have public access", e);
        }
    }

    @Override
    protected void cleanup() {
        State.cleanup();
    }

    // ---------- INSTANCE ----------
    @Url(url = "/login",authenticate = false) // note the url part is irrelevant as this intercepts authentication required URLs.  You can go here too for fun.
    public static void getAuthenticationPage(@Nonnull final State state) {
        final Container content = new Container();
        final Form form = content.form();
        form.alignment("center");
        final Table table = form.table();
        table.row().data(getTop()).spanCell(2);
        table.row();
        table.row().data(new HorizontalRule()).spanCell(2).alignCell("center");
        table.row();
        table.row().data(new Header2("Login")).spanCell(2).alignCell("center");
        table.row();
        table.row().header("Avatar Name:").alignCell("right").data(new InputText("login_username").size(20).autofocus());
        table.row().header(SL.brandNameUniversal() + " Password:").alignCell("right").data(new InputPassword("login_password").size(20));
        if (SL.canIM()) {
            table.row().data("If you do not enter a password, your avatar will be IMed a login in "+ Config.getGridName()).spanCell(2).alignCell("center");
        }
        table.row().data(new ButtonSubmit("Login")).spanCell(2).alignCell("center");
        state.page().template(new SLPageTemplate(SLPageTemplate.PAGELAYOUT.CENTERCOLUMN));
        state.page().add(content);
    }

    private static Container getTop() {
        final Container top = new Container();
        top.add(new Paragraph(new Header1("Welcome to "+SL.brandNameUniversal())));
        if (Config.isOfficial()) {
            top.p("SL.Coagulate.net is an umbrella service that contains a set of different services used in Second Life.").
                    p().add(new PlainText("Currently this consists of the following:")).
                    add(new UnorderedList().
                            add(new ListItem("RPHUD -  Obsolete RP HUD (").
                                    a("https://www.coagulate.net/wiki/index.php/RPHUD","Limited Documentation Here").
                                    add(")")).
                            add(new ListItem("GPHUD - Next Generation RP HUD (").
                                    a("https://sl.coagulate.net/Docs/GPHUD/","Documentation").
                                    add(")")).
                            add(new ListItem("Quiet Life Rentals - Land rental company in Second Life (").
                                    a("https://coagulate.sl/Docs/QLR/","Documentation").
                                    add(")")))
                    .add(new Paragraph("These services are run by ").a("secondlife:///app/agent/8dc52677-bea8-4fc3-b69b-21c5e2224306/about","Iain Maltz"));
        } else {
            top.add(new Paragraph().a("https://sl.coagulate.net/landingpage","(C) Iain Maltz / Iain Price, Coagulate"));
            top.p("This service is operated by "+Config.getBrandingOwnerHumanReadable());
        }
        return top;
    }

    private State state() {
        return State.get();
    }

    @Override
    protected void renderUnhandledError(final HttpRequest request, final HttpContext context, final HttpResponse response, final Throwable t) {
        SL.report("SL UnkEx: " + t.getLocalizedMessage(), t, state());
        final Page page = Page.page();
        page.template(new SLPageTemplate(SLPageTemplate.PAGELAYOUT.CENTERCOLUMN));
        page.resetRoot();
        page.root().header1("Unhandled Internal Error");
        page.root().p("Sorry, an unhandled internal error occurred.");
        page.root().p("A developer has been notified of this issue.");
        page.responseCode(HttpStatus.SC_OK);
        processOutput(response, null);
    }

    @Override
    protected void renderSystemError(final HttpRequest request, final HttpContext context, final HttpResponse response, final SystemException systemException) {
        SL.report("SL SysEx: " + systemException.getLocalizedMessage(), systemException, state());
        final Page page = Page.page();
        page.template(new SLPageTemplate(SLPageTemplate.PAGELAYOUT.CENTERCOLUMN));
        page.resetRoot();
        page.root().header1("Internal Error");
        page.root().p("Sorry, an internal error occurred.");
        page.root().p("A developer has been notified of this issue.");
        page.responseCode(HttpStatus.SC_OK);
        processOutput(response, null);
    }

    @Override
    protected void renderUserError(final HttpRequest request, final HttpContext context, final HttpResponse response, final UserException userException) {
        SL.report("SL User: " + userException.getLocalizedMessage(), userException, state());
        final Page page = Page.page();
        page.template(new SLPageTemplate(SLPageTemplate.PAGELAYOUT.CENTERCOLUMN));
        page.resetRoot();
        page.root().header1("Error");
        page.root().p("Sorry, your request could not be completed, please review your data and try again");
        page.root().p("Error: " + userException.getLocalizedMessage());
        page.responseCode(HttpStatus.SC_OK);
        processOutput(response, null);
    }
}
