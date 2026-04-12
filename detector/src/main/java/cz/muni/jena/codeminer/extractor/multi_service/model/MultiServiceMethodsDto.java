package cz.muni.jena.codeminer.extractor.multi_service.model;

import cz.muni.jena.inference.dto.BaseDto;

import java.util.List;

public record MultiServiceMethodsDto(List<MethodDto> methods) implements BaseDto {

    public record MethodDto(String name, String signature) {}
}
