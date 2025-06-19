package com.boycottpro.usercauses;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.boycottpro.models.UserCauses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetUserCausesHandlerTest {

    @Mock
    private DynamoDbClient dynamoDb;

    @Mock
    private Context context;

    @InjectMocks
    private GetUserCausesHandler handler;

    @Test
    void testValidUserAndCauseIdReturnsUserCause() throws Exception {
        String userId = "user-123";
        String causeId = "cause-456";
        String causeDesc = "Animal cruelty";
        String timestamp = "2025-06-18T12:00:00Z";

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("user_id", AttributeValue.fromS(userId));
        item.put("cause_id", AttributeValue.fromS(causeId));
        item.put("cause_desc", AttributeValue.fromS(causeDesc));
        item.put("timestamp", AttributeValue.fromS(timestamp));

        QueryResponse queryResponse = QueryResponse.builder().items(List.of(item)).build();
        when(dynamoDb.query(any(QueryRequest.class))).thenReturn(queryResponse);

        Map<String, String> pathParams = Map.of("user_id", userId, "cause_id", causeId);
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent().withPathParameters(pathParams);

        var response = handler.handleRequest(requestEvent, context);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains(userId));
        assertTrue(response.getBody().contains(causeDesc));
        assertTrue(response.getBody().contains(timestamp));
    }

    @Test
    void testMissingUserIdReturns400() {
        Map<String, String> pathParams = Map.of("cause_id", "cause-123");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withPathParameters(pathParams);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing user_id"));
    }

    @Test
    void testMissingCauseIdReturns400() {
        Map<String, String> pathParams = Map.of("user_id", "user-123");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withPathParameters(pathParams);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing cause_id"));
    }

    @Test
    void testQueryReturnsEmptyList() {
        String userId = "user-123";
        String causeId = "cause-999";

        QueryResponse queryResponse = QueryResponse.builder().items(Collections.emptyList()).build();
        when(dynamoDb.query(any(QueryRequest.class))).thenReturn(queryResponse);

        Map<String, String> pathParams = Map.of("user_id", userId, "cause_id", causeId);
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withPathParameters(pathParams);

        var response = handler.handleRequest(event, context);

        assertEquals(200, response.getStatusCode());
    }
}
