# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in proguard-android-optimize.txt.

# Left empty on purpose. Every dependency that needs keep rules (Room, WorkManager, Glance,
# kotlinx.serialization, Coil) ships them as consumer rules, so the release build shrinks cleanly
# with no missing-class warnings. Add a rule here only when R8 reports one a library does not ship.
