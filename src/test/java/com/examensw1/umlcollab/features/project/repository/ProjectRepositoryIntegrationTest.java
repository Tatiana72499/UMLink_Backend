package com.examensw1.umlcollab.features.project.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.examensw1.umlcollab.features.project.model.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:umlink;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProjectRepositoryIntegrationTest {

    @Autowired
    private ProjectRepository repository;

    @Test
    void debePersistirProyectoConVersionInicial() {
        Project project = new Project();
        project.setName("Biblioteca");
        project.setDescription("Modelo UML");
        project.setOwnerName("Tatiana");

        Project saved = repository.saveAndFlush(project);

        assertNotNull(saved.getId());
        assertEquals(0L, saved.getVersion());
        assertEquals("Biblioteca", repository.findById(saved.getId()).orElseThrow().getName());
    }
}
