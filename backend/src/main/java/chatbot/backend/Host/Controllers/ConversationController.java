package chatbot.backend.Host.Controllers;

import chatbot.backend.Application.Services.ConversationService;
import chatbot.backend.Domain.Entities.Conversation;
import chatbot.backend.Domain.Entities.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    @Autowired
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

    @GetMapping()
    public ResponseEntity<Conversation> getMessages(@RequestParam String conversationId){
        Conversation conversation = conversationService.getConversationById(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conversation);
    }

}
