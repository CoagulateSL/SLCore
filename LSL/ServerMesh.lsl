setshard(key k) { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,0,k,<1,1,1>,<0,0,0>,0]); }
setlogo(key k) { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,1,k,<1,1,1>,<0,0,0>,0]); }
autosetshard() {
	setDev(FALSE);
	key k=SLCORE_COAGULATE_LOGO;
	if (DEV) { k=SLCORE_COAGULATE_DEV_LOGO; }
	setshard(k);
}