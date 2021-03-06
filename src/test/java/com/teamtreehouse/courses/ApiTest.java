package com.teamtreehouse.courses;

import com.google.gson.Gson;
import com.teamtreehouse.courses.dao.Sql2oCourseDao;
import com.teamtreehouse.courses.dao.Sql2oReviewDao;
import com.teamtreehouse.courses.model.Course;
import com.teamtreehouse.courses.model.Review;
import com.teamtreehouse.testing.ApiClient;
import com.teamtreehouse.testing.ApiResponse;
import org.junit.*;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import spark.Spark;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ApiTest {

    public static final String PORT = "4567";
    public static final String TEST_DATA_SOURCE = "jdbc:h2:mem:testing";
    private Connection conn;
    private ApiClient client;
    private Gson gson;
    private Sql2oCourseDao courseDao;
    private Sql2oReviewDao reviewDao;

    /*
     * The first thing we need to do before testing any other function is to fired up our server
     * we do this by calling our main method in the Api Class.
     * We are going to keep our server running throgh all of these tests
     *
     * So we are going to use an annotation called @BeforeClass which will runs before any test run and only once
     * after it's all done
     * */
    @BeforeClass
    public static void startServer(){
        /*
        * This static method on BeforeClass will just call the main method in Api Class, then for the String Array args
        * that we must pass to the main method here some considerations:
        *
        * 1. we want the port to be set to the 4567 and it is need to be set here since if you set it later after the
        * server already runs it will invoke error.
        * 2. We want to set the database to be not the one we have been created in the file database. We want it in the
        * memory so that it will be fast but volatile
        * */
        String[] args = {PORT, // the port setting
                TEST_DATA_SOURCE // the database in memory setting
        };
        Api.main(args);
    }

    /*
    * Next is the same as @BeforeClass we need to Stop the server once and for all in the end of all tests. Thus as
    * you might guess it needs another annotation called @AfterClass
    * */
    @AfterClass
    public static void stopServer(){
        /*
        * to stop server Spark has a static method called stop
        * */
        Spark.stop();
    }

    /*
    * As before each test we need to make a connection open and stay open for the EACH whole test to perform
    *
    * The difference here and the @BeforeClass is that this is performed before each test. The @BeforeClass is performed
    * once before the whole tests
    *
    * After we make connection to open we need to initialize a client that will do a request. We already copy paste the
    * ApiClient.java class from the teacher's note. If you need to review the code playback the video around 10:35
    *
    * We can the initialize a client using this ApiClientclass
    *
    * We also need to initialize Gson so that we can parse JSON.
    *
    * We also need to initialized coiurseDao to test if we can find course by Id
    * */
    @Before
    public void setUp() throws Exception {
        Sql2o sql2o = new Sql2o(TEST_DATA_SOURCE + ";INIT=RUNSCRIPT from 'classpath:db/init.sql'",
                "", "");
        courseDao = new Sql2oCourseDao(sql2o);
        reviewDao = new Sql2oReviewDao(sql2o); //<- adding reviewDao initialization to test review app controller
        conn = sql2o.open();
        client = new ApiClient("http://localhost:" + PORT);
        gson = new Gson();
    }

    /*
    * After each test we need to close the connections
    * */
    @After
    public void tearDown() throws Exception {
        conn.close();
    }

    /*
    * Now we start testing:
    * first we want to make sure adding Courses Returns Created Status
    * before this session we know by using Postman this works
    * */
    @Test
    public void addingCoursesReturnsCreatedStatus() throws Exception {
        /*
        * first we need to build our data into JSON which is a bit troublesome here. The best way is to map the
        * variable to value and then for each pair of variable to value data we parse into JSON using gson
        *
        * Now the first part the arrage
        * */
        Map<String, String> values = new HashMap<>();
        values.put("name", "Test"); // -> name of the course
        values.put("url", "http://test.com"); //-> the url for the course

        /*
        * Action : we parse the data into JSON and seek response using client rewiest and catch it as the new
        * ApiResponse object
        *
        * Here the request is POST meaning it has to declare the request body which is the JSON parsed data using gson
        * */
        ApiResponse res = client.request("POST", "/courses", gson.toJson(values));

        /*
        * Assert if the response status is 201 using ApiResponse getter for status
        * */
        assertEquals(201, res.getStatus());
    }

    /*
    * Now we want to test of the courses can be accessed by id
    * We already make a new courses to test the dao earlier we can just use that by copy paste it here
    * */
    private Course newTestCourse() {
        return new Course("Test", "http://what.com");
    }

    /*
    * Now we test if the course can be accessed by ID
    *
    * But first we need to make sure we make course dao initialized in the @Before
    * */

    @Test
    public void courseCanBeAccessedById() throws Exception {
        /*
        * Arrange: make a newTestCourse and add it into dao
        * */
        Course course = newTestCourse();
        courseDao.add(course);

        /*
        * Act: We are going to make the client make a GET request to course/id and catch the response as ApiResponse
        * object
        *
        * Since this is a GET request we do not need to declare the request body into client.
        * */
        ApiResponse res = client.request("GET", "/courses/" + course.getId());
        /*
        * Since the response is in JSON we need to parse it using gson. What we parse is the response body which the
        * Course object is located and pased parsed from JSON result as Course.class
        * */
        Course retreived = gson.fromJson(res.getBody(), Course.class);

        /*
        * Assert: we will compare if the course we created earlier is the same as retrieved if res body
        * */
        assertEquals(course, retreived);
    }

    /*
    * Next we test is missing Courses returns Not Found Status
    * */

    @Test
    public void missingCoursesReturnsNotFoundStatus() throws Exception {
        /*
        * We do not need to arrange anythings since we just want to simulate not founding any course meaning we do not
        * need to create one in the beginning
        *
        * We just right into act searching for course let say number 42
        * */
        ApiResponse res = client.request("GET", "/courses/42");

        /*
        * Asserts: it needs to be 404 since the inteded course is not even exist
        * */
        assertEquals(404, res.getStatus());
    }

    /*
    * Now we test all aspect of reviews
    * Begin with creating new reviews returns the correct response status (which is 201)
    * */

    @Test
    public void addingReviewsReturnsCreatedStatus() throws Exception {
        /*
        * First we need to build a new course using newTestCourse method since the database used will be volatile
        * thus we need to add it into dao
        *
        * Only the reviews will be created using JSON
        * */
        Course course = newTestCourse();
        courseDao.add(course);
        Map<String, Object> values = new HashMap<>();
        values.put("rating", 5);
        values.put("comment", "This is good");

        /*
        * creating a JSON client Request
        * */
        ApiResponse res = client.request("POST", "/courses/" + course.getId() + "/reviews",
                gson.toJson(values));

        /*
        * Asserts: it response with 201
        * */
        assertEquals(201, res.getStatus());
    }

    @Test
    public void getFromNonReviewedCourseReturnsEmptyArray() throws Exception {
        Course course = newTestCourse();
        courseDao.add(course);

        ApiResponse res = client.request("GET", "/courses/" + course.getId() + "/reviews");

        Review[] retrieved = gson.fromJson(res.getBody(), Review[].class);

        assertEquals(0, retrieved.length);
    }
    /*
   * Now test all non existing courses error in 404
   * */

    @Test
    public void addReviewsToNonExistingCoursesReturnsNotFoundStatus() throws Exception {
        /*
        * We can just copy paste the first arrangement of this test from above except the Course initialization part
        * */
        Map<String, Object> values = new HashMap<>();
        values.put("rating", 5);
        values.put("comment", "This is good");

        /*
        * The act part using the ApiResponse also similar but we just give any number as courseId
        * */
        ApiResponse res = client.request("POST", "/courses/42/reviews",
                gson.toJson(values));

        /*
        * Asserts that it will produce status 404 (Not Found)
        * */
        assertEquals(404, res.getStatus());
    }

    @Test
    public void fetchReviewsForNonExistingCoursesWillReturnsNotFoundStatus() throws Exception {
        /*
        * Just straight to the act since we do not have to create anything
        * if the course does not exist it will just get 404 (intercepted) before even get to examine whether the reviews
        * is available or not.
        *
        * This to prevent the retuns of empty list of reviews that falsely suggest the Course exist but have no reviews
        * */
        ApiResponse res = client.request("GET", "/courses/24/reviews00");

        /*
        * Asserts: it will get response status 404 Not Found Status
        * */
        assertEquals(404, res.getStatus());
    }

    /*
    * Now testing the response body that returns list of reviews from the GET requests
    * To do this we need to make multiple reviews I guess thus it will be better to make a private method for it
    * However, I want the courseId as argument passsed so that I can simulate multiple Course but reviews all called
    * at the same time
    * */
    private Review newTestReview(int courseId) {
        return new Review(courseId, 5, "Just test Command");
    }

    /*
    * Now we start testing the client's GET requests
    * */

    @Test
    public void getFindAllReviewsReturnsAllReviewsRegardlessCourseId() throws Exception {
        /*
        * Creates new Course and then use it to make reviews
        * We will simulate 2 Courses with overall 3 reviews (2 for course1 and 1 for course2)
        * */
        Course course1 = newTestCourse();
        courseDao.add(course1);
        reviewDao.add(newTestReview(course1.getId()));
        reviewDao.add(newTestReview(course1.getId()));

        Course course2 = newTestCourse();
        courseDao.add(course2);
        reviewDao.add(newTestReview(course2.getId()));

        /*
        * Act: call the GET /reviews by client
        * */
        ApiResponse res = client.request("GET", "/reviews");

        /*
        * Since the body of the response will consist of Arrays of Reviews we can use Review[] as class
        * */
        Review[] retrieved = gson.fromJson(res.getBody(), Review[].class);

        /*
        * Asserts: the length of retrieved array of Reviews should consist of 3 in size (or length since this is array
        * not a list)
        * */
        assertEquals(3, retrieved.length);
    }

    @Test
    public void getReviewsOfSpecificCourseReturnsOnlyTheCourseReviews() throws Exception {
        /*
        * We can just copy paste arrangement from previous test for findAll() reviews since the scenario is the same
        * But here we just want to call reviews from course1 not from course2
        * */
        Course course1 = newTestCourse();
        courseDao.add(course1);
        reviewDao.add(newTestReview(course1.getId()));
        reviewDao.add(newTestReview(course1.getId()));

        Course course2 = newTestCourse();
        courseDao.add(course2);
        reviewDao.add(newTestReview(course2.getId()));

        /*
         * Act: call the GET /reviews by client
         * */
        ApiResponse res = client.request("GET",
                String.format("/courses/%d/reviews", course1.getId()));

        /*
         * Since the body of the response will consist of Arrays of Reviews we can use Review[] as class
         * */
        Review[] retrieved = gson.fromJson(res.getBody(), Review[].class);

        /*
        * Asserts: the length of the array should be 2 since it must only consist reviews for course1 which is 2
        * reviews
        * */
        assertEquals(2, retrieved.length);
    }
}