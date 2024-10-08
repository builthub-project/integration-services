#!/bin/bash
docker kill platform-integration-services
docker container rm platform-integration-services
docker rmi -f platform-integration-services

