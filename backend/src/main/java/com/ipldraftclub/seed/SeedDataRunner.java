package com.ipldraftclub.seed;

import com.ipldraftclub.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Startup hook that loads roster data through the validated importer. Safe to re-run.
 * <ul>
 *   <li>app.seed.enabled=true  -> loads the bundled DEVELOPMENT SAMPLE dataset</li>
 *   <li>app.seed.csv-path=...  -> additionally imports your own CSV (see docs/ADDING_SEASONS in the README)</li>
 * </ul>
 */
@Component
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    static final String SAMPLE_RESOURCE = "seed/sample_player_seasons.csv";

    private final AppProperties props;
    private final RosterCsvImporter importer;

    public SeedDataRunner(AppProperties props, RosterCsvImporter importer) {
        this.props = props;
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.seed().enabled()) {
            try (Reader reader = new InputStreamReader(new ClassPathResource(SAMPLE_RESOURCE).getInputStream(),
                    StandardCharsets.UTF_8)) {
                log.warn("Loading the bundled DEVELOPMENT SAMPLE roster (incomplete, unofficial, illustrative ratings).");
                report(importer.importCsv(reader, "classpath:" + SAMPLE_RESOURCE, true));
            } catch (Exception e) {
                log.error("Sample roster import failed: {}", e.getMessage(), e);
            }
        }
        if (StringUtils.hasText(props.seed().csvPath())) {
            Path path = Path.of(props.seed().csvPath());
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                report(importer.importCsv(reader, path.toString(), true));
            } catch (Exception e) {
                log.error("Roster import from {} failed: {}", path, e.getMessage(), e);
            }
        }
    }

    private void report(ImportSummary summary) {
        log.info(summary.describe());
        summary.messages().forEach(m -> log.warn("  {}", m));
    }
}
