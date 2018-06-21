all: start

help:
	@echo "Targets include the following:"
	@echo "help                 - show help information"
	@echo "start                - start dev environment"
	@echo "stop                 - stop dev environment"
	@echo "start_proxy          - start dev environment + mitmproxy"
	@echo "stop_proxy           - stop dev environment + mitmproxy"
	@echo "view_proxy           - view mitmproxy"

start:
	@docker-compose up -d

stop:
	@docker-compose stop
	@docker-compose rm -f

start_proxy:
	@docker-compose -f docker-compose.yml -f docker-compose.proxy.yml up -d

stop_proxy:
	@docker-compose -f docker-compose.yml -f docker-compose.proxy.yml stop
	@docker-compose -f docker-compose.yml -f docker-compose.proxy.yml rm -f

view_proxy:
	@docker-compose -f docker-compose.yml -f docker-compose.proxy.yml exec mitmproxy mitmproxy -r /tmp/traffic.mitm -p 9999
