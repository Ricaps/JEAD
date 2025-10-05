package cz.muni.jena.inference.model.mapping;

import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.exception.InferenceFailedException;
import cz.muni.jena.grpc.InferenceRequest;
import cz.muni.jena.grpc.InferenceResponse;
import cz.muni.jena.inference.InferenceUtil;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.inference.model.Label;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface InferenceMapper {

    InferenceRequest.InferenceRequestContent mapItemToRequest(InferenceItem<?> inferenceItem);
    Collection<InferenceRequest.InferenceRequestContent> mapItemToRequest(Collection<? extends InferenceItem<?>> inferenceItem);
    InferenceRequest mapContentsToRequest(Collection<InferenceRequest.InferenceRequestContent> contents, String modelName);

    @Mapping(source = "label", target = "labelName")
    @Mapping(source = "score", target = "value")
    Label mapLabelEvaluationToLabel(InferenceResponse.LabelEvaluation labelEvaluation);
    List<Label> mapLabelEvaluationToLabel(List<InferenceResponse.LabelEvaluation> labelEvaluation);

    default <T extends EvaluatedNode> InferenceItem<T> mapResponseToItem(Map<UUID, InferenceItem<T>> inferenceItemMap, InferenceResponse.InferenceResponseContent responseContent) throws InferenceFailedException {
        UUID itemID = InferenceUtil.parseItemID(responseContent.getId());
        InferenceItem<T> referenceItem = inferenceItemMap.get(itemID);
        List<Label> labels = mapLabelEvaluationToLabel(responseContent.getLabelEvaluationList());

        if (referenceItem == null) {
            throw new InferenceFailedException("Cannot find reference inference item with ID %s".formatted(responseContent.getId()));
        }
        return new InferenceItem<>(itemID, referenceItem.evaluableItem(), labels);

    }
}
