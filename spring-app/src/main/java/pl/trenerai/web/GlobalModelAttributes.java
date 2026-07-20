package pl.trenerai.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
    private final ObjectProvider<BuildProperties> buildProperties;

    public GlobalModelAttributes(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @ModelAttribute("appVersion")
    String appVersion() {
        var build = buildProperties.getIfAvailable();
        return build != null ? build.getVersion() : "dev";
    }
}
