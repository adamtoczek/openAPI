import com.sun.istack.NotNull;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HeaderConfig;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.collection.IsArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MigoAPITest {
//    private static String token;
    private static final String endpoint = "https://qa-interview-api.migo.money";
    private static final String user = "egg";
    private static final String pass= "f00BarbAz!";
    private List<String> deleteIDs = new ArrayList<>();

    @BeforeAll
    public static void getAuthToken() {
        RestAssured.baseURI = endpoint;
        String token = given().auth().preemptive().basic(user, pass).when().post("/token").then().statusCode(200).extract().path("key");
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .build().header("X-API-KEY", token);
        RestAssured.config().headerConfig(HeaderConfig.headerConfig().overwriteHeadersWithName("X-API-KEY"));
    }

    @Test
    public void verifyCreatePath() {
        String body = new JSONObject()
                .put("firstName","Adam ")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
            .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then().statusCode(200).log().all()
                .body("firstName", notNullValue())
                .body("lastName", notNullValue())
                .body("phone", notNullValue())
                .body("id", notNullValue())
                .extract().path("id");
        deleteIDs.add(id);
    }

    @Test
    public void verifyNewClientIsOnList () {
        String body = new JSONObject()
                .put("firstName","Adam ")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then()
                .extract().path("id");

        deleteIDs.add(id);

        given().when().get("/clients").then().body("clients.id", hasItem(id));
    }

    @Test
    public void verifyNewClientIsReadable () {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then()
                .extract().path("id");
        deleteIDs.add(id);

        given().pathParam("id", id).when().get("/client/{id}")
                .then().log().all()
                .statusCode(200)
                .body("firstName", equalTo("Adam"))
                .body("lastName", equalTo("Kowalski"))
                .body("phone", equalTo("123 456 789"))
                .body("id", notNullValue());

    }


    @Test
    public void verifyUpdatePath() {
        String body = new JSONObject()
                .put("firstName","Adam ")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String updated = new JSONObject()
                .put("firstName","Lew")
                .put("lastName", "Tolstoj")
                .put("phone", "987654")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then()
                .extract().path("id");
        deleteIDs.add(id);

        given().pathParam("id", id).contentType("application/json").body(updated).
                when().put("/client/{id}")
                .then().log().all()
                .statusCode(200)
                .body("firstName", equalTo("Lew"))
                .body("lastName", equalTo("Tolstoj"))
                .body("phone", equalTo("987654"))
                .body("id", equalTo(id));
    }

    @Test
    public void verifyUpdatedClientIsPersisted () {
        String body = new JSONObject()
                .put("firstName","Adam ")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String updated = new JSONObject()
                .put("firstName","Lew")
                .put("lastName", "Tolstoj")
                .put("phone", "987654")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then()
                .extract().path("id");
        deleteIDs.add(id);

        given().pathParam("id", id).contentType("application/json").body(updated)
                .when().put("/client/{id}")
                .then().statusCode(200);

        given().pathParam("id", id).when().get("/client/{id}")
                .then().log().all()
                .statusCode(200)
                .body("firstName", equalTo("Lew"))
                .body("lastName", equalTo("Tolstoj"))
                .body("phone", equalTo("987654"))
                .body("id", equalTo(id));
    }


    @Test
    public void verifyReadPath() {

        ValidatableResponse resp = given().when().log().all().get("/clients")
                .then().log().all()
                .statusCode(200)
                .body("clients", everyItem(hasKey("firstName")))
                .body("clients", everyItem(hasKey("lastName")))
                .body("clients", everyItem(hasKey("phone")))
                .body("clients", everyItem(hasKey("id")));
    }

    @Test
    public void verifyDeletePath() {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then()
                .extract().path("id");

        given().pathParam("id", id).when().delete("/client/{id}")
                .then()
                .statusCode(200)
                .body("message", equalTo("client deleted"));
    }

    @Test
    public void verifyCantReadDeleted () {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client").then().extract().path("id");

        given().pathParam("id", id).when().delete("/client/{id}").then().statusCode(200);

        given().pathParam("id", id).when().get("/client/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo("client not found"));
    }

    @Test
    public void verifyCantDeleteDeleted () {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client").then().extract().path("id");

        given().pathParam("id", id).when().delete("/client/{id}").then().statusCode(200);

        given().pathParam("id", id).when().delete("/client/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo("client not found"));
    }

    @Test
    public void verifyDeletedNotListed () {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123 456 789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client").then().extract().path("id");

        given().pathParam("id", id).when().delete("/client/{id}").then().statusCode(200);

        given().when().get("/clients")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(id)));
    }


    @Test
    public void verifyAuthRequiredOnRead () {
        given().header("X-API-KEY", "123")
                .when().get("/clients")
                .then()
                    .statusCode(403)
                    .body("message", equalTo("invalid or missing api key"));
    }
    @Test
    public void verifyAuthRequiredOnDelete () {
        given()
                .header("X-API-KEY", "123")
                .when().delete("/client/1234")
                .then()
                    .statusCode(403)
                    .body("message", equalTo("invalid or missing api key"));
    }
    @Test
    public void verifyAuthRequiredOnCreate () {
        given().header("X-API-KEY", "123")
                .when().post("/client")
                .then()
                    .statusCode(403)
                    .body("message", equalTo("invalid or missing api key"));
    }
    @Test
    public void verifyAuthRequiredOnUpdate () {
        given().header("X-API-KEY", "123")
                .when().put("/client/1234")
                .then()
                    .statusCode(403)
                    .body("message", equalTo("invalid or missing api key"));
    }

    /*
    Swagger says that response should be scheme array of ClientDetails
     */
    @Test
    public void verifyReadGivesArray() {
        given().when().get("/clients").then().body("", everyItem(hasKey("id")));
    }

    @Test
    public void verifyCreateInputMaxLength() {
        String body = new JSONObject()
                .put("firstName","01234567890123456789012345678901234567890123456789")
                .put("lastName", "01234567890123456789012345678901234567890123456789")
                .put("phone", "01234567890123456789012345678901234567890123456789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then().statusCode(200).log().all()
                .body("firstName", equalTo("01234567890123456789012345678901234567890123456789"))
                .body("lastName", equalTo("01234567890123456789012345678901234567890123456789"))
                .body("phone", equalTo("01234567890123456789012345678901234567890123456789"))
                .body("id", notNullValue())
                .extract().path("id");
        deleteIDs.add(id);
    }

    @Test
    public void verifyCreateFirstNameOverMaxLength () {
        String body = new JSONObject()
                .put("firstName","01234567890123456789012345678901234567890123456789a")
                .put("lastName", "01234567890123456789012345678901234567890123456789")
                .put("phone", "01234567890123456789012345678901234567890123456789")
                .toString();

        ValidatableResponse resp = given().contentType("application/json").body(body)
                .when().post("/client")
                .then();
        deleteIDs.add(resp.extract().path("id"));

        resp.statusCode(400);
    }

    @Test
    public void verifyCreateLastNameOverMaxLength () {
        String body = new JSONObject()
                .put("firstName","01234567890123456789012345678901234567890123456789")
                .put("lastName", "01234567890123456789012345678901234567890123456789a")
                .put("phone", "01234567890123456789012345678901234567890123456789")
                .toString();

        ValidatableResponse resp = given().contentType("application/json").body(body)
                .when().post("/client")
                .then();
        deleteIDs.add(resp.extract().path("id"));

        resp.statusCode(400);
    }

    @Test
    public void verifyCreatePhoneOverMaxLength () {
        String body = new JSONObject()
                .put("firstName","01234567890123456789012345678901234567890123456789")
                .put("lastName", "01234567890123456789012345678901234567890123456789")
                .put("phone", "01234567890123456789012345678901234567890123456789a")
                .toString();

        ValidatableResponse resp = given().contentType("application/json").body(body)
                .when().post("/client")
                .then();
        deleteIDs.add(resp.extract().path("id"));

        resp.statusCode(400);
    }

    @Test
    public void verifyUpdateMaxLength () {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123")
                .toString();

        String update = new JSONObject()
                .put("firstName","01234567890123456789012345678901234567890123456789")
                .put("lastName", "01234567890123456789012345678901234567890123456789")
                .put("phone", "01234567890123456789012345678901234567890123456789")
                .toString();

        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then().statusCode(200)
                .extract().path("id");
        deleteIDs.add(id);

        given().contentType("application/json").pathParam("id", id).body(update)
                .when().put("/client/{id}")
                .then().statusCode(200);
    }

    @Test
    public void verifyCantGetNotExisting () {
        given().pathParam("id", "a").when().get("/client/{id}")
                .then().statusCode(404)
                .body("message", equalTo("client not found"));
    }

    @Test
    public void verifyCantDeleteNotExisitng () {
        given().pathParam("id", "a").when().delete("/client/{id}")
                .then().statusCode(404)
                .body("message", equalTo("client not found"));
    }

    @Test
    public void verifyCantUpdateNotExisting () {
        String update = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123")
                .toString();

        given().pathParam("id", "a").body(update).contentType("application/json")
                .when().put("/client/{id}")
                .then().statusCode(404)
                .body("message", equalTo("client not found"));
    }

    @Test
    public void verifyFirstNameCreateRequired () {
        String body = new JSONObject()
                .put("lastName", "Kowalski")
                .put("phone", "123")
                .toString();

        given().body(body).contentType("application/json")
                .when().post("/client")
                .then().statusCode(400)
                .body("message", equalTo("firstName is required"));
    }

    @Test
    public void verifyLastNameCreateRequired () {
        String body = new JSONObject()
                .put("firstName", "Kowalski")
                .put("phone", "123")
                .toString();

        given().body(body).contentType("application/json")
                .when().post("/client")
                .then().statusCode(400)
                .body("message", equalTo("lastName is required"));
    }

    @Test
    public void verifyPhoneCreateRequired () {
        String body = new JSONObject()
                .put("lastName", "Kowalski")
                .put("firstName", "123")
                .toString();

        given().body(body).contentType("application/json")
                .when().post("/client")
                .then().statusCode(400)
                .body("message", equalTo("phone is required"));
    }

    @Test
    public void verifyFirstNameUpdateRequired() {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123")
                .toString();
        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then().statusCode(200)
                .extract().path("id");
        deleteIDs.add(id);

        String update = new JSONObject()
                .put("lastName", "Kowalski")
                .put("phone", "123")
                .toString();

        given().pathParam("id", id).body(update).contentType("application/json")
                .when().put("/client/{id}")
                .then().statusCode(400)
                .body("message", equalTo("firstName is required"));
    }

    @Test
    public void verifyLastNameUpdateRequired () {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123")
                .toString();
        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then().statusCode(200)
                .extract().path("id");
        deleteIDs.add(id);

        String update = new JSONObject()
                .put("firstName","Adam")
                .put("phone", "123")
                .toString();

        given().pathParam("id", id).body(update).contentType("application/json")
                .when().put("/client/{id}")
                .then().statusCode(400)
                .body("message", equalTo("lastName is required"));
    }

    @Test
    public void verifyPhoneUpdateRequired () {
        String body = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .put("phone", "123")
                .toString();
        String id = given().contentType("application/json").body(body)
                .when().post("/client")
                .then().statusCode(200)
                .extract().path("id");
        deleteIDs.add(id);

        String update = new JSONObject()
                .put("firstName","Adam")
                .put("lastName", "Kowalski")
                .toString();

        given().pathParam("id", id).body(update).contentType("application/json")
                .when().put("/client/{id}")
                .then().statusCode(400)
                .body("message", equalTo("phone is required"));
    }


    @AfterAll
    public void cleanUp() {
        for (String id : deleteIDs) {
            given().pathParam("id", id).when().delete("/client/{id}").then().statusCode(200);
            System.out.println(id + " deleted");
        }
    }


}
