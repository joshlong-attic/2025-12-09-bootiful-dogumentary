package com.example.adoptions.vet;

import com.example.adoptions.adoptions.DogAdoptedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class Dogtor {

    @ApplicationModuleListener
    void on(DogAdoptedEvent e) throws Exception {
        Thread.sleep(5000);
        IO.println("scheduling " + e + " for an appointment at the Dogtor");
    }
}
