call ant release
java -jar ../build/signapk.jar ../build/platform.x509.pem ../build/platform.pk8 bin/CarLauncher-release-unsigned.apk bin/CarLauncher-release.apk
zipalign -fv 4 bin/CarLauncher-release.apk bin/CarLauncher.apk