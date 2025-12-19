package edu.univ.erp.api.reports;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.types.TranscriptEntry;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.util.CsvExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

// API layer specifically for generating reports.
public class ReportsApi {

    private static final Logger log = LoggerFactory.getLogger(ReportsApi.class);
    private final StudentService studentService = new StudentService();
    private final CsvExporter csvExporter = new CsvExporter();

    // API to generate and provide a student transcript file for download.
    public ApiResponse<Void> downloadStudentTranscript(int studentId, File targetFile) {
        try {
            // 1. Get the data from the service layer
            List<TranscriptEntry> transcriptData = studentService.generateTranscriptData(studentId);

            if (transcriptData.isEmpty()) {
                log.warn("API: No transcript data found for student {}", studentId);
                return ApiResponse.success(null, "No completed courses with final grades found to generate transcript.");
            }

            // 2. Use the exporter utility to write the file
            boolean success = csvExporter.exportTranscript(transcriptData, targetFile);

            if (success) {
                log.info("API: Transcript successfully generated for student {} at {}", studentId, targetFile.getAbsolutePath());
                // Provide a clear message for the user, including the file path
                return ApiResponse.success(null, "Transcript saved successfully!\nLocation: " + targetFile.getAbsolutePath());
            } else {
                log.error("API: Transcript file generation failed for student {}", studentId);
                return ApiResponse.error("Failed to write transcript file. Check application logs.");
            }
        } catch (Exception e) {
            log.error("API: Unexpected error generating transcript for student {}", studentId, e);
            return ApiResponse.error("An unexpected error occurred while generating the transcript.");
        }
    }
}