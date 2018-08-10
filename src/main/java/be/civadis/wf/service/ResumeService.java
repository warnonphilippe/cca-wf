package be.civadis.wf.service;

import org.springframework.stereotype.Service;

/**
 * Service pour la demo du processus workflow hireProcessWithJpa
 */
@Service
public class ResumeService {

    // voir https://www.activiti.org/userguide/index.html#springSpringBoot

    public void storeResume(String applicantName) {
        System.out.println("Storing "+ applicantName +"'s resume ...");
    }

    public void sendMail(String email, String type){
        System.out.println("Send "+ type + " mail to " + email);
    }

}
