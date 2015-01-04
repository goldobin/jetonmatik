#!/bin/bash

curl --basic -u 1901498a-9ded-4eba-82e77e9946c0e231:YHihutYPGdmkhHeO0CKeyIhDkjR5yogq \
    -H "Accept: application/json" \
    -X POST http://localhost:8080/token \
    --data "grant_type=client_credentials&scope=jetonmatik:clients:modify" \
    -D -

echo