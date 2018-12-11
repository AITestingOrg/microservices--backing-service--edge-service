//package aist.edge.edgeservice;
//
//import static org.assertj.core.api.Java6Assertions.assertThat;
//
//import com.palantir.docker.compose.DockerComposeRule;
//import com.palantir.docker.compose.configuration.ShutdownStrategy;
//import com.palantir.docker.compose.connection.Container;
//import com.palantir.docker.compose.connection.DockerPort;
//import com.palantir.docker.compose.connection.waiting.HealthChecks;
//
//import java.util.*;
//
//import org.joda.time.Duration;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.ClassRule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.http.*;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.util.*;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest(classes = EdgeServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//public class EdgeServiceIntegrationTestsTripCmd {
//    protected static final Logger LOG = LoggerFactory.getLogger(EdgeServiceIntegrationTestsTripCmd.class);
//
//    private static String discoveryServiceURL;
//    private static String mongoURL;
//    private static String userServiceURL;
//    private static String tripCommandURL;
//    private static Duration DEFAULT_TIMEOUT = Duration.standardMinutes(2);
//
//    // Wait for all services to have ports open
//    @ClassRule
//    public static DockerComposeRule docker = DockerComposeRule.builder().pullOnStartup(true)
//            .file("src/test/resources/docker-compose-trip-cmd.yml").shutdownStrategy(ShutdownStrategy.KILL_DOWN)
//            .waitingForService("discoveryservice", HealthChecks.toHaveAllPortsOpen(), DEFAULT_TIMEOUT)
//            .waitingForService("mysqlserver", HealthChecks.toHaveAllPortsOpen(), DEFAULT_TIMEOUT)
//            .waitingForService("mongo", HealthChecks.toHaveAllPortsOpen(), DEFAULT_TIMEOUT)
//            .waitingForService("rabbitmq", HealthChecks.toHaveAllPortsOpen(), DEFAULT_TIMEOUT)
//            .waitingForService("userservice", HealthChecks.toHaveAllPortsOpen(), DEFAULT_TIMEOUT)
//            .waitingForService("tripmanagementcmd", HealthChecks.toHaveAllPortsOpen(), DEFAULT_TIMEOUT)
//            .build();
//
//    // Get IP addresses and ports to run tests on
//    @BeforeClass
//    public static void initialize() throws Exception {
//
//        LOG.info("Initializing ports from Docker");
//
//        Container discoveryContainer = docker.containers().container("discoveryservice");
//        DockerPort discoveryPort = discoveryContainer.port(8761);
//        discoveryServiceURL = String.format("http://%s:%s", discoveryPort.getIp(),
//        	discoveryPort.getExternalPort());
//        if(!discoveryPort.isListeningNow()){
//            LOG.info("Discovery service didn't respond over HTTP");
//            throw new Exception(String.format("Discovery didn't respond, port: %s", discoveryPort.getInternalPort()));
//        }
//        LOG.info("Discovery service responded over HTTP");
//        
//        Container mongoContainer = docker.containers().container("mongo");
//        DockerPort mongoPort = mongoContainer.port(27017);
//        mongoURL = String.format("http://%s:%s", mongoPort.getIp(), mongoPort.getExternalPort());
//        if(!mongoPort.isListeningNow()){
//            LOG.info("Mongo service didn't respond over HTTP");
//            throw new Exception(String.format("Mongo didn't respond, port: %s", mongoPort.getInternalPort()));
//        }
//        LOG.info("Mongo service responded over HTTP");
//        
//        Container userContainer = docker.containers().container("userservice");
//        DockerPort userPort = userContainer.port(8080);
//        userServiceURL = String.format("http://%s:%s", userPort.getIp(), userPort.getExternalPort());
//        if(!userPort.isListeningNow()){
//            LOG.info("User service didn't respond over HTTP");
//            throw new Exception(String.format("User didn't respond, port: %s", userPort.getInternalPort()));
//        }
//        LOG.info("User service responded over HTTP");
//        
//        Container tripManagementCmdContainer = docker.containers().container("tripmanagementcmd");
//        DockerPort tripManagementCmdPort = tripManagementCmdContainer.port(8080);
//        tripCommandURL = String.format("http://%s:%s", tripManagementCmdPort.getIp(),
//        	tripManagementCmdPort.getExternalPort());
//        if(!userPort.isListeningNow()){
//            LOG.info("TripManagementCmd service didn't respond over HTTP");
//            throw new Exception(String.format("TripManagementCmd didn't respond, port: %s", tripManagementCmdPort.getInternalPort()));
//        }
//        LOG.info("TripManagementCmd service responded over HTTP");
//        
//        LOG.info("Containers initialized correctly");
//    }
//
//    private TestRestTemplate restTemplate = new TestRestTemplate();
//
//    private String token;
//
//    @Before
//    public void setUp() throws JSONException {
//        String plainCreds = "front-end:front-end";
//        byte[] plainCredsBytes = plainCreds.getBytes();
//        byte[] base64CredsBytes = Base64.getEncoder().encode(plainCredsBytes);
//        String base64Creds = new String(base64CredsBytes);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Basic " + base64Creds);
//        headers.add("Content-Type", "application/x-www-form-urlencoded");
//
//        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
//        parameters.add("username", "front-end");
//        parameters.add("secret", "front-end");
//        String body = "grant_type=password&scope=webclient&username=passenger&password=password";
//        HttpEntity<String> request = new HttpEntity<>(body, headers);
//
//        // when:
//        ResponseEntity<String> response = restTemplate.postForEntity(userServiceURL + "/auth/oauth/token", request,
//                String.class, parameters);
//
//        // then:
//        assertThat(response.getStatusCodeValue()).isEqualTo(200);
//        JSONObject json = new JSONObject(response.getBody());
//        token = json.getString("access_token");
//    }
//
//    @Test
//    public void tripCommandPOSTRequestSuccess() {
//        // given:
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Bearer " + token);
//        headers.add("Content-Type", "application/json");
//
//        String body = "{ \"originAddress\": \"Weston, FL\", \"destinationAddress\": "
//                + "\"Miami, FL\", \"userId\": \"4eaf29bc-3909-49d4-a104-3d17f68ba672\" }";
//        HttpEntity<String> request = new HttpEntity<>(body, headers);
//
//        // when:
//        ResponseEntity<String> response = restTemplate.postForEntity(tripCommandURL + "/api/v1/trip", request,
//                String.class);
//
//        // then:
//        assertThat(response.getStatusCodeValue()).isEqualTo(201);
//    }
//
//}