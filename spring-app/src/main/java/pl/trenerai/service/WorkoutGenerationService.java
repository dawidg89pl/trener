package pl.trenerai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
    public enum TrainingGoal {
        IMPROVE_FTP("improve_ftp", "zwiększenie FTP", "Skup plan na progresji mocy progowej, interwałach sweet spot, progowych i VO₂max."),
        LONG_DISTANCE_ENDURANCE("long_distance_endurance", "wytrzymałość na długich dystansach", "Skup plan na dłuższej pracy tlenowej w Z2, tempie i odporności na zmęczenie podczas długich jazd.");

        private final String id;
        private final String label;
        private final String promptInstruction;

        TrainingGoal(String id, String label, String promptInstruction) {
            this.id = id;
            this.label = label;
            this.promptInstruction = promptInstruction;
        }

        public String label() { return label; }
        public String promptInstruction() { return promptInstruction; }
        public static TrainingGoal fromId(String id) {
            for (TrainingGoal goal : values()) if (goal.id.equals(id)) return goal;
            throw new IllegalArgumentException("Wybierz prawidłowy cel treningowy.");
        }
    }

    private final CurrentUserService current; private final UserSettingsRepository settings; private final ActivityRepository activities; private final GeneratedWorkoutRepository workouts; private final SecretCrypto crypto; private final UserEventService eventLog;
    private final ObjectMapper json = new ObjectMapper(); private final HttpClient http = HttpClient.newHttpClient();
    public WorkoutGenerationService(CurrentUserService current, UserSettingsRepository settings, ActivityRepository activities, GeneratedWorkoutRepository workouts, SecretCrypto crypto, UserEventService eventLog) { this.current=current;this.settings=settings;this.activities=activities;this.workouts=workouts;this.crypto=crypto;this.eventLog=eventLog; }

    @Transactional
    public int generate(List<Long> activityIds, LocalDate startDate, TrainingGoal trainingGoal) throws Exception {
        AppUser user=current.get(); UserSettings userSettings=current.settings(); String apiKey=crypto.decrypt(userSettings.getOpenAiKeyEncrypted());
        if(workouts.existsByUserIdAndDeletedFalse(user.getId())) throw new IllegalStateException("Masz już aktywny plan. Anuluj go przed wygenerowaniem nowego.");
        if(apiKey.isBlank()) throw new IllegalStateException("Najpierw zapisz klucz dostawcy AI w ustawieniach.");
        List<Activity> selected=activities.findByIdInAndUserId(activityIds,user.getId());
        if(selected.isEmpty()) throw new IllegalStateException("Wybierz przynajmniej jedną aktywność.");
        JsonNode response=callAi(apiKey, userSettings.getAiProvider(), userSettings.getOpenAiModel(), selected, startDate, trainingGoal, userSettings.getStravaFtp());
        JsonNode list=response.path("dni");
        if(!list.isArray()||list.size()!=7) throw new IllegalStateException("Dostawca AI nie zwrócił pełnego planu na 7 dni w wymaganym formacie.");
        int count=0;
        for(int dayIndex=0;dayIndex<7;dayIndex++){
            JsonNode workout=list.get(dayIndex);
            if(!"workout".equalsIgnoreCase(workout.path("type").asText())) continue;
            if(!workout.path("steps").isArray()||workout.path("steps").isEmpty()) throw new IllegalStateException("Dostawca AI zwrócił dzień treningowy bez wymaganych kroków.");
            LocalDate date=startDate.plusDays(dayIndex);
            String name=workout.path("name").asText("Trening "+(dayIndex+1));
            workouts.save(new GeneratedWorkout(user,name,date,toZwo(workout,date)));
            count++;
        }
        if(count==0) throw new IllegalStateException("Dostawca AI nie zaplanował żadnego treningu w okresie 7 dni.");
        for(Activity activity:selected) activity.setAnalyzed(true);
        eventLog.record(user, "WORKOUTS_GENERATED", "Wygenerowano 7-dniowy plan: " + count + " treningów");
        return count;
    }

    @Transactional
    public int cancelActivePlan() {
        AppUser user = current.get();
        List<GeneratedWorkout> activeWorkouts = workouts
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(user.getId());
        activeWorkouts.forEach(GeneratedWorkout::markDeleted);
        workouts.saveAll(activeWorkouts);
        if (!activeWorkouts.isEmpty()) {
            eventLog.record(user, "PLAN_CANCELLED", "Anulowano aktywny plan treningowy");
        }
        return activeWorkouts.size();
    }

    private JsonNode callAi(String key,String providerId,String model,List<Activity> activities,LocalDate start,TrainingGoal trainingGoal,Integer ftp) throws Exception {
        String activityJson=json.writeValueAsString(activities.stream().map(this::activityPayload).toList());
        String ftpContext=ftp==null?"FTP zawodnika nie jest dostępne w profilu Strava.":"Aktualne FTP zawodnika ze Stravy: "+ftp+" W.";
        String prompt="Jesteś trenerem kolarstwa. Ułóż plan na DOKŁADNIE 7 kolejnych dni, od "+start+" do "+start.plusDays(6)+". Dla każdego dnia zdecyduj, czy zawodnik powinien wykonać trening (type=workout), czy odpoczywać (type=rest). Liczbę treningów dobierz samodzielnie na podstawie obciążenia, zmęczenia i charakteru analizowanych aktywności; nie wypełniaj każdego dnia treningiem, jeśli regeneracja będzie korzystniejsza. Główny cel planu: "+trainingGoal.label()+". "+trainingGoal.promptInstruction()+" "+ftpContext+" Zwróć wyłącznie JSON zawierający dokładnie 7 elementów w kolejności dni: {\"dni\":[{\"day\":1,\"type\":\"workout|rest\",\"name\":string,\"description\":string,\"steps\":[{\"intensity\":\"warmup|steady|cooldown\",\"duration_sec\":number,\"power\":number,\"power_low\":number,\"power_high\":number,\"cadence\":number}]}]}. Dla dnia odpoczynku zwróć pustą tablicę steps. Power, power_low i power_high podawaj jako ułamek FTP. W opisie dnia treningowego uzasadnij, jak realizuje wybrany cel i uwzględnia regenerację po pozostałych dniach. Aktywności: "+activityJson;
        AiProviderCatalog.Provider provider=AiProviderCatalog.provider(providerId);
        java.util.Map<String,Object> requestBody=new LinkedHashMap<>();
        requestBody.put("model",model);
        requestBody.put("response_format",java.util.Map.of("type","json_object"));
        requestBody.put("messages",List.of(java.util.Map.of("role","user","content",prompt)));
        if(!"gemini".equals(providerId)) requestBody.put("temperature",0.7);
        String body=json.writeValueAsString(requestBody);
        HttpRequest request=HttpRequest.newBuilder(URI.create(provider.chatCompletionsUrl())).header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build();
        HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString()); if(response.statusCode()<200||response.statusCode()>=300) throw new IllegalStateException(provider.label()+" odrzucił żądanie (HTTP "+response.statusCode()+"): "+apiErrorMessage(response.body()));

        return json.readTree(response.body()).path("choices").get(0).path("message").path("content").isTextual() ? json.readTree(json.readTree(response.body()).path("choices").get(0).path("message").path("content").asText()) : json.readTree(response.body());
    }

    private String apiErrorMessage(String responseBody) {
        try {
            String message=json.readTree(responseBody).path("error").path("message").asText();
            if(!message.isBlank()) return message.length()>400?message.substring(0,400)+"…":message;
        } catch(Exception ignored) {}
        return "Brak szczegółów błędu po stronie dostawcy.";
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
