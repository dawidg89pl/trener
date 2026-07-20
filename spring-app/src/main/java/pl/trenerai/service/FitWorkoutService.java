package pl.trenerai.service;

import com.garmin.fit.BufferEncoder;
import com.garmin.fit.DateTime;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.Intensity;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Sport;
import com.garmin.fit.WktStepDuration;
import com.garmin.fit.WktStepTarget;
import com.garmin.fit.WorkoutMesg;
import com.garmin.fit.WorkoutStepMesg;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import pl.trenerai.model.GeneratedWorkout;

@Service
public class FitWorkoutService {
    public byte[] encode(GeneratedWorkout workout) {
        try {
            List<FitStep> steps = parseSteps(workout.getZwoContent());
            if (steps.isEmpty()) throw new IllegalStateException("Trening nie zawiera kroków do zapisania w FIT.");

            BufferEncoder encoder = new BufferEncoder(Fit.ProtocolVersion.V2_0);
            FileIdMesg fileId = new FileIdMesg();
            fileId.setType(com.garmin.fit.File.WORKOUT);
            fileId.setManufacturer(Manufacturer.DEVELOPMENT);
            fileId.setProduct(1);
            fileId.setSerialNumber(workout.getId() == null ? System.currentTimeMillis() : workout.getId());
            fileId.setTimeCreated(new DateTime(workout.getCreatedAt()));
            encoder.write(fileId);

            WorkoutMesg workoutMessage = new WorkoutMesg();
            workoutMessage.setWktName(workout.getName());
            workoutMessage.setSport(Sport.CYCLING);
            workoutMessage.setNumValidSteps(steps.size());
            encoder.write(workoutMessage);

            for (int index = 0; index < steps.size(); index++) {
                FitStep source = steps.get(index);
                WorkoutStepMesg step = new WorkoutStepMesg();
                step.setMessageIndex(index);
                step.setWktStepName(source.name());
                step.setIntensity(source.intensity());
                step.setDurationType(WktStepDuration.TIME);
                step.setDurationTime((float) source.durationSeconds());
                step.setTargetType(WktStepTarget.POWER);
                step.setTargetValue(0L);
                step.setCustomTargetPowerLow(percentPower(source.powerLow()));
                step.setCustomTargetPowerHigh(percentPower(source.powerHigh()));
                if (source.cadence() > 0) step.setNotes("Kadencja: " + source.cadence() + " rpm");
                encoder.write(step);
            }
            return encoder.close();
        } catch (IllegalStateException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Nie udało się utworzyć pliku FIT.", error);
        }
    }

    private List<FitStep> parseSteps(String zwo) throws Exception {
        List<FitStep> steps = new ArrayList<>();
        var document = secureDocumentBuilderFactory().newDocumentBuilder()
                .parse(new InputSource(new StringReader(zwo)));
        var workouts = document.getElementsByTagName("workout");
        if (workouts.getLength() == 0) return steps;
        Node workout = workouts.item(0);
        for (int index = 0; index < workout.getChildNodes().getLength(); index++) {
            Node node = workout.getChildNodes().item(index);
            if (node instanceof Element element) addElement(element, steps);
        }
        return steps;
    }

    private void addElement(Element element, List<FitStep> steps) {
        if ("IntervalsT".equals(element.getTagName())) {
            int repeats = intAttribute(element, "Repeat", 1);
            int cadence = intAttribute(element, "Cadence", 0);
            for (int repeat = 0; repeat < repeats; repeat++) {
                steps.add(constantStep("Interwał", Intensity.INTERVAL,
                        intAttribute(element, "OnDuration", 0), doubleAttribute(element, "OnPower", 0), cadence));
                steps.add(constantStep("Regeneracja", Intensity.RECOVERY,
                        intAttribute(element, "OffDuration", 0), doubleAttribute(element, "OffPower", 0), cadence));
            }
            return;
        }
        int duration = intAttribute(element, "Duration", 0);
        if (duration <= 0) return;
        double low = element.hasAttribute("PowerLow")
                ? doubleAttribute(element, "PowerLow", 0) : doubleAttribute(element, "Power", 0);
        double high = element.hasAttribute("PowerHigh")
                ? doubleAttribute(element, "PowerHigh", low) : doubleAttribute(element, "Power", low);
        steps.add(new FitStep(stepName(element.getTagName()), stepIntensity(element.getTagName()),
                duration, low, high, intAttribute(element, "Cadence", 0)));
    }

    private FitStep constantStep(String name, Intensity intensity, int duration, double power, int cadence) {
        return new FitStep(name, intensity, duration, power, power, cadence);
    }

    private String stepName(String tagName) {
        return switch (tagName) {
            case "Warmup" -> "Rozgrzewka";
            case "Cooldown" -> "Schłodzenie";
            case "Ramp" -> "Rampa mocy";
            case "FreeRide" -> "Jazda swobodna";
            default -> "Stała moc";
        };
    }

    private Intensity stepIntensity(String tagName) {
        return switch (tagName) {
            case "Warmup" -> Intensity.WARMUP;
            case "Cooldown" -> Intensity.COOLDOWN;
            default -> Intensity.ACTIVE;
        };
    }

    private long percentPower(double powerFraction) {
        long percentage = Math.max(0, Math.min(1000, Math.round(powerFraction * 100)));
        return 1000 + percentage;
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private int intAttribute(Element element, String name, int fallback) {
        try { return Integer.parseInt(element.getAttribute(name)); } catch (Exception ignored) { return fallback; }
    }
    private double doubleAttribute(Element element, String name, double fallback) {
        try { return Double.parseDouble(element.getAttribute(name)); } catch (Exception ignored) { return fallback; }
    }

    private record FitStep(String name, Intensity intensity, int durationSeconds,
            double powerLow, double powerHigh, int cadence) {}
}
