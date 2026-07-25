package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class BisGlossaryDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(BisGlossaryDataLoader.class);
    private final GlossaryService glossaryService;

    public BisGlossaryDataLoader(GlossaryService glossaryService) {
        this.glossaryService = glossaryService;
    }

    @PostConstruct
    public void loadInitialData() {
        try {
            if (glossaryService.getAllEntries().isEmpty()) {
                List<GlossaryEntry> initialEntries = Arrays.asList(
                    createEntry("ECCN", "Export Control Classification Number",
                        "A five-character alphanumeric code used in the Commerce Control List (CCL) to identify items for export control purposes.",
                        "Export Controls"),
                    createEntry("CCL", "Commerce Control List",
                        "A list of items under export control maintained by the Bureau of Industry and Security (BIS).",
                        "Export Controls"),
                    createEntry("EAR", "Export Administration Regulations",
                        "Regulations that implement the Export Administration Act and govern export and reexport of commercial items.",
                        "Export Controls"),
                    createEntry("BIS", "Bureau of Industry and Security",
                        "The U.S. government agency responsible for implementing and enforcing export controls.",
                        "Export Controls"),
                    createEntry("STA", "Strategic Trade Authorization",
                        "A license exception that allows exports of certain items to certain destinations without a license.",
                        "Export Controls"),
                    createEntry("ENC", "Encryption Commodities, Software, and Technology",
                        "Items that use encryption for information security and are subject to export controls.",
                        "Cryptography"),
                    createEntry("CCATS", "Commodity Classification Automated Tracking System",
                        "A system used to request commodity classifications from BIS.",
                        "Export Controls"),
                    createEntry("NLR", "No License Required",
                        "A designation indicating that no export license is required for a particular item.",
                        "Export Controls"),
                    createEntry("TMP", "Temporary Imports, Exports, and Reexports",
                        "A license exception for temporary exports and reexports.",
                        "Export Controls"),
                    createEntry("RPL", "Servicing and Replacement of Parts and Equipment",
                        "A license exception for replacement parts and servicing of previously exported items.",
                        "Export Controls")
                );

                glossaryService.importBulkEntries(initialEntries);
            }
        } catch (Exception e) {
            // Log error but don't prevent application startup
            logger.error("Failed to load initial glossary data", e);
        }
    }

    GlossaryEntry createEntry(String term, String definition, String context, String category) {
        GlossaryEntry entry = new GlossaryEntry();
        entry.setTerm(term);
        entry.setDefinition(definition);
        entry.setRegulatoryContext(context);
        entry.setCategory(category);
        return entry;
    }
}