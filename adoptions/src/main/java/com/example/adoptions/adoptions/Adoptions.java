package com.example.adoptions.adoptions;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;

@Configuration
class Adoptions {
}

@Controller
@ResponseBody
class MeController {

    @GetMapping("/me")
    Map<String, String> me(Principal principal) {
        return Map.of("name", principal.getName());
    }
}

@Controller
@ResponseBody
class DogsController {

    private final DogRepository repository;

    DogsController(DogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/dogs")
    Collection<Dog> dogs() {
        return this.repository.findAll();
    }
}

@Transactional
@Controller
@ResponseBody
class AdoptionsController {

    private final DogRepository repository;
    private final ApplicationEventPublisher applicationEventPublisher;

    AdoptionsController(DogRepository repository, ApplicationEventPublisher applicationEventPublisher) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostMapping("/dogs/{dogId}/adoptions")
    void adopt(@PathVariable int dogId, @RequestParam String owner) {

        this.repository.findById(dogId).ifPresent(dog -> {
            var updated = this.repository.save(new Dog(
                    dog.id(), owner, dog.description(), dog.name()
            ));
            IO.println("adopted " + updated);
            this.applicationEventPublisher.publishEvent(
                    new DogAdoptedEvent(dog.id()));
        });

    }

}

record Dog(@Id int id, String owner, String description, String name) {
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}