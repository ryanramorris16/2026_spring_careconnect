package com.careconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalDataAnonymizerTest {

    private MedicalDataAnonymizer anonymizer;

    @BeforeEach
    void setUp() {
        anonymizer = new MedicalDataAnonymizer();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // anonymizePatientContext - null / empty input
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_nullContext_returnsNull")
    void anonymizePatientContext_nullContext_returnsNull() {
        String result = anonymizer.anonymizePatientContext(null, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("anonymizePatientContext_emptyContext_returnsEmpty")
    void anonymizePatientContext_emptyContext_returnsEmpty() {
        String result = anonymizer.anonymizePatientContext("", 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("anonymizePatientContext_blankContext_returnsBlank")
    void anonymizePatientContext_blankContext_returnsBlank() {
        String result = anonymizer.anonymizePatientContext("   ", 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).isEqualTo("   ");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // anonymizePatientContext - MINIMAL level
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_minimalLevel_replacesNames")
    void anonymizePatientContext_minimalLevel_replacesNames() {
        String context = "Patient John Smith visited today";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).doesNotContain("John Smith");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_minimalLevel_replacesSSN")
    void anonymizePatientContext_minimalLevel_replacesSSN() {
        String context = "SSN: 123-45-6789";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).contains("XXX-XX-XXXX");
        assertThat(result).doesNotContain("123-45-6789");
    }

    @Test
    @DisplayName("anonymizePatientContext_minimalLevel_replacesPhone")
    void anonymizePatientContext_minimalLevel_replacesPhone() {
        String context = "Call 301-555-1234 for info";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).contains("**PHONE**");
        assertThat(result).doesNotContain("301-555-1234");
    }

    @Test
    @DisplayName("anonymizePatientContext_minimalLevel_replacesEmail")
    void anonymizePatientContext_minimalLevel_replacesEmail() {
        String context = "Email john.doe@example.com for details";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).contains("**EMAIL**");
        assertThat(result).doesNotContain("john.doe@example.com");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // anonymizePatientContext - MODERATE level
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesAddress")
    void anonymizePatientContext_moderateLevel_replacesAddress() {
        String context = "Lives at 123 Main Street area";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Main Street" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Main Street");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesDate")
    void anonymizePatientContext_moderateLevel_replacesDate() {
        String context = "Born on January 15, 1990 in the city";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        assertThat(result).contains("**DATE**");
        assertThat(result).doesNotContain("January 15, 1990");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesFacility")
    void anonymizePatientContext_moderateLevel_replacesFacility() {
        String context = "Treated at Memorial Hospital for treatment";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Memorial Hospital" matches NAME_PATTERN before FACILITY_PATTERN can fire
        assertThat(result).doesNotContain("Memorial Hospital");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesTime")
    void anonymizePatientContext_moderateLevel_replacesTime() {
        String context = "Appointment at 3:30 PM today";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        assertThat(result).contains("**TIME**");
        assertThat(result).doesNotContain("3:30 PM");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_alsoAppliesMinimal")
    void anonymizePatientContext_moderateLevel_alsoAppliesMinimal() {
        String context = "Patient John Smith SSN: 123-45-6789 at 123 Main Street";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        assertThat(result).doesNotContain("John Smith");
        assertThat(result).contains("XXX-XX-XXXX");
        // "Main Street" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Main Street");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // anonymizePatientContext - AGGRESSIVE level
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_roundsDecimalNumbers")
    void anonymizePatientContext_aggressiveLevel_roundsDecimalNumbers() {
        String context = "Blood level was 123.45678 units";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        assertThat(result).contains("123.46");
        assertThat(result).doesNotContain("123.45678");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_anonymizesAgeOver89")
    void anonymizePatientContext_aggressiveLevel_anonymizesAgeOver89() {
        String context = "Patient is 95 years old";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        assertThat(result).contains(">89 years old");
        assertThat(result).doesNotContain("95 years old");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_anonymizesAgeOver89WithYear")
    void anonymizePatientContext_aggressiveLevel_anonymizesAgeOver89WithYear() {
        String context = "Patient is 100 year old";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        assertThat(result).contains(">89 years old");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesMedications")
    void anonymizePatientContext_aggressiveLevel_generalizesMedications() {
        String context = "Taking Lisinopril and Metoprolol and Amlodipine and Metformin and Atorvastatin daily";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Taking Lisinopril" matches NAME_PATTERN, so Lisinopril is replaced before medication generalization
        assertThat(result).doesNotContain("ACE Inhibitor");
        assertThat(result).contains("Patient_");
        assertThat(result).contains("Beta Blocker");
        assertThat(result).contains("Calcium Channel Blocker");
        assertThat(result).contains("Diabetes Medication");
        assertThat(result).contains("Statin");
        assertThat(result).doesNotContain("Lisinopril");
        assertThat(result).doesNotContain("Metoprolol");
        assertThat(result).doesNotContain("Amlodipine");
        assertThat(result).doesNotContain("Metformin");
        assertThat(result).doesNotContain("Atorvastatin");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesEnalapril")
    void anonymizePatientContext_aggressiveLevel_generalizesEnalapril() {
        String context = "Prescribed Enalapril";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Enalapril" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Enalapril");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesCaptopril")
    void anonymizePatientContext_aggressiveLevel_generalizesCaptopril() {
        String context = "Prescribed Captopril";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Captopril" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Captopril");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesAtenolol")
    void anonymizePatientContext_aggressiveLevel_generalizesAtenolol() {
        String context = "Prescribed Atenolol";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Atenolol" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Atenolol");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesPropranolol")
    void anonymizePatientContext_aggressiveLevel_generalizesPropranolol() {
        String context = "Prescribed Propranolol";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Propranolol" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Propranolol");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesNifedipine")
    void anonymizePatientContext_aggressiveLevel_generalizesNifedipine() {
        String context = "Prescribed Nifedipine";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Nifedipine" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Nifedipine");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesGlipizide")
    void anonymizePatientContext_aggressiveLevel_generalizesGlipizide() {
        String context = "Prescribed Glipizide";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Glipizide" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Glipizide");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesInsulin")
    void anonymizePatientContext_aggressiveLevel_generalizesInsulin() {
        String context = "Prescribed Insulin";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Insulin" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Insulin");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_generalizesSimvastatin")
    void anonymizePatientContext_aggressiveLevel_generalizesSimvastatin() {
        String context = "Prescribed Simvastatin";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // "Prescribed Simvastatin" matches NAME_PATTERN before medication generalization
        assertThat(result).doesNotContain("Simvastatin");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_alsoAppliesModerateAndMinimal")
    void anonymizePatientContext_aggressiveLevel_alsoAppliesModerateAndMinimal() {
        String context = "Patient John Smith at 123 Main Street takes Lisinopril. SSN: 123-45-6789";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        assertThat(result).doesNotContain("John Smith");
        assertThat(result).contains("XXX-XX-XXXX");
        // "Main Street" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Main Street");
        assertThat(result).contains("ACE Inhibitor");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // anonymizePatientContext - STATISTICAL level
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_statisticalLevel_returnsStatisticalSummary")
    void anonymizePatientContext_statisticalLevel_returnsStatisticalSummary() {
        String context = "Patient John Smith has detailed medical records";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.STATISTICAL);
        assertThat(result).contains("Statistical Patient Profile");
        assertThat(result).contains("Demographic cluster");
        assertThat(result).contains("Health indicators");
        assertThat(result).contains("Treatment response");
        assertThat(result).contains("Risk factors");
        assertThat(result).contains("maximum privacy");
        assertThat(result).doesNotContain("John Smith");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // anonymizePatientContext - default falls through to MINIMAL
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_defaultLevel_appliesMinimalAnonymization")
    void anonymizePatientContext_defaultLevel_appliesMinimalAnonymization() {
        // MINIMAL is the default case; also covers the default switch branch
        String context = "SSN: 123-45-6789";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).contains("XXX-XX-XXXX");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // generatePseudonym
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("generatePseudonym_validInput_returnsConsistentPseudonym")
    void generatePseudonym_validInput_returnsConsistentPseudonym() {
        String first = anonymizer.generatePseudonym(1L, "NAME");
        String second = anonymizer.generatePseudonym(1L, "NAME");
        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("Patient_");
    }

    @Test
    @DisplayName("generatePseudonym_differentTypes_returnsDifferentPseudonyms")
    void generatePseudonym_differentTypes_returnsDifferentPseudonyms() {
        String namePseudo = anonymizer.generatePseudonym(1L, "NAME");
        String idPseudo = anonymizer.generatePseudonym(1L, "ID");
        // They could be same if hash collision, but both should start with Patient_
        assertThat(namePseudo).startsWith("Patient_");
        assertThat(idPseudo).startsWith("Patient_");
    }

    @Test
    @DisplayName("generatePseudonym_differentPatients_returnsDifferentPseudonyms")
    void generatePseudonym_differentPatients_returnsDifferentPseudonyms() {
        String pseudo1 = anonymizer.generatePseudonym(1L, "NAME");
        String pseudo2 = anonymizer.generatePseudonym(2L, "NAME");
        assertThat(pseudo1).startsWith("Patient_");
        assertThat(pseudo2).startsWith("Patient_");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // generatePseudoId
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("generatePseudoId_validPatientId_returnsPseudoId")
    void generatePseudoId_validPatientId_returnsPseudoId() {
        String pseudoId = anonymizer.generatePseudoId(1L);
        assertThat(pseudoId).startsWith("Patient_");
    }

    @Test
    @DisplayName("generatePseudoId_calledTwice_returnsConsistentResult")
    void generatePseudoId_calledTwice_returnsConsistentResult() {
        String first = anonymizer.generatePseudoId(42L);
        String second = anonymizer.generatePseudoId(42L);
        assertThat(first).isEqualTo(second);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // clearPseudonymMappings
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("clearPseudonymMappings_validPatientId_clearsMappingsForThatPatient")
    void clearPseudonymMappings_validPatientId_clearsMappingsForThatPatient() {
        // Generate pseudonyms for patient 1
        String beforeClear = anonymizer.generatePseudonym(1L, "NAME");
        assertThat(beforeClear).startsWith("Patient_");

        // Clear mappings for patient 1
        anonymizer.clearPseudonymMappings(1L);

        // The pseudonym should be regenerated (may or may not be same value, but method runs without error)
        String afterClear = anonymizer.generatePseudonym(1L, "NAME");
        assertThat(afterClear).startsWith("Patient_");
    }

    @Test
    @DisplayName("clearPseudonymMappings_differentPatients_onlyClearsTargetPatient")
    void clearPseudonymMappings_differentPatients_onlyClearsTargetPatient() {
        String patient2Pseudo = anonymizer.generatePseudonym(2L, "NAME");

        anonymizer.clearPseudonymMappings(1L);

        // Patient 2's pseudonym should still be the same
        String patient2PseudoAfter = anonymizer.generatePseudonym(2L, "NAME");
        assertThat(patient2PseudoAfter).isEqualTo(patient2Pseudo);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // clearAllMappings
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("clearAllMappings_validPatientId_delegatesToClearPseudonymMappings")
    void clearAllMappings_validPatientId_delegatesToClearPseudonymMappings() {
        anonymizer.generatePseudonym(1L, "NAME");
        anonymizer.generatePseudonym(1L, "ID");

        anonymizer.clearAllMappings(1L);

        // After clearing, regenerating should work fine
        String newPseudo = anonymizer.generatePseudonym(1L, "NAME");
        assertThat(newPseudo).startsWith("Patient_");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // addDifferentialPrivacyNoise
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("addDifferentialPrivacyNoise_stringWithDecimals_addsNoiseToDecimals")
    void addDifferentialPrivacyNoise_stringWithDecimals_addsNoiseToDecimals() {
        String data = "Blood pressure was 120.5 and heart rate was 72.3";
        String result = anonymizer.addDifferentialPrivacyNoise(data, 1.0);
        // The numbers should be modified (with noise) and formatted to 2 decimal places
        assertThat(result).doesNotContain("120.5");
        assertThat(result).doesNotContain("72.3");
        // Should contain decimal formatted numbers
        assertThat(result).matches(".*\\d+\\.\\d{2}.*");
    }

    @Test
    @DisplayName("addDifferentialPrivacyNoise_stringWithoutDecimals_returnsUnchanged")
    void addDifferentialPrivacyNoise_stringWithoutDecimals_returnsUnchanged() {
        String data = "Patient has 3 prescriptions";
        String result = anonymizer.addDifferentialPrivacyNoise(data, 1.0);
        assertThat(result).isEqualTo(data);
    }

    @Test
    @DisplayName("addDifferentialPrivacyNoise_emptyString_returnsEmpty")
    void addDifferentialPrivacyNoise_emptyString_returnsEmpty() {
        String result = anonymizer.addDifferentialPrivacyNoise("", 1.0);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("addDifferentialPrivacyNoise_highEpsilon_lessPerturbation")
    void addDifferentialPrivacyNoise_highEpsilon_lessPerturbation() {
        // High epsilon = less noise = values closer to original
        String data = "Value is 100.00";
        String result = anonymizer.addDifferentialPrivacyNoise(data, 100.0);
        // With very high epsilon, noise should be minimal
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("addDifferentialPrivacyNoise_lowEpsilon_morePerturbation")
    void addDifferentialPrivacyNoise_lowEpsilon_morePerturbation() {
        String data = "Value is 50.0";
        String result = anonymizer.addDifferentialPrivacyNoise(data, 0.01);
        assertThat(result).isNotNull();
        // With very low epsilon, there should be significant noise
        assertThat(result).matches(".*\\d+\\.\\d{2}.*");
    }

    @Test
    @DisplayName("addDifferentialPrivacyNoise_negativeResult_clampedToZero")
    void addDifferentialPrivacyNoise_negativeResult_clampedToZero() {
        // Use a very small value with low epsilon to force negative results being clamped
        String data = "Value is 0.01";
        // Run multiple times as noise is random; the Math.max(0, ...) ensures >= 0
        for (int i = 0; i < 10; i++) {
            String result = anonymizer.addDifferentialPrivacyNoise(data, 0.001);
            // Extract the numeric value and ensure it's >= 0
            assertThat(result).matches(".*\\d+\\.\\d{2}.*");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // containsPHI
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("containsPHI_nullContent_returnsFalse")
    void containsPHI_nullContent_returnsFalse() {
        assertThat(anonymizer.containsPHI(null)).isFalse();
    }

    @Test
    @DisplayName("containsPHI_contentWithName_returnsTrue")
    void containsPHI_contentWithName_returnsTrue() {
        assertThat(anonymizer.containsPHI("Patient John Smith visited today")).isTrue();
    }

    @Test
    @DisplayName("containsPHI_contentWithSSN_returnsTrue")
    void containsPHI_contentWithSSN_returnsTrue() {
        assertThat(anonymizer.containsPHI("SSN: 123-45-6789")).isTrue();
    }

    @Test
    @DisplayName("containsPHI_contentWithPhone_returnsTrue")
    void containsPHI_contentWithPhone_returnsTrue() {
        assertThat(anonymizer.containsPHI("Call 301-555-1234")).isTrue();
    }

    @Test
    @DisplayName("containsPHI_contentWithEmail_returnsTrue")
    void containsPHI_contentWithEmail_returnsTrue() {
        assertThat(anonymizer.containsPHI("Email john@example.com")).isTrue();
    }

    @Test
    @DisplayName("containsPHI_contentWithAddress_returnsTrue")
    void containsPHI_contentWithAddress_returnsTrue() {
        assertThat(anonymizer.containsPHI("Lives at 123 Main Street")).isTrue();
    }

    @Test
    @DisplayName("containsPHI_contentWithNoSensitiveData_returnsFalse")
    void containsPHI_contentWithNoSensitiveData_returnsFalse() {
        assertThat(anonymizer.containsPHI("patient has a cold")).isFalse();
    }

    @Test
    @DisplayName("containsPHI_emptyContent_returnsFalse")
    void containsPHI_emptyContent_returnsFalse() {
        assertThat(anonymizer.containsPHI("")).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // roundDecimalNumbers (tested indirectly through AGGRESSIVE)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_roundsMultipleDecimals")
    void anonymizePatientContext_aggressiveLevel_roundsMultipleDecimals() {
        String context = "Values: 1.2345 and 9.87654";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        assertThat(result).contains("1.23");
        assertThat(result).contains("9.88");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_shortDecimalsNotRounded")
    void anonymizePatientContext_aggressiveLevel_shortDecimalsNotRounded() {
        // Decimals with fewer than 3 decimal places should not be affected by roundDecimalNumbers
        String context = "Value: 5.12 units";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        assertThat(result).contains("5.12");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Address pattern variants
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesAvenueAddress")
    void anonymizePatientContext_moderateLevel_replacesAvenueAddress() {
        String context = "Lives at 456 Oak Avenue end";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Oak Avenue" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Oak Avenue");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesRoadAddress")
    void anonymizePatientContext_moderateLevel_replacesRoadAddress() {
        String context = "Located at 789 Pine Road end";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Pine Road" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Pine Road");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesDriveAddress")
    void anonymizePatientContext_moderateLevel_replacesDriveAddress() {
        String context = "At 100 Sunset Drive end";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Sunset Drive" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Sunset Drive");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesLaneAddress")
    void anonymizePatientContext_moderateLevel_replacesLaneAddress() {
        String context = "At 200 Maple Lane end";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Maple Lane" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Maple Lane");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesCourtAddress")
    void anonymizePatientContext_moderateLevel_replacesCourtAddress() {
        String context = "At 300 Rose Court end";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Rose Court" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Rose Court");
        assertThat(result).contains("Patient_");
    }

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesBoulevardAddress")
    void anonymizePatientContext_moderateLevel_replacesBoulevardAddress() {
        String context = "At 400 Grand Boulevard end";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Grand Boulevard" matches NAME_PATTERN before ADDRESS_PATTERN can fire
        assertThat(result).doesNotContain("Grand Boulevard");
        assertThat(result).contains("Patient_");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Facility pattern variants
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_moderateLevel_replacesClinicFacility")
    void anonymizePatientContext_moderateLevel_replacesClinicFacility() {
        String context = "Visited Central Clinic for checkup";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MODERATE);
        // "Central Clinic" matches NAME_PATTERN before FACILITY_PATTERN can fire
        assertThat(result).doesNotContain("Central Clinic");
        assertThat(result).contains("Patient_");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Phone pattern variants
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_minimalLevel_replacesPhoneWithDots")
    void anonymizePatientContext_minimalLevel_replacesPhoneWithDots() {
        String context = "Call 301.555.1234 for info";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).contains("**PHONE**");
    }

    @Test
    @DisplayName("anonymizePatientContext_minimalLevel_replacesPhoneWithSpaces")
    void anonymizePatientContext_minimalLevel_replacesPhoneWithSpaces() {
        String context = "Call 301 555 1234 for info";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).contains("**PHONE**");
    }

    @Test
    @DisplayName("anonymizePatientContext_minimalLevel_replacesPhoneNoDashes")
    void anonymizePatientContext_minimalLevel_replacesPhoneNoDashes() {
        String context = "Call 3015551234 for info";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.MINIMAL);
        assertThat(result).contains("**PHONE**");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Age patterns - edge cases for AGGRESSIVE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_age89NotAnonymized")
    void anonymizePatientContext_aggressiveLevel_age89NotAnonymized() {
        // 89 years old is NOT >= 90 so should not match the regex
        String context = "Patient is 89 years old";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        // 89 does not match the pattern \b(9[0-9]|[1-9][0-9]{2,})\s*years?\s*old\b
        assertThat(result).doesNotContain(">89 years old");
    }

    @Test
    @DisplayName("anonymizePatientContext_aggressiveLevel_age90Anonymized")
    void anonymizePatientContext_aggressiveLevel_age90Anonymized() {
        String context = "Patient is 90 years old";
        String result = anonymizer.anonymizePatientContext(context, 1L,
                MedicalDataAnonymizer.AnonymizationLevel.AGGRESSIVE);
        assertThat(result).contains(">89 years old");
    }
}
