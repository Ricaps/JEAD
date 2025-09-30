package cz.muni.jena.configuration.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import cz.muni.jena.grpc.InferenceServiceGrpc;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    public static final String INFERENCE_SERVER_CHANNEL = "inference-server";

    @Bean
    InferenceServiceGrpc.InferenceServiceBlockingV2Stub inferenceStub(GrpcChannelFactory channelFactory) {
        return InferenceServiceGrpc.newBlockingV2Stub(channelFactory.createChannel(INFERENCE_SERVER_CHANNEL));
    }
}
