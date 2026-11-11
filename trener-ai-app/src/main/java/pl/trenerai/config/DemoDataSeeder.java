package pl.trenerai.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.trenerai.model.Activity;
import pl.trenerai.model.AppUser;
import pl.trenerai.model.GeneratedWorkout;
import pl.trenerai.model.UserEvent;
import pl.trenerai.repository.ActivityRepository;
import pl.trenerai.repository.AppUserRepository;
import pl.trenerai.repository.GeneratedWorkoutRepository;
import pl.trenerai.repository.UserEventRepository;
import pl.trenerai.service.UserBootstrapService;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_EMAIL = "demo@trenerai.pl";
    private static final String DEMO_PASSWORD = "demo";

    private final UserBootstrapService bootstrap;
    private final AppUserRepository users;
    private final ActivityRepository activities;
    private final GeneratedWorkoutRepository workouts;
    private final UserEventRepository events;

    public DemoDataSeeder(UserBootstrapService bootstrap, AppUserRepository users,
                          ActivityRepository activities, GeneratedWorkoutRepository workouts,
                          UserEventRepository events) {
        this.bootstrap = bootstrap;
        this.users = users;
        this.activities = activities;
        this.workouts = workouts;
        this.events = events;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrap.createIfMissing(DEMO_EMAIL, DEMO_PASSWORD);
        AppUser user = users.findByEmailIgnoreCase(DEMO_EMAIL).orElseThrow();

        if (!activities.findByUserIdOrderByActivityDateDesc(user.getId()).isEmpty()) {
            log.info("Demo: dane już istnieją, pomijam seed");
            return;
        }

        log.info("Demo: tworzę przykładowe dane dla {}", DEMO_EMAIL);
        seedActivities(user);
        seedWorkouts(user);
        seedEvents(user);
        log.info("Demo: seed zakończony");
    }

    private void seedActivities(AppUser user) {
        Random rng = new Random(42);
        LocalDate today = LocalDate.now();
        long stravaId = 10_000_000_000L;

        record Ride(String name, int daysAgo, double km, int minutes, double elev,
                     double avgW, double nrmW, int maxW, double avgHr, double maxHr,
                     double cadence, boolean trainer) {}

        List<Ride> rides = List.of(
            new Ride("Poranny trening Zwift – SST", 1, 32.5, 62, 45, 198.0, 212.0, 340, 148.0, 167.0, 88.0, true),
            new Ride("Jazda rekreacyjna – Dolina Raby", 2, 48.2, 105, 320, 142.0, 155.0, 280, 132.0, 158.0, 82.0, false),
            new Ride("Interwały VO2max na trenażerze", 4, 28.1, 55, 30, 225.0, 248.0, 420, 162.0, 185.0, 92.0, true),
            new Ride("Tempo ride – Kraków-Wieliczka", 5, 42.0, 88, 180, 175.0, 188.0, 310, 145.0, 172.0, 85.0, false),
            new Ride("Odzyskiwanie – lekka jazda", 7, 25.3, 58, 80, 110.0, 118.0, 190, 118.0, 138.0, 78.0, false),
            new Ride("Weekendowa wyprawa – Turbacz", 8, 92.4, 245, 1420, 168.0, 185.0, 380, 142.0, 178.0, 80.0, false),
            new Ride("Sweet Spot 2x20 – Zwift", 10, 35.8, 68, 40, 205.0, 218.0, 350, 152.0, 170.0, 90.0, true),
            new Ride("Poranne endurance – Ojcowski PN", 12, 55.6, 120, 450, 148.0, 160.0, 265, 135.0, 155.0, 83.0, false),
            new Ride("Interwały 4x8 FTP – trenażer", 14, 30.2, 60, 35, 215.0, 232.0, 395, 158.0, 182.0, 91.0, true),
            new Ride("Jazda grupowa – Pętla Niepołomice", 15, 68.5, 142, 280, 172.0, 190.0, 520, 148.0, 188.0, 86.0, false),
            new Ride("Recovery spin", 17, 18.5, 40, 25, 95.0, 102.0, 160, 112.0, 128.0, 76.0, true),
            new Ride("Długa baza – Rabka-Zdrój", 19, 105.2, 280, 1650, 155.0, 172.0, 360, 138.0, 175.0, 79.0, false),
            new Ride("Threshold 3x15 – Zwift Race", 21, 38.4, 72, 50, 210.0, 228.0, 410, 155.0, 180.0, 89.0, true),
            new Ride("Commute – dojazd do biura", 22, 12.8, 28, 45, 130.0, 140.0, 220, 125.0, 145.0, 80.0, false),
            new Ride("Tempo + Sprints – Puszcza Niepołomicka", 24, 58.3, 125, 350, 178.0, 195.0, 680, 146.0, 192.0, 84.0, false),
            new Ride("Endurance Zwift – Watopia Flat", 26, 45.0, 95, 120, 145.0, 155.0, 240, 130.0, 148.0, 86.0, true),
            new Ride("Górska wyprawa – Babia Góra", 28, 118.5, 310, 2100, 162.0, 180.0, 390, 140.0, 182.0, 77.0, false)
        );

        for (Ride r : rides) {
            Activity a = new Activity(user, stravaId++);
            double avgSpeed = r.km / (r.minutes / 60.0);
            double maxSpeed = avgSpeed * (1.15 + rng.nextDouble() * 0.15);
            double kj = r.avgW * r.minutes * 60.0 / 1000.0;
            double cal = kj * 1.05;
            LocalDate date = today.minusDays(r.daysAgo);
            a.update(r.name, null, "Ride", "MountainBikeRide", date,
                    date.atTime(7 + rng.nextInt(10), rng.nextInt(60)),
                    r.km, r.minutes, r.minutes + 5 + rng.nextInt(15), r.elev,
                    r.elev * 0.8, 200.0,
                    avgSpeed, maxSpeed, r.cadence,
                    r.avgW, r.nrmW, r.maxW, kj,
                    r.avgHr, r.maxHr, cal, 12.0 + rng.nextDouble() * 10,
                    null, rng.nextInt(40) + 20,
                    null, r.trainer ? "Zwift Hub" : "Garmin Edge 840", !r.trainer,
                    r.trainer, false, false);
            if (r.daysAgo > 14) {
                a.setAnalyzed(true);
            }
            activities.save(a);
        }
    }

    private void seedWorkouts(AppUser user) {
        LocalDate start = LocalDate.now().plusDays(1);

        record Plan(String name, int dayOffset, String zwo) {}

        List<Plan> plans = List.of(
            new Plan("Regeneracja – łatwe kręcenie", 0,
                zwo("Regeneracja", List.of(seg("Rozgrzewka", 600, 0.45), seg("Łatwe kręcenie", 2400, 0.50), seg("Schładzanie", 300, 0.40)))),
            new Plan("Sweet Spot 2×20 min", 1,
                zwo("Sweet Spot", List.of(seg("Rozgrzewka", 600, 0.55), seg("SST blok 1", 1200, 0.88), seg("Odpoczynek", 300, 0.50), seg("SST blok 2", 1200, 0.88), seg("Schładzanie", 300, 0.45)))),
            new Plan("Endurance 90 min", 2,
                zwo("Endurance", List.of(seg("Rozgrzewka", 600, 0.50), seg("Endurance", 4800, 0.65), seg("Schładzanie", 300, 0.40)))),
            new Plan("Dzień wolny – odpoczynek", 3, null),
            new Plan("VO2max 5×4 min", 4,
                zwo("VO2max", List.of(seg("Rozgrzewka", 900, 0.55),
                    seg("VO2max 1", 240, 1.15), seg("Odpoczynek", 240, 0.45),
                    seg("VO2max 2", 240, 1.15), seg("Odpoczynek", 240, 0.45),
                    seg("VO2max 3", 240, 1.15), seg("Odpoczynek", 240, 0.45),
                    seg("VO2max 4", 240, 1.15), seg("Odpoczynek", 240, 0.45),
                    seg("VO2max 5", 240, 1.15), seg("Schładzanie", 600, 0.40)))),
            new Plan("Tempo 3×15 min", 5,
                zwo("Tempo", List.of(seg("Rozgrzewka", 600, 0.50), seg("Tempo 1", 900, 0.80), seg("Odpoczynek", 300, 0.50), seg("Tempo 2", 900, 0.80), seg("Odpoczynek", 300, 0.50), seg("Tempo 3", 900, 0.80), seg("Schładzanie", 300, 0.45)))),
            new Plan("Długa baza – 3 h endurance", 6,
                zwo("Baza", List.of(seg("Rozgrzewka", 600, 0.50), seg("Baza", 9600, 0.62), seg("Schładzanie", 600, 0.40))))
        );

        for (Plan p : plans) {
            if (p.zwo != null) {
                workouts.save(new GeneratedWorkout(user, p.name, start.plusDays(p.dayOffset), p.zwo));
            }
        }
    }

    private void seedEvents(AppUser user) {
        Instant now = Instant.now();
        List<EventDef> defs = List.of(
            new EventDef("STRAVA_SYNC", "Zsynchronizowano 17 aktywności ze Stravy", 25),
            new EventDef("STRAVA_SYNC", "Zsynchronizowano 3 nowe aktywności ze Stravy", 12),
            new EventDef("WORKOUTS_GENERATED", "Wygenerowano plan treningowy na 7 dni (6 treningów)", 11),
            new EventDef("STRAVA_SYNC", "Zsynchronizowano 2 nowe aktywności ze Stravy", 3),
            new EventDef("STRAVA_SYNC", "Zsynchronizowano 1 nową aktywność ze Stravy", 1)
        );
        for (EventDef e : defs) {
            UserEvent event = new UserEvent(user, e.type, e.desc);
            events.save(event);
            events.flush();
            events.findById(event.getId()).ifPresent(saved -> {
                try {
                    var field = UserEvent.class.getDeclaredField("createdAt");
                    field.setAccessible(true);
                    field.set(saved, now.minus(e.daysAgo, ChronoUnit.DAYS));
                    events.save(saved);
                } catch (ReflectiveOperationException ignored) {}
            });
        }
    }

    private record EventDef(String type, String desc, int daysAgo) {}

    private record Segment(String name, int duration, double ftpFraction) {}

    private static Segment seg(String name, int duration, double ftp) {
        return new Segment(name, duration, ftp);
    }

    private static String zwo(String name, List<Segment> segments) {
        StringBuilder sb = new StringBuilder();
        sb.append("<workout_file>\n<name>").append(name).append("</name>\n");
        sb.append("<author>Trener AI</author>\n<sportType>bike</sportType>\n<workout>\n");
        for (Segment s : segments) {
            sb.append("  <SteadyState Duration=\"").append(s.duration)
              .append("\" Power=\"").append(String.format("%.2f", s.ftpFraction))
              .append("\" />\n");
        }
        sb.append("</workout>\n</workout_file>");
        return sb.toString();
    }
}
