# FarEarth Real-time events reference implementation

* Serves to show how to integrate against the FarEarth real-time events sub-system.
* A functional system requires both the reference service, as well as an active FarEarth catalogue.
* The FarEarth catalogue sends real-time events to the real-time reference implementation and saves the received event data to disk.

## Building from sources

* Requires Apache Maven and at least Java 8.

```
git clone https://github.com/pinkmatter/farearth-events-testing.git
cd src/farearth-events-testing
mvn install
```

## Configuration

* The `application.yml` configuration file exposes the following properties:

 * `server.port`: The port where the local reference implementation will listen for HTTP connections.
 * `kmz-output-directory`:The local path where received GeoJSON files will be saved.
 * `geojson-output-directory`: The local path where received GeoJSON files will be saved.
 * `catalogue-url`: The URL of the FarEarth catalogue.
 * `catalogue-username`: The credentials required to access the FarEarth catalogue.
 * `catalogue-password`: The credentials required to access the FarEarth catalogue.

## Directory locations

* The ouput directories defaults to the current directory (`logs`, `output-kmz` and `output-geojson`).
* Example output data are also included in the `example-data` folder that shows how the output for the reference implementation service will typically look.

## Execution

* Example start-up scripts are included for Windows (`run.bat`) and Linux (`run.sh`).

## Local HTTP end-points and event filtering

* The default end-points where the FarEarth catalogue will target are `/geoJsonEndPoint` and `/kmzEndPoint`.
* However, this can be changed and needs to be supplied to Pinkmatter.
* The `/geoJsonEndPoint` accepts `application/json` formatted HTTP POST requests, while the `/kmzEndPoint` accepts content `multipart/form-data` as a full KMZ file.
* Events can also be filtered via geo-fencing and different areas can be configured to target different end-points accordingly (configured on the FarEarth catalogue).

## Service authentication

* To communicate with the FarEarth catalogue, the following authentication procedure needs to be followed:

 * Build an HTTP POST object with content type `application/x-www-form-urlencoded` and body key value pairs `username` and `password`.
 * Post the object against the catalogue end-point at `/catalogue/login`.
 * A successful log-in will yield a `Set-Cookie` header response with a cookie named `JSESSIONID`.
 * Attach the `JSESSIONID` cookie to any requests made to the FarEarth Catalogue.




