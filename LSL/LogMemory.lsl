integer LOG_MEMORY_MINIMUM=999999999;

logMemory() {
	if (llGetFreeMemory()<LOG_MEMORY_MINIMUM) { LOG_MEMORY_MINIMUM=llGetFreeMemory(); }
}