/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.events;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author eduan
 */
@RestController
public class KmzRestController implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(KmzRestController.class);

    @Value("${kmz-output-directory}")
    private File outputDirectory;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException("KMZ output directory does not exist.");
        }

        LOG.info("KMZ endpoint initialized.");
    }

    @RequestMapping(value = "/kmzEndPoint", method = RequestMethod.POST)
    public void uploadFileHandler(
            @RequestParam(value = "filename", defaultValue = "Random.kmz") String filename,
            @RequestParam("file") MultipartFile file
    ) {
        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();

                File serverFile = getKmzFile(outputDirectory, filename);
                FileUtils.writeByteArrayToFile(serverFile, bytes);

                LOG.info("KMZ downloaded to {} with size {} bytes.", serverFile.getAbsolutePath(), serverFile.length());
            } catch (Exception e) {
                LOG.error("Download failed", e);
            }
        } else {
            LOG.error("Download failed: file is empty");
        }
    }

    private static File getKmzFile(File directory, String specified) {
        String fileNameWithOutExt = FilenameUtils.removeExtension(specified);
        String extension = FilenameUtils.getExtension(specified);
        String nonce = String.valueOf(System.currentTimeMillis());
        String resolved = String.format("%s-%s.%s", fileNameWithOutExt, nonce, extension);
        return new File(directory, resolved);
    }
}
