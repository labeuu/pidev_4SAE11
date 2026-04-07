<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.4
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@echo Cannot start maven from wrapper >&2 && exit /b 1
@GOTO :EOF
:: end batch / begin powershell #>

$ErrorActionPreference = "Stop"
if ($env:MVNW_VERBOSE -eq "true") {
  $VerbosePreference = "Continue"
}

# calculate distributionUrl, requires .mvn/wrapper/maven-wrapper.properties
$distributionUrl = (Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData).distributionUrl
if (!$distributionUrl) {
  Write-Error "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"
}

switch -wildcard -casesensitive ( $($distributionUrl -replace '^.*/','') ) {
  "maven-mvnd-*" {
    $distributionUrl = $distributionUrl -replace '-bin.zip$', '-windows-amd64.zip'
  }
}

$MVNW_REPOURL = $env:MVNW_REPOURL
if ($MVNW_REPOURL) {
  $distributionUrl = $distributionUrl -replace '^https?://[^/]+', $MVNW_REPOURL
}

$wrapperDir = "$scriptDir/.mvn/wrapper"
$zipName = $distributionUrl -replace '^.*/',''
$zipPath = "$wrapperDir/$zipName"
$mavenDirName = $zipName -replace '-bin.zip$',''
$mavenHome = "$wrapperDir/$mavenDirName"

if (!(Test-Path $wrapperDir)) { New-Item -ItemType Directory -Path $wrapperDir | Out-Null }

if (!(Test-Path $mavenHome)) {
  if (!(Test-Path $zipPath)) {
    Write-Verbose "Downloading Maven: $distributionUrl"
    $sec = $env:MVNW_PASSWORD
    $user = $env:MVNW_USERNAME
    if ($user -and $sec) {
      $pair = "$user`:$sec"
      $bytes = [Text.Encoding]::ASCII.GetBytes($pair)
      $basic = [Convert]::ToBase64String($bytes)
      Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath -Headers @{ Authorization = "Basic $basic" }
    } else {
      Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath
    }
  }
  Write-Verbose "Extracting Maven to $mavenHome"
  Expand-Archive -Path $zipPath -DestinationPath $wrapperDir -Force
}

$mvnCmd = Join-Path $mavenHome "bin/mvn.cmd"
if (!(Test-Path $mvnCmd)) { Write-Error "Maven executable not found at $mvnCmd" }

"MVN_CMD=$mvnCmd"

