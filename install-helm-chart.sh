#!/usr/bin/env bash
helm -n ml-images upgrade ml-image-scanner ./charts --wait --debug --install