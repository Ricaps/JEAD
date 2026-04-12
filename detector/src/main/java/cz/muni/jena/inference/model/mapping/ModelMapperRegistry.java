package cz.muni.jena.inference.model.mapping;

import cz.muni.jena.inference.model.EvaluationModel;

import java.util.Map;

public final class ModelMapperRegistry  {

    private final Map<Class<? extends EvaluationModel>, ModelDtoMapper<?, ?>> mapperMap;

    public ModelMapperRegistry(Map<Class<? extends EvaluationModel>, ModelDtoMapper<?, ?>> mapperMap) {
        this.mapperMap = mapperMap;
    }

    @SuppressWarnings("unchecked")
    public <Model extends EvaluationModel> ModelDtoMapper<Model, ?> getMapper(Class<Model> modelClass) {
        var mapper = (ModelDtoMapper<Model, ?>) mapperMap.get(modelClass);
        if (mapper == null) {
            throw new IllegalStateException("Failed to found mapper for model %s".formatted(modelClass));
        }
        return mapper;
    }
}
