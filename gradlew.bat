@ECHO OFF
SETLOCAL

SET "APP_NAME=Gradle"
SET "APP_BASE_NAME=%~n0"
SET "APP_HOME=%~dp0"
SET "CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"

IF DEFINED JAVA_HOME (
    SET "JAVA_CMD=%JAVA_HOME%\bin\java"
) ELSE (
    SET "JAVA_CMD=java"
)

"%JAVA_CMD%" -cp "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
