package cz.muni.jena.codeminer.extractor.comments.model;

import cz.muni.jena.inference.dto.BaseDto;

public record CommentDto(String text, String commentType) implements BaseDto {
}
