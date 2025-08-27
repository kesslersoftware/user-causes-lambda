package com.boycottpro.usercauses;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.boycottpro.models.ResponseMessage;
import com.boycottpro.models.UserCauses;
import com.boycottpro.utilities.JwtUtility;
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
        String sub = null;
        try {
            sub = JwtUtility.getSubFromRestEvent(event);
            if (sub == null) return response(401, Map.of("message", "Unauthorized"));
            Map<String, String> pathParams = event.getPathParameters();
            String causeId = (pathParams != null) ? pathParams.get("cause_id") : null;
            if (causeId == null || causeId.isEmpty()) {
                ResponseMessage message = new ResponseMessage(400,
                        "sorry, there was an error processing your request",
                        "cause_id not present");
                return response(400,message);
            }
            UserCauses userCause = getUserCauses(sub, causeId);
            return response(200,userCause);
        } catch (Exception e) {
            System.out.println(e.getMessage() + " for user " + sub);
            return response(500,Map.of("error", "Unexpected server error: " + e.getMessage()) );
        }
    }
    private APIGatewayProxyResponseEvent response(int status, Object body) {
        String responseBody = null;
        try {
            responseBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(responseBody);
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
            userCause = new UserCauses(null, causeId, causeDesc,timestamp);
        }
        return userCause;
    }
}