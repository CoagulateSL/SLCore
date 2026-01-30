local secrets=require("../../CoagulateSL/SLua/secrets")
local dev = require("../../SLCore/SLua/Dev")
local comms_url_key=nil;
local comms_url=nil;
local broadcastchannel=nil;
local COMMS_PROTOCOL=5;

local function divLSL(a, b)
	return if a * b < 0 then math.ceil(a / b) else math.floor(a / b)
end

local function sign32(a)
    -- what you get for relying on signed 32 bit maths
    while (a>2^31) do
        a=a-2^32
    end
    while (a<-2^31) do
        a=a+2^32
    end
    return a
end

local function calculatebroadcastchannel()
	local loc:vector = ll.GetRegionCorner()
    local x: number = loc.x
    local y: number = loc.y
	x=divLSL(x,256)
	y=divLSL(y,256)
	x=bit32.band(x,0xffff)
	y=bit32.band(y,0x0fff)
	local output:number=sign32((y*0x10000)+x)
    ll.OwnerSay("BI1:"..tostring(output))
	output=sign32(bit32.bxor(output,secrets.CHANNEL_MUTATOR))
    ll.OwnerSay("BI2:"..tostring(output))
	output=-ll.Abs(output);
	if (output>-1000) then 
        output=output-99999
    end
    if (dev.dev()) then
        output=sign32(output-10)
        ll.OwnerSay("Broadcast:"..tostring(output))
    end
	broadcastchannel=output;
end

local function httpsend(data:table,url:string)
    -- cookie=cookie (NOT SET)
    -- interface=interface
    -- runasnocharacter="set"
    if (comms_url~=nil) then
        ll.OwnerSay("Attaching URL")
        data["callback"]=comms_url
        data["url"]=comms_url
    end
    data["developerkey"]=secrets.COMMS_DEVKEY
    data["protocol"]=COMMS_PROTOCOL
    -- IF DIGEST SET (it isn't)
    --local now:number=ll.GetUnixTime()
    --data["timestamp"]=now
    --data["objectkey"]=tostring(ll.GetKey())
    --data["digest"]=ll.SHA1String(tostring(ll.GetKey())..tostring(now)..secrets.DIGEST_SALT)
    local sendto=secrets.SERVER_HOSTNAME.."/"..url
    if (dev.dev()) then
        sendto="dev."..sendto
    end
    sendto="http://"..sendto
    ll.HTTPRequest(sendto,{HTTP_METHOD,"POST"},lljson.encode(data))
    ll.OwnerSay("SENT!")
    ll.OwnerSay(sendto)
    ll.OwnerSay(lljson.encode(data))
end

local function httpcommand(data:table,command:string,url:string)
    data["command"]=command
    httpsend(data,url)
end

local comms_ready_callback=nil
local comms_processor=nil

local function http_request(id:string,method:string,body:string)
    if (id==comms_url_key) then
        if (method==URL_REQUEST_GRANTED) then
            comms_url=body
            -- "check callback" (not enabled in SERVER)
            --httpcommand({},"CallBack","SecondLifeAPI/CallBack");
            comms_ready_callback()
        end
        if (method==URL_REQUEST_DENIED) then
            ll.OwnerSay("Failed to get an incoming URL:"..body.. " - Will retry in 60 seconds")
            ll.SetText("Failed to get an incoming URL\n"..body.. "\nWill retry in 60 seconds",vector(1,.5,.5),1)
            LLTimers:once(60,setup)
        end
        comms_url_key=nil
        return
    end
    ll.OwnerSay("Other response ["..method.."] = "..body)
end

local function http_response(id:string, status:number, metadata:{any}, body:string)
    if (status~=200) then
        ll.OwnerSay("Uh oh, response = "..tostring(number))
    end
    ll.OwnerSay(body)
    comms_processor(lljson.decode(body))
end

function setup(callwhenready,processor)
    if (comms_url~=nil) then
        ll.ReleaseURL(comms_url)
    else 
        LLEvents:on("http_request",http_request)
        LLEvents:on("http_response",http_response)
    end
    comms_ready_callback=callwhenready
    comms_processor=processor
    comms_url_key=ll.RequestURL()
    calculatebroadcastchannel()
end

local function broadcast(message:table)
    ll.RegionSay(broadcastchannel,lljson.encode(message))
end

return {
    setup=setup,
    httpcommand=httpcommand,
    broadcast=broadcast
}