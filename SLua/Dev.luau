local isdev=ll.SubStringIndex(ll.GetObjectDesc(), "DEV")==1

local function announce()
    if (isdev) then
        ll.Whisper(0,"In development mode; "..tostring(ll.GetFreeMemory()).." bytes free")
    end
end
local function dev()
    return isdev
end

return {
    announce=announce,
    dev=dev
}