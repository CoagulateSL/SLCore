integer findPrim(string primname) {
	primname=llToLower(primname);
	integer p=llGetNumberOfPrims();
	for (;p>0;p--)	{
		if (primname==llToLower(llGetLinkName(p))) {
			return p;
		}
	}
	return -1;
}
