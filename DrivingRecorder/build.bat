call ant release
java -jar ../buildtools/signapk.jar ../buildtools/platform.x509.pem ../buildtools/platform.pk8 bin/DrivingRecorder-release-unsigned.apk bin/DrivingRecorder-release.apk
zipalign -fv 4 bin/DrivingRecorder-release.apk bin/DrivingRecorder.apk