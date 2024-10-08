#!/bin/bash
docker build -t platform-integration-services .
docker run -d -it --restart=always --network=microsrv-net -p 9082:9091 --name platform-integration-services platform-integration-services


