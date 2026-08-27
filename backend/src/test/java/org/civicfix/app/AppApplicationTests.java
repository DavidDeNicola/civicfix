package org.civicfix.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica che l'intero contesto Spring si avvii correttamente — tutti i
 * bean, la configurazione di sicurezza, i repository JPA. Usa il profilo
 * "test" (H2 in memoria, vedi application-test.properties) invece del MySQL
 * di sviluppo: senza questo, il test richiederebbe un database esterno
 * raggiungibile e le variabili d'ambiente di produzione, e fallirebbe con
 * "mvn test" su qualunque macchina diversa da quella dello sviluppatore.
 */
@SpringBootTest
@ActiveProfiles("test")
class AppApplicationTests {

	@Test
	void contextLoads() {
	}

}
