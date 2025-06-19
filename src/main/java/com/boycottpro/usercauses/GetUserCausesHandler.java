package com.boycottpro.usercauses;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.boycottpro.models.UserCauses;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class GetUserCausesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "";
    private final DynamoDbClient dynamoDb;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GetUserCausesHandler() {
        this.dynamoDb = DynamoDbClient.create();
    }

    public GetUserCausesHandler(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            Map<String, String> pathParams = event.getPathParameters();
            String userId = (pathParams != null) ? pathParams.get("user_id") : null;
            if (userId == null || userId.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\":\"Missing user_id in path\"}");
            }
            String causeId = (pathParams != null) ? pathParams.get("cause_id") : null;
            if (causeId == null || causeId.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\":\"Missing cause_id in path\"}");
            }
            UserCauses userCause = getUserCauses(userId, causeId);
            String responseBody = objectMapper.writeValueAsString(userCause);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(responseBody);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Unexpected server error: " + e.getMessage() + "\"}");
        }
    }
    public UserCauses getUserCauses(String userId, String causeId) {
        QueryRequest request = QueryRequest.builder()
                .tableName("user_causes")
                .keyConditionExpression("user_id = :uid AND cause_id = :caid")
                .expressionAttributeValues(Map.of(
                        ":uid", AttributeValue.builder().s(userId).build(),
                        ":caid", AttributeValue.builder().s(causeId).build()
                ))
                .projectionExpression("user_id, cause_id, cause_desc, #ts")
                .expressionAttributeNames(Map.of(
                        "#ts", "timestamp"
                ))
                .build();

        QueryResponse response = dynamoDb.query(request);
        UserCauses userCause = new UserCauses();
        // should only be 1 record here
        for (Map<String, AttributeValue> item : response.items()) {
            String causeDesc = item.getOrDefault("cause_desc", AttributeValue.fromS("")).s();
            String timestamp = item.getOrDefault("timestamp", AttributeValue.fromS("")).s();
            userCause = new UserCauses(userId, causeId, causeDesc,timestamp);
        }
        return userCause;
    }
}