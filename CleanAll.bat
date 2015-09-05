@echo off

if exist zhicheng (
	rd /s/q zhicheng
 ) 

cd ./CarLauncher
call ant clean
cd ..

cd ./DrivingRecorder
call ant clean
cd ..

cd ./VoiceNow
call ant clean
cd ..

cd ./3rdLibs/ViewPagerIndicator/library
call ant clean
cd ../../../

cd ./SmartKey
call ant clean
cd ..

cd ./Common
call ant clean
cd ..