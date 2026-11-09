package br.com.organizacao.recursos.reservas;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/** Fluxo completo com armazenamento em memoria (perfil padrao). */
@SpringBootTest(properties = "app.demo-data.enabled=true")
@AutoConfigureMockMvc
class ApplicationFlowTest extends AbstractApplicationFlowTest {
}
