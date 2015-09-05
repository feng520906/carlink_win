call ant release
java -jar ../buildtools/signapk.jar ../buildtools/platform.x509.pem ../buildtools/platform.pk8 bin/VoiceNow-release-unsigned.apk bin/VoiceNow-release.apk
zipalign -fv 4 bin/VoiceNow-release.apk bin/VoiceNow.apk