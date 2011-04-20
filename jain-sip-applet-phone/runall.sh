nohup ant deploy-linux < /dev/null > /tmp/webserver.out 2>&1 &
nohup ant gateway < /dev/null > /tmp/gateway.out 2>&1 &
nohup ant responder  < /dev/null > /tmp/responder.out 2>&1 &
