package cz.muni.jena.inference.model.mapping;

import cz.muni.jena.inference.dto.BaseDto;
import cz.muni.jena.inference.model.EvaluationModel;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class ModelSerializer {

    private final Map<Class<? extends EvaluationModel>, ModelDtoMapper<?, ?>> mapperRegistry;
    private final ObjectMapper objectMapper;

    public ModelSerializer(Map<Class<? extends EvaluationModel>, ModelDtoMapper<?, ?>> mapperRegistry, ObjectMapper objectMapper) {
        this.mapperRegistry = mapperRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes the EvaluationModel to the JSON String, which can be then sent as a content to the inference server <br>
     * Automatically picks Mapper for concrete model class. Mapper must inherit from {@link ModelDtoMapper} class
     * and be registered as bean.
     *
     * @param evaluationModel model to be serialized
     * @return serialized model as string
     */
    @SuppressWarnings("unchecked")
    public <Model extends EvaluationModel> String getSerializedDto(Model evaluationModel) {
        var mapper = (ModelDtoMapper<Model, ? extends BaseDto>) mapperRegistry.get(evaluationModel.getClass());

        if (mapper == null) {
            throw new IllegalStateException("Failed to found mapper for model %s".formatted(evaluationModel));
        }

        BaseDto dto = mapper.toDto(evaluationModel);
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot map %s class to dto!".formatted(evaluationModel.getClass().getName()), e);
        }
    }
}
