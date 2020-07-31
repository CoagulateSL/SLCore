// This file should go in the level ABOVE your git repository, the root of where the FS compile path sits
// It contains all the secrets the LSL codes need, mostly as defines

// Set this to a random string of 10-20 chars, it's used to "salt" digest generation.  Needs to be the same as in the .properties file though ;)
#define DIGEST_SALT "PutSomethingRandomHere"
// Set this to a number between -2.4billion and +2.4 billion.  A random 9 digit number is good.  If you leave this set to zero you'll possibly annoy GM Meter (user is spamming self), at least you did a decade ago.
#define CHANNEL_MUTATOR 123456789
// Your developer key
#define COMMS_DEVKEY "PutYourDeveloperKeyHere"
// the raw FQDN host name of your server (will expect e.g. /GPHUD/system to work.  Requires HTTP and HTTPS access to be working with valid certificate for HTTPS)
#define SERVER_HOSTNAME "your.hostname.com"
// avatar UUID for the "developer" (you probably), gives access to fake startups, and diagnostic/repackaging commands
#define SYSTEM_OWNER_UUID "12345678-90ab-cdef-1234-567890abcdef"
