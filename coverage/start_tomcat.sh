STARTED=0
for i in `seq 1 10`; do
    /etc/init.d/tomcat7 start
    sleep 10
    if [[ $(ps aux | grep "catalina.base" | grep -v grep | wc -l) -eq 0 ]]
    then
        echo "Tomcat not running"
    else
        echo "Tomcat successfully started"
        STARTED=1
        break
    fi
done

if [[ $STARTED -ne 1 ]] ; then
    print "Was unable to start tomcat"
    exit -1
fi
