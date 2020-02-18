/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.events;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author eduan
 */
@Component
public class DataDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(DataDownloader.class);

    private final AtomicInteger counter;
    private final ExecutorService executor;
    private final FarEarthCatalogueClient client;

    private final String catalogueUser;
    private final String cataloguePassword;

    public DataDownloader(
            @Value("${catalogue-url}") String catalogueUrl,
            @Value("${catalogue-username}") String catalogueUser,
            @Value("${catalogue-password}") String cataloguePassword
    ) throws Exception {
        this.executor = Executors.newFixedThreadPool(4);
        this.client = new FarEarthCatalogueClient(catalogueUrl);
        this.counter = new AtomicInteger(0);
        this.catalogueUser = catalogueUser;
        this.cataloguePassword = cataloguePassword;
        tryConnect();
    }

    public void queueDownload(File targetDirectory, String dataID) {
        executor.submit(() -> {
            try {
                if (tryConnect()) {
                    String defaultFilename = String.format("download-%d", counter.incrementAndGet());

                    client.getData(dataID, response -> {
                        String resolvedFilename = FarEarthCatalogueClient.getFilename(response, defaultFilename);
                        File target = new File(targetDirectory, resolvedFilename);

                        try (InputStream is = response.getEntity().getContent()) {
                            FileUtils.copyToFile(is, target);
                            LOG.info("Downloaded product to {}", target.getAbsolutePath());
                        } catch (Throwable t) {
                            LOG.error("Error writing data to " + target.getAbsolutePath(), t);
                        }
                    });
                }
            } catch (Throwable t) {
                LOG.error("Error establishing http get connection for " + dataID, t);
            }
        });
    }

    private boolean tryConnect() throws Exception {
        try {
            client.connect(catalogueUser, cataloguePassword);
            return true;
        } catch (Throwable t) {
            LOG.error("Failed to connect against server", t);
            return false;
        }
    }
}
