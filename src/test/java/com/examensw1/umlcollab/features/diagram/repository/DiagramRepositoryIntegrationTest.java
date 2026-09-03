package com.examensw1.umlcollab.features.diagram.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.examensw1.umlcollab.features.diagram.model.Diagram;
import com.examensw1.umlcollab.features.project.model.Project;
import com.examensw1.umlcollab.features.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DiagramRepositoryIntegrationTest {

    @Autowired
    private DiagramRepository diagrams;

    @Autowired
    private ProjectRepository projects;

    @Test
    void debePersistirDiagramaConVersionInicial() {
        Project project = new Project();
        project.setName("Biblioteca");
        project.setOwnerName("Tatiana");
        Project savedProject = projects.saveAndFlush(project);
        Diagram diagram = new Diagram();
        diagram.setProjectId(savedProject.getId());
        diagram.setName("Dominio");

        Diagram saved = diagrams.saveAndFlush(diagram);

        assertNotNull(saved.getId());
        assertEquals(0L, saved.getVersion());
        assertEquals(1, diagrams.findByProjectId(savedProject.getId()).size());
    }
}
