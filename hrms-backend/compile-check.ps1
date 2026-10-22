$env:JAVA_HOME = 'D:\Java\jdk-11.0.1'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
& "$PSScriptRoot\.mvn\apache-maven-3.9.16\bin\mvn.cmd" compile
exit $LASTEXITCODE
