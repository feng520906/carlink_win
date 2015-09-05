#!/bin/sh
ant release
java -jar ../build/signapk.jar ../build/platform.x509.pem ../build/platform.pk8 bin/VoiceNow-release-unsigned.apk bin/VoiceNow-release.apk
zipalign -fv 4 bin/VoiceNow-release.apk bin/VoiceNow.apk
