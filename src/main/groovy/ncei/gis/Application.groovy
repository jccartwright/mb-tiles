package ncei.gis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
class Application implements CommandLineRunner {

    @Autowired
    MultibeamRepository repository

    @Autowired
    MultibeamService service

    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Override
    void run(String... strings) throws Exception {
        println 'Hello World!'

        List surveys = repository.getSurveys()

    }




}