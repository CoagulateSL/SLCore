-- Secrets for the lua version of GPHUD
return {
    -- Set to a random string ; must match the string in your Java app's .properties file, basically a shared secret
    DIGEST_SALT = "PutSomethingRandomHere",
    -- Random mutator for channel number generation, do NOT set to zero as that might upset GM
    CHANNEL_MUTATOR = 123456789,
    -- Your developer key (stored in the database along with your user record)
    COMMS_DEVKEY = "PutYourDeveloperKeyHere",
    -- Server hostname, expects /GPHUD/system in here to be accessible
    SERVER_HOSTNAME = "your.hostname.com",
    -- UUID for the "developer" (allows access to developer commands)
    SYSTEM_OWNER_UUID = "12345678-90ab-cdef-1234-567890abcdef"
}
