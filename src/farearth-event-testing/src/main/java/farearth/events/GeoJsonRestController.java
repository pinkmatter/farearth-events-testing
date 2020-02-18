/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author eduan
 */
@RestController
public class GeoJsonRestController {

    private static final Logger LOG = LoggerFactory.getLogger(GeoJsonRestController.class);

    private final ObjectMapper mapper;
    private final AtomicInteger counter;
    private final File outputDirectory;
    private final DateTimeFormatter dateTimeformatter;
    private final DataDownloader dataDownloader;

    public GeoJsonRestController(
            @Value("${geojson-output-directory}") File outputDirectory,
            DataDownloader downloader
    ) {
        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException("GeoJSON output directory does not exist.");
        }

        this.counter = new AtomicInteger(0);
        this.mapper = new ObjectMapper();
        this.outputDirectory = outputDirectory;
        this.dataDownloader = downloader;
        this.dateTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss.SSS'Z'");
        LOG.info("GeoJSON endpoint initialized.");
    }

    @RequestMapping(value = "/geoJsonEndPoint", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void geoJsonEndpoint(@RequestBody Map<String, Object> postData) throws Exception {
        try {
            LOG.info("New incoming GeoJSON data...");

            String timestamp = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(dateTimeformatter);
            String filename = String.format("%s-%s", timestamp, String.valueOf(counter.incrementAndGet()));
            File geoJsonDirectory = new File(outputDirectory, filename);
            if (!geoJsonDirectory.mkdirs()) {
                throw new RuntimeException(String.format("Failed to create output directory at '%s'", geoJsonDirectory.getAbsolutePath()));
            }

            writeJsonFile(geoJsonDirectory, postData);
            downloadData(geoJsonDirectory, postData);

            LOG.info("Written new event to {}", geoJsonDirectory.getAbsolutePath());
        } catch (Throwable t) {
            LOG.error("Failed to write events to disk", t);
        }
    }

    private void writeJsonFile(File geoJsonDirectory, Map<String, Object> postData) throws Exception {
        File geoJsonFile = new File(geoJsonDirectory, "data.json");
        String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(postData);
        Files.write(geoJsonFile.toPath(), jsonData.getBytes(StandardCharsets.UTF_8));
    }

    private void downloadData(File geoJsonDirectory, Map<String, Object> postData) {
        List<Map<String, Object>> features = (List<Map<String, Object>>) postData.get("features");
        for (Map<String, Object> feature : features) {
            String type = (String) feature.get("type");
            if ("feature".equalsIgnoreCase(type)) {
                Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                String eventID = (String) feature.get("id");

                File eventDataDirectory = new File(geoJsonDirectory, eventID);
                if (!eventDataDirectory.mkdirs()) {
                    throw new RuntimeException(String.format("Failed to create directory at %s", eventDataDirectory.getAbsolutePath()));
                }

                List<String> dataIDs = (List<String>) properties.get("data");
                for (String id : dataIDs) {
                    dataDownloader.queueDownload(eventDataDirectory, id);
                }
            }
        }
    }

}
