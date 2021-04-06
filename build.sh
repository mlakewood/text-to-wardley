#!/bin/bash

set -euf -o pipefail


npm install -g puppeteer
lein deps
lein ci