package es.merida.tfg.gestion_proyectos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class GestionProyectosApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionProyectosApplication.class, args);
        System.out.println("Aplicación iniciada");
    }
}
