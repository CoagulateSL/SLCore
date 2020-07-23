#ifndef __JSONTOOLS_INCLUDE
#define __JSONTOOLS_INCLUDE
string json="";
string jsontwo="";

string jsonget(string attribute) {
	string ret=llJsonGetValue(json,[attribute]);
	if (ret=="" || ret==JSON_INVALID || ret==JSON_NULL || ret==NULL_KEY) { return ""; }
	return ret;
}
string jsontwoget(string attribute) {
	string ret=llJsonGetValue(jsontwo,[attribute]);
	if (ret=="" || ret==JSON_INVALID || ret==JSON_NULL || ret==NULL_KEY) { return ""; }
	return ret;
}
jsonput(string attribute,string value) {
	json=llJsonSetValue(json,[attribute],value);
}
#endif
