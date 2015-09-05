@echo off

if not exist zhicheng md zhicheng

cd ./CarLauncher
call ./build.bat
cd ..

cd ./DrivingRecorder
call ./build.bat
cd ..

cd ./SmartKey
call ./build.bat
cd ..

cd ./VoiceNow
call ./build.bat
cd ..

xcopy /y CarLauncher\bin\CarLauncher.apk zhicheng\
xcopy /y DrivingRecorder\bin\DrivingRecorder.apk zhicheng\
xcopy /y SmartKey\bin\SmartKey.apk zhicheng\
xcopy /y VoiceNow\bin\VoiceNow.apk zhicheng\