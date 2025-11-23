package cz.muni.jena.codeminer.extractor.comments.model;

import cz.muni.jena.inference.model.mapping.ModelDtoMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentsMapper extends ModelDtoMapper<Comment, CommentDto> {
}
