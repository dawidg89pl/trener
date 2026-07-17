package pl.trenerai.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "strava_activity_id"}))
public class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private AppUser user;
    @Column(name = "strava_activity_id", nullable = false) private Long stravaActivityId;
    private String name;
    @Column(columnDefinition = "TEXT") private String description;
    private String activityType;
    private String sportType;
    private LocalDate activityDate;
    private LocalDateTime startDateLocal;
    private double distanceKm;
    private int movingTimeMin;
    private int elapsedTimeMin;
    private double elevationGainM;
    private Double elevationHighM;
    private Double elevationLowM;
    private Double averageSpeedKph;
    private Double maxSpeedKph;
    private Double averageCadence;
    private Double averageWatts;
    private Double weightedAverageWatts;
    private Integer maxWatts;
    private Double kilojoules;
    private Double averageHeartRate;
    private Double maxHeartRate;
    private Double calories;
    private Double averageTemperatureC;
    private Integer perceivedExertion;
    private Integer sufferScore;
    private Integer workoutType;
    private String deviceName;
    private Boolean deviceWatts;
    private boolean trainer;
    private boolean commute;
    private boolean manual;
    private boolean analyzed;

    protected Activity() {}

    public Activity(AppUser user, Long remoteId) {
        this.user = user;
        this.stravaActivityId = remoteId;
    }

    public void update(
            String name, String description, String activityType, String sportType, LocalDate activityDate,
            LocalDateTime startDateLocal, double distanceKm, int movingTimeMin, int elapsedTimeMin,
            double elevationGainM, Double elevationHighM, Double elevationLowM,
            Double averageSpeedKph, Double maxSpeedKph, Double averageCadence,
            Double averageWatts, Double weightedAverageWatts, Integer maxWatts, Double kilojoules,
            Double averageHeartRate, Double maxHeartRate, Double calories, Double averageTemperatureC,
            Integer perceivedExertion, Integer sufferScore,
            Integer workoutType, String deviceName, Boolean deviceWatts,
            boolean trainer, boolean commute, boolean manual) {
        this.name = name;
        this.description = description;
        this.activityType = activityType;
        this.sportType = sportType;
        this.activityDate = activityDate;
        this.startDateLocal = startDateLocal;
        this.distanceKm = distanceKm;
        this.movingTimeMin = movingTimeMin;
        this.elapsedTimeMin = elapsedTimeMin;
        this.elevationGainM = elevationGainM;
        this.elevationHighM = elevationHighM;
        this.elevationLowM = elevationLowM;
        this.averageSpeedKph = averageSpeedKph;
        this.maxSpeedKph = maxSpeedKph;
        this.averageCadence = averageCadence;
        this.averageWatts = averageWatts;
        this.weightedAverageWatts = weightedAverageWatts;
        this.maxWatts = maxWatts;
        this.kilojoules = kilojoules;
        this.averageHeartRate = averageHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.calories = calories;
        this.averageTemperatureC = averageTemperatureC;
        this.perceivedExertion = perceivedExertion;
        this.sufferScore = sufferScore;
        this.workoutType = workoutType;
        this.deviceName = deviceName;
        this.deviceWatts = deviceWatts;
        this.trainer = trainer;
        this.commute = commute;
        this.manual = manual;
    }

    public Long getId() { return id; }
    public Long getStravaActivityId() { return stravaActivityId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getActivityType() { return activityType; }
    public String getSportType() { return sportType; }
    public LocalDate getActivityDate() { return activityDate; }
    public LocalDateTime getStartDateLocal() { return startDateLocal; }
    public double getDistanceKm() { return distanceKm; }
    public int getMovingTimeMin() { return movingTimeMin; }
    public int getElapsedTimeMin() { return elapsedTimeMin; }
    public double getElevationGainM() { return elevationGainM; }
    public Double getElevationHighM() { return elevationHighM; }
    public Double getElevationLowM() { return elevationLowM; }
    public Double getAverageSpeedKph() { return averageSpeedKph; }
    public Double getMaxSpeedKph() { return maxSpeedKph; }
    public Double getAverageCadence() { return averageCadence; }
    public Double getAverageWatts() { return averageWatts; }
    public Double getWeightedAverageWatts() { return weightedAverageWatts; }
    public Integer getMaxWatts() { return maxWatts; }
    public Double getKilojoules() { return kilojoules; }
    public Double getAverageHeartRate() { return averageHeartRate; }
    public Double getMaxHeartRate() { return maxHeartRate; }
    public Double getCalories() { return calories; }
    public Double getAverageTemperatureC() { return averageTemperatureC; }
    public Integer getPerceivedExertion() { return perceivedExertion; }
    public Integer getSufferScore() { return sufferScore; }
    public Integer getWorkoutType() { return workoutType; }
    public String getDeviceName() { return deviceName; }
    public Boolean getDeviceWatts() { return deviceWatts; }
    public boolean isTrainer() { return trainer; }
    public boolean isCommute() { return commute; }
    public boolean isManual() { return manual; }
    public boolean isAnalyzed() { return analyzed; }
    public void setAnalyzed(boolean value) { analyzed = value; }
}
