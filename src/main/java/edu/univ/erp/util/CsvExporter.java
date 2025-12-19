package edu.univ.erp.util;

import com.opencsv.CSVWriter;
import edu.univ.erp.api.types.TranscriptEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);

    public boolean exportTranscript(List<TranscriptEntry> transcriptData, File outputFile) {
        log.info("Exporting transcript with {} entries to file: {}", transcriptData.size(), outputFile.getAbsolutePath());

        // Define CSV header
        String[] header = {"Course Code", "Course Title", "Credits", "Term", "Grade"};

        // Use try-with-resources for automatic closing of the writer
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(header);

            // Write data rows
            for (TranscriptEntry entry : transcriptData) {
                String[] data = {
                        entry.courseCode(), entry.courseTitle(), String.valueOf(entry.credits()), entry.semester(), entry.finalGrade()};writer.writeNext(data);
            }

            log.info("Transcript CSV export successful.");
            return true;

        } catch (IOException e) {
            log.error("Error writing transcript CSV file: {}", outputFile.getAbsolutePath(), e);
            return false;
        }
    }
}