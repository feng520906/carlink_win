call ant release
java -jar ../buildtools/signapk.jar ../buildtools/platform.x509.pem ../buildtools/platform.pk8 bin/CarLauncher-release-unsigned.apk bin/CarLauncher-release.apk
zipalign -fv 4 bin/CarLauncher-release.apk bin/CarLauncher.apk