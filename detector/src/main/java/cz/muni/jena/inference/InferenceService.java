package cz.muni.jena.inference;

import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.exception.InferenceFailedException;
import cz.muni.jena.grpc.InferenceRequest;
import cz.muni.jena.grpc.InferenceResponse;
import cz.muni.jena.grpc.InferenceServiceGrpc;
import cz.muni.jena.grpc.ServerReadyRequest;
import cz.muni.jena.grpc.ServerReadyResponse;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.inference.model.Label;
import io.grpc.StatusException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
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

    public InferenceService(InferenceServiceGrpc.InferenceServiceBlockingV2Stub inferenceServiceStub) {
        this.inferenceServiceStub = inferenceServiceStub;
    }

    public <T extends EvaluatedNode> Stream<InferenceItem<T>> doInference(Collection<InferenceItem<T>> inferenceItemCollection, @Nonnull String modelName) {
        Objects.requireNonNull(modelName, "Model name cannot be null!");
        if (inferenceItemCollection.isEmpty()) {
            return Stream.of();
        }

        List<InferenceRequest.InferenceRequestContent> contents = inferenceItemCollection
                .stream()
                .map(inferenceItem -> InferenceRequest.InferenceRequestContent
                        .newBuilder()
                        .setId(inferenceItem.id().toString())
                        .setContent(inferenceItem.getContent())
                        .build()
                ).toList();

        LOGGER.info("Sending request with len {}", inferenceItemCollection.size());
        InferenceRequest inferenceRequest = InferenceRequest.newBuilder().addAllContents(contents).setModelName(modelName).build();

        InferenceResponse response = runRequest(inferenceRequest);
        Map<UUID, InferenceItem<T>> inferenceItemMap = inferenceItemCollection.stream().collect(Collectors.toMap(InferenceItem::id, e -> e));
        return response.getContentsList().stream().map(contentList -> this.mapResponseToItem(inferenceItemMap, contentList));
    }

    private <T extends EvaluatedNode> InferenceItem<T> mapResponseToItem(Map<UUID, InferenceItem<T>> inferenceItemMap, InferenceResponse.InferenceResponseContent responseContent) throws InferenceFailedException {
        UUID itemID = parseItemID(responseContent.getId());
        InferenceItem<T> referenceItem = inferenceItemMap.get(itemID);
        List<Label> labels = responseContent.getLabelEvaluationList().stream().map(this::mapLabelEvaluation).toList();

        if (referenceItem == null) {
            throw new InferenceFailedException("Cannot find reference inference item with ID %s".formatted(responseContent.getId()));
        }
        return new InferenceItem<T>(itemID, referenceItem.evaluableItem(), labels);

    }

    private UUID parseItemID(String itemID) {
        try {
            return UUID.fromString(itemID);
        } catch (IllegalArgumentException ex) {
            throw new InferenceFailedException("Failed to parse ID of the response item", ex);
        }

    }

    private Label mapLabelEvaluation(InferenceResponse.LabelEvaluation labelEvaluation) {
        return new Label(labelEvaluation.getLabel(), labelEvaluation.getScore());
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

    private boolean isServerReady() {
        try {
            ServerReadyResponse response = inferenceServiceStub.serverReady(ServerReadyRequest.newBuilder().build());
            return response.getReady();
        } catch (StatusException e) {
            return false;
        }
    }

}
