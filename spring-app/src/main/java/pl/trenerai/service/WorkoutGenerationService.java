package pl.trenerai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pl.trenerai.model.*;
import pl.trenerai.repository.*;

@Service
public class WorkoutGenerationService {
    private final CurrentUserService current; private final UserSettingsRepository settings; private final ActivityRepository activities; private final GeneratedWorkoutRepository workouts; private final SecretCrypto crypto; private final UserEventService eventLog;
    private final ObjectMapper json = new ObjectMapper(); private final HttpClient http = HttpClient.newHttpClient();
    public WorkoutGenerationService(CurrentUserService current, UserSettingsRepository settings, ActivityRepository activities, GeneratedWorkoutRepository workouts, SecretCrypto crypto, UserEventService eventLog) { this.current=current;this.settings=settings;this.activities=activities;this.workouts=workouts;this.crypto=crypto;this.eventLog=eventLog; }

    @Transactional
    public int generate(List<Long> activityIds, LocalDate startDate) throws Exception {
        AppUser user=current.get(); String apiKey=crypto.decrypt(current.settings().getOpenAiKeyEncrypted());
        if(apiKey.isBlank()) throw new IllegalStateException("Najpierw zapisz klucz OpenAI w ustawieniach.");
        List<Activity> selected=activities.findByIdInAndUserId(activityIds,user.getId());
        if(selected.isEmpty()) throw new IllegalStateException("Wybierz przynajmniej jedną aktywność.");
        JsonNode response=callOpenAi(apiKey, selected, startDate);
        JsonNode list=response.path("treningi");
        if(!list.isArray()||list.isEmpty()) throw new IllegalStateException("OpenAI nie zwróciło treningów w wymaganym formacie.");
        int count=0; for(JsonNode workout:list){ if(count==3) break; LocalDate date=startDate.plusDays(count*2L); String name=workout.path("name").asText("Trening "+(count+1)); workouts.save(new GeneratedWorkout(user,name,date,toZwo(workout,date))); count++; }
        for(Activity activity:selected) activity.setAnalyzed(true);
        eventLog.record(user, "WORKOUTS_GENERATED", "Wygenerowano " + count + " treningi");
        return count;
    }

    private JsonNode callOpenAi(String key,List<Activity> activities,LocalDate start) throws Exception {
        String activityJson=json.writeValueAsString(activities.stream().map(this::activityPayload).toList());
        String prompt="Jesteś trenerem kolarstwa. Na podstawie aktywności przygotuj DOKŁADNIE 3 treningi od "+start+". Zwróć wyłącznie JSON: {\"treningi\":[{\"name\":string,\"description\":string,\"steps\":[{\"intensity\":\"warmup|steady|cooldown\",\"duration_sec\":number,\"power\":number,\"power_low\":number,\"power_high\":number,\"cadence\":number}]}]}. Power to ułamek FTP. Aktywności: "+activityJson;
        String body=json.writeValueAsString(java.util.Map.of("model","gpt-4o","response_format",java.util.Map.of("type","json_object"),"messages",List.of(java.util.Map.of("role","user","content",prompt)),"temperature",0.7));
        HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions")).header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build();
        HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString()); if(response.statusCode()<200||response.statusCode()>=300) throw new IllegalStateException("OpenAI odrzuciło żądanie (HTTP "+response.statusCode()+").");
        return json.readTree(response.body()).path("choices").get(0).path("message").path("content").isTextual() ? json.readTree(json.readTree(response.body()).path("choices").get(0).path("message").path("content").asText()) : json.readTree(response.body());
    }

    private String toZwo(JsonNode workout,LocalDate date) throws Exception {
        Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument(); Element root=doc.createElement("workout_file");doc.appendChild(root); add(doc,root,"author","Trener AI");add(doc,root,"name",workout.path("name").asText());add(doc,root,"description","[Planowana data wykonania: "+date+"]\n\n"+workout.path("description").asText());add(doc,root,"sportType","bike"); Element node=doc.createElement("workout");root.appendChild(node);
        for(JsonNode step:workout.path("steps")){String intensity=step.path("intensity").asText("steady").toLowerCase();Element xml=doc.createElement(intensity.contains("warm")?"Warmup":intensity.contains("cool")?"Cooldown":"SteadyState");xml.setAttribute("Duration",String.valueOf(step.path("duration_sec").asInt(300)));if(xml.getTagName().equals("SteadyState"))xml.setAttribute("Power",String.valueOf(step.path("power").asDouble(.85)));else{xml.setAttribute("PowerLow",String.valueOf(step.path("power_low").asDouble(.5)));xml.setAttribute("PowerHigh",String.valueOf(step.path("power_high").asDouble(.7)));}xml.setAttribute("Cadence",String.valueOf(step.path("cadence").asInt(90)));node.appendChild(xml);}
        var transformer=TransformerFactory.newInstance().newTransformer();transformer.setOutputProperty(OutputKeys.INDENT,"yes");transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");var output=new java.io.StringWriter();transformer.transform(new DOMSource(doc),new StreamResult(output));return output.toString();
    }

    private java.util.Map<String, Object> activityPayload(Activity activity) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        put(payload, "name", activity.getName());
        put(payload, "description", activity.getDescription());
        put(payload, "type", activity.getActivityType());
        put(payload, "sport_type", activity.getSportType());
        put(payload, "date", activity.getActivityDate() == null ? null : activity.getActivityDate().toString());
        put(payload, "start_date_local", activity.getStartDateLocal() == null ? null : activity.getStartDateLocal().toString());
        put(payload, "distance_km", activity.getDistanceKm());
        put(payload, "moving_time_min", activity.getMovingTimeMin());
        put(payload, "elapsed_time_min", activity.getElapsedTimeMin());
        put(payload, "elevation_gain_m", activity.getElevationGainM());
        put(payload, "elevation_high_m", activity.getElevationHighM());
        put(payload, "elevation_low_m", activity.getElevationLowM());
        put(payload, "average_speed_kph", activity.getAverageSpeedKph());
        put(payload, "max_speed_kph", activity.getMaxSpeedKph());
        put(payload, "average_cadence", activity.getAverageCadence());
        put(payload, "average_watts", activity.getAverageWatts());
        put(payload, "weighted_average_watts", activity.getWeightedAverageWatts());
        put(payload, "max_watts", activity.getMaxWatts());
        put(payload, "kilojoules", activity.getKilojoules());
        put(payload, "average_heart_rate", activity.getAverageHeartRate());
        put(payload, "max_heart_rate", activity.getMaxHeartRate());
        put(payload, "calories", activity.getCalories());
        put(payload, "average_temperature_c", activity.getAverageTemperatureC());
        put(payload, "perceived_exertion", activity.getPerceivedExertion());
        put(payload, "relative_effort", activity.getSufferScore());
        put(payload, "workout_type", activity.getWorkoutType());
        put(payload, "device_name", activity.getDeviceName());
        put(payload, "power_from_device", activity.getDeviceWatts());
        put(payload, "trainer", activity.isTrainer());
        put(payload, "commute", activity.isCommute());
        put(payload, "manual", activity.isManual());
        return payload;
    }

    private void put(java.util.Map<String, Object> payload, String key, Object value) {
        if (value != null) payload.put(key, value);
    }

    private void add(Document doc,Element parent,String name,String value){Element child=doc.createElement(name);child.setTextContent(value);parent.appendChild(child);}
}
