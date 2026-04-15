@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d E:\project\PaiAgent-One\backend
mvn spring-boot:run
