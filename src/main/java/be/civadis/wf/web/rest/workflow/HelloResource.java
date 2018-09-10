package be.civadis.wf.web.rest.workflow;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class HelloResource {

    @GetMapping(value = "/hello")
    public ResponseEntity<Boolean> hello(){
        return ResponseEntity.ok(true);
    }

}
