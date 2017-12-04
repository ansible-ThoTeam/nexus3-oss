#!/bin/bash

set -ev
./tests/test_groovySyntax.sh
molecule test
