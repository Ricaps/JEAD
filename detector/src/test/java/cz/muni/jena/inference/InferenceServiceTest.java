package cz.muni.jena.inference;

import cz.muni.jena.codeminer.extractor.comments.model.Comment;
import cz.muni.jena.codeminer.extractor.comments.CommentType;
import cz.muni.jena.codeminer.extractor.comments.model.CommentsMapper;
import cz.muni.jena.exception.InferenceFailedException;
import cz.muni.jena.grpc.InferenceRequest;
import cz.muni.jena.grpc.InferenceResponse;
import cz.muni.jena.grpc.InferenceServiceGrpc;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.inference.model.mapping.InferenceMapper;
import cz.muni.jena.inference.model.mapping.ModelSerializer;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InferenceServiceTest {

    public static final Comment COMMENT_1 = new Comment(CommentType.JAVADOC, "test", 0, "test");
    public static final Comment COMMENT_2 = new Comment(CommentType.LINE, "test-2", 10, "test-2");
    public static final List<InferenceItem<Comment>> INPUTS = List.of(
            new InferenceItem<>(COMMENT_1, null),
            new InferenceItem<>(COMMENT_2, null)
    );
    private InferenceServiceGrpc.InferenceServiceBlockingV2Stub stub;

    private InferenceService inferenceService;

    private ModelSerializer modelSerializer;

    final private CommentsMapper commentsMapper = Mappers.getMapper(CommentsMapper.class);

    final private ObjectMapper objectMapper = new ObjectMapper();

    private static void checkResult(InferenceItem<Comment> result1, InferenceItem<Comment> inferenceItem, InferenceResponse.InferenceResponseContent expectedResponseContent1, List<InferenceResponse.LabelEvaluation> labelEvaluations) {
        assertThat(result1.id()).isEqualTo(UUID.fromString(expectedResponseContent1.getId()));
        assertThat(result1.evaluableItem()).isEqualTo(inferenceItem.evaluableItem());
        assertThat(result1.labels()).hasSize(2);
        assertThat(result1.labels().get(0))
                .hasFieldOrPropertyWithValue("labelName", labelEvaluations.get(0).getLabel())
                .hasFieldOrPropertyWithValue("value", labelEvaluations.get(0).getScore());

        assertThat(result1.labels().get(1))
                .hasFieldOrPropertyWithValue("labelName", labelEvaluations.get(1).getLabel())
                .hasFieldOrPropertyWithValue("value", labelEvaluations.get(1).getScore());
    }

    private static InferenceResponse.LabelEvaluation buildLabel(String value, double value1) {
        return InferenceResponse.LabelEvaluation.newBuilder()
                .setLabel(value)
                .setScore(value1)
                .build();
    }

    @BeforeEach
    void beforeEach() {
        stub = mock(InferenceServiceGrpc.InferenceServiceBlockingV2Stub.class);
        InferenceMapper inferenceMapper = Mappers.getMapper(InferenceMapper.class);
        modelSerializer = mock(ModelSerializer.class);
        inferenceService = new InferenceService(stub, inferenceMapper, modelSerializer);
    }

    @Test
    void doInference_modelNameNull_throwsException() {
        assertThatThrownBy(() -> inferenceService.doInference(List.of(), null, 60000))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void doInference_emptyInput_emptyOutput() throws StatusException {
        List<?> result = inferenceService.doInference(List.of(), "model-name", 60000).toList();

        assertThat(result).isEmpty();
        verify(stub, times(0)).modelInference(any());
    }

    @Test
    void doInference_correctInput_correctOutput() throws StatusException {
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        InferenceResponse.LabelEvaluation labelEvaluation1 = buildLabel("label-1", 0.99);

        InferenceResponse.LabelEvaluation labelEvaluation2 = buildLabel("label-2", 0.01);

        InferenceResponse.InferenceResponseContent expectedResponseContent1 = InferenceResponse.InferenceResponseContent.newBuilder()
                .setId(INPUTS.get(0).id().toString())
                .addAllLabelEvaluation(List.of(labelEvaluation1, labelEvaluation2))
                .build();

        InferenceResponse.InferenceResponseContent expectedResponseContent2 = InferenceResponse.InferenceResponseContent.newBuilder()
                .setId(INPUTS.get(1).id().toString())
                .addAllLabelEvaluation(List.of(labelEvaluation1, labelEvaluation2))
                .build();

        InferenceResponse expectedResponse = InferenceResponse.newBuilder().addAllContents(List.of(expectedResponseContent1, expectedResponseContent2)).build();

        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenReturn(expectedResponse);
        List<InferenceItem<Comment>> result = inferenceService.doInference(INPUTS, "model-name", 60000).toList();
        verify(stub, times(1)).modelInference(any());

        checkResult(result.get(0), INPUTS.get(0), expectedResponseContent1, List.of(labelEvaluation1, labelEvaluation2));
        checkResult(result.get(1), INPUTS.get(1), expectedResponseContent2, List.of(labelEvaluation1, labelEvaluation2));
    }

    @Test
    void doInference_throwsStatusException_translatedToInferenceException() throws StatusException {
        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenThrow(new StatusException(Status.CANCELLED));
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        assertThatThrownBy(() -> inferenceService.doInference(INPUTS, "model-name", 60000))
                .isInstanceOf(InferenceFailedException.class)
                .hasMessage("Evaluation of inference request failed with status %s".formatted(Status.CANCELLED));
    }

    @Test
    void doInference_throwsStatusRuntimeException_notTranslated() throws StatusException {
        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenThrow(new StatusRuntimeException(Status.CANCELLED));
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        assertThatThrownBy(() -> inferenceService.doInference(INPUTS, "model-name", 60000).toList())
                .isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void doInference_responseContainsInvalidUuid_throwsInferenceException() throws StatusException {
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        InferenceResponse response = InferenceResponse.newBuilder()
                .addContents(InferenceResponse.InferenceResponseContent.newBuilder()
                        .setId("invalid-uuid")
                        .addLabelEvaluation(buildLabel("label-1", 1.0))
                        .build())
                .build();

        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenReturn(response);

        assertThatThrownBy(() -> inferenceService.doInference(INPUTS, "model-name", 60000).toList())
                .isInstanceOf(InferenceFailedException.class)
                .hasMessage("Failed to parse ID of the response item");
    }

    @Test
    void doInference_responseContainsUnknownUuid_throwsInferenceException() throws StatusException {
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        String missingId = UUID.randomUUID().toString();
        InferenceResponse response = InferenceResponse.newBuilder()
                .addContents(InferenceResponse.InferenceResponseContent.newBuilder()
                        .setId(missingId)
                        .addLabelEvaluation(buildLabel("label-1", 1.0))
                        .build())
                .build();

        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenReturn(response);

        assertThatThrownBy(() -> inferenceService.doInference(INPUTS, "model-name", 60000).toList())
                .isInstanceOf(InferenceFailedException.class)
                .hasMessage("Cannot find reference inference item with ID %s".formatted(missingId));
    }

    @Test
    void doInference_serializerThrowsException_requestIsNotSent() throws StatusException {
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenThrow(new RuntimeException("serialize-failed"));

        assertThatThrownBy(() -> inferenceService.doInference(INPUTS, "model-name", 60000).toList())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("serialize-failed");

        verify(stub, never()).modelInference(any(InferenceRequest.class));
    }

    @Test
    void doInference_passesConfiguredTimeoutToStub() throws StatusException {
        int timeout = 1234;
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        InferenceResponse response = InferenceResponse.newBuilder()
                .addAllContents(List.of(
                        InferenceResponse.InferenceResponseContent.newBuilder().setId(INPUTS.get(0).id().toString()).build(),
                        InferenceResponse.InferenceResponseContent.newBuilder().setId(INPUTS.get(1).id().toString()).build()
                ))
                .build();

        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenReturn(response);

        inferenceService.doInference(INPUTS, "model-name", timeout).toList();

        verify(stub, times(1)).withDeadlineAfter(Duration.ofMillis(timeout));
    }

    @Test
    void doInference_responseOrderIsPreservedInOutput() throws StatusException {
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        InferenceResponse response = InferenceResponse.newBuilder()
                .addAllContents(List.of(
                        InferenceResponse.InferenceResponseContent.newBuilder()
                                .setId(INPUTS.get(1).id().toString())
                                .addLabelEvaluation(buildLabel("label-2", 1.0))
                                .build(),
                        InferenceResponse.InferenceResponseContent.newBuilder()
                                .setId(INPUTS.get(0).id().toString())
                                .addLabelEvaluation(buildLabel("label-1", 1.0))
                                .build()
                ))
                .build();

        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenReturn(response);

        List<InferenceItem<Comment>> result = inferenceService.doInference(INPUTS, "model-name", 60000).toList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(INPUTS.get(1).id());
        assertThat(result.get(1).id()).isEqualTo(INPUTS.get(0).id());
    }

    @Test
    void doInference_responseWithoutLabels_mapsToEmptyLabelList() throws StatusException {
        when(modelSerializer.getSerializedDto(COMMENT_1)).thenReturn(serialize(COMMENT_1));
        when(modelSerializer.getSerializedDto(COMMENT_2)).thenReturn(serialize(COMMENT_2));

        InferenceResponse response = InferenceResponse.newBuilder()
                .addAllContents(List.of(
                        InferenceResponse.InferenceResponseContent.newBuilder().setId(INPUTS.get(0).id().toString()).build(),
                        InferenceResponse.InferenceResponseContent.newBuilder().setId(INPUTS.get(1).id().toString()).build()
                ))
                .build();

        when(stub.withDeadlineAfter(any())).thenReturn(stub);
        when(stub.withDeadlineAfter(any()).modelInference(any())).thenReturn(response);

        List<InferenceItem<Comment>> result = inferenceService.doInference(INPUTS, "model-name", 60000).toList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).labels()).isEmpty();
        assertThat(result.get(1).labels()).isEmpty();
    }

    private String serialize(Comment comment) {
        try {
            return objectMapper.writeValueAsString(commentsMapper.toDto(comment));
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

}