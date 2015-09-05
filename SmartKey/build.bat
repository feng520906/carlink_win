call ant release
java -jar ../buildtools/signapk.jar ../buildtools/platform.x509.pem ../buildtools/platform.pk8 bin/SmartKey-release-unsigned.apk bin/SmartKey-release.apk
zipalign -fv 4 bin/SmartKey-release.apk bin/SmartKey.apk
