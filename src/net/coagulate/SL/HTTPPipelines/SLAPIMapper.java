package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTTP.URLMapper;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.Crypto;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static java.util.logging.Level.WARNING;

public class SLAPIMapper extends URLMapper<Method> {

    private static SLAPIMapper singleton=null;
    public static synchronized URLMapper<Method> get() {
        if (singleton==null) { singleton=new SLAPIMapper(); }
        return singleton;
    }

    private SLAPIMapper() {
    }

    @Override
    protected void processPostEntity(HttpEntity entity, Map<String, String> parameters) {
        JSONObject json;
        String contentString="NOT READ";
        try {
            contentString = ByteTools.convertStreamToString(entity.getContent());
            json=new JSONObject(contentString);
        }
        catch (final JSONException jsonError) {
            SL.reportString("Unparsable JSON",jsonError,contentString);
            throw new SystemRemoteFailureException("Unparsable JSON input",jsonError);
        }
        catch (final IOException ioError) {
            throw new SystemRemoteFailureException("Input stream failed",ioError);
        }
        State.get().jsonIn(json);
    }

    @Override
    protected void initialiseState(HttpRequest request, HttpContext context, Map<String, String> parameters, Map<String, String> cookies) {
        State state=State.get();
        state.setupHTTP(request,context);
        processSLAPI(request,parameters);
        state.parameters(parameters);
        state.cookies(cookies);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private void processSLAPI(HttpRequest request, Map<String, String> parameters) {
        final String shard=requireHeader(request,"X-SecondLife-Shard");
        parameters.put("slapi_shard",shard);
        parameters.put("slapi_region",requireHeader(request,"X-SecondLife-Region"));
        parameters.put("slapi_ownername",optionalHeader(request,"X-SecondLife-Owner-Name"));
        parameters.put("slapi_ownerkey",requireHeader(request,"X-SecondLife-Owner-Key"));
        if (parameters.get("slapi_ownername")==null || parameters.get("slapi_ownername").isEmpty()) {
            final User userlookup=User.findUserKeyNullable(parameters.get("slapi_ownerkey"));
            if (userlookup!=null) {
                final String username=userlookup.getName();
                if (username!=null) { parameters.put("slapi_ownername",username); }
            }
        }
        parameters.put("slapi_objectname",requireHeader(request,"X-SecondLife-Object-Name"));
        final String objectkey=requireHeader(request,"X-SecondLife-Object-Key");
        parameters.put("slapi_objectkey",objectkey);
        parameters.put("slapi_objectvelocity",requireHeader(request,"X-SecondLife-Local-Velocity"));
        parameters.put("slapi_objectrotation",requireHeader(request,"X-SecondLife-Local-Rotation"));
        parameters.put("slapi_objectposition",requireHeader(request,"X-SecondLife-Local-Position"));
        if (!Config.skipShardCheck() && (!"Production".equalsIgnoreCase(shard))) {
            throw new SystemRemoteFailureException("Incorrect shard : "+shard);
        }
    }

    private String requireHeader(final HttpRequest req,
                                 final String header) {
        return getHeader(true,req,header);
    }

    private String optionalHeader(final HttpRequest req,
                                  final String header) {
        return getHeader(false,req,header);
    }

    private String getHeader(final boolean mandatory,
                             final HttpRequest req,
                             final String header) {
        final Header[] headerSet=req.getHeaders(header);
        if (headerSet.length==0) {
            if (!mandatory) { return null; }
            final SystemRemoteFailureException e=new SystemRemoteFailureException("Mandatory data was not supplied to SL API processor");
            SL.report("Missing mandatory header "+header,e,State.get());
            throw e;
        }
        if (headerSet.length>1) {
            final SystemRemoteFailureException e=new SystemRemoteFailureException("Too much mandatory data was supplied to SL API processor");
            SL.report("Excessive mandatory header "+header,e,State.get());
            throw e;
        }
        return headerSet[0].getValue();
    }

    @Override
    protected void loadSession() {
        State state=State.get();
        if (state.cookies().containsKey("coagulateslsessionid")) {
            state.loadSession(state.cookies().get("coagulateslsessionid"));
        }
    }

    @Override
    protected boolean checkAuthenticationNeeded(Method content) {
        return false;
    }

    @Override
    protected Method authenticationPage() {
        try {
            return getClass().getMethod("getAuthenticationPage");
        } catch (NoSuchMethodException e) {
            throw new SystemImplementationException("Authentication page went missing");
        }
    }

    @Override
    protected void executePage(Method content) {
        // it's a static method with no parameters :)
        Url url=content.getAnnotation(Url.class);
        UrlPrefix urlprefix=content.getAnnotation(UrlPrefix.class);
        try {
            if ((url!=null && url.digest()) ||
                    (urlprefix!=null && urlprefix.digest())) { checkDigest(); }
            content.invoke(null,State.get());
        } catch (IllegalAccessException e) {
            throw new SystemImplementationException("Method "+content.getDeclaringClass().getCanonicalName()+"."+content.getName()+" does not have public access");
        } catch (InvocationTargetException e) {
            throw new SystemImplementationException("Method "+content.getDeclaringClass().getCanonicalName()+"."+content.getName()+" thew an exception",e);// todo
        }
    }

    private void checkDigest() {
        final State state=State.get();
        Map<String, String> parameters = state.parameters();
        final String objectKey=parameters.get("slapi_objectkey");
        if (objectKey==null) {
            throw new SystemRemoteFailureException("No object owner key provided to Second Life API");
        }
        final String digest=state.jsonIn().getString("digest");
        if (digest==null) {
            throw new SystemRemoteFailureException("No digest provided to Second Life API");
        }
        final int timestamp=state.jsonIn().getInt("timestamp");
        // not (short-term) replay attack proof :(  still, the wires are /reasonably/ secure.  maybe later.  hmm)
        int timestampOffset= UnixTime.getUnixTime()-timestamp;
        if (timestampOffset<0) { timestampOffset=-timestampOffset; }
        if (timestampOffset>300) {
            throw new SystemRemoteFailureException("Timestamp deviates by more than 300 seconds");
        }

        final String theirObjectKey=state.jsonIn().getString("objectkey");
        if (theirObjectKey!=null && !theirObjectKey.isEmpty()) {
            if (!theirObjectKey.equals(objectKey)) {
                throw new SystemRemoteFailureException("Object key mismatch - headers generated "+objectKey+" but they think it's "+theirObjectKey);
            }
        }
        final String targetDigest= Crypto.SHA1(objectKey+timestamp+ Config.getDigestSalt());
        if (!targetDigest.equalsIgnoreCase(digest)) {
            throw new SystemRemoteFailureException("Incorrect digest provided to Second Life API");
        }
    }

    @Override
    protected void processOutput(HttpResponse response, Method content) {
        String stringOutput;
        try {
            if (State.get().jsonOutNullable()==null) { throw new SystemImplementationException("No response set up by "+content.getDeclaringClass().getCanonicalName()+"."+content.getName()); }
            stringOutput = State.get().jsonOut().toString();
        } catch (@Nonnull final UserException ue) {
            SL.log().log(WARNING, "PageHandlerCaught", ue);
            JSONObject error=new JSONObject();
            error.put("error",ue.getLocalizedMessage());
            stringOutput = error.toString();
        }
        response.setEntity(new StringEntity(stringOutput, ContentType.APPLICATION_JSON));
        response.setStatusCode(HttpStatus.SC_OK);

    }

    @Override
    protected void cleanup() {
        State.cleanup();
    }

    private State state() { return State.get(); }
    @Override
    protected void renderUnhandledError(HttpRequest request, HttpContext context, HttpResponse response, Throwable t) {
        SL.report("SLAPI UnkEx: "+t.getLocalizedMessage(),t,state());
        JSONObject json=new JSONObject();
        json.put("error","Sorry, an unhandled internal error occurred.");
        json.put("responsetype","UnhandledException");
        response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
        response.setStatusCode(200);
    }

    @Override
    protected void renderSystemError(HttpRequest request, HttpContext context, HttpResponse response, SystemException t) {
        SL.report("SLAPI SysEx: "+t.getLocalizedMessage(),t,state());
        JSONObject json=new JSONObject();
        json.put("error","Sorry, an internal error occurred.");
        json.put("responsetype","SystemException");
        response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
        response.setStatusCode(200);
    }

    @Override
    protected void renderUserError(HttpRequest request, HttpContext context, HttpResponse response, UserException t) {
        SL.report("SLAPI User: "+t.getLocalizedMessage(),t,state());
        JSONObject json=new JSONObject();
        json.put("error",t.getLocalizedMessage());
        json.put("responsetype","UserException");
        json.put("errorclass",t.getClass().getName());
        response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
        response.setStatusCode(200);
    }
}