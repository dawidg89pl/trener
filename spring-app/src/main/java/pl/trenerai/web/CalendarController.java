package pl.trenerai.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CalendarController {
    @GetMapping("/calendar")
    String calendar() {
        return "redirect:/#calendar";
    }
}
