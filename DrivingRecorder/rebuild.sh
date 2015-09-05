#!/bin/sh
ant clean
ant release
java -jar ../build/signapk.jar ../build/platform.x509.pem ../build/platform.pk8 bin/DrivingRecorder-release-unsigned.apk bin/DrivingRecorder-release.apk
zipalign -fv 4 bin/DrivingRecorder-release.apk bin/DrivingRecorder.apk
