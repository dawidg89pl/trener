package pl.trenerai.service;

import org.springframework.stereotype.Service;
import pl.trenerai.model.AppUser;
import pl.trenerai.model.UserEvent;
import pl.trenerai.repository.UserEventRepository;

@Service
public class UserEventService {
    private final UserEventRepository events;

    public UserEventService(UserEventRepository events) {
        this.events = events;
    }

    public void record(AppUser user, String type, String description) {
        events.save(new UserEvent(user, type, description));
    }
}
