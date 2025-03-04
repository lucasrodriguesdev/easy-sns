#### 1 Configurar docker-compose.yml para subir localstack local
```
https://github.com/lucasrodriguesdev/easy-sns/blob/master/docker-compose.yml
```
Subir com comando docker-compose up
esperar mensagem *Ready.* que indica que tudo correu bem.

#### 2 Acessar https://app.localstack.cloud/dashboard
No primeiro acesso sera necessário criar conta/logar. Ao acessar o dash no menu lateral temos opção de status
De acordo com nosso docker-compose.yml temos que ter 3 serviços com status avaliable: s3, SNS, SQS

#### 3 Criando um tópico

Selecionar o SNS e depois a opção Create topic, dar um nome e clicar em Submit
deverá ser criado um tópico e vai exibir na lista algo como:
```arn:aws:sns:us-east-1:000000000000:meu-topico```

#### 4 Configurando projeto
Projeto pode ser criado em https://start.spring.io/
```
Project > Maven
Language > Java
Spring boot > 3.4.3
Project metadata
...
Dependencies > Spring WEB
```
Generate.

Abrir na IDE e adicionar no pom.xml as configurações para AWS SNS

```
	<dependencies>
	    ...
		<dependency>
			<groupId>io.awspring.cloud</groupId>
			<artifactId>spring-cloud-aws-starter-sns</artifactId>
		</dependency>
	</dependencies>
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>io.awspring.cloud</groupId>
				<artifactId>spring-cloud-aws-dependencies</artifactId>
				<version>3.3.0</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
```
Baixar as dependencias.

#### 5 Criando as classes
Primeiro precisamos criar a classe de configuração:
https://github.com/lucasrodriguesdev/easy-sns/blob/master/src/main/java/br/com/lucasrodrigues/sns/SNSConfig.java
```
package br.com.lucasrodrigues.sns;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

import java.net.URI;

@Configuration
public class SNSConfig {

    @Bean
    public SnsAsyncClient snsAsyncClient(){
        return SnsAsyncClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")
                ))
                .build();
    }
}
```

Depois criamos uma classe que sera responsavel por se inscrever no tópico:
https://github.com/lucasrodriguesdev/easy-sns/blob/master/src/main/java/br/com/lucasrodrigues/sns/SnsSubscriptionService.java
```
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
```

Agora para facilitar criamos uma classe que ira executar este service de inscrição ao rodar o projeto:
https://github.com/lucasrodriguesdev/easy-sns/blob/master/src/main/java/br/com/lucasrodrigues/sns/SnsStartupRunner.java
```
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
```


Só falta criar um *endpoint* que será usado para receber a mensagem, conforme configuramos em *SnsSubscriptionService*, então vamos criar um controller:
https://github.com/lucasrodriguesdev/easy-sns/blob/master/src/main/java/br/com/lucasrodrigues/sns/SnsController.java
```
package br.com.lucasrodrigues.sns;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sns")
public class SnsController {

    @PostMapping("/notification")
    public ResponseEntity<Void> handleSnsNotification(@RequestBody String message) {
        // Exibe a mensagem recebida no log para visualização
        System.out.println("Mensagem recebida no SNS: " + message);

        // Aqui você pode adicionar a lógica de enviar e-mails ou fazer outro tipo de processamento
        return ResponseEntity.ok().build();
    }
}
```


#### 6 Testando
podemos subir o projeto com comando 
`mvn spring-boot:run`
e esperamos o resultado:
```
Mensagem recebida no SNS: {"Type": "SubscriptionConfirmation", "MessageId": "ad6e2ed1-d370-4b0d-becf-3677312ce4ec", "TopicArn": "arn:aws:sns:us-east-1:000000000000:meu-topico", "Message": "You have chosen to subscribe to the topic arn:aws:sns:us-east-1:000000000000:meu-topico.\nTo confirm the subscription, visit the SubscribeURL included in this message.", "Timestamp": "2025-03-04T18:50:12.436Z", "Token": "75732d656173742d312f2ea01a0d2ea01a0d2ea01a0d2ea01a0d2ea01a0d2ea0", "SubscribeURL": "http://localhost.localstack.cloud:4566/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-east-1:000000000000:meu-topico&Token=75732d656173742d312f2ea01a0d2ea01a0d2ea01a0d2ea01a0d2ea01a0d2ea0", "SignatureVersion": "1", "Signature": "TyPG/KOzS8S1PkUW33tdrbGGVZ1gx1F+2O7xBVuU9Viy/pq5LsDxZBjz5ALrPjqVnjui930dPORtS+abMMRvYwUv569S5RzEXp/yXCjj8RS00prdNdcFxWTSuFH6Wkhm+1foxiFwvzBIGtMrr0UtdpL+IzoT12ED4jLjDzm5aN+TiHc+z13kQwW7Ggbi5AL+SlRLYRd/v5gLWqjyW0qsrg8PJ6utUd4vtTMnwtY2F5ifBBMdmFLazfjiaEdjh0J1VTmSD6GkEF+Kr2GQOfQyaQIBskTntYij1zT8tso7vvGIHIIrf30RAzD7T9eFP9eaClLm6LSOwSfgmiCWCMUzkQ==", "SigningCertURL": "http://localhost.localstack.cloud:4566/_aws/sns/SimpleNotificationService-6c6f63616c737461636b69736e696365.pem"}
```
o log indica que para confirmar inscrição no tópico precisamos acessar a URL 
`"SubscribeURL": "http://localhost.localstack.cloud:4566/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-east-1:000000000000:meu-topico&Token=75732d656173742d312f2ea01a0d2ea01a0d2ea01a0d2ea01a0d2ea01a0d2ea0", `

Ao colar URL no navegador temos uma mensagem como:
```
This XML file does not appear to have any style information associated with it. The document tree is shown below.
<ConfirmSubscriptionResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
<ConfirmSubscriptionResult>
<SubscriptionArn>arn:aws:sns:us-east-1:000000000000:meu-topico:449647d6-511a-4dce-9440-967188b4efc9</SubscriptionArn>
</ConfirmSubscriptionResult>
<ResponseMetadata>
<RequestId>76c52902-7b73-492e-999b-c4f0dac2d836</RequestId>
</ResponseMetadata>
</ConfirmSubscriptionResponse>
```

Agora podemos testar via localstack uma Publish message no topico, e ver se nosso controller estará printando a mensagem.

Para isso acessar
https://app.localstack.cloud/inst/default/resources/sns
Selecionar seu topico e clicar em `publish message`

No form podemos preencher apenas o campo obrigatório `message` e clicar em `publish`

Teremos uma alerta de mensagem publicada com sucesso e voltando nos logs do nosso projeto, podemos verificar que nosso controller recebeu a mensagem:

```
Mensagem recebida no SNS: {"Type": "Notification", "MessageId": "e5430e4a-3da4-4d47-86c5-3715f603da61", "TopicArn": "arn:aws:sns:us-east-1:000000000000:meu-topico", "Message": "TESTE 123", "Timestamp": "2025-03-04T19:31:35.774Z", "UnsubscribeURL": "http://localhost.localstack.cloud:4566/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:000000000000:meu-topico:449647d6-511a-4dce-9440-967188b4efc9", "SignatureVersion": "1", "Signature": "Yanr7llFG4InoCbP/pBSqd4A7jlIEF0QSDQIlq2H4EQqGcKHYjGw/FA11XnLIdTIR5qTTLBDJO0MYnDTCsTf2zyZC6UDDo+0Ex2zmXOIDYCVLDH3WaEwrXKgUDjMnP1tdt4nciNGUaxncJ3FgDQQBXJa1gHHVV+BU6i/8J4lQo2t1GQ8Em6GH7LTzekF4qiPAvHXu7wKIT3ApdWBhOBw6mD7oeZmiuXFOaLuhulpDdW0MW7H5jXZsdj+wwi7ym/UH4BBb2JfWcRZaelfT0XZoLaX/+Uqk3i/E7Zz9diTZwbfF5JsQJVt45l1aoPcfCZnC1CbGKH0XKM8LMFyulqYAA==", "SigningCertURL": "http://localhost.localstack.cloud:4566/_aws/sns/SimpleNotificationService-6c6f63616c737461636b69736e696365.pem"}
```
Ele exibe um JSON que podemos tratar para pegar apenas
`"Message": "TESTE 123"`

Com isso concluimos que esta funcionando nossa configuração, agora podemos seguir para implementações de acordo com necessidade.
