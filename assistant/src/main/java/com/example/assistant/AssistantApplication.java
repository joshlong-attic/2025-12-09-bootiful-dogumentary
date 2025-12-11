package com.example.assistant;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.security.Principal;
import java.util.List;

@SpringBootApplication
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor
                .builder(vectorStore)
                .build();
    }


    @Bean
    PromptChatMemoryAdvisor promptChatMemoryAdvisor(DataSource dataSource) {
        var jdbc = JdbcChatMemoryRepository.builder().dataSource(dataSource).build();
        var mwa = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(jdbc)
                .build();
        return PromptChatMemoryAdvisor
                .builder(mwa)
                .build();
    }

}


record Dog(@Id int id, String owner, String description, String name) {
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record DogAdoptionSuggestion(int id, String name, String description) {
}

@Controller
@ResponseBody
@ImportRuntimeHints(AssistantController.Hints.class)
class AssistantController {

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(DogAdoptionSuggestion.class, MemberCategory.values());
        }
    }

    private final ChatClient ai;

    AssistantController(
            VectorStore vectorStore, DogRepository repository,
            List<McpSyncClient> clients,
            QuestionAnswerAdvisor qa,
            PromptChatMemoryAdvisor memory,
            ChatClient.Builder ai) {

            repository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                        dog.id(), dog.name(), dog.description()
                ));
                vectorStore.add(List.of(dogument));
            });

        var system = """
                You are an AI powered assistant to help people adopt a dog from the adoption\s
                agency named Pooch Palace with locations in Antwerp, Seoul, Tokyo, Singapore, Paris,\s
                Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available\s
                will be presented below. If there is no information, then return a polite response suggesting we\s
                don't have any dogs available.
                """;

        IO.println("***** MCP Tools: %s".formatted(clients.size()));
        this.ai = ai
                .defaultSystem(system)
                .defaultToolCallbacks(SyncMcpToolCallbackProvider.syncToolCallbacks(clients))
                .defaultAdvisors(memory, qa)
                .build();
    }

    @GetMapping("/ask")
    String question(@RequestParam String question, Principal principal) {
        return this.ai
                .prompt(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, principal != null ? principal.getName() : "Josh"))
                .call()
                .content();
//                .entity(DogAdoptionSuggestion.class);
    }

}