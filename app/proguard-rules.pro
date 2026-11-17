# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in proguard-android-optimize.txt.

# Left empty on purpose. Every dependency that needs keep rules (Room, WorkManager, Glance,
# kotlinx.serialization, Coil) ships them as consumer rules, so the release build shrinks cleanly
# with no missing-class warnings. Add a rule here only when R8 reports one a library does not ship.

# R8's optimisations are not deterministic here: its outliner synthesises one shared method for
# compatible call sites and takes the parameter type from whichever method it saw first, so the
# same commit can produce either `Continuation` or `ContinuationImpl` in that signature. That
# changes pg_map_hash, which R8 embeds in the dex as `r8-map-id-...`, and therefore the APK bytes.
# Turning the optimisations off stops the outlining; shrinking and obfuscation still apply.
-dontoptimize
