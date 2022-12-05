package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs;

import com.mongodb.client.MongoClient;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@TestConfiguration
@AutoConfigureBefore(EmbeddedMongoAutoConfiguration.class)
@Import(MongodbIntegrationTestConfiguration.ReplicaSetInitialization.class)
public class MongodbIntegrationTestConfiguration {

  private static final String IP = "localhost";
  private static final int PORT = 28017;

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
  @Slf4j
  static class ReplicaSetInitialization {

    @Autowired
    private MongoClient mongoClient;

    @PostConstruct
    private void initializeReplica() {
      mongoClient
              .getDatabase("admin")
              .runCommand(new Document("replSetInitiate", new Document()));

      await().atMost(10, TimeUnit.SECONDS)
              .pollInterval(1, TimeUnit.SECONDS)
              .until(() -> isReplicaSetReady(mongoClient));
    }

    private Boolean isReplicaSetReady(MongoClient mongoClient) {
      final String adminDb = "admin";

      double replSetStatusOk = (double) mongoClient
              .getDatabase(adminDb)
              .runCommand(new Document("replSetGetStatus", 1))
              .get("ok");
      if (replSetStatusOk == 1.0) {
        log.info("ReplStatusOK is 1.0");
        boolean currentIsMaster = (boolean) mongoClient
                .getDatabase(adminDb)
                .runCommand(new Document("isMaster",
                        1)).get("ismaster");
        if (!currentIsMaster) {
          log.info("Replica set is not ready. Waiting for node to become master.");
        } else {
          log.info("Replica set is ready. Node is now master.");
        }
        return currentIsMaster;
      } else {
        log.warn("Replica set is not ready. Waiting for replStatusOK to be 1.0. Currently {}",
                replSetStatusOk);
        return false;
      }
    }

  }

}
