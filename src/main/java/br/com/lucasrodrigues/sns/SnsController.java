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
