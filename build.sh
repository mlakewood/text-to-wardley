#!/bin/bash

set -euf -o pipefail


npm install -g puppeteer

ls /opt/build/repo/node_modules/puppeteer/.local-chromium/linux-856583
lein deps
lein ci