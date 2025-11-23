package cz.muni.jena.inference.model.mapping;

import cz.muni.jena.inference.dto.BaseDto;
import cz.muni.jena.inference.model.EvaluationModel;

public interface ModelDtoMapper<Model extends EvaluationModel, Dto extends BaseDto> {

    Dto toDto(Model model);
}
