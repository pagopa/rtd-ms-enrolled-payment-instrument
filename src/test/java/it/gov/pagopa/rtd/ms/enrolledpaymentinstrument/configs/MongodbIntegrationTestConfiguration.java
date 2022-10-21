package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs;

import com.mongodb.client.MongoClient;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;

import javax.annotation.PostConstruct;
import java.io.IOException;

@TestConfiguration
@Import(MongodbIntegrationTestConfiguration.ReplicaSetInitialization.class)
@Profile("mongo-integration-test")
public class MongodbIntegrationTestConfiguration {

  private static final String IP = "localhost";
  private static final int PORT = 28017;

  /* Useless beans */
  @MockBean
  KafkaMessageDrivenChannelAdapter<String, TokenManagerWalletChanged> input;

  @Bean
  public MongodConfig embeddedMongoConfiguration() throws IOException {
    return MongodConfig.builder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(IP, PORT, Network.localhostIsIPv6()))
            .replication(new Storage(null, "rs0", 10))
            .cmdOptions(MongoCmdOptions.builder().useNoJournal(false).build())
            .build();
  }

  @TestConfiguration
  @Profile("mongo-integration-test")
  @Slf4j
  static class ReplicaSetInitialization {

    @Autowired
    private MongoClient mongoClient;

    @PostConstruct
    private void initializeReplica() {
      boolean replicaReady = false;
      int maxAttempts = 10;

      mongoClient
              .getDatabase("admin")
              .runCommand(new Document("replSetInitiate", new Document()));

      for (int i = 0; i < maxAttempts && !replicaReady; i++) {
        replicaReady = isReplicaSetReady(mongoClient);
      }
    }

    private Boolean isReplicaSetReady(MongoClient mongoClient) {
      final String adminDb = "admin";

      double replSetStatusOk = (double) mongoClient
              .getDatabase(adminDb)
              .runCommand(new Document("replSetGetStatus", 1))
              .get("ok");
      if (replSetStatusOk == 1.0) {
        log.debug("ReplStatusOK is 1.0");
        boolean currentIsMaster = (boolean) mongoClient
                .getDatabase(adminDb)
                .runCommand(new Document("isMaster",
                        1)).get("ismaster");
        if (!currentIsMaster) {
          log.debug("Replica set is not ready. Waiting for node to become master.");
        } else {
          log.debug("Replica set is ready. Node is now master.");
        }
        return currentIsMaster;
      } else {
        log.debug("Replica set is not ready. Waiting for replStatusOK to be 1.0. Currently {}",
                replSetStatusOk);
        return false;
      }
    }

  }

}
