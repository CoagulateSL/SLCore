//#define OUTPUT_SAY_DEBUG
//#define OUTPUT_SAY
//#define OUTPUT_SAY_OWNER
//#define OUTPUT_SETTEXT
// pick one (or more)

output(string msg) {
#ifdef OUTPUT_SAY_DEBUG
	llSay(DEBUG_CHANNEL,msg);
#endif
#ifdef OUTPUT_SAY
	llSay(0,msg);
#endif
#ifdef OUTPUT_SETTEXT
	llSetText(msg,<1,1,1>,1);
#endif
#ifdef OUTPUT_SAY_OWNER
	llOwnerSay(msg);
#endif
}
