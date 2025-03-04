package br.com.lucasrodrigues.sns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import java.util.concurrent.CompletableFuture;

@Service
public class SnsSubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(SnsSubscriptionService.class);
    private final SnsAsyncClient snsAsyncClient;

    public SnsSubscriptionService(SnsAsyncClient snsAsyncClient) {
        this.snsAsyncClient = snsAsyncClient;
    }

    public void subscribeToTopic() {
        //topico que criamos mais cedo
        String topicArn = "arn:aws:sns:us-east-1:000000000000:meu-topico";
        String protocol = "http"; // "http", "email", "sms", etc.
        String endpoint = "http://host.docker.internal:8080/sns/notification";

        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol(protocol)
                .endpoint(endpoint)
                .returnSubscriptionArn(true)
                .build();

        CompletableFuture<SubscribeResponse> response =
                snsAsyncClient.subscribe(request);

        response.whenComplete((resp, error) -> {
            if (error != null) {
                log.error("Erro ao assinar o tópico SNS: {}",
                        error.getMessage());
            } else {
                log.info("Assinatura bem-sucedida! Subscription ARN: {}",
                        resp.subscriptionArn());
            }
        });
    }
}
