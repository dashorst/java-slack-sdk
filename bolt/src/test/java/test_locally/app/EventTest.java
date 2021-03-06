package test_locally.app;

import com.google.gson.Gson;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.app_backend.SlackSignature;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.app_backend.events.payload.MessageBotPayload;
import com.slack.api.app_backend.events.payload.MessagePayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.request.RequestHeaders;
import com.slack.api.bolt.request.builtin.EventRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageBotEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.util.json.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.AuthTestMockServer;
import util.MockSlackApiServer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@Slf4j
public class EventTest {

    MockSlackApiServer server = new MockSlackApiServer();
    SlackConfig config = new SlackConfig();
    Slack slack = Slack.getInstance(config);

    @Before
    public void setup() throws Exception {
        server.start();
        config.setMethodsEndpointUrlPrefix(server.getMethodsEndpointPrefix());
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    final Gson gson = GsonFactory.createSnakeCase();
    final String secret = "foo-bar-baz";
    final SlackSignature.Generator generator = new SlackSignature.Generator(secret);

    String appMentionPayload = "{\"token\":\"legacy-fixed-value\",\"team_id\":\"T123\",\"api_app_id\":\"A123\",\"event\":{\"client_msg_id\":\"3fd13273-5a6a-4b5c-bd6f-109fd697038c\",\"type\":\"app_mention\",\"text\":\"<@U123> test\",\"user\":\"U234\",\"ts\":\"1583636399.000700\",\"team\":\"T123\",\"blocks\":[{\"type\":\"rich_text\",\"block_id\":\"FMAzp\",\"elements\":[{\"type\":\"rich_text_section\",\"elements\":[{\"type\":\"user\",\"user_id\":\"U123\"},{\"type\":\"text\",\"text\":\" test\"}]}]}],\"channel\":\"C123\",\"event_ts\":\"1583636399.000700\"},\"type\":\"event_callback\",\"event_id\":\"EvV1KV8BM3\",\"event_time\":1583636399,\"authed_users\":[\"U123\"]}";

    @Test
    public void appMention() throws Exception {
        App app = buildApp();
        AtomicBoolean userMessageReceived = new AtomicBoolean(false);
        app.event(AppMentionEvent.class, (req, ctx) -> {
            userMessageReceived.set(req.getEvent().getUser().equals("U234"));
            return ctx.ack();
        });

        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(appMentionPayload, rawHeaders, timestamp);

        EventRequest req = new EventRequest(appMentionPayload, new RequestHeaders(rawHeaders));
        Response response = app.run(req);
        assertEquals(200L, response.getStatusCode().longValue());

        assertTrue(userMessageReceived.get());
    }

    String messagePayload = "{\"token\":\"legacy-fixed-value\",\"team_id\":\"T123\",\"api_app_id\":\"A123\",\"event\":{\"client_msg_id\":\"3fd13273-5a6a-4b5c-bd6f-109fd697038c\",\"type\":\"message\",\"text\":\"<@U123> test\",\"user\":\"U234\",\"ts\":\"1583636399.000700\",\"team\":\"T123\",\"blocks\":[{\"type\":\"rich_text\",\"block_id\":\"FMAzp\",\"elements\":[{\"type\":\"rich_text_section\",\"elements\":[{\"type\":\"user\",\"user_id\":\"U123\"},{\"type\":\"text\",\"text\":\" test\"}]}]}],\"channel\":\"C123\",\"event_ts\":\"1583636399.000700\",\"channel_type\":\"channel\"},\"type\":\"event_callback\",\"event_id\":\"EvV1KA7U3A\",\"event_time\":1583636399,\"authed_users\":[\"U123\"]}";

    @Test
    public void message() throws Exception {
        App app = buildApp();
        AtomicBoolean userMessageReceived = new AtomicBoolean(false);
        app.event(MessageEvent.class, (req, ctx) -> {
            userMessageReceived.set(req.getEvent().getUser().equals("U234"));
            return ctx.ack();
        });
        AtomicBoolean botMessageReceived = new AtomicBoolean(false);
        app.event(MessageBotEvent.class, (req, ctx) -> {
            botMessageReceived.set(req.getEvent().getBotId().equals("B123"));
            return ctx.ack();
        });

        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(messagePayload, rawHeaders, timestamp);

        EventRequest req = new EventRequest(messagePayload, new RequestHeaders(rawHeaders));
        Response response = app.run(req);
        assertEquals(200L, response.getStatusCode().longValue());

        assertTrue(userMessageReceived.get());
        assertFalse(botMessageReceived.get());
    }

    @Test
    public void user() throws Exception {
        App app = buildApp();
        AtomicBoolean userMessageReceived = new AtomicBoolean(false);
        app.event(MessageEvent.class, (req, ctx) -> {
            userMessageReceived.set(req.getEvent().getUser().equals("U123"));
            return ctx.ack();
        });
        AtomicBoolean botMessageReceived = new AtomicBoolean(false);
        app.event(MessageBotEvent.class, (req, ctx) -> {
            botMessageReceived.set(req.getEvent().getBotId().equals("B123"));
            return ctx.ack();
        });

        EventsApiPayload<MessageEvent> payload = buildUserMessagePayload();

        String requestBody = gson.toJson(payload);
        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(requestBody, rawHeaders, timestamp);

        EventRequest req = new EventRequest(requestBody, new RequestHeaders(rawHeaders));
        Response response = app.run(req);
        assertEquals(200L, response.getStatusCode().longValue());
        assertTrue(userMessageReceived.get());
        assertFalse(botMessageReceived.get());
    }

    @Test
    public void bot() throws Exception {
        App app = buildApp();
        AtomicBoolean userMessageReceived = new AtomicBoolean(false);
        app.event(MessageEvent.class, (req, ctx) -> {
            userMessageReceived.set(req.getEvent().getUser().equals("U123"));
            return ctx.ack();
        });
        AtomicBoolean botMessageReceived = new AtomicBoolean(false);
        app.event(MessageBotEvent.class, (req, ctx) -> {
            botMessageReceived.set(req.getEvent().getBotId().equals("B123"));
            return ctx.ack();
        });

        EventsApiPayload<MessageBotEvent> payload = buildUserBotMessagePayload();

        String requestBody = gson.toJson(payload);
        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(requestBody, rawHeaders, timestamp);

        EventRequest req = new EventRequest(requestBody, new RequestHeaders(rawHeaders));
        Response response = app.run(req);
        assertEquals(200L, response.getStatusCode().longValue());

        assertFalse(userMessageReceived.get());
        assertTrue(botMessageReceived.get());
    }

    App buildApp() {
        return new App(AppConfig.builder()
                .signingSecret(secret)
                .singleTeamBotToken(AuthTestMockServer.ValidToken)
                .slack(slack)
                .build());
    }

    void setRequestHeaders(String requestBody, Map<String, List<String>> rawHeaders, String timestamp) {
        rawHeaders.put(SlackSignature.HeaderNames.X_SLACK_REQUEST_TIMESTAMP, Arrays.asList(timestamp));
        rawHeaders.put(SlackSignature.HeaderNames.X_SLACK_SIGNATURE, Arrays.asList(generator.generate(timestamp, requestBody)));
    }

    EventsApiPayload<MessageEvent> buildUserMessagePayload() {
        EventsApiPayload<MessageEvent> payload = new MessagePayload();
        MessageEvent event = new MessageEvent();
        event.setUser("U123");
        event.setText("This is a message sent by a user.");
        payload.setEvent(event);
        payload.setTeamId("T123");
        return payload;
    }

    EventsApiPayload<MessageBotEvent> buildUserBotMessagePayload() {
        EventsApiPayload<MessageBotEvent> payload = new MessageBotPayload();
        MessageBotEvent event = new MessageBotEvent();
        event.setBotId("B123");
        event.setText("This is a message sent by a bot user.");
        payload.setEvent(event);
        payload.setTeamId("T123");
        return payload;
    }

}
