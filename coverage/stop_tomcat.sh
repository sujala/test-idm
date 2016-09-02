# Stop TC.
STARTED=1
for i in `seq 1 10`; do
    /etc/init.d/tomcat7 stop
    sleep 10
    if [[ $(ps aux | grep "catalina.base" | grep -v grep | wc -l) -gt 0 ]]
    then
        echo "Tomcat still running"
    else
        echo "Tomcat successfully stopped"
        STARTED=0
        break
    fi
done

if [[ $STARTED -ne 0 ]] ; then
    print "Was unable to stop tomcat"
    exit -1
fi
