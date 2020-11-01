/**
#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
#define COMMS_INCLUDEDIGEST
#define COMMS_NOCHARACTER
#define COMMS_NOSETTEXT
#define COMMS_INTERFACE "something"
#define COMMS_DEVKEY "xxxxxxxxxxxx"
#define COMMS_DONT_CHECK_CALLBACK
#define COMMS_PROTOCOL "xx"
#define COMMS_SUPPRESS_LINK_MESSAGES
#include "SLCore/LSL/CommsV3.lsl"

call comms_setup() at "go"

	http_request(key id,string method,string body) {
		json=body; body="";
		if (comms_http_request(id,method)) { return; }
		
	http_response( key request_id, integer status, list metadata, string body ) {
		#ifdef DEBUG
		llOwnerSay("REPLY:"+body);
		#endif
		if (status!=200) {
			comms_error((string)status);
			comms_do_callback();
		}
		else
		{
			json=body;
			
			if (comms_http_response(request_id,status)) { return; }
			
			process(NULL_KEY);
		}
	}
**/
#include "configuration.lsl"
#include "SLCore/LSL/JsonTools.lsl"
#ifdef COMMS_INCLUDECOOKIE
string cookie="";
#endif
#ifdef COMMS_INCLUDECALLBACK
string comms_url="";
key comms_url_key=NULL_KEY;
#endif

integer BOOTSTAGE=0;
#define BOOT_COMMS 0
#define BOOT_APP 1
#define BOOT_COMPLETE 2

httpsend(string url) {
	#ifdef COMMS_INCLUDECOOKIE
	json=llJsonSetValue(json,["cookie"],cookie); 
	#endif
	
	#ifdef COMMS_INTERFACE
	json=llJsonSetValue(json,["interface"],COMMS_INTERFACE);
	#endif
	
	#ifdef COMMS_NOCHARACTER
	json=llJsonSetValue(json,["runasnocharacter"],"set");
	#endif
	
	#ifdef COMMS_INCLUDECALLBACK
	if (comms_url!="") { json=llJsonSetValue(json,["callback"],comms_url); }
	if (comms_url!="") { json=llJsonSetValue(json,["url"],comms_url); }
	#endif
	
	#ifdef COMMS_DEVKEY
    json=llJsonSetValue(json,["developerkey"],COMMS_DEVKEY);
	#endif 
	
	#ifdef COMMS_PROTOCOL
	json=llJsonSetValue(json,["protocol"],COMMS_PROTOCOL);
	#endif
	
	#ifdef COMMS_INCLUDEDIGEST
	integer now=llGetUnixTime();
	json=llJsonSetValue(json,["timestamp"],(string)now);
	json=llJsonSetValue(json,["objectkey"],((string)llGetKey()));
	json=llJsonSetValue(json,["digest"],llSHA1String(((string)llGetKey())+((string)now)+DIGEST_SALT));
	#endif
	
	string devinject=""; if (DEV) { devinject="dev."; }
	string SERVER_URL="http://"+devinject+SERVER_HOSTNAME+"/"+url;
	//llOwnerSay(llGetScriptName()+": Sending to "+SERVER_URL+"\n"+json);
	llHTTPRequest(SERVER_URL,[HTTP_METHOD,"POST"],json);
}
httpcommand(string command,string url) {
    json=llJsonSetValue(json,["command"],command);
	httpsend(url);
}
setupHTTPIn() {
	if (comms_url!="") { llReleaseURL(comms_url); }
	comms_url="";
	comms_url_key=llRequestURL();
}
comms_setup() {
	if (BOOTSTAGE==BOOT_COMMS) { setupHTTPIn(); }
}
integer comms_http_request(key id,string method) {
	if (method==URL_REQUEST_DENIED && id==comms_url_key) {
		#ifdef COMMS_NOSETTEXT
		llOwnerSay("Failed to get an incoming URL: "+json+", retry in 60 seconds");
		#else
		llSetText("Failed to get an incoming URL: "+json+", retry in 60 seconds",<1,.5,.5>,1);
		#endif
		llSleep(60);
		setupHTTPIn();
		return TRUE;
	}
	if (method==URL_REQUEST_GRANTED && id==comms_url_key) {
		#ifdef DEBUG
		#ifndef COMMS_DONT_CHECK_CALLBACK
		llOwnerSay("URL complete, verifying with callback server...");
		#else
		llOwnerSay("URL complete");
		#endif
		#endif
		comms_url_key=NULL_KEY;
		comms_url=json;			
		#ifndef COMMS_DONT_CHECK_CALLBACK
		comms_do_callback();
		#else
		BOOTSTAGE++;
		#ifndef COMMS_SUPPRESS_LINK_MESSAGES
		llMessageLinked(LINK_THIS,LINK_SET_STAGE,(string)BOOTSTAGE,NULL_KEY);
		#endif
		#ifdef DEBUG
		llOwnerSay("URL request granted comms V3 calling setup()");
		#endif
		setup();
		#endif
		return TRUE;
	}
	if (jsonget("command")=="CallBack" && jsonget("url")==comms_url) {
		if (BOOTSTAGE==BOOT_COMMS) {
			llHTTPResponse(id,200,json);
			BOOTSTAGE++;
			#ifndef COMMS_SUPPRESS_LINK_MESSAGES
			llMessageLinked(LINK_THIS,LINK_SET_STAGE,(string)BOOTSTAGE,NULL_KEY);
			#endif
			#ifdef DEBUG
			llOwnerSay("Callback complete, return to setup in stage 1!");
			#endif
			setup();
		}
		return TRUE;
	}
	
	return FALSE;
}
#ifdef COMMS_INCLUDEDIGEST
comms_do_callback() {
	httpcommand("CallBack","SecondLifeAPI/CallBack");
}
#endif
comms_error(string message) {
	llOwnerSay(llGetScriptName()+" : Stack Server failed.  Please retry your last operation.");
}
integer comms_http_response(key request_id,integer status) {
	return FALSE;
}