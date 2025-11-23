package cz.muni.jena.inference;

import cz.muni.jena.exception.InferenceFailedException;
import cz.muni.jena.grpc.*;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.inference.model.mapping.InferenceMapper;
import cz.muni.jena.inference.model.mapping.ModelSerializer;
import io.grpc.StatusException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty("inference.enabled")
public class InferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferenceService.class);
    private final InferenceServiceGrpc.InferenceServiceBlockingV2Stub inferenceServiceStub;
    private final InferenceMapper inferenceMapper;
    private final ModelSerializer modelSerializer;

    public InferenceService(InferenceServiceGrpc.InferenceServiceBlockingV2Stub inferenceServiceStub, InferenceMapper inferenceMapper, ModelSerializer modelSerializer) {
        this.inferenceServiceStub = inferenceServiceStub;
        this.inferenceMapper = inferenceMapper;
        this.modelSerializer = modelSerializer;
    }

    public <T extends EvaluationModel> Stream<InferenceItem<T>> doInference(Collection<InferenceItem<T>> inferenceItemCollection, @Nonnull String modelName) {
        Objects.requireNonNull(modelName, "Model name cannot be null!");
        if (inferenceItemCollection.isEmpty()) {
            return Stream.of();
        }

        Collection<InferenceRequest.InferenceRequestContent> contents = inferenceItemCollection.stream()
                .map(inferenceItem ->
                        inferenceMapper.mapItemToRequest(inferenceItem, modelSerializer.getSerializedDto(inferenceItem.evaluableItem()))
                )
                .collect(Collectors.toSet());

        LOGGER.info("Sending request with len {}", inferenceItemCollection.size());
        InferenceRequest inferenceRequest = inferenceMapper.mapContentsToRequest(contents, modelName);

        InferenceResponse response = runRequest(inferenceRequest);
        Map<UUID, InferenceItem<T>> inferenceItemMap = inferenceItemCollection.stream().collect(Collectors.toMap(InferenceItem::id, e -> e));
        Stream<InferenceItem<T>> nodes = response.getContentsList().stream().map(contentList -> this.inferenceMapper.mapResponseToItem(inferenceItemMap, contentList));

        var debug = nodes.toList();

        return debug.stream();
    }

    private InferenceResponse runRequest(InferenceRequest inferenceRequest) {
        try {
            return inferenceServiceStub.modelInference(inferenceRequest);
        } catch (StatusException e) {
            throw new InferenceFailedException("Evaluation of inference request failed with status %s".formatted(e.getStatus()), e);
        }
    }

    @PostConstruct
    private void afterInit() {
        boolean isReady = isServerReady();

        if (!isReady) {
            LOGGER.warn("Inference server is not available! Please check the connection!");
            return;
        }

        LOGGER.info("Connection to inference server succeeded!");
    }

    public boolean isServerReady() {
        try {
            ServerReadyResponse response = inferenceServiceStub.serverReady(ServerReadyRequest.newBuilder().build());
            return response.getReady();
        } catch (StatusException e) {
            return false;
        }
    }

}
