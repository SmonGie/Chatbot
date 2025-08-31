package chatbot.backend.Host.Controllers;

import chatbot.backend.Application.Services.ConversationService;
import chatbot.backend.Domain.Entities.Conversation;
import chatbot.backend.Domain.Entities.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/start")
    public ResponseEntity<Conversation> startConversation(){
        Conversation conversation = conversationService.startConversation();
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Conversation> sendMessage(@PathVariable String conversationId,@RequestBody Message message){
        Conversation messages = conversationService.sendMessage(conversationId, message);
        return ResponseEntity.ok(messages);
    }
}
