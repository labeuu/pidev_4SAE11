package org.example.subcontracting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aligne le schéma MySQL avec le modèle métier :
 * - une mission est soit un projet soit une offre, donc {@code project_id} ou {@code offer_id} peut être NULL
 * - le statut doit accepter les nouveaux états de négociation IA, même sur bases anciennes
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SubcontractMissionColumnsInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final AtomicBoolean RAN = new AtomicBoolean(false);

    private final DataSource dataSource;

    public SubcontractMissionColumnsInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        if (!RAN.compareAndSet(false, true)) {
            return;
        }
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            String product = md != null && md.getDatabaseProductName() != null
                    ? md.getDatabaseProductName().toLowerCase()
                    : "";
            if (!product.contains("mysql") && !product.contains("mariadb")) {
                log.debug("[SUBCONTRACT] Ajustement colonnes mission ignoré (SGBD: {})", product);
                return;
            }
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE subcontracts MODIFY COLUMN project_id BIGINT NULL");
                st.execute("ALTER TABLE subcontracts MODIFY COLUMN offer_id BIGINT NULL");
                // Important: certaines bases historiques ont status en ENUM sans les nouvelles valeurs.
                // On force VARCHAR pour éviter "Data truncated for column 'status'" lors des transitions IA.
                st.execute("ALTER TABLE subcontracts MODIFY COLUMN status VARCHAR(32) NOT NULL");
                log.info("[SUBCONTRACT] Colonnes project_id/offer_id alignées (NULL autorisé) + status en VARCHAR(32)");
            }
        } catch (Exception e) {
            log.warn("[SUBCONTRACT] Impossible d'aligner schéma subcontracts (table absente ou déjà OK) : {}",
                    e.getMessage());
        }
    }
}
