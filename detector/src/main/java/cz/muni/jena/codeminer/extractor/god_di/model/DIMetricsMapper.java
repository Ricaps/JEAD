package cz.muni.jena.codeminer.extractor.god_di.model;

import cz.muni.jena.inference.model.mapping.ModelDtoMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DIMetricsMapper extends ModelDtoMapper<DIMetrics, DIMetricsDto> {

    @Override
    @Mapping(source = "linesOfCode", target = "loc")
    @Mapping(source = "cyclomaticComplexity", target = "cc")
    @Mapping(source = "injectedFields", target = "nooa")
    @Mapping(source = "lcom5", target = "LCOM5")
    @Mapping(source = "methodsCount", target = "noom")
    @Mapping(source = "staticMethodsCount", target = "nocm")
    DIMetricsDto toDto(DIMetrics model);
}
