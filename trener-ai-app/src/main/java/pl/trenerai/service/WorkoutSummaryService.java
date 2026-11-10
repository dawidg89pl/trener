package pl.trenerai.service;

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
public class WorkoutSummaryService {
    private static final double[] ZONE_UPPER_BOUNDS = {0.55, 0.75, 0.90, 1.05, 1.20, 1.50};
    private static final String[] ZONE_LABELS = {
            "Z1 Regeneracja", "Z2 Wytrzymałość", "Z3 Tempo", "Z4 Próg",
            "Z5 VO₂max", "Z6 Beztlenowa", "Z7 Neuromuskularna"
    };
    private static final String[] ZONE_RANGES = {
            "do 55% FTP", "56–75% FTP", "76–90% FTP", "91–105% FTP",
            "106–120% FTP", "121–150% FTP", "powyżej 150% FTP"
    };

    public WorkoutSummary summarize(GeneratedWorkout workout) {
        int[] zoneSeconds = new int[ZONE_LABELS.length];
        List<RawStep> rawSteps = new ArrayList<>();
        try {
            var document = secureDocumentBuilderFactory().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(workout.getZwoContent())));
            String description = textOfFirst(document.getElementsByTagName("description").item(0));
            description = description.replaceFirst("^\\[Planowana data wykonania: [^]]+\\]\\s*", "").trim();
            var workoutNodes = document.getElementsByTagName("workout");
            if (workoutNodes.getLength() > 0) {
                Node workoutNode = workoutNodes.item(0);
                for (int index = 0; index < workoutNode.getChildNodes().getLength(); index++) {
                    Node node = workoutNode.getChildNodes().item(index);
                    if (node instanceof Element step) addStep(step, zoneSeconds, rawSteps);
                }
            }
            return buildSummary(description, zoneSeconds, rawSteps);
        } catch (Exception ignored) {
            return buildSummary("Opis treningu jest dostępny w pliku ZWO.", zoneSeconds, rawSteps);
        }
    }

    private void addStep(Element step, int[] zoneSeconds, List<RawStep> rawSteps) {
        if ("IntervalsT".equals(step.getTagName())) {
            int repeats = intAttribute(step, "Repeat", 1);
            int cadence = intAttribute(step, "Cadence", 0);
            for (int repeat = 0; repeat < repeats; repeat++) {
                int onDuration = intAttribute(step, "OnDuration", 0);
                double onPower = doubleAttribute(step, "OnPower", 0);
                addConstant(zoneSeconds, onDuration, onPower);
                rawSteps.add(new RawStep("Interwał", onDuration, onPower, onPower, cadence));
                int offDuration = intAttribute(step, "OffDuration", 0);
                double offPower = doubleAttribute(step, "OffPower", 0);
                addConstant(zoneSeconds, offDuration, offPower);
                rawSteps.add(new RawStep("Regeneracja", offDuration, offPower, offPower, cadence));
            }
            return;
        }
        int duration = intAttribute(step, "Duration", 0);
        if (duration <= 0) return;
        int cadence = intAttribute(step, "Cadence", 0);
        if (step.hasAttribute("Power")) {
            double power = doubleAttribute(step, "Power", 0);
            addConstant(zoneSeconds, duration, power);
            rawSteps.add(new RawStep(stepLabel(step.getTagName()), duration, power, power, cadence));
        } else if (step.hasAttribute("PowerLow") || step.hasAttribute("PowerHigh")) {
            double low = doubleAttribute(step, "PowerLow", 0);
            double high = doubleAttribute(step, "PowerHigh", low);
            for (int second = 0; second < duration; second++) {
                double progress = (second + 0.5) / duration;
                zoneSeconds[zoneIndex(low + (high - low) * progress)]++;
            }
            rawSteps.add(new RawStep(stepLabel(step.getTagName()), duration, low, high, cadence));
        }
    }

    private void addConstant(int[] zoneSeconds, int duration, double power) {
        if (duration > 0) zoneSeconds[zoneIndex(power)] += duration;
    }

    private int zoneIndex(double power) {
        for (int index = 0; index < ZONE_UPPER_BOUNDS.length; index++) {
            if (power <= ZONE_UPPER_BOUNDS[index]) return index;
        }
        return ZONE_LABELS.length - 1;
    }

    private WorkoutSummary buildSummary(String description, int[] seconds, List<RawStep> rawSteps) {
        int totalSeconds = 0;
        for (int value : seconds) totalSeconds += value;
        List<PowerZone> zones = new ArrayList<>();
        for (int index = 0; index < seconds.length; index++) {
            double percentage = totalSeconds == 0 ? 0 : seconds[index] * 100.0 / totalSeconds;
            zones.add(new PowerZone(index + 1, ZONE_LABELS[index], ZONE_RANGES[index],
                    formatDuration(seconds[index]), percentage));
        }
        double maximumPower = rawSteps.stream().mapToDouble(step -> Math.max(step.powerLow(), step.powerHigh()))
                .max().orElse(1.0);
        maximumPower = Math.max(1.0, maximumPower);
        List<WorkoutStep> steps = new ArrayList<>();
        for (RawStep step : rawSteps) {
            if (step.durationSeconds() <= 0) continue;
            double width = totalSeconds == 0 ? 0 : step.durationSeconds() * 100.0 / totalSeconds;
            double startHeight = 14 + Math.min(1, step.powerLow() / maximumPower) * 80;
            double endHeight = 14 + Math.min(1, step.powerHigh() / maximumPower) * 80;
            String power = Math.round(step.powerLow() * 100) == Math.round(step.powerHigh() * 100)
                    ? Math.round(step.powerLow() * 100) + "% FTP"
                    : Math.round(step.powerLow() * 100) + "–" + Math.round(step.powerHigh() * 100) + "% FTP";
            String cadence = step.cadence() > 0 ? step.cadence() + " rpm" : "Bez wskazania kadencji";
            steps.add(new WorkoutStep(step.label(), formatDuration(step.durationSeconds()), power, cadence,
                    width, startHeight, endHeight));
        }
        return new WorkoutSummary(description, formatDuration(totalSeconds), zones, steps);
    }

    private String stepLabel(String tagName) {
        return switch (tagName) {
            case "Warmup" -> "Rozgrzewka";
            case "Cooldown" -> "Schłodzenie";
            case "Ramp" -> "Rampa mocy";
            case "FreeRide" -> "Jazda swobodna";
            default -> "Odcinek ze stałą mocą";
        };
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

    private String textOfFirst(Node node) { return node == null ? "" : node.getTextContent(); }
    private int intAttribute(Element element, String name, int fallback) {
        try { return Integer.parseInt(element.getAttribute(name)); } catch (Exception ignored) { return fallback; }
    }
    private double doubleAttribute(Element element, String name, double fallback) {
        try { return Double.parseDouble(element.getAttribute(name)); } catch (Exception ignored) { return fallback; }
    }
    private String formatDuration(int seconds) {
        if (seconds <= 0) return "0 min";
        int minutes = (int) Math.round(seconds / 60.0);
        return minutes >= 60 ? minutes / 60 + " h " + minutes % 60 + " min" : minutes + " min";
    }

    private record RawStep(String label, int durationSeconds, double powerLow, double powerHigh, int cadence) {}
    public record WorkoutSummary(String description, String duration, List<PowerZone> zones, List<WorkoutStep> steps) {}
    public record PowerZone(int number, String label, String range, String duration, double percentage) {}
    public record WorkoutStep(String label, String duration, String power, String cadence,
            double widthPercentage, double startHeight, double endHeight) {}
}
