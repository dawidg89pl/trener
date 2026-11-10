package pl.trenerai.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import pl.trenerai.model.AppUser;
import pl.trenerai.model.GeneratedWorkout;

import java.time.LocalDate;

class FitWorkoutServiceTest {

    private final FitWorkoutService service = new FitWorkoutService();

    private GeneratedWorkout workout(String zwo) {
        return new GeneratedWorkout(
                new AppUser("test@test.pl", "hash"),
                "Test",
                LocalDate.now(),
                zwo);
    }

    @Test
    void encodeProducesNonEmptyFitFile() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Test</name>
                  <workout>
                    <Warmup Duration="300" PowerLow="0.4" PowerHigh="0.7" Cadence="85"/>
                    <SteadyState Duration="1200" Power="0.88" Cadence="90"/>
                    <Cooldown Duration="300" PowerLow="0.6" PowerHigh="0.4" Cadence="80"/>
                  </workout>
                </workout_file>
                """;

        byte[] fit = service.encode(workout(zwo));

        assertNotNull(fit);
        assertTrue(fit.length > 100);
    }

    @Test
    void encodeIntervalsWorkout() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Intervals</name>
                  <workout>
                    <IntervalsT Repeat="4" OnDuration="60" OnPower="1.2" OffDuration="120" OffPower="0.5" Cadence="100"/>
                  </workout>
                </workout_file>
                """;

        byte[] fit = service.encode(workout(zwo));

        assertNotNull(fit);
        assertTrue(fit.length > 0);
    }

    @Test
    void encodeEmptyWorkoutThrows() {
        String zwo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workout_file>
                  <name>Empty</name>
                  <workout/>
                </workout_file>
                """;

        assertThrows(IllegalStateException.class, () -> service.encode(workout(zwo)));
    }

    @Test
    void encodeInvalidXmlThrows() {
        assertThrows(IllegalStateException.class, () -> service.encode(workout("not xml")));
    }
}
