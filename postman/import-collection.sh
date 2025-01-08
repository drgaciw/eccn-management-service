#!/bin/bash

# Check if newman is installed
if ! command -v newman &> /dev/null
then
    echo "newman could not be found, installing..."
    npm install -g newman
fi

# Import collection and set environment variables
newman run "postman/ECCN-Management.postman_collection.json" \
    --env-var "auth_url=http://localhost:8081/auth" \
    --env-var "product_service_url=http://localhost:8080" \
    --env-var "classification_service_url=http://localhost:8081" \
    --env-var "risk_service_url=http://localhost:8082" \
    --env-var "compliance_service_url=http://localhost:8083" \
    --reporters cli,json \
    --reporter-json-export "postman/report.json"

echo "Collection imported successfully. Test report saved to postman/report.json"