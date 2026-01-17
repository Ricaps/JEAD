package cz.muni.jena.codeminer.extractor.multi_service.model;

import cz.muni.jena.inference.model.mapping.ModelDtoMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MultiServiceMethodsMapper extends ModelDtoMapper<MultiServiceMethods, MultiServiceMethodsDto> {

    @Override
    MultiServiceMethodsDto toDto(MultiServiceMethods model);
}
