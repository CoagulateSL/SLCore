setshard(key k) { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,0,k,<1,1,1>,<0,0,0>,0]); }
setlogo(key k) { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,1,k,<1,1,1>,<0,0,0>,0]); }
autosetshard() {
	setDev(FALSE);
	key k=LOGO_COAGULATE;
	if (DEV) { k=LOGO_COAGULATE_DEV; }
	setshard(k);
}