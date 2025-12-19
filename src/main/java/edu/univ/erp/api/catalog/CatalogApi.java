package edu.univ.erp.api.catalog;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.types.CourseRow;
import edu.univ.erp.data.SettingsRepository;
import edu.univ.erp.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class CatalogApi {

    private static final Logger log = LoggerFactory.getLogger(CatalogApi.class);
    private final CatalogService catalogService = new CatalogService();
    private final SettingsRepository settingsRepo = new SettingsRepository();

    public ApiResponse<List<CourseRow>> getCurrentCatalog() {
        try {
            String currentSemester = settingsRepo.getCurrentSemester();
            int currentYear = settingsRepo.getCurrentYear();
            log.debug("API: Fetching catalog for current term: {}-{}", currentSemester, currentYear);

            List<CourseRow> catalog = catalogService.getCatalog(currentSemester, currentYear);
            return ApiResponse.success(catalog, "Catalog loaded successfully for " + currentSemester + " " + currentYear + ".");
        } catch (Exception e) {
            log.error("API Error: Failed to fetch catalog", e);
            return ApiResponse.error("A critical error occurred while loading the catalog.");
        }
    }
}