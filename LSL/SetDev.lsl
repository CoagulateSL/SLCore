#ifndef _INCLUDE_SETDEV_HEADER
#define _INCLUDE_SETDEV_HEADER
integer DEV=0;
string DEV_INJECT="";
setDev(integer noisy) {
	if (llSubStringIndex(llGetObjectDesc(),"DEV")==0) {
		DEV=1; DEV_INJECT="dev";
		if (noisy) { llWhisper(0,"In development mode"); }
	} else {
		DEV=0; DEV_INJECT="";
	}
}
#endif
