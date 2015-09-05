call ant release
java -jar ../build/signapk.jar ../build/platform.x509.pem ../build/platform.pk8 bin/SmartKey-release-unsigned.apk bin/SmartKey-release.apk
zipalign -fv 4 bin/SmartKey-release.apk bin/SmartKey.apk
