honors-housing-endpoints
================================

This application implements a backend for the Honors Housing selection using
Google Cloud Endpoints, App Engine, and Java.

## Products
- [App Engine][1]

## Language
- [Java][2]

## APIs
- [Google Cloud Endpoints][3]
- [Google App Engine Maven plugin][6]

## Setup Instructions
1. Update the application version in `appengine-web.xml` if you want to test
   without disrupting the default running instance
1. Update the values in `src/main/java/edu/rit/honors/housing/Constants.java` to
   reflect the respective client IDs you have registered in the
   [APIs Console][4].
1. `mvn clean install`
1. Run the application with `mvn appengine:devserver`, and ensure it's running 
   by visiting your local server's  address (by default [localhost:8080][5].)
1. Get the client library with `mvn appengine:endpoints_get_client_lib`
1. Deploy your application.

If you add any JDO persistence classes, you also need to run `mvn appengine:enhance`


[1]: https://developers.google.com/appengine
[2]: http://java.com/en/
[3]: https://developers.google.com/appengine/docs/java/endpoints/
[4]: https://code.google.com/apis/console
[5]: https://localhost:8080/
[6]: https://developers.google.com/appengine/docs/java/tools/maven
