package br.com.organizacao.recursos.reservas.cucumber;

import br.com.organizacao.recursos.OrganizacaoRecursosApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(classes = OrganizacaoRecursosApplication.class)
@AutoConfigureMockMvc
@Import(CucumberMethodSecurityConfiguration.class)
public class CucumberSpringConfiguration {
}
