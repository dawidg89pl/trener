package pl.trenerai.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import pl.trenerai.model.AppUser;
import pl.trenerai.model.GeneratedWorkout;
import pl.trenerai.service.WorkoutSummaryService.*;

import java.time.LocalDate;

class WorkoutSummaryServiceTest {

    private final WorkoutSummaryService service = new WorkoutSummaryService();

    private GeneratedWorkout workout(String zwo) {
        return new GeneratedWorkout(
                new AppUser("test@test.pl", "hash"),
                "Test Workout",
                LocalDate.now(),
                zwo);
    }

    @Test
    void summarizeSteadyStateWorkout() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Test</name>
                  <description>Trening testowy</description>
                  <workout>
                    <Warmup Duration="300" PowerLow="0.4" PowerHigh="0.7" Cadence="85"/>
                    <SteadyState Duration="1200" Power="0.88" Cadence="90"/>
                    <Cooldown Duration="300" PowerLow="0.6" PowerHigh="0.4" Cadence="80"/>
                  </workout>
                </workout_file>
                """;

        WorkoutSummary summary = service.summarize(workout(zwo));

        assertEquals("Trening testowy", summary.description());
        assertEquals("30 min", summary.duration());
        assertFalse(summary.zones().isEmpty());
        assertEquals(3, summary.steps().size());
        assertEquals("Rozgrzewka", summary.steps().get(0).label());
        assertEquals("Odcinek ze stałą mocą", summary.steps().get(1).label());
        assertEquals("Schłodzenie", summary.steps().get(2).label());
    }

    @Test
    void summarizeIntervalWorkout() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Interwały</name>
                  <description>Interwały VO2max</description>
                  <workout>
                    <IntervalsT Repeat="3" OnDuration="180" OnPower="1.1" OffDuration="180" OffPower="0.5" Cadence="100"/>
                  </workout>
                </workout_file>
                """;

        WorkoutSummary summary = service.summarize(workout(zwo));

        assertEquals(6, summary.steps().size());
        assertEquals("Interwał", summary.steps().get(0).label());
        assertEquals("Regeneracja", summary.steps().get(1).label());
        assertEquals("18 min", summary.duration());
    }

    @Test
    void summarizeInvalidXmlReturnsFallback() {
        WorkoutSummary summary = service.summarize(workout("not xml at all"));

        assertEquals("Opis treningu jest dostępny w pliku ZWO.", summary.description());
        assertEquals("0 min", summary.duration());
        assertTrue(summary.steps().isEmpty());
    }

    @Test
    void summarizeEmptyWorkoutSection() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Pusty</name>
                  <description>Bez kroków</description>
                  <workout/>
                </workout_file>
                """;

        WorkoutSummary summary = service.summarize(workout(zwo));

        assertEquals("0 min", summary.duration());
        assertTrue(summary.steps().isEmpty());
    }

    @Test
    void zoneDistributionSumsTo100Percent() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Zones</name>
                  <description>Test stref</description>
                  <workout>
                    <SteadyState Duration="600" Power="0.5" Cadence="80"/>
                    <SteadyState Duration="600" Power="0.85" Cadence="90"/>
                    <SteadyState Duration="600" Power="1.1" Cadence="95"/>
                  </workout>
                </workout_file>
                """;

        WorkoutSummary summary = service.summarize(workout(zwo));

        double totalPercentage = summary.zones().stream()
                .mapToDouble(PowerZone::percentage).sum();
        assertEquals(100.0, totalPercentage, 0.1);
    }

    @Test
    void stepPowerFormattedCorrectly() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Power</name>
                  <description>Test mocy</description>
                  <workout>
                    <SteadyState Duration="600" Power="0.88" Cadence="90"/>
                    <Warmup Duration="300" PowerLow="0.4" PowerHigh="0.7" Cadence="85"/>
                  </workout>
                </workout_file>
                """;

        WorkoutSummary summary = service.summarize(workout(zwo));

        assertEquals("88% FTP", summary.steps().get(0).power());
        assertEquals("40–70% FTP", summary.steps().get(1).power());
    }

    @Test
    void descriptionStripsPlannedDatePrefix() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Test</name>
                  <description>[Planowana data wykonania: 2026-11-15] Prawdziwy opis</description>
                  <workout>
                    <SteadyState Duration="600" Power="0.7" Cadence="90"/>
                  </workout>
                </workout_file>
                """;

        WorkoutSummary summary = service.summarize(workout(zwo));

        assertEquals("Prawdziwy opis", summary.description());
    }
}
