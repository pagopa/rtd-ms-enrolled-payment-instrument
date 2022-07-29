package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrustructure.persistence.repositories.config;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@TestConfiguration
@Profile("mongo-integration-test")
@Slf4j
public class MongodbReplicaConfig {

  @Resource
  private MongoClient mongoClient;

  @PostConstruct
  public void waitForReplicaSetStatusOk() {
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
