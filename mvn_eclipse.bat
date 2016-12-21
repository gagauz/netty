call ./setenv.bat
call mvn eclipse:clean eclipse:eclipse -DdownloadSources=true -DdownloadSources -DresolveWorkspaceProjects=false
pause