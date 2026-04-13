package cz.muni.jena.inference.model.mapping;

import cz.muni.jena.inference.model.EvaluationModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class ModelMapperRegistryConfig {

    @Bean
    public ModelMapperRegistry mapperRegistry(List<ModelDtoMapper<?, ?>> mappers) {
        Map<Class<? extends EvaluationModel>, ModelDtoMapper<?, ?>> registry = new HashMap<>();

        for (ModelDtoMapper<?, ?> mapper : mappers) {
            registry.put(resolveType(mapper).asSubclass(EvaluationModel.class), mapper);
        }

        return new ModelMapperRegistry(registry);
    }

    private Class<?> resolveType(ModelDtoMapper<?, ?> mapper) {
        ResolvableType resolvableType = ResolvableType.forClass(mapper.getClass());

        ResolvableType mapperType = resolvableType.as(ModelDtoMapper.class);

        ResolvableType modelType = mapperType.getGeneric(0);

        Class<?> resolved = modelType.resolve();
        if (resolved == null) {
            throw new IllegalStateException("Cannot resolve generic mapper %s".formatted(mapper.getClass()));
        }

        return resolved;
    }
}
