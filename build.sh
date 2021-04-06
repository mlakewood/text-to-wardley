#!/bin/bash

set -euf -o pipefail

lein deps
lein ci