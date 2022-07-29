package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrustructure.persistence.repositories.config;

import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version.Main;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mongo-integration-test")
public class EmbeddedConfigMondodb {

  private final MongoProperties properties;

  public EmbeddedConfigMondodb(MongoProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnMissingBean
  public MongodConfig embeddedMongoConfiguration(EmbeddedMongoProperties embeddedProperties) throws IOException {
    final EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();
    final Integer configuredPort = Optional.ofNullable(this.properties.getPort()).orElse(12000);
    final ImmutableMongodConfig.Builder builder = MongodConfig.builder()
        .version(Main.PRODUCTION)
        .net(new Net(
            InetAddress.getByName("localhost").getHostAddress(), configuredPort, Network.localhostIsIPv6()));

    if (storage != null) {
      String databaseDir = storage.getDatabaseDir();
      String replSetName = storage.getReplSetName();
      int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;

      builder
          .replication(new Storage(databaseDir, replSetName, oplogSize))
          .cmdOptions(MongoCmdOptions.builder().useNoJournal(false).build());
    }
    return builder.build();
  }

}
