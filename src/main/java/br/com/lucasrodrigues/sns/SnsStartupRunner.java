package br.com.lucasrodrigues.sns;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SnsStartupRunner implements CommandLineRunner {
    private final SnsSubscriptionService snsSubscriptionService;

    public SnsStartupRunner(SnsSubscriptionService snsSubscriptionService) {
        this.snsSubscriptionService = snsSubscriptionService;
    }

    @Override
    public void run(String... args) {
        snsSubscriptionService.subscribeToTopic();
    }
}
